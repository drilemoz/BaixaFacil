package br.com.baixafacil;

/** Limites de trabalho de favicons: nunca consultar um provedor de ícones externo. */
final class FaviconWorkPolicy {
    static final int MAX_PENDING_ORIGINS = 24;
    static final int MAX_WAITERS_PER_ORIGIN = 8;
    static final int MAX_FAILED_ORIGINS = 96;
    static final int MAX_RESPONSE_BYTES = 64 * 1024;
    static final long RETRY_AFTER_FAILURE_MS = 5 * 60 * 1000L;

    private FaviconWorkPolicy() {}

    static boolean stillCoolingDown(long now, long lastFailure) {
        return now >= lastFailure && now - lastFailure < RETRY_AFTER_FAILURE_MS;
    }

    static boolean shouldTrimCache(int level) {
        return level == 10 || level == 15 || level >= 40;
    }

    static boolean shouldLoadPreview(boolean completed, boolean scrolling, boolean visible) {
        return completed && !scrolling && visible;
    }
}
