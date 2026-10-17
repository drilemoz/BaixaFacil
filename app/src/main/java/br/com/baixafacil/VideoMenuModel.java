package br.com.baixafacil;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Agrupa fontes por identidade conservadora do caminho: nunca apenas pelo domínio.
 * Sem consultas ao servidor, cookies, leitura de playlists ou exposição de URLs assinadas.
 */
final class VideoMenuModel {
    private static final Pattern QUALITY = Pattern.compile("(?i)(^|[/_.-])(2160|1440|1080|720|480|360|240)p(?=$|[/_.-])");
    private static final Pattern NUMBER_FOLDER = Pattern.compile("(?i)(/)(2160|1440|1080|720|480|360|240)(?=/)");

    static final class Source {
        final String url, label, hint;
        final String quality;
        final int pixels;
        Source(String u,String l,String h,String q,int p){url=u;label=l;hint=h;quality=q;pixels=p;}
    }
    static final class Video {
        final String url,domain,title,detail;
        final MediaSourcePolicy.Kind kind;
        final List<Source> sources;
        Video(String u,String d,String t,String sub,MediaSourcePolicy.Kind k,List<Source> s){
            url=u;domain=d;title=t;detail=sub;kind=k;sources=Collections.unmodifiableList(s);
        }
    }
    private static final class Group {
        final MediaSourcePolicy.Kind kind;
        final String host, title;
        final LinkedHashMap<String,Source> variants=new LinkedHashMap<>();
        Group(MediaSourcePolicy.Kind k,String h,String t){kind=k;host=h;title=t;}
    }
    private VideoMenuModel(){}

    static List<Video> display(Map<String,String> candidates,boolean streams,boolean direct){
        LinkedHashMap<String,Group> groups=new LinkedHashMap<>();
        for(String url:candidates.keySet()){
            if(!VideoLinks.allowedHttpUrl(url))continue;
            MediaSourcePolicy.Kind kind=MediaSourcePolicy.classify(url);
            boolean streaming=kind==MediaSourcePolicy.Kind.HLS||kind==MediaSourcePolicy.Kind.DASH;
            boolean explicit=kind==MediaSourcePolicy.Kind.DIRECT;
            boolean hinted=kind==MediaSourcePolicy.Kind.UNSUPPORTED &&
                ("video".equals(candidates.get(url))||"source".equals(candidates.get(url)));
            if(!streaming&&!explicit&&!hinted)continue;
            if(streaming&&!streams)continue;
            if(!streaming&&!direct)continue;
            try {
                URI uri=new URI(url);
                if(uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getHost()==null)continue;
                String path=uri.getPath()==null?"":uri.getPath();
                String quality=quality(path);
                String id=key(uri,kind,path);
                String host=uri.getHost().toLowerCase(Locale.ROOT);
                String title=title(kind,path);
                Group group=groups.get(id);
                if(group==null){group=new Group(kind,host,title);groups.put(id,group);}
                // Troca o token antigo pela URL mais recente para o mesmo arquivo/caminho,
                // sem copiar parâmetros da URL para a interface ou para o armazenamento.
                String variant=canonicalHost(uri,path)+path;
                group.variants.put(variant,new Source(url,"", "",quality,pixels(quality)));
            } catch(Exception ignored){}
        }
        List<Video> out=new ArrayList<>();
        for(int priority=0;priority<3;priority++)for(Group group:groups.values()){
            int rank=group.kind==MediaSourcePolicy.Kind.DIRECT?0:
                group.kind==MediaSourcePolicy.Kind.HLS||group.kind==MediaSourcePolicy.Kind.DASH?1:2;
            if(rank!=priority)continue;
            List<Source> sourceList=new ArrayList<>(group.variants.values());
            sourceList.sort(Comparator.comparingInt((Source s)->s.pixels).reversed());
            List<Source> labeled=new ArrayList<>();
            for(int i=0;i<sourceList.size();i++){
                Source s=sourceList.get(i);
                String format=group.kind==MediaSourcePolicy.Kind.HLS?"HLS":
                    group.kind==MediaSourcePolicy.Kind.DASH?"DASH":
                    group.kind==MediaSourcePolicy.Kind.UNSUPPORTED?"Formato incerto":"Arquivo direto";
                String name=!s.quality.isEmpty()?s.quality+" • "+format:format+" • fonte "+(i+1);
                String hint=!s.quality.isEmpty()?"Qualidade estimada pelo endereço":"Qualidade não informada";
                labeled.add(new Source(s.url,name,hint,s.quality,s.pixels));
            }
            if(labeled.isEmpty())continue;
            String firstQuality=labeled.get(0).quality;
            String detail=group.host;
            if(labeled.size()>1)detail+=" • "+labeled.size()+" fontes/qualidades";
            else if(!firstQuality.isEmpty())detail+=" • "+firstQuality+" (estimado)";
            detail+=group.kind==MediaSourcePolicy.Kind.DIRECT?" • download direto":
                group.kind==MediaSourcePolicy.Kind.UNSUPPORTED?" • formato não confirmado":" • somente online";
            out.add(new Video(labeled.get(0).url,group.host,
                labeled.size()>1?group.kind==MediaSourcePolicy.Kind.DIRECT?"Arquivo de vídeo":group.title:group.title,
                detail,group.kind,labeled));
        }
        return out;
    }
    private static String schemeHost(URI uri){
        return uri.getScheme().toLowerCase(Locale.ROOT)+"://"+uri.getHost().toLowerCase(Locale.ROOT)+
            (uri.getPort()==-1?"":":"+uri.getPort());
    }
    /** Espelhos de CDN do Facebook só são equivalentes quando o caminho tem identidade longa.
     * Não associa vídeos pelo domínio ou por proximidade da requisição.
     */
    private static String canonicalHost(URI uri,String path){
        String host=uri.getHost().toLowerCase(Locale.ROOT);
        if("https".equalsIgnoreCase(uri.getScheme())
                && host.matches("scontent[.-][a-z0-9.-]+\\.fbcdn\\.net")
                && path!=null && path.length()>36
                && path.matches(".*[a-zA-Z0-9_-]{16,}.*")){
            return "https://scontent-mirror.fbcdn.net";
        }
        return schemeHost(uri);
    }
    /** Identidade sem token volátil; só usada para reencontrar metadados da mesma mídia. */
    static String mediaIdentity(String url){
        if(!VideoLinks.allowedHttpUrl(url))return "";
        try{
            URI uri=new URI(url);
            if(uri.getFragment()!=null || uri.getUserInfo()!=null)return "";
            MediaSourcePolicy.Kind kind=MediaSourcePolicy.classify(url);
            String path=uri.getPath()==null?"":uri.getPath();
            return key(uri,kind,path);
        }catch(Exception ignored){return "";}
    }

