# Baixa Fácil 0.1.0-alpha.39 (versionCode 39)

## Novidades principais

- Diálogo **Baixar com Baixa Fácil?** modernizado, com ícones, resumo visual da origem, do arquivo e do destino, além de aviso destacado para links HTTP sem criptografia.
- Corrigido o crash ao tocar nos dois atalhos rápidos da tela **Meus downloads** (abrir e compartilhar).
- Nova área **Configurações > Diagnóstico e alertas** para ativar captura local de erros/alertas, abrir os registros, copiar relatório e limpar o histórico.
- Captura opcional de crashes inesperados por handler global, preservando a privacidade: sem cookies, sem tokens e sem URLs completas com query.

## Validação

- Verificações locais previstas: `python3 tools/ci_preflight.py`, `bash tools/test_publish_mock.sh` e checagem sintática/estrutural dos arquivos Java, XML, Markdown e workflow.
- A correção do crash dos atalhos rápidos depende de validação final em aparelho Android com arquivos reais concluídos.
- **Pendente:** repetir `:app:testDebugUnitTest :app:assembleDebug` no GitHub Actions; este pacote é o código-fonte, não o APK.
