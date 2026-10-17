package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.CookieManager;
import android.webkit.WebStorage;

import java.util.List;

/** Página de configurações: navegador, aparência, privacidade e links padrão. */
public final class SettingsActivity extends Activity {
    public static final String EXTRA_OPEN_FAVORITES="open_favorites";
    private Ui.Palette palette;
    private LinearLayout list;
    private int dp(int x){return Ui.dp(this,x);}

    @Override public void onCreate(Bundle state){
        Ui.selectTheme(this);
        super.onCreate(state);
        render();
        if(getIntent()!=null && getIntent().getBooleanExtra(EXTRA_OPEN_FAVORITES,false)){
            getIntent().removeExtra(EXTRA_OPEN_FAVORITES);
            manageFavorites();
        }
    }
    private void render(){
        palette=new Ui.Palette(this);
        Ui.systemBars(this,palette);
        LinearLayout root=Ui.vertical(this);root.setBackgroundColor(palette.background);
        setContentView(root);
        LinearLayout heading=Ui.horizontal(this);heading.setPadding(dp(14),dp(8),dp(14),dp(5));
        TextView back=Ui.text(this,"‹",34,palette.text,false);
        back.setGravity(Gravity.CENTER);back.setContentDescription("Voltar");
        heading.addView(back,new LinearLayout.LayoutParams(dp(44),dp(48)));
        back.setOnClickListener(v->finish());
        TextView title=Ui.text(this,"Configurações",22,palette.text,true);
        heading.addView(title,new LinearLayout.LayoutParams(-1,dp(48)));
        root.addView(heading);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
        list=Ui.vertical(this);list.setPadding(dp(16),dp(5),dp(16),dp(24));
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        section("Navegador");
        option("Página inicial",BrowserPrefs.home(this),"home",this::changeHome);
        option("Mecanismo de pesquisa",BrowserPrefs.searchEngine(this),"search",this::changeSearch);
        downloadSwitch("Modo computador",
            "Solicitar versão desktop dos sites. Alguns sites podem ignorar esta preferência.",
            BrowserPrefs.desktop(this), BrowserPrefs::setDesktop);
        option("Tamanho do texto",BrowserPrefs.textZoom(this)+"%", "search", this::chooseTextZoom);
        option("Favoritos","Sites salvos: "+BrowserPrefs.favorites(this).size(),"bookmark",this::manageFavorites);
        option("Histórico de navegação","Ver os últimos endereços visitados","menu",this::showHistory);
        option("Limpar dados do navegador","Cookies, cache e histórico locais","trash",this::confirmClear);
        option("Compatibilidade de login", "Cookies de terceiros: " + (BrowserPrefs.thirdPartyCookies(this) ? "ativados (menos privacidade)" : "bloqueados (padrão)"), "lock", this::chooseCookieCompatibility);

        section("Downloads • rede e bateria");
        downloadSwitch("Somente Wi-Fi",
            "Bloqueia downloads nas demais redes, inclusive dados móveis e VPN sem Wi-Fi identificável.",
            DownloadRestrictions.wifiOnly(this), DownloadRestrictions::setWifiOnly);
        downloadSwitch("Permitir dados móveis",
            "Quando Somente Wi-Fi está desligado, permite iniciar pela rede da operadora.",
            DownloadRestrictions.allowMobile(this), DownloadRestrictions::setAllowMobile);
        downloadSwitch("Proteger bateria abaixo de 20%",
            "Pausa ao atingir 20% ou menos sem carregador. Retomada manual.",
            DownloadRestrictions.protectBattery(this), DownloadRestrictions::setProtectBattery);
        TextView downloadInfo=Ui.text(this,
            "As regras são verificadas no início e durante a transferência. Se mudarem, o download é pausado; não será reiniciado sozinho. A navegação web não é bloqueada.",
            12,palette.muted,false);
        downloadInfo.setPadding(dp(8),dp(12),dp(8),dp(3));
        list.addView(downloadInfo);

        section("Privacidade e compatibilidade");
        LinearLayout js=Ui.horizontal(this);js.setPadding(dp(16),dp(8),dp(16),dp(8));js.setBackground(Ui.shape(this,palette.surface,18));
        LinearLayout jsText=Ui.vertical(this);
        jsText.addView(Ui.text(this,"Ativar JavaScript",15,palette.text,true));
        jsText.addView(Ui.text(this,"Alguns sites podem parar de funcionar se desativado",12,palette.muted,false),Ui.lp(this,-1,-2,3));
        js.addView(jsText,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(this);
        toggle.setChecked(BrowserPrefs.javascript(this));
        toggle.setOnCheckedChangeListener((b,checked)->{
            BrowserPrefs.setJavascript(this,checked);
            Toast.makeText(this,"A alteração vale ao recarregar a página",Toast.LENGTH_SHORT).show();
        });
        js.addView(toggle,new LinearLayout.LayoutParams(dp(52),dp(50)));
        list.addView(js,Ui.lp(this,-1,-2,7));
        TextView info=Ui.text(this,"Segurança fixa: arquivos locais e conteúdo misto bloqueados. Cookies de terceiros são opcionais e desligados por padrão. O login OAuth do Google exige navegador compatível.",12,palette.muted,false);
        info.setPadding(dp(8),dp(11),dp(8),dp(8));
        list.addView(info);

        section("Aplicativo");
        option("Aparência","Sistema, claro ou escuro AMOLED","settings",this::chooseAppearance);
        option("Links padrão do Android","Abertura de links por este aplicativo","link",this::openDefaultLinks);
        option("Sobre","Baixa Fácil 0.1.0-alpha.29","webdown",()->new AlertDialog.Builder(this)
            .setTitle("Baixa Fácil").setMessage("Versão 0.1.0-alpha.29\nNavegação e downloads integrados.\nO aplicativo não intercepta downloads internos de outros apps.")
            .setPositiveButton("Fechar",null).show());
    }
    private void chooseCookieCompatibility(){
        new AlertDialog.Builder(this)
            .setTitle("Compatibilidade de login")
            .setMessage("Alguns sites usam cookies de terceiros no login ou verificação de segurança. Ativá-los pode ajudar, mas permite rastreamento entre sites e não garante login no Facebook.\n\nO Google não permite OAuth em WebViews; use um navegador externo para esse login. Cookies externos não serão importados para o Baixa Fácil.")
            .setNegativeButton("Cancelar",null)
            .setNeutralButton("Bloquear",(d,w)->{
                BrowserPrefs.setThirdPartyCookies(this,false);
                render();
                Toast.makeText(this,"Cookies de terceiros bloqueados",Toast.LENGTH_SHORT).show();
            })
            .setPositiveButton("Ativar por escolha",(d,w)->{
                BrowserPrefs.setThirdPartyCookies(this,true);
                render();
                Toast.makeText(this,"Ativado. Volte ao navegador para recarregar a página.",Toast.LENGTH_LONG).show();
            }).show();
    }

    private interface SwitchSaver { void save(android.content.Context context, boolean checked); }
    private void downloadSwitch(String title, String detail, boolean checked, SwitchSaver save){
        LinearLayout line=Ui.horizontal(this);
        line.setPadding(dp(14),dp(11),dp(14),dp(11));
        line.setBackground(Ui.shape(this,palette.surface,17));
        LinearLayout content=Ui.vertical(this);
        content.addView(Ui.text(this,title,15,palette.text,true));
        content.addView(Ui.text(this,detail,12,palette.muted,false),Ui.lp(this,-1,-2,3));
        line.addView(content,new LinearLayout.LayoutParams(0,-2,1));
        Switch button=new Switch(this);
        button.setContentDescription(title);
        button.setChecked(checked);
        button.setOnCheckedChangeListener((v,value)->save.save(this,value));
        LinearLayout.LayoutParams buttonP=new LinearLayout.LayoutParams(dp(55),dp(48));
        buttonP.leftMargin=dp(8);
        line.addView(button,buttonP);
        line.setOnClickListener(v->button.setChecked(!button.isChecked()));
        list.addView(line,Ui.lp(this,-1,-2,7));
    }
    private void section(String label){
        TextView section=Ui.text(this,label,17,palette.text,true);
        section.setPadding(dp(5),dp(20),0,dp(6));
        list.addView(section);
    }
    private void option(String title,String detail,String symbol,Runnable action){
        LinearLayout line=Ui.horizontal(this);
        line.setPadding(dp(14),dp(12),dp(12),dp(12));
        line.setBackground(Ui.shape(this,palette.surface,17));
        line.setClickable(true);line.setFocusable(true);
        line.addView(Ui.icon(this,symbol,palette.muted,22),new LinearLayout.LayoutParams(dp(25),dp(25)));
        LinearLayout content=Ui.vertical(this);
        content.addView(Ui.text(this,title,15,palette.text,true));
        TextView hint=Ui.text(this,detail,12,palette.muted,false);
        content.addView(hint,Ui.lp(this,-1,-2,3));
        LinearLayout.LayoutParams contentP=new LinearLayout.LayoutParams(0,-2,1);contentP.leftMargin=dp(12);
        line.addView(content,contentP);
        line.addView(Ui.icon(this,"forward",palette.muted,19),new LinearLayout.LayoutParams(dp(21),dp(21)));
        line.setOnClickListener(v->action.run());
        list.addView(line,Ui.lp(this,-1,-2,7));
    }
    private void changeHome(){
        EditText field=new EditText(this);
        field.setSingleLine(true);field.setText(BrowserPrefs.home(this));
        field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout wrap=Ui.vertical(this);wrap.setPadding(dp(18),dp(4),dp(18),0);
        wrap.addView(field);
        new AlertDialog.Builder(this).setTitle("Página inicial")
            .setMessage("Digite um site. HTTPS será adicionado se faltar o protocolo.")
            .setView(wrap)
            .setNeutralButton("Google",(d,w)->{BrowserPrefs.setHome(this,BrowserPrefs.DEFAULT_HOME);render();})
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Salvar",(d,w)->{
                if(!BrowserPrefs.setHome(this,field.getText().toString()))
                    Toast.makeText(this,"Endereço inválido",Toast.LENGTH_LONG).show();
                render();
            }).show();
    }
    private void changeSearch(){
        String[] options={"Google","DuckDuckGo"};
        int selected="DuckDuckGo".equals(BrowserPrefs.searchEngine(this))?1:0;
        new AlertDialog.Builder(this).setTitle("Mecanismo de pesquisa")
            .setSingleChoiceItems(options,selected,(d,w)->{BrowserPrefs.setSearchEngine(this,options[w]);d.dismiss();render();})
            .setNegativeButton("Fechar",null).show();
    }
    private void chooseTextZoom(){
        final int[] values={75,100,125,150};
        String[] options={"75%", "100% (padrão)", "125%", "150%"};
        int selected=1;
        for(int i=0;i<values.length;i++)if(values[i]==BrowserPrefs.textZoom(this))selected=i;
        new AlertDialog.Builder(this).setTitle("Tamanho do texto nas páginas")
            .setSingleChoiceItems(options,selected,(d,w)->{
                BrowserPrefs.setTextZoom(this,values[w]);d.dismiss();render();
            }).setNegativeButton("Fechar",null).show();
    }
    private void showHistory(){
        List<String> items=BrowserPrefs.history(this);
        if(items.isEmpty()){Toast.makeText(this,"Histórico vazio",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("Histórico recente")
            .setItems(items.toArray(new CharSequence[0]),(d,w)->{
                Intent result=new Intent();result.putExtra("open_url",items.get(w));
                setResult(RESULT_OK,result);finish();
            }).setNegativeButton("Fechar",null).show();
    }
    private void manageFavorites(){
        List<BrowserPrefs.Favorite> items=BrowserPrefs.favorites(this);
        String[] names=new String[items.size()];
        for(int i=0;i<items.size();i++) names[i]=items.get(i).title+"\n"+items.get(i).url;
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("Favoritos ("+items.size()+")")
            .setNeutralButton("Adicionar",(d,w)->editFavorite(null))
            .setNegativeButton("Fechar",null);
        if(items.isEmpty()) dialog.setMessage("Nenhum site salvo. Adicione um endereço HTTP ou HTTPS.");
        else dialog.setItems(names,(d,w)->favoriteActions(items.get(w)));
        dialog.show();
    }

    private void favoriteActions(BrowserPrefs.Favorite item){
        String[] actions={"Abrir no navegador","Editar nome e endereço","Remover favorito"};
        new AlertDialog.Builder(this).setTitle(item.title).setMessage(item.url)
            .setItems(actions,(d,w)->{
                if(w==0){
                    Intent result=new Intent();result.putExtra("open_url",item.url);
                    setResult(RESULT_OK,result);finish();
                }else if(w==1) editFavorite(item);
                else confirmRemoveFavorite(item);
            }).setNegativeButton("Voltar",(d,w)->manageFavorites()).show();
    }

    private void editFavorite(BrowserPrefs.Favorite current){
        LinearLayout fields=Ui.vertical(this);
        fields.setPadding(dp(18),dp(4),dp(18),0);
        EditText title=new EditText(this);
        title.setSingleLine(true);
        title.setHint("Nome do site (opcional)");
        title.setText(current==null?"":current.title);
        fields.addView(title,Ui.lp(this,-1,50,0));
        EditText url=new EditText(this);
        url.setSingleLine(true);
        url.setHint("site.com ou https://site.com");
        url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        url.setText(current==null?"":current.url);
        fields.addView(url,Ui.lp(this,-1,50,8));
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle(current==null?"Adicionar favorito":"Editar favorito")
            .setView(fields)
            .setNegativeButton("Cancelar",(d,w)->manageFavorites())
            .setPositiveButton("Salvar",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String entered=url.getText().toString();
            boolean ok=current==null
                ?BrowserPrefs.addFavorite(this,entered,title.getText().toString())
                :BrowserPrefs.editFavorite(this,current.url,entered,title.getText().toString());
            if(!ok){
                Toast.makeText(this,"Endereço inválido, duplicado ou limite de 60 favoritos",Toast.LENGTH_LONG).show();
                return;
            }
            dialog.dismiss();
            render();
            manageFavorites();
        }));
        dialog.show();
    }

    private void confirmRemoveFavorite(BrowserPrefs.Favorite item){
        new AlertDialog.Builder(this).setTitle("Remover favorito?")
            .setMessage(item.title+"\n"+item.url)
            .setNegativeButton("Cancelar",(d,w)->manageFavorites())
            .setPositiveButton("Remover",(d,w)->{
                BrowserPrefs.removeFavorite(this,item.url);
                render();
                manageFavorites();
            }).show();
    }

    private void confirmClear(){
        new AlertDialog.Builder(this).setTitle("Limpar dados do navegador?")
            .setMessage("Remove cookies, cache e histórico local do navegador integrado. Os favoritos, o Chrome e os downloads salvos serão preservados.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Limpar",(d,w)->{
                BrowserPrefs.requestClear(this);
                CookieManager.getInstance().removeAllCookies(done->CookieManager.getInstance().flush());
                WebStorage.getInstance().deleteAllData();
                Toast.makeText(this,"Limpeza solicitada",Toast.LENGTH_SHORT).show();
                render();
            }).show();
    }
    private void chooseAppearance(){
        String[] options={"Sistema","Claro","Escuro AMOLED"};
        new AlertDialog.Builder(this).setTitle("Aparência")
            .setSingleChoiceItems(options,Ui.mode(this),(d,w)->{Ui.setMode(this,w);d.dismiss();recreate();})
            .setNegativeButton("Fechar",null).show();
    }
    private void openDefaultLinks(){
        Intent intent;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
            intent=new Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,Uri.fromParts("package",getPackageName(),null));
        }else{
            intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",getPackageName(),null));
        }
        try{startActivity(intent);}catch(ActivityNotFoundException|SecurityException error){
            Toast.makeText(this,"Configuração indisponível neste Android",Toast.LENGTH_LONG).show();
        }
    }
}
