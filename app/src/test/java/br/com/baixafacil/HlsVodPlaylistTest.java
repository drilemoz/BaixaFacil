package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class HlsVodPlaylistTest {
    private static final String BASE="https://cdn.example.com/a/playlist.m3u8?sig=abc";
    @Test public void parsesCompleteTsVod(){
        HlsVodPlaylist p=HlsVodPlaylist.parse(BASE,"#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:5,\n001.ts?sig=hi\n#EXTINF:3,\n002.ts\n#EXT-X-ENDLIST\n");
        assertEquals(2,p.segments.size());
        assertEquals("https://cdn.example.com/a/001.ts?sig=hi",p.segments.get(0));
        assertNull(p.variantUrl);
    }
    @Test public void selectsHighestMasterBandwidth(){
        HlsVodPlaylist p=HlsVodPlaylist.parse(BASE,"#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=400000,RESOLUTION=640x360\n360.m3u8\n#EXT-X-STREAM-INF:BANDWIDTH=1200000,RESOLUTION=1920x1080\n1080.m3u8\n");
        assertEquals("https://cdn.example.com/a/1080.m3u8",p.variantUrl);
        assertTrue(p.segments.isEmpty());
    }
    @Test public void liveMissingEndRejected(){
        rejected("#EXTM3U\n#EXTINF:5,\n1.ts\n", "finalizadas");
    }
    @Test public void protectedRejected(){
        rejected("#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n#EXTINF:5,\n1.ts\n#EXT-X-ENDLIST", "protegida");
    }
    @Test public void drmSessionKeyRejected(){
        rejected("#EXTM3U\n#EXT-X-SESSION-KEY:METHOD=SAMPLE-AES\n", "protegida");
    }
    @Test public void externalAudioRejected(){
        rejected("#EXTM3U\n#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\"\n", "protegida");
    }
    @Test public void fmp4Rejected(){
        rejected("#EXTM3U\n#EXT-X-MAP:URI=\"init.mp4\"\n#EXTINF:5,\n1.m4s\n#EXT-X-ENDLIST", "protegida");
    }
    @Test public void unexpectedOtherFileRejected(){
        rejected("#EXTM3U\n#EXTINF:5,\n1.mp4\n#EXT-X-ENDLIST", "MPEG-TS");
    }
    @Test public void schemeDowngradeRejected(){
        rejected("#EXTM3U\n#EXTINF:5,\nhttp://cdn.example.com/1.ts\n#EXT-X-ENDLIST", "inseguro");
    }
    @Test public void differentPathsAreNotIdentifiedAsSameVideo(){
        assertFalse(VideoMenuModel.mediaIdentity("https://cdn.example.com/a/a.m3u8").equals(
                VideoMenuModel.mediaIdentity("https://cdn.example.com/a/b.m3u8")));
    }
    @Test public void incompleteListRejected(){
        rejected("#EXTM3U\n#EXTINF:5,\n", "incompleta");
    }
    private void rejected(String content,String message){
        try{HlsVodPlaylist.parse(BASE,content);fail("Deveria rejeitar playlist");}
        catch(IllegalArgumentException e){assertTrue(e.getMessage(),e.getMessage().contains(message));}
    }
}
