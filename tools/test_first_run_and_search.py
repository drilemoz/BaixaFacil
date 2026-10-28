#!/usr/bin/env python3
"""Regressão alpha.71: avisos contextuais e busca somente em domínios autorizados."""
from pathlib import Path
import subprocess, tempfile
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/baixafacil'
main=(src/'MainActivity.java').read_text()
starter=(src/'TransferStarter.java').read_text()
settings=(src/'SettingsActivity.java').read_text()
assert 'requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}' not in main
assert 'maybeAskForDownloadNotifications(activity)' in starter
assert 'TransferJobService.enqueue(activity,id,url,cookie,agent,referer)' in starter
assert 'activity.startForegroundService(request)' in starter
assert 'download_notifications_asked' in starter and '.commit()' in starter
assert 'Notificações de downloads' in settings and 'ACTION_APP_NOTIFICATION_SETTINGS' in settings
with tempfile.TemporaryDirectory() as td:
  file=Path(td)/'PolicyTest.java'
  file.write_text('''package br.com.baixafacil;
public class PolicyTest {
 static void check(boolean b, String message){ if(!b) throw new AssertionError(message); }
 public static void main(String[] args){
  check(!NotificationPermissionPolicy.shouldAsk(32,false,false),"Android 12");
  check(!NotificationPermissionPolicy.shouldAsk(35,true,false),"permission granted");
  check(!NotificationPermissionPolicy.shouldAsk(35,false,true),"already prompted");
  check(NotificationPermissionPolicy.shouldAsk(35,false,false),"first download Android 15");
  int level=AdultContentPolicy.STRICT;
  for(String host:new String[]{"google.bad","google.evil","ads.google.com","search.bing.com","evil.duckduckgo.com"}) {
   String url="https://"+host+"/search?q=test&safe=off";
   check(AdultContentPolicy.safeSearchUrl(url,level).equals(url),"no rewrite "+host);
  }
  check(AdultContentPolicy.safeSearchUrl("https://google.com.br/search?q=x&safe=off",level).endsWith("safe=active"),"Brazil google");
  check(AdultContentPolicy.safeSearchUrl("https://www.google.co.uk/search?q=x",level).endsWith("safe=active"),"UK google");
  String encoded=AdultContentPolicy.safeSearchUrl("https://google.com/search?q=x&%73afe=off&SAFE=off",level);
  check(encoded.equals("https://google.com/search?q=x&safe=active"),"encoded duplicates: "+encoded);
  check(AdultContentPolicy.safeSearchUrl(encoded,level).equals(encoded),"idempotent");
  check(AdultContentPolicy.blocked("https://pornhub.com",level),"block regression");
  System.out.println("PASSOU: SafeSearch com allowlist e notificações no primeiro download.");
 }
}
''',encoding='utf-8')
  subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',td,str(src/'AdultContentPolicy.java'),str(src/'NotificationPermissionPolicy.java'),str(file)],check=True)
  subprocess.run(['java','-cp',td,'br.com.baixafacil.PolicyTest'],check=True)
