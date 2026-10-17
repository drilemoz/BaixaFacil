# Baixa Fácil 0.1.0-alpha.23 (versionCode 23)

## Vídeos no navegador

- Botão **Vídeos (N)** na barra inferior de informações do navegador e **⋮ > Procurar vídeos nesta página**.
- Detecção de URLs de vídeos diretos em elementos HTML5 `<video>/<source>`, links com extensões reconhecidas e requisições diretas de mídia.
- Lista de vídeos detectados com **Baixar** e **Copiar link**, usando o motor HTTP(S) atual.
- Busca automática após carregar a página e busca manual para conteúdo carregado mais tarde.
- Limitações: não faz download de `blob:`, DRM, playlists HLS/DASH, vídeos privados inacessíveis ou de conteúdo que não disponibilize um URL direto. Alguns vídeos do Facebook usam essas tecnologias.
- Não foram adicionadas permissões Android nem bibliotecas externas.

## Preservado

- Nome e identificador Baixa Fácil / `br.com.baixafacil`.
- Navegador com favoritos, histórico, modo computador e zoom.
- Lista compacta de downloads, recuperação manual, UIDT Android 14+ e controles de rede/bateria.

## Validação pendente

- Compilação Android completa pelo GitHub Actions e testes em páginas de vídeo HTML5 e Facebook no aparelho.
