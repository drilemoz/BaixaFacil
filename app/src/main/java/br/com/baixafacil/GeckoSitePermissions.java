package br.com.baixafacil;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import java.net.URI;
import android.widget.Toast;
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
    private final ArrayList<GeckoResult<Integer>> pendingContent=new ArrayList<>();
    private final ArrayList<MediaCallback> pendingMedia=new ArrayList<>();
    private final ArrayList<AlertDialog> dialogs=new ArrayList<>();

    GeckoSitePermissions(MainActivity host,Supplier<String> address){this.host=host;this.address=address;}
    private boolean active(){return !host.isFinishing()&&!host.isDestroyed();}
    private static boolean secureOrigin(String url){
        try{
            URI u=new URI(url);
            return "https".equalsIgnoreCase(u.getScheme()) && u.getHost()!=null && !u.getHost().isEmpty() && u.getRawUserInfo()==null;
        }catch(Exception ignored){return false;}
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
        if(!active()||!secureOrigin(url)||permissions==null||permissions.length==0||pendingAndroid!=null){callback.reject();return;}
        ArrayList<String> missing=new ArrayList<>();
        for(String permission:permissions){
            if(!allowedAndroidPermission(permission)){callback.reject();return;}
            if(host.checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED)missing.add(permission);
        }
        if(missing.isEmpty()){callback.grant();return;}
        pendingAndroid=callback;requestedAndroid=missing.toArray(new String[0]);
        try {host.requestPermissions(requestedAndroid,REQUEST_CODE);}
        catch(RuntimeException error){onRequestPermissionsResult(REQUEST_CODE,new String[0],new int[0]);}
    }
    boolean onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        if(request!=REQUEST_CODE)return false;
        Callback callback=pendingAndroid;
        pendingAndroid=null;requestedAndroid=null;
        if(callback!=null){
            boolean granted=permissions!=null&&permissions.length>0&&grants!=null&&grants.length==permissions.length;
            if(granted)for(int value:grants)if(value!=PackageManager.PERMISSION_GRANTED){granted=false;break;}
            if(granted)callback.grant();else callback.reject();
        }
        return true;
    }
    @Override public GeckoResult<Integer> onContentPermissionRequest(GeckoSession session,ContentPermission perm){
        if(perm.value==ContentPermission.VALUE_ALLOW||perm.value==ContentPermission.VALUE_DENY)
            return GeckoResult.fromValue(perm.value);
        if(perm.permission==PERMISSION_AUTOPLAY_INAUDIBLE)
            return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
        // Notificações web, DRM, acesso a terceiros/rede local e armazenamento persistente não são ativados implicitamente.
        if(perm.permission!=PERMISSION_GEOLOCATION && perm.permission!=PERMISSION_AUTOPLAY_AUDIBLE)
            return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
        if(!active()||!secureOrigin(perm.uri)||pendingContent.size()>=2)
            return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
        GeckoResult<Integer> result=new GeckoResult<>();
        pendingContent.add(result);
        String request=perm.permission==PERMISSION_GEOLOCATION?"localização":"reprodução automática com som";
        AlertDialog dialog=new AlertDialog.Builder(host)
            .setTitle("Permissão do site")
            .setMessage("Permitir "+request+" para "+origin(perm.uri)+"?\n\nA autorização é controlada pelo GeckoView; o Android pode pedir uma permissão adicional.")
            .setPositiveButton("Permitir",(d,w)->finishContent(result,ContentPermission.VALUE_ALLOW))
            .setNegativeButton("Bloquear",(d,w)->finishContent(result,ContentPermission.VALUE_DENY))
            .setOnCancelListener(d->finishContent(result,ContentPermission.VALUE_DENY)).create();
        dialogs.add(dialog);
        dialog.setOnDismissListener(d->dialogs.remove(dialog));
        dialog.show();perm.notifyShown();
        return result;
    }
    private void finishContent(GeckoResult<Integer> result,int value){
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
        if(!active()||!secureOrigin(uri)||(video==null&&audio==null)||(video!=null&&camera==null)||(audio!=null&&microphone==null)||pendingMedia.size()>=2){callback.reject();return;}
        if((camera!=null&&host.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)
            ||(microphone!=null&&host.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)){
            callback.reject();return;
        }
        pendingMedia.add(callback);
        String capability=(camera!=null?"câmera":"")+(camera!=null&&microphone!=null?" e ":"")+(microphone!=null?"microfone":"");
        AlertDialog dialog=new AlertDialog.Builder(host).setTitle("Permitir acesso à mídia?")
            .setMessage("O site "+origin(uri)+" solicita acesso a "+capability+". Permitir somente para esta solicitação?")
            .setPositiveButton("Permitir",(d,w)->{if(pendingMedia.remove(callback))callback.grant(camera,microphone);})
            .setNegativeButton("Bloquear",(d,w)->{if(pendingMedia.remove(callback))callback.reject();})
            .setOnCancelListener(d->{if(pendingMedia.remove(callback))callback.reject();}).create();
        dialogs.add(dialog);dialog.setOnDismissListener(d->dialogs.remove(dialog));dialog.show();
    }
    void cancel(){
        if(pendingAndroid!=null){Callback callback=pendingAndroid;pendingAndroid=null;requestedAndroid=null;callback.reject();}
        for(GeckoResult<Integer> result:new ArrayList<>(pendingContent))finishContent(result,ContentPermission.VALUE_DENY);
        for(MediaCallback callback:new ArrayList<>(pendingMedia)){if(pendingMedia.remove(callback))callback.reject();}
        for(AlertDialog dialog:new ArrayList<>(dialogs))dialog.dismiss();dialogs.clear();
    }
}
