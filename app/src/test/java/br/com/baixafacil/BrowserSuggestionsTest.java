package br.com.baixafacil;

import org.junit.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class BrowserSuggestionsTest {
    private static BrowserSuggestions.Candidate item(String url,String title,boolean favorite){
        return new BrowserSuggestions.Candidate(url,title,favorite);
    }
    @Test public void findsDomainPrefixIgnoringSchemeAndWww(){
        List<BrowserSuggestions.Item> list=BrowserSuggestions.suggest("https://git",Arrays.asList(item("https://www.github.com/home","GitHub",true)));
        assertEquals(1,list.size());assertEquals("github.com",list.get(0).host);
    }
    @Test public void usesFavoriteTitles(){
        List<BrowserSuggestions.Item> list=BrowserSuggestions.suggest("projetos",Arrays.asList(item("https://files.example.org","Projetos privados",true)));
        assertEquals(1,list.size());assertTrue(list.get(0).favorite);
    }
    @Test public void searchesHistoryWithoutFavorites(){
        assertEquals(1,BrowserSuggestions.suggest("docs",Arrays.asList(item("https://docs.example.org/start","",false))).size());
    }
    @Test public void prioritizesDomainOverTitle(){
        List<BrowserSuggestions.Item> list=BrowserSuggestions.suggest("abc",Arrays.asList(
            item("https://other.example.org","abc notes",true),item("https://abc.example.org","",false)));
        assertEquals("abc.example.org",list.get(0).host);
    }
    @Test public void doesNotOfferLoginOrOauthTokens(){
        List<BrowserSuggestions.Item> list=BrowserSuggestions.suggest("example",Arrays.asList(
            item("https://accounts.google.com/o/oauth2/v2/auth","",false),
            item("https://example.org/callback?code=secret","",false),
            item("https://example.org/page","",false)));
        assertEquals(1,list.size());assertEquals("https://example.org/page",list.get(0).url);
    }
    @Test public void excludesNonWebAndCredentials(){
        assertTrue(BrowserSuggestions.suggest("example",Arrays.asList(
            item("file:///sdcard/example.html","",false),item("https://john:pass@example.org/","",true))).isEmpty());
    }
    @Test public void ignoresBlankAndOverlongInput(){
        List<BrowserSuggestions.Candidate> items=Arrays.asList(item("https://docs.example.org","",true));
        assertTrue(BrowserSuggestions.suggest("  ",items).isEmpty());
        assertTrue(BrowserSuggestions.suggest("x".repeat(181),items).isEmpty());
    }
    @Test public void capsAtFiveSuggestionsAndDeduplicatesUrl(){
        List<BrowserSuggestions.Candidate> items=new ArrayList<>();
        for(int i=0;i<10;i++)items.add(item("https://example"+i+".org/a","",false));
        items.add(item("https://example0.org/a","",true));
        List<BrowserSuggestions.Item> list=BrowserSuggestions.suggest("example",items);
        assertEquals(5,list.size());
        assertEquals("https://example0.org/a",list.get(0).url);
    }
    @Test public void usesRecentOrderForEqualScore(){
        List<BrowserSuggestions.Item> list=BrowserSuggestions.suggest("site",Arrays.asList(
            item("https://site-b.org","",false),item("https://site-a.org","",false)));
        assertEquals("https://site-b.org",list.get(0).url);
    }
}
