package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

/** Histórico simples limitado a 150 entradas. Inclui URLs, que podem conter tokens temporários. */
public final class DownloadStore {
    private static final String PREF = "download_history_v1";
    private static final String KEY = "items";
    private static final Object LOCK = new Object();
    private DownloadStore() {}

    private static JSONArray read(SharedPreferences preferences) {
        try { return new JSONArray(preferences.getString(KEY, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    public static String create(Context context, String url) {
        synchronized (LOCK) {
            SharedPreferences preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray old = read(preferences);
            JSONArray updated = new JSONArray();
            String id = UUID.randomUUID().toString();
            JSONObject item = new JSONObject();
            try {
                item.put("id", id);
                item.put("url", url);
                item.put("name", "Preparando download");
                item.put("status", "Aguardando");
                item.put("bytes", 0);
                item.put("total", -1);
                item.put("uri", "");
                item.put("error", "");
                item.put("time", System.currentTimeMillis());
                item.put("etag", "");
                item.put("lastModified", "");
                item.put("attempt", 0);
                item.put("code", "");
                updated.put(item);
                for (int i = 0; i < old.length() && i < 149; i++) updated.put(old.getJSONObject(i));
                preferences.edit().putString(KEY, updated.toString()).commit();
            } catch (Exception ignored) {}
            return id;
        }
    }

    public static void update(Context context, String id, String status, String name,
                              long bytes, long total, String uri, String error) {
        synchronized (LOCK) {
            SharedPreferences preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray data = read(preferences);
            try {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    if (!id.equals(obj.optString("id"))) continue;
                    if (status != null) obj.put("status", status);
                    if (name != null) obj.put("name", name);
                    if (bytes >= 0) obj.put("bytes", bytes);
                    if (total >= -1) obj.put("total", total);
                    if (uri != null) obj.put("uri", uri);
                    if (error != null) obj.put("error", error);
                    break;
                }
                SharedPreferences.Editor editor = preferences.edit().putString(KEY, data.toString());
                // O URI do MediaStore precisa sobreviver a interrupção/crash para retomar.
                if (uri != null || "Na fila".equals(status) || "Interrompido".equals(status)
                        || "Pausado".equals(status) || "Cancelado".equals(status)
                        || "Concluído".equals(status) || "Falhou".equals(status)) editor.commit();
                else editor.apply();
            } catch (Exception ignored) {}
        }
    }

    public static JSONObject get(Context context, String id) {
        synchronized (LOCK) {
            JSONArray data = read(context.getSharedPreferences(PREF, Context.MODE_PRIVATE));
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item != null && id.equals(item.optString("id"))) return item;
            }
            return null;
        }
    }

    public static void setMeta(Context context, String id, String etag, String modified,
                               int attempt, String code) {
        synchronized (LOCK) {
            SharedPreferences preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray data = read(preferences);
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null || !id.equals(item.optString("id"))) continue;
                try {
                    if (etag != null) item.put("etag", etag);
                    if (modified != null) item.put("lastModified", modified);
                    if (attempt >= 0) item.put("attempt", attempt);
                    if (code != null) item.put("code", code);
                    preferences.edit().putString(KEY, data.toString()).commit();
                } catch (Exception ignored) {}
                return;
            }
        }
    }

    /** Fila persistente, sem persistir nenhum cookie, referer ou User-Agent de sessão. */
    public static void queue(Context context,String id) {
        synchronized (LOCK) {
            SharedPreferences pref=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            JSONArray data=read(pref);
            long order=pref.getLong("queue_sequence",0)+1;
            for(int i=0;i<data.length();i++) {
                JSONObject row=data.optJSONObject(i);
                if(row==null || !id.equals(row.optString("id")))continue;
                try {row.put("queueOrder",order);row.put("status","Na fila");row.put("error","");}
                catch(Exception ignored){}
                pref.edit().putLong("queue_sequence",order).putString(KEY,data.toString()).commit();
                return;
            }
        }
    }
    public static List<JSONObject> queued(Context context) {
        List<JSONObject> rows=all(context);
        rows.removeIf(item->!"Na fila".equals(item.optString("status")));
        Collections.sort(rows,Comparator.comparingLong(item->item.optLong("queueOrder",Long.MAX_VALUE)));
        return rows;
    }
    public static void prioritize(Context context,String id) {
        synchronized (LOCK) {
            SharedPreferences pref=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            JSONArray data=read(pref);
            long min=0;
            for(int i=0;i<data.length();i++) {
                JSONObject row=data.optJSONObject(i);
                if(row!=null && "Na fila".equals(row.optString("status")))
                    min=Math.min(min,row.optLong("queueOrder",0));
            }
            for(int i=0;i<data.length();i++) {
                JSONObject row=data.optJSONObject(i);
                if(row==null || !id.equals(row.optString("id"))||!"Na fila".equals(row.optString("status")))continue;
                try{row.put("queueOrder",min-1);}catch(Exception ignored){}
                pref.edit().putString(KEY,data.toString()).commit();return;
            }
        }
    }
    public static boolean recoverable(String status) {
        return RecoveryPolicy.canRecover(status);
    }
    public static List<JSONObject> recoverable(Context context) {
        List<JSONObject> rows=all(context);
        rows.removeIf(item->!recoverable(item.optString("status")));
        Collections.sort(rows,Comparator.comparingLong(item->item.optLong("queueOrder",item.optLong("time",0))));
        return rows;
    }
    /** Estado pendente após crash; não reiniciar downloads silenciosamente. */
    public static int reconcile(Context context) {
        if(TransferJobService.hasScheduledJob(context))return 0;
        int count=0;
        for(JSONObject e:all(context)) {
            String id=e.optString("id"),status=e.optString("status");
            boolean inFlight=RecoveryPolicy.interrupted(status);
            if(inFlight && !TransferService.isActive(id) && !TransferJobService.isActive(id)) {
                update(context,id,"Interrompido",null,-1,-2,null,
                        "O processo anterior terminou. Toque em Recuperar fila ou Retomar.");
                count++;
            }
        }
        return count;
    }

    /** Exclui somente a linha do histórico, não o arquivo do MediaStore. */
    public static boolean remove(Context context, String id) {
        synchronized (LOCK) {
            SharedPreferences pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray old = read(pref);
            JSONArray filtered = new JSONArray();
            boolean found = false;
            for (int i = 0; i < old.length(); i++) {
                JSONObject item = old.optJSONObject(i);
                if (item == null) continue;
                if (id.equals(item.optString("id"))) { found = true; continue; }
                filtered.put(item);
            }
            return found && pref.edit().putString(KEY, filtered.toString()).commit();
        }
    }

    /** Remoção atômica sob a mesma trava de atualização, condicionada a estado final. */
    static boolean removeIfFinished(Context context, String id) {
        synchronized (LOCK) {
            SharedPreferences pref=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            JSONArray old=read(pref),filtered=new JSONArray();
            boolean removed=false;
            for(int i=0;i<old.length();i++) {
                JSONObject row=old.optJSONObject(i);
                if(row==null)continue;
                if(id.equals(row.optString("id")) && DownloadCleanupPolicy.eligible(
                        row.optString("status"),!row.optString("uri").isEmpty())) {
                    removed=true;continue;
                }
                filtered.put(row);
            }
            return removed && pref.edit().putString(KEY,filtered.toString()).commit();
        }
    }

    /** Remove somente registro concluído cuja URI ainda coincide com a seleção confirmada. */
    static boolean removeIfCompleted(Context context, String id, String expectedUri) {
        if(id==null || id.isEmpty() || expectedUri==null || expectedUri.isEmpty()) return false;
        synchronized (LOCK) {
            SharedPreferences pref=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            JSONArray old=read(pref), next=new JSONArray();
            boolean removed=false;
            for(int i=0;i<old.length();i++) {
                JSONObject row=old.optJSONObject(i);
                if(row==null)continue;
                if(id.equals(row.optString("id")) && expectedUri.equals(row.optString("uri"))
                    && DownloadSelectionPolicy.selectable(row.optString("status"),row.optString("uri"))) {
                    removed=true;
                    continue;
                }
                next.put(row);
            }
            return removed && pref.edit().putString(KEY,next.toString()).commit();
        }
    }

    public static List<JSONObject> all(Context context) {
        synchronized (LOCK) {
            JSONArray arr = read(context.getSharedPreferences(PREF, Context.MODE_PRIVATE));
            List<JSONObject> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) out.add(obj);
            }
            return out;
        }
    }
}
