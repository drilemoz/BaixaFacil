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

- Alpha.45: usar **fontes de vídeo** e não número de vídeos quando URLs HLS não são agrupáveis. Não associar thumb de CDN à página sem prova; prévia do player DOM deve ser marcada como **prévia da página**.
- Alpha.45: HLS experimental somente VOD `#EXT-X-ENDLIST`, segmentos MPEG-TS sem proteção, sem `EXT-X-KEY`, DRM, byte-range, mapa fMP4 ou audio separado. Conferir sincronismo TS, limite de segmentos/tamanho, transporte HTTPS sem downgrade e cookies somente mesma origem. O destino é `.ts`, nunca `.mp4`; download interrompido reinicia do começo em retomada. Nunca publicar MediaStore parcial como concluído.

## Versão 0.1.0-alpha.46: motor GeckoView
- O navegador ativo usa `GeckoBrowserView`, `GeckoEngine` e `BrowserPane`, não `android.webkit.WebView`. Regras antigas de interceptação WebView não são aplicáveis até que sejam implementadas usando APIs do Gecko ou extensão compatível.
- Ao trabalhar com GeckoView, preservar a regra de runtime único, não copiar cookies de sistemas Android WebView e validar a extensão `assets/baixafacil_gecko`.
- A próxima versão é `0.1.0-alpha.47` com versionCode `47`.

## Versão 0.1.0-alpha.47: requisito de build GeckoView 158
- A falha comprovada no workflow Gerar APK 18 é o AAR metadata: GeckoView 158 requer compileSdk 37, AndroidX Core 1.19 requer API 37 e AGP 9.1+.
- Padrão atual: compileSdk 37, AGP 9.1.1, Gradle 9.3.1, Build Tools 36.0.0, JDK 17. As recomendações das versões anteriores são históricas.
- Preservar minSdk 29, targetSdk 34 e dependência GeckoView 158. Em toda versão sincronizar `tools/test_publish_mock.sh` (incluindo números de asset, versão e payload fictício), `ci_preflight.py`, GitHub Actions, Gradle, Sobre, UA e documentos.
- Próxima versão: 0.1.0-alpha.48 com versionCode 48.

## Versão 0.1.0-alpha.48: SDK 37.0 com identificador exato
- O workflow Gerar APK 19 falhou na instalação do SDK antes do Gradle: `platforms;android-37` não existe. Instalar `platforms;android-37.0` e verificar `platforms/android-37.0/android.jar`.
- O build deve usar `compileSdk { version = release(37) { minorApiLevel = 0 } }`, prevenindo busca pelo caminho inexistente `android-37`.
- Testes estáticos não provam compilação real. Manter GeckoView 158 e versões de dependências; só corrigir Java se o novo log demonstrar o erro.
- Próxima versão sequencial: 0.1.0-alpha.49 com versionCode 49.


## Versão 0.1.0-alpha.49: separar comandos do SDK no GitHub Actions
- O workflow Gerar APK 20 falhou antes do Gradle: o YAML `run:` dobrado concatenou `sdkmanager --install ...` com `test -s ...`, gerando `Warning: Failed to find package 'test'`.
- Usar exclusivamente `run: |` com uma instrução por linha, `set -euo pipefail` e teste explícito do `android.jar`. Não usar escalar YAML dobrado para duas instruções de shell.
- `python3 tools/test_sdk_workflow.py` valida separação, sintaxe e comportamento da etapa com um `sdkmanager` simulado, inclusive quando o JAR não existe. Incluir o teste no preflight; executar preflight antes da instalação no CI.
- Manter GeckoView 158, AGP 9.1.1, Gradle 9.3.1, compileSdk 37.0, minSdk 29, targetSdk 34, Build Tools 36.0.0; sem alterações no motor HTTP ou navegador.
- Próxima versão sequencial: 0.1.0-alpha.50 com versionCode 50.

