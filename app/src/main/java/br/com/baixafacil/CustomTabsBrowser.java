package br.com.baixafacil;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;

/** Abre sites em navegador instalado, sem transferir cookies ou sessões da WebView. */
final class CustomTabsBrowser {
    private static final String PREFS="browser_external_compatibility";
    private static final String ENABLED="use_custom_tabs_for_login";
    private CustomTabsBrowser() {}
    static boolean forLogin(Context c) {
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(ENABLED,true);
    }
    static void setForLogin(Context c,boolean enabled){
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(ENABLED,enabled).apply();
    }
    static boolean open(Activity activity,String url) {
        if(url==null || !url.equals(UrlTools.extract(url)))return false;
        try {
            String pkg=CustomTabsClient.getPackageName(activity,null);
            if(pkg==null || pkg.equals(activity.getPackageName()))return false;
            CustomTabsIntent tab=new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(new Ui.Palette(activity).teal)
                .setShareState(CustomTabsIntent.SHARE_STATE_DEFAULT)
                .build();
            // Pacote explícito: jamais resolver nossa própria MainActivity como navegador externo.
            tab.intent.setPackage(pkg);
            tab.launchUrl(activity,Uri.parse(url));
            return true;
        } catch (ActivityNotFoundException | SecurityException error) {
            AppEventLog.warn(activity,"Custom Tabs","Nenhum navegador compatível disponível para abrir o endereço.");
            return false;
        } catch (RuntimeException error) {
            AppEventLog.error(activity,"Custom Tabs",error);
            return false;
        }
    }
}
