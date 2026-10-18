# Baixa Fácil 0.1.0-alpha.43 (versionCode 43)

## Novidades

- Filtros de downloads por **tamanho** e **site de origem** combinados com pesquisa, estado e tipo.
- Tamanhos: todos, desconhecido, menos de 10 MB, 10 a 100 MB, 100 MB ou mais. Sem extrapolar bytes parciais como tamanho do arquivo.
- Lista local dos principais domínios do histórico, mostrando somente o host, nunca a URL completa ou seus tokens.
- Faixa de filtros ativos e botão **Limpar filtros**, com aviso específico quando nenhum download atende à seleção.
- Seleção múltipla respeita a lista filtrada; alterar filtros cancela seleções anteriores para proteger arquivos ocultos.

## Segurança e validação

- Não modifica arquivos, histórico nem fila ao aplicar filtros.
- 10 testes JVM de domínio/tamanho e verificação de consistência estrutural, preflight e simulação de publicação.
- **Pendente:** compilação `:app:testDebugUnitTest :app:assembleDebug` no GitHub Actions e teste visual Android real. Este ZIP contém o código-fonte, não um APK.
