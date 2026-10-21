#!/usr/bin/env python3
"""Regressão JDK real + JavaScript empacotado, sem Android SDK."""
from pathlib import Path
import subprocess, tempfile
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/br/com/baixafacil'
main=(java/'MainActivity.java').read_text(encoding='utf-8')
pane=(java/'BrowserPane.java').read_text(encoding='utf-8')
assert 'mediaQuickChips' in main and 'DownloadMediaFilter.matches(typeFilter,guess)' in main
assert 'adapter.reload(all)' in main and 'updateMediaQuickFilters(all)' in main
assert 'AppMenus.show(host,"Imagem "+index,\n                        "Origem: "+shownHost(chosen)' in pane
subprocess.run(['node',str(root/'tools/test_media_scanner.js')],check=True)
with tempfile.TemporaryDirectory() as temp:
    tmp=Path(temp)
    source=tmp/'MediaFilterTestMain.java'
    source.write_text('''import br.com.baixafacil.*;
import java.util.*;
public class MediaFilterTestMain {
  static void check(boolean value){if(!value)throw new AssertionError();}
  public static void main(String[] args){
    check(DownloadMediaFilter.matches("Mídias","clip.mp4"));
    check(DownloadMediaFilter.matches("Mídias","foto.jpg"));
    check(DownloadMediaFilter.matches("Mídias","musica.ogg"));
    check(!DownloadMediaFilter.matches("Mídias","arquivo.pdf"));
    check(DownloadMediaFilter.matches("Outros","arquivo.desconhecido"));
    check(!DownloadMediaFilter.matches("Imagens","video.mp4?token=x"));
    DownloadMediaFilter.Counts x=DownloadMediaFilter.count(Arrays.asList("a.mp4","b.jpg","c.mp3","d.pdf","e.mp4"));
    check(x.media==4 && x.videos==2 && x.images==1 && x.audio==1);
    check(DownloadMediaFilter.count(null).media==0);
  }
}''',encoding='utf-8')
    subprocess.run(['javac','-d',str(tmp),str(java/'DownloadInfo.java'),str(java/'DownloadMediaFilter.java'),str(source)],check=True)
    subprocess.run(['java','-cp',str(tmp),'MediaFilterTestMain'],check=True)
print('PASSOU: filtros de mídia Java reais, UI vinculada, scanner JS testado com DOM fictício')
