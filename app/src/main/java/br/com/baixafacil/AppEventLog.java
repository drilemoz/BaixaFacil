package br.com.baixafacil;

import android.content.Context;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Registro local e opcional de alertas/erros do app, preservando privacidade. */
public final class AppEventLog {
    private static final String PREFS = "app_event_log";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 180;
    private static volatile boolean installed;
    private AppEventLog() {}

    public static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) info(context, "Configurações", "Captura de erros e alertas ativada.");
    }

    public static void install(Context context) {
        if (installed) return;
        synchronized (AppEventLog.class) {
            if (installed) return;
            final Context app = context.getApplicationContext();
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                try {
                    fatal(app, "Crash", crashSummary(error, thread));
                } catch (Throwable ignored) {}
                if (previous != null) previous.uncaughtException(thread, error);
                else System.exit(10);
            });
            installed = true;
        }
    }

    public static void info(Context context, String source, String message) {
        record(context, "INFO", source, message);
    }
    public static void warn(Context context, String source, String message) {
        record(context, "ALERTA", source, message);
    }
    public static void error(Context context, String source, String message) {
        record(context, "ERRO", source, message);
    }
    public static void error(Context context, String source, Throwable error) {
        record(context, "ERRO", source, throwableSummary(error));
    }
    public static void fatal(Context context, String source, String message) {
        record(context, "FATAL", source, message);
    }

    private static void record(Context context, String level, String source, String message) {
        if (context == null || !enabled(context)) return;
        try {
            JSONArray items = readArray(context);
            JSONObject event = new JSONObject();
            event.put("time", System.currentTimeMillis());
            event.put("level", level == null ? "INFO" : level);
            event.put("source", sanitize(source));
            event.put("message", sanitize(message));
            items.put(event);
            while (items.length() > MAX_ITEMS) {
                JSONArray next = new JSONArray();
                for (int i = Math.max(0, items.length() - MAX_ITEMS); i < items.length(); i++) {
                    next.put(items.opt(i));
                }
                items = next;
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, items.toString()).apply();
        } catch (Throwable ignored) {}
    }

    public static List<String> lines(Context context, int limit) {
        ArrayList<String> out = new ArrayList<>();
        JSONArray items = readArray(context);
        int count = 0;
        for (int i = items.length() - 1; i >= 0 && count < limit; i--, count++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            long time = item.optLong("time", 0L);
            String stamp = time > 0 ? new SimpleDateFormat("dd/MM HH:mm:ss", Locale.ROOT).format(new Date(time)) : "--/--";
            out.add(stamp + " • " + item.optString("level") + " • " + item.optString("source") + "\n" + item.optString("message"));
        }
        return out;
    }

    public static String exportText(Context context, int limit) {
        StringBuilder text = new StringBuilder();
        text.append("Baixa Fácil • registros locais\n");
        text.append("Android ").append(Build.VERSION.RELEASE).append(" • SDK ").append(Build.VERSION.SDK_INT).append("\n\n");
        List<String> items = lines(context, limit);
        if (items.isEmpty()) {
            text.append("Nenhum alerta ou erro capturado.");
        } else {
            for (String line : items) {
                text.append(line).append("\n\n");
            }
        }
        return text.toString().trim();
    }

    public static boolean hasItems(Context context) {
        return readArray(context).length() > 0;
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ITEMS).apply();
    }

    private static JSONArray readArray(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, "[]");
        try { return new JSONArray(raw == null ? "[]" : raw); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private static String crashSummary(Throwable error, Thread thread) {
        StringBuilder text = new StringBuilder();
        text.append("Thread ");
        text.append(thread == null ? "desconhecida" : sanitize(thread.getName()));
        text.append(": ").append(throwableSummary(error));
        return text.toString();
    }

    private static String throwableSummary(Throwable error) {
        if (error == null) return "Falha sem exceção detalhada.";
        StringBuilder text = new StringBuilder();
        text.append(error.getClass().getSimpleName());
        if (error.getMessage() != null && !error.getMessage().trim().isEmpty()) {
            text.append(": ").append(sanitize(error.getMessage()));
        }
        StackTraceElement[] stack = error.getStackTrace();
        int added = 0;
        for (int i = 0; stack != null && i < stack.length && added < 5; i++) {
            StackTraceElement element = stack[i];
            if (element == null) continue;
            String owner = element.getClassName();
            if (owner == null) continue;
            if (!owner.startsWith("br.com.baixafacil")) continue;
            text.append(" • ").append(owner.substring(owner.lastIndexOf('.') + 1));
            text.append("#").append(element.getMethodName());
            if (element.getLineNumber() > 0) text.append(":").append(element.getLineNumber());
            added++;
        }
        return text.toString();
    }

    private static String sanitize(String raw) {
        if (raw == null) return "";
        String value = raw.replaceAll("https?://([^\\s/?#]+)([^\\s]*)", "https://$1");
        value = value.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
        if (value.length() > 280) value = value.substring(0, 277) + "...";
        return value;
    }
}
