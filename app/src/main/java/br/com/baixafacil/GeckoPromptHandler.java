package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

/** Suporte básico e explícito para caixas JavaScript e menus HTML do Gecko. */
final class GeckoPromptHandler implements GeckoSession.PromptDelegate {
    private final Activity host;
    GeckoPromptHandler(Activity host){this.host=host;}

    private boolean available(){return !host.isFinishing()&&!host.isDestroyed();}
    private String title(String value){return value==null || value.isEmpty()?"Mensagem do site":value;}
    @Override public GeckoResult<PromptResponse> onAlertPrompt(GeckoSession session,AlertPrompt prompt){
        if(!available())return GeckoResult.fromValue(prompt.dismiss());
        GeckoResult<PromptResponse> result=new GeckoResult<>();
        AlertDialog dialog=new AlertDialog.Builder(host).setTitle(title(prompt.title)).setMessage(prompt.message)
            .setPositiveButton("OK",(d,w)->result.complete(prompt.dismiss()))
            .setOnCancelListener(d->result.complete(prompt.dismiss())).create();
        dialog.show();return result;
    }
    @Override public GeckoResult<PromptResponse> onButtonPrompt(GeckoSession session,ButtonPrompt prompt){
        if(!available())return GeckoResult.fromValue(prompt.dismiss());
        GeckoResult<PromptResponse> result=new GeckoResult<>();
        AlertDialog dialog=new AlertDialog.Builder(host).setTitle(title(prompt.title)).setMessage(prompt.message)
            .setPositiveButton("OK",(d,w)->result.complete(prompt.confirm(ButtonPrompt.Type.POSITIVE)))
            .setNegativeButton("Cancelar",(d,w)->result.complete(prompt.confirm(ButtonPrompt.Type.NEGATIVE)))
            .setOnCancelListener(d->result.complete(prompt.dismiss())).create();
        dialog.show();return result;
    }
    @Override public GeckoResult<PromptResponse> onTextPrompt(GeckoSession session,TextPrompt prompt){
        if(!available())return GeckoResult.fromValue(prompt.dismiss());
        GeckoResult<PromptResponse> result=new GeckoResult<>();
        EditText input=new EditText(host);
        input.setSingleLine(true);
        input.setText(prompt.defaultValue==null?"":prompt.defaultValue);
        input.setPadding(26,12,26,12);
        AlertDialog dialog=new AlertDialog.Builder(host).setTitle(title(prompt.title)).setMessage(prompt.message)
            .setView(input)
            .setPositiveButton("OK",(d,w)->result.complete(prompt.confirm(input.getText().toString())))
            .setNegativeButton("Cancelar",(d,w)->result.complete(prompt.dismiss()))
            .setOnCancelListener(d->result.complete(prompt.dismiss())).create();
        dialog.show();return result;
    }
    @Override public GeckoResult<PromptResponse> onChoicePrompt(GeckoSession session,ChoicePrompt prompt){
        if(!available())return GeckoResult.fromValue(prompt.dismiss());
        // Menus simples e listas múltiplas; grupos hierárquicos são recusados.
        ChoicePrompt.Choice[] choices=prompt.choices;
        if(choices==null||choices.length==0)return GeckoResult.fromValue(prompt.dismiss());
        GeckoResult<PromptResponse> result=new GeckoResult<>();
        String[] labels=new String[choices.length];
        for(int i=0;i<choices.length;i++) labels[i]=choices[i].label;
        for(ChoicePrompt.Choice choice:choices)
            if(choice.items!=null)return GeckoResult.fromValue(prompt.dismiss());
        if(prompt.type==ChoicePrompt.Type.MULTIPLE){
            boolean[] selected=new boolean[choices.length];
            for(int i=0;i<choices.length;i++)selected[i]=choices[i].selected;
            AlertDialog multi=new AlertDialog.Builder(host).setTitle(title(prompt.title))
                .setMultiChoiceItems(labels,selected,(dialog,index,checked)->{
                    if(!choices[index].disabled)selected[index]=checked;
                })
                .setPositiveButton("OK",(dialog,which)->{
                    java.util.ArrayList<ChoicePrompt.Choice> chosen=new java.util.ArrayList<>();
                    for(int i=0;i<choices.length;i++)if(selected[i]&&!choices[i].disabled)chosen.add(choices[i]);
                    result.complete(prompt.confirm(chosen.toArray(new ChoicePrompt.Choice[0])));
                })
                .setNegativeButton("Cancelar",(dialog,which)->result.complete(prompt.dismiss()))
                .setOnCancelListener(d->result.complete(prompt.dismiss())).create();
            multi.show();return result;
        }
        AlertDialog dialog=new AlertDialog.Builder(host).setTitle(title(prompt.title))
            .setItems(labels,(d,index)->{
                if(choices[index].disabled||choices[index].items!=null)result.complete(prompt.dismiss());
                else result.complete(prompt.confirm(choices[index]));
            })
            .setNegativeButton("Cancelar",(d,w)->result.complete(prompt.dismiss()))
            .setOnCancelListener(d->result.complete(prompt.dismiss())).create();
        dialog.show();return result;
    }
}
