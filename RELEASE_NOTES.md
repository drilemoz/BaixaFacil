# Baixa Fácil 0.1.0-alpha.32 (versionCode 32)

## Vídeos sem repetições desnecessárias

- Links repetidos para o mesmo arquivo ou playlist agora formam uma única entrada quando a identidade do caminho é a mesma, mesmo que a URL assinada mude.
- Quando o caminho indica qualidades diferentes do mesmo vídeo (como 720p e 1080p), as fontes compatíveis ficam agrupadas e aparecem em ordem de resolução estimada.
- É possível escolher a fonte no menu antes de reproduzir ou, em arquivos diretos, baixar.
- O contador de vídeos passa a mostrar quantas entradas agrupadas foram identificadas.
- Vídeos diferentes no mesmo domínio continuam separados; URLs com identidade da mídia na query também não são misturadas.

## Limitações

- Qualidade é uma estimativa textual extraída do endereço, não verificação técnica do conteúdo.
- HLS/DASH apenas online via Media3. Não há captura de DRM, conversão de streaming em MP4 ou integração com cookies externos.
- A detecção pode deixar de agrupar fontes quando o site não fornece identidade suficiente; essa escolha evita fundir vídeos diferentes por engano.

## Validação

- 11 testes JVM do modelo de vídeos aprovados, incluindo 7 cenários novos.
- Preflight, testes de publicação simulada, análise sintática Java, XML e integridade ZIP devem ser conferidos antes de publicar.
- Build Android completo e visual no aparelho pendentes do workflow.
