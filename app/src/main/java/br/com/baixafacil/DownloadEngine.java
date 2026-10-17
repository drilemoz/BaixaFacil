package br.com.baixafacil;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Motor HTTP único, compartilhado pelo serviço clássico e pelos jobs UIDT.
 * Nunca grava cookies nem referer de sessão no histórico persistente. */
final class DownloadEngine {
    private static final String CHANNEL = "download_direto_v1";
    private final Context context;
    private final NotificationManager notifications;
    private final int progressNotificationId;
    private final BooleanSupplier stopCheck;
    private final Consumer<Notification> progressUpdate;
    private volatile boolean stopped;
    private volatile HttpURLConnection currentConnection;
    private volatile long lastRestrictionCheck;

    DownloadEngine(Context context, int progressNotificationId, BooleanSupplier stopCheck) {
        this(context, progressNotificationId, stopCheck, null);
    }
    DownloadEngine(Context context, int progressNotificationId, BooleanSupplier stopCheck,
                   Consumer<Notification> progressUpdate) {
        this.context = context.getApplicationContext();
        this.notifications = context.getSystemService(NotificationManager.class);
        this.progressNotificationId = progressNotificationId;
        this.stopCheck = stopCheck;
        this.progressUpdate = progressUpdate;
    }
    private void progress(Notification notice) {
        if(progressUpdate != null) progressUpdate.accept(notice);
        else notifications.notify(progressNotificationId,notice);
    }
    void abort() {
        stopped = true;
        HttpURLConnection connection = currentConnection;
        if (connection != null) connection.disconnect();
    }
    private boolean stopRequested(String id) {
        return stopped || (stopCheck != null && stopCheck.getAsBoolean()) || Thread.currentThread().isInterrupted();
    }

    private void checkStop(String id) throws IOException {
        if (stopRequested(id)) throw new IOException("Transferência interrompida pelo usuário ou Android.");
        long now=android.os.SystemClock.elapsedRealtime();
        if (now-lastRestrictionCheck >= 2500 || lastRestrictionCheck == 0) {
            lastRestrictionCheck=now;
            String restricted=DownloadRestrictions.blocked(context);
            if (restricted != null) throw new IOException(restricted);
        }
    }

    private boolean waitRetry(String id, long millis) {
        long until = System.nanoTime() + millis * 1_000_000L;
        while (!stopRequested(id)) {
            long remaining = (until - System.nanoTime()) / 1_000_000L;
            if (remaining <= 0) return true;
            try { Thread.sleep(Math.min(remaining, 200)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        }
        return false;
    }

    void discardPartial(String id) {
        JSONObject item = DownloadStore.get(context, id);
        if (item == null) return;
        // Se terminou antes do comando de cancelar, preservar o arquivo concluído.
        if ("Concluído".equals(item.optString("status"))) return;
        String saved = item.optString("uri");
        if (!saved.isEmpty()) {
            try {
                if (context.getContentResolver().delete(Uri.parse(saved), null, null) != 1)
                    throw new IOException("Fragmento não removido");
            } catch (Exception e) {
                DownloadStore.update(context, id, "Falhou", null, -1, -2, null,
                        "Não foi possível remover o arquivo parcial: " + e.getClass().getSimpleName());
                return;
            }
        }
        DownloadStore.update(context, id, "Cancelado", null, 0, -1, "", "Cancelado e fragmento descartado.");
        DownloadStore.setMeta(context, id, "", "", 0, "");
    }

    void downloadWithRetries(String id, String address, String cookie, String userAgent, String referer) {
        for (int attempt = 1; attempt <= TransferPolicy.MAX_ATTEMPTS; attempt++) {
            if (stopRequested(id)) return;
            String restriction=DownloadRestrictions.blocked(context);
            if (restriction != null) {
                DownloadStore.update(context,id,"Pausado",null,-1,-2,null,restriction);
                return;
            }
            DownloadStore.setMeta(context, id, null, null, attempt, "");
            DownloadStore.update(context, id, "Conectando", null, -1, -2, null, "");
            try {
                transferOnce(id, address, cookie, userAgent, referer);
                return;
            } catch (Exception ex) {
                if (stopRequested(id)) return;
                // `restriction` já foi declarada no laço: não redeclarar dentro do catch.
                String restrictionAfterFailure = DownloadRestrictions.blocked(context);
                if (restrictionAfterFailure != null) {
                    DownloadStore.update(context,id,"Pausado",null,-1,-2,null,restrictionAfterFailure);
                    return;
                }
                TransferDiagnostics.Failure diagnosis = TransferDiagnostics.classify(ex);
                boolean canRetry = diagnosis.retryable && attempt < TransferPolicy.MAX_ATTEMPTS
                        && !stopRequested(id);
                DownloadStore.setMeta(context, id, null, null, attempt, diagnosis.code);
                JSONObject snapshot = DownloadStore.get(context, id);
                String name = snapshot == null ? "Download" : snapshot.optString("name", "Download");
                String detail = diagnosis.code + " • " + diagnosis.getMessage();
                if (canRetry) {
                    int delaySeconds = (int) (TransferPolicy.retryDelayMillis(attempt) / 1000);
                    DownloadStore.update(context, id, "Nova tentativa", null, -1, -2, null,
                            detail + " Próxima tentativa em " + delaySeconds + " s.");
                    progress(notification(name, "Tentativa " + (attempt + 1) + "/4 em " + delaySeconds + " s", true, 0, -1, false));
                    if (!waitRetry(id, TransferPolicy.retryDelayMillis(attempt))) return;
                } else {
                    String status = "Falhou";
                    DownloadStore.update(context, id, status, null, -1, -2, null, detail);
                    notifications.notify(notificationId(id),
                            notification("Falha no download (" + diagnosis.code + ")", diagnosis.getMessage(), false, 0, -1, true));
                    return;
                }
            }
        }
    }

    private Uri ensureDestination(String id, String name, String mime) throws IOException {
        JSONObject current = DownloadStore.get(context, id);
        if (current != null && !current.optString("uri").isEmpty()) {
            Uri saved = Uri.parse(current.optString("uri"));
            try (ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(saved, "rw")) {
                if (descriptor != null) return saved;
            } catch (Exception unusable) {
                // Registro antigo não pode mais ser aberto. Criar novo arquivo, sem reutilizar bytes desconhecidos.
                DownloadStore.update(context, id, null, null, 0, -2, "", null);
                DownloadStore.setMeta(context, id, "", "", -1, null);
            }
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri created = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (created == null) throw new TransferDiagnostics.Failure("ARMAZENAMENTO", "Não foi possível criar arquivo em Download/.", false);
        // Registrar o URI antes de escrever para que uma falha não perca a referência.
        DownloadStore.update(context, id, null, name, 0, -2, created.toString(), null);
        return created;
    }

    private String typeOrDefault(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) return "application/octet-stream";
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return type.isEmpty() ? "application/octet-stream" : type;
    }

    private String determineName(HttpURLConnection connection, String mime) {
        String name = FileNames.choose(connection.getHeaderField("Content-Disposition"),
                connection.getURL().toString(), mime);
        if (!name.contains(".")) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null && !ext.trim().isEmpty() && !mime.equals("application/octet-stream")) name += "." + ext;
        }
        return name;
    }

