# Validação 0.1.0-alpha.40

## Mudanças

- Correção de Java: eliminar `catch (SecurityException | IllegalArgumentException | RuntimeException)`, que é inválido porque os dois primeiros tipos derivam de `RuntimeException`.
- Atalhos Abrir/Enviar com rótulos e toques de 44 dp, compatíveis com os três temas do app.
- Proteção ao abrir/compartilhar URI `content://` com autoridade, incluindo a construção de ClipData e do Intent dentro de `try`.
- Proteção conservadora de duplicatas (`DownloadDuplicatePolicy`) com igualdade exata incluindo query e host, aplicada tanto ao navegador quanto à área de downloads.
- Eventos diagnósticos sem URL completa em falhas de carregamento da página principal e renderizador WebView.

## Testes automatizados locais

- `DownloadDuplicatePolicyTest` cobre URL ativa, pausada, interrompida, concluída, falha, alterada por assinatura, outro domínio e listas vazias.
- `python3 tools/ci_preflight.py` deve conferir versionamento, integração, políticas e workflow.
- `bash tools/test_publish_mock.sh` verifica criação/atualização da release simulada e SHA-256.
- Sintaxe Java/XML/YAML/ZIP deve ser validada, sem alegar Android compilado.

## Testes pendentes no aparelho

- Compilar com GitHub Actions `:app:testDebugUnitTest :app:assembleDebug`.
- Baixar arquivo grande, abrir e compartilhar pelos atalhos sem crash.
- Tentar repetir o mesmo link enquanto ativo/pausado, e depois de concluído.
- Testar dois links assinados diferentes para o mesmo caminho, que não devem ser agrupados sem confirmação de conteúdo.
- Verificar logs em Configurações > Diagnóstico e alertas para falhas do navegador sem exposição de endereços com tokens.
