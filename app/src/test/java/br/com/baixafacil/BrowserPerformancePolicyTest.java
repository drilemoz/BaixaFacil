package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class BrowserPerformancePolicyTest {
    @Test public void activeRegularPageMayScan(){assertTrue(BrowserPerformancePolicy.allowAutomaticMedia(true,false,false,false));}
    @Test public void backgroundNeverScans(){assertFalse(BrowserPerformancePolicy.allowAutomaticMedia(false,false,false,false));}
    @Test public void lightweightNeverScans(){assertFalse(BrowserPerformancePolicy.allowAutomaticMedia(true,true,false,false));}
    @Test public void pressureNeverScans(){assertFalse(BrowserPerformancePolicy.allowAutomaticMedia(true,false,true,false));}
    @Test public void hungRendererNeverScans(){assertFalse(BrowserPerformancePolicy.allowAutomaticMedia(true,false,false,true));}
    @Test public void firstScanUsesRequestedDelay(){assertEquals(900,BrowserPerformancePolicy.scheduleDelay(900,5000,0));}
    @Test public void debouncedScanRespectsMinimumInterval(){assertEquals(2500,BrowserPerformancePolicy.scheduleDelay(900,5000,4000));}
    @Test public void oldScanUsesRequestedDelay(){assertEquals(900,BrowserPerformancePolicy.scheduleDelay(900,9000,4000));}
    @Test public void backwardsClockDoesNotDelay(){assertEquals(950,BrowserPerformancePolicy.scheduleDelay(950,5000,6000));}
    @Test public void neverUseNegativeDelay(){assertEquals(0,BrowserPerformancePolicy.scheduleDelay(-4,5000,0));}
    @Test public void healthySystemAllowsWork(){assertTrue(BrowserPerformancePolicy.canResumeOptionalWork(false,512L<<20,256L<<20));}
    @Test public void lowMemoryBlocksWork(){assertFalse(BrowserPerformancePolicy.canResumeOptionalWork(true,512L<<20,256L<<20));}
    @Test public void closeToThresholdBlocksWork(){assertFalse(BrowserPerformancePolicy.canResumeOptionalWork(false,330L<<20,256L<<20));}
    @Test public void unknownMemoryBlocksWork(){assertFalse(BrowserPerformancePolicy.canResumeOptionalWork(false,-1,256L<<20));}
    @Test public void uiHiddenAloneIsNotPressure(){assertFalse(BrowserPerformancePolicy.seriousMemoryPressure(20));}
    @Test public void runningLowIsPressure(){assertTrue(BrowserPerformancePolicy.seriousMemoryPressure(10));}
    @Test public void runningCriticalIsPressure(){assertTrue(BrowserPerformancePolicy.seriousMemoryPressure(15));}
    @Test public void backgroundModerateIsPressure(){assertTrue(BrowserPerformancePolicy.seriousMemoryPressure(60));}
}
