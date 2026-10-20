package br.com.baixafacil;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;
import java.util.function.Consumer;

/** Fachada tipada para o GeckoView. Nenhuma instância android.webkit.WebView é criada. */
final class GeckoBrowserView extends GeckoView {
    final GeckoSession session;
    private String url;
    private String title;
    private boolean canBack, canForward;
    private WebExtension.Port contentPort;
    private Consumer<String> scanCallback;
    private Consumer<String> extensionError;
    private Consumer<Integer> findResult;
    private String findTerm="";
    private boolean closed;

    GeckoBrowserView(Context context) {
        this(context, new GeckoSession());
    }
    GeckoBrowserView(Context context, GeckoSession geckoSession) {
        super(context);
        session = geckoSession;
        session.open(GeckoEngine.get(context));
        setSession(session);
        attachExtension();
    }
    String getUrl(){return url;}
    void currentUrl(String value){url=value;}
    String getTitle(){return title;}
    void pageTitle(String value){title=value;}
    boolean canGoBack(){return canBack;}
    boolean canGoForward(){return canForward;}
    void backAvailable(boolean value){canBack=value;}
    void forwardAvailable(boolean value){canForward=value;}
    void goBack(){if(canBack)session.goBack();}
    void goForward(){if(canForward)session.goForward();}
    void loadUrl(String target){session.loadUri(target);}
    void reload(){session.reload();}
    void stopLoading(){session.stop();}
    void setActive(boolean value){session.setActive(value);}
    void setFindListener(Consumer<Integer> listener){findResult=listener;}
    void findAllAsync(String query){
        findTerm=query==null?"":query;
        if(findTerm.isEmpty()){clearMatches();return;}
        session.getFinder().setDisplayFlags(GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL);
        findNext(true);
    }
    void findNext(boolean forward){
        if(findTerm.isEmpty())return;
        session.getFinder().find(findTerm,forward?GeckoSession.FINDER_FIND_FORWARD:GeckoSession.FINDER_FIND_BACKWARDS)
            .accept(result -> {if(findResult!=null) findResult.accept(result.total<=0?0:result.current*10000+result.total);},
                    error -> {if(findResult!=null)findResult.accept(0);});
    }
    void clearMatches(){findTerm="";session.getFinder().clear();}
    void cosmeticFilters(){sendCommand("cosmetic");}
    void scanVideos(boolean includeImages,Consumer<String> response){
        scanCallback=response;
        if(contentPort==null){response.accept("{}");scanCallback=null;return;}
        sendCommand("scan",includeImages);
    }
    private void sendCommand(String name){sendCommand(name,false);}
    private void sendCommand(String name,boolean images){
        if(contentPort==null)return;
        JSONObject message=new JSONObject();
        try{message.put("type",name);message.put("includeImages",images);contentPort.postMessage(message);}catch(Exception ignored){}
    }
    private void attachExtension(){
        GeckoEngine.get(getContext()).getWebExtensionController().ensureBuiltIn(
            "resource://android/assets/baixafacil_gecko/", "gecko.bridge@baixafacil.local"
        ).accept(extension -> {
            if(closed)return;
            session.getWebExtensionController().setMessageDelegate(extension, new WebExtension.MessageDelegate(){
                @Override public void onConnect(WebExtension.Port port){
                    if(closed)return;
                    contentPort=port;
                    port.setDelegate(new WebExtension.PortDelegate(){
                        @Override public void onPortMessage(Object msg, WebExtension.Port source){
                            if(source!=contentPort || !(msg instanceof JSONObject))return;
                            JSONObject payload=(JSONObject)msg;
                            if("scan".equals(payload.optString("type")) && scanCallback!=null){
                                Consumer<String> callback=scanCallback;
                                scanCallback=null;
                                // Compatibilidade do formato do callback da WebView: string JSON escapada.
                                callback.accept(JSONObject.quote(payload.optJSONObject("data")==null?"{}":payload.optJSONObject("data").toString()));
                            }
                        }
                        @Override public void onDisconnect(WebExtension.Port source){
                            if(contentPort==source)contentPort=null;
                        }
                    });
                }
            }, "baixafacil");
        }, failure -> Log.w("BaixaFacilGecko","Extensão de mídia indisponível",failure));
    }
    void destroyBrowser(){
        closed=true;
        scanCallback=null;
        if(contentPort!=null){contentPort.disconnect();contentPort=null;}
        releaseSession();
        if(session.isOpen())session.close();
    }
}
