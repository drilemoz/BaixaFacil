package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class GeckoProtectionProfileTest {
    @Test public void modesAreNormalized() {
        assertEquals(GeckoProtectionProfile.STRONG,GeckoProtectionProfile.normalize(-1));
        assertEquals(GeckoProtectionProfile.STRONG,GeckoProtectionProfile.normalize(4));
        assertEquals(GeckoProtectionProfile.EXTREME,GeckoProtectionProfile.normalize(3));
    }
    @Test public void consentAndCookiesControlEtp() {
        for(int mode=0;mode<=3;mode++) {
            assertEquals(mode!=0,GeckoProtectionProfile.active(mode,true,false));
            assertFalse(GeckoProtectionProfile.active(mode,false,false));
            assertFalse(GeckoProtectionProfile.active(mode,true,true));
        }
    }
    @Test public void strongAndExtremeDiffer() {
        assertFalse(GeckoProtectionProfile.strict(GeckoProtectionProfile.LIGHT));
        assertTrue(GeckoProtectionProfile.strict(GeckoProtectionProfile.STRONG));
        assertTrue(GeckoProtectionProfile.strict(GeckoProtectionProfile.EXTREME));
        assertFalse(GeckoProtectionProfile.extra(GeckoProtectionProfile.STRONG));
        assertTrue(GeckoProtectionProfile.extra(GeckoProtectionProfile.EXTREME));
    }
}
