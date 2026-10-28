#!/usr/bin/env python3
"""Javac semântico para TransferStarter real e suas novas APIs de permissão Android."""
from pathlib import Path
import tempfile, subprocess
src=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/baixafacil'
stubs={
 'android/Manifest.java': 'package android; public class Manifest { public static class permission {public static final String POST_NOTIFICATIONS="android.permission.POST_NOTIFICATIONS";} }',
 'android/content/pm/PackageManager.java': 'package android.content.pm; public class PackageManager { public static final int PERMISSION_GRANTED=0; }',
 'android/content/SharedPreferences.java': '''package android.content; public interface SharedPreferences {
   boolean getBoolean(String key,boolean def);
   Editor edit();
   interface Editor { Editor putBoolean(String key,boolean value); boolean commit(); }
 }''',
 'android/content/Context.java': '''package android.content; public class Context {
   public static final int MODE_PRIVATE=0;
   public SharedPreferences getSharedPreferences(String key,int mode){return null;}
 }''',
 'android/content/Intent.java': '''package android.content; public class Intent {
   public Intent(Context context,Class<?> klass){}
   public Intent setAction(String action){return this;}
   public Intent putExtra(String key,String value){return this;}
 }''',
 'android/app/Activity.java': '''package android.app; public class Activity extends android.content.Context {
   public int checkSelfPermission(String perm){return 0;}
   public void requestPermissions(String[] names,int code){}
   public void startForegroundService(android.content.Intent intent){}
 }''',
 'android/os/Build.java': '''package android.os; public class Build {
   public static class VERSION {public static int SDK_INT=34;}
 }''',
 'br/com/baixafacil/DownloadRestrictions.java': '''package br.com.baixafacil; class DownloadRestrictions {
   static String blocked(android.app.Activity a){return null;}
 }''',
 'br/com/baixafacil/DownloadStore.java': '''package br.com.baixafacil; class DownloadStore {
   static void update(android.app.Activity a,String id,String status,Object a1,int b,int c,Object d,String reason){}
   static Row get(android.app.Activity a,String id){return null;}
   static class Row {String optString(String key){return null;}}
 }''',
 'br/com/baixafacil/TransferJobService.java': '''package br.com.baixafacil; class TransferJobService {
   static boolean enqueue(android.app.Activity a,String id,String url,String cookie,String agent,String referer){return true;}
   static boolean isActive(String id){return false;}
   static int queuePosition(android.app.Activity a,String id){return 0;}
   static boolean hasScheduledJob(android.app.Activity a){return false;}
   static void control(android.app.Activity a,String id,String action){}
 }''',
 'br/com/baixafacil/TransferService.java': '''package br.com.baixafacil; class TransferService {
   static final String ACTION_ENQUEUE="enq",EXTRA_ID="id",EXTRA_URL="url",EXTRA_COOKIE="cookie",EXTRA_USER_AGENT="agent",EXTRA_REFERER="referer";
   static boolean isActive(String id){return false;}
   static int queuePosition(String id){return 0;}
 }'''
}
with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    for name,body in stubs.items():
        path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body,encoding='utf-8')
    files=[str(root/name) for name in stubs]
    files.extend([str(src/'NotificationPermissionPolicy.java'),str(src/'TransferStarter.java')])
    subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',td,*files],check=True)
print('PASSOU: javac semântico de TransferStarter real com stubs Android.')
