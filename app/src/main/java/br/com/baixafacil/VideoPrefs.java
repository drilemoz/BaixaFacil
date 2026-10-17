package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;
/** Configurações locais e conservadoras de detecção. Nunca guarda URLs de vídeo ou cookies. */
final class VideoPrefs {
    private static final String FILE="video_discovery";
    private VideoPrefs(){}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
    static boolean autoScan(Context c){return prefs(c).getBoolean("auto_scan",true);}
    static void setAutoScan(Context c,boolean value){prefs(c).edit().putBoolean("auto_scan",value).apply();}
    static boolean showStreams(Context c){return prefs(c).getBoolean("streaming",true);}
    static void setShowStreams(Context c,boolean value){prefs(c).edit().putBoolean("streaming",value).apply();}
    static boolean showSources(Context c){return prefs(c).getBoolean("direct_links",true);}
    static void setShowSources(Context c,boolean value){prefs(c).edit().putBoolean("direct_links",value).apply();}
}
