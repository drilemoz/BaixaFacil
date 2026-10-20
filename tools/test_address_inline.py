#!/usr/bin/env python3
"""Regressão do editor inline da alpha.60 (contratos estáticos, não teste UI Android)."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
pane=(root/'app/src/main/java/br/com/baixafacil/BrowserPane.java').read_text()
activity=(root/'app/src/main/java/br/com/baixafacil/MainActivity.java').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()

def portion(start,end):
    return pane.split(start,1)[1].split(end,1)[0]

build=portion('private void build(Bundle saved)', 'private void dismissAddressSuggestions()')
open_editor=portion('private void openAddressEditor()', 'private void addSuggestion(String url,String title,String kind)')
close=portion('private void dismissAddressSuggestions()', 'private String shownHost(')
assert 'addressDisplay.setOnClickListener(v->openAddressEditor())' in build
assert 'row.addView(address,new LinearLayout.LayoutParams(0,dp(48),1))' in build
assert 'address.setVisibility(View.GONE)' in build
assert 'view.addView(top,topP)' in build
assert 'view.addView(addressPanel,panelP)' in build
assert build.index('view.addView(top,topP)') < build.index('view.addView(addressPanel,panelP)') < build.index('view.addView(findBar,findP)')
assert 'ScrollView scroll=new ScrollView(host)' in build
assert 'scroll.addView(suggestionRows' in build
assert 'new LinearLayout.LayoutParams(-1,dp(215))' in build
assert 'addressPanel.setVisibility(View.GONE)' in build
assert 'addressEditing=true;' in open_editor
assert 'addressPanel.setVisibility(View.VISIBLE)' in open_editor
assert 'address.setVisibility(View.VISIBLE)' in open_editor
assert 'address.requestFocus()' in open_editor
assert 'address.selectAll()' in open_editor
assert 'keyboard.showSoftInput(address,InputMethodManager.SHOW_IMPLICIT)' in open_editor
assert 'AlertDialog' not in open_editor and 'PopupWindow' not in open_editor
assert 'addressEditorActions.setVisibility(View.VISIBLE)' in open_editor
assert 'addressMenuButton.setVisibility(View.GONE)' in open_editor
assert 'addressPanel.setVisibility(View.GONE)' in close
assert 'addressDisplay.setVisibility(View.VISIBLE)' in close
assert 'keyboard.hideSoftInputFromWindow' in close
assert 'if(browserPane!=null && browserPane.handleAddressBack())return;' in activity
assert 'browserPane.handleAddressBack();\n        downloadsVisible=true;' in activity
assert 'android:windowSoftInputMode="adjustResize"' in manifest
assert 'addressDialog' not in pane
assert 'dismissAddressSuggestions();setAddressFromPage(url)' in pane
assert 'if(addressEditing)updateAddressSuggestions()' in pane
assert 'if(!addressEditing)openAddressEditor();' in pane
print('PASSOU: editor inline sem pop-up, primeiro toque, sugestões, IME e Voltar')
