# Baixa Fácil 0.1.0-alpha.34 (versionCode 34)

## Novidades

- **Reels e feeds:** a busca de vídeos reage à rolagem, identifica quando a própria página vincula um arquivo ao player visível e prioriza o vídeo em reprodução.
- **Mais informações:** título e legenda curta quando encontrados próximos ao player correspondente; não tenta adivinhar nomes pelo domínio do arquivo.
- **Thumb melhorada:** captura limitada de frame com canvas quando permitido, ou recorte da WebView visível. Caso indisponível, mantém ícone de vídeo.
- **Menos duplicatas:** une URLs reassinadas e espelhos Facebook somente quando o caminho e a identidade da mídia conferem, sem misturar vídeos diferentes.
- **Transparência:** fontes sem correspondência visual confirmada são identificadas na lista.

## Limites

- A WebView pode restringir captura de frames e leitura de vídeos de outra origem. Links temporários podem exigir sessão, expirar ou não funcionar no Media3.
- Downloads apenas de arquivos HTTP(S) diretos; HLS/DASH apenas reprodução online; sem DRM ou captura fora das permissões do navegador.

## Verificações

- Testes JVM do agrupamento e dos metadados, validação JavaScript e validação prévia da CI.
- Build Android completo e teste com Facebook real pendentes no workflow e no celular.
