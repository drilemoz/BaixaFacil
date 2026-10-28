package br.com.baixafacil;

import java.net.IDN;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Filtragem conservadora, offline, de navegações. NÃO analisa imagens/termos ou tráfego HTTPS.
 * O nível rigoroso amplia somente a lista de domínios; não equivale a controle parental do SO.
 */
final class AdultContentPolicy {
    static final int OFF=0, MODERATE=1, STRICT=2;
    private static final Set<String> COMMON=new HashSet<>(Arrays.asList(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "tube8.com", "spankbang.com", "eporner.com", "beeg.com",
        "tnaflix.com", "sunporno.com", "porn.com", "youjizz.com", "drtuber.com",
        "sex.com", "thumbzilla.com", "4tube.com", "pornhd.com", "hqporner.com"
    ));
    private static final Set<String> EXTRA=new HashSet<>(Arrays.asList(
        "brazzers.com", "bangbros.com", "realitykings.com", "naughtyamerica.com",
        "onlyfans.com", "chaturbate.com", "stripchat.com", "bongacams.com",
        "livejasmin.com", "myfreecams.com", "cam4.com", "camsoda.com",
        "manyvids.com", "fansly.com", "rule34.xxx", "nhentai.net", "e-hentai.org",
        "hentaihaven.xxx", "literotica.com", "youporngay.com"
    ));
    // Lista deliberadamente limitada: não reescrever domínios apenas parecidos
    // com um buscador (ex.: google.bad) nem subdomínios arbitrários.
    private static final Set<String> GOOGLE_SEARCH_DOMAINS=new HashSet<>(Arrays.asList(
        "google.com", "google.com.br", "google.com.ar", "google.com.mx", "google.com.co",
        "google.com.pe", "google.com.au", "google.com.tr", "google.co.uk", "google.co.jp",
        "google.co.in", "google.co.za", "google.ca", "google.de", "google.fr",
        "google.es", "google.it", "google.pt", "google.cl", "google.nl"
    ));
    private AdultContentPolicy() {}

    private static boolean searchDomain(String host, String domain){
        return host.equals(domain) || host.equals("www."+domain);
    }
    private static boolean googleSearchDomain(String host){
        String base=host.startsWith("www.")?host.substring(4):host;
        return GOOGLE_SEARCH_DOMAINS.contains(base);
    }
    private static boolean matchesEncodedKey(String rawName,String key){
        try { return key.equalsIgnoreCase(URLDecoder.decode(rawName,StandardCharsets.UTF_8)); }
        catch(IllegalArgumentException error) { return key.equalsIgnoreCase(rawName); }
    }

    static int normalize(int mode){return mode==MODERATE||mode==STRICT?mode:OFF;}
    static String host(String raw){
        if(raw==null||raw.length()>4096)return null;
        try {
            URI uri=new URI(raw);
            if(!"https".equalsIgnoreCase(uri.getScheme())&&!"http".equalsIgnoreCase(uri.getScheme()))return null;
            if(uri.getRawUserInfo()!=null)return null;
            String host=uri.getHost();
            if(host==null){
                // java.net.URI não reconhece todos os domínios Unicode; o GeckoView
                // pode normalizá-los via IDNA antes de navegar. Evitar essa lacuna.
                String authority=uri.getRawAuthority();
                if(authority==null || authority.indexOf('@')>=0 || authority.startsWith("["))return null;
                int colon=authority.lastIndexOf(':');
                if(colon>=0){
                    // Portas inválidas e múltiplos ':' não viram falsos domínios.
                    if(authority.indexOf(':')!=colon)return null;
                    String port=authority.substring(colon+1);
                    if(port.isEmpty() || port.length()>5)return null;
                    for(int i=0;i<port.length();i++)if(!Character.isDigit(port.charAt(i)))return null;
                    authority=authority.substring(0,colon);
                }
                host=authority;
            }
            if(host.isEmpty())return null;
            while(host.endsWith("."))host=host.substring(0,host.length()-1);
            if(host.isEmpty())return null;
            return IDN.toASCII(host,IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch(Exception ignored){return null;}
    }
    private static boolean sameOrChild(String host,String domain){return host.equals(domain)||host.endsWith("."+domain);}
    static boolean blocked(String url,int mode){
        if(normalize(mode)==OFF)return false;
        String host=host(url);
        if(host==null)return false;
        for(String name:COMMON)if(sameOrChild(host,name))return true;
        if(mode==STRICT)for(String name:EXTRA)if(sameOrChild(host,name))return true;
        return false;
    }
    /** Pesquisa segura é um filtro de resultados, não bloqueio de conteúdo dentro dos sites. */
    static String safeSearchUrl(String raw,int mode){
        if(normalize(mode)==OFF||raw==null||raw.length()>4096)return raw;
        String host=host(raw);
        if(host==null)return raw;
        try {
            URI uri=new URI(raw);
            String path=uri.getRawPath();
            String query=uri.getRawQuery();
            if(query==null)return raw;
            String key=null,value=null;
            if(googleSearchDomain(host) && "/search".equals(path)) {key="safe";value="active";}
            else if(searchDomain(host,"bing.com")&&"/search".equals(path)){key="adlt";value="strict";}
            else if(searchDomain(host,"duckduckgo.com")&&("/".equals(path)||"/html/".equals(path))) {key="kp";value="1";}
            if(key==null)return raw;
            StringBuilder filtered=new StringBuilder();
            for(String part:query.split("&",-1)){
                String name=part.split("=",2)[0];
                if(matchesEncodedKey(name,key))continue;
                if(filtered.length()>0)filtered.append('&');
                filtered.append(part);
            }
            if(filtered.length()>0)filtered.append('&');
            filtered.append(key).append('=').append(value);
            // Construção com raw components: preservar percent-encoding da consulta original.
            String prefix=raw.substring(0,raw.indexOf('?'));
            String fragment=uri.getRawFragment();
            return prefix+"?"+filtered+(fragment==null?"":"#"+fragment);
        }catch(Exception ignored){return raw;}
    }
}
