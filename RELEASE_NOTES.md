# Baixa Fácil 0.1.0-alpha.35 (versionCode 35)

## Melhorias de desempenho e compatibilidade para ChatGPT

- Perfil de navegação leve automático em `chatgpt.com` (e variantes oficiais), reduzindo processos opcionais dentro do navegador incorporado.
- Detecção de vídeos, análise de subrecursos pelo bloqueador e CSS cosmético são evitados nesses sites; os recursos permanecem nos demais sites.
- Nova opção **Configurações > Navegador > Compatibilidade com ChatGPT**, ligada por padrão.
- Menos atualizações visuais durante o carregamento de páginas complexas.
- Diagnóstico de renderizador WebView sem resposta e botão de acesso rápido a navegador externo em páginas do ChatGPT.

## Observações

- Não desativa JavaScript, cookies essenciais, segurança TLS ou acesso seguro; não contorna restrições de login Google OAuth.
- Reduz trabalho adicional do Baixa Fácil, mas não garante corrigir problemas do próprio site, de memória, de rede ou do componente WebView instalado.
- APK e testes reais em aparelho dependem do GitHub Actions e de validação física.
