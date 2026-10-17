package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class FavoriteRulesTest {
    @Test public void acceptsSecureAndExplicitHttpSites(){
        assertEquals("https://example.com/arquivo.zip",FavoriteRules.url(" example.com/arquivo.zip "));
        assertEquals("https://example.com/",FavoriteRules.url("https://example.com/"));
        assertEquals("http://example.com",FavoriteRules.url("http://example.com"));
    }
    @Test public void refusesUnsafeSchemesOrUserInfo(){
        assertNull(FavoriteRules.url("javascript:alert(1)"));
        assertNull(FavoriteRules.url("file:///sdcard/segredo"));
        assertNull(FavoriteRules.url("content://provider/file"));
        assertNull(FavoriteRules.url("blob:https://example.com/blob"));
        assertNull(FavoriteRules.url("sandbox:/mnt/data/arquivo.zip"));
        assertNull(FavoriteRules.url("https://user:password@example.com/private"));
        assertNull(FavoriteRules.url("https://example.com/\nhttps://evil.com"));
    }
    @Test public void enforcesBounds(){
        assertNull(FavoriteRules.url("https://example.com/"+"x".repeat(2048)));
        assertEquals(80,FavoriteRules.title("X".repeat(100),"https://example.com").length());
        assertEquals(60,FavoriteRules.MAX_FAVORITES);
    }
    @Test public void fallbackNamesAreSafe(){
        assertEquals("example.com",FavoriteRules.title("  ","https://example.com/test"));
        assertEquals("Meu site favorito",FavoriteRules.title(" Meu\nsite\t favorito ","https://example.com"));
        assertEquals("exemplo",FavoriteRules.title("exemplo","https://example.com"));
    }
}
