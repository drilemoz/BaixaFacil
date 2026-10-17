package br.com.baixafacil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Só dados do elemento de vídeo efetivamente associado à URL; sem inferir pelo domínio. */
final class VideoPresentation {
    static final class Context {
        final String title;
        final boolean playing, visible;
        Context(String title, boolean playing, boolean visible){
            this.title=cleanTitle(title);this.playing=playing;this.visible=visible;
        }
        int rank(){return playing && visible?3:visible?2:playing?1:0;}
    }
    private VideoPresentation(){}

    static String cleanTitle(String raw){
        if(raw==null)return "";
        String s=raw.replaceAll("[\\p{Cntrl}\\p{Cf}]", " ").replaceAll("\\s+"," ").trim();
        if(s.length()<4)return "";
        // Evita transformar links de sessão/códigos em título visível.
        String lower=s.toLowerCase(Locale.ROOT);
        if(lower.startsWith("http:")||lower.startsWith("https:")||lower.startsWith("blob:")
                ||lower.startsWith("data:")||lower.startsWith("javascript:")
                ||lower.contains("token=")||lower.contains("access_token")
                ||lower.equals("video")||lower.equals("vídeo")||lower.equals("reels")
                ||lower.equals("facebook")||lower.equals("watch"))return "";
        return s.length()>100?s.substring(0,100).trim()+"…":s;
    }
    static int score(VideoMenuModel.Video video,Map<String,Context> byUrl){
        int top=0;
        for(VideoMenuModel.Source source:video.sources){
            Context c=byUrl.get(source.url);
            if(c!=null)top=Math.max(top,c.rank());
            else{
                String identity=VideoMenuModel.mediaIdentity(source.url);
                if(!identity.isEmpty())for(Map.Entry<String,Context> match:byUrl.entrySet()){
                    if(identity.equals(VideoMenuModel.mediaIdentity(match.getKey())))
                        top=Math.max(top,match.getValue().rank());
                }
            }
        }
        return top;
    }
    static List<VideoMenuModel.Video> sort(List<VideoMenuModel.Video> input,Map<String,Context> byUrl){
        List<VideoMenuModel.Video> ordered=new ArrayList<>(input);
        // Sort estável: fontes sem correspondência continuam na ordem natural.
        ordered.sort(Comparator.comparingInt((VideoMenuModel.Video v)->score(v,byUrl)).reversed());
        return ordered;
    }
}
