package br.com.baixafacil;

import android.app.Application;

/** Inicialização leve do app: tema/privacidade continuam locais e sem coleta externa. */
public final class BaixaFacilApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppEventLog.install(this);
    }
}
