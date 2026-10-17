package br.com.baixafacil;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/** Transferência própria HTTPS/HTTP. Não intercepta o DownloadManager do Android. */
public class TransferService extends Service {
    public static final String ACTION_ENQUEUE = "br.com.baixafacil.ENQUEUE";
    public static final String ACTION_PAUSE = "br.com.baixafacil.PAUSE";
    public static final String ACTION_CANCEL = "br.com.baixafacil.CANCEL";
    public static final String ACTION_PRIORITIZE = "br.com.baixafacil.PRIORITIZE";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_COOKIE = "session_cookie";
    public static final String EXTRA_USER_AGENT = "web_user_agent";
    public static final String EXTRA_REFERER = "web_referer";
    private static final String CHANNEL = "download_direto_v1";
    private static final int FOREGROUND_ID = 100;
    private static final Set<String> ACTIVE = ConcurrentHashMap.newKeySet();

    public static boolean isActive(String id) { return id != null && ACTIVE.contains(id); }

    // Uma transferência ativa e uma fila em memória por vez. Nunca escrever
    // credenciais de sessão no histórico nem na fila persistente.
    private static volatile TransferService live;
    private final Object queueLock = new Object();
    private final PendingQueue<Job> waiting = new PendingQueue<>();
    private final Set<String> pauseRequests = ConcurrentHashMap.newKeySet();
    private final Set<String> cancelRequests = ConcurrentHashMap.newKeySet();
    private volatile String runningId;
    private volatile DownloadEngine currentEngine;
    private boolean draining;
    private ExecutorService executor;
    private NotificationManager notifications;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable stopWhenIdle = () -> {
        synchronized (queueLock) {
            if (!draining && waiting.size() == 0 && runningId == null) stopSelf();
        }
    };

    private static final class Job {
        final String id, url, cookie, userAgent, referer;
        Job(String id, String url, String cookie, String userAgent, String referer) {
            this.id = id;
            this.url = url;
            this.cookie = cookie;
            this.userAgent = userAgent;
            this.referer = referer;
        }
    }

    public static int queuePosition(String id) {
        TransferService service = live;
        if (service == null || id == null) return -1;
        synchronized (service.queueLock) { return service.waiting.position(id); }
    }

