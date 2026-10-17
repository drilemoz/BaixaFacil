package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public final class DesktopUserAgentTest {
    @Test public void changesAndroidToDesktopPreservingChromeVersion() {
        String current = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UP) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/131.0.6778.2 Mobile Safari/537.36";
        String desktop = DesktopUserAgent.toDesktop(current);
        assertTrue(desktop.contains("(X11; Linux x86_64)"));
        assertTrue(desktop.contains("Chrome/131.0.6778.2"));
        assertFalse(desktop.contains("Android 14"));
        assertFalse(desktop.contains(" Mobile "));
    }
    @Test public void handlesMissingAndroidOrEmpty() {
        assertNull(DesktopUserAgent.toDesktop(null));
        assertEquals("",DesktopUserAgent.toDesktop(""));
        assertEquals("Mozilla/5.0 (X11; Linux x86_64) Safari/537.36",DesktopUserAgent.toDesktop("Mozilla/5.0 (X11; Linux x86_64) Mobile Safari/537.36"));
    }
}
