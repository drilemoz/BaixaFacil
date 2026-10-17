package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.webkit.WebStorage;

/** Política local. Nunca enumera ou persiste valores de cookies. */
final class SessionPrefs {
    private static final String FILE="privacy_session_policy";
    private static final String KEEP="keep_logins_between_app_launches";
    private static boolean initializedThisProcess;
    private SessionPrefs(){}
    static boolean keepLogins(Context context){
        return context.getSharedPreferences(FILE,Context.MODE_PRIVATE).getBoolean(KEEP,true);
    }
    static void setKeepLogins(Context context,boolean keep){
        context.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit().putBoolean(KEEP,keep).apply();
    }
    /** Executa uma só vez por processo, antes do primeiro carregamento/restore da WebView. */
    static synchronized boolean shouldClearAtStart(Context context){
        if(initializedThisProcess)return false;
        initializedThisProcess=true;
        return !keepLogins(context);
    }
    static void clearForNewSession(Runnable done){
        // Os dois armazenamentos podem manter a autenticação; o histórico/favoritos não são apagados.
        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(success->{
            CookieManager.getInstance().flush();
            done.run();
        });
    }
}
