#!/usr/bin/env python3
"""Checks version/CI wiring without requiring Android SDK or PyYAML."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
VERSION = "0.1.0-alpha.35"
CODE = "35"

def read(path): return (ROOT / path).read_text(encoding="utf-8")

app = read("app/build.gradle")
workflow = read(".github/workflows/android.yml")
service = read("app/src/main/java/br/com/baixafacil/DownloadEngine.java")
foreground_service = read("app/src/main/java/br/com/baixafacil/TransferService.java")
checks = {
    "Reels context matched to actual video": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["scheduleMediaScan(900)", "videoPreviewMeta", "playing", "findPreview", "decodeVideoFrame"]),
    "Safe video grouping across FB mirrors": all(x in read("app/src/main/java/br/com/baixafacil/VideoMenuModel.java") for x in ["canonicalHost", "mediaIdentity", "getRawQuery", "video_id"]),
    "Titles and active-video ranking tests": all(x in read("app/src/main/java/br/com/baixafacil/VideoPresentation.java") for x in ["cleanTitle", "sort", "mediaIdentity"])
        and all(x in read("app/src/test/java/br/com/baixafacil/VideoPresentationTest.java") for x in ["facebookMirrorSamePathDeduplicated", "playingVisibleVideoRanksFirst", "rejectsSensitiveOrGenericTitles"]),
    "ChatGPT lightweight profile": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        for x in ["BrowserLoadPolicy.lightMode", "if (lightweightPage) return null;", "if(!lightweightPage) applyCosmeticFilters", "if(!lightweightPage)host", "WebViewRenderProcessClient", "rendererUnresponsive"])
        and "chatGptLight" in read("app/src/main/java/br/com/baixafacil/BrowserPrefs.java")
        and "Compatibilidade com ChatGPT" in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
        and "class BrowserLoadPolicyTest" in read("app/src/test/java/br/com/baixafacil/BrowserLoadPolicyTest.java"),
    "styled native menus": all(x in read("app/src/main/java/br/com/baixafacil/AppMenus.java") for x in ["class AppMenus", "static AlertDialog show", "static Entry section", "InputMethodManager"]),
    "dedicated video preferences": all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java") for x in ["Vídeos e mídia", "renderVideoSettings", "VideoPrefs.autoScan", "VideoPrefs.showStreams", "VideoPrefs.showSources"]),
    "accurate video list": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java") for x in ["VideoMenuModel.display", "showVideoActions", "TRANSMISSÕES", "ARQUIVOS"]),
    "video grouped sources and safe selection": all(x in read("app/src/main/java/br/com/baixafacil/VideoMenuModel.java")
        for x in ["static final class Source", "final List<Source> sources", "String id=key(uri,kind,path)", "sourceList.sort", "getRawQuery", "video_id"])
        and all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        for x in ["video.sources.size()>1", "showSourceActions", "Escolher outra qualidade", "source.url"])
        and all(x in read("app/src/test/java/br/com/baixafacil/VideoMenuModelTest.java")
        for x in ["collapsesReissuedTokensForSameManifest", "groupsRenditionsOfSameStreamAndOrdersHighestFirst", "doesNotMergeDifferentVideosFromSameCdn", "doesNotCollapseDifferentQueryIdentities"]),
    "video sources policy tests": "class VideoMenuModelTest" in read("app/src/test/java/br/com/baixafacil/VideoMenuModelTest.java"),

    # AAR metadata do Media3 1.11.1 exige android-36. Impede regressão para API 35.
    "Media3 Android API 36 compileSdk": bool(re.search(r"(?m)^\s*compileSdk\s+36\s*$", app))
        and all(x in app for x in ["media3-exoplayer:1.11.1", "media3-ui:1.11.1", "media3-exoplayer-hls:1.11.1", "media3-exoplayer-dash:1.11.1"]),
    "AGP supports API 36 with existing Gradle": "version '8.10.1'" in read("build.gradle")
        and "gradle-version: '8.11.1'" in workflow,
    "CI installs compile SDK 36": "'platforms;android-36'" in workflow
        and "'build-tools;35.0.0'" in workflow,
    "Android install targets remain unchanged": bool(re.search(r"(?m)^\s*minSdk\s+29\s*$", app))
        and bool(re.search(r"(?m)^\s*targetSdk\s+34\s*$", app)),
    "versionCode": bool(re.search(r"(?m)^\s*versionCode\s+" + CODE + r"\s*$", app)),
    "versionName": "versionName '" + VERSION + "'" in app,
    "workflow version": "version='" + VERSION + "'" in workflow,
    "release publisher version": "VERSION: " + VERSION in workflow,
    "publication directly in Releases": "bash tools/publish_apk.sh" in workflow,
    "no ZIP Artifacts": "upload-artifact" not in workflow and "download-artifact" not in workflow,
    "unit test and Debug compile": ":app:testDebugUnitTest :app:assembleDebug" in workflow,
    "APK version validation": all(x in workflow for x in ["aapt", "versionCode='35'", "versionName='" + VERSION + "'"]),
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
            for x in ["showFavorites", "showFavoriteTools", "Salvar esta página", "openFavoriteSettings"])
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
        and "Próxima versão sequencial: `0.1.0-alpha." + str(int(CODE) + 1) + "`" in read("docs/VERSIONING.md"),
    "login compatibility": "AuthNavigation.isGoogleOAuth" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "openInExternalBrowser" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "setAcceptThirdPartyCookies(web,appliedThirdPartyCookies)" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "thirdPartyCookies" in read("app/src/main/java/br/com/baixafacil/BrowserPrefs.java")
        and "chooseCookieCompatibility" in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
        and "class AuthNavigationTest" in read("app/src/test/java/br/com/baixafacil/AuthNavigationTest.java"),
    "login popup safely integrated": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        for x in ["setSupportMultipleWindows(true)", "onCreateWindow", "LoginWindowPolicy.allowPopup", "setAllowFileAccess(false)", "setAcceptThirdPartyCookies(popup", "loginPopupDialog.dismiss()", "showLoginDiagnosis", "onReceivedHttpError", "WebView.getCurrentWebViewPackage"])
        and all(x in read("app/src/main/java/br/com/baixafacil/LoginWindowPolicy.java")
            for x in ["allowPopup", "safeWebAddress", "hasLoginParameters", "originForReport", "isSafeInternalFrame"])
        and "class LoginWindowPolicyTest" in read("app/src/test/java/br/com/baixafacil/LoginWindowPolicyTest.java"),
    "auth tokens excluded from history": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPrefs.java")
        for x in ["AuthNavigation.isSensitiveLoginPage", "LoginWindowPolicy.hasLoginParameters"]),
    "AndroidX WebKit compatibility": "androidx.webkit:webkit:1.17.1" in app
        and "WebKitCompat.optimize(options)" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        and "WebViewFeature.DOWNLOAD_FAVICONS_ENABLED" in read("app/src/main/java/br/com/baixafacil/WebKitCompat.java")
        and "onRenderProcessGone" in read("app/src/main/java/br/com/baixafacil/BrowserPane.java"),
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
    "adblock only web subresources": all(x in read("app/src/main/java/br/com/baixafacil/BrowserPane.java")
        for x in ["!request.isForMainFrame()", '"GET".equalsIgnoreCase(request.getMethod())', "adBlock.shouldBlock", "applyCosmeticFilters", "showAdBlockMenu"]),
    "adblock settings and per-site controls": all(x in read("app/src/main/java/br/com/baixafacil/SettingsActivity.java")
        for x in ["chooseAdBlockMode", "updateAdLists", "Bloquear rastreadores", "Ocultar anúncios na página"]),
    "documentation": all(VERSION in read(x) for x in ["README.md", "RELEASE_NOTES.md", "docs/VERSIONING.md", "docs/VALIDATION.md"]),
}
for label, valid in checks.items():
    print(("OK" if valid else "ERRO") + ": " + label)
if not all(checks.values()):
    raise SystemExit(1)
print("Pré-validação concluída.")
