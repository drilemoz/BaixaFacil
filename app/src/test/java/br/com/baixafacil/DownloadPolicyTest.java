package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadPolicyTest {
    @Test public void wifiOnlyWorks() {
        assertNull(DownloadPolicy.restriction(true,true,false,20,true,true,false,false,false,15));
        assertNotNull(DownloadPolicy.restriction(true,true,false,20,true,false,false,true,false,60));
        assertNotNull(DownloadPolicy.restriction(true,true,false,20,true,false,true,false,false,60));
    }
    @Test public void mobileAllowedByDefault() {
        assertNull(DownloadPolicy.restriction(false,true,false,20,true,false,false,true,false,60));
        assertNull(DownloadPolicy.restriction(false,true,false,20,true,false,true,false,false,60));
    }
    @Test public void noMobileStillAllowsEthernetAndWifi() {
        assertNotNull(DownloadPolicy.restriction(false,false,false,20,true,false,false,true,false,60));
        assertNotNull(DownloadPolicy.restriction(false,false,false,20,true,false,false,false,false,60));
        assertNull(DownloadPolicy.restriction(false,false,false,20,true,true,false,false,false,60));
        assertNull(DownloadPolicy.restriction(false,false,false,20,true,false,true,false,false,60));
    }
    @Test public void batteryThresholdAndCharger() {
        assertNotNull(DownloadPolicy.restriction(false,true,true,20,true,true,false,false,false,20));
        assertNull(DownloadPolicy.restriction(false,true,true,20,true,true,false,false,true,10));
        assertNull(DownloadPolicy.restriction(false,true,true,20,true,true,false,false,false,21));
        assertNull(DownloadPolicy.restriction(false,true,true,20,true,true,false,false,false,-1));
        assertNull(DownloadPolicy.restriction(false,true,false,20,true,true,false,false,false,10));
    }
    @Test public void offlineAlwaysBlocked() {
        assertNotNull(DownloadPolicy.restriction(false,true,false,20,false,false,false,false,false,100));
    }
}
