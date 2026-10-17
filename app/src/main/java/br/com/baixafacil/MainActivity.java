package br.com.baixafacil;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.widget.ImageView;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Interface mínima para links próprios, compartilhamento e histórico de transferências. */
public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateLoop = new Runnable() {
        @Override public void run() {
            if(downloadsVisible) {
                refresh();
                handler.postDelayed(this, 1200);
            }
        }
    };
    private EditText urlInput;
    private DownloadAdapter adapter;
    private final MediaPreviewLoader mediaPreviewLoader=new MediaPreviewLoader();
    private android.widget.FrameLayout contentHost;
    private ListView downloadsList;
    private BrowserPane browserPane;
    private boolean downloadsVisible;
    private android.widget.FrameLayout[] bottomButtons;
    private int appearanceMode;
    private android.window.OnBackInvokedCallback systemBackCallback;

    private EditText searchInput;
    private TextView recoverQueueAction;
    private String listFilter = "Todos";
    private final java.util.Set<String> expandedDownloads = new java.util.HashSet<>();
    // Compacto é padrão; a visualização detalhada expande as informações de cada item.
    private boolean compactDownloads = true;
    private String typeFilter = "Todos";
    private int sortMode = 0; // 0 recentes, 1 antigos, 2 nome, 3 maiores, 4 menores


    @Override public void onCreate(Bundle savedInstanceState) {
        Ui.selectTheme(this);
        super.onCreate(savedInstanceState);
        appearanceMode = Ui.mode(this);
        compactDownloads = getSharedPreferences("download_view",MODE_PRIVATE).getBoolean("compact",true);
        if(savedInstanceState!=null){
            java.util.ArrayList<String> restored=savedInstanceState.getStringArrayList("EXPANDED_DOWNLOAD_IDS");
            if(restored!=null)expandedDownloads.addAll(restored);
        }
        DownloadStore.reconcile(this);
        createScreen(savedInstanceState);
        handleIntent(getIntent());
        if (Build.VERSION.SDK_INT >= 33) {
            systemBackCallback=this::handleBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,systemBackCallback);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String content = null;
        if (Intent.ACTION_SEND.equals(intent.getAction())) content = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) content = intent.getData().toString();
        if (content != null) {
            String url = UrlTools.extract(content);
            if (url != null) {
                urlInput.setText(url);
                urlInput.setSelection(url.length());
                showDownloads();
                Toast.makeText(this, "Link recebido. Toque em Baixar.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Compartilhamento sem link HTTP/HTTPS direto", Toast.LENGTH_LONG).show();
            }
        }
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }
    private TextView label(String value, int sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        if (bold) text.setTypeface(null, Typeface.BOLD);
        return text;
    }
    private LinearLayout.LayoutParams margins(int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(top);
        return p;
    }
    private Button button(String title) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(title);
        b.setTextSize(14);
        return b;
    }

    private Ui.Palette colors;
    private View emptyState;
    private TextView[] filterChips;

    private android.widget.FrameLayout iconButton(String name, String description, Runnable click) {
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        frame.setMinimumWidth(dp(44)); frame.setMinimumHeight(dp(44));
        frame.setContentDescription(description); frame.setClickable(true); frame.setFocusable(true);
        Ui.Icon icon = Ui.icon(this,name,colors.muted,23);
        android.widget.FrameLayout.LayoutParams p = new android.widget.FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER);
        frame.addView(icon,p);
        frame.setOnClickListener(v -> click.run());
        return frame;
    }

    private void createScreen(Bundle state) {
        colors = new Ui.Palette(this);
        Ui.systemBars(this,colors);
        LinearLayout root = Ui.vertical(this);
        root.setBackgroundColor(colors.background);
        setContentView(root);

        contentHost = new android.widget.FrameLayout(this);
        root.addView(contentHost,new LinearLayout.LayoutParams(-1,0,1));
        ListView history = new ListView(this);
        downloadsList = history;
        history.setDivider(null); history.setDividerHeight(0);
        history.setBackgroundColor(colors.background);
        history.setClipToPadding(false);
        history.setVerticalScrollBarEnabled(false);
        history.setHeaderDividersEnabled(false); history.setFooterDividersEnabled(false);
        contentHost.addView(history,new android.widget.FrameLayout.LayoutParams(-1,-1));

        LinearLayout header = Ui.vertical(this);
        header.setPadding(dp(12),dp(8),dp(12),dp(8));
        LinearLayout appHead = Ui.horizontal(this);
        android.widget.FrameLayout logo = new android.widget.FrameLayout(this);
        logo.setBackground(Ui.shape(this,colors.teal,14));
        logo.addView(Ui.icon(this,"webdown",Color.WHITE,24),new android.widget.FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        appHead.addView(logo,new LinearLayout.LayoutParams(dp(46),dp(46)));
        TextView brand = Ui.text(this,"Baixa Fácil",20,colors.text,true);
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(0,-2,1);
        idParams.leftMargin=dp(12); appHead.addView(brand,idParams);
        appHead.addView(iconButton("settings","Configurações",this::openSettings),new LinearLayout.LayoutParams(dp(44),dp(44)));
        header.addView(appHead,Ui.lp(this,-1,46,0));

        LinearLayout panel = Ui.vertical(this);
        panel.setPadding(dp(14),dp(14),dp(14),dp(14));
        panel.setBackground(Ui.shape(this,colors.surface,22));
        panel.addView(Ui.text(this,"Novo download",17,colors.text,true));
        panel.addView(Ui.text(this,"Cole um link ou digite o endereço do arquivo.",12,colors.muted,false),Ui.lp(this,-1,-2,4));

        LinearLayout input = Ui.horizontal(this);
        input.setPadding(dp(10),0,dp(6),0);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setBackground(Ui.shape(this,colors.soft,16));
        input.addView(Ui.icon(this,"link",colors.muted,21),new LinearLayout.LayoutParams(dp(20),dp(20)));
        urlInput = new EditText(this);
        urlInput.setBackgroundColor(Color.TRANSPARENT);
        urlInput.setTextColor(colors.text); urlInput.setHintTextColor(colors.muted);
        urlInput.setSingleLine(true); urlInput.setHint("site.com/arquivo.zip");
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
        urlInput.setTextSize(15); urlInput.setPadding(dp(10),0,dp(4),0);
        urlInput.setOnEditorActionListener((view,action,event)->{
            if(action == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {beginDownload();return true;}return false;
        });
        input.addView(urlInput,new LinearLayout.LayoutParams(0,dp(54),1));

        android.widget.FrameLayout pasteInline = iconButton("paste","Colar link",this::pasteLink);
        pasteInline.setBackground(Ui.shape(this,colors.surface,14));
        LinearLayout.LayoutParams pasteParams = new LinearLayout.LayoutParams(dp(42),dp(42));
        pasteParams.leftMargin = dp(4);
        input.addView(pasteInline,pasteParams);

        LinearLayout downloadInline = Ui.action(this,"Baixar","webdown",colors.text,colors.surface,14);
        downloadInline.setMinimumHeight(dp(42));
        downloadInline.setOnClickListener(v->beginDownload());
        LinearLayout.LayoutParams downloadParams = new LinearLayout.LayoutParams(-2,dp(42));
        downloadParams.leftMargin = dp(4);
        input.addView(downloadInline,downloadParams);

        panel.addView(input,Ui.lp(this,-1,54,14));
        header.addView(panel,Ui.lp(this,-1,-2,14));
        recoverQueueAction=Ui.pill(this,"Recuperar fila",colors.text,colors.surface);
        recoverQueueAction.setContentDescription("Recolocar downloads interrompidos na fila");
        recoverQueueAction.setOnClickListener(v->recoverPendingQueue());
        recoverQueueAction.setVisibility(View.GONE);
        header.addView(recoverQueueAction,Ui.lp(this,-1,44,9));

        LinearLayout section = Ui.horizontal(this);
        TextView title = Ui.text(this,"Meus downloads",22,colors.text,true);
        section.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        section.addView(iconButton("search","Pesquisar downloads",()->{
            boolean show = searchInput.getVisibility()!=View.VISIBLE;
            searchInput.setVisibility(show?View.VISIBLE:View.GONE);
            if(show) {searchInput.requestFocus(); ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(searchInput,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}
            else {searchInput.setText("");((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(searchInput.getWindowToken(),0);}
        }),new LinearLayout.LayoutParams(dp(44),dp(48)));
        section.addView(iconButton("filter","Filtrar e ordenar downloads",this::showOrganizeMenu),new LinearLayout.LayoutParams(dp(44),dp(48)));
        header.addView(section,Ui.lp(this,-1,52,12));

        filterRow = Ui.horizontal(this);
        filterChips = new TextView[3];
        String[] labels = {"Todos","Em fila","Concluídos"};
        for (int i=0;i<labels.length;i++) {
            final String selection = i==0 ? "Todos" : (i==1 ? "Fila" : "Arquivos");
            TextView chip = Ui.pill(this,labels[i],colors.text,colors.surface);
            LinearLayout.LayoutParams chipParams = Ui.lp(this,-2,44,0);
            if (i > 0) chipParams.leftMargin=dp(8);
            filterRow.addView(chip,chipParams);filterChips[i]=chip;
            chip.setOnClickListener(v->{listFilter=selection; updateFilterChips(); refresh();});
        }
        updateFilterChips();
        header.addView(filterRow,Ui.lp(this,-1,44,3));
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);searchInput.setHint("Pesquisar por nome, status ou origem");
        searchInput.setTextSize(14);searchInput.setTextColor(colors.text);searchInput.setHintTextColor(colors.muted);
        searchInput.setPadding(dp(16),0,dp(12),0);searchInput.setBackground(Ui.shape(this,colors.surface,15));
        searchInput.setVisibility(View.GONE);
        header.addView(searchInput,Ui.lp(this,-1,48,10));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {refresh();}
            @Override public void afterTextChanged(Editable e) {}
        });
        history.addHeaderView(header,null,false);

        LinearLayout empty = Ui.vertical(this);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(16),dp(24),dp(16),dp(30));
        empty.setBackground(Ui.shape(this,colors.surface,26));
        android.widget.FrameLayout emptyIcon = new android.widget.FrameLayout(this);
        emptyIcon.setBackground(Ui.shape(this,colors.soft,60));
        emptyIcon.addView(Ui.icon(this,"folder",colors.muted,46),new android.widget.FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER));
        empty.addView(emptyIcon,new LinearLayout.LayoutParams(dp(94),dp(94)));
        TextView emptyTitle = Ui.text(this,"Seus downloads aparecerão aqui",17,colors.text,true);
        emptyTitle.setGravity(Gravity.CENTER);empty.addView(emptyTitle,Ui.lp(this,-1,-2,16));
        TextView emptySub=Ui.text(this,"Cole um link ou use o navegador para começar.",13,colors.muted,false);
        emptySub.setGravity(Gravity.CENTER);empty.addView(emptySub,Ui.lp(this,-1,-2,10));
        LinearLayout emptyMargins = Ui.vertical(this);
        emptyMargins.setPadding(dp(16),dp(2),dp(16),dp(22));
        emptyMargins.addView(empty,new LinearLayout.LayoutParams(-1,-2));
        emptyState=emptyMargins;
        history.addFooterView(emptyMargins,null,false);

        adapter = new DownloadAdapter();
        history.setAdapter(adapter);
        history.setOnItemClickListener((parent,view,position,id)->{
            int index=position-history.getHeaderViewsCount();
            if(index>=0 && index<adapter.getCount()) showItemActions(adapter.getItem(index));
        });
        refresh();
        browserPane = new BrowserPane(this,colors,state);
        contentHost.addView(browserPane.view,new android.widget.FrameLayout.LayoutParams(-1,-1));
        createBottomBar(root);
        if (state != null && state.getBoolean("DOWNLOADS_VISIBLE",false)) showDownloads();
        else showBrowser();
    }

    private void createBottomBar(LinearLayout root) {
        LinearLayout bar=Ui.horizontal(this);
        bar.setPadding(dp(6),0,dp(6),dp(4));
        bar.setBackground(Ui.shape(this,colors.surface,20));
        String[] icons={"back","forward","reload","home","webdown"};
        String[] names={"Página anterior","Próxima página","Recarregar","Página inicial","Alternar downloads"};
        bottomButtons = new android.widget.FrameLayout[5];
        for (int i=0;i<icons.length;i++) {
            final int action=i;
            android.widget.FrameLayout button=new android.widget.FrameLayout(this);
            button.setContentDescription(names[i]);button.setClickable(true);button.setFocusable(true);
            button.setMinimumHeight(dp(52));
            bottomButtons[i]=button;
            bar.addView(button,new LinearLayout.LayoutParams(0,dp(52),1));
            button.setOnClickListener(v->{
                if(action==4){if(downloadsVisible)showBrowser();else showDownloads();return;}
                if(downloadsVisible)return;
                if(action==0)browserPane.goBack();
                else if(action==1)browserPane.goForward();
                else if(action==2)browserPane.reload();
                else if(action==3)browserPane.home();
            });
        }
        root.addView(bar,Ui.lp(this,-1,56,0));
        updateBottomBar();
    }
    private void updateBottomBar(){
        if (bottomButtons==null)return;
        String[] icons={"back","forward","reload","home","webdown"};
        for(int i=0;i<bottomButtons.length;i++){
            android.widget.FrameLayout b=bottomButtons[i];b.removeAllViews();
            int color=downloadsVisible&&i!=4 ? colors.muted : (i==4&&downloadsVisible?colors.teal:colors.text);
            b.addView(Ui.icon(this,icons[i],color,24),new android.widget.FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
            if(i==4){b.setBackground(Ui.shape(this,downloadsVisible?colors.soft:colors.surface,18));}
        }
    }
    public void showDownloads() {
        if(downloadsList==null||browserPane==null)return;
        downloadsVisible=true;
        downloadsList.setVisibility(View.VISIBLE);
        browserPane.view.setVisibility(View.GONE);
        browserPane.pause();
        handler.removeCallbacks(updateLoop);
        handler.post(updateLoop);
        refresh();updateBottomBar();
    }
    private void showBrowser() {
        if(downloadsList==null||browserPane==null)return;
        downloadsVisible=false;
        downloadsList.setVisibility(View.GONE);
        browserPane.view.setVisibility(View.VISIBLE);
        browserPane.resume();
        handler.removeCallbacks(updateLoop);
        updateBottomBar();
    }
    public void openSettings(){startActivityForResult(new Intent(this,SettingsActivity.class),1001);}
    public void openVideoSettings(){
        Intent intent=new Intent(this,SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_OPEN_VIDEOS,true);
        startActivityForResult(intent,1001);
    }
    public void openFavoriteSettings(){
        Intent intent=new Intent(this,SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_OPEN_FAVORITES,true);
        startActivityForResult(intent,1001);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==1001 && result==RESULT_OK && data!=null){
            String url=data.getStringExtra("open_url");
            if(url!=null&&browserPane!=null){showBrowser();browserPane.navigate(url);}
        }
    }
    @Override protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);
        out.putBoolean("DOWNLOADS_VISIBLE",downloadsVisible);
        out.putStringArrayList("EXPANDED_DOWNLOAD_IDS",new java.util.ArrayList<>(expandedDownloads));
        if(browserPane!=null)browserPane.saveState(out);
    }
    private void handleBack(){
        if(downloadsVisible){showBrowser();return;}
        if(browserPane!=null&&browserPane.canGoBack()){browserPane.goBack();return;}
        finish();
    }
    @SuppressWarnings("deprecation")
    @Override public void onBackPressed(){handleBack();}

    private LinearLayout filterRow;
    private void updateFilterChips() {
        if (filterChips == null) return;
        for(int i=0;i<filterChips.length;i++) {
            boolean selected = i==0 ? "Todos".equals(listFilter) : i==1 ? "Fila".equals(listFilter) : "Arquivos".equals(listFilter);
            filterChips[i].setTextColor(selected ? Color.WHITE : colors.text);
            filterChips[i].setBackground(Ui.shape(this,selected ? Ui.TEAL : colors.surface,50));
        }
    }

    /** Abre a tela oficial do Android; cada ROM pode mostrar opções diferentes. */
    private void openLinkSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent appLinks = new Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
            if (startSettings(appLinks)) return;
        }
        Intent appDetails = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        if (!startSettings(appDetails)) {
            Toast.makeText(this, "Não foi possível abrir as configurações deste app", Toast.LENGTH_LONG).show();
        }
    }

    private boolean startSettings(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException error) {
            return false;
        }
    }

    private void pasteLink() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(this, "Área de transferência vazia", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        CharSequence text = clip == null || clip.getItemCount() == 0 ? null : clip.getItemAt(0).coerceToText(this);
        String extracted = UrlTools.extract(text == null ? null : text.toString());
        if (extracted == null) extracted = UrlTools.normalizeAddress(text == null ? null : text.toString());
        if (extracted == null) {
            Toast.makeText(this, "Não foi encontrado link HTTP/HTTPS", Toast.LENGTH_LONG).show();
            return;
        }
        urlInput.setText(extracted);
        urlInput.setSelection(extracted.length());
    }

    private void showOrganizeMenu() {
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Visualização: "+(compactDownloads?"Compacta":"Detalhada"),
            "Alternar entre cartões compactos e expandidos","folder",()->{
                compactDownloads=!compactDownloads;
                getSharedPreferences("download_view",MODE_PRIVATE).edit().putBoolean("compact",compactDownloads).apply();
                adapter.notifyDataSetChanged();
            }));
        rows.add(AppMenus.Entry.item("Filtros de status", "Mostrar ou ocultar as opções Todos/Em fila/Concluídos","filter",()->
            filterRow.setVisibility(filterRow.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE)));
        rows.add(AppMenus.Entry.item("Tipo: "+typeFilter,"Filtrar arquivos pela extensão","file",this::chooseFileType));
        rows.add(AppMenus.Entry.item("Ordenação","Recentes, antigos, nome ou tamanho","menu",this::chooseSortMode));
        AppMenus.show(this,"Organizar downloads","Personalize a lista sem alterar os arquivos.",rows);
    }
    private void chooseFileType() {
        final String[] types={"Todos","Aplicativos","Vídeos","Áudios","Imagens","Documentos","Compactados","Outros"};
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(String type:types){
            rows.add(AppMenus.Entry.item(type+(type.equals(typeFilter)?" • atual":""),
                "Filtrar a lista de downloads","file",()->{typeFilter=type;refresh();}));
        }
        AppMenus.show(this,"Tipos de arquivo","Filtragem estimada pela extensão do nome.",rows);
    }
    private void chooseSortMode() {
        final String[] modes={"Mais recentes", "Mais antigos", "Nome A–Z", "Maiores primeiro", "Menores primeiro"};
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(int i=0;i<modes.length;i++){
            final int mode=i;
            rows.add(AppMenus.Entry.item(modes[i]+(mode==sortMode?" • atual":""),
                "Organizar a lista de downloads","menu",()->{sortMode=mode;refresh();}));
        }
        AppMenus.show(this,"Ordenar downloads","Escolha a sequência da lista.",rows);
    }

    private void beginDownload() {
        String normalized = UrlTools.normalizeAddress(urlInput.getText().toString());
        // Uma variável capturada pelo listener do diálogo deve ser final/effectively final.
        final String link = normalized != null ? normalized : UrlTools.extract(urlInput.getText().toString());
        if (link == null) {
            AppEventLog.warn(this,"Downloads","Link não reconhecido para download direto.");
            new AlertDialog.Builder(this)
                .setTitle("Link não reconhecido")
                .setMessage("Digite um endereço válido, por exemplo: site.com/arquivo.zip. Adicionamos HTTPS automaticamente quando necessário. Links privados sandbox: não são acessíveis por este aplicativo.")
                .setPositiveButton("Entendi", null).show();
            return;
        }
        previewDownload(link);
    }

    /** Consulta HTTP HEAD fora da thread de UI; é opcional e não salva tokens/cookies. */
    private void previewDownload(final String url) {
        final android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Consultando informações do arquivo...");
        progress.setCancelable(false);
        progress.show();
        new Thread(() -> {
            DownloadPreview.Result result=DownloadPreview.probe(url);
            handler.post(() -> {
                if(isFinishing() || isDestroyed())return;
                progress.dismiss();
                String type=result.type==null||result.type.isEmpty()?"Tipo não informado":result.type;
                DownloadDialogs.confirm(this,
                    "Baixar com Baixa Fácil?",
                    result.name,
                    "Origem",
                    hostLabel(url),
                    "Arquivo",
                    result.length>=0?readableSize(result.length)+" • "+type:"Tamanho não informado • "+type,
                    "Destino",
                    "Download/ pública",
                    result.message,
                    url.startsWith("http://"),
                    ()->enqueue(url));
            });
        },"download-head-preview").start();
    }

    private void enqueue(String link) {
        String restriction=DownloadRestrictions.blocked(this);
        if(restriction!=null) {
            AppEventLog.warn(this,"Downloads","Download bloqueado pelas regras locais de rede ou bateria.");
            new AlertDialog.Builder(this).setTitle("Download aguardando")
                .setMessage(restriction+"\n\nAltere as opções em Configurações > Downloads.")
                .setPositiveButton("Entendi",null).show();
            return;
        }
        String id=DownloadStore.create(this,link);
        boolean started=TransferStarter.enqueue(this,id,link,null,null,null);
        if(!started)AppEventLog.error(this,"Downloads","Android bloqueou ou recusou a fila do download.");
        Toast.makeText(this,started?"Download adicionado":"Android bloqueou o download",Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void recoverPendingQueue() {
        String restriction=DownloadRestrictions.blocked(this);
        if(restriction!=null){Toast.makeText(this,restriction,Toast.LENGTH_LONG).show();return;}
        List<JSONObject> entries=DownloadStore.recoverable(this);
        int restored=0;
        for(JSONObject entry:entries) {
            String id=entry.optString("id"),url=entry.optString("url");
            if (!TransferHttp.isWebUrl(url)||TransferStarter.isActive(id))continue;
            if(TransferStarter.enqueue(this,id,url,null,null,null))restored++;
        }
        Toast.makeText(this,restored+" download(s) recolocado(s). Links autenticados podem exigir nova sessão.",Toast.LENGTH_LONG).show();
        refresh();
    }

    private void refresh() {
        if (adapter == null) return;
        adapter.reload();
        if (recoverQueueAction != null) {
            int count=DownloadStore.recoverable(this).size();
            recoverQueueAction.setText("Recuperar fila ("+count+")");
            recoverQueueAction.setVisibility(count>0?View.VISIBLE:View.GONE);
        }
        if (emptyState != null) emptyState.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void showItemActions(JSONObject entry) {
        String uriText = entry.optString("uri");
        String status = entry.optString("status");
        String id = entry.optString("id");
        boolean completed = "Concluído".equals(status) && !uriText.isEmpty();
        if (!completed) {
            String error = entry.optString("error");
            String code = entry.optString("code");
            int position = TransferStarter.queuePosition(this,id);
            String detail = status + " • tentativa " + entry.optInt("attempt", 0)
                    + "/" + TransferPolicy.MAX_ATTEMPTS
                    + "\nRecebidos: " + entry.optLong("bytes", 0) + " bytes"
                    + (position > 0 ? "\nPosição na fila: " + position : "")
                    + (code.isEmpty() ? "" : "\nCódigo: " + code)
                    + (error.isEmpty() ? "" : "\n" + error);
            boolean active = TransferStarter.isActive(id) || "Na fila".equals(status);
            java.util.List<MenuAction> items = new java.util.ArrayList<>();
            if (active && position > 0) items.add(new MenuAction("webdown","Baixar primeiro","Mova este item para o início da fila.",false,()->controlDownload(id, TransferService.ACTION_PRIORITIZE)));
            if (active) {
                items.add(new MenuAction("pause","Pausar","Mantém o fragmento já baixado.",false,()->controlDownload(id, TransferService.ACTION_PAUSE)));
                items.add(new MenuAction("x","Cancelar","Descarta o arquivo parcial deste download.",true,()->confirmCancel(id)));
            } else {
                items.add(new MenuAction("reload","Tentar novamente","Recoloca este download na fila.",false,()->retryDownload(entry)));
                items.add(new MenuAction("trash","Remover do histórico","Apaga o registro e o fragmento parcial.",true,()->confirmRemoveEntry(entry)));
            }
            items.add(new MenuAction("copy","Copiar diagnóstico","Copia os detalhes técnicos para a área de transferência.",false,()->copyText("Diagnóstico Baixa Fácil", detail)));
            showActionMenu(entry.optString("name", "Download"), detail, items);
            return;
        }
        Uri uri = safeUri(uriText);
        java.util.List<MenuAction> items = new java.util.ArrayList<>();
        if ("Vídeos".equals(DownloadInfo.kind(entry.optString("name"))) && uri!=null)
            items.add(new MenuAction("play","Reproduzir vídeo","Abre o reprodutor Media3 integrado.",false,()->playLocalVideo(uri)));
        String itemKind=DownloadInfo.kind(entry.optString("name"));
        if (("Vídeos".equals(itemKind)||"Imagens".equals(itemKind)) && uri!=null)
            items.add(new MenuAction("search","Detalhes da mídia","Mostrar resolução e duração quando disponíveis.",false,()->showMediaDetails(uri,entry.optString("name"))));
        items.add(new MenuAction("file","Abrir arquivo","Abre o arquivo no app compatível do Android.",false,()->openFile(uri)));
        items.add(new MenuAction("link","Compartilhar arquivo","Envia o arquivo para outro aplicativo.",false,()->shareFile(uri)));
        if(uri!=null)items.add(new MenuAction("paste","Renomear arquivo","Altera o nome salvo na pasta Download.",false,()->renameFile(entry, uri)));
        if(uri!=null)items.add(new MenuAction("trash","Excluir arquivo","Remove o arquivo do armazenamento e da lista.",true,()->confirmDeleteFile(entry, uri)));
        items.add(new MenuAction("folder","Remover só do histórico","Mantém o arquivo, mas tira o item da lista do app.",false,()->{ DownloadStore.remove(this, id); refresh(); }));
        items.add(new MenuAction("copy","Copiar link original","Copia a URL usada para baixar este arquivo.",false,()->copyText("URL", entry.optString("url"))));
        showActionMenu(entry.optString("name"), "Escolha uma ação para este arquivo concluído.", items);
    }

    private static final class MenuAction {
        final String icon, title, subtitle;
        final boolean danger;
        final Runnable action;
        MenuAction(String icon, String title, String subtitle, boolean danger, Runnable action) {
            this.icon = icon; this.title = title; this.subtitle = subtitle; this.danger = danger; this.action = action;
        }
    }

    private void showActionMenu(String title, String subtitle, java.util.List<MenuAction> actions) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).create();
        LinearLayout root = Ui.vertical(this);
        root.setPadding(dp(18),dp(18),dp(18),dp(14));
        root.setBackground(Ui.shape(this, colors.surface, 24));
        TextView titleView = Ui.text(this, title, 17, colors.text, true);
        root.addView(titleView);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subtitleView = Ui.text(this, subtitle, 12, colors.muted, false);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,-2);
            sp.topMargin = dp(6);
            root.addView(subtitleView, sp);
        }
        android.widget.ScrollView scroller = new android.widget.ScrollView(this);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = Ui.vertical(this);
        LinearLayout.LayoutParams listP = new LinearLayout.LayoutParams(-1,-2);
        listP.topMargin = dp(14);
        for (MenuAction action : actions) {
            LinearLayout row = Ui.horizontal(this);
            row.setPadding(dp(14),dp(12),dp(14),dp(12));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.shape(this, colors.soft, 18));
            row.setClickable(true);
            row.setFocusable(true);
            int tint = action.danger ? colors.danger : colors.text;
            android.widget.FrameLayout badge = new android.widget.FrameLayout(this);
            badge.setBackground(Ui.shape(this, colors.surface, 14));
            badge.addView(Ui.icon(this, action.icon, tint, 20), new android.widget.FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
            row.addView(badge, new LinearLayout.LayoutParams(dp(40), dp(40)));
            LinearLayout labels = Ui.vertical(this);
            LinearLayout.LayoutParams labelsP = new LinearLayout.LayoutParams(0,-2,1);
            labelsP.leftMargin = dp(12);
            labels.addView(Ui.text(this, action.title, 14, tint, true));
            TextView sub = Ui.text(this, action.subtitle, 12, colors.muted, false);
            LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1,-2);
            subP.topMargin = dp(2);
            labels.addView(sub, subP);
            row.addView(labels, labelsP);
            row.addView(Ui.icon(this, "chevron", colors.muted, 16), new LinearLayout.LayoutParams(dp(16),dp(16)));
            row.setOnClickListener(v -> { dialog.dismiss(); action.action.run(); });
            LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(-1,-2);
            rowP.topMargin = dp(8);
            list.addView(row, rowP);
        }
        scroller.addView(list, new android.widget.FrameLayout.LayoutParams(-1,-2));
        root.addView(scroller, listP);
        TextView close = Ui.pill(this, "Fechar", colors.text, colors.soft);
        LinearLayout.LayoutParams closeP = new LinearLayout.LayoutParams(-2,dp(42));
        closeP.gravity = Gravity.END;
        closeP.topMargin = dp(12);
        close.setOnClickListener(v -> dialog.dismiss());
        root.addView(close, closeP);
        dialog.setView(root);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.92f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void copyText(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, "Texto copiado", Toast.LENGTH_SHORT).show();
    }

    private void controlDownload(String id,String action) {
        TransferStarter.control(this,id,action);
        handler.postDelayed(this::refresh,350);
    }

    private void confirmCancel(String id) {
        new AlertDialog.Builder(this).setTitle("Cancelar download?")
            .setMessage("O arquivo parcial será descartado. Esta ação não pode ser desfeita.")
            .setNegativeButton("Voltar", null)
            .setPositiveButton("Cancelar download", (dialog, which) ->
                    controlDownload(id, TransferService.ACTION_CANCEL)).show();
    }

    private void confirmRemoveEntry(JSONObject entry) {
        new AlertDialog.Builder(this).setTitle("Remover este registro?")
            .setMessage("Arquivos parciais deste download também serão descartados.")
            .setNegativeButton("Voltar", null)
            .setPositiveButton("Remover", (dialog, which) -> {
                String saved = entry.optString("uri");
                if (!saved.isEmpty()) {
                    try {
                        getContentResolver().delete(Uri.parse(saved), null, null);
                    } catch (Exception error) {
                        Toast.makeText(this, "Não foi possível excluir o fragmento", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                DownloadStore.remove(this, entry.optString("id"));
                refresh();
            }).show();
    }

    private void renameFile(JSONObject entry, Uri uri) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setText(entry.optString("name"));
        field.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Renomear arquivo")
            .setView(field)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (dialog, which) -> {
                String typed = field.getText().toString().trim();
                if (typed.isEmpty()) {
                    Toast.makeText(this, "Nome vazio", Toast.LENGTH_SHORT).show();
                    return;
                }
                String name = FileNames.clean(typed);
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                try {
                    if (getContentResolver().update(uri, values, null, null) != 1) {
                        throw new IllegalStateException("Arquivo não atualizado");
                    }
                    DownloadStore.update(this, entry.optString("id"), null, name, -1, -2, null, null);
                    refresh();
                    Toast.makeText(this, "Arquivo renomeado", Toast.LENGTH_SHORT).show();
                } catch (Exception error) {
                    Toast.makeText(this, "Não foi possível renomear o arquivo", Toast.LENGTH_LONG).show();
                }
            }).show();
    }

    private void confirmDeleteFile(JSONObject entry, Uri uri) {
        new AlertDialog.Builder(this).setTitle("Excluir arquivo da pasta Download?")
            .setMessage("O arquivo será apagado do armazenamento, não apenas da lista.")
            .setNegativeButton("Voltar", null)
            .setPositiveButton("Excluir", (dialog, which) -> {
                try {
                    if (getContentResolver().delete(uri, null, null) != 1)
                        throw new IllegalStateException("Arquivo não excluído");
                    DownloadStore.remove(this, entry.optString("id"));
                    refresh();
                    Toast.makeText(this, "Arquivo excluído", Toast.LENGTH_SHORT).show();
                } catch (Exception error) {
                    Toast.makeText(this, "Arquivo não encontrado ou sem permissão para excluir", Toast.LENGTH_LONG).show();
                }
            }).show();
    }

    private void retryDownload(JSONObject entry) {
        String url=entry.optString("url"),id=entry.optString("id");
        if(!TransferHttp.isWebUrl(url)||id.isEmpty()) {
            Toast.makeText(this,"O link anterior não é válido",Toast.LENGTH_LONG).show();return;
        }
        if(TransferStarter.isActive(id) || "Na fila".equals(entry.optString("status"))) {
            Toast.makeText(this,"Este download já está na fila",Toast.LENGTH_SHORT).show();return;
        }
        boolean ok=TransferStarter.enqueue(this,id,url,null,null,null);
        Toast.makeText(this,ok?"Retomada solicitada":"Android bloqueou a retomada",Toast.LENGTH_SHORT).show();
        refresh();
    }

    private String mime(Uri uri) {
        if(uri==null)return "*/*";
        try{
            String value = getContentResolver().getType(uri);
            return value == null ? "*/*" : value;
        }catch(SecurityException | IllegalArgumentException error){
            AppEventLog.error(this,"Arquivos",error);
            return "*/*";
        }
    }
    private Uri safeUri(String raw){
        if(raw==null)return null;
        String trimmed=raw.trim();
        if(trimmed.isEmpty())return null;
        try{
            Uri uri=Uri.parse(trimmed);
            String scheme=uri.getScheme();
            if(scheme==null)return null;
            if(!"content".equalsIgnoreCase(scheme) && !"file".equalsIgnoreCase(scheme))return null;
            return uri;
        }catch(RuntimeException error){
            AppEventLog.error(this,"Arquivos",error);
            return null;
        }
    }
    private String hostLabel(String url){
        try{
            java.net.URI uri=new java.net.URI(url);
            String host=uri.getHost();
            return host==null||host.isEmpty()?"Origem indisponível":host;
        }catch(Exception ignored){return "Origem indisponível";}
    }
    private void showMediaDetails(Uri uri,String filename){
        Toast.makeText(this,"Lendo detalhes locais do arquivo...",Toast.LENGTH_SHORT).show();
        mediaPreviewLoader.load(this,uri,filename,result->{
            if(isFinishing() || isDestroyed())return;
            new AlertDialog.Builder(this).setTitle("Detalhes: "+filename)
                .setMessage(result.details.isEmpty()?"Metadados não disponíveis para este arquivo.":result.details)
                .setPositiveButton("Fechar",null).show();
        });
    }
    private void playLocalVideo(Uri uri) {
        if(uri==null || !"content".equalsIgnoreCase(uri.getScheme()))return;
        Intent play=new Intent(this,MediaPlayerActivity.class);
        play.putExtra(MediaPlayerActivity.EXTRA_MEDIA_URL,uri.toString());
        play.putExtra(MediaPlayerActivity.EXTRA_LOCAL,true);
        play.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(play);
    }
    private void openFile(Uri uri) {
        if(uri==null){
            AppEventLog.warn(this,"Arquivos","Tentativa de abrir arquivo sem URI válida.");
            Toast.makeText(this, "Arquivo indisponível para abrir", Toast.LENGTH_LONG).show();
            return;
        }
        Intent open = new Intent(Intent.ACTION_VIEW);
        open.setDataAndType(uri, mime(uri));
        open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(Intent.createChooser(open, "Abrir com")); }
        catch (ActivityNotFoundException e) {
            AppEventLog.warn(this,"Arquivos","Nenhum app disponível para abrir o arquivo selecionado.");
            Toast.makeText(this, "Nenhum aplicativo pode abrir este arquivo", Toast.LENGTH_LONG).show();
        } catch (SecurityException | IllegalArgumentException | RuntimeException error){
            AppEventLog.error(this,"Arquivos",error);
            Toast.makeText(this, "Não foi possível abrir o arquivo", Toast.LENGTH_LONG).show();
        }
    }
    private void shareFile(Uri uri) {
        if(uri==null){
            AppEventLog.warn(this,"Arquivos","Tentativa de compartilhar arquivo sem URI válida.");
            Toast.makeText(this, "Arquivo indisponível para compartilhar", Toast.LENGTH_LONG).show();
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mime(uri));
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.setClipData(ClipData.newUri(getContentResolver(), "Download", uri));
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivity(Intent.createChooser(share, "Compartilhar arquivo"));}
        catch (ActivityNotFoundException error){
            AppEventLog.warn(this,"Arquivos","Nenhum app disponível para compartilhar o arquivo selecionado.");
            Toast.makeText(this, "Nenhum aplicativo aceitou o compartilhamento", Toast.LENGTH_LONG).show();
        } catch (SecurityException | IllegalArgumentException | RuntimeException error){
            AppEventLog.error(this,"Arquivos",error);
            Toast.makeText(this, "Não foi possível compartilhar o arquivo", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if(Ui.mode(this)!=appearanceMode){recreate();return;}
        if(browserPane!=null && BrowserPrefs.consumeClearRequest(this))browserPane.clearLocalData();
        if(browserPane!=null&&!downloadsVisible)browserPane.resume();
        handler.removeCallbacks(updateLoop);
        handler.post(updateLoop);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(updateLoop);
        if(browserPane!=null)browserPane.pause();
        super.onPause();
    }
    @Override public void onTrimMemory(int level){
        super.onTrimMemory(level);
        if(browserPane!=null)browserPane.onMemoryPressure(level);
    }
    @Override public void onLowMemory(){
        super.onLowMemory();
        if(browserPane!=null)browserPane.onMemoryPressure(15);
    }
    @Override protected void onDestroy(){
        mediaPreviewLoader.shutdown();
        if(Build.VERSION.SDK_INT>=33 && systemBackCallback!=null){
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(systemBackCallback);
            systemBackCallback=null;
        }
        if(browserPane!=null)browserPane.destroy();
        super.onDestroy();
    }

    private String readableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format(Locale.getDefault(),"%.0f KB",bytes/1024.0);
        if (bytes < 1073741824) return String.format(Locale.getDefault(),"%.1f MB",bytes/1048576.0);
        return String.format(Locale.getDefault(),"%.2f GB",bytes/1073741824.0);
    }
    private class DownloadAdapter extends BaseAdapter {
        private final List<JSONObject> data=new ArrayList<>();
        private final java.util.Map<String,long[]> speeds=new java.util.HashMap<>();
        void reload() {
            List<JSONObject> next=new ArrayList<>();
            boolean rateChanged=false;
            long now=android.os.SystemClock.elapsedRealtime();
            String query=searchInput==null?"":searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
            for(JSONObject item:DownloadStore.all(MainActivity.this)) {
                String status=item.optString("status");
                if("Arquivos".equals(listFilter)&&!"Concluído".equals(status)) continue;
                if("Fila".equals(listFilter) && ("Concluído".equals(status) || "Cancelado".equals(status) || "Falhou".equals(status) || "Interrompido".equals(status))) continue;
                if(!query.isEmpty() && !(item.optString("name")+" "+status+" "+item.optString("url")).toLowerCase(Locale.ROOT).contains(query))continue;
                String guess=item.optString("name");
                if("Preparando download".equals(guess)||guess.isEmpty()) guess=FileNames.choose(null,item.optString("url"),null);
                if(!"Todos".equals(typeFilter) && !typeFilter.equals(DownloadInfo.kind(guess)))continue;
                next.add(item);
                String id=item.optString("id");
                if(TransferStarter.isActive(id) && TransferStarter.queuePosition(MainActivity.this,id)<=0) {
                    long bytes=item.optLong("bytes",0);
                    long[] previous=speeds.get(id);
                    if(previous==null) speeds.put(id,new long[]{bytes,now,-1});
                    else if(now-previous[1]>=800) {
                        long instantaneous=bytes>=previous[0] ? (bytes-previous[0])*1000/Math.max(1,now-previous[1]) : -1;
                        long nextSpeed=instantaneous<0?-1:(previous[2]<0?instantaneous:(previous[2]*7+instantaneous*3)/10);
                        if(previous[2]!=nextSpeed) rateChanged=true;
                        previous[0]=bytes;previous[1]=now;previous[2]=nextSpeed;
                    }
                } else speeds.remove(id);
            }
            java.util.Comparator<JSONObject> ordering;
            if(sortMode==1) ordering=java.util.Comparator.comparingLong(row->row.optLong("time",0));
            else if(sortMode==2) ordering=java.util.Comparator.comparing(row->row.optString("name", "").toLowerCase(Locale.ROOT));
            else if(sortMode==3) ordering=(a,b)->Long.compare(b.optLong("total",-1),a.optLong("total",-1));
            else if(sortMode==4) ordering=(a,b)->Long.compare(a.optLong("total",-1)<0?Long.MAX_VALUE:a.optLong("total",-1),b.optLong("total",-1)<0?Long.MAX_VALUE:b.optLong("total",-1));
            else ordering=(a,b)->Long.compare(b.optLong("time",0),a.optLong("time",0));
            next.sort(ordering);
            if(!data.toString().equals(next.toString())||rateChanged) {
                data.clear();data.addAll(next);notifyDataSetChanged();
            }
        }
        @Override public int getCount(){return data.size();}
        @Override public JSONObject getItem(int pos){return data.get(pos);}
        @Override public long getItemId(int pos){return pos;}
        // 1 item ocupa cerca de 100 dp no modo compacto, contra os cartões altos anteriores.
        @Override public View getView(int pos,View convertView,ViewGroup parent){
            JSONObject entry=getItem(pos);
            String filename=entry.optString("name","Arquivo");
            String status=entry.optString("status");
            String id=entry.optString("id");
            long bytes=entry.optLong("bytes",0),total=entry.optLong("total",-1);
            boolean finished="Concluído".equals(status)&&!entry.optString("uri").isEmpty();
            boolean active=TransferStarter.isActive(id)||"Na fila".equals(status);
            boolean error="Falhou".equals(status)||"Interrompido".equals(status);
            int queuePosition=TransferStarter.queuePosition(MainActivity.this,id);
            int stateColor=error?colors.danger:finished?colors.teal:colors.warn;
            if(active&&queuePosition<=0)stateColor=colors.teal;
            boolean expanded=!compactDownloads||expandedDownloads.contains(id);
            String host="";
            try{host=new java.net.URI(entry.optString("url")).getHost();}catch(Exception ignored){}
            if(host==null||host.isEmpty())host="Origem indisponível";

            LinearLayout outer=Ui.vertical(MainActivity.this);
            outer.setPadding(dp(12),dp(3),dp(12),dp(3));
            LinearLayout card=Ui.vertical(MainActivity.this);
            card.setPadding(dp(11),dp(10),dp(10),dp(10));
            card.setBackground(Ui.shape(MainActivity.this,colors.surface,16));
            outer.addView(card,new LinearLayout.LayoutParams(-1,-2));
            card.setClickable(true);card.setFocusable(true);
            card.setContentDescription(filename+". "+status+". Toque para "+(expanded?"recolher":"expandir")+" detalhes.");
            card.setOnClickListener(v->{
                if(expandedDownloads.contains(id))expandedDownloads.remove(id);
                else expandedDownloads.add(id);
                notifyDataSetChanged();
            });

            LinearLayout head=Ui.horizontal(MainActivity.this);
            android.widget.FrameLayout fileIcon=new android.widget.FrameLayout(MainActivity.this);
            fileIcon.setBackground(Ui.shape(MainActivity.this,colors.soft,11));
            String type="Compactados".equals(DownloadInfo.kind(filename))?"archive":"file";
            fileIcon.addView(Ui.icon(MainActivity.this,type,colors.text,20),
                new android.widget.FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));
            head.addView(fileIcon,new LinearLayout.LayoutParams(dp(46),dp(46)));
            LinearLayout info=Ui.vertical(MainActivity.this);
            TextView filenameView=Ui.text(MainActivity.this,filename,14,colors.text,true);
            filenameView.setSingleLine(true);
            filenameView.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            info.addView(filenameView);
            TextView hostView=Ui.text(MainActivity.this,host,11,colors.muted,false);
            hostView.setSingleLine(true);
            hostView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(hostView,Ui.lp(MainActivity.this,-1,-2,1));
            LinearLayout.LayoutParams infoParams=new LinearLayout.LayoutParams(0,-2,1);
            infoParams.leftMargin=dp(9);head.addView(info,infoParams);
            android.widget.FrameLayout menu=iconButton("more","Mais ações de "+filename,()->showItemActions(entry));
            head.addView(menu,new LinearLayout.LayoutParams(dp(40),dp(40)));
            card.addView(head);
            if(finished && ("Vídeos".equals(DownloadInfo.kind(filename)) || "Imagens".equals(DownloadInfo.kind(filename)))) {
                String itemUri=entry.optString("uri");
                String sourceHost=host;
                Uri previewUri=safeUri(itemUri);
                fileIcon.setTag(itemUri);
                if(previewUri!=null)mediaPreviewLoader.load(MainActivity.this,previewUri,filename,result->{
                    if(!itemUri.equals(fileIcon.getTag()) || isFinishing() || isDestroyed())return;
                    if(result.bitmap!=null && !result.bitmap.isRecycled()){
                        fileIcon.removeAllViews();
                        ImageView preview=new ImageView(MainActivity.this);
                        preview.setImageBitmap(result.bitmap);
                        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        fileIcon.addView(preview,new android.widget.FrameLayout.LayoutParams(-1,-1));
                    }
                    if(!result.details.isEmpty())hostView.setText(sourceHost+" · "+result.details);
                });
            }

            String stateLabel=active?(queuePosition>0?"Na fila":"Baixando"):status;
            int percentage=total>0?(int)Math.min(100,bytes*100.0/total):0;
            LinearLayout stateRow=Ui.horizontal(MainActivity.this);
            stateRow.addView(Ui.text(MainActivity.this,stateLabel,12,stateColor,true),new LinearLayout.LayoutParams(0,dp(20),1));
            if(total>0)stateRow.addView(Ui.text(MainActivity.this,percentage+"%",12,colors.text,true));
            card.addView(stateRow,Ui.lp(MainActivity.this,-1,20,3));

            if(total>0||active){
                android.widget.ProgressBar progress=new android.widget.ProgressBar(MainActivity.this,null,android.R.attr.progressBarStyleHorizontal);
                progress.setMax(100);progress.setProgress(percentage);
                progress.setIndeterminate(total<=0&&active);
                progress.setProgressTintList(android.content.res.ColorStateList.valueOf(colors.teal));
                progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(colors.soft));
                progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(colors.teal));
                card.addView(progress,Ui.lp(MainActivity.this,-1,3,2));
            }

            LinearLayout bottom=Ui.horizontal(MainActivity.this);
            String amount=readableSize(bytes)+(total>0?" / "+readableSize(total):"");
            long[] rate=speeds.get(id);
            String rateText=queuePosition>0?"Fila #"+queuePosition:
                finished?"Pronto":active?(rate!=null&&rate[2]>0?readableSize(rate[2])+"/s":"Medindo..."):"";
            if(active&&queuePosition<=0&&rate!=null&&rate[2]>0){
                long seconds=DownloadInfo.etaSeconds(total,bytes,rate[2]);
                if(seconds>0)rateText+=" · "+DownloadInfo.etaLabel(seconds);
            }
            LinearLayout metrics=Ui.horizontal(MainActivity.this);
            TextView amountView=Ui.text(MainActivity.this,amount,11,colors.muted,false);
            amountView.setSingleLine(true);
            amountView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            metrics.addView(amountView,new LinearLayout.LayoutParams(-2,dp(33)));
            if(!rateText.isEmpty()){
                TextView rateView=Ui.text(MainActivity.this,"  ·  "+rateText,11,colors.muted,false);
                rateView.setSingleLine(true);
                rateView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                metrics.addView(rateView,new LinearLayout.LayoutParams(0,dp(33),1));
            }
            bottom.addView(metrics,new LinearLayout.LayoutParams(0,dp(36),1));

            // Ações com área de toque de 40 dp; o menu ⋮ mantém todas as operações.
            if(finished){
                final Uri itemUri=safeUri(entry.optString("uri"));
                bottom.addView(smallDownloadAction("file","Abrir "+filename,
                        ()->openFile(itemUri)),new LinearLayout.LayoutParams(dp(44),dp(44)));
                bottom.addView(smallDownloadAction("link","Compartilhar "+filename,
                        ()->shareFile(itemUri)),new LinearLayout.LayoutParams(dp(44),dp(44)));
            }else if(active){
                bottom.addView(smallDownloadAction("pause","Pausar "+filename,
                        ()->controlDownload(id,TransferService.ACTION_PAUSE)),new LinearLayout.LayoutParams(dp(40),dp(40)));
                bottom.addView(smallDownloadAction("x","Cancelar "+filename,
                        ()->confirmCancel(id)),new LinearLayout.LayoutParams(dp(40),dp(40)));
            }else if(!"Cancelado".equals(status)){
                bottom.addView(smallDownloadAction("play","Retomar "+filename,
                        ()->retryDownload(entry)),new LinearLayout.LayoutParams(dp(40),dp(40)));
            }
            card.addView(bottom,Ui.lp(MainActivity.this,-1,40,0));

            if(expanded){
                View separator=new View(MainActivity.this);
                separator.setBackgroundColor(colors.border);
                card.addView(separator,Ui.lp(MainActivity.this,-1,1,5));
                TextView more=Ui.text(MainActivity.this,
                        "Estado: "+status+"   •   "+amount+"\nOrigem: "+entry.optString("url"),
                        12,colors.muted,false);
                more.setMaxLines(4);
                more.setEllipsize(android.text.TextUtils.TruncateAt.END);
                more.setPadding(0,dp(8),0,dp(8));
                card.addView(more,new LinearLayout.LayoutParams(-1,-2));
                TextView hint=Ui.text(MainActivity.this,"Mais opções no menu ⋮",11,colors.muted,false);
                card.addView(hint,Ui.lp(MainActivity.this,-1,-2,1));
            }
            return outer;
        }
    }

    private android.widget.FrameLayout smallDownloadAction(String icon,String description,Runnable action){
        android.widget.FrameLayout box=new android.widget.FrameLayout(this);
        box.setBackground(Ui.shape(this,colors.soft,12));
        box.setContentDescription(description);box.setClickable(true);box.setFocusable(true);
        box.addView(Ui.icon(this,icon,colors.text,18),
            new android.widget.FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));
        box.setOnClickListener(v->action.run());
        return box;
    }
}
