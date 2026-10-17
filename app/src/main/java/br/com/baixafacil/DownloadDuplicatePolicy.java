package br.com.baixafacil;

import java.util.List;

/** Conservador: somente URLs exatamente iguais são possíveis downloads repetidos.
 * Nunca elimina parâmetros assinados nem assume identidade por domínio/nome.
 */
public final class DownloadDuplicatePolicy {
    private DownloadDuplicatePolicy() {}

    public enum Kind { NONE, IN_PROGRESS, COMPLETED }
    public static final class Record {
        public final String url, status;
        public final boolean hasLocalFile;
        public Record(String url, String status, boolean hasLocalFile) {
            this.url=url; this.status=status; this.hasLocalFile=hasLocalFile;
        }
    }
    public static Kind find(String url, List<Record> records) {
        if (url==null || url.isEmpty() || records==null) return Kind.NONE;
        boolean completed=false;
        for (Record r:records) {
            if(r==null || !url.equals(r.url))continue;
            String status=r.status==null?"":r.status;
            if("Na fila".equals(status)||"Baixando".equals(status)||"Aguardando".equals(status)
                    ||"Preparando".equals(status)||"Pausado".equals(status)
                    ||"Interrompido".equals(status))return Kind.IN_PROGRESS;
            if("Concluído".equals(status)&&r.hasLocalFile)completed=true;
        }
        return completed?Kind.COMPLETED:Kind.NONE;
    }
}
