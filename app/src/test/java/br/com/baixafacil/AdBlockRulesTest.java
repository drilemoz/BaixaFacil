package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdBlockRulesTest {
    private final String page="https://example.com/reading";
    private AdBlockRules build(){
        AdBlockRules.Builder b=new AdBlockRules.Builder();
        b.add("||doubleclick.net^");
        b.add("||tracker.example.net^$third-party");
        b.add("@@||safe.tracker.example.net^");
        b.add("0.0.0.0 ads.example.org");
        return b.build();
    }
    @Test public void blockSubdomainsOnlyAndNeverLookalikes(){
        AdBlockRules r=build();
        assertTrue(r.shouldBlock("https://ads.doubleclick.net/asset.js",page,AdBlockRules.STRONG,true));
        assertFalse(r.shouldBlock("https://notdoubleclick.net/asset.js",page,AdBlockRules.STRONG,true));
        assertTrue(r.shouldBlock("https://ads.example.org/ad.js",page,AdBlockRules.STRONG,true));
    }
    @Test public void allowlistAndFirstPartyBypass(){
        AdBlockRules r=build();
        assertFalse(r.shouldBlock("https://safe.tracker.example.net/a",page,AdBlockRules.STRONG,true));
        assertFalse(r.shouldBlock("https://doubleclick.net/page","https://doubleclick.net/page",AdBlockRules.EXTREME,true));
    }
    @Test public void oauthAndCheckpointNotFiltered(){
        AdBlockRules r=build();
        assertFalse(r.shouldBlock("https://doubleclick.net/a","https://example.com/login?next=1",AdBlockRules.STRONG,true));
        assertFalse(r.shouldBlock("https://doubleclick.net/a","https://www.facebook.com/checkpoint/",AdBlockRules.STRONG,true));
        assertFalse(r.shouldBlock("https://doubleclick.net/a","https://accounts.google.com/v3/signin",AdBlockRules.EXTREME,true));
    }
    @Test public void extremeIsThirdPartyAndOptIn(){
        AdBlockRules r=build();
        String ad="https://remote.net/advertising/script.js";
        assertFalse(r.shouldBlock(ad,page,AdBlockRules.STRONG,true));
        assertTrue(r.shouldBlock(ad,page,AdBlockRules.EXTREME,true));
        assertFalse(r.shouldBlock("https://example.com/ads/banner.js",page,AdBlockRules.EXTREME,true));
    }
    @Test public void parserSkipsComplexRules(){
        AdBlockRules.Builder b=new AdBlockRules.Builder();
        assertEquals(0,b.add("/evil.*/"));
        assertEquals(0,b.add("##.ad"));
        assertEquals(0,b.add("||example.com^$script,third-party"));
        assertEquals(0,b.add("||evil*.com^"));
        assertEquals(1,b.add("||metrics.example.net^"));
        assertEquals(1,b.build().count());
    }
    @Test public void blocksNoNonWebUrlsAndProtectionModes(){
        AdBlockRules r=build();
        assertFalse(r.shouldBlock("blob:https://doubleclick.net/foo",page,AdBlockRules.EXTREME,true));
        assertFalse(r.shouldBlock("https://doubleclick.net/a","file:///local",AdBlockRules.EXTREME,true));
        assertFalse(r.shouldBlock("https://doubleclick.net/a",page,AdBlockRules.OFF,true));
    }
    @Test public void videoAndCaptchaDomainsKeepWorking(){
        AdBlockRules.Builder b=new AdBlockRules.Builder();
        b.add("||googlevideo.com^");b.add("||recaptcha.net^");
        AdBlockRules r=b.build();
        assertFalse(r.shouldBlock("https://rr1.googlevideo.com/video",page,AdBlockRules.EXTREME,true));
        assertFalse(r.shouldBlock("https://api.recaptcha.net/x",page,AdBlockRules.STRONG,true));
    }
}
