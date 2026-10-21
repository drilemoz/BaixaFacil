package br.com.baixafacil;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/** Miniaturas locais de 96 x 70: 1 thread, cache LRU 4 MiB, fila e callbacks limitados. */
final class MediaPreviewLoader {
    static final class Result {
        final Bitmap bitmap;
        final String details;
        Result(Bitmap bitmap,String details){this.bitmap=bitmap;this.details=details;}
    }
    private static final Result EMPTY = new Result(null,"");
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{
        Thread t=new Thread(r,"baixa-facil-preview");t.setPriority(Thread.MIN_PRIORITY);return t;
    });
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LruCache<String,Result> cache=new LruCache<String,Result>(4*1024){
        @Override protected int sizeOf(String key,Result item){
            return item.bitmap==null?1:Math.max(1,item.bitmap.getByteCount()/1024);
        }
    };
    // Duplicatas da mesma URI compartilham o mesmo trabalho. Limitar o mapa também
    // limita a fila interna do executor e as referências transitórias às views.
    private final HashMap<String,ArrayList<Consumer<Result>>> pending=new HashMap<>();
    private volatile boolean closed;
    private long cacheGeneration;

    void load(Context context,Uri uri,String filename,Consumer<Result> receive){
        if(receive==null || uri==null || !"content".equalsIgnoreCase(uri.getScheme()) || uri.getAuthority()==null)return;
        String kind=DownloadInfo.kind(filename);
        if(!"Vídeos".equals(kind) && !"Imagens".equals(kind))return;
        String key=uri.toString();
        if(key.length()>2048)return;
        Result cached;
        long generation;
        boolean start=false, busy=false;
        synchronized(this){
            if(closed)return;
            cached=cache.get(key);
            if(cached==null){
                ArrayList<Consumer<Result>> listeners=pending.get(key);
                if(listeners!=null){
                    if(listeners.size()<MediaWorkPolicy.MAX_LISTENERS_PER_PREVIEW)listeners.add(receive);
                    else busy=true;
                }else if(pending.size()>=MediaWorkPolicy.MAX_PENDING_PREVIEWS){
                    busy=true;
                }else{
                    listeners=new ArrayList<>();listeners.add(receive);
                    pending.put(key,listeners);
                    start=true;
                }
            }
            generation=cacheGeneration;
        }
        if(cached!=null){receive.accept(cached);return;}
        if(busy){receive.accept(EMPTY);return;}
        if(!start)return;
        Context app=context.getApplicationContext();
        try{
            worker.execute(()->{
                Result extracted=EMPTY;
                try{
                    if(!Thread.currentThread().isInterrupted())extracted=extract(app,uri,"Vídeos".equals(kind));
                }catch(RuntimeException | LinkageError ignored){/* arquivo inválido ou recurso não disponível */}
                final Result completed=extracted;
                final ArrayList<Consumer<Result>> callbacks;
                final Result delivery;
                synchronized(MediaPreviewLoader.this){
                    callbacks=pending.remove(key);
                    if(closed || cacheGeneration!=generation){
                        // Não repovoar o cache após low-memory ou destruição.
                        delivery=EMPTY;
                    }else{
                        cache.put(key,completed);
                        delivery=completed;
                    }
                }
                if(callbacks!=null && !closed){
                    main.post(()->{
                        if(closed)return;
                        for(Consumer<Result> callback:callbacks)callback.accept(delivery);
                    });
                }
            });
        }catch(RejectedExecutionException ignored){
            synchronized(this){pending.remove(key);}
            if(!closed)receive.accept(EMPTY);
        }
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
            }catch(Exception | LinkageError ignored){/* formato/codec pode não ter frame */}
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

    /** Evita cache acumulado quando o Android sinaliza pressão de memória. */
    synchronized void onTrimMemory(int level){
        if(!MediaWorkPolicy.shouldTrimPreviewCache(level)||closed)return;
        cacheGeneration++;
        cache.evictAll();
        // Não reciclar bitmaps aqui: ImageViews ainda podem estar usando a imagem.
    }
    synchronized void shutdown(){
        closed=true;
        cacheGeneration++;
        worker.shutdownNow();
        pending.clear();
        cache.evictAll();
        main.removeCallbacksAndMessages(null);
    }
}
