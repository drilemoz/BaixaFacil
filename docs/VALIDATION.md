# Validação 0.1.0-alpha.23

## Implementação

- Nova classe `VideoLinks.java` para URLs HTTP/HTTPS diretas e identificação conservadora de HLS/DASH/segmentos.
- `BrowserPane`: inspeção HTML5 com `WebView.evaluateJavascript` e callback JSON. Não utiliza `addJavascriptInterface` nem cookies durante a detecção.
- `WebViewClient.shouldInterceptRequest` observa URLs reconhecidas sem interceptar o corpo, sempre retornando `null`.
- Botão Vídeos e lista de candidatos com Baixar e Copiar link.
- Limite de 24 URLs em memória por página e comprimento máximo de 4096 caracteres.

## Verificações locais

- 40 testes Java/JVM aprovados, incluindo 3 de `VideoLinks`.
- JavaScript executado em Node.js com DOM simulado: vídeo MP4, link WebM e vídeo `blob:`.
- Análise sintática de 35 fontes Java aprovada com `JavacTask.parse`.
- Pré-validação GitHub Actions e simulação de publicação SHA-256 aprovadas.

## Pendências

1. Executar `:app:testDebugUnitTest` e `:app:assembleDebug` no GitHub Actions.
2. Testar no Android detecção e download de um MP4 direto.
3. Testar vídeos do Facebook, especialmente carregamento dinâmico, blob/streaming e falta de URL direta.
4. Verificar o botão Vídeos em temas claro e AMOLED, e a troca Navegador/Downloads.
5. Confirmar que conteúdo de streaming ou páginas HTML não é falsamente apresentado como vídeo concluído.
