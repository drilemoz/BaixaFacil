package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadInfoTest {
    @Test public void identifiesFileKinds() {
        assertEquals("Aplicativos",DownloadInfo.kind("app.APK"));
        assertEquals("Vídeos",DownloadInfo.kind("filme.mp4"));
        assertEquals("Áudios",DownloadInfo.kind("audio.flac"));
        assertEquals("Imagens",DownloadInfo.kind("foto.webp"));
        assertEquals("Documentos",DownloadInfo.kind("relatorio.pdf"));
        assertEquals("Compactados",DownloadInfo.kind("dados.tar.gz"));
        assertEquals("Outros",DownloadInfo.kind("arquivo"));
    }
    @Test public void estimatesOnlyWhenValid() {
        assertEquals(20,DownloadInfo.etaSeconds(3000,1000,100));
        assertEquals(-1,DownloadInfo.etaSeconds(-1,1000,100));
        assertEquals(-1,DownloadInfo.etaSeconds(1000,500,0));
        assertEquals(-1,DownloadInfo.etaSeconds(1000,1000,100));
        assertEquals("~2 min",DownloadInfo.etaLabel(115));
    }
}