    private void transferOnce(String id, String address, String cookie, String userAgent, String referer) throws Exception {
        HttpURLConnection conn = null;
        try {
            JSONObject existing = DownloadStore.get(context, id);
            if (existing == null) throw new TransferDiagnostics.Failure("REGISTRO", "Download não encontrado.", false);
            String savedUriText = existing.optString("uri");
            String oldEtag = existing.optString("etag");
            String oldModified = existing.optString("lastModified");
            Uri destination = null;
            // Garantir que o URI salvo ainda seja acessível. Não usar arquivos privados de outros apps.
            if (!savedUriText.isEmpty()) {
                try {
                    destination = Uri.parse(savedUriText);
                    try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(destination, "rw")) {
                        if (pfd == null) throw new IOException("Arquivo parcial inacessível");
                    }
                } catch (Exception notFound) {
                    destination = null;
                    DownloadStore.update(context, id, null, null, 0, -2, "", null);
                    DownloadStore.setMeta(context, id, "", "", -1, null);
                }
            }
            long offset = 0;
            if (destination != null) {
                try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(destination, "rw")) {
                    if (pfd == null) throw new IOException("Arquivo parcial inacessível");
                    offset = pfd.getStatSize();
                    if (offset < 0) offset = 0;
                }
            }
            // Sem um validador HTTP é mais seguro reiniciar do que combinar versões distintas.
            String validator = TransferPolicy.ifRange(oldEtag, oldModified);
            if (validator == null) offset = 0;

            // Redirecionamentos controlados impedem vazamento de cookies entre origens
            // e downgrade silencioso de HTTPS para HTTP.
            checkStop(id);
            conn = openHttpConnection(id, address, offset, validator, cookie, userAgent, referer);
            checkStop(id);
            int http = conn.getResponseCode();
            if (offset > 0 && http == 416) {
                // A cópia pode ter mudado. Zerá-la permite GET completo na próxima tentativa.
                truncateFile(destination);
                DownloadStore.update(context, id, null, null, 0, -2, null, null);
                DownloadStore.setMeta(context, id, "", "", -1, null);
                throw new TransferDiagnostics.Failure("RANGE_416", "Servidor rejeitou retomada; reiniciando arquivo.", true);
            }
            if (http < 200 || http >= 300) throw TransferDiagnostics.http(http);
            if (http == 204) throw new TransferDiagnostics.Failure("HTTP_204", "Servidor não enviou arquivo.", false);
            if (http != 200 && http != 206) throw new TransferDiagnostics.Failure("HTTP", "Resposta HTTP não suportada: " + http, false);
            String encoding = conn.getContentEncoding();
            if (encoding != null && !encoding.trim().isEmpty() && !"identity".equalsIgnoreCase(encoding)) {
                throw new TransferDiagnostics.Failure("CODIFICACAO", "Resposta HTTP compactada: bytes não verificáveis para retomada.", false);
            }

