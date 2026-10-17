package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class UrlToolsAddressTest {
    @Test public void expandsDomainToHttps() {
        assertEquals("https://google.com", UrlTools.normalizeAddress("google.com"));
        assertEquals("https://www.google.com", UrlTools.normalizeAddress(" www.google.com "));
        assertEquals("https://servidor.com/arquivo.zip", UrlTools.normalizeAddress("servidor.com/arquivo.zip"));
        assertEquals("https://192.168.1.10:8080/arquivo", UrlTools.normalizeAddress("192.168.1.10:8080/arquivo"));
    }
    @Test public void preservesExplicitScheme() {
        assertEquals("http://example.com/a", UrlTools.normalizeAddress("http://example.com/a"));
        assertEquals("https://example.com/a?token=1", UrlTools.normalizeAddress("https://example.com/a?token=1"));
    }
    @Test public void rejectsUnsafeOrUnusableUrls() {
        assertNull(UrlTools.normalizeAddress("javascript:alert(1)"));
        assertNull(UrlTools.normalizeAddress("sandbox:/mnt/data/private.zip"));
        assertNull(UrlTools.normalizeAddress("file:///sdcard/foo.zip"));
        assertNull(UrlTools.normalizeAddress("https://user:secret@example.com"));
        assertNull(UrlTools.normalizeAddress("example.com@evil.com"));
        assertNull(UrlTools.normalizeAddress("um texto solto"));
    }
    @Test public void browserSearchIsDifferentFromDownloadUrl() {
        assertEquals("https://www.google.com/search?q=como+baixar+zip",
                UrlTools.browserAddress("como baixar zip"));
        assertNull(UrlTools.browserAddress("javascript:alert(1)"));
        assertNull(UrlTools.browserAddress("sandbox:/mnt/data/file.apk"));
        assertEquals("https://example.org",UrlTools.browserAddress("example.org"));
    }
}