    private static String key(URI uri,MediaSourcePolicy.Kind kind,String path){
        // URLs do mesmo arquivo com assinaturas diferentes são uma fonte apenas.
        // Somente agrupa variantes de qualidade explícita no MESMO caminho lógico.
        String normalized=QUALITY.matcher(path).replaceAll("$1{qualidade}");
        normalized=NUMBER_FOLDER.matcher(normalized).replaceAll("$1{qualidade}");
        String key=kind+":"+canonicalHost(uri,path)+normalized;
        String query=uri.getRawQuery();
        if(query==null)return key;
        // Se a identidade da mídia está na query, não juntar filmes diferentes.
        // Endpoints sem extensão podem ter identidade desconhecida: nunca reduzi-los
        // somente ao caminho (ex.: /video?id=123 vs /video?id=456).
        if(kind==MediaSourcePolicy.Kind.UNSUPPORTED)return key+"?"+query;
        StringBuilder identity=new StringBuilder();
        for(String parameter:query.split("&")){
            int eq=parameter.indexOf('=');
            String name=(eq<0?parameter:parameter.substring(0,eq)).toLowerCase(Locale.ROOT);
            if(name.equals("id")||name.equals("video")||name.equals("video_id")||
                    name.equals("videoid")||name.equals("v")||name.equals("media_id")||
                    name.equals("file_id")||name.equals("clip_id")||name.equals("asset_id")||
                    name.equals("playlist_id")||name.equals("content_id")){
                identity.append('|').append(parameter);
            }
        }
        return key+identity;
    }
    private static String title(MediaSourcePolicy.Kind kind,String path){
        if(kind==MediaSourcePolicy.Kind.HLS)return "Transmissão HLS";
        if(kind==MediaSourcePolicy.Kind.DASH)return "Transmissão DASH";
        if(kind==MediaSourcePolicy.Kind.UNSUPPORTED)return "Fonte de vídeo não verificada";
        String file=path.substring(path.lastIndexOf('/')+1);
        if(file.length()>0 && file.length()<48)return file;
        return "Arquivo de vídeo";
    }
    private static String quality(String path){
        Matcher matcher=QUALITY.matcher(path);
        if(matcher.find())return matcher.group(2)+"p";
        Matcher folder=NUMBER_FOLDER.matcher(path);
        if(folder.find())return folder.group(2)+"p";
        return "";
    }
    private static int pixels(String quality){
        if(quality.isEmpty())return -1;
        try{return Integer.parseInt(quality.substring(0,quality.length()-1));}
        catch(NumberFormatException ignored){return -1;}
    }
}
