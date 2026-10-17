package br.com.baixafacil;

import java.net.URI;

/** Política conservadora para popups e diagnóstico: sem observar credenciais nem tokens. */
public final class LoginWindowPolicy {
    private LoginWindowPolicy() {}

    /** Somente uma janela disparada por um gesto real do usuário. */
    public static boolean allowPopup(boolean userGesture, boolean alreadyOpen) {
        return userGesture && !alreadyOpen;
    }

    /** Nenhum esquema local, javascript:, intent:, blob: ou URL com credenciais. */
    public static boolean safeWebAddress(String address) {
        if (address == null || address.length() > 4096) return false;
        try {
            URI uri = new URI(address);
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && !uri.getHost().isEmpty()
                    && uri.getRawUserInfo() == null;
        } catch (Exception ignored) { return false; }
    }

    /** Não grava na lista de endereços tokens e parâmetros típicos de login OAuth. */
    public static boolean hasLoginParameters(String address) {
        if (!safeWebAddress(address)) return false;
        try {
            String query=new URI(address).getRawQuery();
            if(query==null) return false;
            for(String parameter:query.split("&")) {
                String key=parameter.split("=",2)[0].toLowerCase(java.util.Locale.ROOT);
                if(key.equals("code") || key.equals("access_token") || key.equals("id_token")
                        || key.equals("auth_token") || key.equals("session_state")
                        || key.equals("oauth_token"))return true;
            }
        } catch(Exception ignored) {}
        return false;
    }

    /** Iframes internos temporários podem usar about:blank; esquemas locais continuam bloqueados. */
    public static boolean isSafeInternalFrame(String target) {
        return "about:blank".equalsIgnoreCase(target) || "about:srcdoc".equalsIgnoreCase(target);
    }

    /** Diagnósticos jamais contêm path, query, fragmento ou credenciais. */
    public static String originForReport(String address) {
        if (!safeWebAddress(address)) return "não disponível";
        try {
            URI uri = new URI(address);
            String host = uri.getHost();
            if (host.length() > 110) return "não disponível";
            return uri.getScheme().toLowerCase(java.util.Locale.ROOT) + "://" + host;
        } catch (Exception ignored) { return "não disponível"; }
    }
}
