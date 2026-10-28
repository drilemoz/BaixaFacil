package br.com.baixafacil;

/** Aplica a mesma política de conteúdo à sessão principal e às janelas secundárias.
 * Não intercepta downloads, não armazena endereços e não substitui Safe Browsing.
 */
final class BrowserContentGate {
    enum Action { ALLOW, BLOCK, REWRITE }
    static final class Decision {
        final Action action;
        final String url;
        private Decision(Action action, String url) { this.action=action; this.url=url; }
    }
    private BrowserContentGate() {}

    static boolean blocked(String url, int level) {
        return AdultContentPolicy.blocked(url, level);
    }
    static Decision evaluate(String url, int level) {
        if (blocked(url, level)) return new Decision(Action.BLOCK, null);
        String rewritten=AdultContentPolicy.safeSearchUrl(url, level);
        if (rewritten!=null && !rewritten.equals(url))
            return new Decision(Action.REWRITE, rewritten);
        return new Decision(Action.ALLOW, url);
    }
}
