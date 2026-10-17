package br.com.baixafacil;

import java.net.URI;
import java.util.Locale;

/** Classificação conservadora de fontes de vídeo direto identificadas na WebView.
 * Não converte playlists HLS/DASH, blob, DRM ou páginas HTML em arquivos de vídeo.
 */
public final class VideoLinks {
    public static final int MAX_VIDEO_URLS = 24;
    public static final int MAX_URL_LENGTH = 4096;
    private VideoLinks() {}

    public static boolean isDirectFile(String url) {
        if (!allowedHttpUrl(url)) return false;
        String path;
        try { path = new URI(url).getPath(); }
        catch (Exception e) { return false; }
        if (path == null) return false;
        String file = path.toLowerCase(Locale.ROOT);
        return file.endsWith(".mp4") || file.endsWith(".m4v")
                || file.endsWith(".webm") || file.endsWith(".mov")
                || file.endsWith(".ogv") || file.endsWith(".3gp");
    }

    public static boolean isManifest(String url) {
        if (url == null) return false;
        try {
            String path = new URI(url).getPath();
            if (path == null) return false;
            String lower = path.toLowerCase(Locale.ROOT);
            return lower.endsWith(".m3u8") || lower.endsWith(".mpd")
                    || lower.endsWith(".m4s") || lower.endsWith(".ts");
        } catch (Exception e) { return false; }
    }

    public static boolean allowedHttpUrl(String url) {
        if (url == null || url.length() > MAX_URL_LENGTH || url.isEmpty() || !TransferHttp.isWebUrl(url)) return false;
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (Character.isISOControl(c) || Character.isWhitespace(c)) return false;
        }
        return true;
    }

    /** video/source são indicadores fornecidos pelo elemento HTML, não uma garantia do servidor. */
    public static boolean isVideoCandidate(String url, String source, String mime) {
        if (!allowedHttpUrl(url) || isManifest(url)) return false;
        if (isDirectFile(url)) return true;
        String kind = source == null ? "" : source.toLowerCase(Locale.ROOT);
        String type = mime == null ? "" : mime.toLowerCase(Locale.ROOT).trim();
        if (type.startsWith("video/") && !type.contains("mpegurl") && !type.contains("dash")) return true;
        return "video".equals(kind) || "source".equals(kind);
    }

    public static String label(String url) {
        if (!allowedHttpUrl(url)) return "Vídeo";
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            String file = path == null ? "" : path.substring(path.lastIndexOf('/') + 1);
            if (file.length() > 0 && file.length() <= 80 && isDirectFile(url)) return file;
            return "Vídeo de " + uri.getHost();
        } catch (Exception e) { return "Vídeo"; }
    }
}
