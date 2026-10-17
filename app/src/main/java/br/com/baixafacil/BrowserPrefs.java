package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Preferências e histórico do navegador isolado, sem registrar cookies ou cabeçalhos. */
public final class BrowserPrefs {
    private static final String FILE = "browser_ui";
    private static final String KEY_HISTORY = "history"; // compatibilidade com alpha.12
    private static final String KEY_HOME = "home_url";
    private static final String KEY_SEARCH = "search_engine";
    private static final String KEY_JS = "javascript_enabled";
    private static final String KEY_CLEAR = "clear_requested";
    private static final String KEY_DESKTOP = "desktop_mode";
    private static final String KEY_TEXT_ZOOM = "text_zoom";
    private static final String KEY_FAVORITES = "favorite_sites_v1";
    public static final String DEFAULT_HOME = "https://www.google.com/";
    private BrowserPrefs() {}

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    public static String home(Context c) { return prefs(c).getString(KEY_HOME, DEFAULT_HOME); }
    public static String searchEngine(Context c) { return prefs(c).getString(KEY_SEARCH, "Google"); }
    public static boolean javascript(Context c) { return prefs(c).getBoolean(KEY_JS, true); }
    public static void setJavascript(Context c, boolean enabled) { prefs(c).edit().putBoolean(KEY_JS, enabled).apply(); }
    public static boolean desktop(Context c) { return prefs(c).getBoolean(KEY_DESKTOP, false); }
    public static void setDesktop(Context c, boolean enabled) { prefs(c).edit().putBoolean(KEY_DESKTOP, enabled).apply(); }
    public static int textZoom(Context c) {
        int stored = prefs(c).getInt(KEY_TEXT_ZOOM, 100);
        return stored == 75 || stored == 100 || stored == 125 || stored == 150 ? stored : 100;
    }
    public static void setTextZoom(Context c, int percentage) {
        if (percentage == 75 || percentage == 100 || percentage == 125 || percentage == 150)
            prefs(c).edit().putInt(KEY_TEXT_ZOOM, percentage).apply();
    }
    public static void setSearchEngine(Context c, String engine) {
        prefs(c).edit().putString(KEY_SEARCH, "DuckDuckGo".equals(engine) ? "DuckDuckGo" : "Google").apply();
    }
    public static boolean setHome(Context c, String typed) {
        String normalized = UrlTools.normalizeAddress(typed);
        // Página inicial deve ser uma URL HTTP(S) normal; não aceita protocolos internos.
        if (normalized == null) return false;
        prefs(c).edit().putString(KEY_HOME, normalized).apply();
        return true;
    }
    public static List<String> history(Context c) {
        String raw = prefs(c).getString(KEY_HISTORY, "");
        ArrayList<String> items = new ArrayList<>();
        if (raw != null) for (String item : raw.split("\n")) {
            if (UrlTools.extract(item) != null && UrlTools.extract(item).equals(item) && !items.contains(item)) items.add(item);
        }
        return items;
    }
    public static void remember(Context c, String url) {
        if (url == null || !url.equals(UrlTools.extract(url))) return;
        List<String> items = history(c);
        items.remove(url);
        items.add(0,url);
        while (items.size() > 12) items.remove(items.size()-1);
        prefs(c).edit().putString(KEY_HISTORY, String.join("\n", items)).apply();
    }
    /** Favoritos deliberadamente separados do histórico e da limpeza de cache/cookies. */
    public static final class Favorite {
        public final String url;
        public final String title;
        Favorite(String url, String title) { this.url = url; this.title = title; }
    }

    public static List<Favorite> favorites(Context c) {
        ArrayList<Favorite> items = new ArrayList<>();
        String saved = prefs(c).getString(KEY_FAVORITES, "[]");
        try {
            JSONArray json = new JSONArray(saved == null ? "[]" : saved);
            for (int i = 0; i < json.length() && items.size() < FavoriteRules.MAX_FAVORITES; i++) {
                JSONObject object = json.optJSONObject(i);
                if (object == null) continue;
                String url = FavoriteRules.url(object.optString("url", ""));
                if (url == null || findFavorite(items, url) >= 0) continue;
                items.add(new Favorite(url, FavoriteRules.title(object.optString("title", ""), url)));
            }
        } catch (Exception ignored) {
            // Dados inválidos não devem quebrar o navegador nem ser executados como URLs.
        }
        return items;
    }

    private static int findFavorite(List<Favorite> items, String url) {
        for (int i = 0; i < items.size(); i++) if (items.get(i).url.equals(url)) return i;
        return -1;
    }

    public static boolean isFavorite(Context c, String raw) {
        String url = FavoriteRules.url(raw);
        return url != null && findFavorite(favorites(c), url) >= 0;
    }

    public static boolean addFavorite(Context c, String rawUrl, String rawTitle) {
        String url = FavoriteRules.url(rawUrl);
        if (url == null) return false;
        List<Favorite> items = favorites(c);
        int previous = findFavorite(items, url);
        if (previous >= 0) items.remove(previous);
        else if (items.size() >= FavoriteRules.MAX_FAVORITES) return false;
        items.add(0, new Favorite(url, FavoriteRules.title(rawTitle, url)));
        writeFavorites(c, items);
        return true;
    }

    public static boolean editFavorite(Context c, String originalUrl, String editedUrl, String editedTitle) {
        String url = FavoriteRules.url(editedUrl);
        if (url == null) return false;
        List<Favorite> items = favorites(c);
        int current = findFavorite(items, originalUrl);
        int duplicate = findFavorite(items, url);
        if (current < 0 || (duplicate >= 0 && duplicate != current)) return false;
        items.set(current, new Favorite(url, FavoriteRules.title(editedTitle, url)));
        writeFavorites(c, items);
        return true;
    }

    public static boolean removeFavorite(Context c, String rawUrl) {
        String url = FavoriteRules.url(rawUrl);
        if (url == null) return false;
        List<Favorite> items = favorites(c);
        int position = findFavorite(items, url);
        if (position < 0) return false;
        items.remove(position);
        writeFavorites(c, items);
        return true;
    }

    private static void writeFavorites(Context c, List<Favorite> items) {
        JSONArray json = new JSONArray();
        for (Favorite item : items) {
            JSONObject object = new JSONObject();
            try { object.put("url", item.url); object.put("title", item.title); json.put(object); }
            catch (Exception ignored) { /* JSONObject só recebe strings válidas normalizadas. */ }
        }
        prefs(c).edit().putString(KEY_FAVORITES, json.toString()).apply();
    }

    public static void requestClear(Context c) { prefs(c).edit().remove(KEY_HISTORY).putBoolean(KEY_CLEAR,true).apply(); }
    public static boolean consumeClearRequest(Context c) {
        boolean clear = prefs(c).getBoolean(KEY_CLEAR,false);
        if (clear) prefs(c).edit().putBoolean(KEY_CLEAR,false).apply();
        return clear;
    }
    public static String destination(Context c, String input) {
        String fullUrl = UrlTools.normalizeAddress(input);
        if (fullUrl != null) return fullUrl;
        String result = UrlTools.browserAddress(input);
        if (result == null || !result.startsWith("https://www.google.com/search?q=")) return result;
        if ("DuckDuckGo".equals(searchEngine(c))) {
            String query = input == null ? "" : input.trim();
            return "https://duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        }
        return result;
    }
}
