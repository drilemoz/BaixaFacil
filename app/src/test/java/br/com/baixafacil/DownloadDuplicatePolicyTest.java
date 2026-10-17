package br.com.baixafacil;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

public class DownloadDuplicatePolicyTest {
    private DownloadDuplicatePolicy.Record row(String url,String status,boolean file){
        return new DownloadDuplicatePolicy.Record(url,status,file);
    }
    @Test public void detectsQueueOfExactUrl(){
        assertEquals(DownloadDuplicatePolicy.Kind.IN_PROGRESS,DownloadDuplicatePolicy.find("https://host/file.gguf?sig=a",Arrays.asList(row("https://host/file.gguf?sig=a","Na fila",false))));
    }
    @Test public void prefersActiveOverCompleted(){
        assertEquals(DownloadDuplicatePolicy.Kind.IN_PROGRESS,DownloadDuplicatePolicy.find("https://host/file.gguf",Arrays.asList(row("https://host/file.gguf","Concluído",true),row("https://host/file.gguf","Baixando",false))));
    }
    @Test public void detectsCompletedWithSavedFile(){
        assertEquals(DownloadDuplicatePolicy.Kind.COMPLETED,DownloadDuplicatePolicy.find("https://host/file.gguf",Arrays.asList(row("https://host/file.gguf","Concluído",true))));
    }
    @Test public void ignoresFailedAndCancelled(){
        assertEquals(DownloadDuplicatePolicy.Kind.NONE,DownloadDuplicatePolicy.find("https://host/file.gguf",Arrays.asList(row("https://host/file.gguf","Falhou",false),row("https://host/file.gguf","Cancelado",false))));
    }
    @Test public void pausedOrInterruptedNeedManualResume(){
        assertEquals(DownloadDuplicatePolicy.Kind.IN_PROGRESS,DownloadDuplicatePolicy.find("https://host/file.gguf",Arrays.asList(row("https://host/file.gguf","Pausado",false))));
        assertEquals(DownloadDuplicatePolicy.Kind.IN_PROGRESS,DownloadDuplicatePolicy.find("https://host/file.gguf",Arrays.asList(row("https://host/file.gguf","Interrompido",false))));
    }
    @Test public void ignoresMissingCompletedFile(){
        assertEquals(DownloadDuplicatePolicy.Kind.NONE,DownloadDuplicatePolicy.find("https://host/file.gguf",Arrays.asList(row("https://host/file.gguf","Concluído",false))));
    }
    @Test public void retainsSignedUrlIdentity(){
        assertEquals(DownloadDuplicatePolicy.Kind.NONE,DownloadDuplicatePolicy.find("https://host/file.gguf?sig=b",Arrays.asList(row("https://host/file.gguf?sig=a","Concluído",true))));
    }
    @Test public void retainsDifferentHosts(){
        assertEquals(DownloadDuplicatePolicy.Kind.NONE,DownloadDuplicatePolicy.find("https://other/file.gguf",Arrays.asList(row("https://host/file.gguf","Concluído",true))));
    }
    @Test public void noRecordsOrUrl(){
        assertEquals(DownloadDuplicatePolicy.Kind.NONE,DownloadDuplicatePolicy.find("",Collections.emptyList()));
        assertEquals(DownloadDuplicatePolicy.Kind.NONE,DownloadDuplicatePolicy.find("https://host/file.gguf",null));
    }
}
