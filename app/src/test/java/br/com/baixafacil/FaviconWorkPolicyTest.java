package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

/** Limites de fila de favicons e suspensão de miniaturas durante rolagem. */
public class FaviconWorkPolicyTest {
    @Test public void limitesPositivosEAcimaDosTrabalhosAtivos(){
        assertTrue(FaviconWorkPolicy.MAX_PENDING_ORIGINS<=24);
        assertTrue(FaviconWorkPolicy.MAX_WAITERS_PER_ORIGIN<=8);
        assertTrue(FaviconWorkPolicy.MAX_FAILED_ORIGINS<=96);
        assertEquals(65536,FaviconWorkPolicy.MAX_RESPONSE_BYTES);
    }
    @Test public void falhasTemValidadeEUmRelogioMonotonico(){
        long start=1000L;
        assertTrue(FaviconWorkPolicy.stillCoolingDown(start,start));
        assertTrue(FaviconWorkPolicy.stillCoolingDown(start+299999,start));
        assertFalse(FaviconWorkPolicy.stillCoolingDown(start+300000,start));
        assertFalse(FaviconWorkPolicy.stillCoolingDown(0,start));
    }
    @Test public void cacheNaoReciclaAoOcultarUi(){
        assertFalse(FaviconWorkPolicy.shouldTrimCache(20));
        assertTrue(FaviconWorkPolicy.shouldTrimCache(10));
        assertTrue(FaviconWorkPolicy.shouldTrimCache(15));
        assertTrue(FaviconWorkPolicy.shouldTrimCache(40));
    }
    @Test public void miniaturasSoNaListaParadaEVisivel(){
        assertTrue(FaviconWorkPolicy.shouldLoadPreview(true,false,true));
        assertFalse(FaviconWorkPolicy.shouldLoadPreview(true,true,true));
        assertFalse(FaviconWorkPolicy.shouldLoadPreview(true,false,false));
        assertFalse(FaviconWorkPolicy.shouldLoadPreview(false,false,true));
    }
}
