#!/usr/bin/env python3
"""Verifica contratos até a alpha.63 e exercita o parser real de importação com stubs JDK.
Não equivale a teste Android/GeckoView em aparelho.
"""
from pathlib import Path
import tempfile,subprocess,textwrap
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/br/com/baixafacil'
read=lambda name:(SRC/name).read_text(encoding='utf-8')
pane=read('BrowserPane.java')
assert 'ListPopupWindow' not in pane and 'iconButton("copy","Copiar endereço"' not in pane
assert 'iconButton("forward","Abrir endereço"' not in pane
assert 'iconButton("settings","Configurações"' not in pane
for symbol in ['openAddressEditor','showCompactMenu','startQrScanner','startVoiceSearch','clipboardUrl','BrowserTopSites.mostVisited','showDetectedImages','setLoadingProgress','progressLabel']:
    assert symbol in pane,symbol
assert 'AndroidKeyStore' in read('SecureLoginStore.java')
assert 'if(guid==null || guid.isEmpty() || !readable(c))return false;' in read('SecureLoginStore.java')
assert 'static synchronized boolean deleteAll(Context c)' in read('SecureLoginStore.java')
assert 'if(!SecureLoginStore.readable(host))' in pane
assert 'confirmDeleteAllLogins' in pane
assert 'AES/GCM/NoPadding' in read('SecureLoginStore.java')
assert 'setAutocompleteStorageDelegate' in read('GeckoEngine.java')
assert 'BrowserImport.Kind.HISTORY' in pane and 'showImportPreview' in pane
assert 'BrowserPrefs.importHistory' in read('BrowserImport.java')
assert 'BrowserTopSites.record' not in read('BrowserImport.java')
assert 'MAX_HISTORY = 120' in read('BrowserPrefs.java')
assert 'onLoginSave' in read('GeckoPromptHandler.java')
assert 'SitePermissionDialog.show' in read('GeckoSitePermissions.java')
assert 'msg.includeImages' in (ROOT/'app/src/main/assets/baixafacil_gecko/media.js').read_text()
assert 'setImageBitmap(found)' in read('FaviconLoader.java')
assert 'com.journeyapps:zxing-android-embedded:4.3.0' in (ROOT/'app/build.gradle').read_text()
with tempfile.TemporaryDirectory(prefix='baixafacil-parser-') as temp:
    t=Path(temp)
    stubs={
        'android/content/Context.java':'package android.content; public class Context {}',
        'org/mozilla/geckoview/Autocomplete.java':'''package org.mozilla.geckoview;
          public class Autocomplete {
            public static class LoginEntry {
              public String origin,username,password;
              public static class Builder {
                private final LoginEntry value=new LoginEntry();
                public Builder origin(String v){value.origin=v;return this;}
                public Builder username(String v){value.username=v;return this;}
                public Builder password(String v){value.password=v;return this;}
                public LoginEntry build(){return value;}
              }
            }
          }''',
        'br/com/baixafacil/FavoriteRules.java':'''package br.com.baixafacil;
          public class FavoriteRules {
            public static String url(String u){return u!=null&&(u.startsWith("https://")||u.startsWith("http://"))?u:null;}
            public static String title(String title,String url){return title;}
          }''',
        'br/com/baixafacil/BrowserPrefs.java':'''package br.com.baixafacil;
          public class BrowserPrefs {
            public static int saved=0,historySaved=0;
            public static String safeHistoryUrl(String url){return url!=null&&url.startsWith("https://")&&!url.contains("token=")&&!url.contains("login")?url:null;}
            public static int importFavorites(android.content.Context c,java.util.List<BrowserImport.Record> r){saved+=r.size();return r.size();}
            public static int importHistory(android.content.Context c,java.util.List<BrowserImport.Record> r){historySaved+=r.size();return r.size();}
            public static boolean addFavorite(android.content.Context c,String url,String title){
              if(!url.startsWith("https://")&&!url.startsWith("http://"))return false;
              saved++;return true;
            }
          }''',
        'br/com/baixafacil/SecureLoginStore.java':'''package br.com.baixafacil;
          public class SecureLoginStore {
            public static int saved=0;
            public static String secureOrigin(String v){
              try{java.net.URI u=new java.net.URI(v);
                if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)return null;
                return "https://"+u.getHost();
              }catch(Exception e){return null;}
            }
            public static boolean save(android.content.Context c,org.mozilla.geckoview.Autocomplete.LoginEntry e){saved++;return true;}
            public static boolean readable(android.content.Context c){return true;}
          }''',
        'br/com/baixafacil/BrowserImportTestMain.java':r'''package br.com.baixafacil;
          public class BrowserImportTestMain {
            static void check(boolean b){if(!b)throw new AssertionError();}
            public static void main(String[] args){
              android.content.Context c=new android.content.Context();
              String html="<DT><A HREF=\"https://example.com?a=1&amp;b=2\">Exemplo</A>"+
                "<a href='javascript:alert(1)'>Perigoso</a>";
              check(BrowserImport.importBookmarks(c,html)==1);
              String csv="name,url,username,password\r\nExemplo,https://example.com/login,ana,\"s,egredo\"\r\n"+
                 "Outro,http://exemplo.com,user,inseguro\r\n";
              check(BrowserImport.importLogins(c,csv)==1);
              check(SecureLoginStore.saved==1);
              check(BrowserImport.importLogins(c,"url,username,password\nhttps://x.com,user,pass")==1);
              check(BrowserImport.importLogins(c,"username,password\nuser,pass")==0);
              check(BrowserImport.importLogins(c,"url,username,password\nhttps://multi.test,ana,\"senha\ncom quebra\"")==1);
              check(BrowserImport.importLogins(c,"url,username,password\nhttps://invalid.test,ana,\"sem fechar")==0);
              check(CsvRows.parse("url,username,password\nhttps://ok.test,joao,\"uma,duas\"",500).size()==2);
              check(CsvRows.parse("url,username,password\nhttps://x.test,ana,\"a\"n",500)==null);
              check(BrowserImport.importBookmarks(c,"<a href='https://valid.test'>oi</a>"+"x".repeat(400001))==0);
              BrowserImport.Preview fav=BrowserImport.preview(BrowserImport.Kind.BOOKMARKS,
                "<DT><A ADD_DATE=\"123\" HREF=\"https://site.test/a?x=1&amp;y=2\">Nome &amp; Site</A>"+
                "<A HREF=\"javascript:alert(1)\">Ruim</A>"+
                "<A HREF=\"https://site.test/a?x=1&amp;y=2\">Duplicado</A>");
              check(fav!=null && fav.records.size()==1 && fav.rejected==2);
              check(fav.records.get(0).url.contains("x=1&y=2"));
              check(BrowserImport.importPreview(c,fav)==1);
              BrowserImport.Preview history=BrowserImport.preview(BrowserImport.Kind.HISTORY,
                "url,title,visit_time\r\nhttps://news.test/1,Notícia,123\r\n"+
                "https://news.test/1,Repetida,124\r\nhttps://news.test/?token=secret,Protegida,125");
              check(history!=null && history.records.size()==1 && history.rejected==2);
              check(BrowserImport.importPreview(c,history)==1 && BrowserPrefs.historySaved==1);
              check(BrowserImport.preview(BrowserImport.Kind.BOOKMARKS,
                "url,name\nhttps://example.test,Exemplo").records.size()==1);
              check(BrowserImport.preview(BrowserImport.Kind.HISTORY,
                "url,username,password\nhttps://news.test,me,secret")==null);
              check(BrowserImport.preview(BrowserImport.Kind.BOOKMARKS,
                "url,name,password\nhttps://news.test,Title,secret")==null);
              check(BrowserImport.preview(BrowserImport.Kind.HISTORY,
                "name,nothing\nsite,foo")==null);
              check(BrowserImport.preview(BrowserImport.Kind.HISTORY,
                "url,title\nhttps://ok.test,\"unclosed")==null);
            }
          }''',
    }
    for path,body in stubs.items():
        dest=t/path;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(body,encoding='utf-8')
    sources=[str(p) for p in t.rglob('*.java')]+[str(SRC/'BrowserImport.java'),str(SRC/'CsvRows.java')]
    result=subprocess.run(['javac','-d',str(t/'classes'),*sources],capture_output=True,text=True)
    assert result.returncode==0,result.stderr
    result=subprocess.run(['java','-cp',str(t/'classes'),'br.com.baixafacil.BrowserImportTestMain'],capture_output=True,text=True)
    assert result.returncode==0,result.stderr
print('PASSOU: contratos UI, mídia, permissões, cofre e importação HTML/CSV (JDK com stubs)')
