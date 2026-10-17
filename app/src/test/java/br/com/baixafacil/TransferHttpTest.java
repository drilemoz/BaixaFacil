package br.com.baixafacil;

import static org.junit.Assert.*;
import org.junit.Test;

public class TransferHttpTest {
    @Test public void sameOriginAllowsExactProtocolHostAndPort() {
        assertTrue(TransferHttp.sameOrigin("https://example.com/file", "https://EXAMPLE.com:443/page"));
        assertTrue(TransferHttp.sameOrigin("http://example.com:80/file", "http://example.com/page"));
        assertFalse(TransferHttp.sameOrigin("https://example.com/file", "https://evil.example.com/page"));
        assertFalse(TransferHttp.sameOrigin("https://example.com/file", "http://example.com/page"));
        assertFalse(TransferHttp.sameOrigin("https://example.com:8443/file", "https://example.com/page"));
        assertFalse(TransferHttp.sameOrigin("https://example.com/file", "blob:https://example.com/id"));
    }

    @Test public void rejectsDowngradeAndBadSchemes() {
        assertTrue(TransferHttp.isDowngrade("https://example.com/x", "http://example.com/x"));
        assertFalse(TransferHttp.isDowngrade("http://example.com/x", "https://example.com/y"));
        assertTrue(TransferHttp.isDowngrade("https://example.com/x", "file:///tmp/local"));
        assertFalse(TransferHttp.isWebUrl("https://user:pass@example.com/x"));
        assertFalse(TransferHttp.isWebUrl("javascript:alert(1)"));
        assertTrue(TransferHttp.isWebUrl("https://example.com/file.zip?q=1"));
    }

    @Test public void validatesSensitiveHeaders() {
        assertEquals("session=abc; foo=bar", TransferHttp.safeHeader("session=abc; foo=bar", 8192));
        assertNull(TransferHttp.safeHeader("x\r\nInjected: yes", 8192));
        assertNull(TransferHttp.safeHeader("x\n", 8192));
        assertNull(TransferHttp.safeHeader("aaaa", 3));
        assertNull(TransferHttp.safeHeader("", 1024));
    }

    @Test public void handlesRedirectStatusesOnly() {
        for (int status : new int[]{301, 302, 303, 307, 308}) assertTrue(TransferHttp.isRedirect(status));
        for (int status : new int[]{200, 204, 206, 300, 304, 401, 503}) assertFalse(TransferHttp.isRedirect(status));
    }
}
