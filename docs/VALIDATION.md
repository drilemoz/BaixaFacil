# Validação 0.1.0-alpha.34

## Pontos alterados

- Validar varredura após rolar por vídeos/Reels, sem chamadas repetidas a cada frame ou execução em páginas sensíveis de autenticação.
- Confirmar que a relação entre título/thumb e URL utiliza o elemento de vídeo ou identidade segura do mesmo arquivo; outros recursos devem aparecer como sem associação visual confirmada.
- Confirmar deduplicação de espelhos `scontent.*.fbcdn.net` com mesmo caminho longo e parâmetros de identidade iguais; vídeos distintos não são fundidos.
- Miniatura: tentar frame canvas somente com política padrão de origem, fallback WebView; nem JavaScript nem Android fazem downloads adicionais para thumbnails.
- Regressão: download só de arquivo direto, HLS/DASH apenas player online, DRM não suportado; controles de login, anúncios e rede intactos.

## Testes executados

- `python3 tools/ci_preflight.py`.
- `bash tools/test_publish_mock.sh`.
- JVM `VideoMenuModelTest` e `VideoPresentationTest` com executor de JUnit compatível.
- Teste de sintaxe e execução JavaScript em Node.js com player simulado.
- Parser sintático de fontes Java com JavacTask e integridade/estrutura ZIP.

## Pendências

- Build Android/Gradle completo no GitHub Actions e validação de interface no aparelho.
- Testar no Facebook e em sites de vídeos, considerando URLs temporárias/autenticação e restrições de origem.
