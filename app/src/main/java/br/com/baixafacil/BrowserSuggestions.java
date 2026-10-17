package br.com.baixafacil;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Sugestões somente locais: nunca pesquisa remota durante a digitação. */
final class BrowserSuggestions {
    static final int LIMIT=5;
    static final class Item {
        final String url, title, host;
        final boolean favorite;
        final int priority, order;
        Item(String url,String title,String host,boolean favorite,int priority,int order){
            this.url=url;this.title=title;this.host=host;this.favorite=favorite;
            this.priority=priority;this.order=order;
        }
    }
    static final class Candidate {
        final String url,title;
        final boolean favorite;
        Candidate(String url,String title,boolean favorite){this.url=url;this.title=title;this.favorite=favorite;}
    }
    private BrowserSuggestions(){}
    static List<Item> suggest(String raw,List<Candidate> candidates){
        List<Item> result=new ArrayList<>();
        if(raw==null)return result;
        String term=raw.trim().toLowerCase(Locale.ROOT);
        if(term.isEmpty()||term.length()>180)return result;
        term=term.replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
        if(term.isEmpty())return result;
        Set<String> seen=new HashSet<>();
        int order=0;
        for(Candidate c:candidates){
            if(c==null || c.url==null || !seen.add(c.url))continue;
            if(UrlTools.extract(c.url)==null || !c.url.equals(UrlTools.extract(c.url)) ||
                AuthNavigation.isSensitiveLoginPage(c.url) || LoginWindowPolicy.hasLoginParameters(c.url))continue;
            try {
                URI uri=new URI(c.url);
                String host=uri.getHost();
                if(host==null||uri.getUserInfo()!=null)continue;
                host=host.toLowerCase(Locale.ROOT);
                String hostShown=host.startsWith("www.")?host.substring(4):host;
                String title=c.title==null?"":c.title.trim();
                String url=c.url.toLowerCase(Locale.ROOT);
                int score=hostShown.startsWith(term)?90:hostShown.contains(term)?70:
                    title.toLowerCase(Locale.ROOT).contains(term)?60:url.contains(term)?30:0;
                if(score==0)continue;
                if(c.favorite)score+=5;
                result.add(new Item(c.url,title.isEmpty()?hostShown:title,hostShown,c.favorite,score,order++));
            } catch(Exception ignored){}
        }
        result.sort(Comparator.comparingInt((Item i)->i.priority).reversed().thenComparingInt(i->i.order));
        return result.size()>LIMIT?new ArrayList<>(result.subList(0,LIMIT)):result;
    }
}
