package br.com.baixafacil;

import java.net.URI;

/** Regras puras dos favoritos; não faz acesso à rede nem usa permissões do Android. */
public final class FavoriteRules {
    public static final int MAX_FAVORITES = 60;
    public static final int MAX_URL_LENGTH = 2048;
    public static final int MAX_TITLE_LENGTH = 80;
    private FavoriteRules() {}

    /** URLs válidas são HTTP(S), sem credenciais, que já passaram pela normalização segura. */
    public static String url(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.length() == 0 || trimmed.length() > MAX_URL_LENGTH) return null;
        String normalized = UrlTools.normalizeAddress(trimmed);
        return normalized != null && normalized.length() <= MAX_URL_LENGTH ? normalized : null;
    }

    public static String title(String raw, String url) {
        String normalized = raw == null ? "" : raw.replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
        if (normalized.isEmpty()) {
            try { normalized = new URI(url).getHost(); }
            catch (Exception ignored) { normalized = "Site favorito"; }
        }
        if (normalized == null || normalized.isEmpty()) normalized = "Site favorito";
        return normalized.length() > MAX_TITLE_LENGTH ? normalized.substring(0, MAX_TITLE_LENGTH) : normalized;
    }
}
