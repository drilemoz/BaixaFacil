package br.com.baixafacil;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class DownloadMediaFilterTest {
    @Test public void filterMediaButNotDocumentsOrApk(){
        assertTrue(DownloadMediaFilter.matches("Mídias","filme.mp4"));
        assertTrue(DownloadMediaFilter.matches("Mídias","foto.avif"));
        assertTrue(DownloadMediaFilter.matches("Mídias","foto.jpg"));
        assertTrue(DownloadMediaFilter.matches("Mídias","audio.mp3"));
        assertFalse(DownloadMediaFilter.matches("Mídias","package.apk"));
        assertFalse(DownloadMediaFilter.matches("Mídias","documento.pdf"));
        assertTrue(DownloadMediaFilter.matches("Todos","documento.pdf"));
    }
    @Test public void filterIsSpecificAndDoesNotMistakeQuery(){
        assertTrue(DownloadMediaFilter.matches("Vídeos","VIDEO.MP4?token=abc"));
        assertFalse(DownloadMediaFilter.matches("Imagens","VIDEO.MP4?token=abc"));
        assertFalse(DownloadMediaFilter.matches("Mídias","desconhecido.bin"));
        assertTrue(DownloadMediaFilter.matches("Outros","desconhecido.bin"));
    }
    @Test public void countsAreLocalRecordsNotDiskScan(){
        DownloadMediaFilter.Counts counts=DownloadMediaFilter.count(Arrays.asList(
                "video.mp4","foto.jpg","som.ogg","manual.pdf","outro.apk","video.MP4"));
        assertEquals(2,counts.videos);assertEquals(1,counts.images);
        assertEquals(1,counts.audio);assertEquals(4,counts.media);
        assertEquals(0,DownloadMediaFilter.count(null).media);
    }
}
