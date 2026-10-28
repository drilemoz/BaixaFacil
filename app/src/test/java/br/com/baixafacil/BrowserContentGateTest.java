package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

/** Testa a mesma decisão de navegação aplicada à aba e ao pop-up do GeckoView. */
public class BrowserContentGateTest {
    @Test public void blocksKnownDomainsInBothModes(){
        assertEquals(BrowserContentGate.Action.BLOCK,
            BrowserContentGate.evaluate("https://sub.xvideos.com/", AdultContentPolicy.MODERATE).action);
        assertEquals(BrowserContentGate.Action.BLOCK,
            BrowserContentGate.evaluate("https://onlyfans.com", AdultContentPolicy.STRICT).action);
        assertEquals(BrowserContentGate.Action.ALLOW,
            BrowserContentGate.evaluate("https://onlyfans.com", AdultContentPolicy.MODERATE).action);
    }
    @Test public void normalizesUnicodeDotWithoutOverblocking(){
        assertEquals(BrowserContentGate.Action.BLOCK,
            BrowserContentGate.evaluate("https://pornhub。com/", AdultContentPolicy.MODERATE).action);
        assertEquals(BrowserContentGate.Action.ALLOW,
            BrowserContentGate.evaluate("https://pornhub.com.example.org/", AdultContentPolicy.STRICT).action);
    }
    @Test public void protectedSearchOnlyOnKnownRealHosts(){
        for(String host:new String[]{"google.bad", "google.evil", "ads.google.com", "search.bing.com", "abc.duckduckgo.com"}){
            String url="https://"+host+"/search?q=test&safe=off";
            assertEquals("should not rewrite " + host,url,AdultContentPolicy.safeSearchUrl(url, AdultContentPolicy.STRICT));
        }
        assertEquals("https://google.com.br/search?q=oi&safe=active",
            AdultContentPolicy.safeSearchUrl("https://google.com.br/search?q=oi&safe=off",AdultContentPolicy.MODERATE));
        assertEquals("https://www.google.co.uk/search?q=oi&safe=active",
            AdultContentPolicy.safeSearchUrl("https://www.google.co.uk/search?q=oi",AdultContentPolicy.STRICT));
    }
    @Test public void encodedDuplicateFlagsAreStripped(){
        String rewritten=AdultContentPolicy.safeSearchUrl("https://google.com/search?q=x&%73afe=off&SAFE=off",AdultContentPolicy.STRICT);
        assertEquals("https://google.com/search?q=x&safe=active", rewritten);
        assertEquals(rewritten,AdultContentPolicy.safeSearchUrl(rewritten,AdultContentPolicy.STRICT));
    }
    @Test public void searchIsRewrittenOnceAndOffMeansOff(){
        String original="https://www.google.com/search?q=hello&safe=off";
        BrowserContentGate.Decision rewritten=BrowserContentGate.evaluate(original, AdultContentPolicy.MODERATE);
        assertEquals(BrowserContentGate.Action.REWRITE, rewritten.action);
        assertTrue(rewritten.url.endsWith("safe=active"));
        assertEquals(BrowserContentGate.Action.ALLOW,
            BrowserContentGate.evaluate(rewritten.url, AdultContentPolicy.MODERATE).action);
        assertEquals(BrowserContentGate.Action.ALLOW,
            BrowserContentGate.evaluate(original, AdultContentPolicy.OFF).action);
    }
}
