# Validação 0.1.0-alpha.43

## Escopo

- Novo `DownloadListFilters.java` para tamanho e site (host HTTP(S) somente, sem credenciais, query ou fragmento).
- `MainActivity`: menu com filtros por tamanho e site, barra de filtros ativos, limpar filtros e estado vazio específico.
- `DownloadAdapter.reload`: critérios combinados com pesquisa, tipo e status; nunca alterar transferências ou arquivos.
- A ação `Selecionar visíveis` usa `adapter.getCount`/`adapter.getItem` já filtrados; mudança de qualquer filtro elimina seleção múltipla anterior.

## Testes locais

- `DownloadListFiltersTest`: 10 casos JVM cobrindo limites, tamanho desconhecido, bytes parciais versus concluídos, classificação de hosts, URLs com credenciais e ordenação por contagem.
- `python3 tools/ci_preflight.py`
- `bash tools/test_publish_mock.sh`
- Parser sintático de todas as fontes Java, XML/YAML e verificação CRC do ZIP.

## Cenários para testar no aparelho

- Aplicar sucessivamente Tipo + Tamanho + Site; confirmar que somente arquivos com todas as condições aparecem.
- Testar exatamente 10 MiB e 100 MiB; verificar o filtro "Desconhecido" quando o servidor não forneceu total.
- Validar estado vazio, barra de filtros ativos e botão Limpar.
- Selecionar múltiplos arquivos e tocar Selecionar visíveis; alterar o filtro e confirmar que a seleção anterior desaparece.
- Verificar que o menu de sites mostra somente domínio, sem parâmetros de autenticação.
- Executar `:app:testDebugUnitTest` e `:app:assembleDebug` no GitHub Actions; testar visualmente na tela Android.

## Limitações

- Filtro por tamanho usa metadados do histórico; não verifica tamanho físico de arquivos por MediaStore nem faz HEAD adicional.
- Hosts ficam limitados aos 30 mais frequentes para manter o diálogo compacto. Pesquisa local pode encontrar registros de outros sites.
- Compilação completa Android não executada no ambiente local por ausência do Android SDK/Gradle.
