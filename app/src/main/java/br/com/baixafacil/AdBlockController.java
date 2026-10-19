package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Motor off-line em memória. Atualizações remotas somente após toque do usuário.
 * Não lê cookies, tráfego pessoal ou conteúdo de páginas, nem usa VPN ou root.
 */
public final class AdBlockController {
    public static final String PREFS="adblock_v1";
    private static final String CACHE="filters_domains_v1.txt";
    private static final int MAX_BYTES=5*1024*1024;
    private static final String[] SOURCES={
        "https://easylist.to/easylist/easylist.txt",
        "https://easylist.to/easylist/easyprivacy.txt"
    };
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{
        Thread t=new Thread(r,"BaixaFacil-AdBlock");t.setDaemon(true);return t;
    });
    private static final AtomicInteger BLOCKED=new AtomicInteger();
    private final Context context;
    private volatile AdBlockRules rules=AdBlockRules.empty();
    private volatile int mode=AdBlockRules.STRONG;
    private volatile boolean trackers=true,cosmetic=true;
    private volatile Set<String> exemptions=Collections.emptySet();
    private volatile String pageUrl="";
    private volatile long lastRulesTimestamp = -1;

    public AdBlockController(Context context) {
        this.context=context.getApplicationContext();
        loadPreferences();
        WORKER.execute(this::loadLocalRules);
    }
    private SharedPreferences preferences(){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public void loadPreferences(){
        SharedPreferences prefs=preferences();
        int chosen=prefs.getInt("mode",AdBlockRules.STRONG);
        mode=chosen>=0&&chosen<=3?chosen:AdBlockRules.STRONG;
        trackers=prefs.getBoolean("trackers",true);
        cosmetic=prefs.getBoolean("cosmetic",true);
        exemptions=Collections.unmodifiableSet(new HashSet<>(prefs.getStringSet("except_sites",Collections.emptySet())));
        long newest=prefs.getLong("updated_at",0);
        if(lastRulesTimestamp>=0 && newest!=lastRulesTimestamp)WORKER.execute(this::loadLocalRules);
    }
    public int mode(){return mode;}
    public boolean cosmetic(){return cosmetic;}
    public int ruleCount(){return rules.count();}
    /** Contagem somente em memória dos eventos onContentBlocked do Gecko; não retém URLs. */
    public static int sessionBlocked(){return BLOCKED.get();}
    public static void noteGeckoBlocked(){BLOCKED.incrementAndGet();}
    public void setPage(String url){ pageUrl=url==null?"":url; }
    public boolean isException(String url){
        String host=AdBlockRules.host(url);
        if(host==null)return false;
        for(String domain:exemptions)if(host.equals(domain)||host.endsWith("."+domain))return true;
        return false;
    }
    public void setException(String url,boolean allow){
        String host=AdBlockRules.host(url);
        if(host==null)return;
        if(host.startsWith("www."))host=host.substring(4);
        else if(host.startsWith("m."))host=host.substring(2);
        Set<String> sites=new HashSet<>(exemptions);
        if(allow)sites.add(host);
        else {
            final String current=host;
            sites.removeIf(domain->current.equals(domain) || current.endsWith("."+domain));
        }
        preferences().edit().putStringSet("except_sites",sites).apply();
        exemptions=Collections.unmodifiableSet(sites);
    }
    public boolean shouldBlock(String resource){
        String page=pageUrl;
        if(isException(page))return false;
        boolean block=rules.shouldBlock(resource,page,mode,trackers);
        // Regras locais não são conectadas à rede do GeckoView.
        // O contador público recebe exclusivamente eventos nativos do Gecko.
        return block;
    }
    public boolean shouldHideCosmetic(String url){
        return mode!=AdBlockRules.OFF && cosmetic && !isException(url)
            && AdBlockRules.host(url)!=null && !AuthNavigation.isSensitiveLoginPage(url);
    }
    private void loadLocalRules(){
        AdBlockRules.Builder builder=new AdBlockRules.Builder();
        try(InputStream seed=context.getAssets().open("adblock_seed.txt")) {parse(seed,builder,10000);}
        catch(Exception ignored){}
        File file=new File(context.getFilesDir(),CACHE);
        if(file.isFile()){
            try(InputStream input=new FileInputStream(file)){parse(input,builder,AdBlockRules.MAX_RULES);}
            catch(Exception ignored){}
        }
        rules=builder.build();
        lastRulesTimestamp=preferences().getLong("updated_at",0);
    }
    private static int parse(InputStream stream,AdBlockRules.Builder builder,int limit) throws Exception {
        int count=0;
        BufferedReader in=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
        String line;
        while((line=in.readLine())!=null){
            count+=builder.add(line);
            if(count>=limit)break;
        }
        return count;
    }
    public interface Completion {void finished(boolean success,String message);}
    public void updateNow(Completion callback){
        WORKER.execute(()->{
            boolean ok=false;
            String message="Não foi possível atualizar as listas. As regras locais foram mantidas.";
            File destination=new File(context.getFilesDir(),CACHE);
            File temp=new File(context.getFilesDir(),CACHE+".tmp");
            try {
                AdBlockRules.Builder verify=new AdBlockRules.Builder();
                int total=0;
                try(FileOutputStream out=new FileOutputStream(temp,false)) {
                    for(String source:SOURCES) {
                        HttpURLConnection connection=(HttpURLConnection)new URL(source).openConnection();
                        connection.setConnectTimeout(9000);
                        connection.setReadTimeout(12000);
                        connection.setInstanceFollowRedirects(false);
                        connection.setRequestProperty("Accept","text/plain");
                        try {
                            if(connection.getResponseCode()!=200 || connection.getContentLengthLong()>MAX_BYTES)
                                throw new IllegalStateException("Servidor não retornou a lista esperada");
                            int remaining=MAX_BYTES;
                            try(InputStream input=connection.getInputStream()){
                                BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8));
                                String line;
                                while((line=reader.readLine())!=null){
                                    remaining-=line.getBytes(StandardCharsets.UTF_8).length+1;
                                    if(remaining<0)throw new IllegalStateException("Lista muito grande");
                                    if(verify.add(line)!=0){
                                        byte[] bytes=(line+"\n").getBytes(StandardCharsets.UTF_8);
                                        out.write(bytes);
                                        total++;
                                    }
                                }
                            }
                        } finally {connection.disconnect();}
                    }
                    out.getFD().sync();
                }
                if(total<100 || !temp.renameTo(destination))throw new IllegalStateException("Conteúdo insuficiente ou erro ao salvar");
                loadLocalRules();
                preferences().edit().putLong("updated_at",System.currentTimeMillis()).apply();
                ok=true;
                message="Listas locais atualizadas; ainda não são usadas para bloquear requisições no GeckoView.";
            }catch(Exception ex){temp.delete();}
            callback.finished(ok,message);
        });
    }
    public static void setMode(Context c,int value){
        if(value<0||value>3)return;
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt("mode",value).apply();
    }
    public static void setTrackers(Context c,boolean enabled){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean("trackers",enabled).apply();}
    public static void setCosmetic(Context c,boolean enabled){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean("cosmetic",enabled).apply();}
    public static int savedMode(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("mode",AdBlockRules.STRONG);}
    public static boolean savedCosmetic(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean("cosmetic",true);}
    public static boolean savedTrackers(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean("trackers",true);}
}
