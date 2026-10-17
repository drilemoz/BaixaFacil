package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginWindowPolicyTest {
    @Test public void onlyExplicitAndSingleWindow() {
        assertTrue(LoginWindowPolicy.allowPopup(true,false));
        assertFalse(LoginWindowPolicy.allowPopup(false,false));
        assertFalse(LoginWindowPolicy.allowPopup(true,true));
    }
    @Test public void rejectsDangerousSchemesAndEmbeddedCredentials() {
        assertTrue(LoginWindowPolicy.safeWebAddress("https://www.facebook.com/checkpoint/?v=abc"));
        assertTrue(LoginWindowPolicy.safeWebAddress("http://example.org/"));
        assertFalse(LoginWindowPolicy.safeWebAddress("javascript:alert(1)"));
        assertFalse(LoginWindowPolicy.safeWebAddress("blob:https://site.com/id"));
        assertFalse(LoginWindowPolicy.safeWebAddress("file:///sdcard/test"));
        assertFalse(LoginWindowPolicy.safeWebAddress("https://user:pass@example.org/"));
        assertFalse(LoginWindowPolicy.safeWebAddress("https://"));
    }
    @Test public void sensitiveTokensNotHistory() {
        assertTrue(LoginWindowPolicy.hasLoginParameters("https://example.org/callback?code=private"));
        assertTrue(LoginWindowPolicy.hasLoginParameters("https://example.org/?id_token=private"));
        assertFalse(LoginWindowPolicy.hasLoginParameters("https://example.org/?page=2"));
        assertTrue(LoginWindowPolicy.isSafeInternalFrame("about:blank"));
        assertFalse(LoginWindowPolicy.isSafeInternalFrame("file:///test"));
    }
    @Test public void reportsOnlyOrigin() {
        assertEquals("https://example.org", LoginWindowPolicy.originForReport("https://example.org/private?code=SECRET#t"));
        assertEquals("não disponível",LoginWindowPolicy.originForReport("https://user:pass@example.org/login"));
    }
}
