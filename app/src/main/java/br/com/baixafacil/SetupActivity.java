package br.com.baixafacil;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Assistente local, sem permissões especiais ou recursos baixados. */
public final class SetupActivity extends Activity {
    private int step,theme,adult;
    private String search;
    private boolean trackers,wifi,battery;
    private Ui.Palette colors;
    private LinearLayout content;
    private int dp(int n){return Ui.dp(this,n);}
    @Override public void onCreate(Bundle state){
        Ui.selectTheme(this);super.onCreate(state);
        theme=Ui.mode(this);adult=ContentPrefs.level(this);search=BrowserPrefs.searchEngine(this);
        trackers=AdBlockController.savedTrackers(this);wifi=DownloadRestrictions.wifiOnly(this);
        battery=DownloadRestrictions.protectBattery(this);
        if(state!=null){step=state.getInt("step");theme=state.getInt("theme",theme);
            adult=state.getInt("adult",adult);search=state.getString("search",search);
            trackers=state.getBoolean("trackers",trackers);wifi=state.getBoolean("wifi",wifi);
            battery=state.getBoolean("battery",battery);}
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out){
        out.putInt("step",step);out.putInt("theme",theme);out.putInt("adult",adult);
        out.putString("search",search);out.putBoolean("trackers",trackers);
        out.putBoolean("wifi",wifi);out.putBoolean("battery",battery);
        super.onSaveInstanceState(out);
    }
    private void line(String title,String caption){
        TextView a=Ui.text(this,title,22,colors.text,true);content.addView(a,Ui.lp(this,-1,-2,12));
        TextView b=Ui.text(this,caption,13,colors.muted,false);
        content.addView(b,Ui.lp(this,-1,-2,9));
    }
    private void choice(String name,String detail,boolean chosen,Runnable select){
        LinearLayout card=Ui.vertical(this);card.setPadding(dp(16),dp(13),dp(16),dp(13));
        card.setBackground(Ui.outline(this,chosen?colors.soft:colors.surface,16,chosen?colors.teal:colors.border));
        TextView label=Ui.text(this,(chosen?"✓  ":"○  ")+name,16,colors.text,true);
        card.addView(label);
        TextView desc=Ui.text(this,detail,12,colors.muted,false);
        card.addView(desc,Ui.lp(this,-1,-2,5));
        card.setClickable(true);card.setFocusable(true);card.setContentDescription(name);
        card.setOnClickListener(v->{select.run();render();});
        content.addView(card,Ui.lp(this,-1,-2,9));
    }
    private TextView action(String label,Runnable cb,boolean primary){
        TextView t=Ui.pill(this,label,primary?colors.surface:colors.text,primary?colors.teal:colors.soft);
        t.setGravity(Gravity.CENTER);t.setOnClickListener(v->cb.run());
        return t;
    }
    private void render(){
        colors=new Ui.Palette(this);Ui.systemBars(this,colors);
        LinearLayout outer=Ui.vertical(this);outer.setBackgroundColor(colors.background);
        outer.setPadding(dp(18),dp(18),dp(18),dp(12));setContentView(outer);
        TextView progress=Ui.text(this,"BAIXA FÁCIL     •     Configuração "+(step+1)+" de 3",13,colors.teal,true);
        outer.addView(progress,Ui.lp(this,-1,-2,8));
        ScrollView scroll=new ScrollView(this);content=Ui.vertical(this);
        content.setPadding(0,dp(8),0,dp(18));scroll.addView(content);
        outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(step==0){
            line("Aparência e pesquisa","Escolha um tema e o mecanismo de busca. Tudo poderá ser alterado depois.");
            choice("Seguir o sistema","Claro ou escuro conforme o Android.",theme==0,()->theme=0);
            choice("Claro","Interface branca.",theme==1,()->theme=1);
            choice("Escuro AMOLED","Fundo preto para telas OLED.",theme==2,()->theme=2);
            line("Buscador", "Usado ao digitar termos em vez de endereços.");
            choice("Google","Pesquisa padrão.","Google".equals(search),()->search="Google");
            choice("DuckDuckGo","Outra opção de pesquisa.","DuckDuckGo".equals(search),()->search="DuckDuckGo");
        }else if(step==1){
            line("Privacidade e conteúdo","O filtro adulto é opcional e diferente da proteção contra rastreamento.");
            choice("Filtro desativado","Sem bloqueio por categoria adulta.",adult==AdultContentPolicy.OFF,()->adult=AdultContentPolicy.OFF);
            choice("Filtro moderado","Bloqueia domínios adultos conhecidos e força pesquisa segura quando compatível.",adult==AdultContentPolicy.MODERATE,()->adult=AdultContentPolicy.MODERATE);
            choice("Filtro rigoroso","Amplia a lista de domínios; ainda não é um controle parental completo.",adult==AdultContentPolicy.STRICT,()->adult=AdultContentPolicy.STRICT);
            choice("Bloquear rastreadores", "Proteção nativa GeckoView ETP (recomendada).",trackers,()->trackers=!trackers);
            TextView caveat=Ui.text(this,"O filtro não analisa imagens, redes sociais ou todas as páginas. Pode haver falsos positivos e sites que escapem. Não substitui o controle parental do Android.",12,colors.warn,false);
            content.addView(caveat,Ui.lp(this,-1,-2,12));
        }else{
            line("Downloads e bateria","Essas regras afetam downloads novos e em andamento, não a navegação.");
            choice("Somente Wi-Fi","Não baixar por rede móvel ou VPN sem Wi-Fi identificado.",wifi,()->wifi=!wifi);
            choice("Proteger bateria abaixo de 20%","Pausa transferências quando a bateria está baixa sem carregar.",battery,()->battery=!battery);
            TextView note=Ui.text(this,"Permissões de câmera, microfone e localização continuarão sendo pedidas somente quando necessárias. Senhas não são importadas automaticamente.",13,colors.muted,false);
            content.addView(note,Ui.lp(this,-1,-2,16));
        }
        LinearLayout buttons=Ui.horizontal(this);
        buttons.addView(action(step==0?"Pular":"Voltar",()->{
            if(step==0){ContentPrefs.completeSetup(this);finish();}
            else {step--;render();}},false),new LinearLayout.LayoutParams(0,dp(52),1));
        TextView next=action(step==2?"Concluir":"Continuar",()->{
            if(step<2){step++;render();}else saveAndFinish();},true);
        LinearLayout.LayoutParams nextP=new LinearLayout.LayoutParams(0,dp(52),1);nextP.leftMargin=dp(10);
        buttons.addView(next,nextP);outer.addView(buttons);
    }
    private void saveAndFinish(){
        Ui.setMode(this,theme);
        BrowserPrefs.setSearchEngine(this,search);
        ContentPrefs.setLevel(this,adult);
        AdBlockController.setTrackers(this,trackers);
        DownloadRestrictions.setWifiOnly(this,wifi);
        DownloadRestrictions.setProtectBattery(this,battery);
        ContentPrefs.completeSetup(this);
        finish();
    }
    @Override public void onBackPressed(){if(step>0){step--;render();}else super.onBackPressed();}
}
