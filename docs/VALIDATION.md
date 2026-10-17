# Validação 0.1.0-alpha.32

## Agrupamento de vídeos

- Testar links HLS com assinaturas distintas no mesmo caminho: um único resultado, URL mais recente para ação.
- Testar playlists 360p, 720p e 1080p do mesmo caminho lógico: grupo único e ordenado, opção de fonte obrigatória.
- Testar conteúdos distintos no mesmo CDN e IDs distintos na query: resultados separados.
- Não exibir parâmetros assinados em rótulos, descrições, diagnósticos ou histórico.
- Preservar diferenciação de download direto vs. HLS/DASH vs. formato não verificado.

## Validações locais

- `python3 tools/ci_preflight.py`
- `bash tools/test_publish_mock.sh`
- Compilar e executar testes JVM `VideoMenuModelTest` com 11 casos.
- Analisar sintaticamente arquivos Java e validar XML/YAML/ZIP.

## Pendências

- Compilação Android completa no GitHub Actions, testes de interface e execução no aparelho.
- Reprodução e download reais dependem da disponibilidade e autorização do servidor da mídia.