            long count = 0;
            long length = conn.getContentLengthLong();
            long total = length;
            boolean resumed = offset > 0 && http == 206;
            if (resumed) {
                // Mesmo com If-Range, conferir cabeçalhos adicionais se o servidor fornecê-los.
                String responseEtag = conn.getHeaderField("ETag");
                String responseModified = conn.getHeaderField("Last-Modified");
                if (responseEtag != null && !oldEtag.isEmpty() && !responseEtag.equals(oldEtag)) {
                    throw new TransferDiagnostics.Failure("ARQUIVO_ALTERADO", "ETag mudou durante a retomada.", false);
                }
                if (responseModified != null && !oldModified.isEmpty()
                        && !responseModified.equals(oldModified)) {
                    throw new TransferDiagnostics.Failure("ARQUIVO_ALTERADO", "Last-Modified mudou durante a retomada.", false);
                }
                total = TransferPolicy.rangeTotal(conn.getHeaderField("Content-Range"), offset, length);
                if (total < 0) throw new TransferDiagnostics.Failure("RANGE_INVALIDO", "Content-Range não corresponde ao arquivo parcial.", false);
                count = offset;
            } else if (http == 206) {
                throw new TransferDiagnostics.Failure("RANGE_INVALIDO", "Servidor enviou resposta parcial sem solicitação válida.", false);
            }
            String mime = typeOrDefault(conn.getContentType());
            String name = determineName(conn, mime);
            if (destination == null) destination = ensureDestination(id, name, mime);
            else {
                // Conservar nome da primeira tentativa; evitar alterar arquivo pela versão atual do servidor.
                JSONObject saved = DownloadStore.get(context, id);
                name = saved == null ? name : saved.optString("name", name);
            }
            DownloadStore.update(context, id, resumed ? "Retomando" : "Baixando", name, count, total, null, "");
            progress(notification(name, resumed ? "Retomando..." : "Baixando...", true, count, total, false));
            long lastUi = 0;
            long lastSavedCount = count;
            byte[] buffer = new byte[64 * 1024];
            try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(destination, "rw")) {
                if (pfd == null) throw new TransferDiagnostics.Failure("ARMAZENAMENTO", "Não foi possível abrir arquivo parcial.", false);
                try (InputStream input = conn.getInputStream()) {
                    FileOutputStream output = new FileOutputStream(pfd.getFileDescriptor());
                    FileChannel channel = output.getChannel();
                    if (resumed) {
                        if (channel.size() != offset) {
                            throw new TransferDiagnostics.Failure("ARQUIVO_ALTERADO", "Tamanho do arquivo parcial mudou.", false);
                        }
                        channel.position(offset);
                    } else {
                        channel.truncate(0);
                        channel.position(0);
                        // Somente após descartar bytes antigos, persistir a identidade da nova resposta.
                        DownloadStore.setMeta(context, id, conn.getHeaderField("ETag"),
                                conn.getHeaderField("Last-Modified"), -1, null);
                        count = 0;
                        DownloadStore.update(context, id, "Baixando", name, 0, total, null, "");
                    }
                    int n;
                    while ((n = input.read(buffer)) != -1) {
                        checkStop(id);
                        try { output.write(buffer, 0, n); }
                        catch (IOException storage) {
                            throw new TransferDiagnostics.Failure("ARMAZENAMENTO", "Falha ao gravar em Download/ (verifique espaço livre).", false);
                        }
                        count += n;
                        long now = System.currentTimeMillis();
                        if (now - lastUi >= 800) {
                            DownloadStore.update(context, id, "Baixando", null, count, total, null, null);
                            progress(notification(name, "Baixando...", true, count, total, false));
                            lastUi = now;
                            lastSavedCount = count;
                        }
                    }
                    try { output.flush(); channel.force(false); }
                    catch (IOException storage) {
                        throw new TransferDiagnostics.Failure("ARMAZENAMENTO", "Não foi possível salvar os bytes no armazenamento.", false);
                    }
                }
            } catch (IOException error) {
                // Nunca apagar fragmento em falha. Tamanho real será verificado na próxima tentativa.
                DownloadStore.update(context, id, null, null, Math.max(count, lastSavedCount), total, null, null);
                throw error;
            }
            if (total >= 0 && count != total) {
                DownloadStore.update(context, id, null, null, count, total, null, null);
                throw new TransferDiagnostics.Failure("INCOMPLETO", "Arquivo incompleto: " + count + " de " + total + " bytes.", true);
            }
            checkStop(id);
            // Só publicar no catálogo público depois de receber tudo.
            ContentValues completed = new ContentValues();
            completed.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (context.getContentResolver().update(destination, completed, null, null) != 1) {
                throw new TransferDiagnostics.Failure("ARMAZENAMENTO", "Não foi possível publicar arquivo em Download/.", false);
            }
            DownloadStore.update(context, id, "Concluído", name, count, total, destination.toString(), "");
            DownloadStore.setMeta(context, id, null, null, -1, "");
            notifications.notify(notificationId(id), notification("Download concluído", name, false, count, total, true));
        } finally {
            if (conn != null) conn.disconnect();
            currentConnection = null;
        }
    }

    private HttpURLConnection openHttpConnection(String id, String address, long offset, String validator,
                                                  String cookie, String userAgent, String referer) throws IOException {
        if (!TransferHttp.isWebUrl(address)) {
            throw new TransferDiagnostics.Failure("URL", "Endereço de download inválido.", false);
        }
        URL current = new URL(address);
        for (int step = 0; step <= 8; step++) {
            checkStop(id);
            if (!TransferHttp.isWebUrl(current.toString()) || TransferHttp.isDowngrade(address, current.toString())) {
                throw new TransferDiagnostics.Failure("HTTPS", "Redirecionamento para protocolo inseguro ou inválido.", false);
            }
            HttpURLConnection candidate = (HttpURLConnection) current.openConnection();
            candidate.setInstanceFollowRedirects(false);
            candidate.setConnectTimeout(20000);
            candidate.setReadTimeout(30000);
            candidate.setRequestMethod("GET");
            candidate.setRequestProperty("Accept", "*/*");
            candidate.setRequestProperty("Accept-Encoding", "identity");
            candidate.setRequestProperty("User-Agent", userAgent == null
                    ? "BaixaFacil/0.1.0-alpha.32 Android" : userAgent);
            boolean sameOrigin = TransferHttp.sameOrigin(address, current.toString());
            if (sameOrigin && cookie != null) candidate.setRequestProperty("Cookie", cookie);
            if (sameOrigin && referer != null && TransferHttp.sameOrigin(current.toString(), referer)) {
                candidate.setRequestProperty("Referer", referer);
            }
            if (offset > 0) {
                candidate.setRequestProperty("Range", "bytes=" + offset + "-");
                candidate.setRequestProperty("If-Range", validator);
            }
            currentConnection = candidate;
            final int code;
            try { code = candidate.getResponseCode(); }
            catch (IOException error) { candidate.disconnect(); throw error; }
            if (!TransferHttp.isRedirect(code)) return candidate;
            String location = candidate.getHeaderField("Location");
            candidate.disconnect();
            currentConnection = null;
            if (location == null || location.isEmpty() || step == 8) {
                throw new TransferDiagnostics.Failure("REDIRECIONAMENTO", "Redirecionamento inválido ou excessivo.", false);
            }
            URL next;
            try { next = new URL(current, location); }
            catch (Exception invalid) {
                throw new TransferDiagnostics.Failure("REDIRECIONAMENTO", "Destino inválido.", false);
            }
            if (!TransferHttp.isWebUrl(next.toString()) || TransferHttp.isDowngrade(address, next.toString())) {
                throw new TransferDiagnostics.Failure("HTTPS", "Redirecionamento inseguro bloqueado.", false);
            }
            current = next;
        }
        throw new TransferDiagnostics.Failure("REDIRECIONAMENTO", "Número de redirecionamentos excedido.", false);
    }

    private void truncateFile(Uri file) throws IOException {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(file, "rw")) {
            if (pfd == null) throw new IOException("Arquivo parcial inacessível");
            FileOutputStream stream = new FileOutputStream(pfd.getFileDescriptor());
            stream.getChannel().truncate(0);
            stream.getChannel().force(false);
        }
    }

    private int notificationId(String id) { return 200 + (id.hashCode() & 0x7FFFFFFF) % 1000000; }
    private Notification notification(String title, String subtitle, boolean ongoing, long received, long total, boolean autoCancel) {
        Notification.Builder builder = new Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title).setContentText(subtitle)
            .setOngoing(ongoing).setOnlyAlertOnce(true).setAutoCancel(autoCancel);
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        builder.setContentIntent(android.app.PendingIntent.getActivity(context, 0, open,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE));
        if (ongoing) {
            if (total > 0) builder.setProgress(100, (int)Math.min(100, received * 100.0 / total), false);
            else builder.setProgress(100, 0, true);
        }
        return builder.build();
    }
}
