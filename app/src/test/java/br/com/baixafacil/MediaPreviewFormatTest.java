package br.com.baixafacil;
import org.junit.Test;
import static org.junit.Assert.*;
public class MediaPreviewFormatTest {
 @Test public void formatsDuration(){
  assertEquals("",MediaPreviewFormat.duration(-1));
  assertEquals("00:00",MediaPreviewFormat.duration(0));
  assertEquals("02:05",MediaPreviewFormat.duration(125999));
  assertEquals("1:01:01",MediaPreviewFormat.duration(3661000));
 }
 @Test public void combinesOnlyAvailableMetadata(){
  assertEquals("1920×1080 • 01:30",MediaPreviewFormat.description(1920,1080,90000));
  assertEquals("1920×1080",MediaPreviewFormat.description(1920,1080,-1));
  assertEquals("02:00",MediaPreviewFormat.description(0,0,120000));
  assertEquals("",MediaPreviewFormat.description(0,-1,-1));
 }
}
