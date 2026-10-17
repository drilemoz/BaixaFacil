package br.com.baixafacil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regras HTTP puras, verificáveis sem runtime Android. */
public final class TransferPolicy {
    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+|\\*)", Pattern.CASE_INSENSITIVE);
    public static final int MAX_ATTEMPTS = 4; // tentativa inicial + três tentativas adicionais

    private TransferPolicy() {}

    public static boolean retryHttp(int status) {
        return status == 408 || status == 425 || status == 429 || (status >= 500 && status <= 599);
    }

    public static long retryDelayMillis(int completedAttempts) {
        if (completedAttempts <= 0) return 0;
        return Math.min(8000L, 2000L << Math.min(2, completedAttempts - 1));
    }

    /** Um ETag fraco não serve para If-Range. Prefere ETag forte a Last-Modified. */
    public static String ifRange(String etag, String lastModified) {
        if (etag != null && !etag.trim().isEmpty() && !etag.trim().startsWith("W/") && !hasControl(etag)) return etag.trim();
        if (lastModified != null && !lastModified.trim().isEmpty() && !hasControl(lastModified)) return lastModified.trim();
        return null;
    }

    private static boolean hasControl(String text) {
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) < 32 || text.charAt(i) == 127) return true;
        return false;
    }

    /** Aceita somente resposta de range único, com início exato e dimensões coerentes. */
    public static long rangeTotal(String header, long expectedStart, long contentLength) {
        if (header == null) return -1;
        Matcher match = CONTENT_RANGE.matcher(header.trim());
        if (!match.matches()) return -1;
        try {
            long start = Long.parseLong(match.group(1));
            long end = Long.parseLong(match.group(2));
            if (start != expectedStart || end < start) return -1;
            long piece = Math.addExact(Math.subtractExact(end, start), 1L);
            if (contentLength >= 0 && piece != contentLength) return -1;
            if ("*".equals(match.group(3))) return -1; // não é possível verificar conclusão
            long total = Long.parseLong(match.group(3));
            if (total <= end || total < expectedStart) return -1;
            return total;
        } catch (NumberFormatException | ArithmeticException invalid) {
            return -1;
        }
    }
}
