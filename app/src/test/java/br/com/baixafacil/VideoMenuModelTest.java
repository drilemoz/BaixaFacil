package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.List;

public class VideoMenuModelTest {
    @Test public void groupsDirectBeforePlaylistsAndDoesNotExposeTokens() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example.org/live/720p.m3u8?token=abc","stream");
        sources.put("https://cdn.example.org/film/movie.mp4?secret=xyz","direct");
        sources.put("https://cdn.example.org/banner.jpg","unverified");
        List<VideoMenuModel.Video> out=VideoMenuModel.display(sources,true,true);
        assertEquals(2,out.size());
        assertEquals(MediaSourcePolicy.Kind.DIRECT,out.get(0).kind);
        assertEquals(MediaSourcePolicy.Kind.HLS,out.get(1).kind);
        assertFalse(out.get(0).detail.contains("xyz"));
        assertFalse(out.get(1).detail.contains("abc"));
        assertTrue(out.get(1).detail.contains("720p"));
    }
    @Test public void respectsStreamingAndDirectFilters() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example.org/live.mpd","stream");
        sources.put("https://cdn.example.org/c.mp4","direct");
        assertEquals(1,VideoMenuModel.display(sources,false,true).size());
        assertEquals(1,VideoMenuModel.display(sources,true,false).size());
        assertEquals(0,VideoMenuModel.display(sources,false,false).size());
    }
    @Test public void doesNotOfferUnknownResourceAsDownloadableVideo() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example.org/ads.js","unverified");
        sources.put("blob:https://cdn.example.org/123","video");
        sources.put("https://user:password@cdn.example.org/c.mp4","direct");
        assertTrue(VideoMenuModel.display(sources,true,true).isEmpty());
    }
    @Test public void showsElementWithoutExtensionButMarksUnverified() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example.org/videoplayback?token=x","video");
        List<VideoMenuModel.Video> out=VideoMenuModel.display(sources,true,true);
        assertEquals(1,out.size());
        assertEquals(MediaSourcePolicy.Kind.UNSUPPORTED,out.get(0).kind);
        assertTrue(out.get(0).detail.contains("não confirmado"));
    }
    @Test public void collapsesReissuedTokensForSameManifest() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://media.example/video/master.m3u8?token=old","stream");
        sources.put("https://media.example/video/master.m3u8?token=new","stream");
        List<VideoMenuModel.Video> out=VideoMenuModel.display(sources,true,true);
        assertEquals(1,out.size());
        assertEquals(1,out.get(0).sources.size());
        assertEquals("https://media.example/video/master.m3u8?token=new",out.get(0).sources.get(0).url);
        assertFalse(out.get(0).detail.contains("token"));
    }
    @Test public void groupsRenditionsOfSameStreamAndOrdersHighestFirst() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://media.example/clip_a/360p/index.m3u8?sig=one","stream");
        sources.put("https://media.example/clip_a/1080p/index.m3u8?sig=two","stream");
        sources.put("https://media.example/clip_a/720p/index.m3u8?sig=three","stream");
        List<VideoMenuModel.Video> out=VideoMenuModel.display(sources,true,true);
        assertEquals(1,out.size());
        assertEquals(3,out.get(0).sources.size());
        assertEquals("1080p",out.get(0).sources.get(0).quality);
        assertEquals("720p",out.get(0).sources.get(1).quality);
        assertEquals("360p",out.get(0).sources.get(2).quality);
        assertEquals(MediaSourcePolicy.Kind.HLS,out.get(0).kind);
    }
    @Test public void doesNotMergeDifferentVideosFromSameCdn() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example/movies/alpha/720p/index.m3u8","stream");
        sources.put("https://cdn.example/movies/bravo/720p/index.m3u8","stream");
        assertEquals(2,VideoMenuModel.display(sources,true,true).size());
    }
    @Test public void groupsDirectQualityVariantsButNotDifferentFileIds() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example/movies/abc_480p.mp4","direct");
        sources.put("https://cdn.example/movies/abc_1080p.mp4","direct");
        sources.put("https://cdn.example/movies/def_1080p.mp4","direct");
        List<VideoMenuModel.Video> out=VideoMenuModel.display(sources,true,true);
        assertEquals(2,out.size());
        assertEquals(2,out.get(0).sources.size());
        assertEquals(MediaSourcePolicy.Kind.DIRECT,out.get(0).kind);
        assertEquals("1080p",out.get(0).sources.get(0).quality);
    }
    @Test public void treatsDifferentManifestPathsAsDifferentMedia() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example/show/master.m3u8","stream");
        sources.put("https://cdn.example/show/episode2.m3u8","stream");
        assertEquals(2,VideoMenuModel.display(sources,true,true).size());
    }
    @Test public void keepsSourcesOffLabelsAndDoesNotMergeKinds() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example/v/720p.m3u8?auth=secret123","stream");
        sources.put("https://cdn.example/v/720p.mp4?auth=secret456","direct");
        List<VideoMenuModel.Video> out=VideoMenuModel.display(sources,true,true);
        assertEquals(2,out.size());
        for(VideoMenuModel.Video item:out){
            assertFalse(item.detail.contains("secret"));
            assertFalse(item.title.contains("secret"));
            for(VideoMenuModel.Source src:item.sources)assertFalse(src.label.contains("secret"));
        }
    }
    @Test public void doesNotCollapseDifferentQueryIdentities() {
        LinkedHashMap<String,String> sources=new LinkedHashMap<>();
        sources.put("https://cdn.example/clips/movie.mp4?id=movieA&token=one","direct");
        sources.put("https://cdn.example/clips/movie.mp4?id=movieB&token=two","direct");
        sources.put("https://cdn.example/stream?video_id=a","video");
        sources.put("https://cdn.example/stream?video_id=b","video");
        assertEquals(4,VideoMenuModel.display(sources,true,true).size());
    }
}
