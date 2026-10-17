package br.com.baixafacil;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileNames {
    private static final Pattern ENCODED = Pattern.compile("(?:^|;)\\s*filename\\*\\s*=\\s*(?:\"?UTF-8''|UTF-8'')([^;\"]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIMPLE = Pattern.compile("(?:^|;)\\s*filename\\s*=\\s*(?:\"([^\"]+)\"|([^;]+))", Pattern.CASE_INSENSITIVE);
    private FileNames() {}

    public static String choose(String disposition, String url, String mime) {
        String name = null;
        if (disposition != null) {
            Matcher encoded = ENCODED.matcher(disposition);
            if (encoded.find()) {
                try { name = URLDecoder.decode(encoded.group(1).trim(), StandardCharsets.UTF_8.name()); }
                catch (Exception ignored) {}
            }
            if (name == null) {
                Matcher simple = SIMPLE.matcher(disposition);
                if (simple.find()) name = simple.group(1) != null ? simple.group(1) : simple.group(2);
            }
        }
        if (name == null || name.trim().isEmpty()) {
            try {
                String path = new URI(url).getRawPath();
                if (path != null && !path.endsWith("/")) {
                    name = URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1).replace("+", "%2B"), "UTF-8");
                }
            } catch (Exception ignored) {}
        }
        name = clean(name);
        if (name.equals("arquivo") && mime != null) {
            if (mime.toLowerCase(Locale.ROOT).startsWith("application/pdf")) name += ".pdf";
            else if (mime.toLowerCase(Locale.ROOT).startsWith("application/zip")) name += ".zip";
        }
        return name;
    }

    public static String clean(String value) {
        if (value == null) return "arquivo";
        // Nunca deixar um cabeçalho HTTP definir diretórios, caminhos absolutos ou nomes especiais.
        String clean = value.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1);
        clean = clean.replaceAll("[\\p{Cntrl}\\/:*?\"<>|]", "_").trim();
        while (clean.startsWith(".")) clean = clean.substring(1);
        while (clean.endsWith(".")) clean = clean.substring(0, clean.length()-1);
        if (clean.isEmpty()) return "arquivo";
        if (clean.length() > 120) clean = clean.substring(0, 120);
        return clean;
    }
}
