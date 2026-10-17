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
    public static final String EXTRA_OPEN_VIDEOS="open_videos";
    private boolean videoSettingsPage;
    private Ui.Palette palette;
    private LinearLayout list;
    private int dp(int x){return Ui.dp(this,x);}

    @Override public void onCreate(Bundle state){
        Ui.selectTheme(this);
        super.onCreate(state);
        videoSettingsPage=getIntent()!=null && getIntent().getBooleanExtra(EXTRA_OPEN_VIDEOS,false);
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
        back.setOnClickListener(v->{if(videoSettingsPage){videoSettingsPage=false;render();}else finish();});
        TextView title=Ui.text(this,videoSettingsPage?"Vídeos e mídia":"Configurações",22,palette.text,true);
        heading.addView(title,new LinearLayout.LayoutParams(-1,dp(48)));
        root.addView(heading);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
        list=Ui.vertical(this);list.setPadding(dp(16),dp(5),dp(16),dp(24));
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(videoSettingsPage){renderVideoSettings();return;}

        section("Navegador");
        option("Página inicial",BrowserPrefs.home(this),"home",this::changeHome);
        option("Mecanismo de pesquisa",BrowserPrefs.searchEngine(this),"search",this::changeSearch);
        downloadSwitch("Modo computador",
            "Solicitar versão desktop dos sites. Alguns sites podem ignorar esta preferência.",
            BrowserPrefs.desktop(this), BrowserPrefs::setDesktop);
        downloadSwitch("Compatibilidade com ChatGPT",
            "Pula varreduras de vídeo e filtros extras no chatgpt.com para diminuir trabalho do navegador. Não desativa JavaScript ou cookies. Padrão: ativado.",
            BrowserPrefs.chatGptLight(this), BrowserPrefs::setChatGptLight);
        option("Tamanho do texto",BrowserPrefs.textZoom(this)+"%", "search", this::chooseTextZoom);
        option("Favoritos","Sites salvos: "+BrowserPrefs.favorites(this).size(),"bookmark",this::manageFavorites);
        option("Histórico de navegação","Ver os últimos endereços visitados","menu",this::showHistory);
        option("Limpar dados do navegador","Cookies, cache e histórico locais","trash",this::confirmClear);
        option("Compatibilidade de login", "Cookies de terceiros: " + (BrowserPrefs.thirdPartyCookies(this) ? "ativados (menos privacidade)" : "bloqueados (padrão)"), "lock", this::chooseCookieCompatibility);

        section("🛡 Bloqueio de anúncios");
        option("Proteção", adBlockModeName(), "shield", this::chooseAdBlockMode);
        downloadSwitch("Bloquear rastreadores", "Bloqueia domínios conhecidos de métricas e rastreamento.",
            AdBlockController.savedTrackers(this),AdBlockController::setTrackers);
        downloadSwitch("Ocultar anúncios na página", "Esconde banners reconhecidos com CSS local; exige JavaScript.",
            AdBlockController.savedCosmetic(this),AdBlockController::setCosmetic);
        option("Atualizar listas de filtros", "EasyList + EasyPrivacy; atualização manual por HTTPS", "reload", this::updateAdLists);
        TextView adTip=Ui.text(this,"Modo Forte é o padrão. O Extremo pode prejudicar vídeos, logins e páginas. No navegador use ⋮ para desativar por site. Alterações exigem recarregar a página.",12,palette.muted,false);
        adTip.setPadding(dp(8),dp(10),dp(8),dp(5));list.addView(adTip);

        section("Vídeos e mídia");
        option("Configurações de vídeos","Detecção, arquivos diretos e streaming HLS/DASH","play",()->{videoSettingsPage=true;render();});

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
        option("Sobre","Baixa Fácil 0.1.0-alpha.35","webdown",()->{
            AppMenus.info(this,"Baixa Fácil","Versão 0.1.0-alpha.35\nNavegador e downloads integrados.\nO app não intercepta downloads de outros aplicativos.",AppMenus.entries());
        });
    }
    private String adBlockModeName(){
        String[] names={"Desligado","Leve","Forte (padrão)","Extremo"};
        int mode=AdBlockController.savedMode(this);
        return names[mode>=0&&mode<names.length?mode:AdBlockRules.STRONG];
    }
    private void renderVideoSettings(){
        section("Identificação de vídeos");
        downloadSwitch("Detecção automática", "Procurar fontes após o carregamento da página, sem consultar servidores adicionais.",
            VideoPrefs.autoScan(this), VideoPrefs::setAutoScan);
        downloadSwitch("Mostrar arquivos diretos", "Mostrar links de vídeos MP4, WebM e outros formatos diretos identificáveis.",
            VideoPrefs.showSources(this), VideoPrefs::setShowSources);
        downloadSwitch("Mostrar transmissões HLS/DASH", "Exibir playlists que podem ser reproduzidas online no Media3.",
            VideoPrefs.showStreams(this), VideoPrefs::setShowStreams);
        section("Reprodução e downloads");
        option("Entenda os formatos", "Arquivos diretos e reprodução online","play",()->{
            List<AppMenus.Entry> actions=AppMenus.entries();
            actions.add(AppMenus.Entry.item("Voltar às opções", "Consultar configurações de vídeos", "back",()->{videoSettingsPage=true;render();}));
            AppMenus.info(this,"Como funcionam os vídeos",
                "Arquivos MP4, WebM e outros formatos diretos podem ser baixados se o servidor permitir.\n\n"
                + "HLS (.m3u8) e DASH (.mpd) são playlists: nesta versão o Media3 pode reproduzi-las online, mas o app não faz download completo de segmentos.\n\n"
                + "Fontes blob:, DRM e conteúdo de sessões protegidas não são convertidos em arquivos. Os resultados detectados não ficam salvos no histórico.",actions);
        });
        TextView note=Ui.text(this,"As alterações entram em vigor ao voltar ao navegador. Quando houver resultados, o botão flutuante Vídeos aparece sobre a página; dentro da lista use Atualizar para fazer nova busca manualmente.",12,palette.muted,false);
        note.setPadding(dp(8),dp(12),dp(8),dp(6));list.addView(note);
    }
    @Override public void onBackPressed(){
        if(videoSettingsPage){videoSettingsPage=false;render();return;}
        super.onBackPressed();
    }

    private void chooseAdBlockMode(){
        String[] choices={"Desligado", "Leve", "Forte", "Extremo"};
        String[] notes={"Sem bloqueio de anúncios", "Domínios publicitários conhecidos", "Anúncios e rastreadores", "Filtros agressivos que podem quebrar sites"};
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(int i=0;i<choices.length;i++){
            final int mode=i;
            rows.add(AppMenus.Entry.item(choices[i]+(AdBlockController.savedMode(this)==i?" • atual":""),notes[i],"shield",()->{
                AdBlockController.setMode(this,mode);render();
                Toast.makeText(this,"Recarregue a página para aplicar",Toast.LENGTH_SHORT).show();
            }));
        }
        AppMenus.show(this,"Bloqueio de anúncios","Escolha a intensidade da proteção.",rows);
    }
    private void updateAdLists(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Atualizar agora","Baixar e aplicar as regras compatíveis","reload",()->{
            Toast.makeText(this,"Atualizando listas de filtros…",Toast.LENGTH_SHORT).show();
            new AdBlockController(this).updateNow((ok,msg)->runOnUiThread(()->Toast.makeText(this,msg,Toast.LENGTH_LONG).show()));
        }));
        AppMenus.info(this,"Atualizar filtros",
            "A atualização de EasyList e EasyPrivacy usa HTTPS e pode consumir alguns MB. Só são interpretadas regras simples de domínio; em caso de falha, a lista anterior será preservada.",rows);
    }
    private void chooseCookieCompatibility(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Bloquear cookies de terceiros"+(BrowserPrefs.thirdPartyCookies(this)?"":" • atual"),
            "Privacidade recomendada","shield",()->{
                BrowserPrefs.setThirdPartyCookies(this,false);render();
                Toast.makeText(this,"Cookies de terceiros bloqueados",Toast.LENGTH_SHORT).show();
            }));
        rows.add(AppMenus.Entry.item("Permitir cookies de terceiros"+(BrowserPrefs.thirdPartyCookies(this)?" • atual":""),
            "Pode ajudar em alguns logins, mas reduz a privacidade","lock",()->{
                BrowserPrefs.setThirdPartyCookies(this,true);render();
                Toast.makeText(this,"Opção alterada. Recarregue a página.",Toast.LENGTH_LONG).show();
            }));
        AppMenus.info(this,"Compatibilidade de login",
            "Cookies de terceiros podem ajudar certos sites, mas permitem rastreamento entre origens e não garantem login no Facebook. O Google OAuth não é suportado em WebViews. Cookies do navegador externo nunca são importados.",rows);
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
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(String engine:new String[]{"Google","DuckDuckGo"}){
            String selected=BrowserPrefs.searchEngine(this).equals(engine)?" • atual":"";
            rows.add(AppMenus.Entry.item(engine+selected,"Escolher mecanismo de busca","search",()->{
                BrowserPrefs.setSearchEngine(this,engine);render();
            }));
        }
        AppMenus.show(this,"Mecanismo de pesquisa","Escolha um dos buscadores disponíveis.",rows);
    }
    private void chooseTextZoom(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        int selected=BrowserPrefs.textZoom(this);
        for(int zoom:new int[]{75,100,125,150}){
            rows.add(AppMenus.Entry.item(zoom+"%"+(selected==zoom?" • atual":""),
                zoom==100?"Padrão":"Escala do texto da WebView","search",()->{
                    BrowserPrefs.setTextZoom(this,zoom);render();
                }));
        }
        AppMenus.show(this,"Tamanho do texto","Alterar o tamanho nas páginas web.",rows);
    }
    private void showHistory(){
        List<String> items=BrowserPrefs.history(this);
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(String url:items){
            String domain=Uri.parse(url).getHost();
            rows.add(AppMenus.Entry.item(domain==null?"Site":"Abrir "+domain,
                "Histórico de navegação","globe",()->{
                    Intent result=new Intent();result.putExtra("open_url",url);
                    setResult(RESULT_OK,result);finish();
                }));
        }
        AppMenus.show(this,"Histórico recente",items.isEmpty()?"Nenhum site visitado recentemente.":"Escolha um site para abrir.",rows);
    }
    private void manageFavorites(){
        List<BrowserPrefs.Favorite> items=BrowserPrefs.favorites(this);
        List<AppMenus.Entry> entries=AppMenus.entries();
        for(BrowserPrefs.Favorite favorite:items){
            String domain="Site salvo";
            try {domain=Uri.parse(favorite.url).getHost();}catch(Exception ignored){}
            entries.add(AppMenus.Entry.item(favorite.title,domain,"bookmark",()->favoriteActions(favorite)));
        }
        entries.add(AppMenus.Entry.section("GERENCIAR"));
        entries.add(AppMenus.Entry.item("Adicionar favorito","Nome e endereço do site","bookmark",()->editFavorite(null)));
        AppMenus.show(this,"Favoritos ("+items.size()+")",
            items.isEmpty()?"Nenhum site salvo. Adicione um endereço HTTP ou HTTPS.":"Escolha um site para abrir ou editar.",entries);
    }
    private void favoriteActions(BrowserPrefs.Favorite item){
        List<AppMenus.Entry> entries=AppMenus.entries();
        entries.add(AppMenus.Entry.item("Abrir no navegador","Navegar até o site salvo","globe",()->{
            Intent result=new Intent();result.putExtra("open_url",item.url);
            setResult(RESULT_OK,result);finish();
        }));
        entries.add(AppMenus.Entry.item("Editar favorito","Alterar nome e endereço","paste",()->editFavorite(item)));
        entries.add(AppMenus.Entry.danger("Remover favorito","Não exclui dados nem arquivos do site","trash",()->confirmRemoveFavorite(item)));
        entries.add(AppMenus.Entry.item("Voltar aos favoritos","Abrir a lista de sites salvos","back",this::manageFavorites));
        String domain=Uri.parse(item.url).getHost();
        AppMenus.show(this,item.title,domain,entries);
    }

    private void editFavorite(BrowserPrefs.Favorite current){
        LinearLayout fields=Ui.vertical(this);
        fields.setPadding(dp(17),dp(16),dp(17),dp(10));
        fields.setBackground(Ui.shape(this,palette.surface,22));
        TextView heading=Ui.text(this,current==null?"Adicionar favorito":"Editar favorito",19,palette.text,true);
        fields.addView(heading);
        TextView hint=Ui.text(this,"Salve apenas endereços HTTP ou HTTPS.",12,palette.muted,false);
        fields.addView(hint,Ui.lp(this,-1,-2,5));
        EditText title=new EditText(this);
        title.setSingleLine(true);title.setHint("Nome do site (opcional)");
        title.setTextColor(palette.text);title.setHintTextColor(palette.muted);
        title.setText(current==null?"":current.title);
        fields.addView(title,Ui.lp(this,-1,50,14));
        EditText url=new EditText(this);
        url.setSingleLine(true);url.setHint("site.com ou https://site.com");
        url.setTextColor(palette.text);url.setHintTextColor(palette.muted);
        url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        url.setText(current==null?"":current.url);
        fields.addView(url,Ui.lp(this,-1,50,8));
        LinearLayout actions=Ui.horizontal(this);actions.setGravity(Gravity.END);
        TextView cancel=Ui.pill(this,"Cancelar",palette.text,palette.soft);
        TextView save=Ui.pill(this,"Salvar",palette.surface,palette.teal);
        LinearLayout.LayoutParams saveP=Ui.lp(this,-2,44,0);saveP.leftMargin=dp(8);
        actions.addView(cancel,Ui.lp(this,-2,44,0));actions.addView(save,saveP);
        fields.addView(actions,Ui.lp(this,-1,-2,16));
        AlertDialog dialog=new AlertDialog.Builder(this).setView(fields).create();
        cancel.setOnClickListener(v->{dialog.dismiss();manageFavorites();});
        save.setOnClickListener(v->{
            String entered=url.getText().toString();
            boolean ok=current==null
                ?BrowserPrefs.addFavorite(this,entered,title.getText().toString())
                :BrowserPrefs.editFavorite(this,current.url,entered,title.getText().toString());
            if(!ok){Toast.makeText(this,"Endereço inválido, duplicado ou limite de 60 favoritos",Toast.LENGTH_LONG).show();return;}
            dialog.dismiss();render();manageFavorites();
        });
        dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels*92/100,-2);
        }
    }

    private void confirmRemoveFavorite(BrowserPrefs.Favorite item){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.danger("Remover favorito","Excluir apenas este atalho salvo","trash",()->{
            BrowserPrefs.removeFavorite(this,item.url);render();manageFavorites();
        }));
        rows.add(AppMenus.Entry.item("Cancelar","Voltar sem remover","back",this::manageFavorites));
        AppMenus.info(this,"Remover favorito?","Confirme a remoção de: "+item.title,rows);
    }
    private void confirmClear(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.danger("Limpar agora","Apagar cookies, cache e histórico local","trash",()->{
            BrowserPrefs.requestClear(this);
            CookieManager.getInstance().removeAllCookies(done->CookieManager.getInstance().flush());
            WebStorage.getInstance().deleteAllData();
            Toast.makeText(this,"Limpeza solicitada",Toast.LENGTH_SHORT).show();render();
        }));
        AppMenus.info(this,"Limpar dados do navegador?",
            "Apaga cookies, cache e histórico da WebView. Favoritos, arquivos baixados e outros navegadores não serão afetados.",rows);
    }
    private void chooseAppearance(){
        String[] options={"Sistema","Claro","Escuro AMOLED"};
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(int i=0;i<options.length;i++){
            final int mode=i;
            rows.add(AppMenus.Entry.item(options[i]+(Ui.mode(this)==i?" • atual":""),
                i==0?"Seguir o tema do Android":i==1?"Fundo claro":"Fundo preto e baixo consumo em AMOLED",
                "settings",()->{Ui.setMode(this,mode);recreate();}));
        }
        AppMenus.show(this,"Aparência","Tema do aplicativo e seus menus.",rows);
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
