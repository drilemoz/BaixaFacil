package br.com.baixafacil;

/**
 * Limita recriações automáticas após onCrash/onKill do GeckoView.
 * Sem dependências Android: o histórico contém apenas contagem e tempo monotônico,
 * nunca URLs, cookies ou dados da página.
 */
final class GeckoCrashPolicy {
    static final long WINDOW_MS = 120_000L;
    static final int MAX_AUTO_RESTARTS = 2;

    private GeckoCrashPolicy() {}

    static final class Decision {
        final long windowStartMs;
        final int crashes;
        final boolean restartAutomatically;
        Decision(long start, int count) {
            windowStartMs = start;
            crashes = count;
            restartAutomatically = count <= MAX_AUTO_RESTARTS;
        }
    }

    static Decision afterFailure(long nowMs, long previousStartMs, int previousCrashes) {
        boolean newWindow = previousStartMs <= 0 || nowMs < previousStartMs
                || nowMs - previousStartMs > WINDOW_MS || previousCrashes < 1;
        long start = newWindow ? nowMs : previousStartMs;
        int attempts = newWindow ? 1 : previousCrashes >= MAX_AUTO_RESTARTS + 1
                ? MAX_AUTO_RESTARTS + 1 : previousCrashes + 1;
        return new Decision(start, attempts);
    }
}
