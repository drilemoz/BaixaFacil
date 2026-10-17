package br.com.baixafacil;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

/** Snapshot sob demanda. PSS corresponde apenas ao processo do app, NÃO ao renderer WebView separado. */
final class BrowserMemoryReport {
    private BrowserMemoryReport() {}

    static final class Snapshot {
        final long availableBytes, lowThresholdBytes, totalBytes, javaHeapUsedBytes;
        final long appPssBytes;
        final boolean systemLowMemory;
        Snapshot(long available, long threshold, long total, long heap, long pss, boolean low) {
            availableBytes=available; lowThresholdBytes=threshold; totalBytes=total;
            javaHeapUsedBytes=heap; appPssBytes=pss; systemLowMemory=low;
        }
        String summary() {
            return "RAM disponível no sistema: " + megabytes(availableBytes) + " MB\n"
                + "Limite de memória baixa do Android: " + megabytes(lowThresholdBytes) + " MB\n"
                + "RAM total informada: " + megabytes(totalBytes) + " MB\n"
                + "Memória Java utilizada no app: " + megabytes(javaHeapUsedBytes) + " MB\n"
                + "PSS estimado do processo Baixa Fácil: " + megabytes(appPssBytes) + " MB\n"
                + "Memória baixa sinalizada pelo Android: " + (systemLowMemory?"sim":"não") + "\n"
                + "O PSS informado NÃO inclui necessariamente o processo separado da WebView ou uso de GPU.";
        }
    }

    static Snapshot capture(Context context) {
        ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();
        if(manager!=null)manager.getMemoryInfo(memory);
        Runtime runtime=Runtime.getRuntime();
        long used=Math.max(0,runtime.totalMemory()-runtime.freeMemory());
        long pss=-1;
        try{pss=Debug.getPss()*1024L;}catch(RuntimeException ignored){}
        return new Snapshot(manager==null?-1:memory.availMem,
            manager==null?-1:memory.threshold,manager==null?-1:memory.totalMem,
            used,pss,manager!=null&&memory.lowMemory);
    }

    private static String megabytes(long value){return value<0?"desconhecido":Long.toString(value/(1024L*1024L));}
}
