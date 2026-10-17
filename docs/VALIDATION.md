# Validação 0.1.0-alpha.38

## Correção de compilação Gerar APK 10

- Antes: `SettingsActivity.java:163` tentava iterar `WebStorage.getOrigins` (Map bruto) diretamente como `Map.Entry<String,WebStorage.Origin>`, falhando em `:app:compileDebugJavaWithJavac`.
- Agora: `WebStorageOrigins.checked` valida tipos de chave/valor e origem HTTP(S), retornando lista tipada.
- Executar `WebStorageOriginsTest` (5 casos) e contrato Java com callback raw `Map`, além de `python3 tools/ci_preflight.py` e `bash tools/test_publish_mock.sh`.
- Build Android final e testes de UI pendentes de GitHub Actions.

## Cenários funcionais no Android real

- Entrar no ChatGPT, alternar para Downloads e retornar: o site deve continuar vivo; temporizadores JavaScript devem voltar ao primeiro plano.
- Rolar feed de vídeos: varreduras não devem ocorrer mais de uma vez a cada 3,5 segundos (independente do debounce). Alternar de página invalida resultados antigos.
- Com pressão de memória, interromper apenas varreduras e descartar previews temporárias, mantendo downloads/cookies/histórico.
- Abrir ⋮ > Login e diagnóstico > Desempenho e memória: confirmar RAM disponível, heap Java, PSS (somente app), WebView e opção externa; nenhuma informação sensível ou URL com query no relatório.
- Process renderer crash: o Android deve conseguir recriar a WebView sem crash de todo o aplicativo; validar no aparelho.

## Verificações locais

- `python3 tools/ci_preflight.py`
- `bash tools/test_publish_mock.sh`
- Testes JVM da política `BrowserPerformancePolicyTest`.
- Java sintático, estrutura XML/YAML e integridade ZIP.

## Pendências

- `:app:testDebugUnitTest`, `:app:assembleDebug` e Android lint no GitHub Actions.
- Medições de memória/reprodução em dispositivo físico.