## Versão 0.1.0-alpha.50: compilar fachada GeckoView e manter histórico
- O workflow Gerar APK 21 instalou SDK 37.0 e executou javac. Único erro: `BrowserPane.java:440` invocava `web.canGoForward()`, inexistente em `GeckoBrowserView`.
- A fachada declara `boolean canGoForward(){return canForward;}` e o estado recebe `onCanGoForward`; preservar a semântica de histórico.
- `tools/test_gecko_browser_contract.py` valida referências de métodos `web`/`loginPopup` usados pelo `BrowserPane` e o contrato dos callbacks. Executar em `ci_preflight.py`, mas não o confundir com compilação Android real.
- Manter demais bibliotecas e configurações, sem copiar cookies Gecko para downloads. Próxima versão: `0.1.0-alpha.51`, `versionCode 51`.

## Versão 0.1.0-alpha.51: distribuir quatro APKs GeckoView por ABI
- APK universal carrega bibliotecas de várias CPUs e fica grande; usar `splits.abi` com `enable true`, `reset()`, quatro ABIs e `universalApk false`.
- `tools/collect_abi_apks.py` deve ler output-metadata.json, exigir 4 elementos de ABI, mesma versão e bibliotecas nativas exclusivas de cada ABI; não presumir capitalização dos APKs do Gradle.
- GitHub Actions deve verificar assinatura e badging de **cada** APK; Release contém **quatro** assets `.apk`, e `publish_apk.sh` deve confirmar os 4 SHA-256 após download.
- Testes localmente com APKs falsos validam lógica, não tamanho real ou execução no Android; novas falhas devem ser analisadas nos logs CI.
- Não incluir universal APK nem subir ZIP para Artifacts; preservar minSdk 29, targetSdk 34, GeckoView 158 e proteção de Secrets.
- Próxima versão sequencial: `0.1.0-alpha.52` (versionCode 52).

## Versão 0.1.0-alpha.52: arquitetura nativa GeckoView real
- No workflow Gerar APK 23, Debug, testes e Release compilaram; a coleta rejeitou `x86` sem .so (`set()`). GeckoView 158 não oferece ABI x86 32 bits.
- `splits.abi`: permitir somente armeabi-v7a, arm64-v8a e x86_64. Proibir universal e APK x86 sem motor; exigir `lib/<ABI>/libxul.so` no coletor.
- GitHub Actions publica três APKs e verifica `apksigner`, `aapt`, versões e SHA-256 após download. Testar x86 ausente, libxul.so faltante e extras na preflight.
- Preservar assinatura e demais requisitos de segurança; próxima versão 0.1.0-alpha.53/versionCode 53.

## Versão 0.1.0-alpha.53: segurança de recuperação GeckoView
- Nunca recrear indefinidamente a Activity após `onCrash`/`onKill`; a sessão fechada não pode ser reativada. Até duas tentativas automáticas por 120 segundos, depois escolha explícita do usuário. Adiar recuperação se a aba está oculta.
- Persistir somente contagem e tempo monotônico para o limite, sem URLs, tokens ou cookies. Testar a regra Java real via JDK e JUnit da CI.
- O resumo da Release deve informar tamanho **real** de cada APK, sem converter tamanho do código-fonte em estimativa de APK.
- Preservar GeckoView 158, ABI ARM32/ARM64/x86_64, SDK 37.0, download engine. Próxima versão `0.1.0-alpha.54`/versionCode 54.

## Versão 0.1.0-alpha.54: arquivos, tela cheia e permissões GeckoView
- `FilePrompt` usa exclusivamente seletor Android ACTION_OPEN_DOCUMENT com URIs `content://`; não copiar arquivos para caminhos privados, não registrar URIs e não conceder armazenamento amplo. Seletor ativo deve resolver ao cancelar/encerrar. Pastas e captura direta permanecem indisponíveis.
- Delegado de permissões deve exigir site HTTPS e aceite explícito para geolocalização, câmera, microfone e autoplay sonoro. Não permitir captura de tela/áudio do aparelho, rede local, DRM ou notificações de sites sem implementação e testes dedicados.
- Tela cheia é controlada por onFullScreen/exitFullScreen; restaurar barras ao usar Voltar, Downloads, destruição da Activity e falha Gecko; não cancelar downloads.
- Preservar minSdk29, targetSdk34, compileSdk37.0, GeckoView 158 e APKs ARM32, ARM64, x86_64 com versionCode 54.
- Não confundir pré-validação com compilação Android. Próxima versão sequencial: 0.1.0-alpha.55 / versionCode 55.

