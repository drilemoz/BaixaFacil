#!/usr/bin/env python3
"""Checks version/CI wiring without requiring Android SDK or PyYAML."""
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
VERSION = "0.1.0-alpha.49"
CODE = "49"

def read(path): return (ROOT / path).read_text(encoding="utf-8")

app = read("app/build.gradle")
workflow = read(".github/workflows/android.yml")
service = read("app/src/main/java/br/com/baixafacil/DownloadEngine.java")
foreground_service = read("app/src/main/java/br/com/baixafacil/TransferService.java")
checks = {
    "API 37.0 workflow shell regression": subprocess.run([sys.executable, str(ROOT / "tools/test_sdk_workflow.py")], check=False).returncode == 0,
    "alpha44 Downloads-Home navigation regression": all(x in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        for x in ["MainNavigationPolicy.bottomAction(downloadsVisible,action)",
                  "MainNavigationPolicy.systemBack(", "Voltar ao navegador", "executeNavigation(",
                  "case SHOW_BROWSER: showBrowser();", "case LOAD_HOME: if(browserPane!=null)browserPane.home();"])
        and "if(downloadsVisible)return;" not in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        and all(x in read("app/src/main/java/br/com/baixafacil/MainNavigationPolicy.java")
                for x in ["if(index==BACK || index==HOME) return Command.SHOW_BROWSER", "downloadsVisible ? Command.SHOW_BROWSER : Command.SHOW_DOWNLOADS", "Command.CLOSE_SELECTION"])
        and "class MainNavigationPolicyTest" in read("app/src/test/java/br/com/baixafacil/MainNavigationPolicyTest.java"),
    "alpha43 local download filters": all(x in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        for x in ["chooseSizeFilter", "chooseOriginFilter", "clearDownloadFilters", "updateActiveFilterBar", "DownloadListFilters.matchesSize", "DownloadListFilters.matchesOrigin", "selectVisibleDownloads", "endDownloadSelection();"])
        and all(x in read("app/src/main/java/br/com/baixafacil/DownloadListFilters.java")
        for x in ["class DownloadListFilters", "public static String originHost", "getRawUserInfo", "getHost()", "matchesSize", "matchesOrigin", "countOrigins"])
        and "class DownloadListFiltersTest" in read("app/src/test/java/br/com/baixafacil/DownloadListFiltersTest.java"),
    "alpha41 Custom Tabs wiring": all(x in app for x in ["androidx.browser:browser:1.10.0", "minorApiLevel = 0"])
        and all(x in read("app/src/main/java/br/com/baixafacil/CustomTabsBrowser.java")
            for x in ["CustomTabsClient.getPackageName", "tab.intent.setPackage", "tab.launchUrl", "forLogin"])
        and 'android.support.customtabs.action.CustomTabsService' in read("app/src/main/AndroidManifest.xml")
        and all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java") for x in ["Login em Custom Tabs", "CustomTabsBrowser::setForLogin", "CustomTabsBrowser.forLogin"])
        and all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["openCompatibleBrowser", "openLoginBrowser", "Abrir com Custom Tabs"]),
    "alpha42 multi-select safety": all(x in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
            for x in ["startDownloadSelection", "selectVisibleDownloads", "shareSelectedDownloads", "confirmSelectedCleanup", "runSelectedCleanup", "selectedCompletedRows", "DownloadSelectionPolicy.selectable", "ACTION_SEND_MULTIPLE", "FLAG_GRANT_READ_URI_PERMISSION"])
        and all(x in read("app/src/main/java/br/com/baixafacil/DownloadCleanup.java")
            for x in ["runSelected", "DownloadStore.removeIfCompleted", "getContentResolver().delete"])
        and all(x in read("app/src/main/java/br/com/baixafacil/DownloadStore.java")
            for x in ["removeIfCompleted", "expectedUri.equals", "DownloadSelectionPolicy.selectable"])
        and "class DownloadSelectionPolicyTest" in read("app/src/test/java/br/com/baixafacil/DownloadSelectionPolicyTest.java"),
    "alpha41 safe cleanup": all(x in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
            for x in ["showCleanDownloadsOptions", "confirmClearDownloads", "runDownloadCleanup", "Limpar downloads"])
        and all(x in read("app/src/main/java/br/com/baixafacil/DownloadCleanup.java")
            for x in ["new Report", "DownloadStore.removeIfFinished", "getContentResolver().delete"])
        and all(x in read("app/src/main/java/br/com/baixafacil/DownloadCleanupPolicy.java")
            for x in ["Concluído", "Cancelado", "Falhou", "content://media/"])
        and "class DownloadCleanupPolicyTest" in read("app/src/test/java/br/com/baixafacil/DownloadCleanupPolicyTest.java"),
    "GeckoView cookies and session storage": all(x in read("app/src/main/java/br/com/baixafacil/SessionPrefs.java") for x in ["StorageController.ClearFlags.SITE_DATA", "GeckoEngine.clear", "shouldClearAtStart"]) and all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java") for x in ["StorageController.ClearFlags.COOKIES", "StorageController.ClearFlags.DOM_STORAGES", "StorageController.ClearFlags.ALL"]),
    "Gecko memory and lifecycle": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["web.setActive(false)", "web.setActive(true)", "cancelMediaScan()", "onMemoryPressure(int level)", "BrowserMemoryReport.capture", "BrowserPerformancePolicy.scheduleDelay"]) and "browserPane.onMemoryPressure(level)" in read("app/src/main/java/br/com/baixafacil/MainActivity.java"),
    "Local media discovery extension": all(x in read("app/src/main/assets/baixafacil_gecko/media.js") for x in ["querySelectorAll(", "data.media.push", "data.media.push", "port.postMessage"]) and "scanPageVideos" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java"),
    "Safe video grouping across FB mirrors": all(x in read("app/src/main/java/br/com/baixafacil/VideoMenuModel.java") for x in ["canonicalHost", "mediaIdentity", "getRawQuery", "video_id"]),
    "Titles and active-video ranking tests": all(x in read("app/src/main/java/br/com/baixafacil/VideoPresentation.java") for x in ["cleanTitle", "sort", "mediaIdentity"])
        and all(x in read("app/src/test/java/br/com/baixafacil/VideoPresentationTest.java") for x in ["facebookMirrorSamePathDeduplicated", "playingVisibleVideoRanksFirst", "rejectsSensitiveOrGenericTitles"]),
    "ChatGPT lightweight Gecko profile": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["BrowserLoadPolicy.lightMode", "lightweightPage", "scheduleMediaScan(1300)", "!lightweightPage", "recoverGeckoCrash"]) and "chatGptLight" in read("app/src/main/java/br/com/baixafacil/BrowserPrefs.java"),
    "styled native menus": all(x in read("app/src/main/java/br/com/baixafacil/AppMenus.java") for x in ["class AppMenus", "static AlertDialog show", "static Entry section", "InputMethodManager"]),
    "dedicated video preferences": all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java") for x in ["Vídeos e mídia", "renderVideoSettings", "VideoPrefs.autoScan", "VideoPrefs.showStreams", "VideoPrefs.showSources"]),
    "accurate video list": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["VideoMenuModel.display", "showVideoActions", "HLS/DASH", "ARQUIVOS"]),
    "video grouped sources and safe selection": all(x in read("app/src/main/java/br/com/baixafacil/VideoMenuModel.java")
        for x in ["static final class Source", "final List<Source> sources", "String id=key(uri,kind,path)", "sourceList.sort", "getRawQuery", "video_id"])
        and all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        for x in ["video.sources.size()>1", "showSourceActions", "Escolher outra qualidade", "source.url"])
        and all(x in read("app/src/test/java/br/com/baixafacil/VideoMenuModelTest.java")
        for x in ["collapsesReissuedTokensForSameManifest", "groupsRenditionsOfSameStreamAndOrdersHighestFirst", "doesNotMergeDifferentVideosFromSameCdn", "doesNotCollapseDifferentQueryIdentities"]),
    "video sources policy tests": "class VideoMenuModelTest" in read("app/src/test/java/br/com/baixafacil/VideoMenuModelTest.java"),

    # AAR metadata do Media3 1.11.1 exige android-36. Impede regressão para API 35.
    "Media3 e GeckoView Android API 37 compileSdk": bool(re.search(r"compileSdk\s*\{\s*version\s*=\s*release\(37\)\s*\{\s*minorApiLevel\s*=\s*0\s*\}", app))
        and all(x in app for x in ["media3-exoplayer:1.11.1", "media3-ui:1.11.1", "media3-exoplayer-hls:1.11.1", "media3-exoplayer-dash:1.11.1"]),
    "AGP supports API 37 and AndroidX Core 1.19": "version '9.1.1'" in read("build.gradle")
        and "gradle-version: '9.3.1'" in workflow,
    "CI installs exact Android SDK Platform 37.0": "'platforms;android-37.0'" in workflow
        and "'build-tools;36.0.0'" in workflow
        and 'platforms/android-37.0/android.jar' in workflow
        and "'platforms;android-37'" not in workflow,
    "Gradle API 37.0 and APK inspector": all(x in workflow for x in [
        "'platforms;android-37.0'", "'build-tools;36.0.0'", "gradle-version: '9.3.1'", 'tool_dir="$ANDROID_HOME/build-tools/36.0.0"',
        ":app:testDebugUnitTest :app:assembleDebug", "versionCode='49'",
    ]) and "version '9.1.1'" in read("build.gradle") and "minorApiLevel = 0" in app,
    "Android install targets remain unchanged": bool(re.search(r"(?m)^\s*minSdk\s+29\s*$", app))
        and bool(re.search(r"(?m)^\s*targetSdk\s+34\s*$", app)),
    "versionCode": bool(re.search(r"(?m)^\s*versionCode\s+" + CODE + r"\s*$", app)),
    "versionName": "versionName '" + VERSION + "'" in app,
    "workflow version": "version='" + VERSION + "'" in workflow,
    "release publisher version": "VERSION: " + VERSION in workflow,
    "publication directly in Releases": "bash tools/publish_apk.sh" in workflow,
    "no ZIP Artifacts": "upload-artifact" not in workflow and "download-artifact" not in workflow,
    "unit test and Debug compile": ":app:testDebugUnitTest :app:assembleDebug" in workflow,
    "Gecko session management": all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java") for x in ["renderSessionSettings", "Manter logins entre aberturas", "clearGecko("]) and all(x in read("app/src/main/java/br/com/baixafacil/SessionPrefs.java") for x in ["shouldClearAtStart", "clearForNewSession", "StorageController.ClearFlags.SITE_DATA"]),
    "address suggestions": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["ListPopupWindow", "updateAddressSuggestions", "BrowserSuggestions.suggest", "dismissAddressSuggestions"])
        and all(x in read("app/src/main/java/br/com/baixafacil/BrowserSuggestions.java") for x in ["LoginWindowPolicy.hasLoginParameters", "AuthNavigation.isSensitiveLoginPage", "LIMIT=5"]),
    "APK version validation": all(x in workflow for x in ["aapt", "versionCode='" + CODE + "'", "versionName='" + VERSION + "'"]),
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
    "Gecko browser tools and desktop mode": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["web.findAllAsync", "web.findNext", "setUserAgentMode", "setViewportMode", "showPageTools"]) and all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java") for x in ["Modo computador", "Tamanho do texto"]),
    "favorites validation": all(x in read("app/src/main/java/br/com/baixafacil/FavoriteRules.java")
        for x in ["MAX_FAVORITES = 60", "MAX_URL_LENGTH = 2048", "UrlTools.normalizeAddress", "MAX_TITLE_LENGTH = 80"])
        and "class FavoriteRulesTest" in read("app/src/test/java/br/com/baixafacil/FavoriteRulesTest.java"),
    "favorites storage and controls": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPrefs.java")
        for x in ["KEY_FAVORITES", "addFavorite", "editFavorite", "removeFavorite", "isFavorite"])
        and all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
            for x in ["showFavorites", "showFavoriteTools", "Salvar esta página", "openFavoriteSettings"])
        and all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
            for x in ["manageFavorites", "editFavorite", "confirmRemoveFavorite", "EXTRA_OPEN_FAVORITES"]),
    "Java engine restriction variable collision": service.count("String restriction=DownloadRestrictions.blocked(context);") == 1
        and "String restrictionAfterFailure = DownloadRestrictions.blocked(context);" in service,
    "compact download cards and expand": all(x in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        for x in ["expandedDownloads", "compactDownloads", "smallDownloadAction", "Visualização: ", "setOnClickListener"]),
    "alpha40 duplicate protection": all(x in read("app/src/main/java/br/com/baixafacil/DownloadDuplicatePolicy.java")
        for x in ["class DownloadDuplicatePolicy", "url.equals(r.url)", "Kind.IN_PROGRESS", "Kind.COMPLETED"])
        and all(x in read("app/src/main/java/br/com/baixafacil/DownloadDuplicateUi.java")
        for x in ["maybeShow(", "Baixar novamente", "DownloadStore.all", "Ver downloads"])
        and "DownloadDuplicateUi.maybeShow" in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        and "DownloadDuplicateUi.maybeShow" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "class DownloadDuplicatePolicyTest" in read("app/src/test/java/br/com/baixafacil/DownloadDuplicatePolicyTest.java"),
    "alpha40 quick action crash correction": "labeledDownloadAction" in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        and '"Abrir","Abrir "' in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        and '"Enviar","Compartilhar "' in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        and 'catch (SecurityException | IllegalArgumentException | RuntimeException error)' not in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        and 'catch (RuntimeException error)' in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        and 'ClipData.newUri' in read("app/src/main/java/br/com/baixafacil/MainActivity.java"),
    "download confirmation and diagnostics": all(x in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        for x in ["DownloadDialogs.confirm", "safeUri(", 'AppEventLog.warn(this,"Downloads"', 'AppEventLog.warn(this,"Arquivos"'])
        and all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        for x in ["DownloadDialogs.confirm", 'AppEventLog.warn(host,"Downloads"', 'AppEventLog.warn(host,"Navegador"'])
        and all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
        for x in ["renderDiagnosticsSettings", "Diagnóstico e alertas", "showEventLogViewer", "copyEventLog"])
        and "class AppEventLog" in read("app/src/main/java/br/com/baixafacil/AppEventLog.java")
        and "class DownloadDialogs" in read("app/src/main/java/br/com/baixafacil/DownloadDialogs.java")
        and 'android:name=".BaixaFacilApp"' in read("app/src/main/AndroidManifest.xml"),
    "Gecko video direct-source detection": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["web.scanVideos", "scanPageVideos", "showDetectedVideos", "Fontes (", "VideoLinks.isVideoCandidate"]) and "class VideoLinksTest" in read("app/src/test/java/br/com/baixafacil/VideoLinksTest.java"),
    "alpha45 HLS VOD conservative support": all(x in read("app/src/main/java/br/com/baixafacil/HlsVodPlaylist.java") for x in ["#EXT-X-ENDLIST", "#EXT-X-KEY:", "#EXT-X-MAP:", "#EXT-X-BYTERANGE:", "MAX_SEGMENTS", "Segmento inválido"])
        and all(x in read("app/src/main/java/br/com/baixafacil/DownloadEngine.java") for x in ["transferHlsVod", "video/mp2t", "channel.truncate(0)", "IS_PENDING", "HLS_NAO_SUPORTADO"])
        and all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["offerHlsDownload", "Fontes de vídeo", "Prévia do player", 'Ui.pill(host,"Baixar"'])
        and "class HlsVodPlaylistTest" in read("app/src/test/java/br/com/baixafacil/HlsVodPlaylistTest.java"),
    "documentation version code": "`versionCode`: `" + CODE + "`" in read("docs/VERSIONING.md")
        and "Próxima versão sequencial: `0.1.0-alpha." + str(int(CODE) + 1) + "`" in read("docs/VERSIONING.md"),
    "Gecko login compatibility": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["AuthNavigation.isGoogleOAuth", "openInExternalBrowser", "GeckoEngine.updatePreferences", "thirdPartyCookies"]) and "chooseCookieCompatibility" in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java") and "class AuthNavigationTest" in read("app/src/test/java/br/com/baixafacil/AuthNavigationTest.java"),
    "Gecko popup login separation": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["onNewSession", "LoginWindowPolicy.allowPopup", "loginPopupDialog.dismiss()", "showLoginDiagnosis", "onLoadError", "onExternalResponse"]) and "class LoginWindowPolicyTest" in read("app/src/test/java/br/com/baixafacil/LoginWindowPolicyTest.java"),
    "auth tokens excluded from history": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPrefs.java")
        for x in ["AuthNavigation.isSensitiveLoginPage", "LoginWindowPolicy.hasLoginParameters"]),
    "GeckoView dependency and bundled bridge": "org.mozilla.geckoview:geckoview:158.0.20261007201254" in app and "maven.mozilla.org/maven2" in read("settings.gradle") and "ensureBuiltIn(" in read("app/src/main/java/br/com/baixafacil/GeckoBrowserView.java") and "nativeMessagingFromContent" in read("app/src/main/assets/baixafacil_gecko/manifest.json") and "android.webkit.WebView" not in read("app/src/main/java/br/com/baixafacil/BrowserPane.java"),
    "Media3 player and secure classification": all(x in app for x in ["media3-exoplayer:1.11.1", "media3-ui:1.11.1", "media3-exoplayer-hls:1.11.1", "media3-exoplayer-dash:1.11.1"])
        and "class MediaPlayerActivity" in read("app/src/main/java/br/com/baixafacil/MediaPlayerActivity.java")
        and "class MediaSourcePolicy" in read("app/src/main/java/br/com/baixafacil/MediaSourcePolicy.java")
        and "isPlaylist" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "MediaSourcePolicyTest" in read("app/src/test/java/br/com/baixafacil/MediaSourcePolicyTest.java")
        and '.MediaPlayerActivity' in read("app/src/main/AndroidManifest.xml"),
    "local media thumbnails and metadata": all(x in read("app/src/main/java/br/com/baixafacil/MediaPreviewLoader.java")
        for x in ["MediaMetadataRetriever", "getScaledFrameAtTime", "loadThumbnail", "Executors.newSingleThreadExecutor", "LruCache", '"content"'])
        and "mediaPreviewLoader.load" in read("app/src/main/java/br/com/baixafacil/MainActivity.java")
        and "MediaPreviewFormatTest" in read("app/src/test/java/br/com/baixafacil/MediaPreviewFormatTest.java"),
    "adblock core and source limits": all(x in read("app/src/main/java/br/com/baixafacil/AdBlockRules.java")
        for x in ["shouldBlock", "MAX_RULES", "getRawUserInfo", "isTracker", "loginUrl"])
        and all(x in read("app/src/main/java/br/com/baixafacil/AdBlockController.java")
        for x in ["adblock_seed.txt", "easylist.txt", "easyprivacy.txt", "setException", "updateNow", "setMode"])
        and "class AdBlockRulesTest" in read("app/src/test/java/br/com/baixafacil/AdBlockRulesTest.java"),
    "Gecko native tracking protection and cosmetic rules": "setUseTrackingProtection(AdBlockController.savedTrackers(host))" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") and "web.cosmeticFilters()" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") and "bf-ad-css" in read("app/src/main/assets/baixafacil_gecko/media.js"),
    "adblock settings and per-site controls": all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
        for x in ["chooseAdBlockMode", "updateAdLists", "Bloquear rastreadores", "Ocultar anúncios na página"]),
    "documentation": all(VERSION in read(x) for x in ["README.md", "RELEASE_NOTES.md", "docs/VERSIONING.md", "docs/VALIDATION.md"]),
}
for label, valid in checks.items():
    print(("OK" if valid else "ERRO") + ": " + label)
if not all(checks.values()):
    raise SystemExit(1)
print("Pré-validação concluída.")
