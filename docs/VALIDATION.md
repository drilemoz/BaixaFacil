# Validação 0.1.0-alpha.41

## Inspeção preventiva

- `python3 tools/ci_preflight.py` com checagens de dependência `androidx.browser:browser:1.10.0`, `CustomTabsClient`, pacote explícito, `<queries>` da API Android 11+ e menu/preferência visíveis.
- `bash tools/test_publish_mock.sh` para simular criação, atualização e divergência de SHA-256 na publicação de APK.
- `DownloadCleanupPolicyTest` para estados de limpeza e proteção de registros em andamento/pausados e validação da URI MediaStore.
- Análise da sintaxe Java, XML, YAML e integridade de ZIP.

## Testes funcionais pendentes no dispositivo

1. Instalar APK no Android e validar Custom Tabs com Chrome/Firefox: abrir página, voltar ao app, verificar cookies separados e opção desativada para login.
2. Abrir Google OAuth e confirmar que ele é encaminhado ao navegador compatível; sem expectativa de sessão sincronizada na WebView.
3. Adicionar downloads concluídos, pausados, em fila e falhos com fragmento. **Limpar lista** deve retirar apenas finalizados e preservar arquivos/retomadas.
4. **Apagar arquivos e limpar lista** deve excluir somente arquivos MediaStore concluídos com permissão, preservar os registros quando a exclusão falhar e deixar ativos/pausados intactos.
5. Repetir em temas claro e AMOLED, Android 10 e Android 14+ quando possível.

## Limitação do ambiente local

Compilação `:app:testDebugUnitTest :app:assembleDebug` e teste de execução Android pendentes até build com SDK/Gradle no GitHub Actions. Sintaxe Java aprovada isoladamente não garante resolução de APIs Android.
