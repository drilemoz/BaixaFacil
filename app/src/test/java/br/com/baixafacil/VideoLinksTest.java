package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class VideoLinksTest {
    @Test public void directVideoWithQuery() {
        assertTrue(VideoLinks.isDirectFile("https://cdn.example.org/media/clip.MP4?sig=abc"));
        assertTrue(VideoLinks.isDirectFile("https://a.example/movie.webm"));
        assertTrue(VideoLinks.isVideoCandidate("https://a.example/media/stream?id=123", "video", ""));
        assertTrue(VideoLinks.isVideoCandidate("https://a.example/play?id=2", "", "video/mp4"));
    }
    @Test public void unsafeAndStreamingRejected() {
        assertFalse(VideoLinks.isVideoCandidate("blob:https://fb.example/id", "video", "video/mp4"));
        assertFalse(VideoLinks.isVideoCandidate("https://a.example/playlist.m3u8", "video", ""));
        assertFalse(VideoLinks.isVideoCandidate("https://a.example/list.mpd", "source", "video/mp4"));
        assertFalse(VideoLinks.isVideoCandidate("javascript:alert(1)", "video", ""));
        assertFalse(VideoLinks.isVideoCandidate("https://user:password@a.example/v.mp4", "video", ""));
        assertFalse(VideoLinks.isVideoCandidate("https://a.example/file.jpg", "link", ""));
        assertFalse(VideoLinks.isVideoCandidate("https://a.example/movie.m4s", "source", "video/mp4"));
        assertFalse(VideoLinks.allowedHttpUrl("https://a.example/evil.mp4\nheader"));
        assertFalse(VideoLinks.allowedHttpUrl("http://a.example/file.mp4 with spaces"));
    }
    @Test public void titleAndBounds() {
        assertEquals("movie.mp4",VideoLinks.label("https://a.example/folder/movie.mp4?token=q"));
        assertEquals("Vídeo de a.example",VideoLinks.label("https://a.example/dynamic?x=1"));
        assertFalse(VideoLinks.allowedHttpUrl("https://a.example/"+"a".repeat(5000)));
    }
}
