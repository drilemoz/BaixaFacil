package br.com.baixafacil;

/** Política pura da seleção múltipla: nunca tocar na fila, parciais ou URIs de terceiros. */
public final class DownloadSelectionPolicy {
    private DownloadSelectionPolicy() {}

    public static boolean selectable(String status, String uri) {
        return "Concluído".equals(status) && DownloadCleanupPolicy.deleteFile(status, uri);
    }

    /** Limite conservador para um único compartilhamento Android. */
    public static final int MAX_SHARE_ITEMS = 25;
}
