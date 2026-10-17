package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.net.Uri;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Navegador embutido no host principal. A WebView não é destruída na troca de abas. */
final class BrowserPane {
    private final MainActivity host;
    private final Ui.Palette colors;
    final LinearLayout view;
    private WebView web;
    private EditText address;
    private TextView status;
    private ProgressBar loading;
    private FrameLayout securityIcon;
    private LinearLayout findBar;
    private EditText findInput;
    private TextView findCount;
    private String standardUserAgent;
    private boolean appliedDesktop;
    private int appliedTextZoom;
    private boolean appliedThirdPartyCookies;
    private TextView videosButton;
    private final LinkedHashMap<String,String> videoUrls = new LinkedHashMap<>();
    private int mediaElements;
    private int unsupportedMedia;
    private int scanGeneration;

    // Executado somente quando a pessoa pede a busca ou após uma página terminar.
    // Não injeta bridge JS nem lê cookies, tokens de sessão ou dados de outros frames.
    private static final String VIDEO_SCAN_JS = "(function(){try{" +
            "var out={media:[],players:0,unsupported:0};" +
            "function put(u,k,t){if(!u||typeof u!=='string')return;" +
            "if(u.startsWith('blob:')||u.startsWith('data:')||u.startsWith('mediasource:')){out.unsupported++;return;}" +
            "if(out.media.length<40&&u.length<4096)out.media.push({url:u,kind:k,mime:t||''});}" +
            "var players=document.querySelectorAll('video');out.players=players.length;" +
            "for(var i=0;i<players.length&&i<30;i++){var v=players[i];put(v.currentSrc||v.src,'video',v.getAttribute('type'));" +
            "var src=v.querySelectorAll('source');for(var j=0;j<src.length&&j<6;j++)put(src[j].src,'source',src[j].type);}" +
            "var sources=document.querySelectorAll('source[type^=\"video/\"]');" +
            "for(var i=0;i<sources.length&&i<30;i++)put(sources[i].src,'source',sources[i].type);" +
            "var links=document.querySelectorAll('a[href]');for(var i=0;i<links.length&&i<150;i++){" +
            "var a=links[i];if(/\\.(mp4|m4v|webm|mov|ogv|3gp)(\\?|#|$)/i.test(a.href))put(a.href,'link','');}" +
            "return JSON.stringify(out);" +
            "}catch(e){return JSON.stringify({media:[],players:0,unsupported:0});}})();";


    BrowserPane(MainActivity host, Ui.Palette colors, Bundle saved) {
        this.host = host;
        this.colors = colors;
        view = Ui.vertical(host);
        view.setPadding(dp(12),dp(4),dp(12),dp(4));
        view.setBackgroundColor(colors.background);
        build(saved);
    }
    private int dp(int n) { return Ui.dp(host,n); }

