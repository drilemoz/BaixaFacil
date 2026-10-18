package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadCleanupPolicyTest {
    @Test public void completedAndCancelledAreCleanable() {
        assertTrue(DownloadCleanupPolicy.eligible("Concluído",true));
        assertTrue(DownloadCleanupPolicy.eligible("Concluído",false));
        assertTrue(DownloadCleanupPolicy.eligible("Cancelado",false));
    }
    @Test public void activeAndRecoverableAreProtected() {
        for(String s:new String[]{"Na fila","Baixando","Conectando","Retomando","Pausado","Interrompido","Nova tentativa","Aguardando"}) {
            assertFalse(s,DownloadCleanupPolicy.eligible(s,false));
        }
    }
    @Test public void failedWithPartialIsProtected() {
        assertFalse(DownloadCleanupPolicy.eligible("Falhou",true));
        assertTrue(DownloadCleanupPolicy.eligible("Falhou",false));
    }
    @Test public void deleteRequiresCompletedMediaStoreContentUri() {
        assertTrue(DownloadCleanupPolicy.deleteFile("Concluído","content://media/external/downloads/100"));
        assertFalse(DownloadCleanupPolicy.deleteFile("Pausado","content://media/external/downloads/100"));
        assertFalse(DownloadCleanupPolicy.deleteFile("Concluído","file:///sdcard/Download/a.mp4"));
        assertFalse(DownloadCleanupPolicy.deleteFile("Concluído","content://example.provider/file/10"));
        assertFalse(DownloadCleanupPolicy.deleteFile("Concluído","content://media.evil.example/10"));
    }
}
