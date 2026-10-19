package br.com.baixafacil;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import java.util.ArrayList;

/** Abre ACTION_OPEN_DOCUMENT; não lê arquivos diretamente nem mantém URIs depois do prompt. */
final class GeckoFilePicker {
    static final int REQUEST_CODE=6454;
    private final MainActivity host;
    private GeckoSession.PromptDelegate.FilePrompt current;
    private GeckoResult<GeckoSession.PromptDelegate.PromptResponse> pending;
    GeckoFilePicker(MainActivity activity){host=activity;}

    GeckoResult<GeckoSession.PromptDelegate.PromptResponse> open(GeckoSession.PromptDelegate.FilePrompt prompt){
        if(host.isFinishing() || host.isDestroyed() || pending!=null){
            return GeckoResult.fromValue(prompt.dismiss());
        }
        // A seleção de pastas e captura imediata exigem fluxos separados; falhar explicitamente.
        if(prompt.type==GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER){
            Toast.makeText(host,"Envio de pastas ainda não é compatível",Toast.LENGTH_SHORT).show();
            return GeckoResult.fromValue(prompt.dismiss());
        }
        String[] mime=GeckoPickerPolicy.mimeTypes(prompt.mimeTypes);
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime.length==1?mime[0]:"*/*");
        if(mime.length>1)intent.putExtra(Intent.EXTRA_MIME_TYPES,mime);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,prompt.type==GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result=new GeckoResult<>();
        current=prompt;pending=result;
        try {host.startActivityForResult(intent,REQUEST_CODE);}
        catch(ActivityNotFoundException | SecurityException failure){
            cancel();
            Toast.makeText(host,"Seletor de arquivos indisponível",Toast.LENGTH_LONG).show();
        }
        return result;
    }

    boolean onActivityResult(int request,int resultCode,Intent data){
        if(request!=REQUEST_CODE)return false;
        GeckoSession.PromptDelegate.FilePrompt prompt=current;
        GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result=pending;
        current=null;pending=null;
        if(prompt==null||result==null)return true;
        if(resultCode!=Activity.RESULT_OK || data==null){result.complete(prompt.dismiss());return true;}
        ArrayList<Uri> uris=new ArrayList<>();
        ClipData clip=data.getClipData();
        if(clip!=null){
            for(int i=0;i<clip.getItemCount() && i<32;i++){
                Uri uri=clip.getItemAt(i).getUri();
                if(uri!=null && GeckoPickerPolicy.safeUriScheme(uri.getScheme()) && !uris.contains(uri))uris.add(uri);
            }
        } else if(data.getData()!=null){
            Uri uri=data.getData();
            if(GeckoPickerPolicy.safeUriScheme(uri.getScheme()))uris.add(uri);
        }
        if(uris.isEmpty()){
            result.complete(prompt.dismiss());return true;
        }
        try {
            if(prompt.type==GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE){
                result.complete(prompt.confirm(host.getApplicationContext(),uris.toArray(new Uri[0])));
            } else {
                result.complete(prompt.confirm(host.getApplicationContext(),uris.get(0)));
            }
        }catch(RuntimeException error){
            // Prompt pode ter sido encerrado pelo Gecko durante a atividade externa.
            if(!prompt.isComplete())result.complete(prompt.dismiss());
            else result.completeExceptionally(error);
        }
        return true;
    }
    void cancel(){
        GeckoSession.PromptDelegate.FilePrompt prompt=current;
        GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result=pending;
        current=null;pending=null;
        if(prompt!=null&&result!=null){
            if(!prompt.isComplete())result.complete(prompt.dismiss());
            else result.completeExceptionally(new IllegalStateException("Seleção de arquivos encerrada"));
        }
    }
}
