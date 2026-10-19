#!/usr/bin/env python3
"""Run the real pure-Java crash policy with JDK 17, no Android SDK needed."""
from pathlib import Path
import shutil
import subprocess
import tempfile

root=Path(__file__).resolve().parents[1]
source=root/'app/src/main/java/br/com/baixafacil/GeckoCrashPolicy.java'
assert shutil.which('javac') and shutil.which('java'), 'JDK requerido'
assert 'GeckoCrashPolicy.afterFailure(' in (root/'app/src/main/java/br/com/baixafacil/BrowserPane.java').read_text()
with tempfile.TemporaryDirectory() as td:
    directory=Path(td)
    harness=directory/'CrashPolicyHarness.java'
    harness.write_text('''package br.com.baixafacil;
public final class CrashPolicyHarness {
    static void check(boolean ok) { if(!ok) throw new AssertionError("Politica de falha invalida"); }
    public static void main(String[] args) {
        GeckoCrashPolicy.Decision a=GeckoCrashPolicy.afterFailure(1000,0,0);
        check(a.restartAutomatically && a.crashes==1);
        GeckoCrashPolicy.Decision b=GeckoCrashPolicy.afterFailure(1100,a.windowStartMs,a.crashes);
        check(b.restartAutomatically && b.crashes==2);
        GeckoCrashPolicy.Decision c=GeckoCrashPolicy.afterFailure(1200,b.windowStartMs,b.crashes);
        check(!c.restartAutomatically && c.crashes==3);
        check(!GeckoCrashPolicy.afterFailure(1300,c.windowStartMs,c.crashes).restartAutomatically);
        check(GeckoCrashPolicy.afterFailure(200001,c.windowStartMs,c.crashes).restartAutomatically);
        check(GeckoCrashPolicy.afterFailure(10,c.windowStartMs,c.crashes).restartAutomatically);
        check(GeckoCrashPolicy.afterFailure(2000,1000,-1).restartAutomatically);
        check(!GeckoCrashPolicy.afterFailure(2000,1000,Integer.MAX_VALUE).restartAutomatically);
        System.out.println("PASSOU: limite de reinicios, janela, reboot e contador invalido");
    }
}''',encoding='utf-8')
    subprocess.run(['javac','-encoding','UTF-8','-d',td,str(source),str(harness)],check=True)
    subprocess.run(['java','-cp',td,'br.com.baixafacil.CrashPolicyHarness'],check=True)
