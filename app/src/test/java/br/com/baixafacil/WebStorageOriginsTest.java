package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Regressão do erro de javac do workflow Gerar APK 10: callback usa Map bruto. */
public class WebStorageOriginsTest {
    @Test public void acceptsValidHttpAndHttpsOrigins() {
        Map<Object,Object> raw=new LinkedHashMap<>();
        raw.put("https://chatgpt.com",new Stored(7));
        raw.put("http://example.org:8080",new Stored(2));
        List<Map.Entry<String,Stored>> result=WebStorageOrigins.checked(raw,Stored.class);
        assertEquals(2,result.size());
        assertEquals("https://chatgpt.com",result.get(0).getKey());
        assertEquals(7,result.get(0).getValue().size);
    }
    @Test public void ignoresWrongKeyAndWrongValueTypes() {
        Map<Object,Object> raw=new LinkedHashMap<>();
        raw.put(17,new Stored(1));
        raw.put("https://example.org","invalid");
        raw.put("https://good.org",new Stored(2));
        assertEquals(1,WebStorageOrigins.checked(raw,Stored.class).size());
    }
    @Test public void rejectsNonOriginPathsQueriesAndCredentials() {
        Map<Object,Object> raw=new LinkedHashMap<>();
        for(String url:new String[]{"file:///tmp/data","https://user:pass@host.test",
            "https://host.test/path","https://host.test/?token=abc",
            "https://host.test/#fragment","javascript:alert(1)"})raw.put(url,new Stored(1));
        assertTrue(WebStorageOrigins.checked(raw,Stored.class).isEmpty());
    }
    @Test public void rejectsNullAndMalformedEntries() {
        assertTrue(WebStorageOrigins.checked(null,Stored.class).isEmpty());
        Map<Object,Object> raw=new LinkedHashMap<>();
        raw.put("https://valid.test",null);
        raw.put("not a uri",new Stored(1));
        assertTrue(WebStorageOrigins.checked(raw,Stored.class).isEmpty());
        assertTrue(WebStorageOrigins.checked(raw,null).isEmpty());
    }
    @Test public void acceptsRawMapFromAndroidCallbackWithoutGenericCast() {
        Map raw=new LinkedHashMap();
        raw.put("https://site.test",new Stored(3));
        List<Map.Entry<String,Stored>> typed=WebStorageOrigins.checked(raw,Stored.class);
        assertEquals(1,typed.size());
    }
    private static final class Stored {final int size;Stored(int size){this.size=size;}}
}
