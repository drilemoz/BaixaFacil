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
            settings.getContentBlocking().setCookieBehavior(cookiePolicy(context));
            runtime = GeckoRuntime.create(context.getApplicationContext(), settings);
        }
        return runtime;
    }

    private static int cookiePolicy(Context context) {
        return BrowserPrefs.thirdPartyCookies(context)
            ? ContentBlocking.CookieBehavior.ACCEPT_ALL
            : ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY;
    }

    static void updatePreferences(Context context) {
        GeckoRuntime gecko = get(context);
        gecko.getSettings().getContentBlocking().setCookieBehavior(cookiePolicy(context));
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