    @Override public void onCreate() {
        super.onCreate();
        live = this;
        executor = Executors.newSingleThreadExecutor();
        notifications = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Transferências iniciadas por você");
        notifications.createNotificationChannel(channel);
        // A fábrica de notificações de progresso pertence ao DownloadEngine.
        // O Service deve construir a sua própria notificação inicial de foreground.
        // Isto evita chamar o método notification(...) removido na alpha.15.
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Notification startingNotice = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Baixa Fácil")
                .setContentText("Preparando transferência")
                .setContentIntent(android.app.PendingIntent.getActivity(this, 0, openApp,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
        startForeground(FOREGROUND_ID, startingNotice,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { mainHandler.postDelayed(stopWhenIdle, 1000); return START_NOT_STICKY; }
        String id = intent.getStringExtra(EXTRA_ID);
        String action = intent.getAction();
        if (id == null || DownloadStore.get(this, id) == null) {
            mainHandler.postDelayed(stopWhenIdle, 1000);
            return START_NOT_STICKY;
        }
        if (ACTION_PAUSE.equals(action) || ACTION_CANCEL.equals(action) || ACTION_PRIORITIZE.equals(action)) {
            control(id, action);
            return START_NOT_STICKY;
        }
        String url = intent.getStringExtra(EXTRA_URL);
        if (!ACTION_ENQUEUE.equals(action) || UrlTools.extract(url) == null) {
            mainHandler.postDelayed(stopWhenIdle, 1000);
            return START_NOT_STICKY;
        }
        String cookie = TransferHttp.safeHeader(intent.getStringExtra(EXTRA_COOKIE), 8192);
        String userAgent = TransferHttp.safeHeader(intent.getStringExtra(EXTRA_USER_AGENT), 512);
        String referer = TransferHttp.safeHeader(intent.getStringExtra(EXTRA_REFERER), 2048);
        if (!ACTIVE.add(id)) return START_NOT_STICKY;
        pauseRequests.remove(id);
        cancelRequests.remove(id);
        mainHandler.removeCallbacks(stopWhenIdle);
        synchronized (queueLock) {
            DownloadStore.update(this, id, "Na fila", null, -1, -2, null, "");
            waiting.offer(id, new Job(id, url, cookie, userAgent, referer));
            if (!draining) {
                draining = true;
                try { executor.execute(this::drainQueue); }
                catch (RuntimeException error) {
                    draining = false;
                    waiting.remove(id);
                    ACTIVE.remove(id);
                    DownloadStore.update(this, id, "Falhou", null, -1, -2, null,
                            "Não foi possível enfileirar a transferência.");
                }
            }
        }
        return START_NOT_STICKY;
    }

    private void control(String id, String action) {
        if (ACTION_PRIORITIZE.equals(action)) {
            synchronized (queueLock) { waiting.moveNext(id); }
            return;
        }
        boolean cancel = ACTION_CANCEL.equals(action);
        if (cancel) cancelRequests.add(id);
        else pauseRequests.add(id);
        boolean queued;
        boolean processing;
        synchronized (queueLock) {
            queued = waiting.remove(id) != null;
            processing = id.equals(runningId);
            if (queued) ACTIVE.remove(id);
        }
        if (queued) {
            if (cancel) discardPartial(id);
            else DownloadStore.update(this, id, "Pausado", null, -1, -2, null, "Pausado antes de iniciar.");
            cancelRequests.remove(id);
            pauseRequests.remove(id);
        } else if (processing) {
            DownloadEngine engine = currentEngine;
            if (engine != null) engine.abort();
        } else {
            cancelRequests.remove(id);
            pauseRequests.remove(id);
        }
        mainHandler.removeCallbacks(stopWhenIdle);
        mainHandler.postDelayed(stopWhenIdle, 3000);
    }

    private void drainQueue() {
        while (true) {
            Job job;
            synchronized (queueLock) {
                job = waiting.poll();
                if (job == null) { draining = false; runningId = null; break; }
                runningId = job.id;
            }
            try {
                downloadWithRetries(job.id, job.url, job.cookie, job.userAgent, job.referer);
            } finally {
                if (cancelRequests.contains(job.id)) discardPartial(job.id);
                else if (pauseRequests.contains(job.id)) {
                    DownloadStore.update(this, job.id, "Pausado", null, -1, -2, null,
                            "Pausado pelo usuário. Toque para retomar.");
                }
                pauseRequests.remove(job.id);
                cancelRequests.remove(job.id);
                ACTIVE.remove(job.id);
                currentEngine = null;
                synchronized (queueLock) { runningId = null; }
            }
        }
        mainHandler.postDelayed(stopWhenIdle, 3000);
    }

    private void discardPartial(String id) {
        new DownloadEngine(this, FOREGROUND_ID, () -> false).discardPartial(id);
    }

    private void downloadWithRetries(String id, String address, String cookie, String userAgent, String referer) {
        DownloadEngine runner = new DownloadEngine(this, FOREGROUND_ID,
                () -> pauseRequests.contains(id) || cancelRequests.contains(id));
        currentEngine = runner;
        try { runner.downloadWithRetries(id, address, cookie, userAgent, referer); }
        finally { currentEngine = null; }
    }

    @Override public void onDestroy() {
        mainHandler.removeCallbacks(stopWhenIdle);
        synchronized (queueLock) {
            String id;
            while ((id = waiting.pollId()) != null) {
                DownloadStore.update(this, id, "Interrompido", null, -1, -2, null,
                        "Fila interrompida pelo Android. Retome manualmente.");
                ACTIVE.remove(id);
            }
        }
        if (runningId != null) pauseRequests.add(runningId);
        DownloadEngine engine = currentEngine;
        if (engine != null) engine.abort();
        executor.shutdownNow();
        live = null;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
