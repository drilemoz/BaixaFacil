#!/usr/bin/env python3
"""Tests for GeckoView 158 native ABIs and strict Gradle APK collector."""
import json
import tempfile
import zipfile
from pathlib import Path
from collect_abi_apks import ABIS, collect

ROOT = Path(__file__).resolve().parents[1]
gradle = (ROOT/'app/build.gradle').read_text(encoding='utf-8')
yaml = (ROOT/'.github/workflows/android.yml').read_text(encoding='utf-8')
publish = (ROOT/'tools/publish_apk.sh').read_text(encoding='utf-8')
assert ABIS == ('armeabi-v7a','arm64-v8a','x86_64')
assert all(x in gradle for x in ('enable true', 'reset()', 'universalApk false',
              "include 'armeabi-v7a', 'arm64-v8a', 'x86_64'"))
assert "'x86', 'x86_64'" not in gradle
assert 'versionCode 57' in gradle and "versionName '0.1.0-alpha.57'" in gradle
assert 'tools/collect_abi_apks.py' in yaml and 'apk_dir=dist' in yaml
assert 'for abi in armeabi-v7a arm64-v8a x86_64; do' in yaml
assert 'APK_DIR:' in yaml and 'APK_FILE:' not in yaml
assert '--pattern "Baixa-Facil-v${VERSION}-*.apk"' in publish
assert 'sha256sum' in publish and 'apk_count" -eq 3' in publish

with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    directory=root/'app/build/outputs/apk/debug'
    directory.mkdir(parents=True)
    els=[]
    def make_apk(abi, libpath=None):
        name=f'app-{abi}-debug.apk'
        with zipfile.ZipFile(directory/name,'w') as apk:
            apk.writestr('AndroidManifest.xml','demo')
            if libpath is not False:
                apk.writestr(libpath or f'lib/{abi}/libxul.so',b'gecko-example')
        return name
    for abi in ABIS:
        name=make_apk(abi)
        els.append({'filters':[{'filterType':'ABI','value':abi}],
                    'versionCode':57,'versionName':'0.1.0-alpha.57','outputFile':name})
    meta={'variantName':'debug','applicationId':'br.com.baixafacil','elements':els}
    metadata_file=directory/'output-metadata.json'
    def write_meta(): metadata_file.write_text(json.dumps(meta),encoding='utf-8')
    def rejects(tag,code=56):
        write_meta()
        try: collect(root,'debug','0.1.0-alpha.57',code,root/tag)
        except ValueError: return
        raise AssertionError(f'{tag}: collector accepted invalid APK')
    write_meta()
    paths=collect(root,'debug','0.1.0-alpha.57',57,root/'dist')
    assert len(paths)==3 and all(p.is_file() for p in paths)
    assert all(p.name.endswith(f'{abi}.apk') for p,abi in zip(paths,ABIS))
    rejects('bad_version',code=52)
    # Duplication of ABI filter
    meta['elements'][1]['filters'][0]['value']='armeabi-v7a'
    rejects('bad_duplicate')
    meta['elements'][1]['filters'][0]['value']='arm64-v8a'
    # Unsupported x86 even if Gradle produces an APK (the build 23 failure)
    x86={'filters':[{'filterType':'ABI','value':'x86'}],
         'versionCode':57,'versionName':'0.1.0-alpha.57',
         'outputFile':make_apk('x86',libpath=False)}
    meta['elements'].append(x86)
    rejects('bad_x86')
    meta['elements'].pop()
    # Missing x86_64 APK/ABI
    element=meta['elements'].pop()
    rejects('bad_missing')
    meta['elements'].append(element)
    # Wrong architecture library
    make_apk('x86_64','lib/arm64-v8a/libxul.so')
    rejects('bad_wrong_lib')
    # Gecko lib absent, even if some other native library exists
    make_apk('x86_64','lib/x86_64/libother.so')
    rejects('bad_no_xul')
print('PASSOU: 3 ABIs GeckoView, x86 vetado, filtros, bibliotecas nativas e versionamento.')
