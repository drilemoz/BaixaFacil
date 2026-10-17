package br.com.baixafacil;

import java.net.URI;
import java.util.Locale;

/** Seleciona mídias explícitas, sem consultar cookies ou contornar DRM. */
public final class MediaSourcePolicy {
    public enum Kind { DIRECT, HLS, DASH, UNSUPPORTED }
    private MediaSourcePolicy() {}
    public static Kind classify(String url) {
        if (url == null || url.isEmpty() || url.length() > 4096) return Kind.UNSUPPORTED;
        try {
            URI uri = new URI(url);
            String scheme=uri.getScheme(), host=uri.getHost();
            if (scheme==null || host==null || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
                  || uri.getUserInfo()!=null || uri.getFragment()!=null) return Kind.UNSUPPORTED;
            String path = uri.getPath()==null?"":uri.getPath().toLowerCase(Locale.ROOT);
            if(path.endsWith(".m3u8"))return Kind.HLS;
            if(path.endsWith(".mpd"))return Kind.DASH;
            if(path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".webm")
                    || path.endsWith(".mov") || path.endsWith(".mkv") || path.endsWith(".3gp")
                    || path.endsWith(".ogv"))return Kind.DIRECT;
            return Kind.UNSUPPORTED;
        } catch (Exception ignored){return Kind.UNSUPPORTED;}
    }
    public static boolean isPlaylist(String url){Kind kind=classify(url);return kind==Kind.HLS||kind==Kind.DASH;}
    public static boolean isDirect(String url){return classify(url)==Kind.DIRECT;}
}
