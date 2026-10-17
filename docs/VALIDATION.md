# Validação 0.1.0-alpha.25

## Mudanças de compatibilidade

1. `WebChromeClient.onCreateWindow` cria WebView de popup isolada apenas com gesto explícito, impedindo janelas automáticas e popups aninhados.
2. A WebView secundária preserva permissões e restrições de segurança da principal; cookies de terceiros só são permitidos por preferência explícita.
3. URLs OAuth do Google são encaminhadas ao navegador externo, sem importar cookies ou imitar outro User-Agent.
4. O menu de diagnóstico exibe versão da WebView e falhas HTTP/rede sem caminhos, senhas, cookies, query ou fragmento das URLs.
5. Histórico descarta páginas sensíveis e query strings com códigos de login; subframes internos `about:blank`/`about:srcdoc` são suportados.

## Testes locais previstos

- `tools/ci_preflight.py`: versões, workflow, políticas e controles do navegador.
- `tools/test_publish_mock.sh`: publicar/atualizar Release e detectar divergência SHA-256.
- Compilar e executar testes Java/JVM de `LoginWindowPolicy` e `AuthNavigation`.
- Parser Java para a árvore de fontes/testes.
- XML/YAML e CRC do ZIP.

## Pendências de integração no aparelho

- Testar fluxo de login normal e popup no Facebook, inclusive CAPTCHA; validação do site pode rejeitar WebView.
- Testar Google OAuth para confirmar encaminhamento externo e ausência de compartilhamento indevido de cookies.
- Testar abertura e fechamento repetidos de popup, mudança de tema e retomada das páginas.
- Executar `:app:assembleDebug` no GitHub Actions. Não afirmar APK validado apenas com checagens locais.
