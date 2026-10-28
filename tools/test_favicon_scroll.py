#!/usr/bin/env python3
"""Compila o carregador real com stubs Android; testa política, origem e integração de scroll."""
from pathlib import Path
import subprocess, tempfile, textwrap
root=Path(__file__).resolve().parents[1]
source=root/'app/src/main/java/br/com/baixafacil'
main=(source/'MainActivity.java').read_text(encoding='utf-8')
browser=(source/'BrowserPane.java').read_text(encoding='utf-8')
loader=(source/'FaviconLoader.java').read_text(encoding='utf-8')
for expected in ['setOnScrollListener(new AbsListView.OnScrollListener()',
                 'if(!moving && adapter!=null && downloadsVisible && activityVisible)',
                 'outer.removeAllViews()',
                 'FaviconWorkPolicy.shouldLoadPreview(finished,scrollingDownloads,downloadsVisible && activityVisible)']:
    assert expected in main, expected
assert 'favicons.onTrimMemory(level)' in browser
for expected in ['setInstanceFollowRedirects(false)', 'negative.remove(origin)',
                 'MAX_PENDING_ORIGINS', 'MAX_WAITERS_PER_ORIGIN',
                 'worker.getQueue().clear()', 'generation!=currentGeneration',
                 'origin.equals(target.getTag())', 'cache.evictAll()']:
    assert expected in loader, expected
stubs={
'android/content/Context.java': 'package android.content; public class Context {}',
'android/graphics/Bitmap.java': '''package android.graphics; public class Bitmap {
    public int getByteCount(){return 1024;} }''',
'android/graphics/BitmapFactory.java': '''package android.graphics; public class BitmapFactory {
    public static class Options {public boolean inJustDecodeBounds; public int outWidth=16,outHeight=16,inSampleSize=1;}
    public static Bitmap decodeByteArray(byte[] data,int start,int length,Options options){return new Bitmap();}
}''',
'android/net/Uri.java': '''package android.net; public class Uri {
    private final java.net.URI inner;
    private Uri(String str){inner=java.net.URI.create(str);}
    public static Uri parse(String s){return new Uri(s);}
    public String getScheme(){return inner.getScheme();}
    public String getHost(){return inner.getHost();}
    public String getUserInfo(){return inner.getUserInfo();}
}''',
'android/os/SystemClock.java': '''package android.os; public class SystemClock {
    public static long uptimeMillis(){return System.nanoTime()/1000000L;} }''',
'android/util/LruCache.java': '''package android.util; import java.util.LinkedHashMap;
    public class LruCache<K,V> {
        private final LinkedHashMap<K,V> data=new LinkedHashMap<>();
        public LruCache(int size){}
        protected int sizeOf(K key,V value){return 1;}
        public synchronized V get(K key){return data.get(key);}
        public synchronized V put(K key,V value){return data.put(key,value);}
        public synchronized void evictAll(){data.clear();}
    }''',
'android/widget/ImageView.java': '''package android.widget; public class ImageView {
    private Object tag; public void setTag(Object value){tag=value;}
    public Object getTag(){return tag;}
    public void setImageBitmap(android.graphics.Bitmap value){}
    public boolean post(Runnable r){r.run();return true;}
}''',
'br/com/baixafacil/FaviconCheck.java': '''package br.com.baixafacil;
import java.lang.reflect.Method;
public class FaviconCheck {
    private static void ok(boolean good){if(!good)throw new AssertionError();}
    public static void main(String[] args)throws Exception {
        ok(FaviconWorkPolicy.MAX_PENDING_ORIGINS==24);
        ok(FaviconWorkPolicy.MAX_WAITERS_PER_ORIGIN==8);
        ok(FaviconWorkPolicy.MAX_FAILED_ORIGINS==96);
        ok(FaviconWorkPolicy.shouldLoadPreview(true,false,true));
        ok(!FaviconWorkPolicy.shouldLoadPreview(true,true,true));
        ok(!FaviconWorkPolicy.shouldLoadPreview(false,false,true));
        ok(FaviconWorkPolicy.stillCoolingDown(299999,0));
        ok(!FaviconWorkPolicy.stillCoolingDown(300000,0));
        ok(!FaviconWorkPolicy.stillCoolingDown(10,100));
        ok(!FaviconWorkPolicy.shouldTrimCache(20));
        ok(FaviconWorkPolicy.shouldTrimCache(15));
        Method m=FaviconLoader.class.getDeclaredMethod("safeOrigin",String.class);
        m.setAccessible(true);
        ok("https://example.org".equals(m.invoke(null,"https://EXAMPLE.ORG/page?x=1")));
        ok(m.invoke(null,"http://example.org")==null);
        ok(m.invoke(null,"https://user:pass@example.org/path")==null);
        ok(m.invoke(null,"https://localhost:8000")==null);
        ok(m.invoke(null,"https://127.0.0.1/a")==null);
        FaviconLoader loader=new FaviconLoader();
        loader.onTrimMemory(15);
        loader.shutdown();
        loader.load(new android.content.Context(),"https://example.org",new android.widget.ImageView());
        System.out.println("FaviconPolicy / loader / scroll: OK");
    }
}'''
}
with tempfile.TemporaryDirectory(prefix='bf65_favicon_') as temp:
    tmp=Path(temp);files=[]
    for name,content in stubs.items():
        f=tmp/name;f.parent.mkdir(parents=True,exist_ok=True)
        f.write_text(textwrap.dedent(content),encoding='utf-8');files.append(str(f))
    files+=list(map(str,[source/'FaviconWorkPolicy.java',source/'FaviconLoader.java']))
    subprocess.run(['javac','-encoding','UTF-8','-d',str(tmp/'compiled'),*files],check=True)
    subprocess.run(['java','-cp',str(tmp/'compiled'),'br.com.baixafacil.FaviconCheck'],check=True)
print('Testes alpha.65: OK')
