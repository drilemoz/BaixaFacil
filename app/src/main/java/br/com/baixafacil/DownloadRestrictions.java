package br.com.baixafacil;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;

/** Preferências locais e avaliação conservadora da rede ativa/bateria. */
final class DownloadRestrictions {
    private static final String PREF = "download_restrictions_v1";
    private static final String WIFI_ONLY = "wifi_only";
    private static final String ALLOW_MOBILE = "allow_mobile";
    private static final String PROTECT_BATTERY = "protect_battery";
    static final int BATTERY_THRESHOLD = 20;

    private DownloadRestrictions() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
    static boolean wifiOnly(Context c) { return prefs(c).getBoolean(WIFI_ONLY, false); }
    static boolean allowMobile(Context c) { return prefs(c).getBoolean(ALLOW_MOBILE, true); }
    static boolean protectBattery(Context c) { return prefs(c).getBoolean(PROTECT_BATTERY, false); }
    static void setWifiOnly(Context c, boolean enabled) { prefs(c).edit().putBoolean(WIFI_ONLY,enabled).apply(); }
    static void setAllowMobile(Context c, boolean enabled) { prefs(c).edit().putBoolean(ALLOW_MOBILE,enabled).apply(); }
    static void setProtectBattery(Context c, boolean enabled) { prefs(c).edit().putBoolean(PROTECT_BATTERY,enabled).apply(); }

    /** null significa permitido no instante da consulta, nunca garantia futura de rede. */
    static String blocked(Context context) {
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network active=cm==null?null:cm.getActiveNetwork();
        NetworkCapabilities cap=active==null?null:cm.getNetworkCapabilities(active);
        boolean connected=cap!=null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean wifi=cap!=null && cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean ethernet=cap!=null && cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        boolean cellular=cap!=null && cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

        int percentage=-1;
        boolean charging=false;
        if (protectBattery(context)) {
            Intent state=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (state!=null) {
                int level=state.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                int scale=state.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
                if (level>=0 && scale>0) percentage=(int)((long)level*100/scale);
                int status=state.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
                charging=status==BatteryManager.BATTERY_STATUS_CHARGING
                        || status==BatteryManager.BATTERY_STATUS_FULL;
            }
        }
        return DownloadPolicy.restriction(wifiOnly(context),allowMobile(context),protectBattery(context),
                BATTERY_THRESHOLD,connected,wifi,ethernet,cellular,charging,percentage);
    }
}
