package br.com.baixafacil;

/**
 * Destino dos controles principais, sem dependências do Android.
 * A Home sempre oferece uma saída da tela Downloads sem descarregar a WebView.
 */
final class MainNavigationPolicy {
    static final int BACK=0;
    static final int FORWARD=1;
    static final int RELOAD=2;
    static final int HOME=3;
    static final int DOWNLOADS=4;

    enum Command {
        SHOW_BROWSER, SHOW_DOWNLOADS, GO_BACK, GO_FORWARD, RELOAD_PAGE, LOAD_HOME,
        CLOSE_SELECTION, EXIT_APP, NONE
    }

    private MainNavigationPolicy() {}

    static Command bottomAction(boolean downloadsVisible, int index){
        if(index==DOWNLOADS) return downloadsVisible ? Command.SHOW_BROWSER : Command.SHOW_DOWNLOADS;
        if(downloadsVisible){
            if(index==BACK || index==HOME) return Command.SHOW_BROWSER;
            return Command.NONE;
        }
        switch(index){
            case BACK: return Command.GO_BACK;
            case FORWARD: return Command.GO_FORWARD;
            case RELOAD: return Command.RELOAD_PAGE;
            case HOME: return Command.LOAD_HOME;
            default: return Command.NONE;
        }
    }

    static Command systemBack(boolean downloadsVisible, boolean selectingDownloads, boolean browserCanGoBack){
        if(downloadsVisible) return selectingDownloads ? Command.CLOSE_SELECTION : Command.SHOW_BROWSER;
        return browserCanGoBack ? Command.GO_BACK : Command.EXIT_APP;
    }
}
