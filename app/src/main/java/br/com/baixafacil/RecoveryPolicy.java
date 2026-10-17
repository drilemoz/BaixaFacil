package br.com.baixafacil;

/** Política explícita para restauração após encerramento do processo. */
public final class RecoveryPolicy {
    private RecoveryPolicy() {}
    public static boolean interrupted(String status) {
        return "Na fila".equals(status)||"Conectando".equals(status)
                ||"Baixando".equals(status)||"Retomando".equals(status)
                ||"Nova tentativa".equals(status);
    }
    public static boolean canRecover(String status) {
        return "Aguardando".equals(status)||"Interrompido".equals(status);
    }
}
