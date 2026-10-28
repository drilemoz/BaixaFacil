package br.com.baixafacil;

import java.net.URI;
import java.util.Locale;

/** Isola permissões web da origem do documento principal. Não registra caminhos ou tokens. */
final class GeckoPermissionPolicy {
    private GeckoPermissionPolicy(){}

    /** HTTPS estrito; porta 443 implícita e explícita são a mesma origem. */
    static String origin(String raw){
        if(raw==null || raw.length()>4096)return null;
        try {
            URI uri=new URI(raw);
            String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme()) || host==null || host.isEmpty() ||
                uri.getRawUserInfo()!=null)
                return null;
            int port=uri.getPort();
            if(port==0 || port>65535 || port< -1)return null;
            return "https://"+host.toLowerCase(Locale.ROOT)+":"+(port<0?443:port);
        }catch(Exception ignored){return null;}
    }
    static boolean sameOrigin(String requested,String current){
        String a=origin(requested);
        return a!=null && a.equals(origin(current));
    }
}
