package br.com.baixafacil;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import java.net.URI;
import java.util.IdentityHashMap;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Permissões por origem HTTPS, sem concessão silenciosa de câmera/microfone/localização. */
final class GeckoSitePermissions implements GeckoSession.PermissionDelegate {
    static final int REQUEST_CODE=6455;
    private final MainActivity host;
    private final Supplier<String> address;
    private Callback pendingAndroid;
    private String[] requestedAndroid;
    private String requestedAndroidOrigin;
    private final ArrayList<GeckoResult<Integer>> pendingContent=new ArrayList<>();
    private final ArrayList<MediaCallback> pendingMedia=new ArrayList<>();
    private final ArrayList<AlertDialog> dialogs=new ArrayList<>();
    private final IdentityHashMap<GeckoResult<Integer>,String> contentOrigins=new IdentityHashMap<>();
    private final IdentityHashMap<MediaCallback,String> mediaOrigins=new IdentityHashMap<>();
    private final IdentityHashMap<AlertDialog,String> dialogOrigins=new IdentityHashMap<>();

    GeckoSitePermissions(MainActivity host,Supplier<String> address){this.host=host;this.address=address;}
    private boolean active(){return !host.isFinishing()&&!host.isDestroyed();}
    private static boolean secureOrigin(String url){return GeckoPermissionPolicy.origin(url)!=null;}
    private boolean activeFor(String origin){
        return active() && GeckoPermissionPolicy.sameOrigin(origin,address.get());
    }
    /** Rejeita solicitações ainda pendentes quando muda a origem do documento principal. */
    void onPageLocationChanged(String destination){
        if(pendingAndroidOrigin!=null && !GeckoPermissionPolicy.sameOrigin(pendingAndroidOrigin,destination)){
            Callback previous=pendingAndroid;
            pendingAndroid=null;requestedAndroid=null;requestedAndroidOrigin=null;
            if(previous!=null)previous.reject();
        }
        for(GeckoResult<Integer> result:new ArrayList<>(pendingContent)){
            if(!GeckoPermissionPolicy.sameOrigin(contentOrigins.get(result),destination))
                finishContent(result,ContentPermission.VALUE_DENY);
        }
        for(MediaCallback callback:new ArrayList<>(pendingMedia)){
            if(!GeckoPermissionPolicy.sameOrigin(mediaOrigins.get(callback),destination)){
                pendingMedia.remove(callback);mediaOrigins.remove(callback);callback.reject();
            }
        }
        // Não interromper consentimento após navegação para outra página da mesma origem.
        for(AlertDialog dialog:new ArrayList<>(dialogs)){
            if(!GeckoPermissionPolicy.sameOrigin(dialogOrigins.get(dialog),destination))dialog.dismiss();
        }
        dialogs.removeIf(dialog->!dialog.isShowing());
    }
    private static String origin(String url){
        try{return new URI(url).getHost();}catch(Exception error){return "site desconhecido";} // Nenhum path, query ou token exibido.
    }
    private static boolean allowedAndroidPermission(String permission){
        return Manifest.permission.CAMERA.equals(permission)
            || Manifest.permission.RECORD_AUDIO.equals(permission)
            || Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)
            || Manifest.permission.ACCESS_FINE_LOCATION.equals(permission);
    }
    @Override public void onAndroidPermissionsRequest(GeckoSession session,String[] permissions,Callback callback){
        String url=address.get();
        if(!activeFor(url)||permissions==null||permissions.length==0||pendingAndroid!=null){callback.reject();return;}
        ArrayList<String> missing=new ArrayList<>();
        for(String permission:permissions){
            if(!allowedAndroidPermission(permission)){callback.reject();return;}
            if(host.checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED)missing.add(permission);
        }
        if(missing.isEmpty()){callback.grant();return;}
        pendingAndroid=callback;requestedAndroid=missing.toArray(new String[0]);
        requestedAndroidOrigin=GeckoPermissionPolicy.origin(url);
        try {host.requestPermissions(requestedAndroid,REQUEST_CODE);}
        catch(RuntimeException error){onRequestPermissionsResult(REQUEST_CODE,new String[0],new int[0]);}
    }
    boolean onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        if(request!=REQUEST_CODE)return false;
        Callback callback=pendingAndroid;
        String requestedOrigin=requestedAndroidOrigin;
        String[] expectedPermissions=requestedAndroid;
        pendingAndroid=null;requestedAndroid=null;requestedAndroidOrigin=null;
        if(callback!=null){
            boolean granted=permissions!=null && requestedOrigin!=null && expectedPermissions!=null &&
                permissions.length==expectedPermissions.length && grants!=null && grants.length==permissions.length;
            if(granted)for(int i=0;i<permissions.length;i++){
                if(!permissions[i].equals(expectedPermissions[i]) || grants[i]!=PackageManager.PERMISSION_GRANTED){
                    granted=false;break;
                }
            }
            if(granted && activeFor(requestedOrigin))callback.grant();else callback.reject();
        }
        return true;
    }
    @Override public GeckoResult<Integer> onContentPermissionRequest(GeckoSession session,ContentPermission perm){
        // Frames de terceiros não herdam o consentimento do documento principal.
        if(!activeFor(perm.uri))return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
        if(perm.value==ContentPermission.VALUE_ALLOW||perm.value==ContentPermission.VALUE_DENY)
            return GeckoResult.fromValue(perm.value);
        if(perm.permission==PERMISSION_AUTOPLAY_INAUDIBLE)
            return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
        // Notificações web, DRM, acesso a terceiros/rede local e armazenamento persistente não são ativados implicitamente.
        if(perm.permission!=PERMISSION_GEOLOCATION && perm.permission!=PERMISSION_AUTOPLAY_AUDIBLE)
            return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
        if(pendingContent.size()>=2)
            return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
        GeckoResult<Integer> result=new GeckoResult<>();
        pendingContent.add(result);contentOrigins.put(result,GeckoPermissionPolicy.origin(perm.uri));
        String request=perm.permission==PERMISSION_GEOLOCATION?"localização":"reprodução automática com som";
        AlertDialog dialog=SitePermissionDialog.show(host,"Permissão do site",origin(perm.uri),
            "O site solicita "+request+". O Android poderá exigir uma permissão adicional.",
            ()->finishContent(result,activeFor(perm.uri)?ContentPermission.VALUE_ALLOW:ContentPermission.VALUE_DENY),
            ()->finishContent(result,ContentPermission.VALUE_DENY),
            ()->cleanupDialogs());
        dialogs.add(dialog);dialogOrigins.put(dialog,GeckoPermissionPolicy.origin(perm.uri));
        perm.notifyShown();
        return result;
    }
    private void finishContent(GeckoResult<Integer> result,int value){
        contentOrigins.remove(result);
        if(pendingContent.remove(result))result.complete(value);
    }
    private static MediaSource firstSource(MediaSource[] sources,int type){
        if(sources==null)return null;
        for(MediaSource source:sources){
            if(source!=null && source.source==MediaSource.SOURCE_SCREEN)continue;
            if(source!=null&&source.source==type)return source;
        }
        return null;
    }
    @Override public void onMediaPermissionRequest(GeckoSession session,String uri,MediaSource[] video,MediaSource[] audio,MediaCallback callback){
        // Nunca compartilhar tela nem áudio interno usando esta permissão.
        MediaSource camera=video==null?null:firstSource(video,MediaSource.SOURCE_CAMERA);
        MediaSource microphone=audio==null?null:firstSource(audio,MediaSource.SOURCE_MICROPHONE);
        if(!activeFor(uri)||(video==null&&audio==null)||(video!=null&&camera==null)||(audio!=null&&microphone==null)||pendingMedia.size()>=2){callback.reject();return;}
        if((camera!=null&&host.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            ||(microphone!=null&&host.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)){
            callback.reject();return;
        }
        pendingMedia.add(callback);mediaOrigins.put(callback,GeckoPermissionPolicy.origin(uri));
        String capability=(camera!=null?"câmera":"")+(camera!=null&&microphone!=null?" e ":"")+(microphone!=null?"microfone":"");
        AlertDialog dialog=SitePermissionDialog.show(host,"Permitir acesso à mídia?",origin(uri),
            "Este site pede acesso a "+capability+". Permitir somente para esta solicitação?",
            ()->{mediaOrigins.remove(callback);if(pendingMedia.remove(callback)){
                if(activeFor(uri))callback.grant(camera,microphone);else callback.reject();
            }},
            ()->{mediaOrigins.remove(callback);if(pendingMedia.remove(callback))callback.reject();},
            ()->cleanupDialogs());
        dialogs.add(dialog);dialogOrigins.put(dialog,GeckoPermissionPolicy.origin(uri));
    }
    private void cleanupDialogs(){
        dialogs.removeIf(x->!x.isShowing());
        dialogOrigins.keySet().removeIf(x->!x.isShowing());
    }
    void cancel(){
        if(pendingAndroid!=null){Callback callback=pendingAndroid;pendingAndroid=null;requestedAndroid=null;requestedAndroidOrigin=null;callback.reject();}
        for(GeckoResult<Integer> result:new ArrayList<>(pendingContent))finishContent(result,ContentPermission.VALUE_DENY);
        for(MediaCallback callback:new ArrayList<>(pendingMedia)){
            mediaOrigins.remove(callback);if(pendingMedia.remove(callback))callback.reject();
        }
        for(AlertDialog dialog:new ArrayList<>(dialogs))dialog.dismiss();
        dialogs.clear();dialogOrigins.clear();
    }
}
