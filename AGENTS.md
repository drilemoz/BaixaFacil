# Regras do projeto
- Não afirmar que intercepta o `DownloadManager` do Android: apps comuns não têm essa interface universal.
- Toda alteração gera próxima `0.1.0-alpha.N` e incrementa `versionCode` em 1.
- Sincronizar Gradle, README, CHANGELOG, docs/VERSIONING, docs/VALIDATION, notas e workflow.
- Usar MediaStore para pasta Download pública; preservar validação TLS; nunca exigir permissões amplas sem necessidade.
- Registrar diferença entre testes de lógica executados, build de GitHub Actions e testes no aparelho.
- Retomada HTTP: validar ETag forte ou Last-Modified e resposta 206/Content-Range; não anexar bytes sem verificação; manter parcial até sucesso.
- Não prometer retomada automática após reinício do processo; retomada manual no histórico.
- Configurar abertura de links é atalho do Android, não interceptação universal.
- Navegador integrado: só oferecer downloads HTTP(S) iniciados nessa WebView; não afirmar suporte a blob:/data:/sandbox: ou acesso às sessões de outros apps.
- Cookies recebidos do navegador nunca devem ir para histórico, logs, diagnósticos ou outro host/origem em redirecionamentos; HTTPS não pode redirecionar para HTTP.
- Ao evoluir WebView, manter `file://`/`content://`, mixed content e cookies de terceiros bloqueados, salvo justificativa de segurança e testes.
- Fila alpha.5: apenas itens pendentes podem ser priorizados; pausa preserva fragmento e cancelamento deve apagar o fragmento com verificação. Desde alpha.15 status/ordem são persistidos, mas a recuperação exige ação manual do usuário; não afirmar reinício automático após processo ou aparelho reiniciar.
- A aba Arquivos gerencia somente concluídos registrados no histórico do app, não faz varredura universal de Download.

- Navegador alpha.6: manter botão de saída sempre visível; Voltar do sistema retorna à tela principal, setas do navegador navegam no histórico. Android 13+ deve usar callback moderno com registro e remoção corretos. Encerrar BrowserActivity não deve cancelar TransferService.

- Se o GitHub Actions indicar erro de compilação Java, corrigir a falha semântica efetiva; `JavacTask.parse` confirma apenas sintaxe, não declarações conflitantes.

- Alpha.8: workflow sem `upload-artifact`; publicar somente asset `.apk` na Release da versão, com `contents: write`, usando `GITHUB_TOKEN` automático.
- A assinatura de Release exige os quatro Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Nunca registrar valores, fazer commit do arquivo de importação ou incluir keystore no ZIP de código.
- Sem nenhum Secret, permitir fallback Debug; com Secrets parciais, interromper com erro. Arquivo `.p12` deve ficar no diretório temporário do runner e ser apagado em `always()`.

- Alpha.9: validar APK embutido com `aapt dump badging`, assinatura com `apksigner` e bytes publicados na Release comparando SHA-256 com `gh release download`. Não considerar sucesso apenas porque `gh release upload` retornou zero.
- Manter `tools/ci_preflight.py` sincronizado com versionCode/versionName e o script de publicação por Release; não incluir ZIP em Artifacts.

- Alpha.10: manter tema claro/escuro/sistema, teclado Ir e normalização HTTPS para domínios sem esquema; entrada inválida não deve virar download ou liberar esquema fora de HTTP(S).
- O navegador pode pesquisar termos livres, mas o download direto sempre exige URL HTTP(S); indicadores de velocidade só podem derivar de bytes e tempo realmente observados.

- Alpha.11: `beginDownload()` deve manter URL capturada pela lambda HTTP como variável final; logs de compilação Java têm prioridade sobre validação meramente sintática.

- Alpha.13: navegador e downloads compartilham MainActivity e barra inferior; alternar não deve destruir WebView nem reiniciar TransferService.
- Configurações do navegador em SettingsActivity; preferências de página inicial, buscador e JS devem preservar validações HTTP(S) e bloqueios de arquivo/mixed content/cookies de terceiros.
- Limpeza de dados de navegação não deve remover arquivos ou histórico do motor de downloads; histórico WebView contém URLs somente, jamais cookies.

- Alpha.15: a fila persiste somente status/ordem e URL, sem cookies, agentes ou referer da sessão; recuperação requer toque do usuário.
- Android 14+ UIDT: não agendar automaticamente em segundo plano; requer `RUN_USER_INITIATED_JOBS`, rede e notificação via JobService. `onStopJob` cancela sem reagendar automaticamente.
- `DownloadEngine` compartilhado por `TransferService` (Android 10–13/fallback) e `TransferJobService` (Android 14+). Qualquer mudança de HTTP, parcial, timeout ou TLS deve ser refletida/testada nos dois fluxos.

- Alpha.16: a prévia usa HEAD sem corpo/cookies e não garante metadados ou suporte do servidor; deve informar tamanho desconhecido quando ausente. Não alterar motor HTTP para confiar em prévia.
- Filtragem por tipo baseia-se na extensão e não garante o MIME real; ETA é estimativa sobre amostras de velocidade e somente com tamanho informado.

- Alpha.17: `TransferService.onCreate` monta a notificação inicial com `Notification.Builder` e encaminha para `startForeground`. A fábrica `notification(...)` em `DownloadEngine` é privada; não invocá-la no `TransferService`, pois gerou erro de compilação no workflow Gerar APK 15.

- Alpha.18: configurações locais podem bloquear novas transferências por rede/bateria. Verificar ao enfileirar e durante os dois motores (Foreground Service + UIDT), pausar sem descartar parcial e não reiniciar automaticamente. Somente Wi-Fi prevalece sobre permitir dados móveis; não liberar redes VPN sem transporte verificável. Não confundir bloqueio de downloads com bloqueio de navegação WebView.

- Alpha.19: modo computador usa viewport amplo e User-Agent transformado a partir do original da WebView; alternar restaura exatamente o original, nunca usar versão fixa de navegador.
- Localizar na página deve usar `WebView.findAllAsync` e `findNext`, sem injetar JavaScript; fechar a busca limpa os realces. Zoom por pinça e tamanho de texto não devem desbloquear acesso a arquivos locais, cookies de terceiros ou conteúdo misto.

- Alpha.20: favoritos são salvos em `browser_ui` como JSON com URL/título (até 60); nunca gravar cookies ou cabeçalhos. Rejeitar esquemas fora de HTTP(S), credenciais e URLs muito longas. A limpeza do histórico não apaga favoritos; o gerenciador de favoritos é separado de downloads e requer ação para excluir um site.

- Alpha.21: listas de download usam itens compactos e detalhe expansível. Manter atalhos rápidos como ações independentes do toque no card, preservar o menu ⋮ completo, guardar escolha Compacta/Detalhada e evitar regressões de acessibilidade.
- Alpha.21: `DownloadEngine.downloadWithRetries` mantém dois controles de restrição usando nomes distintos (`restriction` no loop e `restrictionAfterFailure` no catch); Javac completo detecta conflitos semânticos que o parser não detecta.

- Alpha.23: detectar candidatos de vídeo apenas por URLs HTTP/HTTPS diretas. `blob:`, DRM, HLS/DASH e playlists segmentadas não devem ser oferecidos como vídeos baixáveis completos. Não prometer download universal do Facebook.
- `WebViewClient.shouldInterceptRequest` deve apenas observar URLs diretas e sempre retornar `null`, sem ler o corpo, alterar cookies ou interceptar resposta. Os links observados permanecem apenas em memória da página.
