package br.com.baixafacil;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class DownloadListFiltersTest {
    private static final long MB=1024L*1024;

    @Test public void sizeRangesDoNotOverlap(){
        assertTrue(DownloadListFilters.matchesSize(2,10*MB-1,0,"Baixando"));
        assertFalse(DownloadListFilters.matchesSize(2,10*MB,0,"Baixando"));
        assertTrue(DownloadListFilters.matchesSize(3,10*MB,0,"Baixando"));
        assertTrue(DownloadListFilters.matchesSize(3,100*MB-1,0,"Baixando"));
        assertFalse(DownloadListFilters.matchesSize(3,100*MB,0,"Baixando"));
        assertTrue(DownloadListFilters.matchesSize(4,100*MB,0,"Baixando"));
    }
    @Test public void unknownSizeAndEmptyFile(){
        assertTrue(DownloadListFilters.matchesSize(1,-1,0,"Na fila"));
        assertFalse(DownloadListFilters.matchesSize(1,0,0,"Concluído"));
        assertTrue(DownloadListFilters.matchesSize(2,0,0,"Concluído"));
    }
    @Test public void neverGuessSizeFromPartial(){
        assertTrue(DownloadListFilters.matchesSize(1,-1,500*MB,"Pausado"));
        assertFalse(DownloadListFilters.matchesSize(4,-1,500*MB,"Pausado"));
    }
    @Test public void useCompletedBytesIfTotalWasUnknown(){
        assertTrue(DownloadListFilters.matchesSize(4,-1,150*MB,"Concluído"));
        assertFalse(DownloadListFilters.matchesSize(1,-1,150*MB,"Concluído"));
    }
    @Test public void allIsInclusiveAndUnknownModeIsPermissive(){
        assertTrue(DownloadListFilters.matchesSize(0,-1,-1,"Pausado"));
        assertTrue(DownloadListFilters.matchesSize(999,-1,-1,"Pausado"));
    }
    @Test public void hostHasNoQueryOrPath(){
        assertEquals("site.com",DownloadListFilters.originHost("https://Site.Com/file.mp4?token=secret#frag"));
        assertEquals("site.com",DownloadListFilters.originHost("HTTP://site.com:8080/a"));
        assertEquals("site.com",DownloadListFilters.originHost("https://site.com./a"));
    }
    @Test public void rejectNonWebAndCredentials(){
        for(String url:new String[]{null,"", "file:///tmp/a", "https://user:pass@site.com/a", "https://", "https://%ZZ", "ftp://site.com/a"}){
            assertEquals(String.valueOf(url),"",DownloadListFilters.originHost(url));
        }
    }
    @Test public void originMatchIsExactHost(){
        assertTrue(DownloadListFilters.matchesOrigin("a.site.com","https://a.site.com/file.zip?sig=x"));
        assertFalse(DownloadListFilters.matchesOrigin("site.com","https://a.site.com/file.zip"));
        assertFalse(DownloadListFilters.matchesOrigin("site.com","https://site.com.evil.org/a"));
        assertTrue(DownloadListFilters.matchesOrigin("", "file:///tmp/a"));
        assertTrue(DownloadListFilters.matchesOrigin(null,"https://any.org/a"));
    }
    @Test public void countOnlyHostsSortedByFrequency(){
        List<DownloadListFilters.OriginCount> count=DownloadListFilters.countOrigins(Arrays.asList(
            "https://b.org/2?token=xx", "https://a.org/1", "https://b.org/3", "file:///private"));
        assertEquals(3,count.size());
        assertEquals("b.org",count.get(0).host);
        assertEquals(2,count.get(0).count);
        assertEquals("Origem não identificada",count.get(1).label());
        assertEquals("a.org",count.get(2).host);
        // tie in count sorted by host
    }
    @Test public void originListHandlesEmpty(){
        assertTrue(DownloadListFilters.countOrigins(null).isEmpty());
        assertTrue(DownloadListFilters.countOrigins(Arrays.asList()).isEmpty());
    }
}
