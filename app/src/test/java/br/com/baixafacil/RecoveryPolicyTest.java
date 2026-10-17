package br.com.baixafacil;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecoveryPolicyTest {
    @Test public void identifiesOrphanedDownloadStates() {
        for(String state:new String[]{"Na fila","Conectando","Baixando","Retomando","Nova tentativa"})
            assertTrue(RecoveryPolicy.interrupted(state));
    }
    @Test public void recoversOnlyExplicitlyPendingItems() {
        assertTrue(RecoveryPolicy.canRecover("Aguardando"));
        assertTrue(RecoveryPolicy.canRecover("Interrompido"));
        for(String state:new String[]{"Pausado","Falhou","Cancelado","Concluído"}) {
            assertFalse(RecoveryPolicy.canRecover(state));
            assertFalse(RecoveryPolicy.interrupted(state));
        }
        assertFalse(RecoveryPolicy.canRecover("Na fila"));
        assertTrue(RecoveryPolicy.interrupted("Na fila"));
    }
    @Test public void rejectsAbsentStatuses() {
        assertFalse(RecoveryPolicy.canRecover(null));
        assertFalse(RecoveryPolicy.interrupted(""));
    }
}
