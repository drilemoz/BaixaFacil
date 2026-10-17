package br.com.baixafacil;

import java.net.URI;
import java.util.Locale;

/** Perfis de compatibilidade opcionais apenas para origens reconhecidas.
 * Nunca modifica o User-Agent, o TLS, o JavaScript, cookies ou a navegação.
 */
final class BrowserLoadPolicy {
    private BrowserLoadPolicy() {}
    static boolean isChatGpt(String raw) {
        if (raw == null || raw.length() > 8192) return false;
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme(), host = uri.getHost();
            if (scheme == null || host == null || uri.getRawUserInfo() != null) return false;
            if (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")) return false;
            String h = host.toLowerCase(Locale.ROOT);
            return h.equals("chatgpt.com") || h.equals("www.chatgpt.com") || h.equals("chat.openai.com");
        } catch (Exception ignored) { return false; }
    }
    static boolean lightMode(String url, boolean preferenceEnabled) {
        return preferenceEnabled && isChatGpt(url);
    }
    static boolean autoScanAllowed(String url, boolean preferenceEnabled) {
        return !lightMode(url, preferenceEnabled);
    }
}
