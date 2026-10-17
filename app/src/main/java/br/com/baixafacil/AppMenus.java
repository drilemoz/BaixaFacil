package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.ArrayList;

/** Um único estilo de menu para navegador, vídeos, favoritos e configurações.
 * Sem bibliotecas de UI, anúncios, coleta de dados ou alterações na WebView.
 */
final class AppMenus {
    private AppMenus() {}
    static final class Entry {
        final String title, description, icon;
        final Runnable onClick;
        final boolean section, danger;
        Entry(String t,String d,String i,Runnable r,boolean s,boolean red){title=t;description=d;icon=i;onClick=r;section=s;danger=red;}
        static Entry item(String t,String d,String i,Runnable r){return new Entry(t,d,i,r,false,false);}
        static Entry danger(String t,String d,String i,Runnable r){return new Entry(t,d,i,r,false,true);}
        static Entry section(String t){return new Entry(t,"","",null,true,false);}
        static Entry note(String text){return new Entry("",text,"",null,false,false);}
    }
    static List<Entry> entries(){return new ArrayList<>();}
    static AlertDialog show(Activity host, String title, String description, List<Entry> entries){
        Ui.Palette p=new Ui.Palette(host);
        int d=Ui.dp(host,1);
        LinearLayout root=Ui.vertical(host);
        root.setPadding(d*18,d*19,d*18,d*12);
        root.setBackground(Ui.shape(host,p.surface,24));
        TextView heading=Ui.text(host,title,19,p.text,true);
        heading.setMaxLines(2);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        if(description!=null && !description.isEmpty()){
            TextView subtitle=Ui.text(host,description,12,p.muted,false);
            subtitle.setMaxLines(4);subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            root.addView(subtitle,Ui.lp(host,-1,-2,6));
        }
        // Limita o menu para não ocupar a tela toda nem ficar encoberto pelo teclado.
        ScrollView scroll=new ScrollView(host){
            @Override protected void onMeasure(int w,int h){
                int limit=host.getResources().getDisplayMetrics().heightPixels*62/100;
                super.onMeasure(w,MeasureSpec.makeMeasureSpec(limit,MeasureSpec.AT_MOST));
            }
        };
        scroll.setVerticalScrollBarEnabled(false);scroll.setFillViewport(false);
        LinearLayout rows=Ui.vertical(host);
        rows.setPadding(0,d*5,0,d*4);
        final AlertDialog dialog=new AlertDialog.Builder(host).setView(root).create();
        for(Entry item:entries){
            if(item.title.isEmpty() && item.onClick==null){
                TextView note=Ui.text(host,item.description,13,p.muted,false);
                note.setPadding(d*9,d*9,d*9,d*9);
                rows.addView(note);
                continue;
            }
            if(item.section){
                TextView section=Ui.text(host,item.title,12,p.muted,true);
                section.setPadding(d*8,d*12,0,d*3);
                rows.addView(section);continue;
            }
            LinearLayout row=Ui.horizontal(host);
            row.setPadding(d*12,d*10,d*12,d*10);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.shape(host,p.soft,15));
            row.setClickable(true);row.setFocusable(true);
            row.setContentDescription(item.title);
            int fg=item.danger?p.danger:p.text;
            FrameLayout iconBox=new FrameLayout(host);
            iconBox.setBackground(Ui.shape(host,p.surface,12));
            iconBox.addView(Ui.icon(host,item.icon,fg,21),new FrameLayout.LayoutParams(d*22,d*22,Gravity.CENTER));
            row.addView(iconBox,new LinearLayout.LayoutParams(d*40,d*40));
            LinearLayout labels=Ui.vertical(host);
            labels.addView(Ui.text(host,item.title,14,fg,true));
            if(item.description!=null && !item.description.isEmpty()){
                TextView sub=Ui.text(host,item.description,12,p.muted,false);
                sub.setMaxLines(2);sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
                labels.addView(sub,Ui.lp(host,-1,-2,2));
            }
            LinearLayout.LayoutParams labelP=new LinearLayout.LayoutParams(0,-2,1);labelP.leftMargin=d*11;
            row.addView(labels,labelP);
            row.addView(Ui.icon(host,"forward",p.muted,17),new LinearLayout.LayoutParams(d*19,d*19));
            row.setOnClickListener(v->{dialog.dismiss();if(item.onClick!=null)item.onClick.run();});
            rows.addView(row,Ui.lp(host,-1,-2,7));
        }
        scroll.addView(rows,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,Ui.lp(host,-1,-2,8));
        TextView close=Ui.pill(host,"Fechar",p.text,p.soft);
        close.setClickable(true);close.setFocusable(true);
        close.setOnClickListener(v->dialog.dismiss());
        LinearLayout.LayoutParams closeP=Ui.lp(host,-2,44,8);closeP.gravity=Gravity.END;
        root.addView(close,closeP);
        // Fecha IME aberto durante pesquisas para não deixar teclado atrás do menu.
        InputMethodManager keyboard=(InputMethodManager)host.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focused=host.getCurrentFocus();
        if(focused instanceof EditText)focused.clearFocus();
        if(keyboard!=null)keyboard.hideSoftInputFromWindow(host.getWindow().getDecorView().getWindowToken(),0);
        dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            dialog.getWindow().setLayout(host.getResources().getDisplayMetrics().widthPixels*92/100,ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }
    static AlertDialog info(Activity host,String title,String text,List<Entry> actions){
        List<Entry> items=entries();
        items.add(Entry.note(text));
        items.addAll(actions);
        return show(host,title,"",items);
    }
}
