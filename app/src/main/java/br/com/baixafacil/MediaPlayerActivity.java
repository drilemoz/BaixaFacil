package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/** Player aberto somente por ação explícita e sem reutilizar cookies privados do navegador. */
public final class MediaPlayerActivity extends Activity {
    static final String EXTRA_MEDIA_URL="media_url";
    static final String EXTRA_LOCAL="local";
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView state;
    @Override public void onCreate(Bundle stateBundle){
        Ui.selectTheme(this);
        super.onCreate(stateBundle);
        Ui.Palette palette=new Ui.Palette(this);
        Ui.systemBars(this,palette);
        String address=getIntent().getStringExtra(EXTRA_MEDIA_URL);
        boolean local=getIntent().getBooleanExtra(EXTRA_LOCAL,false);
        if (!validSource(address,local)){
            Toast.makeText(this,"Arquivo ou endereço de vídeo não compatível",Toast.LENGTH_LONG).show();finish();return;
        }
        LinearLayout root=Ui.vertical(this);root.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,12));
        root.setBackgroundColor(palette.background);setContentView(root);
        LinearLayout head=Ui.horizontal(this);
        TextView back=Ui.pill(this,"‹ Voltar",palette.text,palette.surface);
        back.setOnClickListener(v->finish());head.addView(back,new LinearLayout.LayoutParams(-2,Ui.dp(this,44)));
        TextView title=Ui.text(this,"Reprodutor de vídeos",18,palette.text,true);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=Ui.dp(this,12);
        head.addView(title,tp);root.addView(head);
        playerView=new PlayerView(this);playerView.setUseController(true);
        playerView.setBackgroundColor(android.graphics.Color.BLACK);
        root.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));
        this.state=Ui.text(this,local?"Abrindo arquivo baixado...":"Conectando ao vídeo...",12,palette.muted,false);
        this.state.setPadding(Ui.dp(this,6),Ui.dp(this,10),Ui.dp(this,6),Ui.dp(this,10));
        root.addView(this.state,new LinearLayout.LayoutParams(-1,-2));
        TextView warning=Ui.text(this,"Streams HLS/DASH: reprodução online quando permitida pelo servidor. Sem suporte a DRM e sem downloads de playlists nesta versão.",11,palette.muted,false);
        root.addView(warning,new LinearLayout.LayoutParams(-1,-2));
        try{
            player=new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            player.addListener(new Player.Listener(){
                @Override public void onPlaybackStateChanged(int playbackState){
                    if(MediaPlayerActivity.this.state==null)return;
                    if(playbackState==Player.STATE_READY)MediaPlayerActivity.this.state.setText("Pronto para reprodução");
                    else if(playbackState==Player.STATE_BUFFERING)MediaPlayerActivity.this.state.setText("Carregando vídeo...");
                    else if(playbackState==Player.STATE_ENDED)MediaPlayerActivity.this.state.setText("Vídeo concluído");
                }
                @Override public void onPlayerError(PlaybackException error){
                    if(MediaPlayerActivity.this.state!=null)MediaPlayerActivity.this.state.setText("Falha na reprodução. O site pode exigir sessão, codec ou DRM indisponível.");
                }
            });
            MediaItem item=MediaItem.fromUri(Uri.parse(address));
            player.setMediaItem(item);
            player.prepare();player.play();
        }catch(RuntimeException error){
            new AlertDialog.Builder(this).setTitle("Não foi possível reproduzir")
                .setMessage("Este endereço não pôde ser aberto pelo reprodutor integrado.")
                .setPositiveButton("Fechar",(d,w)->finish()).show();
        }
    }
    private boolean validSource(String address,boolean local){
        if(address==null||address.length()>4096)return false;
        if(local){
            Uri uri=Uri.parse(address);
            return "content".equalsIgnoreCase(uri.getScheme()) && uri.getAuthority()!=null;
        }
        return MediaSourcePolicy.classify(address)!=MediaSourcePolicy.Kind.UNSUPPORTED;
    }
    @Override protected void onPause(){if(player!=null)player.pause();super.onPause();}
    @Override protected void onDestroy(){
        if(playerView!=null)playerView.setPlayer(null);
        if(player!=null){player.release();player=null;}
        super.onDestroy();
    }
}
