package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class MainNavigationPolicyTest {
    @Test public void homeInDownloadsReturnsToBrowserWithoutReload(){
        assertEquals(MainNavigationPolicy.Command.SHOW_BROWSER,
            MainNavigationPolicy.bottomAction(true,MainNavigationPolicy.HOME));
    }
    @Test public void backInDownloadsAlsoReturnsToBrowser(){
        assertEquals(MainNavigationPolicy.Command.SHOW_BROWSER,
            MainNavigationPolicy.bottomAction(true,MainNavigationPolicy.BACK));
    }
    @Test public void downloadTabAlwaysAlternates(){
        assertEquals(MainNavigationPolicy.Command.SHOW_DOWNLOADS,
            MainNavigationPolicy.bottomAction(false,MainNavigationPolicy.DOWNLOADS));
        assertEquals(MainNavigationPolicy.Command.SHOW_BROWSER,
            MainNavigationPolicy.bottomAction(true,MainNavigationPolicy.DOWNLOADS));
    }
    @Test public void homeInBrowserStillOpensConfiguredHome(){
        assertEquals(MainNavigationPolicy.Command.LOAD_HOME,
            MainNavigationPolicy.bottomAction(false,MainNavigationPolicy.HOME));
    }
    @Test public void irrelevantPageActionsStayInactiveInDownloads(){
        assertEquals(MainNavigationPolicy.Command.NONE,
            MainNavigationPolicy.bottomAction(true,MainNavigationPolicy.FORWARD));
        assertEquals(MainNavigationPolicy.Command.NONE,
            MainNavigationPolicy.bottomAction(true,MainNavigationPolicy.RELOAD));
    }
    @Test public void browserPageActionsRemainUnchanged(){
        assertEquals(MainNavigationPolicy.Command.GO_BACK,
            MainNavigationPolicy.bottomAction(false,MainNavigationPolicy.BACK));
        assertEquals(MainNavigationPolicy.Command.GO_FORWARD,
            MainNavigationPolicy.bottomAction(false,MainNavigationPolicy.FORWARD));
        assertEquals(MainNavigationPolicy.Command.RELOAD_PAGE,
            MainNavigationPolicy.bottomAction(false,MainNavigationPolicy.RELOAD));
    }
    @Test public void systemBackFirstLeavesSelectionThenDownloads(){
        assertEquals(MainNavigationPolicy.Command.CLOSE_SELECTION,
            MainNavigationPolicy.systemBack(true,true,false));
        assertEquals(MainNavigationPolicy.Command.SHOW_BROWSER,
            MainNavigationPolicy.systemBack(true,false,false));
    }
    @Test public void systemBackBrowserUsesHistoryOrExits(){
        assertEquals(MainNavigationPolicy.Command.GO_BACK,
            MainNavigationPolicy.systemBack(false,false,true));
        assertEquals(MainNavigationPolicy.Command.EXIT_APP,
            MainNavigationPolicy.systemBack(false,false,false));
    }
    @Test public void unknownBottomButtonsDoNothing(){
        assertEquals(MainNavigationPolicy.Command.NONE,
            MainNavigationPolicy.bottomAction(true,-1));
        assertEquals(MainNavigationPolicy.Command.NONE,
            MainNavigationPolicy.bottomAction(false,88));
    }
}