## Versão 0.1.0-alpha.55: ícone do Internet XP
- ícone do Internet XP nas densidades padrão e versão redonda de launcher.
- Preservar applicationId, nome Baixa Fácil e assinatura de APK ao atualizar; versão subsequente 0.1.0-alpha.56 / versionCode 56.
- Launcher pode usar máscara/cor de papel de parede; verificar em dispositivos reais.

## Versão 0.1.0-alpha.56: ícone do Internet XP
- ícone adaptativo, redondo e monocromático tematizável do Internet XP.
- Preservar applicationId, nome Baixa Fácil e assinatura de APK ao atualizar; versão subsequente 0.1.0-alpha.57 / versionCode 57.
- Launcher pode usar máscara/cor de papel de parede; verificar em dispositivos reais.

## Versão 0.1.0-alpha.57: ETP nativa GeckoView
- `GeckoProtectionProfile` é lógica Java pura e teste regressivo. Os quatro perfis controlam `setEnhancedTrackingProtectionLevel` no GeckoRuntime 158. Leve DEFAULT, Forte STRICT, Extremo STRICT com proteção social e limpeza de parâmetros, Desligado NONE.
- ETP requer comportamento de cookies compatível: quando ligado, preferências sem compatibilidade usam `ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS` (cookies de terceiros isolados por origem, não completamente recusados). Se usuário permitir terceiros por compatibilidade (`ACCEPT_ALL`), ETP é explicitamente suspenso. Jamais mascarar essa decisão.
- Exceções por site continuam só para o CSS; não anunciar como exceção da rede GeckoView. Listas EasyList/EasyPrivacy são locais e não interceptam requisições. Contagem deve vir exclusivamente de `onContentBlocked`, sem logging de URLs/tokens.
- Preservar Safe Browsing, GeckoView 158, download engine, minSdk 29, targetSdk 34, compileSdk 37.0, três ABI e ícone Internet XP; próxima versão 0.1.0-alpha.58 / versionCode 58.

## Versão 0.1.0-alpha.58: correção ABI após workflow 27
- O workflow 27 da alpha.57 compilou Debug e Release, mas falhou **depois** com `Versão incorreta no metadata do APK armeabi-v7a` porque a chamada `collect_abi_apks.py` passava `--code 56`, divergente do Gradle `versionCode 57`.
- Alpha.58/versionCode 58: sincronizar `--code 58` com Gradle e `aapt`. `ci_preflight.py` e `test_abi_pipeline.py` devem verificar o argumento da linha executável real no workflow e falhar se houver divergência, não só conferir a versão do APK.
- Preservar bibliotecas nativas do GeckoView, três ABI e políticas de assinatura. Próxima versão `0.1.0-alpha.59`/versionCode 59.

## Versão 0.1.0-alpha.59: barra, importação e credenciais
- Editor de URL é acionado tocando no domínio, mantendo ícone e ⋮ apenas. URLs HTTP/HTTPS validadas antes de navegar; clipboard lido sob ação do usuário.
- `BrowserTopSites` armazena visitas/títulos sem inventar histórico e segue limpeza; não guardar URLs de login, token ou parâmetro OAuth.
- `SecureLoginStore` cifra credenciais apenas para origens HTTPS usando Android Keystore e AES-GCM, sem backup. Nenhum login do GeckoView pode ser salvo sem confirmação. Preferências antigas e cookies não podem ser invalidados na migração.
- QR usa câmera local com permissão explícita; voz depende de serviço Android; ambos só aceitam navegação após edição ou confirmação do usuário.
- Scans automáticos permanecem leves, imagens apenas na busca manual, sem DRM, cookies ou nova transferência não solicitada.
- Manter GeckoView 158, ABI ARM32/ARM64/x86_64, versionamento sequencial, assinatura e scripts de CI. Próxima versão `0.1.0-alpha.60`/versionCode 60.
