package br.com.baixafacil;

/** Regras puras de economia de trabalho. Nenhuma leitura de memória é feita por quadro. */
final class BrowserPerformancePolicy {
    static final long MIN_SCAN_INTERVAL_MS = 3500L;
    static final long SAFE_HEADROOM_BYTES = 128L * 1024L * 1024L;
    private BrowserPerformancePolicy() {}

    static boolean allowAutomaticMedia(boolean foreground, boolean lightMode,
                                      boolean memoryPressure, boolean rendererUnresponsive) {
        return foreground && !lightMode && !memoryPressure && !rendererUnresponsive;
    }

    static long scheduleDelay(long requestedMs, long nowUptimeMs, long lastScanUptimeMs) {
        long delay = Math.max(0, requestedMs);
        if (lastScanUptimeMs <= 0 || lastScanUptimeMs > nowUptimeMs) return delay;
        long elapsed = nowUptimeMs - lastScanUptimeMs;
        return Math.max(delay, Math.max(0, MIN_SCAN_INTERVAL_MS - elapsed));
    }

    /** WebView.onPause() não suspende timers JS, mas pauseTimers() afeta todas as WebViews. */
    static boolean canResumeOptionalWork(boolean lowMemory, long availableBytes, long lowThresholdBytes) {
        if (lowMemory) return false;
        if (availableBytes < 0 || lowThresholdBytes < 0) return false;
        return availableBytes >= lowThresholdBytes &&
                availableBytes - lowThresholdBytes >= SAFE_HEADROOM_BYTES;
    }

    static boolean seriousMemoryPressure(int trimLevel) {
        // TRIM_MEMORY_RUNNING_LOW (10), RUNNING_CRITICAL (15), e eventos de pressão em background.
        // UI_HIDDEN (20) sozinho não significa pouco espaço; onPause já suspende a página.
        return trimLevel == 10 || trimLevel == 15 || trimLevel >= 40;
    }
}
