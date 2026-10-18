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

- Alpha.24: autenticação Google OAuth em WebView não é suportada; não falsificar User-Agent, cookies ou redirecionamentos para contornar provedores de login. Oferecer navegador externo sem importar seus cookies ou prometer continuidade da sessão na WebView.
- Cookies de terceiros desativados por padrão; somente optar por habilitar depois de aviso de privacidade. Bloqueios de conteúdo misto/acesso local e restrições de downloads permanecem obrigatórios.

- Alpha.25: só criar WebView popup quando `WebChromeClient.onCreateWindow` receber gesto do usuário e não houver outra janela; liberar WebView ao fechar, sem pontes JavaScript e sem permitir esquemas locais ou conteúdo misto.
- Google OAuth continua proibido em WebView; usar navegador externo. Cookies externos jamais são importados. Cookies de terceiros opcionais exigem escolha explícita do usuário.
- Não registrar no histórico URLs de login nem parâmetros OAuth. Diagnósticos exibem somente origem (sem path/query), motor WebView, flags e códigos numéricos de erro; nunca cookies, tokens ou credenciais.

- Alpha.26: - Integrado AndroidX WebKit 1.17.1. Desativa downloads de favicons quando a WebView disponibiliza a API; mantém fallback e as proteções existentes. - Trata encerramento do renderer para recriar a tela e evitar falha total do app. Não altera política OAuth do Google, cookies nem o motor HTTP.

- Alpha.27: - Adicionado reprodutor de mídia **Media3 1.11.1** para vídeos locais concluídos e links públicos MP4/WebM/HLS/DASH. - Botão **Reproduzir** na descoberta de vídeos e ação **Reproduzir vídeo** no menu de arquivos concluídos. - Downloads de arquivo direto continuam pelo motor HTTP existente. HLS/DASH são reproduzidos online; sem download segmentado nesta etapa, sem DRM e sem importar cookies ou tokens do navegador.

- Alpha.28: - Central de downloads ganhou **miniaturas de vídeos e imagens concluídos**, geradas a partir do MediaStore sem internet. - Para vídeos locais, lê duração e resolução reais com MediaMetadataRetriever; menu **Detalhes da mídia** exibe os dados quando disponíveis. - Geração ocorre fora da interface, em fila de uma thread com cache LRU de até 4 MB; formatos sem prévia mantêm o ícone anterior. - Preservados Media3 e WebKit, evitando consumo extra em arquivos ainda baixando.

- Alpha.29: Media3 1.11.1 exige Android compileSdk >=36. Usar `compileSdk 36` com Android Gradle Plugin 8.10.1 e Gradle 8.11.1/JDK 17; workflow deve instalar `platforms;android-36`. Preservar minSdk29/targetSdk34, Build Tools 35.0.0 e checagem automática do preflight; nunca baixar ou omitir Media3 para mascarar erro AAR metadata.

- Alpha.30: o bloqueador jamais intercepta requisições da página principal, POST, cookies ou downloads do motor HTTP. Atualizações de listas ocorrem só a pedido do usuário por HTTPS, com limite e cache privado. Apenas regras simples de domínio são suportadas, nunca alegar compatibilidade completa com EasyList/uBlock.
- Sites de login/CAPTCHA e vídeo sensíveis precisam de exceções. Modo Forte como padrão, Extremo opt-in, permissão por site para desativar. Contadores são locais por sessão; não registrar URLs, tokens ou históricos.

- Alpha.31: diálogos de ferramentas, favoritos, histórico, organização, diagnóstico e preferências usam `AppMenus` com tema dinâmico, ícones e rolagem. Confirmar exclusões com uma ação explícita. Ao abrir o menu, ocultar o IME de pesquisa, evitando teclado atrás de popups.
- Alpha.31: configurações de vídeo (`VideoPrefs`) são independentes, locais e não guardam URLs; a tela de vídeos organiza por tipo. Somente arquivos HTTP(S) diretos com extensão reconhecida recebem ação de download, playlists HLS/DASH apenas reprodução, fontes não verificadas sem download. Nunca mostrar query de URL assinada na lista ou transferir cookies para o Media3.

- Alpha.32: agrupar mídias apenas por identidade conservadora de caminho e tipo, com tratamento de parâmetros de identidade (`id`, `video_id`, `media_id` etc.). Nunca agrupar apenas por domínio e nunca expor query/tokens no menu. Qualidade deduzida pelo caminho é estimativa, não metadado verificado.
- Alpha.32: se há várias variantes, exigir escolha explícita da fonte no menu; manter download só de vídeos diretos reconhecidos, HLS/DASH apenas reprodução e formatos não verificados sem botão de download.

- Alpha.34: fontes de rede sem correspondência comprovada não recebem título ou thumb do Reels visível. Associar somente por URL direta exata ou identidade conservadora de arquivo, nunca apenas pelo host ou por tempo de requisição.
- Alpha.34: varreduras após rolagem têm debounce e só usam DOM da própria página, sem polling contínuo, bridge JS, download extra, acesso a cookies ou violação de políticas de origem do canvas.
- Alpha.34: frame/miniatura de vídeo somente em memória e de tamanho limitado; não salvar imagens de páginas ou URLs assinadas no histórico/diagnóstico. Manter bloqueio de DRM e HLS/DASH como download completo.

