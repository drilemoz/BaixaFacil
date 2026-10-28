#!/usr/bin/env python3
"""Compila SetupActivity/ContentPrefs reais com stubs mínimos Android/UI (não é build Android)."""
from pathlib import Path
import tempfile,subprocess
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/baixafacil'
stubs={
'android/content/Context.java':'''package android.content; public class Context {public static final int MODE_PRIVATE=0;public SharedPreferences getSharedPreferences(String s,int n){return null;}}''',
'android/content/SharedPreferences.java':'''package android.content; public interface SharedPreferences {int getInt(String k,int d);boolean getBoolean(String k,boolean d);Editor edit(); interface Editor {Editor putInt(String k,int v);Editor putBoolean(String k,boolean v);void apply();boolean commit();}}''',
'android/app/Activity.java':'''package android.app; import android.content.Context; import android.os.Bundle; import android.view.View;public class Activity extends Context {protected void onCreate(Bundle b){} protected void onSaveInstanceState(Bundle b){} public void onBackPressed(){} public void finish(){} public void setContentView(View v){}}''',
'android/os/Bundle.java':'''package android.os;public class Bundle{public int getInt(String k){return 0;}public int getInt(String k,int d){return d;}public String getString(String k,String d){return d;}public boolean getBoolean(String k,boolean d){return d;} public void putInt(String k,int v){}public void putString(String k,String v){}public void putBoolean(String k,boolean b){}}''',
'android/view/Gravity.java':'''package android.view; public final class Gravity {public static final int CENTER=17;}''',
'android/view/View.java':'''package android.view;public class View {public void setClickable(boolean b){}public void setFocusable(boolean b){} public void setContentDescription(CharSequence s){} public void setOnClickListener(OnClickListener l){} public interface OnClickListener{void onClick(View v);}}''',
'android/widget/LinearLayout.java':'''package android.widget;import android.view.View;import android.content.Context; public class LinearLayout extends View {public LinearLayout(Context c){}public static class LayoutParams{public LayoutParams(int a,int b){}public LayoutParams(int a,int b,float f){} public int leftMargin;} public void setPadding(int a,int b,int c,int d){} public void setBackground(Object o){}public void setBackgroundColor(int color){} public void addView(View v){}public void addView(View v,LayoutParams p){}}''',
'android/widget/ScrollView.java':'''package android.widget;import android.content.Context;import android.view.View;public class ScrollView extends View{public ScrollView(Context c){}public void addView(View v){}}''',
'android/widget/TextView.java':'''package android.widget;import android.view.View; public class TextView extends View{public void setGravity(int g){}}''',
'br/com/baixafacil/Ui.java':'''package br.com.baixafacil;import android.app.Activity;import android.content.Context;import android.widget.*;public class Ui{public static int dp(Context c,int n){return n;}public static int mode(Context c){return 0;}public static void setMode(Context c,int n){}public static void selectTheme(Activity a){} public static void systemBars(Activity a,Palette p){}public static LinearLayout vertical(Context c){return new LinearLayout(c);}public static LinearLayout horizontal(Context c){return new LinearLayout(c);}public static TextView text(Context c,String s,int n,int color,boolean bold){return new TextView();}public static LinearLayout.LayoutParams lp(Context c,int w,int h,int m){return new LinearLayout.LayoutParams(w,h);}public static TextView pill(Context c,String s,int fg,int bg){return new TextView();}public static Object outline(Context c,int fill,int radius,int border){return null;}public static class Palette{public int text,surface,soft,teal,border,muted,warn,background;public Palette(Context c){}}}''',
'br/com/baixafacil/BrowserPrefs.java':'''package br.com.baixafacil;import android.content.Context;public class BrowserPrefs{static String searchEngine(Context c){return "Google";} static void setSearchEngine(Context c,String s){}}''',
'br/com/baixafacil/AdBlockController.java':'''package br.com.baixafacil;import android.content.Context;public class AdBlockController{static boolean savedTrackers(Context c){return true;}static void setTrackers(Context c,boolean v){}}''',
'br/com/baixafacil/DownloadRestrictions.java':'''package br.com.baixafacil;import android.content.Context;public class DownloadRestrictions{static boolean wifiOnly(Context c){return false;}static boolean protectBattery(Context c){return true;}static void setWifiOnly(Context c,boolean v){}static void setProtectBattery(Context c,boolean v){}}''',
}
with tempfile.TemporaryDirectory() as td:
    base=Path(td)
    for name,data in stubs.items():
        target=base/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(data)
    source_files=[str(base/name) for name in stubs]+[str(src/name) for name in ['AdultContentPolicy.java','ContentPrefs.java','SetupActivity.java']]
    subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',td,*source_files],check=True)
    print('PASSOU: compilação semântica de SetupActivity, ContentPrefs e AdultContentPolicy com stubs Android.')
