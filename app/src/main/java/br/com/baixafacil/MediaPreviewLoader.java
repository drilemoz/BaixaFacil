package br.com.baixafacil;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.Size;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Extrai miniaturas sem rede, em uma única thread e cache de até 4 MB. */
final class MediaPreviewLoader {
    static final class Result {
        final Bitmap bitmap;
        final String details;
        Result(Bitmap bitmap,String details){this.bitmap=bitmap;this.details=details;}
    }
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{
        Thread t=new Thread(r,"baixa-facil-preview");t.setPriority(Thread.MIN_PRIORITY);return t;
    });
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LruCache<String,Result> cache=new LruCache<String,Result>(4*1024){
        @Override protected int sizeOf(String key,Result item){
            return item.bitmap==null?1:Math.max(1,item.bitmap.getByteCount()/1024);
        }
    };
    private final HashSet<String> pending=new HashSet<>();
    private boolean closed;
    void load(Context context,Uri uri,String filename,Consumer<Result> receive){
        if(uri==null || !"content".equalsIgnoreCase(uri.getScheme()) || uri.getAuthority()==null)return;
        String kind=DownloadInfo.kind(filename);
        if(!"Vídeos".equals(kind) && !"Imagens".equals(kind))return;
        String key=uri.toString();
        if(key.length()>2048)return;
        Result existing;
        synchronized(this){
            if(closed)return;
            existing=cache.get(key);
            if(existing==null){if(!pending.add(key))return;}
        }
        if(existing!=null){receive.accept(existing);return;}
        Context app=context.getApplicationContext();
        worker.execute(()->{
            Result result=extract(app,uri,"Vídeos".equals(kind));
            synchronized(MediaPreviewLoader.this){
                pending.remove(key);
                if(closed){if(result.bitmap!=null)result.bitmap.recycle();return;}
                cache.put(key,result);
            }
            main.post(()->{if(!closed)receive.accept(result);});
        });
    }
    private static Result extract(Context context,Uri uri,boolean video){
        Bitmap bitmap=null;
        String details="";
        if(video){
            MediaMetadataRetriever retriever=new MediaMetadataRetriever();
            try{
                retriever.setDataSource(context,uri);
                int w=number(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                int h=number(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                long duration=longNumber(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                details=MediaPreviewFormat.description(w,h,duration);
                bitmap=retriever.getScaledFrameAtTime(-1,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,96,70);
            }catch(Exception | LinkageError ignored){ /* formato/codec pode não ter frame */ }
            finally{try{retriever.release();}catch(Exception ignored){}}
        }
        if(bitmap==null){
            try{bitmap=context.getContentResolver().loadThumbnail(uri,new Size(96,70),null);}
            catch(Exception | LinkageError ignored){}
        }
        if(!video && bitmap!=null)details=bitmap.getWidth()+"×"+bitmap.getHeight()+" (prévia)";
        return new Result(bitmap,details);
    }
    private static int number(String value){try{return Integer.parseInt(value);}catch(Exception e){return 0;}}
    private static long longNumber(String value){try{return Long.parseLong(value);}catch(Exception e){return -1;}}
    synchronized void shutdown(){
        closed=true;
        worker.shutdownNow();
        cache.evictAll();
        pending.clear();
    }
}
