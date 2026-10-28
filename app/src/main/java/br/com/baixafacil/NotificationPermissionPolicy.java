package br.com.baixafacil;

/** Permissão opcional, pedida no primeiro download efetivamente iniciado. */
final class NotificationPermissionPolicy {
    private NotificationPermissionPolicy() {}
    static boolean shouldAsk(int sdk, boolean granted, boolean askedBefore) {
        return sdk >= 33 && !granted && !askedBefore;
    }
}
