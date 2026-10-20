package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.Autocomplete;
import org.mozilla.geckoview.GeckoSession;

/** Suporte básico e explícito para caixas JavaScript e menus HTML do Gecko. */
final class GeckoPromptHandler implements GeckoSession.PromptDelegate {
    private final Activity host;
    private final GeckoFilePicker picker;
    GeckoPromptHandler(Activity host,GeckoFilePicker picker){this.host=host;this.picker=picker;}
    @Override public GeckoResult<PromptResponse> onFilePrompt(GeckoSession session,FilePrompt prompt){
        return picker.open(prompt);
    }

    @Override public GeckoResult<PromptResponse> onLoginSave(GeckoSession session,
            AutocompleteRequest<Autocomplete.LoginSaveOption> request){
        if(!available() || request.options==null || request.options.length==0)
            return GeckoResult.fromValue(request.dismiss());
        Autocomplete.LoginSaveOption entry=request.options[0];
        if(SecureLoginStore.secureOrigin(entry.value.origin)==null)
            return GeckoResult.fromValue(request.dismiss());
        GeckoResult<PromptResponse> result=new GeckoResult<>();
        Ui.Palette p=new Ui.Palette(host);
        final android.widget.LinearLayout root=Ui.vertical(host);
        root.setPadding(Ui.dp(host,18),Ui.dp(host,16),Ui.dp(host,18),Ui.dp(host,14));
        root.setBackground(Ui.shape(host,p.surface,24));
        root.addView(Ui.text(host,"Salvar senha?",19,p.text,true));
        root.addView(Ui.text(host,"Site: "+entry.value.origin+"\nUsuário: "+entry.value.username+"\nA senha será cifrada no aparelho.",13,p.muted,false),Ui.lp(host,-1,-2,10));
        AlertDialog dialog=new AlertDialog.Builder(host).setView(root)
            .setPositiveButton("Salvar",(d,w)->result.complete(request.confirm(entry)))
            .setNegativeButton("Agora não",(d,w)->result.complete(request.dismiss()))
            .setOnCancelListener(d->result.complete(request.dismiss())).create();
        dialog.show();dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        return result;
    }
    @Override public GeckoResult<PromptResponse> onLoginSelect(GeckoSession session,
            AutocompleteRequest<Autocomplete.LoginSelectOption> request){
        if(!available()||request.options==null||request.options.length==0)
            return GeckoResult.fromValue(request.dismiss());
        GeckoResult<PromptResponse> result=new GeckoResult<>();
        String[] labels=new String[request.options.length];
        for(int i=0;i<labels.length;i++){
            Autocomplete.LoginEntry e=request.options[i].value;
            labels[i]=e.username+"  •  "+e.origin;
        }
        AlertDialog dialog=new AlertDialog.Builder(host).setTitle("Preencher login salvo")
            .setItems(labels,(d,i)->result.complete(request.confirm(request.options[i])))
            .setNegativeButton("Cancelar",(d,w)->result.complete(request.dismiss()))
            .setOnCancelListener(d->result.complete(request.dismiss())).create();
        dialog.show();return result;
    }
    private java.util.List<ChoicePrompt.Choice> flatten(ChoicePrompt.Choice[] options){
        java.util.List<ChoicePrompt.Choice> items=new java.util.ArrayList<>();
        if(options==null)return items;
        for(ChoicePrompt.Choice choice:options){
            if(choice.separator)continue;
            if(choice.items!=null){items.addAll(flatten(choice.items));}
            else items.add(choice);
        }
        return items;
    }
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
        ChoicePrompt.Choice[] choices=flatten(prompt.choices).toArray(new ChoicePrompt.Choice[0]);
        if(choices.length==0)return GeckoResult.fromValue(prompt.dismiss());
        GeckoResult<PromptResponse> result=new GeckoResult<>();
        String[] labels=new String[choices.length];
        for(int i=0;i<choices.length;i++)labels[i]=choices[i].label+(choices[i].disabled?" (indisponível)":"");
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
