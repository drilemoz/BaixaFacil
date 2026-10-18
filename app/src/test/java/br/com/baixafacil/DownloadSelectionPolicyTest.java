package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadSelectionPolicyTest {
    @Test public void completedMediaStoreFilesCanBeSelected() {
        assertTrue(DownloadSelectionPolicy.selectable("Concluído", "content://media/external/downloads/125"));
    }
    @Test public void incompleteStatusesAreProtected() {
        for(String s:new String[]{"Aguardando", "Na fila", "Baixando", "Conectando", "Pausado", "Interrompido", "Falhou", "Cancelado", "Retomando"})
            assertFalse(s, DownloadSelectionPolicy.selectable(s,"content://media/external/downloads/125"));
    }
    @Test public void unknownOrLocalUrisAreExcluded() {
        for(String uri:new String[]{null,"", "file:///sdcard/Download/test.mp4", "content://other.authority/12", "https://example.org/file.mp4", "content://media.evil.example/0"})
            assertFalse(String.valueOf(uri),DownloadSelectionPolicy.selectable("Concluído",uri));
    }
    @Test public void sharingIsBounded() {
        assertEquals(25,DownloadSelectionPolicy.MAX_SHARE_ITEMS);
    }
}
