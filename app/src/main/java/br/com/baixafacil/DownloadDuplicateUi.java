package br.com.baixafacil;

import android.app.Activity;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Aviso de duplicata sem revelar a URL ou parâmetros de autenticação. */
final class DownloadDuplicateUi {
    private DownloadDuplicateUi() {}
    static boolean maybeShow(Activity activity, String url, Runnable retry, Runnable openDownloads) {
        List<DownloadDuplicatePolicy.Record> records=new ArrayList<>();
        for(JSONObject entry:DownloadStore.all(activity)) {
            records.add(new DownloadDuplicatePolicy.Record(entry.optString("url"),entry.optString("status"),
                !entry.optString("uri").isEmpty()));
        }
        DownloadDuplicatePolicy.Kind kind=DownloadDuplicatePolicy.find(url,records);
        if(kind==DownloadDuplicatePolicy.Kind.NONE)return false;
        List<AppMenus.Entry> actions=AppMenus.entries();
        if(kind==DownloadDuplicatePolicy.Kind.IN_PROGRESS){
            actions.add(AppMenus.Entry.item("Ver downloads","Consultar o item já existente na fila","folder",openDownloads));
            AppMenus.info(activity,"Link já está na fila",
                "Este endereço exato já possui um download em fila, em andamento ou aguardando retomada. Abra a lista para acompanhar ou retomar o item existente.",actions);
        } else {
            actions.add(AppMenus.Entry.item("Baixar novamente","Iniciar uma nova transferência do mesmo endereço","download",retry));
            actions.add(AppMenus.Entry.item("Ver arquivo existente","Ir até a lista de downloads concluídos","file",openDownloads));
            AppMenus.info(activity,"Arquivo já baixado",
                "Este endereço exato já aparece como concluído. Você pode conservar o arquivo existente ou solicitar outro download. Links temporários podem expirar.",actions);
        }
        return true;
    }
}
