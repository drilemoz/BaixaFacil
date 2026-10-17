# Baixa Fácil 0.1.0-alpha.36 (versionCode 36)

## Novidades

- Nova tela **Cookies e sessões** nas Configurações, com controle de permanência de logins entre inicializações.
- Opções para apagar cookies globalmente, limpar dados locais do navegador e remover armazenamento local de uma origem individual.
- Barra de endereços com sugestões locais ao digitar, usando favoritos e histórico, até cinco resultados e sem requisições de pesquisa durante a digitação.
- Filtro de endereços sensíveis e links com tokens de login.

## Limitações e segurança

- A API pública da WebView não fornece uma lista de cookies de todos os sites. O gerenciador exibe apenas origens com WebStorage, que não é a mesma coisa que cookies.
- Desativar a permanência dos logins apaga cookies e WebStorage na próxima inicialização completa do processo; não no retorno de Configurações.
- Não mudou o bloqueio padrão de cookies de terceiros nem as restrições de downloads, vídeos e OAuth do Google.

## Validação

- Pré-validação e teste de publicação simulada são executados localmente.
- Teste de comportamento real, interface e build Android completo ainda exigem GitHub Actions/aparelho.
