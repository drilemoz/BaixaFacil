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
import android.os.SystemClock;
import android.app.ActivityManager;
import android.util.Base64;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.WebRequestError;
import org.mozilla.geckoview.WebResponse;
import org.mozilla.geckoview.StorageController;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Navegador embutido no host principal. O GeckoView não é destruído na troca de abas. */
final class BrowserPane {
    private final MainActivity host;
    private final Ui.Palette colors;
    final LinearLayout view;
    private GeckoBrowserView web;
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
    private GeckoBrowserView loginPopup;
    private boolean popupGestureAllowed;
    private AlertDialog loginPopupDialog;
    private String lastNavigationProblem = "Nenhuma falha registrada nesta sessão.";
    private int lastHttpStatus;
    private boolean googleLoginDialogVisible;
    private TextView videosButton;
    private final LinkedHashMap<String,String> videoUrls = new LinkedHashMap<>();
    private final LinkedHashMap<String,PreviewMeta> videoPreviewMeta = new LinkedHashMap<>();
    private PreviewMeta pageVideoPreview;
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

    // A varredura de vídeos roda na extensão empacotada assets/baixafacil_gecko/media.js.
    // Não executa código JavaScript arbitrário recebido de sites.

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
        web=new GeckoBrowserView(host);
        GeckoSession session=web.session;
        standardUserAgent=GeckoSession.getDefaultUserAgent();
        appliedDesktop=BrowserPrefs.desktop(host);
        appliedTextZoom=BrowserPrefs.textZoom(host);
        appliedThirdPartyCookies=BrowserPrefs.thirdPartyCookies(host);
        session.getSettings().setAllowJavascript(BrowserPrefs.javascript(host));
        session.getSettings().setUserAgentMode(appliedDesktop ?
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP : GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        session.getSettings().setViewportMode(appliedDesktop ?
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP : GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        session.getSettings().setUseTrackingProtection(AdBlockController.savedTrackers(host));
        web.setFindListener(code -> {
            if(findCount!=null && findBar.getVisibility()==View.VISIBLE) {
                int current=code/10000, total=code%10000;
                findCount.setText(total==0?"0/0":current+"/"+total);
            }
        });
        session.setPromptDelegate(new GeckoPromptHandler(host));
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate(){
            @Override public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession gecko,
                    GeckoSession.NavigationDelegate.LoadRequest request) {
                String url=request.uri;
                if(AuthNavigation.isGoogleOAuth(url)){
                    offerExternalGoogleLogin(url);
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                if(!LoginWindowPolicy.safeWebAddress(url)){
                    status.setText("Endereço bloqueado: apenas HTTP e HTTPS.");
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                // Janelas são tratadas por onNewSession; nunca permitir abertura automática.
                if(request.target==GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW){
                    popupGestureAllowed=request.hasUserGesture;
                    if(!popupGestureAllowed)return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            }
            @Override public void onLocationChange(GeckoSession gecko,String url,
                    List<GeckoSession.PermissionDelegate.ContentPermission> permissions,Boolean gesture) {
                web.currentUrl(url);
                if(url!=null && url.equals(UrlTools.extract(url))){
                    setAddressFromPage(url);showSecurity(url);BrowserPrefs.remember(host,url);
                }
            }
            @Override public void onCanGoBack(GeckoSession gecko,boolean value){ web.backAvailable(value); }
            @Override public void onCanGoForward(GeckoSession gecko,boolean value){ web.forwardAvailable(value); }
            @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession gecko,String uri){
                return createLoginWindow(uri);
            }
            @Override public GeckoResult<String> onLoadError(GeckoSession gecko,String url,WebRequestError error){
                lastNavigationProblem="Erro GeckoView: "+error.code;
                AppEventLog.error(host,"Navegador","Falha GeckoView: código "+error.code);
                status.setText("Falha ao abrir página ("+error.code+"). Consulte diagnóstico.");
                return null; // Não libera páginas internas inseguras nem ignora TLS.
            }
        });
        session.setProgressDelegate(new GeckoSession.ProgressDelegate(){
            @Override public void onPageStart(GeckoSession gecko,String url){
                adBlock.setPage(url);
                lastHttpStatus=0;
                lightweightPage=BrowserLoadPolicy.lightMode(url,BrowserPrefs.chatGptLight(host));
                lastProgressBucket=-1;
                lastAutomaticScanUptime=0;
                rendererUnresponsive=false;
                scanGeneration++;
                cancelMediaScan();
                synchronized(videoUrls){videoUrls.clear();}
                synchronized(videoPreviewMeta){videoPreviewMeta.clear();}
                mediaElements=0; unsupportedMedia=0;pageVideoPreview=null;
                updateVideosButton();
            }
            @Override public void onPageStop(GeckoSession gecko,boolean success){
                String url=web.getUrl();
                if(success && !lightweightPage)applyCosmeticFilters(url);
                if(findBar.getVisibility()==View.VISIBLE && findInput.length()>0)
                    web.findAllAsync(findInput.getText().toString());
                status.setText(!success?"A página não terminou de carregar. Consulte o diagnóstico."
                    : url!=null&&url.startsWith("http://")?"Atenção: conexão HTTP sem criptografia."
                    : AuthNavigation.isFacebookCheckpoint(url)?"Verificação do Facebook: use navegador externo se necessário."
                    : "Página pronta. Toque num arquivo para baixar.");
                if(success)scheduleMediaScan(1300);
            }
            @Override public void onProgressChange(GeckoSession gecko,int progress){
                int bucket=progress/10;
                if(bucket==lastProgressBucket && progress!=100)return;
                lastProgressBucket=bucket;
                loading.setProgress(progress);
                loading.setVisibility(progress>=100?View.INVISIBLE:View.VISIBLE);
                if(progress<100)status.setText("Carregando página: "+(bucket*10)+"%");
            }
        });
        session.setContentDelegate(new GeckoSession.ContentDelegate(){
            @Override public void onTitleChange(GeckoSession gecko,String title){web.pageTitle(title);}
            @Override public void onContextMenu(GeckoSession gecko,int x,int y,
                    GeckoSession.ContentDelegate.ContextElement element){
                String target=element.linkUri!=null?element.linkUri:element.srcUri;
                if(target!=null && !target.isEmpty())showLinkMenu(target);
            }
            @Override public void onExternalResponse(GeckoSession gecko,WebResponse response){
                if(response.uri!=null)offerDownload(response.uri,standardUserAgent,-1);
            }
            @Override public void onCrash(GeckoSession gecko){recoverGeckoCrash();}
            @Override public void onKill(GeckoSession gecko){recoverGeckoCrash();}
        });
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
            // Não abre páginas antes de concluir a limpeza de dados do GeckoView.
            SessionPrefs.clearForNewSession(host,()->{
                if(web!=null)navigate(BrowserPrefs.home(host));
            });
            return;
        }
        if(saved!=null){
            String previous=saved.getString("BROWSER_GECKO_URL");
            if(previous!=null && LoginWindowPolicy.safeWebAddress(previous)){
                navigate(previous);return;
            }
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
        if(web!=null)web.setActive(false);
    }
    void resume(){
        if(web==null)return;
        pageActive=true;
        web.setActive(true);
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
        if(web==null)return;
        GeckoSessionSettings settings=web.session.getSettings();
        settings.setAllowJavascript(BrowserPrefs.javascript(host));
        settings.setUseTrackingProtection(AdBlockController.savedTrackers(host));
        int zoom=BrowserPrefs.textZoom(host);
        if(appliedTextZoom!=zoom){appliedTextZoom=zoom;forceReload=true;}
        boolean thirdParty=BrowserPrefs.thirdPartyCookies(host);
        if(thirdParty!=appliedThirdPartyCookies){appliedThirdPartyCookies=thirdParty;forceReload=true;}
        GeckoEngine.updatePreferences(host);
        boolean desktop=BrowserPrefs.desktop(host);
        if(desktop!=appliedDesktop){
            appliedDesktop=desktop;
            settings.setUserAgentMode(desktop ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP : GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
            settings.setViewportMode(desktop ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP : GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
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
        // CSS fixo na extensão empacotada; não lê cookies nem credenciais.
        web.cosmeticFilters();
    }
    private void showAdBlockMenu(){
        int mode=adBlock.mode();
        String[] names={"Desligado","Leve","Forte","Extremo"};
        String page=web==null?null:web.getUrl();
        String summary="Modo cosmético salvo: "+names[mode]+" • "+adBlock.ruleCount()+" regras cadastradas (domínios não interceptados)\n"
            +"Bloqueios de domínios por listas locais: não disponível no GeckoView\n"
            +"Ocultação CSS neste site: "+(adBlock.isException(page)?"desativada":"ativa");
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
            rows.add(AppMenus.Entry.item("Abrir ChatGPT no Chrome ou navegador","Alternativa se o GeckoView estiver lento","globe",()->openInExternalBrowser(web.getUrl())));
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
        rows.add(AppMenus.Entry.item("Aumentar zoom","Ampliar o conteúdo da página","search",()->{changeTextZoom(10);}));
        rows.add(AppMenus.Entry.item("Diminuir zoom","Reduzir o conteúdo da página","search",()->{changeTextZoom(-10);}));
        rows.add(AppMenus.Entry.item(BrowserPrefs.desktop(host)?"Desativar modo computador":"Ativar modo computador",
            "Alternar visualização desktop e celular","globe",()->{
                BrowserPrefs.setDesktop(host,!BrowserPrefs.desktop(host));applyPagePreferences(false);
                Toast.makeText(host,"Visualização atualizada",Toast.LENGTH_SHORT).show();
            }));
        rows.add(AppMenus.Entry.item("Abrir com Custom Tabs","Navegador instalado com sessão própria","globe",()->openCompatibleBrowser(web.getUrl())));
        rows.add(AppMenus.Entry.item("Abrir em outro navegador","Escolher um navegador externo","forward",()->openInExternalBrowser(web.getUrl())));
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
        rows.add(AppMenus.Entry.item("Diagnóstico do navegador","GeckoView, privacidade e erros sem dados sensíveis","alert",this::showLoginDiagnosis));
        rows.add(AppMenus.Entry.item("Desempenho e memória","RAM disponível, PSS do app e estado do GeckoView","settings",this::showPerformanceDiagnosis));
        rows.add(AppMenus.Entry.item("Ajuda com login","Google, Facebook e verificações","lock",this::showLoginHelp));
        rows.add(AppMenus.Entry.item("Abrir com Custom Tabs","Compatibilidade com login e navegador externo","globe",()->openCompatibleBrowser(web.getUrl())));
        rows.add(AppMenus.Entry.item("Escolher navegador externo","Alternativa caso Custom Tabs não esteja disponível","forward",()->openInExternalBrowser(web.getUrl())));
        AppMenus.show(host,"Login e diagnóstico","Ferramentas de compatibilidade e segurança.",rows);
    }

    /** Apenas janelas criadas por gesto e carregadas em GeckoSession separada. */
    private GeckoResult<GeckoSession> createLoginWindow(String uri){
        if(!LoginWindowPolicy.allowPopup(popupGestureAllowed,loginPopup!=null) ||
                (uri!=null && !LoginWindowPolicy.safeWebAddress(uri))){
            popupGestureAllowed=false;
            return GeckoResult.fromValue(null);
        }
        popupGestureAllowed=false;
        GeckoBrowserView popup=new GeckoBrowserView(host);
        loginPopup=popup;
        popup.session.getSettings().setAllowJavascript(BrowserPrefs.javascript(host));
        popup.session.getSettings().setUseTrackingProtection(AdBlockController.savedTrackers(host));
        popup.session.setPromptDelegate(new GeckoPromptHandler(host));
        popup.session.setNavigationDelegate(new GeckoSession.NavigationDelegate(){
            @Override public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession gecko,
                    GeckoSession.NavigationDelegate.LoadRequest request){
                if(AuthNavigation.isGoogleOAuth(request.uri)){
                    if(loginPopupDialog!=null)loginPopupDialog.dismiss();
                    offerExternalGoogleLogin(request.uri);
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                return GeckoResult.fromValue(LoginWindowPolicy.safeWebAddress(request.uri)?AllowOrDeny.ALLOW:AllowOrDeny.DENY);
            }
            @Override public void onLocationChange(GeckoSession gecko,String url,
                    List<GeckoSession.PermissionDelegate.ContentPermission> perms,Boolean gesture){popup.currentUrl(url);}
        });
        popup.session.setContentDelegate(new GeckoSession.ContentDelegate(){
            @Override public void onCloseRequest(GeckoSession gecko){
                if(loginPopupDialog!=null)loginPopupDialog.dismiss();
            }
            @Override public void onExternalResponse(GeckoSession gecko,WebResponse response){
                if(response.uri!=null)offerDownload(response.uri,standardUserAgent,-1);
            }
        });
        LinearLayout content=Ui.vertical(host);
        content.setPadding(dp(10),dp(8),dp(10),dp(4));
        content.addView(Ui.text(host,"Janela do site • autenticação",14,colors.text,true),Ui.lp(host,-1,40,0));
        FrameLayout container=new FrameLayout(host);
        int height=Math.max(dp(260),Math.min(dp(530),(int)(host.getResources().getDisplayMetrics().heightPixels*0.60f)));
        container.addView(popup,new FrameLayout.LayoutParams(-1,-1));
        content.addView(container,new LinearLayout.LayoutParams(-1,height));
        AlertDialog dialog=new AlertDialog.Builder(host).setView(content)
            .setNegativeButton("Fechar",null)
            .setNeutralButton("Abrir no navegador",(d,w)->{
                String target=popup.getUrl();
                if(!LoginWindowPolicy.safeWebAddress(target))target=web.getUrl();
                openInExternalBrowser(target);
            }).create();
        loginPopupDialog=dialog;
        dialog.setOnDismissListener(d->{
            if(loginPopup==popup)loginPopup=null;
            if(loginPopupDialog==dialog)loginPopupDialog=null;
            ViewGroup parent=(ViewGroup)popup.getParent();
            if(parent!=null)parent.removeView(popup);
            popup.stopLoading();popup.destroyBrowser();
        });
        dialog.show();
        return GeckoResult.fromValue(popup.session);
    }

    private void recoverGeckoCrash(){
        lastNavigationProblem="Processo de conteúdo Gecko encerrado pelo Android.";
        AppEventLog.error(host,"Navegador","Processo de conteúdo Gecko encerrado.");
        if(loginPopupDialog!=null)loginPopupDialog.dismiss();
        host.getWindow().getDecorView().post(()->{
            if(!host.isFinishing()&&!host.isDestroyed()){
                Toast.makeText(host,"O navegador parou. Reabrindo a página.",Toast.LENGTH_LONG).show();
                host.recreate();
            }
        });
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
        String engine="GeckoView 158.0.20261007201254 (Mozilla)";
        String report="Baixa Fácil • diagnóstico de compatibilidade\n"
            +"Motor: "+engine+"\n"
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
        rows.add(AppMenus.Entry.note("GeckoView está empacotado no APK. Atualize o Baixa Fácil para receber correções do motor."));
        AppMenus.info(host,"Diagnóstico do navegador",report,rows);
    }

    private void copyLinkText(String label, String text){
        ClipboardManager cb=(ClipboardManager)host.getSystemService(Activity.CLIPBOARD_SERVICE);
        if(cb!=null)cb.setPrimaryClip(ClipData.newPlainText(label,text));
        Toast.makeText(host,"Diagnóstico copiado sem dados de sessão",Toast.LENGTH_SHORT).show();
    }

    private void offerExternalGoogleLogin(String url) {
        if(googleLoginDialogVisible) return;
        googleLoginDialogVisible=true;
        AlertDialog dialog=new AlertDialog.Builder(host)
            .setTitle("Login do Google no navegador")
            .setMessage("Alguns provedores restringem OAuth em navegadores incorporados. Para proteger sua conta, abra a autenticação em um navegador compatível.\n\nA sessão do navegador externo é independente e não entra automaticamente no Baixa Fácil.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Abrir navegador",(d,w)->openLoginBrowser(url))
            .create();
        dialog.setOnDismissListener(d->googleLoginDialogVisible=false);
        dialog.show();
    }

    private void showLoginHelp() {
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Abrir com Custom Tabs","Usar um navegador instalado para o login","globe",()->openCompatibleBrowser(web.getUrl())));
        rows.add(AppMenus.Entry.item("Ver diagnóstico","GeckoView, privacidade e falhas recentes","alert",this::showLoginDiagnosis));
        AppMenus.info(host,"Ajuda com login",
            "Google OAuth pode rejeitar navegadores incorporados. Use o navegador externo.\n\n"
            + "Facebook: mantenha JavaScript ligado, desative o modo computador e tente a opção de cookies de terceiros somente se necessário (menos privacidade).\n\n"
            + "A sessão externa não é importada. Nem todos os sites aceitam login em navegadores incorporados.",rows);
    }

    private void openCompatibleBrowser(String url) {
        if(!CustomTabsBrowser.open(host,url))openInExternalBrowser(url);
    }
    private void openLoginBrowser(String url) {
        if(CustomTabsBrowser.forLogin(host))openCompatibleBrowser(url);
        else openInExternalBrowser(url);
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
        videosButton.setText(count>0?"Fontes ("+count+")":"Vídeos");
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
        web.scanVideos(value->{
            if(generation!=scanGeneration || !page.equals(web.getUrl()))return;
            try {
                Object decoded=new JSONTokener(value).nextValue();
                JSONObject result=new JSONObject(decoded.toString());
                mediaElements=result.optInt("players",0);
                unsupportedMedia=result.optInt("unsupported",0);
                JSONObject pagePreview=result.optJSONObject("pagePreview");
                pageVideoPreview=pagePreview==null?null:new PreviewMeta(pagePreview.optString("title"),
                    pagePreview.optInt("x"),pagePreview.optInt("y"),
                    pagePreview.optInt("w"),pagePreview.optInt("h"),
                    pagePreview.optBoolean("playing"),pagePreview.optBoolean("visible"),
                    pagePreview.optString("frame"));
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
        TextView heading=Ui.text(host,"Fontes de vídeo ("+videos.size()+")",19,p.text,true);
        root.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        TextView subtitle=Ui.text(host,"Uma página pode fornecer várias qualidades do mesmo vídeo. Miniaturas individuais só aparecem quando a fonte foi identificada com segurança.",12,p.muted,false);
        root.addView(subtitle,Ui.lp(host,-1,-2,6));
        // Prévia opcional da página, sem atribuí-la indevidamente a uma URL de CDN.
        PreviewMeta pageMeta=pageVideoPreview;
        Bitmap cover=pageMeta==null?null:decodeVideoFrame(pageMeta.frame);
        if(cover==null && pageMeta!=null && snapshot!=null)cover=cropVisiblePreview(snapshot,pageMeta);
        if(cover!=null){
            LinearLayout previewHeader=Ui.horizontal(host);
            previewHeader.setGravity(Gravity.CENTER_VERTICAL);
            ImageView image=new ImageView(host);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageBitmap(cover);
            previewHeader.addView(image,new LinearLayout.LayoutParams(dp(100),dp(66)));
            TextView text=Ui.text(host,"Prévia do player visível na página. A correspondência com as fontes abaixo não foi confirmada.",12,p.muted,false);
            LinearLayout.LayoutParams txtParams=new LinearLayout.LayoutParams(0,-2,1);txtParams.leftMargin=dp(12);
            previewHeader.addView(text,txtParams);
            root.addView(previewHeader,Ui.lp(host,-1,-2,10));
        }
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
                TextView section=Ui.text(host,"HLS/DASH • DOWNLOAD CONFORME SUPORTE",12,p.muted,true);
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
        if(video.kind==MediaSourcePolicy.Kind.DIRECT || video.kind==MediaSourcePolicy.Kind.HLS){
            TextView download=Ui.pill(host,"Baixar",p.teal,p.surface);
            download.setContentDescription("Baixar "+(video.kind==MediaSourcePolicy.Kind.HLS?"HLS compatível":"vídeo"));
            download.setOnClickListener(v->{
                dialog.dismiss();
                if(video.sources.size()>1)showVideoActions(video,number);
                else if(video.kind==MediaSourcePolicy.Kind.HLS)
                    offerHlsDownload(video.sources.get(0).url,standardUserAgent);
                else offerDownload(video.sources.get(0).url,standardUserAgent,-1);
            });
            LinearLayout.LayoutParams buttonP=new LinearLayout.LayoutParams(-2,dp(44));buttonP.leftMargin=dp(5);
            row.addView(download,buttonP);
        }else{
            row.addView(Ui.icon(host,"forward",p.muted,18),new LinearLayout.LayoutParams(dp(20),dp(20)));
        }
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
        // GeckoView usa compositor separado; draw(Canvas) não captura a página com confiabilidade.
        return null;
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
        AppMenus.show(host,"Fontes "+number,"Escolha a fonte. As qualidades são estimadas pelo endereço e podem diferir do vídeo real.",rows);
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
        if(direct)rows.add(AppMenus.Entry.item("Baixar arquivo","Salvar esta fonte pelo gerenciador","download",()->offerDownload(source.url,standardUserAgent,-1)));
        if(video.kind==MediaSourcePolicy.Kind.HLS)rows.add(AppMenus.Entry.item("Baixar HLS (TS)",
            "Somente vídeos finalizados sem DRM, em segmentos MPEG-TS","download",
            ()->offerHlsDownload(source.url,standardUserAgent)));
        rows.add(AppMenus.Entry.item("Copiar endereço","Copiar somente o link da fonte selecionada","copy",()->copyLink(source.url)));
        rows.add(AppMenus.Entry.item(video.sources.size()>1?"Escolher outra qualidade":"Voltar à lista",
            "Ver as outras fontes encontradas","back",()->{
                if(video.sources.size()>1)showVideoActions(video,number);
                else showDetectedVideos();
            }));
        String hint=streaming?"HLS: tentativa de baixar vídeo finalizado em TS; outros formatos, transmissões ao vivo e DRM não são suportados. DASH: reprodução online."
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
    private void changeTextZoom(int change){
        int next=Math.max(70,Math.min(180,BrowserPrefs.textZoom(host)+change));
        BrowserPrefs.setTextZoom(host,next);
        applyPagePreferences(true);
        Toast.makeText(host,"Tamanho do texto: "+next+"%",Toast.LENGTH_SHORT).show();
    }
    void saveState(Bundle output){
        // Somente URL validada; nunca serializar cookies, formulário ou dados de login no Bundle.
        if(web!=null && LoginWindowPolicy.safeWebAddress(web.getUrl()))
            output.putString("BROWSER_GECKO_URL",web.getUrl());
    }
    void destroy(){
        pageActive=false;cancelMediaScan();dismissAddressSuggestions();
        if(loginPopupDialog!=null)loginPopupDialog.dismiss();
        if(web!=null){
            web.stopLoading();
            ViewGroup parent=(ViewGroup)web.getParent();
            if(parent!=null)parent.removeView(web);
            web.destroyBrowser();web=null;
        }
    }
    void clearLocalData(){
        closeFind();
        if(web!=null)web.session.purgeHistory();
        GeckoEngine.clear(StorageController.ClearFlags.ALL_CACHES,null,
            msg -> host.runOnUiThread(()->Toast.makeText(host,msg,Toast.LENGTH_LONG).show()));
        navigate(BrowserPrefs.home(host));
    }
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
            rows.add(AppMenus.Entry.item("Baixar arquivo","Tentar transferir pelo gerenciador","download",()->offerDownload(url,standardUserAgent,-1)));
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

    private void offerHlsDownload(String url,String agent){
        if(MediaSourcePolicy.classify(url)!=MediaSourcePolicy.Kind.HLS){
            DownloadDialogs.info(host,"HLS não compatível","A URL selecionada não é uma playlist HLS válida.");
            return;
        }
        String domain;
        try{domain=new URI(url).getHost();}catch(Exception e){domain="servidor";}
        DownloadDialogs.confirm(host,"Baixar vídeo HLS?",
            "O Baixa Fácil tentará reunir os segmentos em um arquivo MPEG-TS (.ts). Este recurso é experimental e só aceita vídeos finalizados, sem DRM, com segmentos TS convencionais.",
            "Origem",domain,"Formato","HLS VOD sem proteção",
            "Destino","Download/ pública (.ts)",
            "Vídeos ao vivo, criptografados, fMP4 ou que exigem sessão de outro domínio serão recusados. Ao retomar, a transferência HLS reinicia do começo.",
            url.startsWith("http://"),()->startDownload(url,agent));
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
        // A API pública do GeckoView não expõe cookies HTTP ao host.
        // Não reutilizar CookieManager da WebView Android (origem/sessão diferentes).
        String cookies=null;
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
