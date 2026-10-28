#!/usr/bin/env python3
"""Compila o GeckoSitePermissions Java REAL com stubs de API e testa callbacks.

Complementa os testes de política: a alpha.67 passou no parser, mas falhou no javac
por usar o identificador inexistente pendingAndroidOrigin (workflow Gerar APK 34).
Não substitui a compilação Gradle com Android SDK/GeckoView.
"""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/br/com/baixafacil'
STUBS = {
    'android/Manifest.java': '''package android;
public final class Manifest {public static final class permission {
  public static final String CAMERA="android.permission.CAMERA",RECORD_AUDIO="android.permission.RECORD_AUDIO",
    ACCESS_COARSE_LOCATION="android.permission.ACCESS_COARSE_LOCATION", ACCESS_FINE_LOCATION="android.permission.ACCESS_FINE_LOCATION";
}}''',
    'android/content/pm/PackageManager.java': '''package android.content.pm;
public final class PackageManager { public static final int PERMISSION_GRANTED=0; }''',
    'android/app/AlertDialog.java': '''package android.app;
public class AlertDialog {private boolean showing=true;
  public boolean isShowing(){return showing;} public void dismiss(){showing=false;}
}''',
    'org/mozilla/geckoview/GeckoResult.java': '''package org.mozilla.geckoview;
public class GeckoResult<T> {
  public T value;
  public static <V> GeckoResult<V> fromValue(V value){GeckoResult<V> r=new GeckoResult<>();r.value=value;return r;}
  public void complete(T value){this.value=value;}
}''',
    'org/mozilla/geckoview/GeckoSession.java': '''package org.mozilla.geckoview;
public class GeckoSession { public interface PermissionDelegate {
  int PERMISSION_AUTOPLAY_INAUDIBLE=1,PERMISSION_GEOLOCATION=2,PERMISSION_AUTOPLAY_AUDIBLE=3;
  interface Callback {void reject();void grant();}
  interface MediaCallback {void reject();void grant(MediaSource camera,MediaSource microphone);}
  class MediaSource {
    public static final int SOURCE_SCREEN=1,SOURCE_CAMERA=2,SOURCE_MICROPHONE=3;
    public int source;
  }
  class ContentPermission {
    public static final int VALUE_ALLOW=1,VALUE_DENY=2;
    public int value,permission;
    public String uri;
    public void notifyShown(){}
  }
  void onAndroidPermissionsRequest(GeckoSession session,String[] permissions,Callback callback);
  GeckoResult<Integer> onContentPermissionRequest(GeckoSession session,ContentPermission permission);
  void onMediaPermissionRequest(GeckoSession session,String uri,MediaSource[] video,MediaSource[] audio,MediaCallback callback);
}}''',
    'br/com/baixafacil/MainActivity.java': '''package br.com.baixafacil;
public class MainActivity {
  public String[] lastRequested;
  public boolean isFinishing(){return false;} public boolean isDestroyed(){return false;}
  public int checkSelfPermission(String permission){return -1;}
  public void requestPermissions(String[] permissions,int code){lastRequested=permissions;}
}''',
    'br/com/baixafacil/SitePermissionDialog.java': '''package br.com.baixafacil;
import android.app.AlertDialog;
public final class SitePermissionDialog {
  public static AlertDialog show(MainActivity host,String title,String site,String description,
    Runnable allow,Runnable deny,Runnable close){return new AlertDialog();}
}''',
    'br/com/baixafacil/PermissionBuildRegression.java': '''package br.com.baixafacil;
import android.Manifest;
import android.content.pm.PackageManager;
import org.mozilla.geckoview.GeckoSession;
import java.util.concurrent.atomic.AtomicReference;
public class PermissionBuildRegression {
  static class Callback implements GeckoSession.PermissionDelegate.Callback {
    int grants,denials;
    public void grant(){grants++;} public void reject(){denials++;}
  }
  static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
  public static void main(String[] args){
    MainActivity host=new MainActivity();
    AtomicReference<String> page=new AtomicReference<>("https://site.example/a");
    GeckoSitePermissions permissions=new GeckoSitePermissions(host,page::get);
    // Troca de origem antes da resposta do Android: rejeitar sem concessão tardia.
    Callback different=new Callback();
    permissions.onAndroidPermissionsRequest(null,new String[]{Manifest.permission.CAMERA},different);
    check(host.lastRequested!=null,"a solicitacao deveria chegar ao Android");
    page.set("https://other.example/");
    permissions.onPageLocationChanged(page.get());
    check(different.denials==1 && different.grants==0,"troca de origem nao cancelou");
    permissions.onRequestPermissionsResult(GeckoSitePermissions.REQUEST_CODE,
      new String[]{Manifest.permission.CAMERA},new int[]{PackageManager.PERMISSION_GRANTED});
    check(different.denials==1 && different.grants==0,"resposta antiga concedeu permissao");
    // Página nova na MESMA origem deve preservar a permissão pendente.
    page.set("https://site.example/page1");
    Callback same=new Callback();
    permissions.onAndroidPermissionsRequest(null,new String[]{Manifest.permission.CAMERA},same);
    page.set("https://site.example/page2");
    permissions.onPageLocationChanged(page.get());
    check(same.denials==0,"navegacao na mesma origem cancelou a solicitacao");
    permissions.onRequestPermissionsResult(GeckoSitePermissions.REQUEST_CODE,
      new String[]{Manifest.permission.CAMERA},new int[]{PackageManager.PERMISSION_GRANTED});
    check(same.grants==1 && same.denials==0,"nao concedeu na mesma origem");
    // Troca somente de porta tambem invalida a autorizacao.
    Callback port=new Callback();
    permissions.onAndroidPermissionsRequest(null,new String[]{Manifest.permission.RECORD_AUDIO},port);
    page.set("https://site.example:8443/new");
    permissions.onPageLocationChanged(page.get());
    check(port.denials==1 && port.grants==0,"porta diferente nao cancelada");
    System.out.println("GeckoSitePermissions: javac real e callbacks por origem OK");
  }
}''',
}
with tempfile.TemporaryDirectory(prefix='bf68_gecko_javac_') as directory:
    root=Path(directory)
    sources=[]
    for name, code in STUBS.items():
        file=root/name
        file.parent.mkdir(parents=True,exist_ok=True)
        file.write_text(code,encoding='utf-8')
        sources.append(str(file))
    classes=root/'classes'
    classes.mkdir()
    source_class = SRC/'GeckoSitePermissions.java'
    compilable = ['javac','--release','17','-encoding','UTF-8','-d',str(classes),
                  *sources,str(SRC/'GeckoPermissionPolicy.java'),str(source_class)]
    subprocess.run(compilable,check=True)
    subprocess.run(['java','-cp',str(classes),'br.com.baixafacil.PermissionBuildRegression'],check=True)
    # Prova que o novo teste detecta exatamente a falha do workflow 34.
    broken = root/'broken/br/com/baixafacil/GeckoSitePermissions.java'
    broken.parent.mkdir(parents=True,exist_ok=True)
    source = source_class.read_text(encoding='utf-8')
    needle='if(requestedAndroidOrigin!=null && !GeckoPermissionPolicy.sameOrigin(requestedAndroidOrigin,destination))'
    assert needle in source, 'a guarda de origem foi modificada sem atualizar a regressao'
    broken.write_text(source.replace(needle,
        'if(pendingAndroidOrigin!=null && !GeckoPermissionPolicy.sameOrigin(pendingAndroidOrigin,destination))',1),encoding='utf-8')
    regression=subprocess.run([*compilable[:-1],str(broken)],capture_output=True,text=True)
    assert regression.returncode!=0 and 'pendingAndroidOrigin' in regression.stderr and 'cannot find symbol' in regression.stderr, \
        'o javac deveria reprovar o identificador inexistente do workflow 34'
print('PASSOU: javac real, cancelamento por origem e regressao negativa da alpha.67.')
