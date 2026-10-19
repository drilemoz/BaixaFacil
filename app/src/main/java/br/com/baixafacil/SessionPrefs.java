package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;
import org.mozilla.geckoview.StorageController;

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
    /** Executa uma só vez por processo, antes do primeiro carregamento GeckoView. */
    static synchronized boolean shouldClearAtStart(Context context){
        if(initializedThisProcess)return false;
        initializedThisProcess=true;
        return !keepLogins(context);
    }
    static void clearForNewSession(Context context,Runnable done){
        GeckoEngine.get(context);
        GeckoEngine.clear(StorageController.ClearFlags.SITE_DATA,done,
            error -> android.widget.Toast.makeText(context,
                "Não foi possível limpar a sessão anterior; navegação suspensa para proteger seus dados.",
                android.widget.Toast.LENGTH_LONG).show());
    }
}
