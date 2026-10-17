# Validação 0.1.0-alpha.36

## Cookies e sessões

- Conferir `WebStorage.getOrigins` e `deleteOrigin` sem confundir limpeza por origem com limpeza de cookies.
- Conferir `CookieManager.removeAllCookies` assíncrono, flush e confirmação explícita; preservar dados de downloads e favoritos.
- Testar opção Manter logins: ativada por padrão; ao desativar, limpar somente na próxima inicialização completa do processo, antes de abrir a página inicial.
- Nunca incluir cookies ou tokens em diagnósticos.

## Sugestões da barra de endereços

- Filtrar a entrada local por domínio, título e URL, máximo cinco sugestões, favoritos e histórico, sem consultas remotas por tecla.
- Rejeitar URL com credenciais, esquemas proibidos, páginas sensíveis e parâmetros de autenticação.
- Escolher sugestão deve navegar normalmente e fechar a lista; perder foco também deve fechar.

## Verificações locais

- `python3 tools/ci_preflight.py`
- `bash tools/test_publish_mock.sh`
- JVM: `BrowserSuggestions` + UrlTools/LoginWindowPolicy/AuthNavigation
- Sintaxe Java, YAML e ZIP; build Android completo e testes em aparelho pendentes.
