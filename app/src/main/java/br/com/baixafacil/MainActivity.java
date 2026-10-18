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
import android.widget.HorizontalScrollView;
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
    private final java.util.Set<String> selectedDownloadIds = new java.util.LinkedHashSet<>();
    private boolean selectingDownloads;
    private LinearLayout selectionBar;
    private TextView selectionCounter;

    // Compacto é padrão; a visualização detalhada expande as informações de cada item.
    private boolean compactDownloads = true;
    private String typeFilter = "Todos";
    private int sizeFilter = DownloadListFilters.ALL_SIZES;
    // null = todos, "" = origens sem host válido; nunca persistir URL ou tokens.
    private String originFilter = null;
    private LinearLayout activeFilterBar;
    private TextView activeFilterSummary;
    private TextView emptyTitle;
    private TextView emptySub;
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
        section.addView(iconButton("check","Selecionar arquivos",this::startDownloadSelection),new LinearLayout.LayoutParams(dp(44),dp(48)));
        section.addView(iconButton("filter","Filtrar e ordenar downloads",this::showOrganizeMenu),new LinearLayout.LayoutParams(dp(44),dp(48)));
        section.addView(iconButton("trash","Limpar downloads",this::showCleanDownloadsOptions),new LinearLayout.LayoutParams(dp(44),dp(48)));
        header.addView(section,Ui.lp(this,-1,52,12));

        // Barra contextual visível somente na seleção múltipla; rolagem horizontal em telas pequenas.
        selectionBar=Ui.vertical(this);
        selectionBar.setPadding(dp(10),dp(10),dp(10),dp(10));
        selectionBar.setBackground(Ui.shape(this,colors.surface,16));
        selectionCounter=Ui.text(this,"0 arquivos selecionados",14,colors.text,true);
        selectionBar.addView(selectionCounter);
        HorizontalScrollView actionScroll=new HorizontalScrollView(this);
        actionScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout batchActions=Ui.horizontal(this);
        addSelectionAction(batchActions,"Selecionar visíveis","check",this::selectVisibleDownloads);
        addSelectionAction(batchActions,"Enviar","link",this::shareSelectedDownloads);
        addSelectionAction(batchActions,"Remover da lista","folder",()->confirmSelectedCleanup(false));
        addSelectionAction(batchActions,"Apagar arquivos","trash",()->confirmSelectedCleanup(true));
        addSelectionAction(batchActions,"Sair","x",this::endDownloadSelection);
        actionScroll.addView(batchActions,new HorizontalScrollView.LayoutParams(-2,-2));
        selectionBar.addView(actionScroll,Ui.lp(this,-1,46,8));
        selectionBar.setVisibility(View.GONE);
        header.addView(selectionBar,Ui.lp(this,-1,-2,6));

        filterRow = Ui.horizontal(this);
        filterChips = new TextView[3];
        String[] labels = {"Todos","Em fila","Concluídos"};
        for (int i=0;i<labels.length;i++) {
            final String selection = i==0 ? "Todos" : (i==1 ? "Fila" : "Arquivos");
            TextView chip = Ui.pill(this,labels[i],colors.text,colors.surface);
            LinearLayout.LayoutParams chipParams = Ui.lp(this,-2,44,0);
            if (i > 0) chipParams.leftMargin=dp(8);
            filterRow.addView(chip,chipParams);filterChips[i]=chip;
            chip.setOnClickListener(v->{endDownloadSelection();listFilter=selection; updateFilterChips(); refresh();});
        }
        updateFilterChips();
        header.addView(filterRow,Ui.lp(this,-1,44,3));
        activeFilterBar=Ui.horizontal(this);
        activeFilterBar.setPadding(dp(12),dp(3),dp(8),dp(3));
        activeFilterBar.setBackground(Ui.shape(this,colors.soft,13));
        activeFilterSummary=Ui.text(this,"",12,colors.text,true);
        activeFilterSummary.setMaxLines(2);
        activeFilterSummary.setEllipsize(android.text.TextUtils.TruncateAt.END);
        activeFilterBar.addView(activeFilterSummary,new LinearLayout.LayoutParams(0,-2,1));
        TextView clearFilters=Ui.pill(this,"Limpar",colors.text,colors.surface);
        clearFilters.setContentDescription("Limpar os filtros de downloads");
        clearFilters.setOnClickListener(v->clearDownloadFilters());
        activeFilterBar.addView(clearFilters,new LinearLayout.LayoutParams(-2,dp(44)));
        activeFilterBar.setVisibility(View.GONE);
        header.addView(activeFilterBar,Ui.lp(this,-1,-2,7));
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);searchInput.setHint("Pesquisar por nome, status ou origem");
        searchInput.setTextSize(14);searchInput.setTextColor(colors.text);searchInput.setHintTextColor(colors.muted);
        searchInput.setPadding(dp(16),0,dp(12),0);searchInput.setBackground(Ui.shape(this,colors.surface,15));
        searchInput.setVisibility(View.GONE);
        header.addView(searchInput,Ui.lp(this,-1,48,10));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) {endDownloadSelection();refresh();}
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
        emptyTitle = Ui.text(this,"Seus downloads aparecerão aqui",17,colors.text,true);
        emptyTitle.setGravity(Gravity.CENTER);empty.addView(emptyTitle,Ui.lp(this,-1,-2,16));
        emptySub=Ui.text(this,"Cole um link ou use o navegador para começar.",13,colors.muted,false);
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
            if(index>=0 && index<adapter.getCount()) {
                JSONObject item=adapter.getItem(index);
                if(selectingDownloads)toggleDownloadSelection(item);
                else showItemActions(item);
            }
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
        endDownloadSelection();
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
        if(downloadsVisible && selectingDownloads){endDownloadSelection();return;}
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
        rows.add(AppMenus.Entry.item("Tamanho: "+DownloadListFilters.sizeLabel(sizeFilter),
            "Inclui tamanho desconhecido e faixas de até 10 MB, 10 a 100 MB ou 100 MB+","filter",this::chooseSizeFilter));
        rows.add(AppMenus.Entry.item("Site: "+(originFilter==null?"Todos":originFilter.isEmpty()?"Origem não identificada":originFilter),
            "Mostrar downloads pelo domínio de origem","globe",this::chooseOriginFilter));
        rows.add(AppMenus.Entry.item("Ordenação","Recentes, antigos, nome ou tamanho","menu",this::chooseSortMode));
        if(hasExtraFilters() || !"Todos".equals(listFilter) || (searchInput!=null && searchInput.length()>0))
            rows.add(AppMenus.Entry.item("Limpar filtros aplicados","Voltar a exibir todos os downloads","reload",this::clearDownloadFilters));
        AppMenus.show(this,"Organizar downloads","Filtros combináveis, apenas locais. Nenhuma transferência é alterada.",rows);
    }
    private boolean hasExtraFilters(){
        return !"Todos".equals(typeFilter) || sizeFilter!=DownloadListFilters.ALL_SIZES || originFilter!=null;
    }
    private void updateActiveFilterBar(){
        if(activeFilterBar==null || activeFilterSummary==null)return;
        if(!hasExtraFilters()){activeFilterBar.setVisibility(View.GONE);return;}
        StringBuilder text=new StringBuilder("Filtros: ");
        if(!"Todos".equals(typeFilter))text.append(typeFilter).append(" · ");
        if(sizeFilter!=DownloadListFilters.ALL_SIZES)text.append(DownloadListFilters.sizeLabel(sizeFilter)).append(" · ");
        if(originFilter!=null)text.append(originFilter.isEmpty()?"Origem não identificada":originFilter).append(" · ");
        String label=text.toString();
        if(label.endsWith(" · "))label=label.substring(0,label.length()-3);
        activeFilterSummary.setText(label);
        activeFilterBar.setVisibility(View.VISIBLE);
    }
    private void applyDownloadFilters(){
        endDownloadSelection();
        updateActiveFilterBar();
        refresh();
    }
    private void clearDownloadFilters(){
        endDownloadSelection();
        typeFilter="Todos";
        sizeFilter=DownloadListFilters.ALL_SIZES;
        originFilter=null;
        listFilter="Todos";
        updateFilterChips();
        if(searchInput!=null)searchInput.setText("");
        updateActiveFilterBar();
        refresh();
    }
    private void chooseSizeFilter(){
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(int mode=DownloadListFilters.ALL_SIZES;mode<=DownloadListFilters.FROM_100_MB;mode++){
            final int choice=mode;
            String title=DownloadListFilters.sizeLabel(mode);
            String hint=mode==DownloadListFilters.UNKNOWN_SIZE?"Para arquivos ainda sem tamanho informado pelo servidor":
                mode==DownloadListFilters.ALL_SIZES?"Mostrar todos os tamanhos":"Usa o tamanho total quando conhecido";
            rows.add(AppMenus.Entry.item(title+(sizeFilter==mode?" • atual":""),hint,"filter",()->{
                sizeFilter=choice;applyDownloadFilters();
            }));
        }
        AppMenus.show(this,"Filtrar por tamanho","Itens em andamento sem tamanho total não são classificados pelos bytes parciais.",rows);
    }
    private void chooseOriginFilter(){
        List<String> urls=new ArrayList<>();
        for(JSONObject item:DownloadStore.all(this))urls.add(item.optString("url"));
        List<DownloadListFilters.OriginCount> origins=DownloadListFilters.countOrigins(urls);
        List<AppMenus.Entry> rows=AppMenus.entries();
        rows.add(AppMenus.Entry.item("Todos os sites"+(originFilter==null?" • atual":""),
            "Sem filtro de origem","globe",()->{originFilter=null;applyDownloadFilters();}));
        for(int i=0;i<Math.min(30,origins.size());i++){
            DownloadListFilters.OriginCount item=origins.get(i);
            String domain=item.host;
            rows.add(AppMenus.Entry.item(item.label()+(domain.equals(originFilter)?" • atual":""),
                item.count+" registro(s) neste site","globe",()->{originFilter=domain;applyDownloadFilters();}));
        }
        if(origins.size()>30)rows.add(AppMenus.Entry.note("Exibidos os 30 sites mais frequentes. Use a pesquisa para localizar arquivos de outros sites."));
        AppMenus.show(this,"Filtrar por site",origins.isEmpty()?"Ainda não há origens no histórico.":
            "Os nomes são obtidos somente do histórico local, sem acessar os sites nem expor parâmetros de links.",rows);
    }
    private void addSelectionAction(LinearLayout row,String title,String glyph,Runnable click){
        Ui.Palette p=colors;
        LinearLayout action=Ui.action(this,title,glyph,p.text,p.soft,12);
        action.setPadding(dp(10),0,dp(10),0);
        action.setOnClickListener(v->click.run());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,dp(44));
        params.rightMargin=dp(7);
        row.addView(action,params);
    }

    private void startDownloadSelection(){
        selectingDownloads=true;
        selectedDownloadIds.clear();
        selectionBar.setVisibility(View.VISIBLE);
        updateSelectionCount();
        if(adapter!=null)adapter.notifyDataSetChanged();
        Toast.makeText(this,"Selecione arquivos concluídos. Toque longo também seleciona.",Toast.LENGTH_SHORT).show();
    }
    private void endDownloadSelection(){
        if(!selectingDownloads)return;
        selectingDownloads=false;
        selectedDownloadIds.clear();
        if(selectionBar!=null)selectionBar.setVisibility(View.GONE);
        if(adapter!=null)adapter.notifyDataSetChanged();
    }
    private void updateSelectionCount(){
        if(selectionCounter!=null)selectionCounter.setText(selectedDownloadIds.size()+" arquivo(s) selecionado(s)");
    }
    private void toggleDownloadSelection(JSONObject row){
        if(row==null)return;
        String id=row.optString("id");
        JSONObject current=DownloadStore.get(this,id);
        if(current==null || !DownloadSelectionPolicy.selectable(current.optString("status"),current.optString("uri"))){
            Toast.makeText(this,"Seleção disponível apenas para arquivos concluídos",Toast.LENGTH_SHORT).show();
            return;
        }
        if(!selectingDownloads){ selectingDownloads=true; selectionBar.setVisibility(View.VISIBLE); }
        if(!selectedDownloadIds.add(id))selectedDownloadIds.remove(id);
        updateSelectionCount();
        adapter.notifyDataSetChanged();
    }
    private void selectVisibleDownloads(){
        if(adapter==null)return;
        int added=0;
        for(int i=0;i<adapter.getCount();i++){
            JSONObject row=adapter.getItem(i);
            if(DownloadSelectionPolicy.selectable(row.optString("status"),row.optString("uri"))){
                if(selectedDownloadIds.add(row.optString("id")))added++;
            }
        }
        updateSelectionCount();adapter.notifyDataSetChanged();
        if(added==0)Toast.makeText(this,"Nenhum arquivo novo selecionável neste filtro",Toast.LENGTH_SHORT).show();
    }
    private java.util.List<JSONObject> selectedCompletedRows(){
        java.util.List<JSONObject> rows=new java.util.ArrayList<>();
        for(String id:new java.util.ArrayList<>(selectedDownloadIds)){
            JSONObject current=DownloadStore.get(this,id);
            if(current!=null && DownloadSelectionPolicy.selectable(current.optString("status"),current.optString("uri")))rows.add(current);
        }
        return rows;
    }
    private void shareSelectedDownloads(){
        java.util.List<JSONObject> rows=selectedCompletedRows();
        if(rows.isEmpty()){Toast.makeText(this,"Selecione ao menos um arquivo concluído",Toast.LENGTH_SHORT).show();return;}
        if(rows.size()>DownloadSelectionPolicy.MAX_SHARE_ITEMS){
            Toast.makeText(this,"Selecione até "+DownloadSelectionPolicy.MAX_SHARE_ITEMS+" arquivos para compartilhar de uma vez",Toast.LENGTH_LONG).show();return;
        }
        java.util.ArrayList<Uri> uris=new java.util.ArrayList<>();
        for(JSONObject row:rows){
            Uri uri=safeUri(row.optString("uri"));
            if(uri!=null && "content".equalsIgnoreCase(uri.getScheme()) && !uris.contains(uri))uris.add(uri);
        }
        if(uris.isEmpty()){Toast.makeText(this,"Arquivos selecionados indisponíveis",Toast.LENGTH_SHORT).show();return;}
        try{
            Intent intent=new Intent(uris.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE);
            intent.setType("*/*");
            if(uris.size()==1)intent.putExtra(Intent.EXTRA_STREAM,uris.get(0));
            else intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);
            ClipData clip=ClipData.newUri(getContentResolver(),"Arquivos Baixa Fácil",uris.get(0));
            for(int i=1;i<uris.size();i++)clip.addItem(new ClipData.Item(uris.get(i)));
            intent.setClipData(clip);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent,"Enviar arquivos selecionados"));
        }catch(RuntimeException error){
            AppEventLog.error(this,"Compartilhamento múltiplo",error);
            Toast.makeText(this,"Não foi possível abrir o compartilhamento",Toast.LENGTH_LONG).show();
        }
    }
    private void confirmSelectedCleanup(boolean deleteFiles){
        java.util.List<JSONObject> rows=selectedCompletedRows();
        if(rows.isEmpty()){Toast.makeText(this,"Nenhum arquivo concluído selecionado",Toast.LENGTH_SHORT).show();return;}
        final java.util.Set<String> ids=new java.util.LinkedHashSet<>();
        for(JSONObject row:rows)ids.add(row.optString("id"));
        List<AppMenus.Entry> actions=AppMenus.entries();
        actions.add(AppMenus.Entry.danger(deleteFiles?"Apagar arquivos selecionados":"Remover registros selecionados",
            deleteFiles?"Excluir definitivamente "+ids.size()+" arquivo(s) do armazenamento":"Manter os arquivos; remover "+ids.size()+" registro(s)","trash",()->runSelectedCleanup(ids,deleteFiles)));
        AppMenus.info(this,deleteFiles?"Apagar "+ids.size()+" arquivo(s)?":"Remover "+ids.size()+" da lista?",
            deleteFiles?"A operação apaga os arquivos concluídos do MediaStore. Se o Android negar acesso, o registro será preservado. Não pode ser desfeita.":
            "Apenas os registros selecionados serão retirados do histórico. Os arquivos permanecem na pasta Download.",actions);
    }
    private void runSelectedCleanup(java.util.Set<String> ids,boolean deleteFiles){
        Toast.makeText(this,"Processando arquivos selecionados…",Toast.LENGTH_SHORT).show();
        final java.util.Set<String> snapshot=new java.util.LinkedHashSet<>(ids);
        new Thread(()->{
            DownloadCleanup.Report report=DownloadCleanup.runSelected(getApplicationContext(),snapshot,deleteFiles);
            handler.post(()->{
                if(isFinishing()||isDestroyed())return;
                endDownloadSelection();refresh();
                String result=report.removed+" registro(s) removido(s)";
                if(deleteFiles)result+=" • "+report.deletedFiles+" arquivo(s) apagado(s)";
                if(report.failedFiles>0)result+=" • "+report.failedFiles+" sem permissão ou não encontrados";
                Toast.makeText(this,result,Toast.LENGTH_LONG).show();
            });
        },"selected-download-cleanup").start();
    }

    private void showCleanDownloadsOptions() {
        int eligible=0, protectedCount=0, completed=0;
        for(JSONObject row:DownloadStore.all(this)) {
            String status=row.optString("status");
            if(DownloadCleanupPolicy.eligible(status,!row.optString("uri").isEmpty())) {
                eligible++;
                if("Concluído".equals(status))completed++;
            } else protectedCount++;
        }
        final int total=eligible, files=completed;
        List<AppMenus.Entry> actions=AppMenus.entries();
        actions.add(AppMenus.Entry.item("Limpar lista (manter arquivos)",
            total+" registros finalizados; nenhum arquivo será apagado","folder",()->confirmClearDownloads(false,total)));
        actions.add(AppMenus.Entry.danger("Apagar arquivos e limpar lista",
            files+" concluídos; exclusão definitiva do armazenamento","trash",()->confirmClearDownloads(true,total)));
        AppMenus.info(this,"Limpar downloads",
            "Ações aplicadas a downloads terminados. Transferências em andamento, na fila, pausadas ou com partes recuperáveis são protegidas ("+protectedCount+" registros). A limpeza da lista mantém os arquivos físicos.",actions);
    }

    private void confirmClearDownloads(boolean deleteFiles,int count) {
        if(count==0){Toast.makeText(this,"Não há downloads finalizados para limpar",Toast.LENGTH_LONG).show();return;}
        List<AppMenus.Entry> actions=AppMenus.entries();
        actions.add(AppMenus.Entry.danger(deleteFiles?"Confirmar exclusão de arquivos":"Confirmar limpeza da lista",
            deleteFiles?"Arquivos concluídos serão apagados definitivamente":"Os arquivos baixados permanecerão no dispositivo",
            "trash",()->runDownloadCleanup(deleteFiles)));
        AppMenus.info(this,deleteFiles?"Excluir arquivos e registros?":"Limpar histórico de downloads?",
            deleteFiles?"Arquivos baixados e os registros correspondentes serão excluídos. Arquivos sem permissão ou que falharem permanecerão na lista. Esta ação não pode ser desfeita.":
            "Remove os registros de downloads concluídos, cancelados e falhas sem arquivo parcial. Arquivos baixados ficam na pasta Download. Não afeta fila nem pausados.",actions);
    }

    private void runDownloadCleanup(boolean deleteFiles) {
        Toast.makeText(this,"Limpando downloads...",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            DownloadCleanup.Report report=DownloadCleanup.run(getApplicationContext(),deleteFiles);
            handler.post(()->{
                if(isFinishing() || isDestroyed())return;
                refresh();
                String message=report.removed+" registros removidos";
                if(deleteFiles) message+=" • "+report.deletedFiles+" arquivos apagados";
                if(report.failedFiles>0)message+=" • "+report.failedFiles+" não apagados";
                Toast.makeText(this,message,Toast.LENGTH_LONG).show();
            });
        },"download-cleanup").start();
    }

    private void chooseFileType() {
        final String[] types={"Todos","Aplicativos","Vídeos","Áudios","Imagens","Documentos","Compactados","Outros"};
        List<AppMenus.Entry> rows=AppMenus.entries();
        for(String type:types){
            rows.add(AppMenus.Entry.item(type+(type.equals(typeFilter)?" • atual":""),
                "Filtrar a lista de downloads","file",()->{typeFilter=type;applyDownloadFilters();}));
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
        if(DownloadDuplicateUi.maybeShow(this,link,()->enqueueNew(link),this::showDownloads))return;
        enqueueNew(link);
    }
    private void enqueueNew(String link) {
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
        updateActiveFilterBar();
        if(selectingDownloads){
            // IDs inválidos/alterados nunca sobrevivem na seleção.
            selectedDownloadIds.removeIf(id->{
                JSONObject row=DownloadStore.get(this,id);
                return row==null || !DownloadSelectionPolicy.selectable(row.optString("status"),row.optString("uri"));
            });
            updateSelectionCount();
        }
        if (recoverQueueAction != null) {
            int count=DownloadStore.recoverable(this).size();
            recoverQueueAction.setText("Recuperar fila ("+count+")");
            recoverQueueAction.setVisibility(count>0?View.VISIBLE:View.GONE);
        }
        if (emptyState != null) {
            boolean empty=adapter.getCount()==0;
            emptyState.setVisibility(empty?View.VISIBLE:View.GONE);
            if(empty && emptyTitle!=null && emptySub!=null){
                boolean hasHistory=!DownloadStore.all(this).isEmpty();
                emptyTitle.setText(hasHistory?"Nenhum download neste filtro":"Seus downloads aparecerão aqui");
                emptySub.setText(hasHistory?"Experimente limpar os filtros ou mudar a pesquisa.":"Cole um link ou use o navegador para começar.");
            }
        }
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
        }catch(RuntimeException error){
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
            // MediaStore retorna content://. Não compartilhar file:// (FileUriExposedException).
            if(!"content".equalsIgnoreCase(scheme) || uri.getAuthority()==null || uri.getAuthority().isEmpty())return null;
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
        try { startActivity(play); }
        catch(RuntimeException error){
            AppEventLog.error(this,"Player",error);
            Toast.makeText(this,"Não foi possível abrir o reprodutor",Toast.LENGTH_LONG).show();
        }
    }
    private void openFile(Uri uri) {
        if(uri==null){
            AppEventLog.warn(this,"Arquivos","Tentativa de abrir arquivo sem URI válida.");
            Toast.makeText(this, "Arquivo indisponível para abrir", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setDataAndType(uri, mime(uri));
            open.setClipData(ClipData.newUri(getContentResolver(), "Arquivo", uri));
            open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(open, "Abrir com"));
        }
        catch (ActivityNotFoundException e) {
            AppEventLog.warn(this,"Arquivos","Nenhum app disponível para abrir o arquivo selecionado.");
            Toast.makeText(this, "Nenhum aplicativo pode abrir este arquivo", Toast.LENGTH_LONG).show();
        } catch (RuntimeException error){
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
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(mime(uri));
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setClipData(ClipData.newUri(getContentResolver(), "Download", uri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Compartilhar arquivo"));
        }
        catch (ActivityNotFoundException error){
            AppEventLog.warn(this,"Arquivos","Nenhum app disponível para compartilhar o arquivo selecionado.");
            Toast.makeText(this, "Nenhum aplicativo aceitou o compartilhamento", Toast.LENGTH_LONG).show();
        } catch (RuntimeException error){
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
                if(!DownloadListFilters.matchesSize(sizeFilter,item.optLong("total",-1),item.optLong("bytes",0),status))continue;
                if(!DownloadListFilters.matchesOrigin(originFilter,item.optString("url")))continue;
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
            boolean checked=selectedDownloadIds.contains(id);
            card.setBackground(selectingDownloads && checked ?
                Ui.outline(MainActivity.this,colors.soft,16,colors.teal) : Ui.shape(MainActivity.this,colors.surface,16));
            outer.addView(card,new LinearLayout.LayoutParams(-1,-2));
            card.setClickable(true);card.setFocusable(true);
            card.setContentDescription(filename+". "+status+". "+(selectingDownloads?(checked?"Selecionado":"Toque para selecionar"):
                "Toque para "+(expanded?"recolher":"expandir")+" detalhes."));
            card.setOnLongClickListener(v->{toggleDownloadSelection(entry);return true;});
            card.setOnClickListener(v->{
                if(selectingDownloads){toggleDownloadSelection(entry);return;}
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
            if(selectingDownloads){
                android.widget.FrameLayout selectionIcon=new android.widget.FrameLayout(MainActivity.this);
                selectionIcon.setBackground(Ui.outline(MainActivity.this,checked?colors.teal:colors.soft,8,checked?colors.teal:colors.border));
                if(checked)selectionIcon.addView(Ui.icon(MainActivity.this,"check",Color.WHITE,18),
                    new android.widget.FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));
                LinearLayout.LayoutParams iconP=new LinearLayout.LayoutParams(dp(27),dp(27));
                iconP.leftMargin=dp(5);
                head.addView(selectionIcon,iconP);
            }
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
            if(!selectingDownloads){
                android.widget.FrameLayout menu=iconButton("more","Mais ações de "+filename,()->showItemActions(entry));
                head.addView(menu,new LinearLayout.LayoutParams(dp(40),dp(40)));
            }
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

            // Atalhos com ícones e rótulos: toque >= 44 dp, espaço entre botões.
            if(selectingDownloads){
                TextView hint=Ui.text(MainActivity.this,DownloadSelectionPolicy.selectable(status,entry.optString("uri"))?
                    (checked?"Selecionado • toque para desmarcar":"Toque para selecionar este arquivo"):
                    "Item protegido: não entra na seleção",11,colors.muted,false);
                bottom.addView(hint,new LinearLayout.LayoutParams(0,dp(44),1));
            }else if(finished){
                final Uri itemUri=safeUri(entry.optString("uri"));
                LinearLayout.LayoutParams openP=new LinearLayout.LayoutParams(-2,dp(44));
                openP.leftMargin=dp(6);
                bottom.addView(labeledDownloadAction("file","Abrir","Abrir "+filename,()->openFile(itemUri)),openP);
                LinearLayout.LayoutParams shareP=new LinearLayout.LayoutParams(-2,dp(44));
                shareP.leftMargin=dp(7);
                bottom.addView(labeledDownloadAction("link","Enviar","Compartilhar "+filename,()->shareFile(itemUri)),shareP);
            }else if(active){
                bottom.addView(smallDownloadAction("pause","Pausar "+filename,
                        ()->controlDownload(id,TransferService.ACTION_PAUSE)),new LinearLayout.LayoutParams(dp(40),dp(40)));
                bottom.addView(smallDownloadAction("x","Cancelar "+filename,
                        ()->confirmCancel(id)),new LinearLayout.LayoutParams(dp(40),dp(40)));
            }else if(!"Cancelado".equals(status)){
                bottom.addView(smallDownloadAction("play","Retomar "+filename,
                        ()->retryDownload(entry)),new LinearLayout.LayoutParams(dp(40),dp(40)));
            }
            card.addView(bottom,Ui.lp(MainActivity.this,-1,44,0));

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

    private LinearLayout labeledDownloadAction(String icon,String label,String description,Runnable action){
        LinearLayout row=Ui.horizontal(this);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(10),0,dp(11),0);
        row.setBackground(Ui.shape(this,colors.soft,14));
        row.setContentDescription(description);
        row.setClickable(true);row.setFocusable(true);
        row.addView(Ui.icon(this,icon,colors.teal,17),new LinearLayout.LayoutParams(dp(18),dp(18)));
        TextView title=Ui.text(this,label,12,colors.text,true);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.leftMargin=dp(6);
        row.addView(title,p);
        row.setOnClickListener(v->action.run());
        return row;
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
