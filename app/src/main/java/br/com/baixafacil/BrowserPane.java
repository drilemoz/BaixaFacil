package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.net.Uri;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.app.ActivityManager;
import android.util.Base64;
import android.content.pm.PackageInfo;
import android.webkit.WebResourceResponse;
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
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebViewRenderProcess;
import android.webkit.WebViewRenderProcessClient;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private ListPopupWindow suggestionsPopup;
    private boolean suppressSuggestions;
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
    private WebView loginPopup;
    private AlertDialog loginPopupDialog;
    private String lastNavigationProblem = "Nenhuma falha registrada nesta sessão.";
    private int lastHttpStatus;
    private boolean googleLoginDialogVisible;
    private TextView videosButton;
    private final LinkedHashMap<String,String> videoUrls = new LinkedHashMap<>();
    private final LinkedHashMap<String,PreviewMeta> videoPreviewMeta = new LinkedHashMap<>();
    private int mediaElements;
    private int unsupportedMedia;
    private int scanGeneration;
    private Runnable pendingMediaScan;
    private volatile boolean pageActive=true;
    private volatile boolean memoryPressure;
    private long lastAutomaticScanUptime;
    private volatile boolean lightweightPage;
    private boolean rendererUnresponsive;
    private int lastProgressBucket=-1;
    private final AdBlockController adBlock;

    // Executado somente quando a pessoa pede a busca ou após uma página terminar.
    // Não injeta bridge JS nem lê cookies, tokens de sessão ou dados de outros frames.
    private static final String VIDEO_SCAN_JS =
        "(function(){" +
        "try {" +
        "var out={media:[],players:0,unsupported:0};" +
        "var attemptedFrames=0;" +
        "function safeText(x){return typeof x==='string'?x.replace(/\\s+/g,' ').trim().slice(0,105):'';}" +
        "function bounds(el){" +
        "if(!el)return {x:0,y:0,w:0,h:0,visible:false};" +
        "try {" +
        "var r=el.getBoundingClientRect();" +
        "var w=Math.max(0,Math.round(r.width)),h=Math.max(0,Math.round(r.height));" +
        "return {x:Math.round(r.left),y:Math.round(r.top),w:w,h:h," +
        "visible:w>25&&h>25&&r.bottom>0&&r.right>0&&r.top<innerHeight&&r.left<innerWidth};" +
        "}catch(e){return {x:0,y:0,w:0,h:0,visible:false};}" +
        "}" +
        "function titleFor(v){" +
        "if(!v)return '';" +
        "var t=safeText(v.getAttribute('aria-label')||v.getAttribute('title')||'');" +
        "if(t)return t;" +
        "try{" +
        "var block=v.closest('article,[role=\"article\"],[data-pagelet]');" +
        "if(block){" +
        "var heading=block.querySelector('[data-ad-preview=\"message\"],h1,h2,h3,[role=\"heading\"]');" +
        "if(heading)return safeText(heading.textContent);" +
        "}" +
        "}catch(e){}" +
        "return '';" +
        "}" +
        "function frameFor(v,rect){" +
        "if(!v||!rect.visible||attemptedFrames>=3||v.readyState<2||!v.videoWidth||!v.videoHeight)return '';" +
        "attemptedFrames++;" +
        "try {" +
        "var c=document.createElement('canvas');c.width=96;c.height=70;" +
        "c.getContext('2d').drawImage(v,0,0,96,70);" +
        "var data=c.toDataURL('image/jpeg',0.52);" +
        "return data.length<18000?data:'';" +
        "}catch(e){return '';}" +
        "}" +
        "function put(u,kind,mime,v){" +
        "if(!u||typeof u!=='string')return;" +
        "if(/^(blob:|data:|mediasource:)/i.test(u)){out.unsupported++;return;}" +
        "if(out.media.length>=40||u.length>4096)return;" +
        "var r=bounds(v), playing=!!(v&&!v.paused&&!v.ended&&v.readyState>=2);" +
        "out.media.push({url:u,kind:kind,mime:mime||'',title:titleFor(v)," +
        "playing:playing,visible:r.visible,x:r.x,y:r.y,w:r.w,h:r.h,frame:frameFor(v,r)});" +
        "}" +
        "var players=document.querySelectorAll('video');out.players=players.length;" +
        "for(var i=0;i<players.length&&i<25;i++){" +
        "var v=players[i];" +
        "put(v.currentSrc||v.src,'video',v.getAttribute('type'),v);" +
        "var src=v.querySelectorAll('source');" +
        "for(var j=0;j<src.length&&j<5;j++)put(src[j].src,'source',src[j].type,v);" +
        "}" +
        "var sources=document.querySelectorAll('source[type^=\"video/\"]');" +
        "for(var k=0;k<sources.length&&k<20;k++){" +
        "var node=sources[k];" +
        "var owner=node.parentElement&&node.parentElement.tagName==='VIDEO'?node.parentElement:null;" +
        "if(!owner)put(node.src,'source',node.type,null);" +
        "}" +
        "var links=document.querySelectorAll('a[href]');" +
        "for(var l=0;l<links.length&&l<100;l++){" +
        "var a=links[l];" +
        "if(/\\.(mp4|m4v|webm|mov|ogv|3gp)(\\?|#|$)/i.test(a.href))put(a.href,'link','',null);" +
        "}" +
        "return JSON.stringify(out);" +
        "} catch(e){return JSON.stringify({media:[],players:0,unsupported:0});}" +
        "})()";

    private static final class PreviewMeta {
        final String title,frame;
        final int x,y,width,height;
        final boolean playing,visible;
        PreviewMeta(String title,int x,int y,int width,int height,boolean playing,boolean visible,String frame){
            this.title=VideoPresentation.cleanTitle(title);
            this.x=x;this.y=y;this.width=width;this.height=height;
            this.playing=playing;this.visible=visible;
            this.frame=frame==null?"":frame.length()<18000?frame:"";
        }
        boolean hasBounds(){return width>25 && height>25 && visible;}
        boolean hasInfo(){return hasBounds()||!title.isEmpty()||!frame.isEmpty()||playing;}
        int rank(){return playing&&visible?3:visible?2:playing?1:0;}
    }

    BrowserPane(MainActivity host, Ui.Palette colors, Bundle saved) {
        this.host = host;
        this.colors = colors;
        adBlock = new AdBlockController(host);
        view = Ui.vertical(host);
        view.setPadding(0,dp(4),0,0);
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
        address.setOnFocusChangeListener((v,focused)->{
            if(focused)updateAddressSuggestions();
            else dismissAddressSuggestions();
        });
        address.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                if(!suppressSuggestions)updateAddressSuggestions();
            }
            @Override public void afterTextChanged(Editable e){}
        });
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
        LinearLayout.LayoutParams topP=Ui.lp(host,-1,48,0);
        topP.leftMargin=dp(12);topP.rightMargin=dp(12);
        view.addView(top,topP);
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
        LinearLayout.LayoutParams findP=Ui.lp(host,-1,44,5);
        findP.leftMargin=dp(12);findP.rightMargin=dp(12);
        view.addView(findBar,findP);

        loading=new ProgressBar(host,null,android.R.attr.progressBarStyleHorizontal);
        loading.setMax(100);
        loading.setProgressTintList(android.content.res.ColorStateList.valueOf(colors.teal));
        loading.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(colors.background));
        LinearLayout.LayoutParams loadP=Ui.lp(host,-1,3,4);
        loadP.leftMargin=dp(12);loadP.rightMargin=dp(12);
        view.addView(loading,loadP);

        FrameLayout frame=new FrameLayout(host);
        frame.setBackgroundColor(colors.background);
        frame.setClipToPadding(false);
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
        // Habilita popups somente com gesto do usuário; o onCreateWindow controla a janela filha.
        options.setSupportMultipleWindows(true);
        options.setSafeBrowsingEnabled(true);
        WebKitCompat.optimize(options);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,appliedThirdPartyCookies);
        web.setFindListener((active,total,finished)->{
            if(finished && findCount!=null && findBar.getVisibility()==View.VISIBLE)
                findCount.setText(total==0?"0/0":(active+1)+"/"+total);
        });
        // Em feeds/Reels, a navegação pode trocar vídeos sem abrir outra página.
        // Varredura após a rolagem, com debounce, sem rede nem loop de polling.
        web.setOnScrollChangeListener((v,sx,sy,oldX,oldY)->{
            if(sy!=oldY && VideoPrefs.autoScan(host))scheduleMediaScan(900);
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest request) {
                String target=request.getUrl().toString();
                if (request.isForMainFrame() && AuthNavigation.isGoogleOAuth(target)) {
                    offerExternalGoogleLogin(target);
                    return true;
                }
                if (target.equals(UrlTools.extract(target))) return false;
                if (!request.isForMainFrame() && LoginWindowPolicy.isSafeInternalFrame(target)) return false;
                if(request.isForMainFrame()) status.setText("Endereço bloqueado: apenas HTTP e HTTPS.");
                return true;
            }
            @Override public void onPageStarted(WebView w,String url,android.graphics.Bitmap icon) {
                adBlock.setPage(url);
                if (AuthNavigation.isGoogleOAuth(url)) {
                    w.stopLoading();
                    offerExternalGoogleLogin(url);
                    return;
                }
                lastHttpStatus=0;
                lightweightPage=BrowserLoadPolicy.lightMode(url,BrowserPrefs.chatGptLight(host));
                lastProgressBucket=-1;
                lastAutomaticScanUptime=0;
                rendererUnresponsive=false;
                scanGeneration++;
                if(pendingMediaScan!=null)web.removeCallbacks(pendingMediaScan);
                pendingMediaScan=null;
                synchronized(videoUrls){ videoUrls.clear(); }
                synchronized(videoPreviewMeta){ videoPreviewMeta.clear(); }
                mediaElements=0;
                unsupportedMedia=0;
                updateVideosButton();
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest request) {
                // Em ChatGPT dispensar TODO processamento opcional em subrecursos: chat pesado e dinâmico.
                // A WebView segue carregando normalmente; não alteramos respostas, cookies ou TLS.
                if (!pageActive || lightweightPage || memoryPressure) return null;
                // Não bloqueia documento principal, POST ou esquemas internos; evita quebrar login.
                if (!request.isForMainFrame() && "GET".equalsIgnoreCase(request.getMethod())
                        && adBlock.shouldBlock(request.getUrl().toString())) {
                    return new WebResourceResponse("text/plain", "UTF-8",
                            new java.io.ByteArrayInputStream(new byte[0]));
                }
                // Observação de vídeo ocorre apenas quando não foi bloqueado pelo filtro.
                if (!request.isForMainFrame() && "GET".equalsIgnoreCase(request.getMethod())) {
                    String candidate=request.getUrl().toString();
                    if(MediaSourcePolicy.isDirect(candidate) || MediaSourcePolicy.isPlaylist(candidate)) {
                        boolean added=false;
                        synchronized(videoUrls) {
                            if(!videoUrls.containsKey(candidate) && videoUrls.size()<VideoLinks.MAX_VIDEO_URLS) {
                                videoUrls.put(candidate,MediaSourcePolicy.isPlaylist(candidate)?"stream":"direct");
                                added=true;
                            }
                        }
                        if(added)host.runOnUiThread(()->{
                            updateVideosButton();
                            if(web!=null && web.getUrl()!=null && web.getUrl().contains("facebook.com"))
                                scheduleMediaScan(950);
                        });
                    }
                }
                return null;
            }
            @Override public void onPageFinished(WebView w,String url) {
                if(!lightweightPage) applyCosmeticFilters(url);
                if (url!=null && url.equals(UrlTools.extract(url))) {
                    setAddressFromPage(url);
                    showSecurity(url);
                    BrowserPrefs.remember(host,url);
                }
                if (findBar.getVisibility()==View.VISIBLE && findInput.length()>0)
                    web.findAllAsync(findInput.getText().toString());
                status.setText(lastHttpStatus>=400 ? "Erro HTTP "+lastHttpStatus+". Consulte ⋮ > Diagnóstico de login."
                    : url!=null&&url.startsWith("http://") ? "Atenção: conexão HTTP sem criptografia."
                    : AuthNavigation.isFacebookCheckpoint(url) ? "Verificação do Facebook: tente abrir no navegador externo pelo menu ⋮."
                    : "Página pronta. Toque num arquivo para baixar.");
                // O Facebook e outros sites podem carregar o player depois do HTML.
                scheduleMediaScan(1300);
            }
            @Override public boolean onRenderProcessGone(WebView w,RenderProcessGoneDetail detail) {
                lastNavigationProblem="O processo da WebView foi encerrado; o navegador será recriado.";
                AppEventLog.error(host,"Navegador","Renderizador WebView encerrado pelo Android.");
                rendererUnresponsive=false;
                ViewGroup parent=(ViewGroup)w.getParent();
                if(parent!=null)parent.removeView(w);
                w.destroy();
                web=null;
                if(loginPopupDialog!=null)loginPopupDialog.dismiss();
                host.getWindow().getDecorView().post(()->{
                    Toast.makeText(host,"A página parou. Reabrindo o navegador com segurança.",Toast.LENGTH_LONG).show();
                    host.recreate();
                });
                return true;
            }
            @Override public void onReceivedError(WebView w,WebResourceRequest request,WebResourceError error) {
                if (request.isForMainFrame()) {
                    lastNavigationProblem="Erro de rede da WebView: "+error.getErrorCode();
                    AppEventLog.error(host,"Navegador","Falha de carregamento da página: código "+error.getErrorCode());
                    status.setText("Falha ao abrir página ("+error.getErrorCode()+"). Consulte ⋮ > Diagnóstico de login.");
                }
            }
            @Override public void onReceivedHttpError(WebView w,WebResourceRequest request,WebResourceResponse response) {
                if (request.isForMainFrame()) {
                    lastHttpStatus=response.getStatusCode();
                    lastNavigationProblem="Resposta HTTP da página: "+response.getStatusCode();
                    if (response.getStatusCode() >= 400) {
                        AppEventLog.warn(host,"Navegador","Resposta HTTP "+response.getStatusCode()+" da página principal.");
                        status.setText("Site retornou HTTP "+response.getStatusCode()+". Consulte ⋮ > Diagnóstico de login.");
                    }
                }
            }
        });
        // API 29+: observe travamentos reais do processo de renderização sem matar a sessão.
        web.setWebViewRenderProcessClient(new WebViewRenderProcessClient() {
            @Override public void onRenderProcessUnresponsive(WebView w,WebViewRenderProcess renderer) {
                host.runOnUiThread(()->{
                    if(web!=w || rendererUnresponsive)return;
                    rendererUnresponsive=true;
                    cancelMediaScan();
                    lastNavigationProblem="A WebView ficou temporariamente sem responder; verifique memória do aparelho e atualize o componente.";
                    AppEventLog.warn(host,"Navegador","Renderizador WebView sem resposta durante a navegação.");
                    status.setText("Página pesada: a WebView não responde. Use ⋮ para abrir externamente.");
                });
            }
            @Override public void onRenderProcessResponsive(WebView w,WebViewRenderProcess renderer) {
                host.runOnUiThread(()->{
                    if(web!=w)return;
                    if(rendererUnresponsive)status.setText("A página voltou a responder.");
                    rendererUnresponsive=false;
                });
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onCreateWindow(WebView source, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return createLoginWindow(isUserGesture,resultMsg);
            }
            @Override public void onProgressChanged(WebView w,int progress) {
                // O progresso pode disparar dezenas de vezes em aplicativos SPA; evite refazer texto toda vez.
                int bucket=progress/10;
                if(bucket==lastProgressBucket && progress!=100)return;
                lastProgressBucket=bucket;
                loading.setProgress(progress);
                loading.setVisibility(progress>=100?View.INVISIBLE:View.VISIBLE);
                if(progress<100) status.setText("Carregando página: "+(bucket*10)+"%");
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
        videosButton=Ui.pill(host,"Vídeos",colors.teal,colors.soft);
        videosButton.setTextSize(12);
        videosButton.setPadding(dp(12),0,dp(12),0);
        videosButton.setMinHeight(dp(40));
        videosButton.setVisibility(View.GONE);
        videosButton.setContentDescription("Abrir vídeos encontrados nesta página");
        videosButton.setOnClickListener(v->showDetectedVideos());
        FrameLayout.LayoutParams videoP=new FrameLayout.LayoutParams(-2,dp(40),Gravity.END|Gravity.BOTTOM);
        videoP.rightMargin=dp(14);videoP.bottomMargin=dp(14);
        frame.addView(videosButton,videoP);
        view.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        status=Ui.text(host,"Digite um endereço ou pesquise na web.",11,colors.muted,false);
        status.setSingleLine(true);status.setEllipsize(TextUtils.TruncateAt.END);
        status.setPadding(dp(19),0,dp(19),0);
        LinearLayout bottomInfo=Ui.horizontal(host);
        bottomInfo.addView(status,new LinearLayout.LayoutParams(0,dp(28),1));
        LinearLayout.LayoutParams bottomP=Ui.lp(host,-1,28,2);
        bottomP.leftMargin=0;bottomP.rightMargin=0;
        view.addView(bottomInfo,bottomP);
        if(SessionPrefs.shouldClearAtStart(host)) {
            // Não restaura estado WebView antes da limpeza assíncrona de cookies e storage.
            SessionPrefs.clearForNewSession(()->{
                if(web!=null)navigate(BrowserPrefs.home(host));
            });
            return;
        }
        if (saved != null) {
            Bundle webState=saved.getBundle("BROWSER_WEB_STATE");
            if (webState != null && web.restoreState(webState)!=null) return;
        }
        navigate(BrowserPrefs.home(host));
    }
    private void dismissAddressSuggestions(){
        if(suggestionsPopup!=null && suggestionsPopup.isShowing())suggestionsPopup.dismiss();
    }
    private void setAddressFromPage(String url){
        if(address.hasFocus())return;
        suppressSuggestions=true;
        address.setText(url);
        suppressSuggestions=false;
    }
    private void updateAddressSuggestions(){
        if(address==null||!address.hasFocus()||suppressSuggestions)return;
        String term=address.getText().toString().trim();
        if(term.isEmpty()) {dismissAddressSuggestions();return;}
        java.util.List<BrowserSuggestions.Candidate> candidates=new ArrayList<>();
        for(BrowserPrefs.Favorite favorite:BrowserPrefs.favorites(host))
            candidates.add(new BrowserSuggestions.Candidate(favorite.url,favorite.title,true));
        for(String previous:BrowserPrefs.history(host))
            candidates.add(new BrowserSuggestions.Candidate(previous,"",false));
        java.util.List<BrowserSuggestions.Item> items=BrowserSuggestions.suggest(term,candidates);
        if(items.isEmpty()){dismissAddressSuggestions();return;}
        if(suggestionsPopup==null){
            suggestionsPopup=new ListPopupWindow(host);
            suggestionsPopup.setAnchorView(address);
            suggestionsPopup.setModal(false);
            suggestionsPopup.setBackgroundDrawable(Ui.shape(host,colors.surface,14));
            suggestionsPopup.setOnItemClickListener((parent,view,index,id)->{
                BrowserSuggestions.Item selected=(BrowserSuggestions.Item)parent.getItemAtPosition(index);
                dismissAddressSuggestions();
                navigate(selected.url);
            });
        }
        suggestionsPopup.setAdapter(new BaseAdapter(){
            @Override public int getCount(){return items.size();}
            @Override public Object getItem(int position){return items.get(position);}
            @Override public long getItemId(int position){return position;}
            @Override public android.view.View getView(int position,android.view.View convert,android.view.ViewGroup parent){
                BrowserSuggestions.Item item=items.get(position);
                LinearLayout row=Ui.horizontal(host);
                row.setPadding(dp(12),dp(8),dp(10),dp(8));
                row.addView(Ui.icon(host,item.favorite?"bookmark":"globe",colors.teal,20),
                    new LinearLayout.LayoutParams(dp(24),dp(24)));
                LinearLayout labels=Ui.vertical(host);
                TextView name=Ui.text(host,item.title,14,colors.text,true);
                name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
                labels.addView(name);
                labels.addView(Ui.text(host,item.host+(item.favorite?" • Favorito":" • Histórico"),11,colors.muted,false));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(10);
                row.addView(labels,lp);
                row.setMinimumHeight(dp(52));
                return row;
            }
        });
        suggestionsPopup.setWidth(Math.min(host.getResources().getDisplayMetrics().widthPixels-dp(24),dp(450)));
        suggestionsPopup.setHeight(-2);
        suggestionsPopup.show();
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
        dismissAddressSuggestions();
        suppressSuggestions=true;address.setText(url);suppressSuggestions=false;
        showSecurity(url);
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
    void pause(){
        pageActive=false;
        cancelMediaScan();
        if(web!=null){
            web.onPause();
            // API global; este app mantém uma única WebView principal e fecha popups ao pausar.
            web.pauseTimers();
        }
        // Persiste cookies já aceitos após a navegação, sem armazená-los no histórico do app.
        CookieManager.getInstance().flush();
    }
    void resume(){
        if(web==null)return;
        pageActive=true;
        web.resumeTimers();
        web.onResume();
        adBlock.loadPreferences();
        lightweightPage=BrowserLoadPolicy.lightMode(web.getUrl(),BrowserPrefs.chatGptLight(host));
        if(lightweightPage)cancelMediaScan();
        refreshMemoryPressure();
        applyPagePreferences(false);
        updateVideosButton();
    }
    private void cancelMediaScan(){
        if(pendingMediaScan!=null){
            if(web!=null)web.removeCallbacks(pendingMediaScan);
            pendingMediaScan=null;
        }
    }
    private void refreshMemoryPressure(){
        ActivityManager am=(ActivityManager)host.getSystemService(Activity.ACTIVITY_SERVICE);
        if(am==null)return;
        ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);
        memoryPressure=!BrowserPerformancePolicy.canResumeOptionalWork(info.lowMemory,info.availMem,info.threshold);
    }
    void onMemoryPressure(int level){
        if(!BrowserPerformancePolicy.seriousMemoryPressure(level))return;
        memoryPressure=true;
        cancelMediaScan();
        // Miniaturas de vídeos ficam somente na sessão, são descartáveis e podem ser pesadas.
        synchronized(videoPreviewMeta){videoPreviewMeta.clear();}
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
    private static final String AD_CSS =
        "ins.adsbygoogle,[id^='div-gpt-ad'],[id^='google_ads_'],[data-ad-slot],"
        +"[data-google-query-id],.advertisement,.advertising-banner,.sponsored-banner,"
        +".ad-container[data-ad],.ad-banner[data-ad],.adsbox[data-ad]"
        +"{display:none!important;visibility:hidden!important;}";
    private void applyCosmeticFilters(String url){
        if(web==null || lightweightPage || !BrowserPrefs.javascript(host)||!adBlock.shouldHideCosmetic(url))return;
        // CSS estático local: não lê formulário, cookies, DOM pessoal nem elementos de login.
        String script="(function(){try{var id='bf-ad-css';if(document.getElementById(id))return;"
            +"var s=document.createElement('style');s.id=id;s.textContent=\""+AD_CSS+"\";"
            +"(document.head||document.documentElement).appendChild(s);}catch(e){}})();";
        web.evaluateJavascript(script,null);
    }
    private void showAdBlockMenu(){
        int mode=adBlock.mode();
        String[] names={"Desligado","Leve","Forte","Extremo"};
        String page=web==null?null:web.getUrl();
        String summary="Proteção: "+names[mode]+" • "+adBlock.ruleCount()+" regras locais\n"
            +"Bloqueios nesta sessão: "+AdBlockController.sessionBlocked()+"\n"
            +"Neste site: "+(adBlock.isException(page)?"desativado":"ativo");
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Abrir configurações","Intensidade, listas e rastreadores","settings",host::openSettings));
        rows.add(AppMenus.Entry.item(adBlock.isException(page)?"Ativar neste site":"Desativar neste site",
            "Alterar apenas o domínio aberto","shield",()->{
                if(AdBlockRules.host(page)==null)return;
                adBlock.setException(page,!adBlock.isException(page));web.reload();
            }));
        AppMenus.show(host,"Bloqueador de anúncios",summary,rows);
    }
    private void showPageTools(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Vídeos e mídia","Vídeos encontrados, reprodução e opções","play",this::showVideoTools));
        rows.add(AppMenus.Entry.item("Favoritos","Abrir ou salvar sites","bookmark",this::showFavoriteTools));
        rows.add(AppMenus.Entry.item("Ferramentas da página","Localizar, zoom e modo computador","search",this::showDisplayTools));
        if(BrowserLoadPolicy.isChatGpt(web.getUrl()))
            rows.add(AppMenus.Entry.item("Abrir ChatGPT no Chrome ou navegador","Alternativa se a WebView estiver lenta","globe",()->openInExternalBrowser(web.getUrl())));
        rows.add(AppMenus.Entry.item("Proteção e privacidade","Bloqueador e exceções do site","shield",this::showProtectionTools));
        rows.add(AppMenus.Entry.item("Login e diagnóstico","Compatibilidade, erros e ajuda","lock",this::showLoginTools));
        AppMenus.show(host,"Ferramentas do navegador","Escolha uma área para configurar ou utilizar.",rows);
    }
    private void showVideoTools(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Vídeos encontrados","Abrir a lista organizada por formato","play",this::showDetectedVideos));
        rows.add(AppMenus.Entry.item("Procurar nesta página","Atualizar a detecção de vídeos","search",()->scanPageVideos(true)));
        rows.add(AppMenus.Entry.item("Configurações de vídeos","Detecção, streaming e arquivos diretos","settings",host::openVideoSettings));
        AppMenus.show(host,"Vídeos e mídia","Somente conteúdos disponíveis na página; sem contornar DRM ou login.",rows);
    }
    private void showFavoriteTools(){
        String current=web.getUrl();
        boolean valid=FavoriteRules.url(current)!=null;
        boolean saved=valid && BrowserPrefs.isFavorite(host,current);
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Ver favoritos","Sites salvos no Baixa Fácil","bookmark",this::showFavorites));
        rows.add(AppMenus.Entry.item("Gerenciar favoritos","Adicionar, editar ou excluir sites","settings",host::openFavoriteSettings));
        rows.add(AppMenus.Entry.item(saved?"Remover página":"Salvar esta página",
            saved?"Retirar da lista de favoritos":"Guardar este endereço nos favoritos","bookmark",()->{
                if(!valid){Toast.makeText(host,"Esta página não pode ser salva como favorito",Toast.LENGTH_LONG).show();return;}
                if(saved){BrowserPrefs.removeFavorite(host,current);Toast.makeText(host,"Favorito removido",Toast.LENGTH_SHORT).show();}
                else if(BrowserPrefs.addFavorite(host,current,web.getTitle())) Toast.makeText(host,"Página salva",Toast.LENGTH_SHORT).show();
                else Toast.makeText(host,"Favoritos cheios ou endereço inválido",Toast.LENGTH_LONG).show();
            }));
        AppMenus.show(host,"Favoritos","Acesso rápido aos sites que você salvou.",rows);
    }
    private void showDisplayTools(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Localizar na página","Buscar palavras e navegar entre resultados","search",this::openFind));
        rows.add(AppMenus.Entry.item("Aumentar zoom","Ampliar o conteúdo da página","search",()->{if(!web.zoomIn())Toast.makeText(host,"Zoom máximo",Toast.LENGTH_SHORT).show();}));
        rows.add(AppMenus.Entry.item("Diminuir zoom","Reduzir o conteúdo da página","search",()->{if(!web.zoomOut())Toast.makeText(host,"Zoom mínimo",Toast.LENGTH_SHORT).show();}));
        rows.add(AppMenus.Entry.item(BrowserPrefs.desktop(host)?"Desativar modo computador":"Ativar modo computador",
            "Alternar visualização desktop e celular","globe",()->{
                BrowserPrefs.setDesktop(host,!BrowserPrefs.desktop(host));applyPagePreferences(false);
                Toast.makeText(host,"Visualização atualizada",Toast.LENGTH_SHORT).show();
            }));
        rows.add(AppMenus.Entry.item("Abrir em outro navegador","Compartilhar a página com o navegador externo","forward",()->openInExternalBrowser(web.getUrl())));
        AppMenus.show(host,"Ferramentas da página","Ajustes de visualização e pesquisa.",rows);
    }
    private void showProtectionTools(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Bloqueador de anúncios","Nível e contador de requisições bloqueadas","shield",this::showAdBlockMenu));
        boolean exception=adBlock.isException(web.getUrl());
        rows.add(AppMenus.Entry.item(exception?"Ativar neste site":"Desativar neste site",
            exception?"Restaurar o bloqueio neste domínio":"Criar exceção apenas para este domínio","shield",()->{
                if(AdBlockRules.host(web.getUrl())==null){Toast.makeText(host,"Abra um site HTTPS",Toast.LENGTH_SHORT).show();return;}
                adBlock.setException(web.getUrl(),!exception);web.reload();
            }));
        AppMenus.show(host,"Proteção e privacidade","Exceções não alteram outros sites.",rows);
    }
    private void showLoginTools(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Diagnóstico do navegador","WebView, cookies e erros sem dados sensíveis","alert",this::showLoginDiagnosis));
        rows.add(AppMenus.Entry.item("Desempenho e memória","RAM disponível, PSS do app e estado da WebView","settings",this::showPerformanceDiagnosis));
        rows.add(AppMenus.Entry.item("Ajuda com login","Google, Facebook e verificações","lock",this::showLoginHelp));
        rows.add(AppMenus.Entry.item("Abrir em navegador externo","Para sites que exigem autenticação externa","globe",()->openInExternalBrowser(web.getUrl())));
        AppMenus.show(host,"Login e diagnóstico","Ferramentas de compatibilidade e segurança.",rows);
    }

    /** Popups de autenticação somente após gesto humano, com WebView isolada e sem JS bridge. */
    private boolean createLoginWindow(boolean userGesture, Message resultMsg) {
        if (!LoginWindowPolicy.allowPopup(userGesture, loginPopup != null) || resultMsg == null
                || !(resultMsg.obj instanceof WebView.WebViewTransport)) {
            if (status != null) status.setText("Janela automática bloqueada. Use ⋮ > Diagnóstico de login.");
            return false;
        }
        final WebView popup = new WebView(host);
        loginPopup = popup;
        WebSettings settings = popup.getSettings();
        settings.setJavaScriptEnabled(BrowserPrefs.javascript(host));
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        WebKitCompat.optimize(settings);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(popup,BrowserPrefs.thirdPartyCookies(host));
        popup.setWebViewClient(new WebViewClient(){
            @Override public boolean onRenderProcessGone(WebView w,RenderProcessGoneDetail detail){
                if(loginPopupDialog!=null)loginPopupDialog.dismiss();
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r) {
                String url=r.getUrl().toString();
                if(r.isForMainFrame() && AuthNavigation.isGoogleOAuth(url)){
                    if(loginPopupDialog!=null)loginPopupDialog.dismiss();
                    offerExternalGoogleLogin(url);
                    return true;
                }
                return !LoginWindowPolicy.safeWebAddress(url);
            }
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap favicon){
                if(AuthNavigation.isGoogleOAuth(url)){
                    v.stopLoading();
                    if(loginPopupDialog!=null)loginPopupDialog.dismiss();
                    offerExternalGoogleLogin(url);
                }
            }
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError error){
                if(r.isForMainFrame()){
                    lastNavigationProblem="Erro na janela de autenticação: "+error.getErrorCode();
                    Toast.makeText(host,"A janela de login não carregou ("+error.getErrorCode()+")",Toast.LENGTH_LONG).show();
                }
            }
        });
        popup.setWebChromeClient(new WebChromeClient(){
            @Override public void onCloseWindow(WebView window){
                if(loginPopupDialog!=null)loginPopupDialog.dismiss();
            }
        });
        LinearLayout content = Ui.vertical(host);
        content.setPadding(dp(10),dp(8),dp(10),dp(4));
        TextView hint = Ui.text(host,"Janela do site • autenticação",14,colors.text,true);
        content.addView(hint,Ui.lp(host,-1,40,0));
        FrameLayout container = new FrameLayout(host);
        int height = Math.max(dp(260),Math.min(dp(530),
                (int)(host.getResources().getDisplayMetrics().heightPixels*0.60f)));
        container.addView(popup,new FrameLayout.LayoutParams(-1,-1));
        content.addView(container,new LinearLayout.LayoutParams(-1,height));
        AlertDialog dialog = new AlertDialog.Builder(host)
            .setView(content)
            .setNegativeButton("Fechar",null)
            .setNeutralButton("Abrir no navegador",(d,w)->{
                String target=popup.getUrl();
                if(!LoginWindowPolicy.safeWebAddress(target))target=web.getUrl();
                openInExternalBrowser(target);
            }).create();
        loginPopupDialog = dialog;
        dialog.setOnDismissListener(d->{
            if (loginPopup==popup)loginPopup=null;
            if (loginPopupDialog==dialog)loginPopupDialog=null;
            ViewGroup parent=(ViewGroup)popup.getParent();
            if(parent!=null)parent.removeView(popup);
            popup.stopLoading();
            popup.destroy();
        });
        try {
            dialog.show();
            WebView.WebViewTransport transport=(WebView.WebViewTransport)resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        } catch(RuntimeException failure){
            if(dialog.isShowing())dialog.dismiss();
            else {
                loginPopup=null;loginPopupDialog=null;
                ViewGroup parent=(ViewGroup)popup.getParent();
                if(parent!=null)parent.removeView(popup);
                popup.destroy();
            }
            return false;
        }
    }

    private void showPerformanceDiagnosis(){
        if(web==null)return;
        final String origin=LoginWindowPolicy.originForReport(web.getUrl());
        final boolean hung=rendererUnresponsive;
        final boolean light=lightweightPage;
        final boolean pressure=memoryPressure;
        final boolean visible=pageActive;
        // Debug.getPss pode ler mapas de memória: nunca chamar no thread da interface.
        new Thread(()->{
            final String memory;
            try{memory=BrowserMemoryReport.capture(host.getApplicationContext()).summary();}
            catch(RuntimeException failure){
                host.runOnUiThread(()->Toast.makeText(host,"Não foi possível ler o diagnóstico de memória",Toast.LENGTH_LONG).show());
                return;
            }
            host.runOnUiThread(()->{
                if(web==null||host.isFinishing()||host.isDestroyed())return;
                String report="Origem: "+origin+"\n"
                    +"Navegador em primeiro plano: "+(visible?"sim":"não")+"\n"
                    +"Modo leve ativo: "+(light?"sim":"não")+"\n"
                    +"Varreduras opcionais suspensas: "+(pressure||hung||!visible||light?"sim":"não")+"\n"
                    +"Renderizador: "+(hung?"sem resposta":"sem travamento detectado")+"\n\n"
                    +memory+"\n\nNenhum cookie, texto da página ou parâmetro de URL é incluído.";
                List<AppMenus.Entry> rows=AppMenus.entries();
                rows.add(AppMenus.Entry.item("Copiar relatório","Diagnóstico sem dados de autenticação","copy",()->copyLinkText("Memória Baixa Fácil",report)));
                rows.add(AppMenus.Entry.item("Abrir navegador externo","Alternativa para páginas que permanecem travadas","globe",()->openInExternalBrowser(web.getUrl())));
                AppMenus.info(host,"Desempenho e memória",report,rows);
            });
        },"bf-memory-report").start();
    }

    private void showLoginDiagnosis(){
        PackageInfo current=WebView.getCurrentWebViewPackage();
        String engine=current==null?"Não identificado":current.packageName+" (versão "+current.versionName+")";
        String report="Baixa Fácil • diagnóstico de compatibilidade\n"
            +"Motor WebView: "+engine+"\n"
            +"Origem atual: "+LoginWindowPolicy.originForReport(web.getUrl())+"\n"
            +"JavaScript: "+(BrowserPrefs.javascript(host)?"ativado":"desativado")+"\n"
            +"Cookies de terceiros: "+(BrowserPrefs.thirdPartyCookies(host)?"permitidos":"bloqueados")+"\n"
            +"Modo computador: "+(BrowserPrefs.desktop(host)?"ativado":"desativado")+"\n"
            +"Compatibilidade ChatGPT: "+(BrowserPrefs.chatGptLight(host)?"ativada":"desativada")+"\n"
            +"Renderizador: "+(rendererUnresponsive?"sem resposta":"sem falha de resposta detectada")+"\n"
            +"Janela de login: "+(loginPopup==null?"fechada":"aberta")+"\n"
            +"Falha recente: "+lastNavigationProblem+"\n"
            +"Dados sensíveis como cookies, senhas, caminhos e parâmetros de URLs não são incluídos.";
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Copiar diagnóstico","Sem cookies ou parâmetros de URL","copy",()->copyLinkText("Diagnóstico",report)));
        rows.add(AppMenus.Entry.item("Atualizar WebView","Procurar a versão mais recente no sistema","reload",this::openWebViewUpdate));
        AppMenus.info(host,"Diagnóstico do navegador",report,rows);
    }

    private void copyLinkText(String label, String text){
        ClipboardManager cb=(ClipboardManager)host.getSystemService(Activity.CLIPBOARD_SERVICE);
        if(cb!=null)cb.setPrimaryClip(ClipData.newPlainText(label,text));
        Toast.makeText(host,"Diagnóstico copiado sem dados de sessão",Toast.LENGTH_SHORT).show();
    }

    private void openWebViewUpdate(){
        PackageInfo current=WebView.getCurrentWebViewPackage();
        if(current==null || current.packageName==null){
            Toast.makeText(host,"Componente WebView não identificado",Toast.LENGTH_LONG).show();return;
        }
        String appId=current.packageName;
        try {
            host.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+appId)));
        } catch(ActivityNotFoundException | SecurityException ignored){
            try {
                host.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id="+appId)));
            } catch(ActivityNotFoundException | SecurityException error){
                Toast.makeText(host,"Não foi possível abrir a atualização da WebView",Toast.LENGTH_LONG).show();
            }
        }
    }

    private void offerExternalGoogleLogin(String url) {
        if(googleLoginDialogVisible) return;
        googleLoginDialogVisible=true;
        AlertDialog dialog=new AlertDialog.Builder(host)
            .setTitle("Login do Google no navegador")
            .setMessage("O Google não permite login OAuth dentro de uma WebView. Para proteger sua conta, abra a autenticação em um navegador compatível.\n\nA sessão do navegador externo é independente e não entra automaticamente no Baixa Fácil.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Abrir navegador",(d,w)->openInExternalBrowser(url))
            .create();
        dialog.setOnDismissListener(d->googleLoginDialogVisible=false);
        dialog.show();
    }

    private void showLoginHelp() {
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Abrir navegador externo","Usar um navegador compatível para o login","globe",()->openInExternalBrowser(web.getUrl())));
        rows.add(AppMenus.Entry.item("Ver diagnóstico","WebView, cookies e falhas recentes","alert",this::showLoginDiagnosis));
        AppMenus.info(host,"Ajuda com login",
            "Google: login OAuth não é compatível com WebView. Use o navegador externo.\n\n"
            + "Facebook: mantenha JavaScript ligado, desative o modo computador e tente a opção de cookies de terceiros somente se necessário (menos privacidade).\n\n"
            + "A sessão externa não é importada. Nenhuma alteração garante que todos os sites aceitem login na WebView.",rows);
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

    private void scheduleMediaScan(long delayMs){
        if(web==null || !VideoPrefs.autoScan(host) ||
            !BrowserPerformancePolicy.allowAutomaticMedia(pageActive,lightweightPage,memoryPressure,rendererUnresponsive))return;
        cancelMediaScan();
        int generation=scanGeneration;
        Runnable job=()->{
            pendingMediaScan=null;
            if(generation==scanGeneration && web!=null &&
                BrowserPerformancePolicy.allowAutomaticMedia(pageActive,lightweightPage,memoryPressure,rendererUnresponsive) &&
                !AuthNavigation.isSensitiveLoginPage(web.getUrl())){
                lastAutomaticScanUptime=SystemClock.uptimeMillis();
                scanPageVideos(false);
            }
        };
        pendingMediaScan=job;
        long delay=BrowserPerformancePolicy.scheduleDelay(delayMs,SystemClock.uptimeMillis(),lastAutomaticScanUptime);
        web.postDelayed(job,delay);
    }

    private void updateVideosButton() {
        if(videosButton==null)return;
        int count;
        synchronized(videoUrls){count=VideoMenuModel.display(videoUrls,VideoPrefs.showStreams(host),VideoPrefs.showSources(host)).size();}
        videosButton.setText(count>0?"Vídeos ("+count+")":"Vídeos");
        videosButton.setVisibility(count>0?View.VISIBLE:View.GONE);
    }

    private void scanPageVideos(boolean showResult) {
        if(web==null)return;
        if(!showResult && !BrowserPerformancePolicy.allowAutomaticMedia(pageActive,lightweightPage,memoryPressure,rendererUnresponsive))return;
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
                    PreviewMeta meta=new PreviewMeta(obj.optString("title"),obj.optInt("x",0),obj.optInt("y",0),
                        obj.optInt("w",0),obj.optInt("h",0),obj.optBoolean("playing",false),obj.optBoolean("visible",false),obj.optString("frame"));
                    addVideoCandidate(obj.optString("url"),obj.optString("kind"),obj.optString("mime"),meta);
                }
            } catch(Exception ignored) {
                if(showResult)Toast.makeText(host,"Não foi possível analisar esta página",Toast.LENGTH_SHORT).show();
            }
            updateVideosButton();
            if(showResult)showDetectedVideos();
        });
    }

    private void addVideoCandidate(String url,String kind,String mime) {
        addVideoCandidate(url,kind,mime,null);
    }

    private void addVideoCandidate(String url,String kind,String mime,PreviewMeta meta) {
        if(!VideoLinks.isVideoCandidate(url,kind,mime) && !MediaSourcePolicy.isPlaylist(url))return;
        synchronized(videoUrls){
            if(!videoUrls.containsKey(url) && videoUrls.size()<VideoLinks.MAX_VIDEO_URLS)
                videoUrls.put(url,MediaSourcePolicy.isPlaylist(url)?"stream":(MediaSourcePolicy.isDirect(url)?"direct":("video".equals(kind)||"source".equals(kind)?"video":"unverified")));
        }
        if(meta!=null && meta.hasInfo()) {
            synchronized(videoPreviewMeta){
                PreviewMeta existing=videoPreviewMeta.get(url);
                if(videoPreviewMeta.size()<VideoLinks.MAX_VIDEO_URLS || existing!=null){
                    if(existing==null || meta.rank()>existing.rank() ||
                       (meta.rank()==existing.rank() && !meta.title.isEmpty() && existing.title.isEmpty()) ||
                       (!meta.frame.isEmpty() && existing.frame.isEmpty())) videoPreviewMeta.put(url,meta);
                }
            }
        }
    }

    private void showDetectedVideos() {
        List<VideoMenuModel.Video> videos;
        synchronized(videoUrls){
            videos=VideoMenuModel.display(videoUrls,VideoPrefs.showStreams(host),VideoPrefs.showSources(host));
        }
        if(videos.isEmpty()){
            List<AppMenus.Entry> rows=AppMenus.entries();
            String message=(mediaElements>0 || unsupportedMedia>0)
                ? "Há um player na página, mas não foi encontrado um arquivo direto ou playlist pública compatível. Algumas páginas usam blob:, segmentos protegidos ou sessões restritas."
                : "Nenhum vídeo compatível identificado. Abra ou reproduza o vídeo da página e faça uma nova busca.";
            rows.add(AppMenus.Entry.item("Procurar novamente","Verificar a página atual","search",()->scanPageVideos(true)));
            rows.add(AppMenus.Entry.item("Configurações de vídeos","Ajustar os tipos exibidos","settings",host::openVideoSettings));
            AppMenus.info(host,"Vídeos na página",message,rows);
            return;
        }
        java.util.HashMap<String,VideoPresentation.Context> contexts=new java.util.HashMap<>();
        synchronized(videoPreviewMeta){
            for(Map.Entry<String,PreviewMeta> entry:videoPreviewMeta.entrySet()){
                PreviewMeta meta=entry.getValue();
                contexts.put(entry.getKey(),new VideoPresentation.Context(meta.title,meta.playing,meta.visible));
            }
        }
        showDetectedVideosDialog(VideoPresentation.sort(videos,contexts));
    }

    private void showDetectedVideosDialog(List<VideoMenuModel.Video> videos){
        final Ui.Palette p=new Ui.Palette(host);
        final int d=dp(1);
        final Bitmap snapshot=captureVisiblePage();
        LinearLayout root=Ui.vertical(host);
        root.setPadding(dp(18),dp(19),dp(18),dp(12));
        root.setBackground(Ui.shape(host,p.surface,24));
        TextView heading=Ui.text(host,"Vídeos encontrados ("+videos.size()+")",19,p.text,true);
        root.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        TextView subtitle=Ui.text(host,"Miniaturas usam a área visível da página quando possível. Fontes do mesmo vídeo permanecem agrupadas.",12,p.muted,false);
        root.addView(subtitle,Ui.lp(host,-1,-2,6));
        ScrollView scroll=new ScrollView(host){
            @Override protected void onMeasure(int w,int h){
                int limit=host.getResources().getDisplayMetrics().heightPixels*62/100;
                super.onMeasure(w,MeasureSpec.makeMeasureSpec(limit,MeasureSpec.AT_MOST));
            }
        };
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list=Ui.vertical(host);
        list.setPadding(0,d*5,0,d*4);
        boolean addedDirect=false,addedStreaming=false,addedUnverified=false;
        int shown=0;
        final AlertDialog dialog=new AlertDialog.Builder(host).setView(root).create();
        for(VideoMenuModel.Video video:videos){
            boolean streaming=video.kind==MediaSourcePolicy.Kind.HLS||video.kind==MediaSourcePolicy.Kind.DASH;
            if(streaming && !addedStreaming){
                TextView section=Ui.text(host,"TRANSMISSÕES • SOMENTE ONLINE",12,p.muted,true);
                section.setPadding(d*8,d*12,0,d*3); list.addView(section); addedStreaming=true;
            }
            if(!streaming && video.kind==MediaSourcePolicy.Kind.DIRECT && !addedDirect){
                TextView section=Ui.text(host,"ARQUIVOS • DOWNLOAD DISPONÍVEL",12,p.muted,true);
                section.setPadding(d*8,d*12,0,d*3); list.addView(section); addedDirect=true;
            }
            if(video.kind==MediaSourcePolicy.Kind.UNSUPPORTED && !addedUnverified){
                TextView section=Ui.text(host,"FONTES NÃO VERIFICADAS",12,p.muted,true);
                section.setPadding(d*8,d*12,0,d*3); list.addView(section); addedUnverified=true;
            }
            final int number=++shown;
            list.addView(buildVideoRow(dialog,video,number,p,snapshot),Ui.lp(host,-1,-2,7));
        }
        scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,Ui.lp(host,-1,-2,8));
        LinearLayout actions=Ui.horizontal(host);
        actions.setGravity(Gravity.END);
        TextView refresh=Ui.pill(host,"Atualizar",p.teal,p.soft);
        refresh.setOnClickListener(v->{dialog.dismiss();scanPageVideos(true);});
        actions.addView(refresh,new LinearLayout.LayoutParams(-2,dp(44)));
        TextView settings=Ui.pill(host,"Configurar",p.text,p.soft);
        settings.setOnClickListener(v->{dialog.dismiss();host.openVideoSettings();});
        LinearLayout.LayoutParams settingsP=new LinearLayout.LayoutParams(-2,dp(44)); settingsP.leftMargin=dp(8);
        actions.addView(settings,settingsP);
        TextView close=Ui.pill(host,"Fechar",p.text,p.soft);
        close.setOnClickListener(v->dialog.dismiss());
        LinearLayout.LayoutParams closeP=new LinearLayout.LayoutParams(-2,dp(44)); closeP.leftMargin=dp(8);
        actions.addView(close,closeP);
        root.addView(actions,Ui.lp(host,-1,-2,8));
        dialog.setOnDismissListener(v->{ if(snapshot!=null && !snapshot.isRecycled()) snapshot.recycle(); });
        dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(host.getResources().getDisplayMetrics().widthPixels*94/100,ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private View buildVideoRow(AlertDialog dialog,VideoMenuModel.Video video,int number,Ui.Palette p,Bitmap snapshot){
        boolean streaming=video.kind==MediaSourcePolicy.Kind.HLS||video.kind==MediaSourcePolicy.Kind.DASH;
        PreviewMeta meta=findPreview(video);
        LinearLayout row=Ui.horizontal(host);
        row.setPadding(dp(12),dp(10),dp(12),dp(10));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Ui.shape(host,p.soft,18));
        row.setClickable(true); row.setFocusable(true);

        FrameLayout thumbBox=new FrameLayout(host);
        thumbBox.setBackground(Ui.shape(host,p.surface,14));
        LinearLayout.LayoutParams thumbP=new LinearLayout.LayoutParams(dp(76),dp(56));
        Bitmap thumb=meta==null?null:decodeVideoFrame(meta.frame);
        if(thumb==null && snapshot!=null && meta!=null)thumb=cropVisiblePreview(snapshot,meta);
        if(thumb!=null){
            ImageView preview=new ImageView(host);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setImageBitmap(thumb);
            thumbBox.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        }else{
            thumbBox.addView(Ui.icon(host,streaming?"globe":"play",p.teal,24),new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));
        }
        row.addView(thumbBox,thumbP);

        LinearLayout labels=Ui.vertical(host);
        String title=meta!=null && !TextUtils.isEmpty(meta.title)?meta.title:
            (video.title.equals("Arquivo de vídeo") || video.title.length()>48?"Vídeo "+number:video.title);
        if(video.sources.size()>1)title+=" • "+video.sources.size()+" fontes";
        TextView titleView=Ui.text(host,title,14,p.text,true);
        titleView.setMaxLines(2);titleView.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(titleView);
        String detail=video.detail;
        if(meta!=null && meta.playing && meta.visible) detail="Em reprodução • "+detail;
        else if(meta!=null && meta.visible) detail="Visível na página • "+detail;
        else detail="Fonte sem associação visual confirmada • "+detail;
        TextView sub=Ui.text(host,detail,12,p.muted,false);
        sub.setMaxLines(2); sub.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(sub,Ui.lp(host,-1,-2,2));
        LinearLayout.LayoutParams labelP=new LinearLayout.LayoutParams(0,-2,1); labelP.leftMargin=dp(11);
        row.addView(labels,labelP);
        row.addView(Ui.icon(host,"forward",p.muted,18),new LinearLayout.LayoutParams(dp(20),dp(20)));
        row.setOnClickListener(v->{dialog.dismiss();showVideoActions(video,number);});
        return row;
    }

    private PreviewMeta findPreview(VideoMenuModel.Video video){
        synchronized(videoPreviewMeta){
            PreviewMeta best=null;
            for(VideoMenuModel.Source source:video.sources){
                PreviewMeta meta=videoPreviewMeta.get(source.url);
                if(meta==null){
                    // Link assinado pode ter sido renovado pelo CDN; usa somente a mesma
                    // identidade de arquivo/caminho, nunca domínio isolado ou horário.
                    String identity=VideoMenuModel.mediaIdentity(source.url);
                    if(!identity.isEmpty())for(Map.Entry<String,PreviewMeta> entry:videoPreviewMeta.entrySet()){
                        if(identity.equals(VideoMenuModel.mediaIdentity(entry.getKey()))){
                            PreviewMeta possible=entry.getValue();
                            if(meta==null || possible.rank()>meta.rank())meta=possible;
                        }
                    }
                }
                if(meta==null)continue;
                if(best==null || meta.rank()>best.rank() ||
                    (meta.rank()==best.rank() && best.title.isEmpty() && !meta.title.isEmpty()))best=meta;
            }
            return best;
        }
    }

    private Bitmap decodeVideoFrame(String frame){
        if(frame==null || !frame.startsWith("data:image/jpeg;base64,") || frame.length()>18000)return null;
        try{
            byte[] bytes=Base64.decode(frame.substring("data:image/jpeg;base64,".length()),Base64.DEFAULT);
            if(bytes.length>14500)return null;
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inPreferredConfig=Bitmap.Config.RGB_565;
            Bitmap image=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(image!=null && image.getWidth()<=160 && image.getHeight()<=120)return image;
            if(image!=null)image.recycle();
        }catch(RuntimeException | OutOfMemoryError ignored){}
        return null;
    }

    private Bitmap captureVisiblePage(){
        if(web==null || web.getWidth()<10 || web.getHeight()<10) return null;
        try{
            Bitmap snapshot=Bitmap.createBitmap(web.getWidth(),web.getHeight(),Bitmap.Config.RGB_565);
            Canvas canvas=new Canvas(snapshot);
            web.draw(canvas);
            return snapshot;
        }catch(OutOfMemoryError | RuntimeException ignored){
            return null;
        }
    }

    private Bitmap cropVisiblePreview(Bitmap snapshot,PreviewMeta meta){
        if(snapshot==null || meta==null || !meta.hasBounds()) return null;
        int left=Math.max(0,Math.min(snapshot.getWidth()-1,meta.x));
        int top=Math.max(0,Math.min(snapshot.getHeight()-1,meta.y));
        int width=Math.min(snapshot.getWidth()-left,Math.max(1,meta.width));
        int height=Math.min(snapshot.getHeight()-top,Math.max(1,meta.height));
        if(width<8 || height<8) return null;
        try{
            Bitmap raw=Bitmap.createBitmap(snapshot,left,top,width,height);
            return Bitmap.createScaledBitmap(raw,dp(76),dp(56),true);
        }catch(IllegalArgumentException | OutOfMemoryError ignored){
            return null;
        }
    }

    private void showVideoActions(VideoMenuModel.Video video,int number){
        if(video.sources.size()==1){
            showSourceActions(video,video.sources.get(0),number);
            return;
        }
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.section("FONTES E QUALIDADES"));
        for(VideoMenuModel.Source source:video.sources){
            rows.add(AppMenus.Entry.item(source.label,source.hint,"play",()->showSourceActions(video,source,number)));
        }
        rows.add(AppMenus.Entry.item("Voltar à lista","Ver os outros vídeos encontrados","back",this::showDetectedVideos));
        AppMenus.show(host,"Vídeo "+number,"Escolha uma fonte. As qualidades são estimadas pelo endereço e podem diferir do vídeo real.",rows);
    }
    private void showSourceActions(VideoMenuModel.Video video,VideoMenuModel.Source source,int number){
        boolean streaming=video.kind==MediaSourcePolicy.Kind.HLS||video.kind==MediaSourcePolicy.Kind.DASH;
        boolean direct=video.kind==MediaSourcePolicy.Kind.DIRECT;
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Reproduzir","Abrir esta fonte no Media3","play",()->{
            Intent play=new Intent(host,MediaPlayerActivity.class);
            play.putExtra(MediaPlayerActivity.EXTRA_MEDIA_URL,source.url);
            host.startActivity(play);
        }));
        if(direct)rows.add(AppMenus.Entry.item("Baixar arquivo","Salvar esta fonte pelo gerenciador","download",()->offerDownload(source.url,web.getSettings().getUserAgentString(),-1)));
        rows.add(AppMenus.Entry.item("Copiar endereço","Copiar somente o link da fonte selecionada","copy",()->copyLink(source.url)));
        rows.add(AppMenus.Entry.item(video.sources.size()>1?"Escolher outra qualidade":"Voltar à lista",
            "Ver as outras fontes encontradas","back",()->{
                if(video.sources.size()>1)showVideoActions(video,number);
                else showDetectedVideos();
            }));
        String hint=streaming?"HLS/DASH: reprodução online, sem download segmentado nem acesso a DRM."
            :direct?"Arquivo direto: o download depende da autorização do servidor."
            :"Formato não confirmado: o reprodutor pode não abrir. Download indisponível.";
        AppMenus.show(host,source.label,hint+"\n"+video.domain,rows);
    }

    private void showFavorites(){
        List<BrowserPrefs.Favorite> favorites=BrowserPrefs.favorites(host);
        List<AppMenus.Entry> rows=AppMenus.entries();
        if(favorites.isEmpty()){
            rows.add(AppMenus.Entry.item("Adicionar favorito","Salvar um endereço manualmente","bookmark",host::openFavoriteSettings));
            AppMenus.info(host,"Favoritos","Nenhum site salvo. Abra uma página e selecione Salvar esta página, ou adicione pelo gerenciador.",rows);
            return;
        }
        for(BrowserPrefs.Favorite site:favorites){
            String domain="Site salvo";
            try{domain=new URI(site.url).getHost();}catch(Exception ignored){}
            rows.add(AppMenus.Entry.item(site.title,domain,"bookmark",()->navigate(site.url)));
        }
        rows.add(AppMenus.Entry.section("GERENCIAMENTO"));
        rows.add(AppMenus.Entry.item("Adicionar ou editar","Organizar os favoritos salvos","settings",host::openFavoriteSettings));
        AppMenus.show(host,"Favoritos ("+favorites.size()+")","Toque em um site para abrir.",rows);
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
    void saveState(Bundle output){if(web!=null){Bundle b=new Bundle();web.saveState(b);output.putBundle("BROWSER_WEB_STATE",b);}}
    void destroy(){pageActive=false;cancelMediaScan(); dismissAddressSuggestions(); if(loginPopupDialog!=null)loginPopupDialog.dismiss();if(web!=null){if(pendingMediaScan!=null)web.removeCallbacks(pendingMediaScan);pendingMediaScan=null;web.setDownloadListener(null);web.stopLoading(); ViewGroup parent=(ViewGroup)web.getParent();if(parent!=null)parent.removeView(web);web.destroy();web=null;} }
    void clearLocalData(){closeFind();web.clearCache(true);web.clearHistory();navigate(BrowserPrefs.home(host));}
    private void copyAddress(){
        String url=address.getText().toString().trim();
        if(url.isEmpty()){Toast.makeText(host,"Nenhum endereço para copiar",Toast.LENGTH_SHORT).show();return;}
        ClipboardManager cb=(ClipboardManager)host.getSystemService(Activity.CLIPBOARD_SERVICE);
        if(cb!=null){cb.setPrimaryClip(ClipData.newPlainText("Endereço",url));Toast.makeText(host,"Endereço copiado",Toast.LENGTH_SHORT).show();}
    }
    private void showHistory() {
        List<String> items=BrowserPrefs.history(host);
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(String url:items){
            String domain=Uri.parse(url).getHost();
            rows.add(AppMenus.Entry.item(domain==null?"Site":"Abrir "+domain,"Página visitada","globe",()->navigate(url)));
        }
        AppMenus.show(host,"Histórico recente",items.isEmpty()?"Nenhuma página recente.":"Toque para abrir a página.",rows);
    }
    private void showLinkMenu(String url) {
        boolean supported = url.equals(UrlTools.extract(url));
        List<AppMenus.Entry> rows=AppMenus.entries();
        if(supported){
            rows.add(AppMenus.Entry.item("Abrir link","Navegar nesta janela","globe",()->navigate(url)));
            rows.add(AppMenus.Entry.item("Copiar link","Copiar o endereço completo","copy",()->copyLink(url)));
            rows.add(AppMenus.Entry.item("Baixar arquivo","Tentar transferir pelo gerenciador","download",()->offerDownload(url,web.getSettings().getUserAgentString(),-1)));
            rows.add(AppMenus.Entry.item("Compartilhar link","Enviar para outro aplicativo","link",()->{
                Intent share=new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,url);
                host.startActivity(Intent.createChooser(share,"Compartilhar link"));
            }));
            rows.add(AppMenus.Entry.item("Salvar nos favoritos","Guardar endereço deste link","bookmark",()->{
                if(BrowserPrefs.addFavorite(host,url,null))Toast.makeText(host,"Link salvo nos favoritos",Toast.LENGTH_SHORT).show();
                else Toast.makeText(host,"Favoritos cheios ou URL inválida",Toast.LENGTH_LONG).show();
            }));
        }else{
            rows.add(AppMenus.Entry.item("Copiar endereço","Formato não compatível para download","copy",()->copyLink(url)));
        }
        AppMenus.show(host,"Ações do link",supported?"Escolha o que fazer com este endereço.":"Só é possível baixar URLs HTTP/HTTPS.",rows);
    }
    private void copyLink(String url) {
        ClipboardManager clipboard=(ClipboardManager)host.getSystemService(Activity.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Link",url));
        Toast.makeText(host,"Link copiado",Toast.LENGTH_SHORT).show();
    }

    private void offerDownload(String url,String agent,long length) {
        if(url==null || !url.equals(UrlTools.extract(url))) {
            AppEventLog.warn(host,"Navegador","Tentativa de baixar endereço não compatível com o motor HTTP próprio.");
            DownloadDialogs.info(host,"Download não compatível","O mecanismo próprio aceita somente links HTTP/HTTPS. blob:, data: e arquivos internos não são suportados.");
            return;
        }
        String domain;
        try { domain=new URI(url).getHost(); } catch(Exception ex) { domain="servidor"; }
        String size=length>=0?(length/1024)+" KB":"Tamanho desconhecido";
        DownloadDialogs.confirm(host,
            "Baixar com Baixa Fácil?",
            "Confirme o envio deste arquivo para a fila local do aplicativo.",
            "Origem",domain,
            "Arquivo",size,
            "Destino","Download/ pública",
            "Cookies de sessão não serão armazenados no histórico nem enviados a outros domínios.",
            url.startsWith("http://"),
            ()->startDownload(url,agent));
    }
    private void startDownload(String url,String agent){
        if(DownloadDuplicateUi.maybeShow(host,url,()->startNewDownload(url,agent),host::showDownloads))return;
        startNewDownload(url,agent);
    }
    private void startNewDownload(String url,String agent){
        String restriction=DownloadRestrictions.blocked(host);
        if(restriction!=null){
            AppEventLog.warn(host,"Downloads","Download bloqueado pelas regras locais de rede ou bateria.");
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
        }else {
            AppEventLog.error(host,"Downloads","Android bloqueou ou recusou a fila do download.");
            Toast.makeText(host,"Não foi possível iniciar o download",Toast.LENGTH_LONG).show();
        }
    }
}
