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
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;
import android.content.pm.PackageManager;
import android.Manifest;
import android.speech.RecognizerIntent;
import android.content.res.ColorStateList;
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
import org.mozilla.geckoview.ContentBlocking;
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
    private final GeckoSitePermissions sitePermissions;
    private LinearLayout browserToolbar;
    private LinearLayout bottomInfo;
    private boolean videoFullscreen;
    private boolean findWasVisible;
    private boolean videosWasVisible;
    private EditText address;
    private AlertDialog addressDialog;
    private String editorClipboard;
    private LinearLayout suggestionRows;
    private TextView addressDisplay;
    private TextView progressLabel;
    private final java.util.LinkedHashMap<String,String> imageUrls=new java.util.LinkedHashMap<>();
    private PopupWindow toolsPopup;
    private com.journeyapps.barcodescanner.DecoratedBarcodeView qrCamera;
    private AlertDialog qrDialog;
    private static final int QR_CAMERA_REQUEST=6456;
    private static final int VOICE_REQUEST=6457;
    private static final int IMPORT_REQUEST=6458;
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
    private int appliedProtectionMode;
    private boolean appliedTrackers;
    private int geckoBlockedOnPage;
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
    private boolean geckoProcessGone;
    private boolean geckoRecoveryScheduled;
    private boolean geckoRecoveryDialogVisible;
    private int lastProgressBucket=-1;
    private final AdBlockController adBlock;
    private final FaviconLoader favicons=new FaviconLoader();

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
        sitePermissions=new GeckoSitePermissions(host,()->web==null?null:web.getUrl());
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
        browserToolbar=top;
        LinearLayout row = Ui.horizontal(host);
        row.setBackground(Ui.shape(host,colors.surface,20));
        row.setPadding(dp(5),0,dp(4),0);
        securityIcon=new FrameLayout(host);
        row.addView(securityIcon,new LinearLayout.LayoutParams(dp(32),dp(44)));
        addressDisplay=Ui.text(host,"Pesquisar ou digitar endereço",14,colors.text,true);
        addressDisplay.setSingleLine(true);
        addressDisplay.setEllipsize(TextUtils.TruncateAt.END);
        addressDisplay.setPadding(dp(5),0,0,0);
        addressDisplay.setContentDescription("Editar endereço ou pesquisar");
        addressDisplay.setClickable(true);addressDisplay.setFocusable(true);
        addressDisplay.setOnClickListener(v->openAddressEditor());
        row.addView(addressDisplay,new LinearLayout.LayoutParams(0,dp(48),1));
        securityIcon.setClickable(true);securityIcon.setContentDescription("Informações de segurança da página");
        securityIcon.setOnClickListener(v->showConnectionInfo());
        top.addView(row,new LinearLayout.LayoutParams(0,dp(48),1));
        FrameLayout more=iconButton("more","Menu do navegador",44,()->showCompactMenu());
        top.addView(more,new LinearLayout.LayoutParams(dp(44),dp(44)));
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
        LinearLayout progressRow=Ui.horizontal(host);
        progressRow.setPadding(dp(13),0,dp(13),0);
        progressRow.addView(loading,new LinearLayout.LayoutParams(0,dp(5),1));
        progressLabel=Ui.text(host,"0%",11,colors.muted,true);
        progressLabel.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(dp(45),dp(24));
        labelParams.leftMargin=dp(8);
        progressRow.addView(progressLabel,labelParams);
        progressRow.setTag("bf_progress_row");
        progressRow.setVisibility(View.GONE);
        view.addView(progressRow,new LinearLayout.LayoutParams(-1,dp(24)));

        FrameLayout frame=new FrameLayout(host);
        frame.setBackgroundColor(colors.background);
        frame.setClipToPadding(false);
        web=new GeckoBrowserView(host);
        GeckoSession session=web.session;
        standardUserAgent=GeckoSession.getDefaultUserAgent();
        appliedDesktop=BrowserPrefs.desktop(host);
        appliedTextZoom=BrowserPrefs.textZoom(host);
        appliedThirdPartyCookies=BrowserPrefs.thirdPartyCookies(host);
        appliedProtectionMode=AdBlockController.savedMode(host);
        appliedTrackers=AdBlockController.savedTrackers(host);
        session.getSettings().setAllowJavascript(BrowserPrefs.javascript(host));
        session.getSettings().setUserAgentMode(appliedDesktop ?
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP : GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        session.getSettings().setViewportMode(appliedDesktop ?
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP : GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        session.getSettings().setUseTrackingProtection(GeckoEngine.trackingEnabled(host));
        web.setFindListener(code -> {
            if(findCount!=null && findBar.getVisibility()==View.VISIBLE) {
                int current=code/10000, total=code%10000;
                findCount.setText(total==0?"0/0":current+"/"+total);
            }
        });
        session.setPromptDelegate(new GeckoPromptHandler(host,host.geckoFilePicker()));
        session.setPermissionDelegate(sitePermissions);
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
                    BrowserTopSites.record(host,url);
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
        session.setContentBlockingDelegate(new ContentBlocking.Delegate(){
            @Override public void onContentBlocked(GeckoSession gecko, ContentBlocking.BlockEvent event) {
                // Gecko contou um recurso real. Não inspecionar ou salvar event.uri.
                geckoBlockedOnPage++;
                AdBlockController.noteGeckoBlocked();
            }
        });
        session.setProgressDelegate(new GeckoSession.ProgressDelegate(){
            @Override public void onPageStart(GeckoSession gecko,String url){
                adBlock.setPage(url);
                geckoBlockedOnPage=0;
                lastHttpStatus=0;
                lightweightPage=BrowserLoadPolicy.lightMode(url,BrowserPrefs.chatGptLight(host));
                lastProgressBucket=-1;
                lastAutomaticScanUptime=0;
                rendererUnresponsive=false;
                scanGeneration++;
                cancelMediaScan();
                synchronized(videoUrls){videoUrls.clear();}
                synchronized(imageUrls){imageUrls.clear();}
                synchronized(videoPreviewMeta){videoPreviewMeta.clear();}
                mediaElements=0; unsupportedMedia=0;pageVideoPreview=null;
                updateVideosButton();
                setLoadingProgress(0);
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
                setLoadingProgress(100);
                if(success){
                    BrowserTopSites.setTitle(host,url,web.getTitle());
                    scheduleMediaScan(1300);
                }
            }
            @Override public void onProgressChange(GeckoSession gecko,int progress){
                if(progress==lastProgressBucket)return;
                lastProgressBucket=progress;
                setLoadingProgress(progress);
                if(progress<100)status.setText("Carregando página: "+progress+"%");
            }
        });
        session.setContentDelegate(new GeckoSession.ContentDelegate(){
            @Override public void onFullScreen(GeckoSession gecko,boolean fullScreen){
                if(web!=null && gecko==web.session)setVideoFullscreen(fullScreen);
            }
            @Override public void onTitleChange(GeckoSession gecko,String title){
                web.pageTitle(title);
                BrowserTopSites.setTitle(host,web.getUrl(),title);
            }
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
        videosButton=Ui.pill(host,"◉ Mídia",colors.teal,colors.surface);
        videosButton.setTextSize(12);
        videosButton.setPadding(dp(12),0,dp(12),0);
        videosButton.setMinHeight(dp(40));
        videosButton.setVisibility(View.VISIBLE);
        videosButton.setContentDescription("Buscar imagens e vídeos na página atual");
        videosButton.setOnClickListener(v->scanPageVideos(true));
        FrameLayout.LayoutParams videoP=new FrameLayout.LayoutParams(-2,dp(40),Gravity.END|Gravity.BOTTOM);
        videoP.rightMargin=dp(14);videoP.bottomMargin=dp(14);
        frame.addView(videosButton,videoP);
        view.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        status=Ui.text(host,"Digite um endereço ou pesquise na web.",11,colors.muted,false);
        status.setSingleLine(true);status.setEllipsize(TextUtils.TruncateAt.END);
        status.setPadding(dp(19),0,dp(19),0);
        bottomInfo=Ui.horizontal(host);
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
    private void setLoadingProgress(int progress) {
        if(loading==null)return;
        int value=Math.max(0,Math.min(100,progress));
        loading.setProgress(value);progressLabel.setText(value+"%");
        View parent=(View)loading.getParent();
        if(parent!=null)parent.setVisibility(value>=100?View.GONE:View.VISIBLE);
    }
    private void dismissAddressSuggestions() {
        if(addressDialog!=null){addressDialog.dismiss();addressDialog=null;}
    }
    private String shownHost(String url) {
        try{
            URI u=new URI(url);String hostname=u.getHost();
            if(hostname==null)return "Pesquisar ou digitar endereço";
            int port=u.getPort();
            return hostname+(port>=0?":"+port:"");
        }catch(Exception ignored){return "Pesquisar ou digitar endereço";}
    }
    private void setAddressFromPage(String url) {
        if(addressDisplay!=null)addressDisplay.setText(shownHost(url));
        if(address!=null && addressDialog==null)address.setText(url==null?"":url);
    }
    private void openAddressEditor(){
        if(addressDialog!=null && addressDialog.isShowing())return;
        LinearLayout shell=Ui.vertical(host);
        shell.setBackground(Ui.shape(host,colors.surface,24));
        shell.setPadding(dp(15),dp(16),dp(15),dp(12));
        LinearLayout search=Ui.horizontal(host);
        search.setBackground(Ui.shape(host,colors.soft,19));
        search.setPadding(dp(9),0,dp(5),0);
        search.addView(Ui.icon(host,"search",colors.muted,20),new LinearLayout.LayoutParams(dp(26),dp(26)));
        address=new EditText(host);
        address.setSingleLine(true);address.setSelectAllOnFocus(true);address.setTextSize(14);
        address.setTextColor(colors.text);address.setHintTextColor(colors.muted);
        address.setHint("Pesquisar ou digitar um URL");address.setBackgroundColor(Color.TRANSPARENT);
        address.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setPadding(dp(5),0,dp(4),0);
        String current=web==null?"":web.getUrl();
        address.setText(current==null?"":current);
        search.addView(address,new LinearLayout.LayoutParams(0,dp(52),1));
        search.addView(iconButton("mic","Pesquisar por voz",40,this::startVoiceSearch),
            new LinearLayout.LayoutParams(dp(40),dp(44)));
        search.addView(iconButton("qr","Escanear código QR",40,this::startQrScanner),
            new LinearLayout.LayoutParams(dp(40),dp(44)));
        shell.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
        ScrollView scroll=new ScrollView(host);
        scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
        editorClipboard=clipboardUrl();
        suggestionRows=Ui.vertical(host);
        suggestionRows.setPadding(dp(2),dp(10),dp(2),dp(5));
        scroll.addView(suggestionRows,new ScrollView.LayoutParams(-1,-2));
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView cancel=Ui.pill(host,"Fechar",colors.text,colors.soft);
        LinearLayout.LayoutParams cp=Ui.lp(host,-2,42,8);cp.gravity=Gravity.END;
        shell.addView(cancel,cp);
        AlertDialog dialog=new AlertDialog.Builder(host).setView(shell).create();
        addressDialog=dialog;
        cancel.setOnClickListener(v->dismissAddressSuggestions());
        dialog.setOnDismissListener(d->{if(addressDialog==dialog){addressDialog=null;address=null;suggestionRows=null;editorClipboard=null;}});
        address.setOnEditorActionListener((v,action,event)->{
            if(action==EditorInfo.IME_ACTION_GO || action==EditorInfo.IME_ACTION_SEARCH){navigate(address.getText().toString());return true;}
            return false;
        });
        address.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence v,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence v,int a,int b,int c){updateAddressSuggestions();}
            @Override public void afterTextChanged(Editable e){}
        });
        dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            dialog.getWindow().setLayout(host.getResources().getDisplayMetrics().widthPixels*96/100,
                Math.min(dp(530),host.getResources().getDisplayMetrics().heightPixels*65/100));
        }
        updateAddressSuggestions();
    }
    private void addSuggestion(String url,String title,String kind){
        LinearLayout row=Ui.horizontal(host);row.setPadding(dp(9),dp(8),dp(4),dp(8));
        row.setBackground(Ui.shape(host,colors.soft,14));
        ImageView ico=new ImageView(host);
        ico.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ico.setImageDrawable(host.getDrawable(android.R.drawable.ic_menu_compass));
        row.addView(ico,new LinearLayout.LayoutParams(dp(34),dp(34)));
        favicons.load(host,url,ico);
        LinearLayout labels=Ui.vertical(host);
        TextView name=Ui.text(host,title==null||title.trim().isEmpty()?shownHost(url):title,14,colors.text,true);
        name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);labels.addView(name);
        TextView path=Ui.text(host,url,11,colors.muted,false);
        path.setSingleLine(true);path.setEllipsize(TextUtils.TruncateAt.END);labels.addView(path);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-2,1);pp.leftMargin=dp(12);
        row.addView(labels,pp);
        if(!"Colar".equals(kind)){
            row.addView(iconButton("copy","Copiar "+url,43,()->copyLink(url)),
                new LinearLayout.LayoutParams(dp(43),dp(43)));
        }else{
            TextView paste=Ui.text(host,"Colar",13,colors.teal,true);
            paste.setPadding(dp(5),0,dp(5),0);
            row.addView(paste);paste.setOnClickListener(v->setEditorText(url));
        }
        labels.setOnClickListener(v->{if("Colar".equals(kind))setEditorText(url);else navigate(url);});
        ico.setOnClickListener(v->{if("Colar".equals(kind))setEditorText(url);else navigate(url);});
        LinearLayout.LayoutParams itemP=Ui.lp(host,-1,-2,5);
        suggestionRows.addView(row,itemP);
    }
    private void setEditorText(String text){
        if(address==null)return;
        address.setText(text);address.setSelection(address.length());address.requestFocus();
    }
    private void updateAddressSuggestions(){
        if(suggestionRows==null||address==null)return;
        suggestionRows.removeAllViews();
        String term=address.getText().toString().trim();
        String pasted=editorClipboard;
        if(pasted!=null && !pasted.equals(term)){
            section("Área de transferência");
            addSuggestion(pasted,"Link que você copiou","Colar");
        }
        java.util.LinkedHashSet<String> added=new java.util.LinkedHashSet<>();
        if(!term.isEmpty()){
            section("Endereços e sugestões");
            java.util.List<BrowserSuggestions.Candidate> candidates=new ArrayList<>();
            for(BrowserPrefs.Favorite f:BrowserPrefs.favorites(host))candidates.add(new BrowserSuggestions.Candidate(f.url,f.title,true));
            for(String h:BrowserPrefs.history(host))candidates.add(new BrowserSuggestions.Candidate(h,BrowserTopSites.title(host,h),false));
            for(BrowserSuggestions.Item item:BrowserSuggestions.suggest(term,candidates)){
                if(added.add(item.url))addSuggestion(item.url,item.title,"Histórico");
            }
        }
        section("Sites mais acessados");
        for(BrowserTopSites.Site site:BrowserTopSites.mostVisited(host,12)){
            if(added.add(site.url))addSuggestion(site.url,site.title,"Frequente");
        }
        if(added.isEmpty()){
            for(BrowserPrefs.Favorite item:BrowserPrefs.favorites(host)){
                if(added.add(item.url))addSuggestion(item.url,item.title,"Favorito");
            }
        }
        if(added.isEmpty())suggestionRows.addView(Ui.text(host,
            "Os sites visitados aparecerão aqui, um abaixo do outro.",13,colors.muted,false),Ui.lp(host,-1,-2,9));
    }
    private void section(String text){
        TextView label=Ui.text(host,text,12,colors.muted,true);
        label.setPadding(dp(8),dp(8),0,dp(3));
        suggestionRows.addView(label);
    }
    private String clipboardUrl(){
        try{
            ClipboardManager cb=(ClipboardManager)host.getSystemService(Activity.CLIPBOARD_SERVICE);
            if(cb==null || !cb.hasPrimaryClip())return null;
            ClipData clip=cb.getPrimaryClip();
            if(clip==null||clip.getItemCount()==0)return null;
            CharSequence text=clip.getItemAt(0).coerceToText(host);
            String value=text==null?"":text.toString().trim();
            return value.length()<4096 && value.equals(UrlTools.extract(value))?value:null;
        }catch(Exception ignored){return null;}
    }
    private void showConnectionInfo(){
        String url=web==null?null:web.getUrl();
        String site=shownHost(url);
        AppMenus.info(host,"Segurança da página",
            (url!=null && url.startsWith("https://")?"Conexão HTTPS com ":"Conexão sem HTTPS com ")+site+
            ". O ícone não garante a identidade ou a segurança do conteúdo.",AppMenus.entries());
    }
    private void showSecurity(String url){
        securityIcon.removeAllViews();
        ImageView favicon=new ImageView(host);
        favicon.setImageDrawable(host.getDrawable(android.R.drawable.ic_menu_compass));
        securityIcon.addView(favicon,new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));
        favicons.load(host,url,favicon);
    }
    void navigate(String input){
        String url=BrowserPrefs.destination(host,input);
        if(url==null){Toast.makeText(host,"Endereço inválido ou não suportado",Toast.LENGTH_SHORT).show();return;}
        if(address!=null){
            InputMethodManager keyboard=(InputMethodManager)host.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(address.getWindowToken(),0);
        }
        dismissAddressSuggestions();setAddressFromPage(url);showSecurity(url);
        if(web!=null)web.loadUrl(url);
    }
    void goBack(){if(web.canGoBack()) web.goBack();}
    void goForward(){if(web.canGoForward()) web.goForward();}
    void reload(){web.reload();}
    void home(){navigate(BrowserPrefs.home(host));}
    boolean canGoBack(){return web.canGoBack();}
    void pause(){
        pageActive=false;
        cancelMediaScan();
        if(web!=null && !geckoProcessGone)web.setActive(false);
    }
    void resume(){
        if(web==null)return;
        pageActive=true;
        // Após onCrash/onKill a GeckoSession está fechada: nunca ativá-la novamente.
        if(geckoProcessGone){scheduleGeckoRecovery();return;}
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
        int protectionMode=AdBlockController.savedMode(host);
        boolean trackers=AdBlockController.savedTrackers(host);
        if(protectionMode!=appliedProtectionMode || trackers!=appliedTrackers){
            appliedProtectionMode=protectionMode;
            appliedTrackers=trackers;
            forceReload=true;
        }
        settings.setUseTrackingProtection(GeckoEngine.trackingEnabled(host));
        int zoom=BrowserPrefs.textZoom(host);
        if(appliedTextZoom!=zoom){appliedTextZoom=zoom;forceReload=true;}
        boolean thirdParty=BrowserPrefs.thirdPartyCookies(host);
        if(thirdParty!=appliedThirdPartyCookies){
            appliedThirdPartyCookies=thirdParty;
            settings.setUseTrackingProtection(GeckoEngine.trackingEnabled(host));
            forceReload=true;
        }
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
        String summary="Proteção GeckoView: "+names[mode]
            +(GeckoEngine.trackingEnabled(host)?" (ativa)":" (inativa)")+"\n"
            +"Bloqueios nativos nesta página: "+geckoBlockedOnPage+"\n"
            +"Bloqueios nativos nesta sessão: "+AdBlockController.sessionBlocked()+"\n"
            +"Listas EasyList locais: "+adBlock.ruleCount()+" regras (sem bloqueio de rede)\n"
            +"Exceção visual CSS neste domínio: "+(adBlock.isException(page)?"ativa":"inativa");
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Abrir configurações","Intensidade, listas e rastreadores","settings",host::openSettings));
        rows.add(AppMenus.Entry.item(adBlock.isException(page)?"Reativar ocultação CSS":"Suspender ocultação CSS",
            "Não altera a proteção de rede do GeckoView","shield",()->{
                if(AdBlockRules.host(page)==null)return;
                adBlock.setException(page,!adBlock.isException(page));web.reload();
            }));
        AppMenus.show(host,"Bloqueador de anúncios",summary,rows);
    }
    private void showCompactMenu(){
        if(toolsPopup!=null && toolsPopup.isShowing()){toolsPopup.dismiss();return;}
        LinearLayout list=Ui.vertical(host);
        list.setPadding(dp(7),dp(8),dp(7),dp(8));
        list.setBackground(Ui.shape(host,colors.surface,17));
        PopupWindow pop=new PopupWindow(list,dp(264),-2,true);
        toolsPopup=pop;
        pop.setBackgroundDrawable(Ui.shape(host,colors.surface,17));
        pop.setOutsideTouchable(true);pop.setElevation(dp(9));
        addMenuRow(list,"Favoritos","bookmark",this::showFavoriteTools,pop);
        addMenuRow(list,"Histórico","reload",this::showHistory,pop);
        addMenuRow(list,"Ferramentas da página","search",this::showDisplayTools,pop);
        addMenuRow(list,"Proteção e privacidade","shield",this::showProtectionTools,pop);
        addMenuRow(list,"Senhas e importação","lock",this::showPasswordsAndImport,pop);
        addMenuRow(list,"Login e diagnóstico","alert",this::showLoginTools,pop);
        addMenuRow(list,"Configurações","settings",host::openSettings,pop);
        if(browserToolbar!=null)pop.showAsDropDown(browserToolbar,-dp(20),dp(1),Gravity.END);
    }
    private void addMenuRow(LinearLayout list,String title,String symbol,Runnable run,PopupWindow pop){
        LinearLayout item=Ui.horizontal(host);item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(12),dp(3),dp(6),dp(3));
        item.addView(Ui.icon(host,symbol,colors.text,21),new LinearLayout.LayoutParams(dp(25),dp(25)));
        TextView text=Ui.text(host,title,14,colors.text,false);
        LinearLayout.LayoutParams label=new LinearLayout.LayoutParams(0,-1,1);label.leftMargin=dp(12);
        item.addView(text,label);
        item.setClickable(true);item.setFocusable(true);
        item.setOnClickListener(v->{pop.dismiss();run.run();});
        list.addView(item,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private void showPageTools(){showCompactMenu();}
    private void showVideoTools(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Imagens e vídeos encontrados","Fontes de mídia desta página","play",this::showDetectedMedia));
        rows.add(AppMenus.Entry.item("Procurar nesta página","Atualizar a detecção de mídias","search",()->scanPageVideos(true)));
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
        rows.add(AppMenus.Entry.item(exception?"Reativar ocultação CSS":"Suspender ocultação CSS",
            exception?"Voltar a esconder banners":"Não afeta os bloqueios nativos do GeckoView","shield",()->{
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
        popup.session.getSettings().setUseTrackingProtection(GeckoEngine.trackingEnabled(host));
        popup.session.setPromptDelegate(new GeckoPromptHandler(host,host.geckoFilePicker()));
        // Permissões de mídia e localização são recusadas em popups: não herdar a origem da janela principal.
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

    /** A tela cheia é propriedade da GeckoSession; não altera o estado dos downloads. */
    private void setVideoFullscreen(boolean enabled){
        if(videoFullscreen==enabled)return;
        videoFullscreen=enabled;
        if(enabled){
            findWasVisible=findBar.getVisibility()==View.VISIBLE;
            videosWasVisible=videosButton!=null && videosButton.getVisibility()==View.VISIBLE;
        }
        browserToolbar.setVisibility(enabled?View.GONE:View.VISIBLE);
        findBar.setVisibility(enabled?View.GONE:(findWasVisible?View.VISIBLE:View.GONE));
        View progressParent=(View)loading.getParent();
        if(progressParent!=null)progressParent.setVisibility(enabled?View.GONE:
            loading.getProgress()<100?View.VISIBLE:View.GONE);
        if(bottomInfo!=null)bottomInfo.setVisibility(enabled?View.GONE:View.VISIBLE);
        if(videosButton!=null)videosButton.setVisibility(enabled?View.GONE:(videosWasVisible?View.VISIBLE:View.GONE));
        host.setBrowserFullscreen(enabled);
    }
    boolean isVideoFullscreen(){return videoFullscreen;}
    void exitVideoFullscreen(){
        if(!videoFullscreen)return;
        if(web!=null && !geckoProcessGone && web.session.isOpen())web.session.exitFullScreen();
        setVideoFullscreen(false);
    }
    boolean onAndroidPermissionsResult(int code,String[] permissions,int[] grants){
        if(code==QR_CAMERA_REQUEST){
            if(grants!=null && grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED)showQrScanner();
            else Toast.makeText(host,"Câmera não autorizada",Toast.LENGTH_SHORT).show();
            return true;
        }
        return sitePermissions.onRequestPermissionsResult(code,permissions,grants);
    }

    private void recoverGeckoCrash(){
        // Os dois callbacks podem chegar para a mesma sessão. Tratar somente uma vez.
        if(geckoProcessGone)return;
        geckoProcessGone=true;
        setVideoFullscreen(false);
        host.geckoFilePicker().cancel();
        sitePermissions.cancel();
        lastNavigationProblem="Processo de conteúdo Gecko encerrado pelo Android.";
        AppEventLog.error(host,"Navegador","Processo de conteúdo Gecko encerrado.");
        cancelMediaScan();
        if(loginPopupDialog!=null)loginPopupDialog.dismiss();
        if(status!=null)status.setText("GeckoView interrompido. Abra o navegador para recuperar.");
        if(pageActive)scheduleGeckoRecovery();
    }

    private void scheduleGeckoRecovery(){
        if(geckoRecoveryScheduled || geckoRecoveryDialogVisible)return;
        geckoRecoveryScheduled=true;
        host.getWindow().getDecorView().post(()->{
            geckoRecoveryScheduled=false;
            if(!geckoProcessGone || !pageActive || host.isFinishing() || host.isDestroyed())return;
            // elapsedRealtime evita depender do relógio alterado pelo usuário.
            android.content.SharedPreferences prefs=host.getSharedPreferences("gecko_crash_guard",android.content.Context.MODE_PRIVATE);
            GeckoCrashPolicy.Decision decision=GeckoCrashPolicy.afterFailure(
                SystemClock.elapsedRealtime(),prefs.getLong("window_start",0),prefs.getInt("count",0));
            prefs.edit().putLong("window_start",decision.windowStartMs)
                .putInt("count",decision.crashes).apply();
            if(decision.restartAutomatically){
                Toast.makeText(host,"GeckoView interrompido. Recuperando navegador ("+
                    decision.crashes+"/"+GeckoCrashPolicy.MAX_AUTO_RESTARTS+").",Toast.LENGTH_LONG).show();
                host.recreate();
                return;
            }
            geckoRecoveryDialogVisible=true;
            AlertDialog dialog=new AlertDialog.Builder(host)
                .setTitle("Navegador interrompido repetidamente")
                .setMessage("O GeckoView parou três vezes em pouco tempo. A recuperação automática foi suspensa para evitar reinicializações contínuas. Seus downloads continuam separados do navegador.")
                .setPositiveButton("Tentar novamente",(d,w)->host.recreate())
                .setNegativeButton("Navegador externo",(d,w)->{
                    String current=web==null?null:web.getUrl();
                    if(LoginWindowPolicy.safeWebAddress(current))openInExternalBrowser(current);
                    host.showDownloads();
                })
                .setNeutralButton("Ir aos downloads",(d,w)->host.showDownloads())
                .setOnCancelListener(d->host.showDownloads())
                .create();
            dialog.setOnDismissListener(d->geckoRecoveryDialogVisible=false);
            dialog.show();
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
        int images;
        synchronized(imageUrls){images=imageUrls.size();}
        videosButton.setText(count+images>0?"◉ Mídia ("+(count+images)+")":"◉ Mídia");
        videosButton.setVisibility(View.VISIBLE);
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
        web.scanVideos(showResult,value->{
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
                if(showResult){
                    JSONArray imageItems=result.optJSONArray("images");
                    synchronized(imageUrls){
                        imageUrls.clear();
                        if(imageItems!=null)for(int i=0;i<imageItems.length() && imageUrls.size()<60;i++){
                            String img=imageItems.optString(i);
                            if(img!=null && img.equals(UrlTools.extract(img))) imageUrls.put(img,img);
                        }
                    }
                }
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
            if(showResult)showDetectedMedia();
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

    private void showDetectedMedia(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        int imageCount;
        synchronized(imageUrls){imageCount=imageUrls.size();}
        rows.add(AppMenus.Entry.item("Vídeos e streams","Ver fontes e opções de reprodução","play",this::showDetectedVideos));
        rows.add(AppMenus.Entry.item("Imagens ("+imageCount+")","Ver imagens disponíveis nesta página","file",this::showDetectedImages));
        rows.add(AppMenus.Entry.item("Buscar novamente","Atualizar a varredura da página","search",()->scanPageVideos(true)));
        AppMenus.show(host,"Imagens e mídias","Somente fontes acessíveis no documento atual; não contorna DRM.",rows);
    }
    private void showDetectedImages(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        synchronized(imageUrls){
            int i=0;
            for(String url:imageUrls.keySet()){
                final String chosen=url;final int index=++i;
                rows.add(AppMenus.Entry.item("Imagem "+index,shownHost(url),"file",()->{
                    List<AppMenus.Entry> actions=AppMenus.entries();
                    actions.add(AppMenus.Entry.item("Baixar imagem","Arquivo HTTP/HTTPS público","download",()->offerDownload(chosen,standardUserAgent,-1)));
                    actions.add(AppMenus.Entry.item("Abrir imagem","Abrir no navegador","globe",()->navigate(chosen)));
                    actions.add(AppMenus.Entry.item("Copiar link","Endereço da imagem","copy",()->copyLink(chosen)));
                    AppMenus.show(host,"Imagem "+index,chosen,actions);
                }));
            }
        }
        if(rows.isEmpty())rows.add(AppMenus.Entry.note("Nenhuma imagem HTTP/HTTPS encontrada. Carregue a página e tente novamente."));
        AppMenus.show(host,"Imagens da página","Fontes encontradas na varredura manual.",rows);
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
        setVideoFullscreen(false);
        sitePermissions.cancel();
        pageActive=false;cancelMediaScan();dismissAddressSuggestions();
        if(qrDialog!=null)qrDialog.dismiss();
        if(toolsPopup!=null)toolsPopup.dismiss();
        favicons.shutdown();
        if(loginPopupDialog!=null)loginPopupDialog.dismiss();
        if(web!=null){
            if(!geckoProcessGone)web.stopLoading();
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
    boolean handleBrowserActivityResult(int request,int result,Intent data){
        if(request==VOICE_REQUEST){
            if(result==Activity.RESULT_OK && data!=null){
                ArrayList<String> values=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if(values!=null && !values.isEmpty())setEditorText(values.get(0));
            }
            return true;
        }
        if(request==IMPORT_REQUEST){
            if(result==Activity.RESULT_OK && data!=null && data.getData()!=null)importFromDocument(data.getData());
            return true;
        }
        return false;
    }
    private void startVoiceSearch(){
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Diga uma pesquisa ou endereço");
        try{host.startActivityForResult(intent,VOICE_REQUEST);}
        catch(ActivityNotFoundException e){Toast.makeText(host,"Reconhecimento de voz não disponível neste aparelho",Toast.LENGTH_LONG).show();}
    }
    private void startQrScanner(){
        if(host.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            host.requestPermissions(new String[]{Manifest.permission.CAMERA},QR_CAMERA_REQUEST);return;
        }
        showQrScanner();
    }
    private void showQrScanner(){
        if(qrDialog!=null&&qrDialog.isShowing())return;
        LinearLayout body=Ui.vertical(host);body.setPadding(dp(15),dp(16),dp(15),dp(12));
        body.setBackground(Ui.shape(host,colors.surface,24));
        TextView title=Ui.text(host,"Escanear QR Code",19,colors.text,true);
        body.addView(title,Ui.lp(host,-1,-2,0));
        body.addView(Ui.text(host,"Centralize o código na câmera. Só endereços HTTP/HTTPS serão abertos.",12,colors.muted,false),Ui.lp(host,-1,-2,8));
        qrCamera=new com.journeyapps.barcodescanner.DecoratedBarcodeView(host);
        body.addView(qrCamera,new LinearLayout.LayoutParams(-1,dp(280)));
        TextView cancel=Ui.pill(host,"Cancelar",colors.text,colors.soft);
        LinearLayout.LayoutParams p=Ui.lp(host,-2,44,10);p.gravity=Gravity.END;body.addView(cancel,p);
        AlertDialog dialog=new AlertDialog.Builder(host).setView(body).create();
        qrDialog=dialog;
        cancel.setOnClickListener(v->dialog.dismiss());
        dialog.setOnDismissListener(d->{if(qrCamera!=null){qrCamera.pause();qrCamera=null;}qrDialog=null;});
        dialog.show();
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(host.getResources().getDisplayMetrics().widthPixels*94/100,-2);}
        qrCamera.decodeSingle(new com.journeyapps.barcodescanner.BarcodeCallback(){
            @Override public void barcodeResult(com.journeyapps.barcodescanner.BarcodeResult result){
                String value=result==null?null:result.getText();
                if(value==null)return;
                dialog.dismiss();
                if(value.equals(UrlTools.extract(value))){
                    if(address==null)openAddressEditor();
                    setEditorText(value);
                }
                else Toast.makeText(host,"QR lido. Somente links HTTP/HTTPS válidos são aceitos.",Toast.LENGTH_LONG).show();
            }
            @Override public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> points){}
        });
        qrCamera.resume();
    }
    private void showPasswordsAndImport(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Senhas salvas","Ver e excluir logins protegidos no aparelho","lock",this::manageSavedLogins));
        rows.add(AppMenus.Entry.item("Importar favoritos HTML","Arquivo exportado de outro navegador","bookmark",()->chooseImport("text/html")));
        rows.add(AppMenus.Entry.item("Importar senhas CSV","Importação sensível, exige confirmação","lock",()->{
            DownloadDialogs.confirm(host,"Importar senhas?",
                "O arquivo CSV contém senhas em texto claro. Escolha apenas um arquivo confiável e exclua a cópia original com segurança após importar.",
                "Proteção","Android Keystore","Origem","Arquivo escolhido manualmente",
                "Destino","Armazenamento cifrado do Baixa Fácil","Não importa cookies ou sessões.",false,()->chooseImport("text/*"));
        }));
        AppMenus.show(host,"Senhas e importação","Sem acesso automático aos dados privados de outros navegadores.",rows);
    }
    private void chooseImport(String type){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(type);intent.addCategory(Intent.CATEGORY_OPENABLE);
        try{host.startActivityForResult(intent,IMPORT_REQUEST);}catch(ActivityNotFoundException e){Toast.makeText(host,"Seletor de arquivos indisponível",Toast.LENGTH_LONG).show();}
    }
    private void importFromDocument(Uri uri){
        try(java.io.InputStream in=host.getContentResolver().openInputStream(uri)){
            if(in==null)return;
            java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int read;
            while((read=in.read(buffer))!=-1){
                if(output.size()+read>400_000)throw new IllegalArgumentException("Arquivo maior que 400 KB");
                output.write(buffer,0,read);
            }
            byte[] bytes=output.toByteArray();
            if(bytes.length>400_000)throw new IllegalArgumentException("Arquivo maior que 400 KB");
            String content=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
            String name=uri.getLastPathSegment();
            if(content.toLowerCase(java.util.Locale.ROOT).contains("<a ")||content.toLowerCase(java.util.Locale.ROOT).contains("<dt><a")){
                int count=BrowserImport.importBookmarks(host,content);
                Toast.makeText(host,count+" favoritos importados",Toast.LENGTH_LONG).show();
            }else{
                int count=BrowserImport.importLogins(host,content);
                Toast.makeText(host,count+" senhas importadas de CSV",Toast.LENGTH_LONG).show();
            }
        }catch(Exception err){Toast.makeText(host,"Importação falhou: arquivo inválido ou não suportado",Toast.LENGTH_LONG).show();}
    }
    private void manageSavedLogins(){
        java.util.List<org.mozilla.geckoview.Autocomplete.LoginEntry> entries=SecureLoginStore.load(host);
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(org.mozilla.geckoview.Autocomplete.LoginEntry login:entries){
            rows.add(AppMenus.Entry.danger("Excluir "+login.username,login.origin,"trash",()->{
                new AlertDialog.Builder(host).setTitle("Excluir senha?")
                    .setMessage("Excluir a senha de "+login.username+" em "+login.origin+"?")
                    .setPositiveButton("Excluir",(d,w)->{SecureLoginStore.delete(host,login.guid);manageSavedLogins();})
                    .setNegativeButton("Cancelar",(d,w)->{}).show();
            }));
        }
        if(rows.isEmpty())rows.add(AppMenus.Entry.note("Nenhuma senha armazenada neste aplicativo."));
        AppMenus.show(host,"Senhas salvas","As senhas nunca são exibidas nesta lista.",rows);
    }
    private void showHistory(){openAddressEditor();}
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
