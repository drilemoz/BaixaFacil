#!/usr/bin/env python3
"""Regressão alpha.54: exercita a regra Java real e contratos essenciais de Activity/Gecko.
Não substitui compilação Android nem teste de hardware.
"""
from pathlib import Path
import subprocess
import tempfile

root=Path(__file__).resolve().parents[1]
package=root/'app/src/main/java/br/com/baixafacil'
with tempfile.TemporaryDirectory() as temp:
    target=Path(temp)
    test=target/'GeckoPickerPolicyCheck.java'
    test.write_text('''package br.com.baixafacil;
import java.util.Arrays;
public class GeckoPickerPolicyCheck {
    static void yes(boolean result) { if(!result) throw new AssertionError(); }
    public static void main(String[] args) {
        yes(Arrays.equals(GeckoPickerPolicy.mimeTypes(null),new String[]{"*/*"}));
        yes(Arrays.equals(GeckoPickerPolicy.mimeTypes(new String[]{" IMAGE/PNG ","image/png","image/jpeg", "../bad", "text/plain\\nwrong"}),new String[]{"image/png","image/jpeg"}));
        yes(GeckoPickerPolicy.mimeTypes(new String[]{"audio/*"})[0].equals("audio/*"));
        yes(GeckoPickerPolicy.safeUriScheme("content"));
        yes(GeckoPickerPolicy.safeUriScheme("CONTENT"));
        yes(!GeckoPickerPolicy.safeUriScheme("file"));
        yes(!GeckoPickerPolicy.safeUriScheme("https"));
        String[] more=new String[40]; Arrays.fill(more,"text/plain");
        yes(GeckoPickerPolicy.mimeTypes(more).length==1);
    }
}''')
    result=subprocess.run(['javac','-d',str(target),str(package/'GeckoPickerPolicy.java'),str(test)],capture_output=True,text=True)
    assert result.returncode==0,result.stderr
    result=subprocess.run(['java','-cp',str(target),'br.com.baixafacil.GeckoPickerPolicyCheck'],capture_output=True,text=True)
    assert result.returncode==0,result.stderr

def read(name):return (package/name).read_text()
picker=read('GeckoFilePicker.java')
prompt=read('GeckoPromptHandler.java')
permission=read('GeckoSitePermissions.java')
pane=read('BrowserPane.java')
activity=read('MainActivity.java')
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
checks={
    'onFilePrompt abre SAF e onActivityResult devolve content URI': all(x in picker for x in ['ACTION_OPEN_DOCUMENT','CATEGORY_OPENABLE','EXTRA_ALLOW_MULTIPLE','prompt.confirm(host.getApplicationContext()','safeUriScheme','void cancel()']) and 'onFilePrompt(GeckoSession session,FilePrompt prompt)' in prompt and 'geckoPicker.onActivityResult' in activity,
    'sem amplo armazenamento nem caminhos locais': all(x not in picker for x in ['MANAGE_EXTERNAL_STORAGE','READ_EXTERNAL_STORAGE','_data','file://','ACTION_GET_CONTENT']),
    'permissões são solicitadas, não assumidas': all(x in permission for x in ['secureOrigin','requestPermissions(','onRequestPermissionsResult','onContentPermissionRequest','onMediaPermissionRequest','perm.notifyShown()','callback.reject()','SOURCE_CAMERA','SOURCE_MICROPHONE','SOURCE_SCREEN']) and all(x in manifest for x in ['ACCESS_COARSE_LOCATION','CAMERA','RECORD_AUDIO','android:required="false"']),
    'tela cheia restaura Android e abas': all(x in pane for x in ['onFullScreen(GeckoSession gecko,boolean fullScreen)','setVideoFullscreen(fullScreen)','exitVideoFullscreen()','onCrash','sitePermissions.cancel()']) and all(x in activity for x in ['setBrowserFullscreen(boolean enabled)','browserPane.exitVideoFullscreen();','browserPane.isVideoFullscreen()','FLAG_FULLSCREEN']),
    'popup não herda permissões principais': 'popup.session.setPromptDelegate' in pane and 'popup.session.setPermissionDelegate(sitePermissions)' not in pane,
}
for name,ok in checks.items():
    print(('OK' if ok else 'FALHOU')+': '+name)
assert all(checks.values())
print('OK: política MIME compilada e executada no JDK')
