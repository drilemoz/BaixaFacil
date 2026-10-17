# Baixa Fácil 0.1.0-alpha.28 (versionCode 28)

- Central de downloads ganhou **miniaturas de vídeos e imagens concluídos**, geradas a partir do MediaStore sem internet.
- Para vídeos locais, lê duração e resolução reais com MediaMetadataRetriever; menu **Detalhes da mídia** exibe os dados quando disponíveis.
- Geração ocorre fora da interface, em fila de uma thread com cache LRU de até 4 MB; formatos sem prévia mantêm o ícone anterior.
- Preservados Media3 e WebKit, evitando consumo extra em arquivos ainda baixando.
