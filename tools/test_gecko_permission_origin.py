#!/usr/bin/env python3
"""Executa a política real de permissões (JDK), mais testes de integração estática.
Não substitui testes instrumentados GeckoView/Android.
"""
from pathlib import Path
import subprocess
import tempfile

root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/br/com/baixafacil'
with tempfile.TemporaryDirectory(prefix='bf66_policy_') as temp:
    check=Path(temp)/'PermissionCheck.java'
    check.write_text('''package br.com.baixafacil;
public class PermissionCheck {
  static void yes(boolean ok){if(!ok)throw new AssertionError();}
  public static void main(String[] args){
    yes(GeckoPermissionPolicy.sameOrigin("https://Site.org/a?token=SECRET", "https://site.org/b"));
    yes(GeckoPermissionPolicy.sameOrigin("https://site.org:443", "https://site.org/"));
    yes(GeckoPermissionPolicy.sameOrigin("https://site.org:8080/", "https://SITE.ORG:8080/"));
    yes(!GeckoPermissionPolicy.sameOrigin("https://site.org/", "https://other.org/"));
    yes(!GeckoPermissionPolicy.sameOrigin("https://site.org/", "http://site.org/"));
    yes(!GeckoPermissionPolicy.sameOrigin("https://site.org:8443/", "https://site.org/"));
    yes(!GeckoPermissionPolicy.sameOrigin("https://user:pw@site.org/", "https://site.org/"));
    yes(GeckoPermissionPolicy.origin("https://site.org:0")==null);
    yes(GeckoPermissionPolicy.origin("https://site.org:65536")==null);
    yes(GeckoPermissionPolicy.origin("blob:https://site.org/id")==null);
    yes(GeckoPermissionPolicy.origin("about:blank")==null);
    yes(GeckoPermissionPolicy.origin(null)==null);
    yes(GeckoPermissionPolicy.origin("https://site.org/path?access_token=ABC").equals("https://site.org:443"));
    System.out.println("GeckoPermissionPolicy: OK");
  }
}
''',encoding='utf-8')
    subprocess.run(['javac','-encoding','UTF-8','-d',temp,str(base/'GeckoPermissionPolicy.java'),str(check)],check=True)
    subprocess.run(['java','-cp',temp,'br.com.baixafacil.PermissionCheck'],check=True)
site=(base/'GeckoSitePermissions.java').read_text()
pane=(base/'BrowserPane.java').read_text()
prompt=(base/'GeckoPromptHandler.java').read_text()
assert 'sitePermissions.onPageLocationChanged(url)' in pane
assert 'void onPageLocationChanged(String destination)' in site
assert 'requestedAndroidOrigin' in site
assert 'activeFor(perm.uri)' in site and 'activeFor(uri)' in site
assert 'activeFor(requestedOrigin)' in site
assert 'contentOrigins.remove(result)' in site and 'mediaOrigins.remove(callback)' in site
assert 'dialogOrigins' in site and 'sameOrigin(dialogOrigins.get(dialog),destination)' in site
assert 'prompt.type==ChoicePrompt.Type.MULTIPLE' in prompt
assert 'row.setEnabled(isEnabled(position))' in prompt
assert 'getListView().setItemChecked(index,selected[index])' in prompt
assert 'parentDisabled || choice.disabled' in prompt
print('Gecko permission/form integration contracts: OK')
