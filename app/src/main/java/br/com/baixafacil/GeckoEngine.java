package br.com.baixafacil;

import android.content.Context;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.StorageController;

/** Um GeckoRuntime por processo; dados do Gecko não são os dados da WebView do Android. */
final class GeckoEngine {
    private static GeckoRuntime runtime;
    private GeckoEngine() {}

    static synchronized GeckoRuntime get(Context context) {
        if (runtime == null) {
            GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder().build();
            settings.setAutomaticFontSizeAdjustment(false);
            settings.setFontSizeFactor(BrowserPrefs.textZoom(context) / 100f);
            applyProtection(settings, context);
            runtime = GeckoRuntime.create(context.getApplicationContext(), settings);
            runtime.setAutocompleteStorageDelegate(new SecureLoginStore(context));
        }
        return runtime;
    }

    /** Configuração nativa de ETP (GeckoView 158). Não injeta regras EasyList. */
    private static void applyProtection(GeckoRuntimeSettings settings, Context context) {
        int mode = GeckoProtectionProfile.normalize(AdBlockController.savedMode(context));
        boolean enabled = GeckoProtectionProfile.active(mode,
            AdBlockController.savedTrackers(context), BrowserPrefs.thirdPartyCookies(context));
        ContentBlocking.Settings block = settings.getContentBlocking();
        // Sem autorização de cookies de terceiros, usar isolamento por origem para ETP.
        // Em modo de compatibilidade, preservar ACCEPT_ALL e explicar que ETP fica suspensa.
        block.setCookieBehavior(BrowserPrefs.thirdPartyCookies(context)
            ? ContentBlocking.CookieBehavior.ACCEPT_ALL
            : enabled ? ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS
                      : ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY);
        block.setEnhancedTrackingProtectionLevel(!enabled ? ContentBlocking.EtpLevel.NONE
            : GeckoProtectionProfile.strict(mode) ? ContentBlocking.EtpLevel.STRICT
                                                   : ContentBlocking.EtpLevel.DEFAULT);
        block.setEnhancedTrackingProtectionCategory(GeckoProtectionProfile.strict(mode)
            ? ContentBlocking.EtpCategory.STRICT : ContentBlocking.EtpCategory.STANDARD);
        block.setStrictSocialTrackingProtection(enabled && GeckoProtectionProfile.extra(mode));
        block.setQueryParameterStrippingEnabled(enabled && GeckoProtectionProfile.extra(mode));
    }

    static boolean trackingEnabled(Context context) {
        return GeckoProtectionProfile.active(AdBlockController.savedMode(context),
            AdBlockController.savedTrackers(context), BrowserPrefs.thirdPartyCookies(context));
    }

    static void updatePreferences(Context context) {
        GeckoRuntime gecko = get(context);
        applyProtection(gecko.getSettings(), context);
        gecko.getSettings().setFontSizeFactor(BrowserPrefs.textZoom(context) / 100f);
    }

    static void clear(long flags, Runnable done, java.util.function.Consumer<String> error) {
        getContextStorage().clearData(flags).accept(
            ignored -> { if(done!=null)done.run(); },
            exception -> { if(error!=null)error.accept("Falha ao apagar dados do GeckoView"); }
        );
    }

    static StorageController getContextStorage() {
        if(runtime == null)throw new IllegalStateException("GeckoRuntime não inicializado");
        return runtime.getStorageController();
    }
}
