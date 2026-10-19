package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class GeckoCrashPolicyTest {
    @Test public void permitsOnlyTwoAutomaticRestartsWithinWindow() {
        GeckoCrashPolicy.Decision first = GeckoCrashPolicy.afterFailure(5000,0,0);
        assertTrue(first.restartAutomatically);
        GeckoCrashPolicy.Decision second = GeckoCrashPolicy.afterFailure(5010,first.windowStartMs,first.crashes);
        assertTrue(second.restartAutomatically);
        GeckoCrashPolicy.Decision third = GeckoCrashPolicy.afterFailure(5020,second.windowStartMs,second.crashes);
        assertFalse(third.restartAutomatically);
        assertEquals(3,third.crashes);
        GeckoCrashPolicy.Decision fourth = GeckoCrashPolicy.afterFailure(5030,third.windowStartMs,third.crashes);
        assertFalse(fourth.restartAutomatically);
        assertEquals(3,fourth.crashes);
    }
    @Test public void resetsAfterElapsedWindowAndClockReset() {
        assertTrue(GeckoCrashPolicy.afterFailure(130001,1000,3).restartAutomatically);
        assertTrue(GeckoCrashPolicy.afterFailure(10,4000,3).restartAutomatically);
        assertFalse(GeckoCrashPolicy.afterFailure(121000,1000,3).restartAutomatically);
    }
    @Test public void handlesCorruptedCounter() {
        assertTrue(GeckoCrashPolicy.afterFailure(10000,9000,-5).restartAutomatically);
        assertFalse(GeckoCrashPolicy.afterFailure(10000,9000,Integer.MAX_VALUE).restartAutomatically);
    }
}
