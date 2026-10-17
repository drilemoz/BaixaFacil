package br.com.baixafacil;

/** Regras puras de rede/bateria; sem APIs Android, para testes JVM. */
final class DownloadPolicy {
    private DownloadPolicy() {}

    static String restriction(boolean wifiOnly, boolean mobileAllowed, boolean protectBattery,
                              int threshold, boolean connected, boolean wifi, boolean ethernet,
                              boolean cellular, boolean charging, int batteryPercent) {
        if (!connected) return "Sem conexão de rede para iniciar ou continuar o download.";
        if (wifiOnly && !wifi) return "Download pausado: conecte-se a uma rede Wi-Fi.";
        if (!wifiOnly && !mobileAllowed && !wifi && !ethernet) {
            return "Download pausado: dados móveis e redes não identificadas estão bloqueados.";
        }
        if (protectBattery && batteryPercent >= 0 && batteryPercent <= threshold && !charging) {
            return "Download pausado: bateria em " + batteryPercent + "% (limite " + threshold + "%). Conecte o carregador ou altere a configuração.";
        }
        return null;
    }
}
