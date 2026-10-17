package br.com.baixafacil;

import java.net.URI;

/** Regras sem dependência Android para encaminhar cabeçalhos de sessão com segurança. */
public final class TransferHttp {
    private TransferHttp() {}

    public static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    public static boolean isWebUrl(String address) {
        if (address == null) return false;
        try {
            URI uri = new URI(address);
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && !uri.getHost().isEmpty() && uri.getUserInfo() == null;
        } catch (Exception invalid) { return false; }
    }

    public static boolean sameOrigin(String first, String second) {
        if (!isWebUrl(first) || !isWebUrl(second)) return false;
        try {
            URI a = new URI(first);
            URI b = new URI(second);
            return a.getScheme().equalsIgnoreCase(b.getScheme())
                    && a.getHost().equalsIgnoreCase(b.getHost())
                    && effectivePort(a) == effectivePort(b);
        } catch (Exception invalid) { return false; }
    }

    private static int effectivePort(URI value) {
        if (value.getPort() != -1) return value.getPort();
        return "https".equalsIgnoreCase(value.getScheme()) ? 443 : 80;
    }

    /** Rejeita quebras de linha/controle e cabeçalhos excepcionalmente longos. */
    public static String safeHeader(String input, int maxLen) {
        if (input == null || input.isEmpty() || input.length() > maxLen) return null;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < 32 || c == 127) return null;
        }
        return input;
    }

    /** Nunca levar uma sessão HTTPS para conexões HTTP. */
    public static boolean isDowngrade(String original, String next) {
        if (!isWebUrl(original) || !isWebUrl(next)) return true;
        try {
            return "https".equalsIgnoreCase(new URI(original).getScheme())
                    && !"https".equalsIgnoreCase(new URI(next).getScheme());
        } catch (Exception invalid) { return true; }
    }
}
