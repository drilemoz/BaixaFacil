package br.com.baixafacil;

/** Política conservadora: apenas itens terminados, nunca fila, transferências ou fragmentos recuperáveis. */
public final class DownloadCleanupPolicy {
    private DownloadCleanupPolicy() {}

    public static boolean eligible(String status, boolean hasSavedUri) {
        if ("Concluído".equals(status) || "Cancelado".equals(status)) return true;
        return "Falhou".equals(status) && !hasSavedUri;
    }

    public static boolean deleteFile(String status, String uri) {
        if (!"Concluído".equals(status) || uri == null) return false;
        // Só considerar mídia pública criada no MediaStore; jamais caminhos ou URI de terceiros.
        return uri.startsWith("content://media/");
    }
}
