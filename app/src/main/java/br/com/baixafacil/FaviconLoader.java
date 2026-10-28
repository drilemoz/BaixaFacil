package br.com.baixafacil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.SystemClock;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Ícones da própria origem HTTPS, sem cookies, redirecionamentos ou terceiros. */
final class FaviconLoader {
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(2*1024*1024){
        @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getByteCount();}
    };
    // Uma thread de rede e fila limitada. Não reter dezenas de páginas já abandonadas.
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(FaviconWorkPolicy.MAX_PENDING_ORIGINS),r->{
            Thread thread=new Thread(r,"baixa-facil-favicon");
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    private final HashMap<String,ArrayList<WeakReference<ImageView>>> listeners=new HashMap<>();
    private final LinkedHashMap<String,Long> negative=new LinkedHashMap<>();
    private boolean closed;
    private long generation;

    void load(Context context,String url,ImageView view){
        if(view==null)return;
        String origin=safeOrigin(url);
        if(origin==null)return;
        view.setTag(origin);
        Bitmap ready;
        boolean start=false;
        long currentGeneration;
        synchronized(this){
            if(closed)return;
            ready=cache.get(origin);
            if(ready==null){
                long now=SystemClock.uptimeMillis();
                Long failedAt=negative.get(origin);
                if(failedAt!=null){
                    if(FaviconWorkPolicy.stillCoolingDown(now,failedAt))return;
                    negative.remove(origin);
                }
                ArrayList<WeakReference<ImageView>> waiters=listeners.get(origin);
                if(waiters!=null){
                    // A lista contém apenas weak refs: eliminamos referências já coletadas.
                    waiters.removeIf(ref->ref.get()==null);
                    if(waiters.size()<FaviconWorkPolicy.MAX_WAITERS_PER_ORIGIN)
                        waiters.add(new WeakReference<>(view));
                    return;
                }
                if(listeners.size()>=FaviconWorkPolicy.MAX_PENDING_ORIGINS)return;
                waiters=new ArrayList<>();waiters.add(new WeakReference<>(view));
                listeners.put(origin,waiters);
                start=true;
            }
            currentGeneration=generation;
        }
        if(ready!=null){view.setImageBitmap(ready);return;}
        if(!start)return;
        try{
            worker.execute(()->{
                synchronized(FaviconLoader.this){
                    if(closed || generation!=currentGeneration)return;
                }
                Bitmap icon=fetchIcon(origin);
                ArrayList<WeakReference<ImageView>> waiters;
                synchronized(FaviconLoader.this){
                    if(closed || generation!=currentGeneration)return;
                    waiters=listeners.remove(origin);
                    if(icon!=null){cache.put(origin,icon);}
                    else {
                        negative.put(origin,SystemClock.uptimeMillis());
                        while(negative.size()>FaviconWorkPolicy.MAX_FAILED_ORIGINS)
                            negative.remove(negative.keySet().iterator().next());
                    }
                }
                if(icon==null || waiters==null)return;
                final Bitmap result=icon;
                for(WeakReference<ImageView> ref:waiters){
                    ImageView target=ref.get();
                    if(target==null)continue;
                    target.post(()->{
                        synchronized(FaviconLoader.this){if(closed || generation!=currentGeneration)return;}
                        // Uma view reutilizada não pode receber o ícone do site anterior.
                        if(origin.equals(target.getTag()))target.setImageBitmap(result);
                    });
                }
            });
        }catch(RejectedExecutionException ignored){
            synchronized(this){if(generation==currentGeneration)listeners.remove(origin);}
        }
    }

    private static String safeOrigin(String address){
        try{
            Uri url=Uri.parse(address);
            if(!"https".equalsIgnoreCase(url.getScheme()) || url.getUserInfo()!=null)return null;
            String host=url.getHost();
            if(host==null || !host.matches("(?i)[a-z0-9-]{1,63}(\\.[a-z0-9-]{1,63})+"))return null;
            if(host.matches("(?i).*\\.(local|localhost|internal|test|invalid)$") || host.matches("[0-9.]+"))return null;
            return "https://"+host.toLowerCase(Locale.ROOT);
        }catch(RuntimeException ignored){return null;}
    }

    private static Bitmap fetchIcon(String origin){
        for(String path:new String[]{"/favicon.png","/favicon.ico"}){
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL(origin+path).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(2200);connection.setReadTimeout(2200);
                connection.setRequestProperty("Accept","image/png,image/x-icon,image/*;q=0.6");
                if(connection.getResponseCode()!=200 || connection.getContentLengthLong()>FaviconWorkPolicy.MAX_RESPONSE_BYTES)continue;
                byte[] data;
                try(InputStream stream=connection.getInputStream()){
                    ByteArrayOutputStream output=new ByteArrayOutputStream();
                    byte[] buffer=new byte[4096];int count;
                    while((count=stream.read(buffer))!=-1){
                        if(output.size()+count>FaviconWorkPolicy.MAX_RESPONSE_BYTES)break;
                        output.write(buffer,0,count);
                    }
                    if(count!=-1 || output.size()==0)continue;
                    data=output.toByteArray();
                }
                // Alguns ICO são contêineres para PNG. Não decodificar entradas arbitrárias.
                if(data.length>22 && data[0]==0 && data[1]==0 && data[2]==1 && data[3]==0){
                    int size=(data[8]&255)|((data[9]&255)<<8)|((data[10]&255)<<16)|((data[11]&255)<<24);
                    int offset=(data[12]&255)|((data[13]&255)<<8)|((data[14]&255)<<16)|((data[15]&255)<<24);
                    if(size>0 && offset>=16 && size<=data.length-offset && data[offset]==(byte)137)
                        data=java.util.Arrays.copyOfRange(data,offset,offset+size);
                }
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                BitmapFactory.decodeByteArray(data,0,data.length,options);
                if(options.outWidth<=0 || options.outHeight<=0 || options.outWidth>1024 || options.outHeight>1024)continue;
                options.inJustDecodeBounds=false;
                options.inSampleSize=Math.max(1,Math.max(options.outWidth,options.outHeight)/64);
                Bitmap icon=BitmapFactory.decodeByteArray(data,0,data.length,options);
                if(icon!=null)return icon;
            }catch(Exception | LinkageError ignored){}
            finally{if(connection!=null)connection.disconnect();}
        }
        return null;
    }

    synchronized void onTrimMemory(int level){
        if(!FaviconWorkPolicy.shouldTrimCache(level)||closed)return;
        generation++;
        worker.getQueue().clear();
        listeners.clear();
        cache.evictAll();
        // Não reciclar bitmaps: podem continuar visíveis em ImageViews.
    }
    synchronized void shutdown(){
        if(closed)return;
        closed=true;generation++;
        worker.shutdownNow();listeners.clear();negative.clear();cache.evictAll();
    }
}
