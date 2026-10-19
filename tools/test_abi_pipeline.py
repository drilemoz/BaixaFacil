#!/usr/bin/env python3
"""Regression tests for Gradle ABI matrix and metadata-driven APK collection."""
import json
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path
from collect_abi_apks import ABIS, collect

ROOT = Path(__file__).resolve().parents[1]
gradle = (ROOT/'app/build.gradle').read_text()
yaml = (ROOT/'.github/workflows/android.yml').read_text()
publish = (ROOT/'tools/publish_apk.sh').read_text()
assert all(x in gradle for x in ("enable true", 'reset()', "universalApk false",
              "include 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'"))
assert 'versionCode 51' in gradle and "versionName '0.1.0-alpha.51'" in gradle
assert 'tools/collect_abi_apks.py' in yaml and 'apk_dir=dist' in yaml
assert all(a in yaml for a in ABIS)
assert 'APK_DIR:' in yaml and 'APK_FILE:' not in yaml
assert '--pattern "Baixa-Facil-v${VERSION}-*.apk"' in publish
assert 'sha256sum' in publish and 'apk_count" -eq 4' in publish

with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    directory=root/'app/build/outputs/apk/debug'
    directory.mkdir(parents=True)
    els=[]
    for abi in ABIS:
        name=f'app-{abi}-debug.apk'
        with zipfile.ZipFile(directory/name, 'w') as apk:
            apk.writestr('AndroidManifest.xml', 'demo')
            apk.writestr(f'lib/{abi}/libxul.so', f'{abi}-example')
        els.append({'filters':[{'filterType':'ABI','value':abi}],
                    'versionCode':51,'versionName':'0.1.0-alpha.51','outputFile':name})
    meta={'variantName':'debug','applicationId':'br.com.baixafacil','elements':els}
    (directory/'output-metadata.json').write_text(json.dumps(meta))
    out=root/'dist'
    paths=collect(root,'debug','0.1.0-alpha.51',51,out)
    assert len(paths)==4 and all(p.is_file() for p in paths)
    # Negative tests avoid deleting real output; collector checks metadata before writes.
    meta['elements'][2]['filters']=[{'filterType':'ABI','value':'arm64-v8a'}]
    (directory/'output-metadata.json').write_text(json.dumps(meta))
    try: collect(root,'debug','0.1.0-alpha.51',51,root/'bad1')
    except ValueError: pass
    else: raise AssertionError('Duplicate ABI passed')
    meta['elements'][2]['filters']=[{'filterType':'ABI','value':'x86'}]
    (directory/'output-metadata.json').write_text(json.dumps(meta))
    with zipfile.ZipFile(directory/'app-x86-debug.apk','w') as apk:
        apk.writestr('lib/arm64-v8a/libxul.so','wrong')
    try: collect(root,'debug','0.1.0-alpha.51',51,root/'bad2')
    except ValueError: pass
    else: raise AssertionError('Wrong native ABI passed')
    try: collect(root,'debug','0.1.0-alpha.51',50,root/'bad3')
    except ValueError: pass
    else: raise AssertionError('Wrong versionCode passed')
print('PASSOU: quatro ABI, contratos Gradle/CI, erros de metadata/arquitetura/versão.')
