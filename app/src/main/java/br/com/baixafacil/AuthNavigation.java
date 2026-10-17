package br.com.baixafacil;

import java.net.URI;
import java.util.Locale;

/** Somente classificação de URLs. Não manipula credenciais, cookies nem páginas de login. */
public final class AuthNavigation {
    private AuthNavigation() {}

    public static boolean isGoogleOAuth(String url) {
        if (url == null || url.length() > 4096) return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getRawPath();
            if (!"https".equalsIgnoreCase(scheme) || host == null || path == null) return false;
            if (!"accounts.google.com".equalsIgnoreCase(host)) return false;
            String route = path.toLowerCase(Locale.ROOT);
            return route.equals("/o/oauth2") || route.startsWith("/o/oauth2/");
        } catch (Exception ignored) { return false; }
    }

    /** Evita executar o detector de vídeos em páginas de autenticação e CAPTCHA. */
    public static boolean isSensitiveLoginPage(String url) {
        if (url == null || url.length() > 4096) return false;
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            String host = uri.getHost();
            if (host == null) return false;
            String h = host.toLowerCase(Locale.ROOT);
            if ("accounts.google.com".equals(h)) return true;
            if (!("facebook.com".equals(h) || h.endsWith(".facebook.com"))) return false;
            String path = uri.getRawPath();
            String route = path == null ? "" : path.toLowerCase(Locale.ROOT);
            return route.equals("/login") || route.startsWith("/login/")
                || route.equals("/checkpoint") || route.startsWith("/checkpoint/")
                || route.equals("/recover") || route.startsWith("/recover/");
        } catch (Exception ignored) { return false; }
    }

    public static boolean isFacebookCheckpoint(String url) {
        if (url == null || url.length() > 4096) return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getRawPath();
            if (!"https".equalsIgnoreCase(scheme) || host == null || path == null) return false;
            String h = host.toLowerCase(Locale.ROOT);
            if (!("facebook.com".equals(h) || h.endsWith(".facebook.com"))) return false;
            String route = path.toLowerCase(Locale.ROOT);
            return route.equals("/checkpoint") || route.startsWith("/checkpoint/");
        } catch (Exception ignored) { return false; }
    }
}
