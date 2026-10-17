package br.com.baixafacil;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class VideoPresentationTest {
    @Test public void facebookMirrorSamePathDeduplicated(){
        String path="/v/t39.25447-2/1020304050_longVideoIdentifierABCD1234_720p.mp4";
        LinkedHashMap<String,String> urls=new LinkedHashMap<>();
        urls.put("https://scontent.fdiq3-1.fna.fbcdn.net"+path+"?oh=old&oe=one","direct");
        urls.put("https://scontent.fdiq6-1.fna.fbcdn.net"+path+"?oh=new&oe=two","direct");
        List<VideoMenuModel.Video> videos=VideoMenuModel.display(urls,true,true);
        assertEquals(1,videos.size());
        assertEquals(1,videos.get(0).sources.size());
        assertTrue(videos.get(0).url.contains("fdiq6-1"));
    }
    @Test public void differentFacebookPathsStaySeparate(){
        LinkedHashMap<String,String> urls=new LinkedHashMap<>();
        urls.put("https://scontent.fdiq3-1.fna.fbcdn.net/v/t39.25447-2/videoIdentifierAAAAAAAAAA.mp4","direct");
        urls.put("https://scontent.fdiq6-1.fna.fbcdn.net/v/t39.25447-2/videoIdentifierBBBBBBBBBB.mp4","direct");
        assertEquals(2,VideoMenuModel.display(urls,true,true).size());
    }
    @Test public void differentIdentitiesStaySeparateEvenAcrossMirrors(){
        LinkedHashMap<String,String> urls=new LinkedHashMap<>();
        String path="/v/t39.25447-2/videoIdentifierAAAAAAAAAA.mp4";
        urls.put("https://scontent.fdiq3-1.fna.fbcdn.net"+path+"?video_id=112&oh=a","direct");
        urls.put("https://scontent.fdiq6-1.fna.fbcdn.net"+path+"?video_id=113&oh=b","direct");
        assertEquals(2,VideoMenuModel.display(urls,true,true).size());
    }
    @Test public void unrelatedHostsNeverMerge(){
        LinkedHashMap<String,String> urls=new LinkedHashMap<>();
        String path="/v/t39.25447-2/videoIdentifierAAAAAAAAAA.mp4";
        urls.put("https://cdn1.example.net"+path,"direct");
        urls.put("https://cdn2.example.net"+path,"direct");
        assertEquals(2,VideoMenuModel.display(urls,true,true).size());
    }
    @Test public void playingVisibleVideoRanksFirst(){
        LinkedHashMap<String,String> urls=new LinkedHashMap<>();
        String old="https://cdn.example.net/old.mp4",current="https://cdn.example.net/current.mp4";
        urls.put(old,"direct");urls.put(current,"direct");
        List<VideoMenuModel.Video> videos=VideoMenuModel.display(urls,true,true);
        Map<String,VideoPresentation.Context> meta=new HashMap<>();
        meta.put(current,new VideoPresentation.Context("Reel sendo assistido",true,true));
        List<VideoMenuModel.Video> sorted=VideoPresentation.sort(videos,meta);
        assertEquals(current,sorted.get(0).url);
        assertEquals("Reel sendo assistido",meta.get(current).title);
    }
    @Test public void stableOrderWithUnknownSources(){
        LinkedHashMap<String,String> urls=new LinkedHashMap<>();
        urls.put("https://cdn.example.net/one.mp4","direct");
        urls.put("https://cdn.example.net/two.mp4","direct");
        List<VideoMenuModel.Video> videos=VideoMenuModel.display(urls,true,true);
        assertEquals(videos.get(0).url,VideoPresentation.sort(videos,new HashMap<>()).get(0).url);
    }
    @Test public void rejectsSensitiveOrGenericTitles(){
        assertEquals("",VideoPresentation.cleanTitle("https://example.com/?token=secret"));
        assertEquals("",VideoPresentation.cleanTitle("Vídeo"));
        assertEquals("",VideoPresentation.cleanTitle("Clip access_token=123"));
        assertEquals("Meu vídeo curto",VideoPresentation.cleanTitle(" Meu    vídeo   curto "));
    }
    @Test public void shortOrControlOnlyTitleIgnored(){
        assertEquals("",VideoPresentation.cleanTitle("\u0001\u0002"));
        assertEquals("",VideoPresentation.cleanTitle("A"));
    }
    @Test public void signedUrlRefreshStillMatchesVisibleVideo(){
        LinkedHashMap<String,String> urls=new LinkedHashMap<>();
        String old="https://scontent.fdiq3-1.fna.fbcdn.net/v/t39.25447-2/longIdentifierABCD1234_720p.mp4?oh=old";
        String latest="https://scontent.fdiq6-1.fna.fbcdn.net/v/t39.25447-2/longIdentifierABCD1234_720p.mp4?oh=new";
        urls.put("https://example.org/older.mp4","direct");
        urls.put(latest,"direct");
        Map<String,VideoPresentation.Context> ctx=new HashMap<>();
        ctx.put(old,new VideoPresentation.Context("Reel ativo com nova assinatura",true,true));
        List<VideoMenuModel.Video> videos=VideoPresentation.sort(VideoMenuModel.display(urls,true,true),ctx);
        assertEquals(latest,videos.get(0).url);
    }
}
