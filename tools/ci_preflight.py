#!/usr/bin/env python3
"""Checks version/CI wiring without requiring Android SDK or PyYAML."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
VERSION = "0.1.0-alpha.24"
CODE = "24"

def read(path): return (ROOT / path).read_text(encoding="utf-8")

app = read("app/build.gradle")
workflow = read(".github/workflows/android.yml")
service = read("app/src/main/java/br/com/baixafacil/DownloadEngine.java")
foreground_service = read("app/src/main/java/br/com/baixafacil/TransferService.java")
checks = {
    "versionCode": bool(re.search(r"(?m)^\s*versionCode\s+" + CODE + r"\s*$", app)),
    "versionName": "versionName '" + VERSION + "'" in app,
    "workflow version": "version='" + VERSION + "'" in workflow,
    "release publisher version": "VERSION: " + VERSION in workflow,
    "publication directly in Releases": "bash tools/publish_apk.sh" in workflow,
    "no ZIP Artifacts": "upload-artifact" not in workflow and "download-artifact" not in workflow,
    "unit test and Debug compile": ":app:testDebugUnitTest :app:assembleDebug" in workflow,
    "APK version validation": all(x in workflow for x in ["aapt", "versionCode='24'", "versionName='" + VERSION + "'"]),
    "signature verification": "apksigner" in workflow,
    "code User-Agent version": "BaixaFacil/" + VERSION in service,
    "foreground notification initialization":
        "new Notification.Builder(this, CHANNEL)" in foreground_service
        and "startForeground(FOREGROUND_ID, startingNotice" in foreground_service
        and "setSmallIcon(android.R.drawable.stat_sys_download)" in foreground_service
        and not re.search(r"(?m)^\s*notification\(", foreground_service),
    "UIDT and recovery": all(x in read("app/src/main/AndroidManifest.xml") for x in ["RUN_USER_INITIATED_JOBS", "BIND_JOB_SERVICE", ".TransferJobService"])
        and "setUserInitiated(true)" in read("app/src/main/java/br/com/baixafacil/TransferJobService.java")
        and "recoverPendingQueue" in read("app/src/main/java/br/com/baixafacil/MainActivity.java"),
    "network and battery controls": all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java") for x in ["Somente Wi-Fi", "Permitir dados móveis", "Proteger bateria abaixo de 20%"])
        and "DownloadRestrictions.blocked(context)" in service
        and "DownloadRestrictions.blocked(activity)" in read("app/src/main/java/br/com/baixafacil/TransferStarter.java")
        and "DownloadRestrictions.blocked(this)" in read("app/src/main/java/br/com/baixafacil/TransferJobService.java")
        and "class DownloadPolicyTest" in read("app/src/test/java/br/com/baixafacil/DownloadPolicyTest.java"),
    "browser tools and desktop mode": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        for x in ["findAllAsync", "findNext", "setBuiltInZoomControls(true)", "setUseWideViewPort", "DesktopUserAgent.toDesktop", "showPageTools"])
        and all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
            for x in ["Modo computador", "Tamanho do texto"])
        and "class DesktopUserAgentTest" in read("app/src/test/java/br/com/baixafacil/DesktopUserAgentTest.java"),
    "favorites validation": all(x in read("app/src/main/java/br/com/baixafacil/FavoriteRules.java")
        for x in ["MAX_FAVORITES = 60", "MAX_URL_LENGTH = 2048", "UrlTools.normalizeAddress", "MAX_TITLE_LENGTH = 80"])
        and "class FavoriteRulesTest" in read("app/src/test/java/br/com/baixafacil/FavoriteRulesTest.java"),
    "favorites storage and controls": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPrefs.java")
        for x in ["KEY_FAVORITES", "addFavorite", "editFavorite", "removeFavorite", "isFavorite"])
        and all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
            for x in ["showFavorites", "Salvar página nos favoritos", "Salvar nos favoritos", "openFavoriteSettings"])
        and all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
            for x in ["manageFavorites", "editFavorite", "confirmRemoveFavorite", "EXTRA_OPEN_FAVORITES"]),
    "Java engine restriction variable collision": service.count("String restriction=DownloadRestrictions.blocked(context);") == 1
        and "String restrictionAfterFailure = DownloadRestrictions.blocked(context);" in service,
    "compact download cards and expand": all(x in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        for x in ["expandedDownloads", "compactDownloads", "smallDownloadAction", "Visualização: ", "setOnClickListener"]),
    "video direct-source detection": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        for x in ["VIDEO_SCAN_JS", "shouldInterceptRequest", "scanPageVideos", "showDetectedVideos", "Vídeos (", "VideoLinks.isVideoCandidate"])
        and all(x in read("app/src/main/java/br/com/baixafacil/VideoLinks.java")
            for x in ["isVideoCandidate", "isManifest", "allowedHttpUrl", "MAX_VIDEO_URLS"])
        and "class VideoLinksTest" in read("app/src/test/java/br/com/baixafacil/VideoLinksTest.java"),
    "documentation version code": "`versionCode`: `" + CODE + "`" in read("docs/VERSIONING.md")
        and "Próxima versão sequencial: `0.1.0-alpha.25`" in read("docs/VERSIONING.md"),
    "login compatibility": "AuthNavigation.isGoogleOAuth" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "openInExternalBrowser" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "setAcceptThirdPartyCookies(web,appliedThirdPartyCookies)" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "thirdPartyCookies" in read("app/src/main/java/br/com/baixafacil/BrowserPrefs.java")
        and "chooseCookieCompatibility" in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
        and "class AuthNavigationTest" in read("app/src/test/java/br/com/baixafacil/AuthNavigationTest.java"),
    "documentation": all(VERSION in read(x) for x in ["README.md", "RELEASE_NOTES.md", "docs/VERSIONING.md", "docs/VALIDATION.md"]),
}
for label, valid in checks.items():
    print(("OK" if valid else "ERRO") + ": " + label)
if not all(checks.values()):
    raise SystemExit(1)
print("Pré-validação concluída.")
