package br.com.baixafacil;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/** Classificação sem expor URL, token de autenticação ou rastreamento da exceção. */
public final class TransferDiagnostics {
    private TransferDiagnostics() {}

    public static final class Failure extends IOException {
        public final String code;
        public final boolean retryable;
        public Failure(String code, String message, boolean retryable) {
            super(message);
            this.code = code;
            this.retryable = retryable;
        }
    }
    public static Failure http(int status) {
        String advice;
        if (status == 401 || status == 403) advice = "O arquivo exige autorização ou o link expirou.";
        else if (status == 404 || status == 410) advice = "Arquivo não encontrado ou link expirado.";
        else if (status == 429) advice = "Servidor limitou as solicitações.";
        else if (status >= 500) advice = "Servidor temporariamente indisponível.";
        else advice = "O servidor recusou a transferência.";
        return new Failure("HTTP_" + status, "HTTP " + status + ": " + advice, TransferPolicy.retryHttp(status));
    }
    public static Failure classify(Throwable cause) {
        if (cause instanceof Failure) return (Failure) cause;
        if (cause instanceof SSLException) return new Failure("TLS", "Falha de segurança HTTPS/certificado.", false);
        if (cause instanceof UnknownHostException) return new Failure("DNS", "Não foi possível localizar o servidor (DNS/rede).", true);
        if (cause instanceof SocketTimeoutException) return new Failure("TEMPO", "Tempo de conexão ou resposta esgotado.", true);
        if (cause instanceof ConnectException || cause instanceof NoRouteToHostException) {
            return new Failure("CONEXAO", "Não foi possível conectar ao servidor.", true);
        }
        if (cause instanceof EOFException || cause instanceof SocketException) {
            return new Failure("REDE", "Conexão interrompida durante a transferência.", true);
        }
        if (cause instanceof IOException) return new Failure("IO", "Erro de entrada/saída ou rede: " + cause.getClass().getSimpleName(), true);
        return new Failure("ERRO", "Falha inesperada: " + cause.getClass().getSimpleName(), false);
    }
}
