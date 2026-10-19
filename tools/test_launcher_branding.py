#!/usr/bin/env python3
"""Launcher resources contract: bitmaps, XML, adaptive, round and monochrome."""
from pathlib import Path
import struct
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'app/src/main/res'
ANDROID='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
application=manifest.find('application')
assert application is not None
assert application.attrib[ANDROID+'icon']=='@mipmap/ic_launcher'
assert application.attrib[ANDROID+'roundIcon']=='@mipmap/ic_launcher_round'
assert application.attrib[ANDROID+'label']=='Baixa Fácil'
assert (ROOT/'docs/VERSIONING.md').read_text(encoding='utf-8').count('Próxima versão sequencial: `0.1.0-alpha.58`, `versionCode 58`') == 1
expected={'mdpi':48,'hdpi':72,'xhdpi':96,'xxhdpi':144,'xxxhdpi':192}
for density,side in expected.items():
    for variant in ('ic_launcher','ic_launcher_round'):
        p=RES/f'mipmap-{density}/{variant}.png'
        data=p.read_bytes()
        assert data.startswith(b"\x89PNG\r\n\x1a\n")
        assert struct.unpack('>II',data[16:24])==(side,side),p
        assert data[25]==6,(p,'expected RGBA PNG')
for variant in ('ic_launcher','ic_launcher_round'):
    root=ET.parse(RES/f'mipmap-anydpi-v26/{variant}.xml').getroot()
    assert root.tag=='adaptive-icon'
    children={e.tag:e.attrib.get(ANDROID+'drawable') for e in root}
    assert children=={'background':'@color/xp_launcher_blue','foreground':'@drawable/ic_launcher_foreground','monochrome':'@drawable/ic_launcher_monochrome'},children
color_root=ET.parse(RES/'values/colors.xml').getroot()
assert any(e.attrib.get('name')=='xp_launcher_blue' and e.text=='#0768C6' for e in color_root)
assert ET.parse(RES/'drawable/ic_launcher_monochrome.xml').getroot().tag=='vector'
png=(RES/'drawable-nodpi/ic_launcher_foreground.png').read_bytes()
assert png.startswith(b"\x89PNG\r\n\x1a\n") and struct.unpack('>II',png[16:24])==(432,432)
assert png[25]==6
assert not (RES/'drawable/ic_launcher.xml').exists()
print('OK: ícones Internet XP legado, redondo, adaptativo e monocromático.')
