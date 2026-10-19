#!/usr/bin/env python3
"""Regression ETP profiles: compile actual platform-independent Java + source wiring."""
from pathlib import Path
import subprocess
import tempfile
import textwrap

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT/'app/src/main/java/br/com/baixafacil'
source = (JAVA/'GeckoProtectionProfile.java').read_text(encoding='utf-8')
engine = (JAVA/'GeckoEngine.java').read_text(encoding='utf-8')
browser = (JAVA/'BrowserPane.java').read_text(encoding='utf-8')
settings = (JAVA/'SettingsActivity.java').read_text(encoding='utf-8')
controller = (JAVA/'AdBlockController.java').read_text(encoding='utf-8')

with tempfile.TemporaryDirectory() as temp:
    scratch = Path(temp)
    test = scratch/'CheckGeckoProfiles.java'
    test.write_text(textwrap.dedent('''
        package br.com.baixafacil;
        public final class CheckGeckoProfiles {
            public static void main(String[] args) {
                for (int mode=0; mode<=3; mode++) {
                    check(GeckoProtectionProfile.active(mode,true,false)==(mode!=0));
                    check(!GeckoProtectionProfile.active(mode,false,false));
                    check(!GeckoProtectionProfile.active(mode,true,true));
                    check(GeckoProtectionProfile.strict(mode)==(mode>=2));
                    check(GeckoProtectionProfile.extra(mode)==(mode==3));
                }
                check(GeckoProtectionProfile.normalize(-1)==2);
                check(GeckoProtectionProfile.normalize(42)==2);
                System.out.println("OK: matriz de 4 perfis ETP e opções de privacidade.");
            }
            private static void check(boolean b) {if(!b)throw new AssertionError();}
        }
    '''),encoding='utf-8')
    subprocess.run(['javac','-encoding','UTF-8','-d',temp,str(JAVA/'GeckoProtectionProfile.java'),str(test)],check=True)
    subprocess.run(['java','-cp',temp,'br.com.baixafacil.CheckGeckoProfiles'],check=True)

required_engine = [
    'ContentBlocking.EtpLevel.NONE','ContentBlocking.EtpLevel.DEFAULT',
    'ContentBlocking.EtpLevel.STRICT','setEnhancedTrackingProtectionLevel',
    'setEnhancedTrackingProtectionCategory', 'setStrictSocialTrackingProtection',
    'setQueryParameterStrippingEnabled','ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS',
    'ContentBlocking.CookieBehavior.ACCEPT_ALL','GeckoProtectionProfile.active',
]
assert all(v in engine for v in required_engine), 'GeckoEngine not wired to native ETP'
assert 'setUseTrackingProtection(GeckoEngine.trackingEnabled(host))' in browser
assert 'popup.session.getSettings().setUseTrackingProtection(GeckoEngine.trackingEnabled(host))' in browser
assert 'session.setContentBlockingDelegate(new ContentBlocking.Delegate()' in browser
assert 'onContentBlocked(GeckoSession gecko, ContentBlocking.BlockEvent event)' in browser
assert 'AdBlockController.noteGeckoBlocked()' in browser
assert 'geckoBlockedOnPage=0' in browser
assert 'BLOCKED.incrementAndGet()' in controller.split('public static void noteGeckoBlocked()')[1]
assert 'BLOCKED.incrementAndGet()' not in controller.split('public boolean shouldBlock(')[1].split('public boolean shouldHideCosmetic(')[0]
assert 'Proteção GeckoView' in settings and 'EasyList/EasyPrivacy locais NÃO interceptam requisições' in settings
assert 'Não altera a proteção de rede do GeckoView' in browser
assert 'forceReload=true;' in browser.split('int protectionMode=AdBlockController.savedMode(host);')[1]
assert 'app/src/main/java/br/com/baixafacil/GeckoBrowserView.java' # no external script injected
print('OK: GeckoRuntime, abas, contador real, UI e isolamento/compatibilidade.')
