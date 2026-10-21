package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regressões do agendador de UI e sinalizações reais de pressão de RAM. */
public class MediaWorkPolicyTest {
    @Test public void downloadsAtivosMantemFrequenciaAnterior(){
        assertEquals(1200L,MediaWorkPolicy.refreshDelay(true,true));
        assertTrue(MediaWorkPolicy.needsFrequentRefresh("Baixando"));
        assertTrue(MediaWorkPolicy.needsFrequentRefresh("Conectando"));
        assertTrue(MediaWorkPolicy.needsFrequentRefresh("Retomando"));
        assertTrue(MediaWorkPolicy.needsFrequentRefresh("Na fila"));
        assertTrue(MediaWorkPolicy.needsFrequentRefresh("Aguardando"));
    }
    @Test public void ociosoNaoAcordaInterfaceComMesmaFrequencia(){
        assertEquals(6000L,MediaWorkPolicy.refreshDelay(true,false));
        for(String status:new String[]{"Concluído","Pausado","Falhou","Interrompido","Cancelado"})
            assertFalse(MediaWorkPolicy.needsFrequentRefresh(status));
    }
    @Test public void activityOcultaNaoAgendaTrabalho(){
        assertEquals(-1L,MediaWorkPolicy.refreshDelay(false,true));
        assertEquals(-1L,MediaWorkPolicy.refreshDelay(false,false));
    }
    @Test public void poucaMemoriaDescartaCacheSemTratarUiHiddenComoFalha(){
        assertFalse(MediaWorkPolicy.shouldTrimPreviewCache(20));
        assertTrue(MediaWorkPolicy.shouldTrimPreviewCache(10));
        assertTrue(MediaWorkPolicy.shouldTrimPreviewCache(15));
        assertTrue(MediaWorkPolicy.shouldTrimPreviewCache(40));
        assertTrue(MediaWorkPolicy.shouldTrimPreviewCache(80));
    }
    @Test public void limitesEvitamFilaIlimitada(){
        assertEquals(10,MediaWorkPolicy.MAX_PENDING_PREVIEWS);
        assertEquals(8,MediaWorkPolicy.MAX_LISTENERS_PER_PREVIEW);
    }
}
