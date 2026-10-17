package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class BrowserLoadPolicyTest {
    @Test public void recognizesOnlyTrustedChatGPTOrigins() {
        assertTrue(BrowserLoadPolicy.isChatGpt("https://chatgpt.com/"));
        assertTrue(BrowserLoadPolicy.isChatGpt("https://www.chatgpt.com/c/test"));
        assertTrue(BrowserLoadPolicy.isChatGpt("https://chat.openai.com/?model=some"));
        assertTrue(BrowserLoadPolicy.isChatGpt("HTTPS://CHATGPT.COM/c/123"));
        assertTrue(BrowserLoadPolicy.isChatGpt("http://chatgpt.com/"));
    }
    @Test public void rejectsImpostorDomainsAndProtocols() {
        assertFalse(BrowserLoadPolicy.isChatGpt("https://chatgpt.com.evil.net/"));
        assertFalse(BrowserLoadPolicy.isChatGpt("https://notchatgpt.com/"));
        assertFalse(BrowserLoadPolicy.isChatGpt("https://chatgpt.com@bad.example/"));
        assertFalse(BrowserLoadPolicy.isChatGpt("https://evil.com/?next=https://chatgpt.com"));
        assertFalse(BrowserLoadPolicy.isChatGpt("javascript:chatgpt.com"));
        assertFalse(BrowserLoadPolicy.isChatGpt("file:///chatgpt.com"));
        assertFalse(BrowserLoadPolicy.isChatGpt("https://api.openai.com"));
        assertFalse(BrowserLoadPolicy.isChatGpt("https://accounts.google.com/"));
        assertFalse(BrowserLoadPolicy.isChatGpt("https://chatgpt.com\nexample.org"));
        assertFalse(BrowserLoadPolicy.isChatGpt(null));
    }
    @Test public void userMayDisableOptimizationAndOtherSitesNotChanged() {
        assertTrue(BrowserLoadPolicy.lightMode("https://chatgpt.com/c/123",true));
        assertFalse(BrowserLoadPolicy.lightMode("https://chatgpt.com/c/123",false));
        assertFalse(BrowserLoadPolicy.lightMode("https://www.facebook.com",true));
        assertFalse(BrowserLoadPolicy.lightMode("https://www.google.com",true));
        assertFalse(BrowserLoadPolicy.autoScanAllowed("https://chatgpt.com/",true));
        assertTrue(BrowserLoadPolicy.autoScanAllowed("https://chatgpt.com/",false));
        assertTrue(BrowserLoadPolicy.autoScanAllowed("https://example.org/",true));
    }
}
