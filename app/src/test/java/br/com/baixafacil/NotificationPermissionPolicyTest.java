package br.com.baixafacil;
import org.junit.Test;
import static org.junit.Assert.*;
public class NotificationPermissionPolicyTest {
    @Test public void onlyOnceOnAndroid13Plus(){
        assertFalse(NotificationPermissionPolicy.shouldAsk(32,false,false));
        assertFalse(NotificationPermissionPolicy.shouldAsk(33,true,false));
        assertFalse(NotificationPermissionPolicy.shouldAsk(35,false,true));
        assertTrue(NotificationPermissionPolicy.shouldAsk(33,false,false));
        assertTrue(NotificationPermissionPolicy.shouldAsk(35,false,false));
    }
}
