package br.com.baixafacil;

import android.content.Context;
import org.mozilla.geckoview.Autocomplete;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Arquivos exportados escolhidos explicitamente pelo usuário. Nunca lê dados privados de outros apps. */
final class BrowserImport {
    private BrowserImport(){}
    private static final Pattern ANCHOR=Pattern.compile("(?is)<a\\s+[^>]*?href\\s*=\\s*([\"'])(.*?)\\1[^>]*>(.*?)</a>");
    static int importBookmarks(Context c,String content){
        if(content==null || content.length()>400_000)return 0;
        Matcher matcher=ANCHOR.matcher(content);int count=0;
        while(matcher.find() && count<60){
            String url=html(matcher.group(2));
            String title=html(matcher.group(3).replaceAll("<[^>]{0,250}>"," "));
            if(BrowserPrefs.addFavorite(c,url,title))count++;
        }
        return count;
    }
    private static String html(String v){
        return v.replace("&amp;","&").replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">").trim();
    }
    static int importLogins(Context c,String csv){
        if(csv==null||csv.length()>400_000)return 0;
        List<List<String>> lines=CsvRows.parse(csv,500);
        if(lines==null || lines.size()<2)return 0;
        List<String> header=lines.get(0);
        int u=-1,p=-1,o=-1;
        for(int i=0;i<header.size();i++){
            String name=header.get(i).trim().toLowerCase(java.util.Locale.ROOT);
            if(name.equals("username")||name.equals("usuário")||name.equals("usuario"))u=i;
            if(name.equals("password")||name.equals("senha"))p=i;
            if(name.equals("url")||name.equals("origin")||name.equals("site"))o=i;
        }
        if(u<0||p<0||o<0)return 0;
        int n=0;
        for(int i=1;i<lines.size();i++){
            List<String> row=lines.get(i);
            if(row.size()<=Math.max(o,Math.max(u,p)))continue;
            String origin=SecureLoginStore.secureOrigin(row.get(o)),user=row.get(u),password=row.get(p);
            if(origin==null||password.isEmpty()||password.length()>4096||user.length()>512)continue;
            Autocomplete.LoginEntry e=new Autocomplete.LoginEntry.Builder().origin(origin).username(user).password(password).build();
            if(!SecureLoginStore.readable(c))return n;
            if(SecureLoginStore.save(c,e))n++;
        }
        return n;
    }
}
