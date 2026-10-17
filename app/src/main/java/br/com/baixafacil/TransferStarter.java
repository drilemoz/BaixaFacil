package br.com.baixafacil;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

/** Ações sempre originadas por gesto do usuário em uma Activity visível. */
final class TransferStarter {
    private TransferStarter() {}

    static boolean enqueue(Activity activity,String id,String url,String cookie,String agent,String referer) {
        String restriction=DownloadRestrictions.blocked(activity);
        if(restriction!=null) {
            DownloadStore.update(activity,id,"Pausado",null,-1,-2,null,restriction);
            return false;
        }
        if(Build.VERSION.SDK_INT >= 34) {
            if(TransferJobService.enqueue(activity,id,url,cookie,agent,referer)) return true;
            // Fallback explícito para FGS caso o Android recuse o JobScheduler.
        }
        Intent request=new Intent(activity,TransferService.class);
        request.setAction(TransferService.ACTION_ENQUEUE);
        request.putExtra(TransferService.EXTRA_ID,id);
        request.putExtra(TransferService.EXTRA_URL,url);
        if(cookie!=null) request.putExtra(TransferService.EXTRA_COOKIE,cookie);
        if(agent!=null) request.putExtra(TransferService.EXTRA_USER_AGENT,agent);
        if(referer!=null) request.putExtra(TransferService.EXTRA_REFERER,referer);
        try { activity.startForegroundService(request);return true; }
        catch(RuntimeException error){
            DownloadStore.update(activity,id,"Interrompido",null,-1,-2,null,
                "Android bloqueou o início da transferência. Retome com app aberto.");
            return false;
        }
    }
    static void control(Activity activity,String id,String action) {
        if(Build.VERSION.SDK_INT>=34 && !TransferService.isActive(id) &&
                (TransferJobService.isActive(id) || TransferJobService.queuePosition(activity,id)>0
                    || (TransferJobService.hasScheduledJob(activity)
                        && "Na fila".equals(DownloadStore.get(activity,id)==null?"":DownloadStore.get(activity,id).optString("status"))))) {
            TransferJobService.control(activity,id,action);return;
        }
        Intent request=new Intent(activity,TransferService.class);
        request.setAction(action);
        request.putExtra(TransferService.EXTRA_ID,id);
        try { activity.startForegroundService(request); }
        catch(RuntimeException ignored) {
            DownloadStore.update(activity,id,"Interrompido",null,-1,-2,null,"Comando não executado.");
        }
    }
    static boolean isActive(String id) { return TransferService.isActive(id)||TransferJobService.isActive(id); }
    static int queuePosition(Activity activity,String id) {
        int legacy=TransferService.queuePosition(id);
        return legacy>0?legacy:TransferJobService.queuePosition(activity,id);
    }
}
