package br.com.baixafacil;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlTools {
    private static final Pattern LINK = Pattern.compile("https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);

    private UrlTools() {}

    /** Retorna URL HTTP(S) extraída do texto compartilhado, ou null. */
    public static String extract(String text) {
        if (text == null) return null;
        Matcher matcher = LINK.matcher(text.trim());
        while (matcher.find()) {
            String found = matcher.group();
            while (found.endsWith(".") || found.endsWith(",") || found.endsWith(";")
                    || found.endsWith(")") || found.endsWith("]") || found.endsWith("}")) {
                found = found.substring(0, found.length() - 1);
            }
            try {
                URI uri = new URI(found);
                String scheme = uri.getScheme();
                if (uri.getHost() != null && !uri.getHost().isEmpty()
                        && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                        && uri.getUserInfo() == null) return found;
            } catch (Exception ignored) {
                // Não tentar converter caminhos locais ou links inválidos em downloads.
            }
        }
        return null;
    }
    /** Aceita um domínio digitado sem protocolo; nunca rebaixa HTTPS para HTTP. */
    public static String normalizeAddress(String raw) {
        if (raw == null) return null;
        String input = raw.trim();
        if (input.isEmpty() || input.matches(".*\\s+.*") || input.contains("\"")) return null;
        String low = input.toLowerCase(Locale.ROOT);
        if (low.startsWith("//") || low.contains("\\\\")) return null;
        if (low.matches("^[a-z][a-z0-9+.-]*:.*") && !low.startsWith("http://") && !low.startsWith("https://")) return null;
        String result = (low.startsWith("http://") || low.startsWith("https://")) ? input : "https://" + input;
        try {
            URI uri = new URI(result);
            String host = uri.getHost();
            if (host == null || host.isEmpty() || uri.getUserInfo() != null) return null;
            if (uri.getPort() > 65535) return null;
            if (!host.equalsIgnoreCase("localhost")
                && !host.matches("(?i)^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$")
                && !host.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")
                && !host.contains(":")) return null;
            return result.equals(extract(result)) ? result : null;
        } catch (Exception ignored) { return null; }
    }

    /** Barra de navegador: endereço sem esquema ou texto transformado em pesquisa. */
    public static String browserAddress(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String typed = raw.trim();
        String link = normalizeAddress(typed);
        if (link != null) return link;
        // Protocolo explícito inválido/inseguro, esquema interno ou entrada parecida com URL
        // não deve virar uma pesquisa que possa expor tokens acidentalmente.
        if (typed.matches("(?is)^[a-z][a-z0-9+.-]*:.*") || typed.startsWith("//")
                || typed.contains("/") || typed.contains("@") || typed.contains("\\")) return null;
        return "https://www.google.com/search?q=" + URLEncoder.encode(typed, StandardCharsets.UTF_8);
    }

}