    private FrameLayout iconButton(String symbol, String description, int size, Runnable run) {
        FrameLayout frame = new FrameLayout(host);
        frame.setClickable(true);frame.setFocusable(true);frame.setContentDescription(description);
        frame.addView(Ui.icon(host,symbol,colors.text,21),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));
        frame.setOnClickListener(v -> run.run());
        frame.setMinimumHeight(dp(44));
        return frame;
    }
    private void build(Bundle saved) {
        LinearLayout top = Ui.horizontal(host);
        LinearLayout row = Ui.horizontal(host);
        row.setBackground(Ui.shape(host,colors.surface,20));
        row.setPadding(dp(5),0,dp(4),0);
        securityIcon=new FrameLayout(host);
        row.addView(securityIcon,new LinearLayout.LayoutParams(dp(32),dp(44)));
        address=new EditText(host);
        address.setSingleLine(true);address.setSelectAllOnFocus(true);
        address.setTextSize(14); address.setTextColor(colors.text);address.setHintTextColor(colors.muted);
        address.setHint("Pesquisar ou digitar endereço");
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setPadding(0,0,dp(3),0);
        address.setOnEditorActionListener((v,a,event)->{
            if (a==EditorInfo.IME_ACTION_GO) { navigate(address.getText().toString()); return true; }
            return false;
        });
        row.addView(address,new LinearLayout.LayoutParams(0,dp(48),1));
        row.addView(iconButton("copy","Copiar endereço",34,this::copyAddress),new LinearLayout.LayoutParams(dp(34),dp(44)));
        row.addView(iconButton("chevron","Histórico recente",32,this::showHistory),new LinearLayout.LayoutParams(dp(32),dp(44)));
        row.addView(iconButton("forward","Abrir endereço",38,()->navigate(address.getText().toString())),new LinearLayout.LayoutParams(dp(38),dp(44)));
        top.addView(row,new LinearLayout.LayoutParams(0,dp(48),1));
        top.addView(iconButton("more","Ferramentas da página",44,this::showPageTools),new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout.LayoutParams settingsP = new LinearLayout.LayoutParams(dp(44),dp(44));
        settingsP.leftMargin = dp(2);
        top.addView(iconButton("settings","Configurações",44,host::openSettings),settingsP);
        view.addView(top,Ui.lp(host,-1,48,0));
        showSecurity(BrowserPrefs.home(host));

        findBar = Ui.horizontal(host);
        findBar.setPadding(dp(7),0,dp(5),0);
        findBar.setBackground(Ui.shape(host,colors.surface,17));
        findInput = new EditText(host);
        findInput.setSingleLine(true);
        findInput.setHint("Localizar na página");
        findInput.setTextSize(14);
        findInput.setTextColor(colors.text);
        findInput.setHintTextColor(colors.muted);
        findInput.setBackgroundColor(Color.TRANSPARENT);
        findInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        findInput.setPadding(dp(6),0,0,0);
        findBar.addView(findInput,new LinearLayout.LayoutParams(0,dp(44),1));
        findCount = Ui.text(host,"",11,colors.muted,false);
        findCount.setGravity(Gravity.CENTER);
        findBar.addView(findCount,new LinearLayout.LayoutParams(dp(56),dp(44)));
        findBar.addView(iconButton("back","Ocorrência anterior",40,()->web.findNext(false)),new LinearLayout.LayoutParams(dp(40),dp(44)));
        findBar.addView(iconButton("forward","Próxima ocorrência",40,()->web.findNext(true)),new LinearLayout.LayoutParams(dp(40),dp(44)));
        findBar.addView(iconButton("x","Fechar pesquisa",40,this::closeFind),new LinearLayout.LayoutParams(dp(40),dp(44)));
        findBar.setVisibility(View.GONE);
        findInput.setOnEditorActionListener((v,action,event)->{
            if(action==EditorInfo.IME_ACTION_SEARCH){web.findNext(true);return true;}
            return false;
        });
        findInput.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                if(web==null)return;
                if(s.length()==0){web.clearMatches();findCount.setText("");}
                else {findCount.setText("…");web.findAllAsync(s.toString());}
            }
            @Override public void afterTextChanged(Editable e){}
        });
        view.addView(findBar,Ui.lp(host,-1,44,5));

        loading=new ProgressBar(host,null,android.R.attr.progressBarStyleHorizontal);
        loading.setMax(100);
        loading.setProgressTintList(android.content.res.ColorStateList.valueOf(colors.teal));
        loading.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(colors.background));
        view.addView(loading,Ui.lp(host,-1,3,4));

        FrameLayout frame=new FrameLayout(host);
        frame.setBackground(Ui.shape(host,colors.surface,20));
        frame.setClipToOutline(true);
        web=new WebView(host);
        WebSettings options=web.getSettings();
        standardUserAgent=options.getUserAgentString();
        appliedDesktop=BrowserPrefs.desktop(host);
        appliedTextZoom=BrowserPrefs.textZoom(host);
        appliedThirdPartyCookies=BrowserPrefs.thirdPartyCookies(host);
        options.setSupportZoom(true);
        options.setBuiltInZoomControls(true);
        options.setDisplayZoomControls(false);
        options.setUseWideViewPort(appliedDesktop);
        options.setLoadWithOverviewMode(appliedDesktop);
        options.setTextZoom(appliedTextZoom);
        if(appliedDesktop)options.setUserAgentString(DesktopUserAgent.toDesktop(standardUserAgent));
        options.setJavaScriptEnabled(BrowserPrefs.javascript(host));
        options.setDomStorageEnabled(true);
        options.setAllowFileAccess(false);
        options.setAllowContentAccess(false);
        options.setAllowFileAccessFromFileURLs(false);
        options.setAllowUniversalAccessFromFileURLs(false);
        options.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        options.setJavaScriptCanOpenWindowsAutomatically(false);
        options.setSupportMultipleWindows(false);
        options.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,appliedThirdPartyCookies);
        web.setFindListener((active,total,finished)->{
            if(finished && findCount!=null && findBar.getVisibility()==View.VISIBLE)
                findCount.setText(total==0?"0/0":(active+1)+"/"+total);
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest request) {
                String target=request.getUrl().toString();
                if (request.isForMainFrame() && AuthNavigation.isGoogleOAuth(target)) {
                    offerExternalGoogleLogin(target);
                    return true;
                }
                if (target.equals(UrlTools.extract(target))) return false;
                if(request.isForMainFrame()) status.setText("Endereço bloqueado: apenas HTTP e HTTPS.");
                return true;
            }
            @Override public void onPageStarted(WebView w,String url,android.graphics.Bitmap icon) {
                if (AuthNavigation.isGoogleOAuth(url)) {
                    w.stopLoading();
                    offerExternalGoogleLogin(url);
                    return;
                }
                scanGeneration++;
                synchronized(videoUrls){ videoUrls.clear(); }
                mediaElements=0;
                unsupportedMedia=0;
                updateVideosButton();
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest request) {
                // Apenas observa endereços de arquivos de vídeo diretos. Não intercepta corpo/resposta.
                if (!request.isForMainFrame() && "GET".equalsIgnoreCase(request.getMethod())) {
                    String candidate=request.getUrl().toString();
                    if(VideoLinks.isDirectFile(candidate)) {
                        boolean added=false;
                        synchronized(videoUrls) {
                            if(!videoUrls.containsKey(candidate) && videoUrls.size()<VideoLinks.MAX_VIDEO_URLS) {
                                videoUrls.put(candidate,"Recurso da página");
                                added=true;
                            }
                        }
                        if(added)host.runOnUiThread(BrowserPane.this::updateVideosButton);
                    }
                }
                return null;
            }
            @Override public void onPageFinished(WebView w,String url) {
                if (url!=null && url.equals(UrlTools.extract(url))) {
                    address.setText(url);
                    showSecurity(url);
                    BrowserPrefs.remember(host,url);
                }
                if (findBar.getVisibility()==View.VISIBLE && findInput.length()>0)
                    web.findAllAsync(findInput.getText().toString());
                status.setText(url!=null&&url.startsWith("http://") ? "Atenção: conexão HTTP sem criptografia."
                    : AuthNavigation.isFacebookCheckpoint(url) ? "Verificação do Facebook: tente abrir no navegador externo pelo menu ⋮."
                    : "Página pronta. Toque num arquivo para baixar.");
                // O Facebook e outros sites podem carregar o player depois do HTML.
                final int generation=scanGeneration;
                host.getWindow().getDecorView().postDelayed(()->{
                    if(generation==scanGeneration && !AuthNavigation.isSensitiveLoginPage(web.getUrl()))
                        scanPageVideos(false);
                },1300);
            }
            @Override public void onReceivedError(WebView w,WebResourceRequest request,WebResourceError error) {
                if (request.isForMainFrame()) status.setText("Falha ao abrir página: "+error.getErrorCode());
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView w,int progress) {
                loading.setProgress(progress);
                loading.setVisibility(progress>=100?View.INVISIBLE:View.VISIBLE);
                if(progress<100) status.setText("Carregando página: "+progress+"%");
            }
        });
        // O menu usa somente a URL HTTP(S) que a WebView identificou no toque longo.
        // Não executa JavaScript nem tenta decodificar blob:/data:.
        web.setOnLongClickListener(v -> {
            WebView.HitTestResult hit = web.getHitTestResult();
            if (hit == null) return false;
            int type = hit.getType();
            if (type != WebView.HitTestResult.SRC_ANCHOR_TYPE
                    && type != WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                    && type != WebView.HitTestResult.IMAGE_TYPE) return false;
            String target = hit.getExtra();
            if (target == null || target.trim().isEmpty()) return false;
            showLinkMenu(target.trim());
            return true;
        });
        web.setDownloadListener((url,agent,disposition,mime,length)->offerDownload(url,agent,length));
        frame.addView(web,new FrameLayout.LayoutParams(-1,-1));
        view.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        status=Ui.text(host,"Digite um endereço ou pesquise na web.",11,colors.muted,false);
        status.setSingleLine(true);status.setEllipsize(TextUtils.TruncateAt.END);
        status.setPadding(dp(7),0,dp(7),0);
        LinearLayout bottomInfo=Ui.horizontal(host);
        bottomInfo.addView(status,new LinearLayout.LayoutParams(0,dp(28),1));
        videosButton=Ui.pill(host,"Vídeos",colors.teal,colors.soft);
        videosButton.setTextSize(12);
        videosButton.setPadding(dp(10),0,dp(10),0);
        videosButton.setMinHeight(dp(28));
        videosButton.setContentDescription("Procurar vídeos nesta página");
        videosButton.setOnClickListener(v->scanPageVideos(true));
        bottomInfo.addView(videosButton,new LinearLayout.LayoutParams(-2,dp(28)));
        view.addView(bottomInfo,Ui.lp(host,-1,28,2));
        if (saved != null) {
            Bundle webState=saved.getBundle("BROWSER_WEB_STATE");
            if (webState != null && web.restoreState(webState)!=null) return;
        }
        navigate(BrowserPrefs.home(host));
    }
    private void showSecurity(String url) {
        securityIcon.removeAllViews();
        boolean https = url!=null&&url.startsWith("https://");
        securityIcon.addView(Ui.icon(host,https?"lock":"alert",https?colors.teal:colors.warn,19),
            new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));
    }
    void navigate(String input) {
        String url=BrowserPrefs.destination(host,input);
        if(url==null) { Toast.makeText(host,"Endereço inválido ou não suportado",Toast.LENGTH_SHORT).show(); return; }
        address.setText(url);showSecurity(url);
        InputMethodManager keyboard=(InputMethodManager)host.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (keyboard!=null) keyboard.hideSoftInputFromWindow(address.getWindowToken(),0);
        address.clearFocus();
        web.loadUrl(url);
    }
    void goBack(){if(web.canGoBack()) web.goBack();}
    void goForward(){if(web.canGoForward()) web.goForward();}
    void reload(){web.reload();}
    void home(){navigate(BrowserPrefs.home(host));}
    boolean canGoBack(){return web.canGoBack();}
    void pause(){web.onPause();}
    void resume(){
        applyPagePreferences(false);
        web.onResume();
    }
    private void applyPagePreferences(boolean forceReload){
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(BrowserPrefs.javascript(host));
        int zoom=BrowserPrefs.textZoom(host);
        if(appliedTextZoom!=zoom){settings.setTextZoom(zoom);appliedTextZoom=zoom;}
        boolean thirdParty=BrowserPrefs.thirdPartyCookies(host);
        if(thirdParty!=appliedThirdPartyCookies){
            appliedThirdPartyCookies=thirdParty;
            CookieManager.getInstance().setAcceptThirdPartyCookies(web,thirdParty);
            forceReload=true;
        }
        boolean desktop=BrowserPrefs.desktop(host);
        if(desktop!=appliedDesktop){
            appliedDesktop=desktop;
            settings.setUseWideViewPort(desktop);
            settings.setLoadWithOverviewMode(desktop);
            settings.setUserAgentString(desktop?DesktopUserAgent.toDesktop(standardUserAgent):standardUserAgent);
            forceReload=true;
        }
        if(forceReload && web.getUrl()!=null)web.reload();
    }
    private void showPageTools(){
        String desktop=BrowserPrefs.desktop(host)?"Desativar modo computador":"Ativar modo computador";
        String current=web.getUrl();
        boolean valid=FavoriteRules.url(current)!=null;
        boolean saved=valid && BrowserPrefs.isFavorite(host,current);
        String bookmarkLabel=saved?"Remover página dos favoritos":"Salvar página nos favoritos";
        String[] options={"Procurar vídeos nesta página","Favoritos",bookmarkLabel,"Localizar na página","Aumentar zoom","Diminuir zoom",desktop,"Abrir em navegador externo","Ajuda com login"};
        new AlertDialog.Builder(host).setTitle("Ferramentas do navegador")
            .setItems(options,(d,which)->{
                switch(which){
                    case 0:scanPageVideos(true);break;
                    case 1:showFavorites();break;
                    case 2:
                        if(!valid){Toast.makeText(host,"Esta página não tem endereço HTTP/HTTPS válido",Toast.LENGTH_LONG).show();break;}
                        if(saved){
                            BrowserPrefs.removeFavorite(host,current);
                            Toast.makeText(host,"Página removida dos favoritos",Toast.LENGTH_SHORT).show();
                        }else if(BrowserPrefs.addFavorite(host,current,web.getTitle())){
                            Toast.makeText(host,"Página salva nos favoritos",Toast.LENGTH_SHORT).show();
                        }else Toast.makeText(host,"Favoritos cheios ou URL inválida",Toast.LENGTH_LONG).show();
                        break;
                    case 3:openFind();break;
                    case 4:if(!web.zoomIn())Toast.makeText(host,"Zoom máximo atingido",Toast.LENGTH_SHORT).show();break;
                    case 5:if(!web.zoomOut())Toast.makeText(host,"Zoom mínimo atingido",Toast.LENGTH_SHORT).show();break;
                    case 6:
                        BrowserPrefs.setDesktop(host,!BrowserPrefs.desktop(host));
                        applyPagePreferences(false);
                        Toast.makeText(host,BrowserPrefs.desktop(host)?"Modo computador solicitado":"Modo celular restaurado",Toast.LENGTH_SHORT).show();
                        break;
                    case 7: openInExternalBrowser(web.getUrl()); break;
                    case 8: showLoginHelp(); break;
                }
            }).setNegativeButton("Fechar",null).show();
    }

    private void offerExternalGoogleLogin(String url) {
        new AlertDialog.Builder(host)
            .setTitle("Login do Google no navegador")
            .setMessage("O Google não permite login OAuth dentro de uma WebView. Para proteger sua conta, abra a autenticação em um navegador compatível.\n\nA sessão do navegador externo é independente e não entra automaticamente no Baixa Fácil.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Abrir navegador",(d,w)->openInExternalBrowser(url))
            .show();
    }

    private void showLoginHelp() {
        new AlertDialog.Builder(host)
            .setTitle("Ajuda com login")
            .setMessage("Google: logins OAuth não são permitidos dentro de WebViews. Abra no navegador externo.\n\nFacebook: desafios de verificação podem falhar na WebView. Você pode tentar ativar cookies de terceiros em Configurações > Compatibilidade de login, por sua conta e risco, ou abrir o site no navegador externo.\n\nO navegador externo não compartilha automaticamente cookies com o Baixa Fácil. Não é possível garantir que o login externo permita baixar mídias autenticadas no app.")
            .setNegativeButton("Fechar",null)
            .setPositiveButton("Abrir navegador",(d,w)->openInExternalBrowser(web.getUrl()))
            .show();
    }

    private void openInExternalBrowser(String url) {
        if(url==null || !url.equals(UrlTools.extract(url))){
            Toast.makeText(host,"Nenhum endereço web válido para abrir",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent open=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        open.addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            Intent chooser=Intent.createChooser(open,"Escolha um navegador");
            // Impede que a própria Activity de Baixa Fácil reapareça como destino do login.
            chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS,new android.os.Parcelable[]{
                new ComponentName(host,MainActivity.class)
            });
            host.startActivity(chooser);
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(host,"Instale um navegador compatível (Chrome ou Firefox) para entrar no site",Toast.LENGTH_LONG).show();
        }
    }

    private void updateVideosButton() {
        if(videosButton==null)return;
        int count;
        synchronized(videoUrls){count=videoUrls.size();}
        videosButton.setText(count>0?"Vídeos ("+count+")":"Vídeos");
    }

    private void scanPageVideos(boolean showResult) {
        String page=web.getUrl();
        if (AuthNavigation.isSensitiveLoginPage(page)) {
            if (showResult) Toast.makeText(host,"Busca de vídeos indisponível durante login ou verificação de conta",Toast.LENGTH_LONG).show();
            return;
        }
        if(!VideoLinks.allowedHttpUrl(page)) {
            if(showResult)Toast.makeText(host,"Abra uma página HTTP/HTTPS para procurar vídeos",Toast.LENGTH_LONG).show();
            return;
        }
        int generation=scanGeneration;
        web.evaluateJavascript(VIDEO_SCAN_JS, value->{
            if(generation!=scanGeneration || !page.equals(web.getUrl()))return;
            try {
                Object decoded=new JSONTokener(value).nextValue();
                JSONObject result=new JSONObject(decoded.toString());
                mediaElements=result.optInt("players",0);
                unsupportedMedia=result.optInt("unsupported",0);
                JSONArray found=result.optJSONArray("media");
                if(found!=null)for(int i=0;i<found.length();i++) {
                    JSONObject obj=found.optJSONObject(i);
                    if(obj==null)continue;
                    addVideoCandidate(obj.optString("url"),obj.optString("kind"),obj.optString("mime"));
                }
            } catch(Exception ignored) {
                if(showResult)Toast.makeText(host,"Não foi possível analisar esta página",Toast.LENGTH_SHORT).show();
            }
            updateVideosButton();
            if(showResult)showDetectedVideos();
        });
    }

    private void addVideoCandidate(String url,String kind,String mime) {
        if(!VideoLinks.isVideoCandidate(url,kind,mime))return;
        synchronized(videoUrls){
            if(!videoUrls.containsKey(url) && videoUrls.size()<VideoLinks.MAX_VIDEO_URLS)
                videoUrls.put(url,VideoLinks.label(url));
        }
    }

    private void showDetectedVideos() {
        ArrayList<String> urls;
        ArrayList<String> titles;
        synchronized(videoUrls) {
            urls=new ArrayList<>(videoUrls.keySet());
            titles=new ArrayList<>(videoUrls.values());
        }
        if(urls.isEmpty()) {
            String reason=(mediaElements>0 || unsupportedMedia>0)
                ? "Foi encontrado um player, mas nenhum endereço de vídeo direto compatível. Muitos vídeos do Facebook usam blob:, HLS/DASH, sessões restritas ou proteção; o app não converte nem contorna essas tecnologias.\n\nSe houver um link direto de vídeo, tente abrir o vídeo e procurar novamente."
                : "Nenhum vídeo direto identificado. Em páginas com conteúdo carregado ao rolar, abra ou reproduza o vídeo e toque em Vídeos novamente.";
            new AlertDialog.Builder(host).setTitle("Vídeos na página")
                .setMessage(reason).setPositiveButton("Entendi",null).show();
            return;
        }
        CharSequence[] items=new CharSequence[urls.size()];
        for(int i=0;i<urls.size();i++){
            String domain="";
            try{domain=new URI(urls.get(i)).getHost();}catch(Exception ignored){}
            items[i]=(i+1)+". "+titles.get(i)+"\n"+domain;
        }
        new AlertDialog.Builder(host).setTitle("Vídeos encontrados ("+urls.size()+")")
            .setItems(items,(dialog,index)->{
                String url=urls.get(index);
                new AlertDialog.Builder(host).setTitle("Vídeo "+(index+1))
                    .setMessage("Endereço indicado pela página. O download só funcionará se o servidor entregar um arquivo de vídeo direto e autorizar o acesso.\n\n"+VideoLinks.label(url))
                    .setPositiveButton("Baixar",(d,w)->offerDownload(url,web.getSettings().getUserAgentString(),-1))
                    .setNeutralButton("Copiar link",(d,w)->copyLink(url))
                    .setNegativeButton("Voltar",(d,w)->showDetectedVideos())
                    .show();
            }).setNegativeButton("Fechar",null).show();
    }

    private void showFavorites(){
        List<BrowserPrefs.Favorite> favorites=BrowserPrefs.favorites(host);
        if(favorites.isEmpty()){
            new AlertDialog.Builder(host).setTitle("Favoritos")
                .setMessage("Nenhum site salvo. Abra uma página e use ⋮ > Salvar página nos favoritos.")
                .setNeutralButton("Gerenciar",(d,w)->host.openFavoriteSettings())
                .setPositiveButton("Fechar",null).show();
            return;
        }
        String[] names=new String[favorites.size()];
        for(int i=0;i<favorites.size();i++) names[i]=favorites.get(i).title+"\n"+favorites.get(i).url;
        new AlertDialog.Builder(host).setTitle("Favoritos ("+favorites.size()+")")
            .setItems(names,(d,w)->navigate(favorites.get(w).url))
            .setNeutralButton("Gerenciar",(d,w)->host.openFavoriteSettings())
            .setNegativeButton("Fechar",null).show();
    }
    private void openFind(){
        findBar.setVisibility(View.VISIBLE);
        findInput.requestFocus();
        if(findInput.length()>0)web.findAllAsync(findInput.getText().toString());
        InputMethodManager imm=(InputMethodManager)host.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(imm!=null)imm.showSoftInput(findInput,InputMethodManager.SHOW_IMPLICIT);
    }
    private void closeFind(){
        web.clearMatches();
        findBar.setVisibility(View.GONE);
        findInput.setText("");
        findInput.clearFocus();
        InputMethodManager imm=(InputMethodManager)host.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if(imm!=null)imm.hideSoftInputFromWindow(findInput.getWindowToken(),0);
    }
    void saveState(Bundle output){Bundle b=new Bundle();web.saveState(b);output.putBundle("BROWSER_WEB_STATE",b);}
    void destroy(){ web.setDownloadListener(null);web.stopLoading(); ViewGroup parent=(ViewGroup)web.getParent();if(parent!=null)parent.removeView(web);web.destroy(); }
    void clearLocalData(){closeFind();web.clearCache(true);web.clearHistory();navigate(BrowserPrefs.home(host));}
    private void copyAddress(){
        String url=address.getText().toString().trim();
        if(url.isEmpty()){Toast.makeText(host,"Nenhum endereço para copiar",Toast.LENGTH_SHORT).show();return;}
        ClipboardManager cb=(ClipboardManager)host.getSystemService(Activity.CLIPBOARD_SERVICE);
        if(cb!=null){cb.setPrimaryClip(ClipData.newPlainText("Endereço",url));Toast.makeText(host,"Endereço copiado",Toast.LENGTH_SHORT).show();}
    }
    private void showHistory() {
        List<String> items=BrowserPrefs.history(host);
        if(items.isEmpty()){Toast.makeText(host,"Histórico vazio",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(host).setTitle("Histórico recente")
            .setItems(items.toArray(new CharSequence[0]),(d,which)->navigate(items.get(which)))
            .setNegativeButton("Fechar",null).show();
    }
    private void showLinkMenu(String url) {
        boolean supported = url.equals(UrlTools.extract(url));
        String[] actions = supported
                ? new String[]{"Abrir link", "Copiar link", "Baixar link", "Compartilhar link", "Salvar nos favoritos"}
                : new String[]{"Copiar endereço", "Formato não compatível para download"};
        new AlertDialog.Builder(host).setTitle("Ações do link")
                .setItems(actions,(dialog, which)->{
                    if (!supported) {
                        if (which == 0) copyLink(url);
                        else Toast.makeText(host,"Download próprio aceita apenas HTTP/HTTPS",Toast.LENGTH_LONG).show();
                        return;
                    }
                    switch (which) {
                        case 0: navigate(url); break;
                        case 1: copyLink(url); break;
                        case 2: offerDownload(url,web.getSettings().getUserAgentString(),-1); break;
                        case 3:
                            Intent share=new Intent(Intent.ACTION_SEND);
                            share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,url);
                            host.startActivity(Intent.createChooser(share,"Compartilhar link"));
                            break;
                        case 4:
                            if(BrowserPrefs.addFavorite(host,url,null))
                                Toast.makeText(host,"Link salvo nos favoritos",Toast.LENGTH_SHORT).show();
                            else Toast.makeText(host,"Favoritos cheios ou URL inválida",Toast.LENGTH_LONG).show();
                            break;
                    }
                }).setNegativeButton("Fechar",null).show();
    }
    private void copyLink(String url) {
        ClipboardManager clipboard=(ClipboardManager)host.getSystemService(Activity.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Link",url));
        Toast.makeText(host,"Link copiado",Toast.LENGTH_SHORT).show();
    }

    private void offerDownload(String url,String agent,long length) {
        if(url==null || !url.equals(UrlTools.extract(url))) {
            new AlertDialog.Builder(host).setTitle("Download não compatível")
                .setMessage("O mecanismo próprio aceita somente links HTTP/HTTPS. blob:, data: e arquivos internos não são suportados.")
                .setPositiveButton("OK",null).show();
            return;
        }
        String domain;
        try { domain=new URI(url).getHost(); } catch(Exception ex) { domain="servidor"; }
        String summary="Origem: "+domain+"\n"+(length>=0?"Tamanho informado: "+(length/1024)+" KB\n":"Tamanho desconhecido\n")+
            "Será salvo em Download/. Cookies de sessão não serão armazenados no histórico nem enviados a outros domínios.";
        if (url.startsWith("http://")) summary+="\n\nA conexão HTTP não é criptografada.";
        new AlertDialog.Builder(host).setTitle("Baixar com Baixa Fácil?").setMessage(summary)
            .setNegativeButton("Cancelar",null).setPositiveButton("Baixar",(d,w)->startDownload(url,agent)).show();
    }
    private void startDownload(String url,String agent){
        String restriction=DownloadRestrictions.blocked(host);
        if(restriction!=null){
            new AlertDialog.Builder(host).setTitle("Download aguardando")
                .setMessage(restriction+"\n\nAltere as opções em Configurações > Downloads.")
                .setPositiveButton("Entendi",null).show();
            return;
        }
        String id=DownloadStore.create(host,url);
        String cookies=CookieManager.getInstance().getCookie(url);
        String referer=web.getUrl();
        if(!TransferHttp.sameOrigin(url,referer))referer=null;
        boolean started=TransferStarter.enqueue(host,id,url,cookies,agent,referer);
        if(started){
            Toast.makeText(host,"Download adicionado à fila",Toast.LENGTH_SHORT).show();
            host.showDownloads();
        }else Toast.makeText(host,"Não foi possível iniciar o download",Toast.LENGTH_LONG).show();
    }
}
