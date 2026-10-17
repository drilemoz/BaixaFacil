package br.com.baixafacil;

import static org.junit.Assert.*;
import org.junit.Test;

public class DownloadUtilitiesTest {
    @Test public void extractsHttpsSharedLink() {
        assertEquals("https://example.com/file.zip?token=123", UrlTools.extract("Aqui: https://example.com/file.zip?token=123."));
    }
    @Test public void rejectsInternalChatLink() {
        assertNull(UrlTools.extract("sandbox:/mnt/data/arquivo.zip"));
    }
    @Test public void rejectsFileAndJavascriptSchemes() {
        assertNull(UrlTools.extract("javascript:alert(1)"));
        assertNull(UrlTools.extract("file:///sdcard/Download/a.zip"));
    }
    @Test public void sanitizesUnsafeHeaderFilename() {
        assertEquals("ok.zip", FileNames.choose("attachment; filename=../../ok.zip", "https://example.com/", "application/zip"));
    }
    @Test public void supportsUtf8FileNames() {
        assertEquals("relatório.pdf", FileNames.choose("attachment; filename*=UTF-8''relat%C3%B3rio.pdf", "https://example.com", "application/pdf"));
    }
    @Test public void urlFilename() {
        assertEquals("programa.apk", FileNames.choose(null, "https://example.com/get/programa.apk?x=1", "application/octet-stream"));
    }
}
