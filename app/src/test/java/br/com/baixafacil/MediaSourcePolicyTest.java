package br.com.baixafacil;
import org.junit.Test;
import static org.junit.Assert.*;
public class MediaSourcePolicyTest {
 @Test public void classifiesStreamingWithoutOfferingFileDownload(){
  assertEquals(MediaSourcePolicy.Kind.HLS,MediaSourcePolicy.classify("https://videos.example/movie.m3u8?token=abc"));
  assertEquals(MediaSourcePolicy.Kind.DASH,MediaSourcePolicy.classify("https://videos.example/movie.mpd"));
  assertEquals(MediaSourcePolicy.Kind.DIRECT,MediaSourcePolicy.classify("https://videos.example/movie.mp4"));
 }
 @Test public void rejectsLocalAndCredentialedUris(){
  assertEquals(MediaSourcePolicy.Kind.UNSUPPORTED,MediaSourcePolicy.classify("blob:https://videos.example/blob"));
  assertEquals(MediaSourcePolicy.Kind.UNSUPPORTED,MediaSourcePolicy.classify("file:///tmp/film.mp4"));
  assertEquals(MediaSourcePolicy.Kind.UNSUPPORTED,MediaSourcePolicy.classify("https://user:pass@videos.example/film.mp4"));
  assertEquals(MediaSourcePolicy.Kind.UNSUPPORTED,MediaSourcePolicy.classify("https://videos.example/video.ts"));
 }
}
