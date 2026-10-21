package br.com.baixafacil;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.mozilla.geckoview.Autocomplete;
import org.mozilla.geckoview.GeckoResult;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Cofre local criptografado AES-256-GCM. Senhas nunca entram nos logs nem no backup Android. */
final class SecureLoginStore implements Autocomplete.StorageDelegate {
    private static final String STORE="bf_login_v1";
    private static final String ALIAS="baixafacil_logins_aes_v1";
    private final Context context;
    SecureLoginStore(Context c){context=c.getApplicationContext();}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(STORE,Context.MODE_PRIVATE);}
    static String secureOrigin(String url){
        try{
            URI uri=new URI(url);String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||host.trim().isEmpty()||uri.getRawUserInfo()!=null)return null;
            int port=uri.getPort();
            return "https://"+host.toLowerCase(java.util.Locale.ROOT)+(port>0 && port!=443?":"+port:"");
        }catch(Exception ignored){return null;}
    }
    private static SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
    private static String encrypt(String plain) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] nonce=cipher.getIV();byte[] payload=cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(nonce,Base64.NO_WRAP)+":"+Base64.encodeToString(payload,Base64.NO_WRAP);
    }
    private static String decrypt(String stored) throws Exception {
        String[] parts=stored.split(":",2);if(parts.length!=2)throw new IllegalArgumentException();
        byte[] nonce=Base64.decode(parts[0],Base64.DEFAULT);
        if(nonce.length!=12)throw new IllegalArgumentException();
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,nonce));
        return new String(cipher.doFinal(Base64.decode(parts[1],Base64.DEFAULT)),StandardCharsets.UTF_8);
    }
    private static JSONObject json(Autocomplete.LoginEntry value) throws Exception {
        JSONObject j=new JSONObject();
        j.put("id",value.guid!=null?value.guid:UUID.randomUUID().toString());
        j.put("o",value.origin);j.put("a",value.formActionOrigin==null?"":value.formActionOrigin);
        j.put("r",value.httpRealm==null?"":value.httpRealm);
        j.put("u",value.username);j.put("p",value.password);
        return j;
    }
    /** Nunca converter erro de descriptografia numa lista vazia gravável. */
    static synchronized boolean readable(Context c){
        String encrypted=prefs(c).getString("vault",null);
        if(encrypted==null)return true;
        try{
            JSONArray entries=new JSONArray(decrypt(encrypted));
            for(int i=0;i<entries.length();i++){
                JSONObject item=entries.optJSONObject(i);
                if(item==null || secureOrigin(item.optString("o"))==null ||
                   !item.has("u") || !item.has("p") || item.optString("p").isEmpty())return false;
            }
            return true;
        }catch(Exception ignored){return false;}
    }
    static synchronized List<Autocomplete.LoginEntry> load(Context c){
        List<Autocomplete.LoginEntry> list=new ArrayList<>();
        String encrypted=prefs(c).getString("vault",null);if(encrypted==null)return list;
        try{
            JSONArray json=new JSONArray(decrypt(encrypted));
            for(int i=0;i<json.length() && list.size()<250;i++){
                JSONObject j=json.optJSONObject(i);if(j==null)continue;
                String origin=secureOrigin(j.optString("o"));
                String username=j.optString("u"),password=j.optString("p");
                if(origin==null || username.length()>512 || password.isEmpty() || password.length()>4096)continue;
                Autocomplete.LoginEntry.Builder b=new Autocomplete.LoginEntry.Builder()
                    .guid(j.optString("id")).origin(origin).username(username).password(password);
                if(!j.optString("a").isEmpty())b.formActionOrigin(j.optString("a"));
                if(!j.optString("r").isEmpty())b.httpRealm(j.optString("r"));
                list.add(b.build());
            }
        }catch(Exception ignored){/* Se o cofre ficar inacessível, nunca sobrescrever a cópia anterior. */}
        return list;
    }
    private static synchronized boolean write(Context c,List<Autocomplete.LoginEntry> list){
        if(list.size()>250)return false;
        try{
            JSONArray json=new JSONArray();for(Autocomplete.LoginEntry e:list)json.put(json(e));
            return prefs(c).edit().putString("vault",encrypt(json.toString())).commit();
        }catch(Exception ignored){return false;}
    }
    static synchronized boolean save(Context c,Autocomplete.LoginEntry entry){
        // Nunca sobrescrever dados cifrados caso o Keystore tenha sido invalidado.
        String existing=prefs(c).getString("vault",null);
        if(existing!=null){try{new JSONArray(decrypt(existing));}catch(Exception error){return false;}}
        if(entry==null)return false;
        String origin=secureOrigin(entry.origin);
        String username=entry.username==null?"":entry.username;
        if(origin==null || entry.password==null || entry.password.isEmpty() || entry.password.length()>4096 || username.length()>512)return false;
        List<Autocomplete.LoginEntry> list=load(c);
        for(int i=list.size()-1;i>=0;i--){
            Autocomplete.LoginEntry before=list.get(i);
            if((entry.guid!=null && entry.guid.equals(before.guid)) ||
               (before.origin.equals(origin)&&before.username.equals(username)))list.remove(i);
        }
        String guid=entry.guid!=null&&!entry.guid.trim().isEmpty()?entry.guid:UUID.randomUUID().toString();
        Autocomplete.LoginEntry.Builder b=new Autocomplete.LoginEntry.Builder().guid(guid).origin(origin)
            .username(username).password(entry.password);
        if(entry.formActionOrigin!=null)b.formActionOrigin(entry.formActionOrigin);
        if(entry.httpRealm!=null)b.httpRealm(entry.httpRealm);
        list.add(b.build());return write(c,list);
    }
    static synchronized boolean delete(Context c,String guid){
        if(guid==null || guid.isEmpty() || !readable(c))return false;
        List<Autocomplete.LoginEntry> list=load(c);
        boolean removed=list.removeIf(x->guid.equals(x.guid));
        return removed && write(c,list);
    }
    static synchronized boolean deleteAll(Context c){
        if(!readable(c))return false;
        // O cofre é esvaziado, mas a chave permanece para novos logins.
        return write(c,new ArrayList<>());
    }
    @Override public GeckoResult<Autocomplete.LoginEntry[]> onLoginFetch(String domain){
        String requested=secureOrigin(domain);
        if(requested==null && domain!=null)requested=secureOrigin("https://"+domain);
        List<Autocomplete.LoginEntry> result=new ArrayList<>();
        if(requested!=null)for(Autocomplete.LoginEntry login:load(context))if(requested.equals(login.origin))result.add(login);
        return GeckoResult.fromValue(result.toArray(new Autocomplete.LoginEntry[0]));
    }
    @Override public GeckoResult<Autocomplete.LoginEntry[]> onLoginFetch(){
        return GeckoResult.fromValue(load(context).toArray(new Autocomplete.LoginEntry[0]));
    }
    @Override public void onLoginSave(Autocomplete.LoginEntry login){save(context,login);}
    @Override public void onLoginUsed(Autocomplete.LoginEntry login,int usedFields){}
}
