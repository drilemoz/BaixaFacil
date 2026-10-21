package br.com.baixafacil;

/** Regras puras para limitar trabalho visual dispensável (sem afetar downloads ativos). */
final class MediaWorkPolicy {
    static final int MAX_PENDING_PREVIEWS = 10;
    static final int MAX_LISTENERS_PER_PREVIEW = 8;
    static final long ACTIVE_REFRESH_MS = 1200L;
    static final long IDLE_REFRESH_MS = 6000L;

    private MediaWorkPolicy() {}

    static boolean needsFrequentRefresh(String status) {
        return "Aguardando".equals(status) || "Na fila".equals(status)
            || "Conectando".equals(status) || "Baixando".equals(status)
            || "Retomando".equals(status);
    }

    static long refreshDelay(boolean visible, boolean activeTransfer) {
        return !visible ? -1L : activeTransfer ? ACTIVE_REFRESH_MS : IDLE_REFRESH_MS;
    }

    static boolean shouldTrimPreviewCache(int trimLevel) {
        // UI_HIDDEN=20 não prova pressão de RAM; background/critical >=40, running_low=10/15.
        return trimLevel == 10 || trimLevel == 15 || trimLevel >= 40;
    }
}
