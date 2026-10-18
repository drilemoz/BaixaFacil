package br.com.baixafacil;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Filtros locais de histórico: nunca faz consultas HTTP, DNS ou registra URLs completas. */
public final class DownloadListFilters {
    private DownloadListFilters() {}
    public static final int ALL_SIZES=0;
    public static final int UNKNOWN_SIZE=1;
    public static final int BELOW_10_MB=2;
    public static final int FROM_10_TO_100_MB=3;
    public static final int FROM_100_MB=4;
    private static final long MIB=1024L*1024L;

    public static String sizeLabel(int mode){
        switch(mode){
            case UNKNOWN_SIZE:return "Tamanho desconhecido";
            case BELOW_10_MB:return "Menos de 10 MB";
            case FROM_10_TO_100_MB:return "10 a 100 MB";
            case FROM_100_MB:return "100 MB ou mais";
            default:return "Todos";
        }
    }

    public static boolean matchesSize(int mode,long expected,long received,String status){
        if(mode<ALL_SIZES || mode>FROM_100_MB || mode==ALL_SIZES)return true;
        // Nunca classificar um download incompleto apenas pelos bytes parciais.
        long size=expected>=0?expected:("Concluído".equals(status) && received>=0?received:-1);
        if(mode==UNKNOWN_SIZE)return size<0;
        if(size<0)return false;
        if(mode==BELOW_10_MB)return size<10*MIB;
        if(mode==FROM_10_TO_100_MB)return size>=10*MIB && size<100*MIB;
        if(mode==FROM_100_MB)return size>=100*MIB;
        return true;
    }

    /** Somente host. Caminho, query, token, usuário e senha nunca entram no filtro. */
    public static String originHost(String address){
        if(address==null || address.length()>8192)return "";
        try{
            URI uri=new URI(address);
            String scheme=uri.getScheme();
            if(scheme==null || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)))return "";
            if(uri.getRawUserInfo()!=null)return "";
            String host=uri.getHost();
            if(host==null || host.isEmpty())return "";
            host=host.toLowerCase(Locale.ROOT);
            if(host.endsWith("."))host=host.substring(0,host.length()-1);
            return host;
        }catch(Exception ignored){return "";}
    }

    public static boolean matchesOrigin(String selectedHost,String url){
        return selectedHost==null || selectedHost.equals(originHost(url));
    }

    /** Agrega apenas hosts, sem guardar URL completa ou conteúdo de autenticação. */
    public static List<OriginCount> countOrigins(List<String> urls){
        Map<String,Integer> counters=new HashMap<>();
        if(urls!=null)for(String url:urls){
            String host=originHost(url);
            counters.put(host,counters.getOrDefault(host,0)+1);
        }
        List<OriginCount> result=new ArrayList<>();
        for(Map.Entry<String,Integer> entry:counters.entrySet())result.add(new OriginCount(entry.getKey(),entry.getValue()));
        result.sort(Comparator.comparingInt((OriginCount row)->row.count).reversed().thenComparing(row->row.host));
        return result;
    }

    public static final class OriginCount {
        public final String host;
        public final int count;
        OriginCount(String host,int count){this.host=host;this.count=count;}
        public String label(){return host.isEmpty()?"Origem não identificada":host;}
    }
}
