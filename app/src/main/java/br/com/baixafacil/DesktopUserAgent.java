package br.com.baixafacil;

import java.util.regex.Pattern;

/** Solicita layout desktop sem forjar versão fixa de Chrome/WebView. */
public final class DesktopUserAgent {
    private static final Pattern ANDROID_DEVICE = Pattern.compile("\\([^)]*Android[^)]*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOBILE_TOKEN = Pattern.compile("(?i)\\bMobile\\s*");
    private DesktopUserAgent() {}

    public static String toDesktop(String original) {
        if (original == null || original.trim().isEmpty()) return original;
        String desktop = ANDROID_DEVICE.matcher(original).replaceFirst("(X11; Linux x86_64)");
        desktop = MOBILE_TOKEN.matcher(desktop).replaceAll("");
        return desktop.replaceAll("\\s{2,}", " ").trim();
    }
}
