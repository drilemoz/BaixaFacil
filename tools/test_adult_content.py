#!/usr/bin/env python3
"""Compila e testa o classificador real; não usa stubs do Android."""
from pathlib import Path
import tempfile, subprocess
ROOT=Path(__file__).resolve().parents[1]
policy=ROOT/'app/src/main/java/br/com/baixafacil/AdultContentPolicy.java'
with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    test=root/'AdultContentTest.java'
    test.write_text('''package br.com.baixafacil;
public final class AdultContentTest {
  static void expect(boolean b,String what){ if(!b)throw new AssertionError(what); }
  public static void main(String[] args){
    int off=AdultContentPolicy.OFF,moderate=AdultContentPolicy.MODERATE,strict=AdultContentPolicy.STRICT;
    expect(!AdultContentPolicy.blocked("https://pornhub.com",off),"off must not block");
    expect(AdultContentPolicy.blocked("https://pornhub.com",moderate),"known");
    expect(AdultContentPolicy.blocked("https://www.xvideos.com/path?q=x",moderate),"subdomain");
    expect(AdultContentPolicy.blocked("http://xnxx.com.:8080/",strict),"trailing dot");
    expect(AdultContentPolicy.blocked("https://onlyfans.com",strict),"strict");
    expect(!AdultContentPolicy.blocked("https://onlyfans.com",moderate),"strict only");
    for(String url : new String[]{"https://notpornhub.com", "https://pornhub.com.example.org", "https://example.com/path/pornhub.com",
      "https://google.com", "https://notxnxx.com", "http://127.0.0.1:8000/", "https://localhost/"})
      expect(!AdultContentPolicy.blocked(url,strict),"false positive "+url);
    expect(!AdultContentPolicy.blocked("file:///pornhub.com",strict),"file forbidden by other policies");
    expect(!AdultContentPolicy.blocked("https://user:pass@pornhub.com/",strict),"userinfo excluded");
    String g=AdultContentPolicy.safeSearchUrl("https://www.google.com/search?q=a%20b&safe=off#top",moderate);
    expect(g.equals("https://www.google.com/search?q=a%20b&safe=active#top"),"google rewrite: "+g);
    expect(g.equals(AdultContentPolicy.safeSearchUrl(g,moderate)),"idempotent google");
    String b=AdultContentPolicy.safeSearchUrl("https://www.bing.com/search?q=cat&adlt=off",strict);
    expect(b.endsWith("q=cat&adlt=strict"),"bing: "+b);
    String d=AdultContentPolicy.safeSearchUrl("https://duckduckgo.com/?q=oi+ola",moderate);
    expect(d.endsWith("q=oi+ola&kp=1"),"duckduckgo");
    expect(d.equals(AdultContentPolicy.safeSearchUrl(d,strict)),"idempotent duck");
    expect(AdultContentPolicy.safeSearchUrl("https://duckduckgo.com/?q=test",off).endsWith("q=test"),"off safe search");
    expect(AdultContentPolicy.safeSearchUrl("https://google.com/maps?q=x",strict).equals("https://google.com/maps?q=x"),"maps not search");
    expect(AdultContentPolicy.safeSearchUrl("https://notgoogle.com/search?q=x",strict).equals("https://notgoogle.com/search?q=x"),"spoofed search host");
    System.out.println("PASSOU: off/moderado/rigoroso, hostname, subdomínios, URLs seguras e SafeSearch.");
  }
}
''',encoding='utf-8')
    subprocess.run(['javac','-encoding','UTF-8','-d',td,str(policy),str(test)],check=True)
    subprocess.run(['java','-cp',td,'br.com.baixafacil.AdultContentTest'],check=True)
