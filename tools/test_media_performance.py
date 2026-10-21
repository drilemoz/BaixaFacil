#!/usr/bin/env python3
"""Compila classes reais usando stubs Android e testa filas, cache e cadência com JDK."""
from pathlib import Path
import subprocess, tempfile, textwrap
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/br/com/baixafacil'
main=(java/'MainActivity.java').read_text(encoding='utf-8')
assert 'MediaWorkPolicy.refreshDelay(true,activeDownloadsForRefresh)' in main
assert 'mediaPreviewLoader.onTrimMemory(level)' in main
assert 'mediaPreviewLoader.onTrimMemory(15)' in main
assert 'activityVisible=false;' in main and 'if(downloadsVisible)refresh();' in main
assert 'private boolean activeDownloadsForRefresh;' in main

sources={
'android/content/Context.java': '''package android.content; public class Context {
 public Context getApplicationContext(){return this;}
 public ContentResolver getContentResolver(){return new ContentResolver();}
}''',
'android/content/ContentResolver.java': '''package android.content; import android.graphics.Bitmap; import android.net.Uri;
import android.util.Size; import java.util.concurrent.atomic.AtomicInteger;
public class ContentResolver {public static final AtomicInteger calls=new AtomicInteger();
 public Bitmap loadThumbnail(Uri u, Size s, Object signal){calls.incrementAndGet();
  try {Thread.sleep(100);}catch(InterruptedException ignored){}
  return new Bitmap(96,70);}
}''',
'android/graphics/Bitmap.java': '''package android.graphics; public class Bitmap{
private final int w,h;public Bitmap(int w,int h){this.w=w;this.h=h;}
public int getByteCount(){return w*h*4;}public int getWidth(){return w;}public int getHeight(){return h;}
}''',
'android/media/MediaMetadataRetriever.java': '''package android.media; import android.content.Context;
import android.net.Uri;import android.graphics.Bitmap;
public class MediaMetadataRetriever{public static final int METADATA_KEY_VIDEO_WIDTH=1;
 public static final int METADATA_KEY_VIDEO_HEIGHT=2;public static final int METADATA_KEY_DURATION=3;
 public static final int OPTION_CLOSEST_SYNC=4;
 public void setDataSource(Context c, Uri u){} public String extractMetadata(int k){return null;}
 public Bitmap getScaledFrameAtTime(long t,int o,int w,int h){return new Bitmap(w,h);}
 public void release(){} }
''',
'android/net/Uri.java': '''package android.net; import java.net.URI;
 public class Uri{final String s;Uri(String s){this.s=s;}
 public static Uri parse(String s){return new Uri(s);} public String getScheme(){return URI.create(s).getScheme();}
 public String getAuthority(){return URI.create(s).getAuthority();}
 @Override public String toString(){return s;}
}''',
'android/os/Looper.java':'package android.os; public class Looper{public static Looper getMainLooper(){return new Looper();}}',
'android/os/Handler.java': '''package android.os; public class Handler{public Handler(Looper l){}
 public boolean post(Runnable r){r.run();return true;}public void removeCallbacksAndMessages(Object token){}
}''',
'android/util/LruCache.java': '''package android.util; import java.util.LinkedHashMap;import java.util.Map;
public class LruCache<K,V>{private final int max;private final LinkedHashMap<K,V> map=new LinkedHashMap<>(16,.75f,true);
public LruCache(int size){max=size;}protected int sizeOf(K k,V v){return 1;}
public synchronized V get(K k){return map.get(k);}
public synchronized V put(K k,V v){V old=map.put(k,v);int sz=0; for(Map.Entry<K,V> e:map.entrySet())sz+=sizeOf(e.getKey(),e.getValue());
 while(sz>max&&!map.isEmpty()){K first=map.keySet().iterator().next();V rm=map.remove(first);sz-=sizeOf(first,rm);}return old;}
public synchronized void evictAll(){map.clear();}
}''',
'android/util/Size.java':'package android.util; public class Size{public Size(int w,int h){}}',
'br/com/baixafacil/DownloadInfo.java':'''package br.com.baixafacil;
public class DownloadInfo{public static String kind(String n){return n.endsWith(".mp4")?"Vídeos":"Imagens";}}
''',
'br/com/baixafacil/MediaWorkMain.java': '''package br.com.baixafacil;
import android.content.Context;import android.content.ContentResolver;import android.net.Uri;
import java.util.concurrent.CountDownLatch;import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public class MediaWorkMain {
 static void check(boolean x,String name){if(!x)throw new AssertionError(name);}
 static void wait(CountDownLatch x) throws Exception{check(x.await(5,TimeUnit.SECONDS),"callback timeout");}
 public static void main(String[] args) throws Exception {
  check(MediaWorkPolicy.refreshDelay(true,true)==1200,"active");
  check(MediaWorkPolicy.refreshDelay(true,false)==6000,"idle");
  check(MediaWorkPolicy.refreshDelay(false,true)==-1,"hidden");
  for(String s:new String[]{"Aguardando","Na fila","Conectando","Baixando","Retomando"})check(MediaWorkPolicy.needsFrequentRefresh(s),s);
  for(String s:new String[]{"Concluído","Cancelado","Falhou","Pausado","Interrompido"})check(!MediaWorkPolicy.needsFrequentRefresh(s),s);
  check(!MediaWorkPolicy.shouldTrimPreviewCache(20),"hidden alone");
  check(MediaWorkPolicy.shouldTrimPreviewCache(15),"critical");
  check(MediaWorkPolicy.shouldTrimPreviewCache(40),"background pressure");
  MediaPreviewLoader loader=new MediaPreviewLoader();Context app=new Context();
  CountDownLatch duplicate=new CountDownLatch(3);AtomicInteger calls=new AtomicInteger();
  for(int i=0;i<3;i++)loader.load(app,Uri.parse("content://media/test001"),"x.jpg",r->{check(r.bitmap!=null,"dedup image");calls.incrementAndGet();duplicate.countDown();});
  wait(duplicate);check(calls.get()==3,"all listeners notified");
  check(ContentResolver.calls.get()==1,"same URL extracted once");
  CountDownLatch cached=new CountDownLatch(1);
  loader.load(app,Uri.parse("content://media/test001"),"x.jpg",r->{check(r.bitmap!=null,"cached bitmap");cached.countDown();});
  wait(cached);check(ContentResolver.calls.get()==1,"cache reused");
  loader.onTrimMemory(20);
  CountDownLatch retained=new CountDownLatch(1);
  loader.load(app,Uri.parse("content://media/test001"),"x.jpg",r->retained.countDown());wait(retained);
  check(ContentResolver.calls.get()==1,"hidden does not evict cache");
  loader.onTrimMemory(15);
  CountDownLatch retried=new CountDownLatch(1);
  loader.load(app,Uri.parse("content://media/test001"),"x.jpg",r->retried.countDown());wait(retried);
  check(ContentResolver.calls.get()==2,"cache evicted after low RAM");
  // Pressão enquanto extração está pendente: resultado antigo não repopula o cache.
  CountDownLatch stale=new CountDownLatch(1);
  loader.load(app,Uri.parse("content://media/inflight"),"x.jpg",r->{check(r.bitmap==null,"discard stale bitmap");stale.countDown();});
  Thread.sleep(25);loader.onTrimMemory(15);
  wait(stale);
  int decoded=ContentResolver.calls.get();
  CountDownLatch extractedAgain=new CountDownLatch(1);
  loader.load(app,Uri.parse("content://media/inflight"),"x.jpg",r->extractedAgain.countDown());
  wait(extractedAgain);
  check(ContentResolver.calls.get()==decoded+1,"in-flight did not refill cache after trim");
  CountDownLatch queued=new CountDownLatch(1);AtomicInteger empty=new AtomicInteger();
  for(int i=0;i<15;i++)loader.load(app,Uri.parse("content://media/queue"+i),"x.jpg",r->{
    if(r.bitmap==null)empty.incrementAndGet();queued.countDown();});
  check(empty.get()>=5,"bounded queue rejects overload");
  // Aguarda primeiro trabalho e descarta cache para provar que shutdown não deixa fila ativa.
  wait(queued);loader.shutdown();
  System.out.println("PASSOU: cadência ativo/ocioso, deduplicação, cache, low-memory e fila limitada");
 }
}'''
}
with tempfile.TemporaryDirectory(prefix='bf64_test_') as temp:
    t=Path(temp)
    for name,content in sources.items():
        dest=t/name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(textwrap.dedent(content),encoding='utf-8')
    inputs=[str(t/name) for name in sources]
    inputs.extend(map(str,[java/'MediaWorkPolicy.java',java/'MediaPreviewLoader.java',java/'MediaPreviewFormat.java']))
    subprocess.run(['javac','-encoding','UTF-8','-d',str(t/'classes'),*inputs],check=True)
    subprocess.run(['java','-cp',str(t/'classes'),'br.com.baixafacil.MediaWorkMain'],check=True)
