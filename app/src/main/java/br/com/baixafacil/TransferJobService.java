package br.com.baixafacil;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android 14+: JobScheduler UIDT, disparado apenas por gesto explícito do usuário.
 * Usa o mesmo motor HTTP do serviço clássico, com fila serial persistida.
 * Não persiste cookies ou cabeçalhos de sessão. */
public final class TransferJobService extends JobService {
    static final int JOB_ID = 241514;
    private static final int NOTICE_ID = 110;
    private static final String CHANNEL = "download_direto_v1";
    private static final ConcurrentHashMap<String, Headers> HEADERS = new ConcurrentHashMap<>();
    private static volatile TransferJobService live;
    private volatile DownloadEngine runner;
    private volatile String runningId;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private volatile Thread thread;

    private static final class Headers {
        final String cookie, userAgent, referer;
        Headers(String c, String u, String r) {cookie=c;userAgent=u;referer=r;}
    }
    static boolean supports() { return Build.VERSION.SDK_INT >= 34; }
    static boolean isActive(String id) {
        if (id == null) return false;
        TransferJobService active=live;
        if(active != null && id.equals(active.runningId)) return true;
        return false;
    }
    static int queuePosition(Context ctx, String id) {
        TransferJobService active=live;
        if(id == null || (!hasScheduledJob(ctx) && active == null)) return -1;
        if(active != null && id.equals(active.runningId)) return 0;
        int pos=1;
        for(JSONObject entry:DownloadStore.queued(ctx)) {
            if(id.equals(entry.optString("id"))) return pos;
            pos++;
        }
        return -1;
    }
    static boolean hasScheduledJob(Context ctx) {
        if(!supports()) return false;
        if(live != null) return true;
        JobScheduler scheduler=ctx.getSystemService(JobScheduler.class);
        return scheduler != null && scheduler.getPendingJob(JOB_ID) != null;
    }
    /** Agendar apenas enquanto uma Activity está visível. */
    static boolean enqueue(Context context,String id,String url,String cookie,String agent,String referer) {
        if(!supports()) return false;
        String checked=UrlTools.extract(url);
        if(checked==null || !checked.equals(url) || DownloadStore.get(context,id)==null) return false;
        HEADERS.put(id,new Headers(TransferHttp.safeHeader(cookie,8192),
                TransferHttp.safeHeader(agent,512),TransferHttp.safeHeader(referer,2048)));
        DownloadStore.queue(context,id);
        if(hasScheduledJob(context)) return true;
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);
        if(scheduler == null) return false;
        JobInfo job=new JobInfo.Builder(JOB_ID,new ComponentName(context,TransferJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setUserInitiated(true)
                .setExtras(new PersistableBundle())
                .build();
        try {
            if(scheduler.schedule(job)==JobScheduler.RESULT_SUCCESS) return true;
        }catch(SecurityException | IllegalArgumentException | IllegalStateException error) { /* chamada restrita pelo SO */ }
        HEADERS.remove(id);
        DownloadStore.update(context,id,"Interrompido",null,-1,-2,null,
                "Android não permitiu iniciar o job agora. Retome com o app aberto.");
        return false;
    }
    static void control(Context context,String id,String action) {
        TransferJobService active=live;
        if(TransferService.ACTION_PRIORITIZE.equals(action)) {
            DownloadStore.prioritize(context,id);
            return;
        }
        boolean cancel=TransferService.ACTION_CANCEL.equals(action);
        DownloadStore.update(context,id,cancel?"Cancelado":"Pausado",null,-1,-2,null,
                cancel?"Cancelamento solicitado":"Pausado pelo usuário");
        HEADERS.remove(id);
        if(active!=null && id.equals(active.runningId)) {
            active.runnerStopped.set(true);
            DownloadEngine engine=active.runner;
            if(engine!=null) engine.abort();
            return;
        }
        if(cancel) new DownloadEngine(context,NOTICE_ID,()->false).discardPartial(id);
    }
    private final AtomicBoolean runnerStopped=new AtomicBoolean();
    @Override public boolean onStartJob(JobParameters parameters) {
        NotificationManager manager=getSystemService(NotificationManager.class);
        if(manager!=null) manager.createNotificationChannel(new NotificationChannel(
                CHANNEL,"Downloads",NotificationManager.IMPORTANCE_LOW));
        Notification notification=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Baixa Fácil")
                .setContentText("Transferência iniciada por você")
                .setOngoing(true).build();
        setNotification(parameters,NOTICE_ID,notification,JOB_END_NOTIFICATION_POLICY_REMOVE);
        stopped.set(false);
        live=this;
        thread=new Thread(()->{
            try {
                while(!stopped.get()) {
                    List<JSONObject> queued=DownloadStore.queued(this);
                    if(queued.isEmpty()) break;
                    JSONObject entry=queued.get(0);
                    String id=entry.optString("id");
                    String url=entry.optString("url");
                    if(!TransferHttp.isWebUrl(url)) {
                        DownloadStore.update(this,id,"Falhou",null,-1,-2,null,"URL inválida");
                        continue;
                    }
                    String restriction=DownloadRestrictions.blocked(this);
                    if(restriction!=null) {
                        DownloadStore.update(this,id,"Pausado",null,-1,-2,null,restriction);
                        HEADERS.remove(id);
                        continue;
                    }
                    runningId=id;
                    runnerStopped.set(false);
                    Headers head=HEADERS.remove(id);
                    DownloadEngine engine=new DownloadEngine(this,NOTICE_ID,
                            ()->stopped.get()||runnerStopped.get(),
                            notice -> {
                                if(!stopped.get()) setNotification(parameters,NOTICE_ID,notice,
                                        JOB_END_NOTIFICATION_POLICY_REMOVE);
                            });
                    runner=engine;
                    try {
                        engine.downloadWithRetries(id,url,
                            head==null?null:head.cookie,
                            head==null?null:head.userAgent,
                            head==null?null:head.referer);
                    } finally {
                        runner=null;
                        if(stopped.get()) {
                            JSONObject now=DownloadStore.get(this,id);
                            if(now!=null && !"Concluído".equals(now.optString("status"))
                                    && !"Cancelado".equals(now.optString("status"))) {
                                DownloadStore.update(this,id,"Interrompido",null,-1,-2,null,
                                        "Job interrompido pelo Android. Retome manualmente.");
                            }
                        }else if(runnerStopped.get()) {
                            JSONObject now=DownloadStore.get(this,id);
                            if(now!=null && "Concluído".equals(now.optString("status"))) {
                                // Se a transferência terminou antes do comando, não despublicar.
                            } else if(now!=null && "Cancelado".equals(now.optString("status"))) {
                                engine.discardPartial(id);
                            } else {
                                DownloadStore.update(this,id,"Pausado",null,-1,-2,null,
                                        "Pausado. Toque para retomar.");
                            }
                        }
                        runningId=null;
                    }
                }
            } finally {
                live=null;
                if(!stopped.get()) jobFinished(parameters,false);
            }
        },"uidt-transfer");
        thread.start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        stopped.set(true);
        DownloadEngine engine=runner;
        if(engine!=null)engine.abort();
        Thread t=thread;
        if(t!=null)t.interrupt();
        // Nunca reagendar automaticamente depois que o usuário/Android interromper.
        for(JSONObject e:DownloadStore.queued(this)) {
            DownloadStore.update(this,e.optString("id"),"Interrompido",null,-1,-2,null,
                    "Fila interrompida. Recoloque manualmente.");
        }
        HEADERS.clear();
        live=null;
        return false;
    }
    @Override public void onDestroy() {
        stopped.set(true);
        DownloadEngine engine=runner;
        if(engine!=null)engine.abort();
        HEADERS.clear();
        if(live==this)live=null;
        super.onDestroy();
    }
}
