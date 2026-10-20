package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Histórico de frequência separado do histórico legado. Sem credenciais, tokens ou parâmetros de login. */
final class BrowserTopSites {
    private static final String KEY="most_visited_v1";
    private static final int CAP=80;
    static final class Site {
        final String url,title;final int visits;final long recent;
        Site(String u,String t,int v,long r){url=u;title=t;visits=v;recent=r;}
    }
    private BrowserTopSites(){}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("browser_ui",Context.MODE_PRIVATE);}
    private static boolean allowed(String url){
        return url!=null && url.length()<=2048 && url.equals(UrlTools.extract(url))
            && !AuthNavigation.isSensitiveLoginPage(url) && !LoginWindowPolicy.hasLoginParameters(url);
    }
    static synchronized List<Site> sites(Context c){
        ArrayList<Site> sites=new ArrayList<>();
        try{
            JSONArray json=new JSONArray(prefs(c).getString(KEY,"[]"));
            for(int i=0;i<json.length()&&sites.size()<CAP;i++){
                JSONObject item=json.optJSONObject(i);if(item==null)continue;
                String u=item.optString("u");if(!allowed(u))continue;
                sites.add(new Site(u,item.optString("t",""),Math.max(0,item.optInt("v",0)),item.optLong("r",0)));
            }
        }catch(Exception ignored){}
        return sites;
    }
    private static void save(Context c,List<Site> sites){
        JSONArray json=new JSONArray();
        for(int i=0;i<Math.min(CAP,sites.size());i++){
            Site site=sites.get(i);if(!allowed(site.url))continue;
            JSONObject value=new JSONObject();
            try{value.put("u",site.url);value.put("t",site.title);value.put("v",site.visits);value.put("r",site.recent);json.put(value);}catch(Exception ignored){}
        }
        prefs(c).edit().putString(KEY,json.toString()).apply();
    }
    static synchronized void record(Context c,String url){
        if(!allowed(url))return;
        List<Site> list=sites(c);Site old=null;
        for(Site site:list)if(site.url.equals(url)){old=site;break;}
        if(old!=null)list.remove(old);
        list.add(0,new Site(url,old==null?"":old.title,Math.min(1_000_000,(old==null?0:old.visits)+1),System.currentTimeMillis()));
        if(list.size()>CAP)list=list.subList(0,CAP);
        save(c,list);
    }
    static synchronized void setTitle(Context c,String url,String title){
        if(!allowed(url)||title==null)return;
        title=title.trim();if(title.isEmpty())return;
        if(title.length()>120)title=title.substring(0,120);
        List<Site> sites=sites(c);
        for(int i=0;i<sites.size();i++)if(sites.get(i).url.equals(url)){
            Site old=sites.get(i);sites.set(i,new Site(url,title,old.visits,old.recent));save(c,sites);return;
        }
    }
    static synchronized String title(Context c,String url){for(Site site:sites(c))if(site.url.equals(url))return site.title;return "";}
    static synchronized List<Site> mostVisited(Context c,int max){
        List<Site> list=sites(c);
        Collections.sort(list,(a,b)->{
            int comp=Integer.compare(b.visits,a.visits);
            return comp!=0?comp:Long.compare(b.recent,a.recent);
        });
        if(list.size()>max)return new ArrayList<>(list.subList(0,max));
        return list;
    }
    static synchronized void clear(Context c){prefs(c).edit().remove(KEY).apply();}
}
