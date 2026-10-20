package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Consentimento contextual com o mesmo tema do app; não substitui permissões Android do sistema. */
final class SitePermissionDialog {
    private SitePermissionDialog(){}
    static AlertDialog show(Activity host,String title,String domain,String explanation,
                            Runnable allow,Runnable deny,Runnable cleanup){
        Ui.Palette p=new Ui.Palette(host);
        LinearLayout root=Ui.vertical(host);
        root.setPadding(Ui.dp(host,22),Ui.dp(host,21),Ui.dp(host,22),Ui.dp(host,17));
        root.setBackground(Ui.shape(host,p.surface,25));
        root.addView(Ui.text(host,title,19,p.text,true));
        root.addView(Ui.text(host,domain,15,p.teal,true),Ui.lp(host,-1,-2,10));
        TextView message=Ui.text(host,explanation,13,p.muted,false);
        root.addView(message,Ui.lp(host,-1,-2,10));
        LinearLayout buttons=Ui.horizontal(host);
        TextView block=Ui.pill(host,"Bloquear",p.text,p.soft);
        TextView permit=Ui.pill(host,"Permitir",p.surface,p.teal);
        buttons.addView(block,new LinearLayout.LayoutParams(0,Ui.dp(host,45),1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(host,45),1);lp.leftMargin=Ui.dp(host,10);
        buttons.addView(permit,lp);
        root.addView(buttons,Ui.lp(host,-1,-2,20));
        AlertDialog dialog=new AlertDialog.Builder(host).setView(root).create();
        final boolean[] chosen={false};
        block.setOnClickListener(v->{chosen[0]=true;dialog.dismiss();deny.run();});
        permit.setOnClickListener(v->{chosen[0]=true;dialog.dismiss();allow.run();});
        dialog.setOnDismissListener(v->{if(!chosen[0])deny.run();if(cleanup!=null)cleanup.run();});
        dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(host.getResources().getDisplayMetrics().widthPixels*90/100,ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }
}
