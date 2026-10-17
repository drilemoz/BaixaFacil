package br.com.baixafacil;

import static org.junit.Assert.*;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import org.junit.Test;

public class TransferPolicyTest {
    @Test public void retriableHttpStatuses() {
        for (int code : new int[]{408, 425, 429, 500, 502, 503, 504, 599}) assertTrue(TransferPolicy.retryHttp(code));
        for (int code : new int[]{200, 206, 301, 400, 401, 403, 404, 416}) assertFalse(TransferPolicy.retryHttp(code));
    }
    @Test public void backoff() {
        assertEquals(0, TransferPolicy.retryDelayMillis(0));
        assertEquals(2000, TransferPolicy.retryDelayMillis(1));
        assertEquals(4000, TransferPolicy.retryDelayMillis(2));
        assertEquals(8000, TransferPolicy.retryDelayMillis(3));
        assertEquals(8000, TransferPolicy.retryDelayMillis(10));
    }
    @Test public void contentRangeValidatesExactStartAndTotal() {
        assertEquals(1000, TransferPolicy.rangeTotal("bytes 200-999/1000", 200, 800));
        assertEquals(1000, TransferPolicy.rangeTotal("bytes 200-999/1000", 200, -1));
        assertEquals(-1, TransferPolicy.rangeTotal("bytes 201-999/1000", 200, 799));
        assertEquals(-1, TransferPolicy.rangeTotal("bytes 200-999/1000", 200, 700));
        assertEquals(-1, TransferPolicy.rangeTotal("bytes 200-1000/1000", 200, 801));
        assertEquals(-1, TransferPolicy.rangeTotal("bytes */1000", 200, 800));
        assertEquals(-1, TransferPolicy.rangeTotal("bytes 200-999/*", 200, 800));
        assertEquals(-1, TransferPolicy.rangeTotal("bytes 999-200/1000", 999, -1));
        assertEquals(-1, TransferPolicy.rangeTotal("bytes 1-9223372036854775807/99999999999999999999999", 1, -1));
    }
    @Test public void validatorRequiresStrongEtagOrModificationDate() {
        assertEquals("\"abc\"", TransferPolicy.ifRange("\"abc\"", "Mon, 01 Jan 2024 12:00:00 GMT"));
        assertEquals("Mon, 01 Jan 2024 12:00:00 GMT", TransferPolicy.ifRange("W/\"abc\"", "Mon, 01 Jan 2024 12:00:00 GMT"));
        assertNull(TransferPolicy.ifRange("W/\"abc\"", null));
        assertNull(TransferPolicy.ifRange("\"abc\"\r\nX-Test: injected", null));
        assertNull(TransferPolicy.ifRange(null, null));
    }
    @Test public void classifiesNetworkSecurityAndHttp() {
        assertEquals("DNS", TransferDiagnostics.classify(new UnknownHostException()).code);
        assertTrue(TransferDiagnostics.classify(new UnknownHostException()).retryable);
        assertEquals("TEMPO", TransferDiagnostics.classify(new SocketTimeoutException()).code);
        assertFalse(TransferDiagnostics.classify(new SSLException("bad certificate")).retryable);
        assertEquals("TLS", TransferDiagnostics.classify(new SSLException("bad certificate")).code);
        assertEquals("HTTP_403", TransferDiagnostics.http(403).code);
        assertFalse(TransferDiagnostics.http(403).retryable);
        assertTrue(TransferDiagnostics.http(503).retryable);
    }
}
