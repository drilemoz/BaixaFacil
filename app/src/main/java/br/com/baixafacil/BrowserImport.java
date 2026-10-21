package br.com.baixafacil;

import android.content.Context;
import org.mozilla.geckoview.Autocomplete;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Importação opt-in de arquivos exportados, sem acesso ao armazenamento privado de outros navegadores. */
final class BrowserImport {
    private BrowserImport(){}
    static final int MAX_BYTES=400_000;
    private static final int MAX_ROWS=500;
    private static final Pattern ANCHOR=Pattern.compile("(?is)<a\\b[^>]*?\\bhref\\s*=\\s*([\\\"'])(.*?)\\1[^>]*>(.*?)</a>");
    private static final Pattern HTML_ENTITY=Pattern.compile("&#(x[0-9a-fA-F]{1,6}|[0-9]{1,7});");
    enum Kind { BOOKMARKS, HISTORY, LOGINS }
    static final class Record {
        final String url,title;
        Record(String url,String title){this.url=url;this.title=title;}
    }
    static final class Preview {
        final Kind kind;
        final List<Record> records;
        final int rejected;
        final String format;
        Preview(Kind k,List<Record> records,int rejected,String format){this.kind=k;this.records=records;this.rejected=rejected;this.format=format;}
    }
    private static String html(String value){
        String s=value.replace("&amp;","&").replace("&quot;","\"").replace("&apos;","'")
            .replace("&#39;","'").replace("&lt;","<").replace("&gt;",">");
        Matcher m=HTML_ENTITY.matcher(s);
        StringBuffer b=new StringBuffer();
        while(m.find()){
            String n=m.group(1);String decoded="";
            try{int cp=n.startsWith("x")?Integer.parseInt(n.substring(1),16):Integer.parseInt(n);
                if(Character.isValidCodePoint(cp) && !Character.isISOControl(cp))decoded=new String(Character.toChars(cp));
            }catch(Exception ignored){}
            m.appendReplacement(b,Matcher.quoteReplacement(decoded));
        }
        m.appendTail(b);
        return b.toString().trim();
    }
    private static int column(List<String> head,String... names){
        for(int i=0;i<head.size();i++){
            String key=head.get(i).trim().toLowerCase(Locale.ROOT).replace("\uFEFF","");
            for(String name:names)if(name.equals(key))return i;
        }
        return -1;
    }
    static Preview preview(Kind kind,String input){
        if(kind==Kind.LOGINS||input==null||input.length()>MAX_BYTES)return null;
        List<Record> items=new ArrayList<>();Set<String> seen=new HashSet<>();int rejected=0;
        String format;
        if(kind==Kind.BOOKMARKS && ANCHOR.matcher(input).find()){
            format="HTML de favoritos";
            Matcher matches=ANCHOR.matcher(input);int processed=0;
            while(matches.find()){
                if(processed++>=MAX_ROWS)return null;
                String url=FavoriteRules.url(html(matches.group(2)));
                String title=html(matches.group(3).replaceAll("<[^>]{0,250}>"," "));
                if(url==null||!seen.add(url)){rejected++;continue;}
                items.add(new Record(url,FavoriteRules.title(title,url)));
            }
        }else{
            format=kind==Kind.BOOKMARKS?"CSV de favoritos":"CSV de histórico";
            List<List<String>> rows=CsvRows.parse(input,MAX_ROWS);
            if(rows==null||rows.size()<2)return null;
            List<String> header=rows.get(0);
            // Não permitir usar um CSV de senhas como histórico ou favoritos.
            if(column(header,"password","senha","password_value","secret","token")>=0)return null;
            int urlColumn=column(header,"url","address","endereço","endereco","link","uri");
            int titleColumn=column(header,"title","name","título","titulo","nome");
            if(urlColumn<0)return null;
            for(int i=1;i<rows.size();i++){
                List<String> cells=rows.get(i);
                if(cells.size()<=urlColumn){rejected++;continue;}
                String url=kind==Kind.BOOKMARKS?FavoriteRules.url(cells.get(urlColumn)):
                    BrowserPrefs.safeHistoryUrl(cells.get(urlColumn));
                if(url==null||!seen.add(url)){rejected++;continue;}
                String title=titleColumn>=0&&cells.size()>titleColumn?cells.get(titleColumn):"";
                items.add(new Record(url,kind==Kind.BOOKMARKS?FavoriteRules.title(title,url):""));
            }
        }
        return new Preview(kind,items,rejected,format);
    }
    static int importPreview(Context c,Preview preview){
        if(preview==null)return 0;
        if(preview.kind==Kind.BOOKMARKS)return BrowserPrefs.importFavorites(c,preview.records);
        if(preview.kind==Kind.HISTORY)return BrowserPrefs.importHistory(c,preview.records);
        return 0;
    }
    /** Preserva o contrato de importação legado utilizado por testes e integrações anteriores. */
    static int importBookmarks(Context c,String content){return importPreview(c,preview(Kind.BOOKMARKS,content));}
    static int importLogins(Context c,String csv){
        if(csv==null||csv.length()>MAX_BYTES)return 0;
        List<List<String>> lines=CsvRows.parse(csv,MAX_ROWS);
        if(lines==null || lines.size()<2)return 0;
        List<String> header=lines.get(0);
        int u=column(header,"username","usuário","usuario"),p=column(header,"password","senha"),o=column(header,"url","origin","site");
        if(u<0||p<0||o<0)return 0;
        if(!SecureLoginStore.readable(c))return 0;
        int n=0;
        for(int i=1;i<lines.size();i++){
            List<String> row=lines.get(i);
            if(row.size()<=Math.max(o,Math.max(u,p)))continue;
            String origin=SecureLoginStore.secureOrigin(row.get(o)),user=row.get(u),password=row.get(p);
            if(origin==null||password.isEmpty()||password.length()>4096||user.length()>512)continue;
            Autocomplete.LoginEntry e=new Autocomplete.LoginEntry.Builder().origin(origin).username(user).password(password).build();
            if(SecureLoginStore.save(c,e))n++;
        }
        return n;
    }
}
