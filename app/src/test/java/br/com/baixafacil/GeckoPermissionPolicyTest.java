package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class GeckoPermissionPolicyTest {
    @Test public void sameOriginOnly(){
        assertTrue(GeckoPermissionPolicy.sameOrigin("https://EXAMPLE.org:443/geolocate?state=SECRET",
            "https://example.org/new-page"));
        assertTrue(GeckoPermissionPolicy.sameOrigin("https://example.org:8443/a",
            "https://example.org:8443/b"));
        assertFalse(GeckoPermissionPolicy.sameOrigin("https://example.org/","https://other.example.org/"));
        assertFalse(GeckoPermissionPolicy.sameOrigin("https://example.org/","http://example.org/"));
        assertFalse(GeckoPermissionPolicy.sameOrigin("https://example.org:8443/","https://example.org/"));
    }
    @Test public void blocksUnsafeOrMalformedOrigins(){
        assertNull(GeckoPermissionPolicy.origin("http://example.org"));
        assertNull(GeckoPermissionPolicy.origin("https://user:secret@example.org/private"));
        assertNull(GeckoPermissionPolicy.origin("https://example.org:0"));
        assertNull(GeckoPermissionPolicy.origin("https://example.org:99999"));
        assertNull(GeckoPermissionPolicy.origin("file:///tmp/secret"));
        assertNull(GeckoPermissionPolicy.origin("about:blank"));
        assertNull(GeckoPermissionPolicy.origin(null));
        assertFalse(GeckoPermissionPolicy.sameOrigin("https://a.example","https://b.example"));
    }
}
