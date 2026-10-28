package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferências locais do assistente e do filtro, sem telemetria ou envio de URLs. */
final class ContentPrefs {
    private static final String FILE="content_protection_v1";
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
    static int level(Context c){return AdultContentPolicy.normalize(prefs(c).getInt("adult_level",AdultContentPolicy.OFF));}
    static void setLevel(Context c,int level){prefs(c).edit().putInt("adult_level",AdultContentPolicy.normalize(level)).apply();}
    static boolean needsSetup(Context c){return !prefs(c).getBoolean("setup_complete",false);}
    static void completeSetup(Context c){prefs(c).edit().putBoolean("setup_complete",true).commit();}
    static void resetSetup(Context c){prefs(c).edit().putBoolean("setup_complete",false).apply();}
}
