package br.com.baixafacil;

import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Adapta com validação o mapa não parametrizado devolvido por WebStorage.getOrigins().
 * Não lê cookies; somente origens HTTP(S) válidas passam para o gerenciador local.
 */
final class WebStorageOrigins {
    private WebStorageOrigins() {}

    static <T> List<Map.Entry<String,T>> checked(Map<?,?> raw, Class<T> valueType) {
        List<Map.Entry<String,T>> valid=new ArrayList<>();
        if(raw==null || valueType==null)return valid;
        for(Map.Entry<?,?> entry:raw.entrySet()) {
            if(!(entry.getKey() instanceof String) || !valueType.isInstance(entry.getValue()))continue;
            String origin=(String)entry.getKey();
            if(!isOrigin(origin))continue;
            valid.add(new AbstractMap.SimpleImmutableEntry<>(origin,valueType.cast(entry.getValue())));
        }
        return valid;
    }

    private static boolean isOrigin(String raw) {
        if(raw==null || raw.length()>2048)return false;
        try {
            URI uri=new URI(raw);
            String scheme=uri.getScheme();
            return ("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))
                && uri.getHost()!=null && uri.getUserInfo()==null
                && (uri.getPath()==null||uri.getPath().isEmpty()||"/".equals(uri.getPath()))
                && uri.getRawQuery()==null && uri.getRawFragment()==null;
        }catch(Exception ignored){return false;}
    }
}
