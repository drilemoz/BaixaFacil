#!/usr/bin/env python3
"""Compila a política compartilhada real e checa a ligação das DUAS sessões Gecko."""
from pathlib import Path
import subprocess, tempfile

root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/baixafacil'
pane=(src/'BrowserPane.java').read_text(encoding='utf-8')
popup=pane[pane.index('private GeckoResult<GeckoSession> createLoginWindow('):]
assert 'BrowserContentGate.blocked(uri,ContentPrefs.level(host))' in popup
assert 'BrowserContentGate.evaluate(request.uri,ContentPrefs.level(host))' in popup
assert 'onSubframeLoadRequest' in popup and 'BrowserContentGate.blocked(request.uri,ContentPrefs.level(host))' in popup
assert 'BrowserContentGate.evaluate(url,ContentPrefs.level(host))' in pane[:pane.index('private GeckoResult<GeckoSession> createLoginWindow(')]
assert 'if(loginPopup==popup && popup.session==gecko && gecko.isOpen())popup.loadUrl(decision.url);' in popup
with tempfile.TemporaryDirectory() as td:
    p=Path(td)/'GateTest.java'
    p.write_text('''package br.com.baixafacil;
public class GateTest {
 static void eq(boolean b,String tag){if(!b)throw new AssertionError(tag);}
 public static void main(String[] ignored){
  for(int level:new int[]{AdultContentPolicy.MODERATE,AdultContentPolicy.STRICT}){
   eq(BrowserContentGate.evaluate("https://sub.pornhub.com/",level).action==BrowserContentGate.Action.BLOCK,"blocked primary+popup");
   eq(BrowserContentGate.evaluate("https://pornhub。com/path",level).action==BrowserContentGate.Action.BLOCK,"unicode IDNA dot");
   eq(BrowserContentGate.evaluate("https://pornhub．com/path",level).action==BrowserContentGate.Action.BLOCK,"fullwidth IDNA dot");
   eq(BrowserContentGate.evaluate("https://pornhub.com.:443/a",level).action==BrowserContentGate.Action.BLOCK,"unicode path + trailing dot");
   eq(BrowserContentGate.evaluate("https://pornhub.com.evil.example/a",level).action==BrowserContentGate.Action.ALLOW,"suffix false positive");
   eq(BrowserContentGate.evaluate("https://example.org/a/pornhub.com",level).action==BrowserContentGate.Action.ALLOW,"path false positive");
   BrowserContentGate.Decision d=BrowserContentGate.evaluate("https://google.com/search?q=safeguard&safe=off",level);
   eq(d.action==BrowserContentGate.Action.REWRITE && d.url.endsWith("safe=active"),"rewrite google");
   eq(BrowserContentGate.evaluate(d.url,level).action==BrowserContentGate.Action.ALLOW,"idempotent");
  }
  eq(BrowserContentGate.evaluate("https://pornhub.com",AdultContentPolicy.OFF).action==BrowserContentGate.Action.ALLOW,"off");
  eq(BrowserContentGate.evaluate("https://google.com/search?q=x",AdultContentPolicy.OFF).action==BrowserContentGate.Action.ALLOW,"safe search off");
  eq(BrowserContentGate.evaluate("https://onlyfans.com",AdultContentPolicy.MODERATE).action==BrowserContentGate.Action.ALLOW,"moderate");
  eq(BrowserContentGate.evaluate("https://onlyfans.com",AdultContentPolicy.STRICT).action==BrowserContentGate.Action.BLOCK,"strict");
  System.out.println("PASSOU: política compartilhada principal/popup, unicode, pesquisa, regressões.");
 }
}''',encoding='utf-8')
    subprocess.run(['javac','-encoding','UTF-8','--release','17','-d',td,str(src/'AdultContentPolicy.java'),str(src/'BrowserContentGate.java'),str(p)],check=True)
    subprocess.run(['java','-cp',td,'br.com.baixafacil.GateTest'],check=True)
