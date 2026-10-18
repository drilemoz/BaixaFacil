package br.com.baixafacil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** HLS VOD sem proteção, sem JavaScript, sem leitura de cookies e sem conversão de codec.
 * Somente segmentos MPEG-TS completos são aceitos, para montar um arquivo .ts reproduzível.
 */
final class HlsVodPlaylist {
    private static final Pattern BANDWIDTH=Pattern.compile("(?:^|,)\\s*BANDWIDTH=(\\d+)(?:,|$)",Pattern.CASE_INSENSITIVE);
    static final int MAX_SEGMENTS=1800;
    static final int MAX_PLAYLIST_CHARS=262144;
    final String variantUrl;
    final List<String> segments;
    private HlsVodPlaylist(String variantUrl,List<String> segments){
        this.variantUrl=variantUrl;
        this.segments=Collections.unmodifiableList(segments);
    }
    static HlsVodPlaylist parse(String base,String text) throws IllegalArgumentException {
        if(!VideoLinks.allowedHttpUrl(base)||text==null||text.length()>MAX_PLAYLIST_CHARS)
            throw new IllegalArgumentException("Playlist HLS muito grande ou URL inválida.");
        String[] rows=text.replace("\r","").split("\n",-1);
        if(rows.length==0||!rows[0].replace("\uFEFF","").trim().equals("#EXTM3U"))
            throw new IllegalArgumentException("Resposta não é uma playlist HLS.");
        boolean end=false,hasSegments=false,master=false,expectVariant=false,expectSegment=false;
        long highest=-1,followingBandwidth=0;
        String variant=null;
        ArrayList<String> urls=new ArrayList<>();
        for(String raw:rows){
            String row=raw.trim();
            if(row.isEmpty())continue;
            String upper=row.toUpperCase(Locale.ROOT);
            if(upper.startsWith("#EXT-X-KEY:")||upper.startsWith("#EXT-X-SESSION-KEY:")||
               upper.startsWith("#EXT-X-MAP:")||upper.startsWith("#EXT-X-BYTERANGE:")||
               upper.startsWith("#EXT-X-DISCONTINUITY")||upper.startsWith("#EXT-X-I-FRAME-STREAM-INF:")||
               upper.startsWith("#EXT-X-MEDIA:")||
               upper.startsWith("#EXT-X-GAP")||upper.startsWith("#EXT-X-PART:")||upper.startsWith("#EXT-X-PRELOAD-HINT:"))
                throw new IllegalArgumentException("Playlist protegida ou segmentação não suportada.");
            if(row.equals("#EXT-X-ENDLIST")){end=true;continue;}
            if(upper.startsWith("#EXT-X-STREAM-INF:")){
                master=true;expectVariant=true;expectSegment=false;
                Matcher m=BANDWIDTH.matcher(row.substring(row.indexOf(':')+1));
                followingBandwidth=m.find()?parseBandwidth(m.group(1)):0;
                continue;
            }
            if(upper.startsWith("#EXTINF:")){
                hasSegments=true;expectSegment=true;continue;
            }
            if(row.startsWith("#"))continue;
            if(expectVariant){
                String resolved=resolve(base,row);
                if(!MediaSourcePolicy.isPlaylist(resolved)||MediaSourcePolicy.classify(resolved)!=MediaSourcePolicy.Kind.HLS)
                    throw new IllegalArgumentException("Variante HLS inválida.");
                if(variant==null||followingBandwidth>highest){variant=resolved;highest=followingBandwidth;}
                expectVariant=false;
                continue;
            }
            if(expectSegment){
                String resolved=resolve(base,row);
                String path;
                try {path=new URI(resolved).getPath().toLowerCase(Locale.ROOT);}catch(Exception e){throw new IllegalArgumentException("Segmento inválido.");}
                if(!(path.endsWith(".ts")||path.endsWith(".m2ts")||path.endsWith(".mpegts")))
                    throw new IllegalArgumentException("Somente segmentos MPEG-TS são suportados.");
                urls.add(resolved);
                if(urls.size()>MAX_SEGMENTS)throw new IllegalArgumentException("Playlist acima do limite de segmentos.");
                expectSegment=false;
                continue;
            }
            throw new IllegalArgumentException("Formato de playlist não reconhecido.");
        }
        if(expectVariant||expectSegment)throw new IllegalArgumentException("Playlist incompleta.");
        if(master && !hasSegments && variant!=null)return new HlsVodPlaylist(variant,Collections.emptyList());
        if(master||!hasSegments||!end||urls.isEmpty())
            throw new IllegalArgumentException("Apenas playlists HLS de vídeo finalizadas são suportadas, não transmissões ao vivo.");
        return new HlsVodPlaylist(null,urls);
    }
    private static long parseBandwidth(String value){
        try{return Long.parseLong(value);}catch(NumberFormatException e){return 0;}
    }
    private static String resolve(String base,String value){
        try {
            URI uri=new URI(base).resolve(new URI(value));
            String url=uri.toString();
            if(!VideoLinks.allowedHttpUrl(url)||uri.getRawFragment()!=null||
                    TransferHttp.isDowngrade(base,url))
                throw new IllegalArgumentException("Link do segmento inseguro.");
            return url;
        }catch(URISyntaxException e){throw new IllegalArgumentException("URL da playlist inválida.");}
    }
}
