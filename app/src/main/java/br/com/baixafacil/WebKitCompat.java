package br.com.baixafacil;

import android.webkit.WebSettings;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/** Configuração opcional de APIs Jetpack, com fallback para WebViews mais antigas. */
final class WebKitCompat {
    private WebKitCompat() {}
    static void optimize(WebSettings settings) {
        // Baixa Fácil não exibe favicons; dispensa requisições adicionais.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOWNLOAD_FAVICONS_ENABLED)) {
            WebSettingsCompat.setDownloadFaviconsEnabled(settings, false);
        }
    }
}
