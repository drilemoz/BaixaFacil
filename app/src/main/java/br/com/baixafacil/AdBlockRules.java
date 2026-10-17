package br.com.baixafacil;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Motor pequeno e estrito. Interpreta APENAS hosts e ||dominio^ de listas ABP.
 * Sem regex, scripts remotos, regras de URL genéricas ou permissões especiais.
 */
public final class AdBlockRules {
    public static final int OFF = 0, LIGHT = 1, STRONG = 2, EXTREME = 3;
    public static final int MAX_RULES = 160000;
    private final Set<String> blocked;
    private final Set<String> allowed;

    private AdBlockRules(Set<String> blocked, Set<String> allowed) {
        this.blocked = Collections.unmodifiableSet(new HashSet<>(blocked));
        this.allowed = Collections.unmodifiableSet(new HashSet<>(allowed));
    }
    public int count(){ return blocked.size(); }
    public static AdBlockRules empty(){ return new AdBlockRules(Collections.emptySet(),Collections.emptySet()); }
    public static final class Builder {
        private final HashSet<String> blocked = new HashSet<>(), allowed = new HashSet<>();
        public int add(String raw) {
            if(raw==null) return 0;
            String value=raw.trim();
            if(value.isEmpty() || value.startsWith("!") || value.startsWith("#") || value.startsWith("[")
                    || value.contains("##") || value.contains("#@#") || value.startsWith("/")) return 0;
            boolean exception=value.startsWith("@@||");
            if(exception) value=value.substring(4);
            else if(value.startsWith("||")) value=value.substring(2);
            else if(value.startsWith("0.0.0.0 ") || value.startsWith("127.0.0.1 ")) {
                String[] parts=value.split("\\s+");
                if(parts.length!=2)return 0;
                value=parts[1];
            } else return 0;
            int caret=value.indexOf('^');
            if(caret>=0) {
                String options=value.substring(caret+1);
                if(!options.isEmpty() && !options.equals("$third-party")) return 0;
                value=value.substring(0,caret);
            }
            String host=domain(value);
            if(host==null || (!exception && blocked.size()>=MAX_RULES))return 0;
            (exception?allowed:blocked).add(host);
            return 1;
        }
        public AdBlockRules build(){return new AdBlockRules(blocked,allowed);}
    }
    public static String domain(String raw) {
        if(raw==null || raw.length()>253 || raw.length()<4)return null;
        String v=raw.toLowerCase(Locale.ROOT);
        if(v.endsWith("."))v=v.substring(0,v.length()-1);
        if(!v.contains(".") || v.startsWith(".") || v.endsWith(".") || v.contains(".."))return null;
        for(int i=0;i<v.length();i++){
            char c=v.charAt(i);
            if(!((c>='a'&&c<='z')||(c>='0'&&c<='9')||c=='-'||c=='.'))return null;
        }
        if(v.startsWith("-")||v.endsWith("-")||v.contains("-.")||v.contains(".-"))return null;
        return v;
    }
    public static String host(String url){
        if(url==null || url.length()>8192)return null;
        try {
            URI u=new URI(url);
            String scheme=u.getScheme();
            if(!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))return null;
            if(u.getRawUserInfo()!=null)return null;
            return domain(u.getHost());
        } catch(Exception ex){return null;}
    }
    private static boolean matches(String host,String domain) {
        return host.equals(domain)||host.endsWith("."+domain);
    }
    private static boolean covered(String host,Set<String> rules) {
        String candidate=host;
        while(candidate!=null){
            if(rules.contains(candidate))return true;
            int dot=candidate.indexOf('.');
            candidate=dot<0?null:candidate.substring(dot+1);
        }
        return false;
    }
    /** Nunca filtrar login, CAPTCHA, URLs principais ou endereços sem HTTP(S). */
    public boolean shouldBlock(String resourceUrl,String pageUrl,int mode,boolean trackersEnabled) {
        if(mode==OFF)return false;
        String resource=host(resourceUrl),page=host(pageUrl);
        if(resource==null || page==null)return false;
        if(matches(page,"accounts.google.com") || loginUrl(pageUrl))return false;
        if(resource.equals(page) || essential(resource))return false;
        if(covered(resource,allowed))return false;
        boolean known=covered(resource,blocked);
        if(known){
            if(!trackersEnabled && isTracker(resource)) return false;
            return mode>=LIGHT && (mode>=STRONG || !isTracker(resource));
        }
        if(mode!=EXTREME)return false;
        // Extra só em recurso de terceiros. Nunca bloquear vídeo nem API pelo nome genérico.
        if(matches(resource,page)||matches(page,resource))return false;
        String path;
        try {path=new URI(resourceUrl).getPath().toLowerCase(Locale.ROOT);}catch(Exception ex){return false;}
        return path.startsWith("/ads/") || path.startsWith("/advertising/")
                || path.endsWith("/prebid.js") || path.endsWith("/ads.js") || path.endsWith("/advertisement.js");
    }
    private static boolean loginUrl(String url){
        try {
            URI u=new URI(url);String path=u.getPath();
            if(path==null)return false;
            String p=path.toLowerCase(Locale.ROOT);
            return p.contains("/login")||p.contains("/signin")||p.contains("/oauth")
                ||p.contains("/checkpoint")||p.contains("/challenge")||p.contains("/captcha")
                ||p.contains("/auth/")||p.contains("/account/verify");
        }catch(Exception ex){return false;}
    }
    private static boolean essential(String host){
        return matches(host,"gstatic.com")||matches(host,"googlevideo.com")
            || matches(host,"recaptcha.net")||matches(host,"hcaptcha.com")
            || matches(host,"fbcdn.net")||matches(host,"cdninstagram.com")
            || matches(host,"googleapis.com")||matches(host,"cloudflare.com")
            || matches(host,"cloudflareinsights.com")
            || matches(host,"stripe.com")||matches(host,"paypal.com")
            || matches(host,"youtube.com")||matches(host,"ytimg.com");
    }
    private static boolean isTracker(String host){
        return matches(host,"google-analytics.com")||matches(host,"googletagmanager.com")
            || matches(host,"connect.facebook.net")||matches(host,"analytics.twitter.com")
            || matches(host,"clarity.ms")||matches(host,"hotjar.com")
            || matches(host,"stats.wp.com")||matches(host,"pixel.wp.com")
            || matches(host,"bat.bing.com");
    }
}
