package br.com.baixafacil;

/** Presets de proteção do GeckoView. Não representa filtros EasyList de rede. */
final class GeckoProtectionProfile {
    static final int OFF = 0;
    static final int LIGHT = 1;
    static final int STRONG = 2;
    static final int EXTREME = 3;
    private GeckoProtectionProfile() {}

    static int normalize(int mode) {
        return mode >= OFF && mode <= EXTREME ? mode : STRONG;
    }

    /** O usuário pode suspender ETP independentemente do filtro cosmético. */
    static boolean active(int mode, boolean trackers, boolean cookiesForCompatibility) {
        return trackers && !cookiesForCompatibility && normalize(mode) != OFF;
    }

    /** STRICT ativa as listas de conteúdo que podem afetar funcionalidades dos sites. */
    static boolean strict(int mode) { return normalize(mode) >= STRONG; }
    static boolean extra(int mode) { return normalize(mode) == EXTREME; }
}
