# Validação 0.1.0-alpha.39

## Mudanças desta versão

- Novo diálogo visual de confirmação de download usando `DownloadDialogs`, com resumo da origem, do arquivo, do destino e aviso para HTTP não criptografado.
- Correção defensiva dos atalhos rápidos **Abrir** e **Compartilhar** na lista de downloads concluídos com `safeUri`, tratamento de `ActivityNotFoundException`, `SecurityException`, `IllegalArgumentException` e outros erros de runtime.
- Nova captura local opcional `AppEventLog` com registros de alertas/erros e `UncaughtExceptionHandler` global via `BaixaFacilApp`.
- Nova seção **Configurações > Diagnóstico e alertas** com ativação, visualização, cópia e limpeza dos registros locais.

## Verificações locais

- `python3 tools/ci_preflight.py`
- `bash tools/test_publish_mock.sh`
- Busca de sincronização de versão (`0.1.0-alpha.39`, `versionCode 39`) em Gradle, workflow, docs, User-Agent e tela Sobre.
- Inspeção sintática/estrutural dos arquivos Java modificados (`MainActivity`, `BrowserPane`, `SettingsActivity`, `MediaPlayerActivity`, `DownloadDialogs`, `AppEventLog`, `BaixaFacilApp`).

## Cenários funcionais para Android real

- Abrir um download concluído pela ação rápida do card e confirmar que o app não fecha, inclusive quando não houver app compatível instalado.
- Compartilhar um download concluído pela ação rápida do card e confirmar que falhas são tratadas sem crash.
- Iniciar um download a partir do navegador e pelo campo manual, validando o novo diálogo visual e o aviso de HTTP quando aplicável.
- Ativar **Configurações > Diagnóstico e alertas**, provocar um erro tratável (por exemplo, compartilhar item sem handler disponível) e verificar o registro local/copiar relatório.
- Confirmar que os registros não exibem cookies, tokens nem URLs completas com parâmetros sensíveis.

## Pendências

- Executar `:app:testDebugUnitTest` e `:app:assembleDebug` no GitHub Actions.
- Validar a experiência visual final do novo diálogo e da tela de diagnósticos em aparelho físico.
