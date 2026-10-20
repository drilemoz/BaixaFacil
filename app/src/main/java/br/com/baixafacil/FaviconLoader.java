package br.com.baixafacil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Favicons somente na própria origem HTTPS, sem cookies, rastreadores ou serviços de terceiros. */
final class FaviconLoader {
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(2*1024*1024){
        @Override protected int sizeOf(String k,Bitmap b){return b.getByteCount();}
    };
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile boolean closed;
    private final java.util.HashMap<String,java.util.ArrayList<java.lang.ref.WeakReference<ImageView>>> listeners=new java.util.HashMap<>();
    private final java.util.HashMap<String,Long> negative=new java.util.HashMap<>();
    void load(Context context,String url,ImageView view){
        String host;
        try{
            Uri parsed=Uri.parse(url);
            if(!"https".equalsIgnoreCase(parsed.getScheme()))return;
            host=parsed.getHost();
            if(host==null || !host.matches("(?i)[a-z0-9-]{1,63}(\\.[a-z0-9-]{1,63})+") || host.matches(".*\\.(local|localhost|internal|test|invalid)$"))return;
        }catch(Exception ignored){return;}
        String origin="https://"+host.toLowerCase(java.util.Locale.ROOT);
        view.setTag(origin);
        Bitmap bmp=cache.get(origin);
        if(bmp!=null){view.setImageBitmap(bmp);return;}
        synchronized(listeners){
            Long failedAt=negative.get(origin);
            if(failedAt!=null && android.os.SystemClock.uptimeMillis()-failedAt<60000)return;
            java.util.ArrayList<java.lang.ref.WeakReference<ImageView>> targets=listeners.get(origin);
            if(targets!=null){targets.add(new java.lang.ref.WeakReference<>(view));return;}
            targets=new java.util.ArrayList<>();targets.add(new java.lang.ref.WeakReference<>(view));listeners.put(origin,targets);
        }
        if(closed)return;
        try{worker.execute(()->{
            if(closed)return;
            Bitmap icon=null;
            for(String path:new String[]{"/favicon.png","/favicon.ico"}){
                HttpURLConnection connection=null;
                try{
                    connection=(HttpURLConnection)new URL(origin+path).openConnection();
                    connection.setInstanceFollowRedirects(false);
                    connection.setConnectTimeout(2200);connection.setReadTimeout(2200);
                    connection.setRequestProperty("Accept","image/png,image/x-icon,image/*;q=0.6");
                    if(connection.getResponseCode()!=200||connection.getContentLengthLong()>65536)continue;
                    try(InputStream stream=connection.getInputStream()){
                        java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
                        byte[] buffer=new byte[4096];int count;
                        while((count=stream.read(buffer))!=-1 && output.size()<=65536)output.write(buffer,0,count);
                        byte[] data=output.toByteArray();
                        if(data.length>65536)continue;
                        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;
                        if(data.length>22 && data[0]==0 && data[1]==0 && data[2]==1 && data[3]==0){
                            int size=(data[8]&255)|((data[9]&255)<<8)|((data[10]&255)<<16)|((data[11]&255)<<24);
                            int offset=(data[12]&255)|((data[13]&255)<<8)|((data[14]&255)<<16)|((data[15]&255)<<24);
                            if(size>0 && offset>=16 && size<=data.length-offset && data[offset]==(byte)137){
                                data=java.util.Arrays.copyOfRange(data,offset,offset+size);
                            }
                        }
                        BitmapFactory.decodeByteArray(data,0,data.length,opts);
                        if(opts.outWidth<=0||opts.outHeight<=0||opts.outWidth>1024||opts.outHeight>1024)continue;
                        opts.inJustDecodeBounds=false;opts.inSampleSize=Math.max(1,Math.max(opts.outWidth,opts.outHeight)/64);
                        icon=BitmapFactory.decodeByteArray(data,0,data.length,opts);
                        if(icon!=null)break;
                    }
                }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
            }
            java.util.ArrayList<java.lang.ref.WeakReference<ImageView>> targets;
            synchronized(listeners){
                targets=listeners.remove(origin);
                if(icon==null)negative.put(origin,android.os.SystemClock.uptimeMillis());
            }
            if(icon!=null&&!closed){
                Bitmap found=icon;cache.put(origin,found);
                if(targets!=null)for(java.lang.ref.WeakReference<ImageView> weak:targets){
                    ImageView target=weak.get();if(target==null)continue;
                    target.post(()->{if(origin.equals(target.getTag()))target.setImageBitmap(found);});
                }
            }
        });}catch(java.util.concurrent.RejectedExecutionException ignored){}
    }
    void shutdown(){closed=true;worker.shutdownNow();
        synchronized(listeners){listeners.clear();negative.clear();}cache.evictAll();}
}