- Alpha.35: perfil de compatibilidade para páginas pesadas deve reconhecer somente hosts oficiais ChatGPT, sem User-Agent falso ou remoção de cookies/JS/TLS. Evitar trabalhos opcionais de varredura de vídeo, filtragem por requisições e CSS cosmético na WebView apenas enquanto o perfil estiver ativo.
- Alpha.35: observar renderizador não responsivo sem terminá-lo automaticamente; preservar histórico, sessão e o acesso a navegador externo. Perfil opt-out persistido e teste contra falsos domínios.

- Alpha.36: sugestões são exclusivamente locais, só exibem URL HTTP(S) validada de favoritos/histórico e nunca páginas de login ou URL com parâmetros OAuth. Não buscar sugestões remotas por tecla nem registrar conteúdo digitado.
- Alpha.36: a tela Cookies e sessões deve distinguir claramente `CookieManager.removeAllCookies` (global) de `WebStorage.deleteOrigin` (dados locais por origem, sem remover todos os cookies); nunca enumerar ou registrar valor de cookie.
- Alpha.36: a opção de não manter logins apaga cookies e armazenamento web na próxima inicialização completa do processo e antes do primeiro carregamento de página; não apagar ao abrir Configurações e não afetar arquivos ou favoritos.

- Alpha.37: só suspender timers com a WebView fora da vista e sempre retomá-los ao retornar, sem interromper serviços de download. O perfil ChatGPT permanece ativado por padrão; não limpar dados/cache por conta própria.
- Alpha.37: varreduras de vídeo são opcionais e canceladas ao sair da aba, entrar em modo leve ou detectar pressão de memória. Não consultar memória em loop ou na renderização; diagnóstico de PSS é apenas do processo do app (não mede o renderer WebView separado) e deve ficar fora da thread de interface.

- Alpha.38: `WebStorage.getOrigins` é callback de `Map` bruto no Android. Nunca iterar `origins.entrySet()` diretamente como `Map.Entry<String,WebStorage.Origin>`. Usar adaptador `WebStorageOrigins.checked` com verificação de tipos de cada entrada e apenas origens HTTP(S), com testes JVM de regressão. Apagar WebStorage por origem não remove cookies da origem.

- Alpha.40: URLs repetidas são verificadas por igualdade exata, inclusive query; não deduplicar por nome, domínio, path sem assinatura nem tamanho aparente. Downloads ativos/pausados devem ser acessados pela lista e um arquivo concluído só deve ser baixado novamente por escolha explícita.
- Alpha.40: `catch` múltiplo não pode conter `RuntimeException` e subclasses. Atalhos Abrir/Enviar validam URIs content:// e capturam falhas de Intent/ClipData/chooser, mantendo leituras de cookies e URLs assinadas fora do diagnóstico.

- Alpha.41: Custom Tabs requer navegador externo instalado, permissão de visibilidade da ação do serviço `android.support.customtabs.action.CustomTabsService`, `CustomTabsClient.getPackageName` e pacote explícito para não retornar à MainActivity. Sem sincronização de cookies, downloads ou sessões entre WebView e navegador externo; fallback somente ao seletor de navegador do Android.
- Alpha.41: limpeza em lote exige confirmação, trabalha fora da UI e remove apenas `Concluído`, `Cancelado` e `Falhou` sem URI. Nunca excluir fila, pausados, interrompidos, itens em andamento ou fragmentos recuperáveis. Exclusão física apenas para MediaStore `content://media/...`, mantendo o registro se a exclusão não for bem-sucedida.

- Alpha.42: seleção múltipla aceita somente `Concluído` com URI `content://media/...`; nunca escolher filas ativas, pausados, interrompidos ou fragmentos.
- Alpha.42: a limpeza da seleção deve confirmar, executar fora da UI, revalidar status e URI atuais, e não remover o registro quando a exclusão física falha. `Remover da lista` mantém arquivos. `Enviar` usa somente URIs públicas com permissão temporária de leitura, limitado a 25.

- Alpha.43: filtros do histórico por tamanho e host são apenas de apresentação. Total conhecido prevalece; bytes recebidos só servem de tamanho final quando o status é Concluído. Não inferir tamanho total de download parcial, nem consultar rede para filtrar.
- Alpha.43: extrair somente host HTTP(S) da URL local e nunca incluir path/query/token no texto de filtro, no diagnóstico ou em requisições. Filtros se combinam com status, pesquisa e tipo; trocar filtros encerra seleção múltipla e "Selecionar visíveis" deve operar exclusivamente na lista filtrada.

- Alpha.44: jamais bloquear **Home** ou **Voltar** por `downloadsVisible`; ambos devem permitir sair de Downloads para a WebView sem recarregar/destruir página. O botão Home na WebView abre a página inicial configurada. A aba Downloads permanece alternável, seleção múltipla é encerrada primeiro pelo Voltar do Android e navegação WebView mantém histórico/timers/cookies.
- As decisões da barra inferior e Voltar do sistema ficam em `MainNavigationPolicy` (Java puro) com testes JVM; manter os rótulos de abas acessíveis e o atalho de retorno do cabeçalho para evitar regressão.
