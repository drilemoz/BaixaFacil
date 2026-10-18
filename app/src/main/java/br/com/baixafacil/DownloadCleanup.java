package br.com.baixafacil;

import android.content.Context;
import android.net.Uri;
import org.json.JSONObject;

/** Execução de limpeza fora da UI. Nunca toca em fila, jobs, pausados ou arquivos parciais. */
final class DownloadCleanup {
    private DownloadCleanup() {}
    static final class Report {
        final int removed, deletedFiles, failedFiles, preserved;
        Report(int removed,int deletedFiles,int failedFiles,int preserved){
            this.removed=removed;this.deletedFiles=deletedFiles;this.failedFiles=failedFiles;this.preserved=preserved;
        }
    }
    /** Exclusão específica dos IDs confirmados, exclusivamente arquivos concluídos do MediaStore. */
    static Report runSelected(Context context, java.util.Set<String> ids, boolean deleteFiles) {
        if(ids==null || ids.isEmpty())return new Report(0,0,0,0);
        int removed=0, deleted=0, failed=0, preserved=0;
        for(JSONObject row:DownloadStore.all(context)) {
            String id=row.optString("id");
            if(!ids.contains(id))continue;
            String uri=row.optString("uri");
            JSONObject current=DownloadStore.get(context,id);
            if(current==null || !uri.equals(current.optString("uri"))
                    || !DownloadSelectionPolicy.selectable(current.optString("status"),uri)) {
                preserved++;continue;
            }
            if(deleteFiles) {
                try {
                    if(context.getContentResolver().delete(Uri.parse(uri),null,null)!=1) {
                        failed++;continue;
                    }
                    deleted++;
                }catch(RuntimeException error){
                    AppEventLog.error(context,"Exclusão múltipla",error);
                    failed++;continue;
                }
            }
            if(DownloadStore.removeIfCompleted(context,id,uri)) removed++;
            else preserved++;
        }
        return new Report(removed,deleted,failed,preserved);
    }

    static Report run(Context context, boolean deleteFiles) {
        int removed=0, deleted=0, failed=0, preserved=0;
        for (JSONObject row : DownloadStore.all(context)) {
            String id=row.optString("id"),status=row.optString("status");
            String uri=row.optString("uri");
            if (!DownloadCleanupPolicy.eligible(status,!uri.isEmpty())) {preserved++;continue;}
            // Confirma o estado atual antes de qualquer ação; nunca excluir de filas ativas.
            JSONObject current=DownloadStore.get(context,id);
            if(current==null || !DownloadCleanupPolicy.eligible(current.optString("status"),
                    !current.optString("uri").isEmpty())) {preserved++;continue;}
            if (deleteFiles && "Concluído".equals(status)) {
                if(!DownloadCleanupPolicy.deleteFile(status,uri)) {failed++;continue;}
                try {
                    if(context.getContentResolver().delete(Uri.parse(uri),null,null)!=1) {
                        failed++;continue;
                    }
                    deleted++;
                } catch (RuntimeException error) {
                    AppEventLog.error(context,"Limpeza de downloads",error);
                    failed++;continue;
                }
            }
            if (DownloadStore.removeIfFinished(context,id)) removed++;
            else preserved++;
        }
        return new Report(removed,deleted,failed,preserved);
    }
}
