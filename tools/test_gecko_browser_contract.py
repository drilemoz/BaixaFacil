#!/usr/bin/env python3
"""Contrato da fachada do GeckoView usado pelo BrowserPane.

Não substitui a compilação Android pelo javac do Gradle. Detecta chamadas a
métodos ausentes na fachada antes que a CI baixe o SDK e dependências.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
PANE = (ROOT / 'app/src/main/java/br/com/baixafacil/BrowserPane.java').read_text(encoding='utf-8')
VIEW = (ROOT / 'app/src/main/java/br/com/baixafacil/GeckoBrowserView.java').read_text(encoding='utf-8')

# Métodos herdados de android.view.View / android.view.ViewGroup.
INHERITED = {'getParent', 'postDelayed', 'removeCallbacks'}

# O compilador Java resolve tipos e herança. Este teste cobre o contrato de nomes
# concretamente usado pelo BrowserPane, sem tentar substituir o javac Android.
caller_methods = set(re.findall(r'\b(?:web|loginPopup)\s*\.\s*([A-Za-z_]\w*)\s*\(', PANE))
# Excluir trechos comentados antes de fazer a análise de declarações.
view_source = re.sub(r'/\*.*?\*/|//[^\n]*', '', VIEW, flags=re.S)
declared_methods = set(re.findall(
    r'\b(?:void|boolean|int|long|String|GeckoSession|[A-Za-z_]\w*(?:<[^;{}]*?>)?)\s+([A-Za-z_]\w*)\s*\(',
    view_source))
missing = sorted(caller_methods - declared_methods - INHERITED)
assert not missing, 'Métodos não implementados pela fachada GeckoBrowserView: ' + ', '.join(missing)

# Regressão do erro concreto do workflow Gerar APK 21.
assert re.search(r'\bboolean\s+canGoForward\s*\(\s*\)\s*\{\s*return\s+canForward\s*;\s*}', VIEW), \
    'canGoForward() deve usar o estado de onCanGoForward do GeckoSession'
assert re.search(r'\bboolean\s+canGoBack\s*\(\s*\)\s*\{\s*return\s+canBack\s*;\s*}', VIEW), \
    'canGoBack() deve usar o estado de onCanGoBack do GeckoSession'
for callback, setter in [('onCanGoForward', 'forwardAvailable'), ('onCanGoBack', 'backAvailable')]:
    assert re.search(r'\b'+callback+r'\s*\([^)]*\)\s*\{\s*web\.'+setter+r'\(value\);\s*}', PANE), \
        callback + ' precisa atualizar a fachada'
print(f'OK: {len(caller_methods)} chamadas distintas do BrowserPane cobertas pela fachada/inherança')
print('OK: histórico anterior/próximo e callbacks do GeckoSession conectados')
