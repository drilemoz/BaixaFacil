# Baixa Fácil 0.1.0-alpha.59 (versionCode 59) - 2026-10-20

- Nova barra de navegação simplificada e editor de endereços vertical, frequentes, clipboard opt-in, voz e QR.
- Favicon local por domínio HTTPS, progresso GeckoView com percentual e menu ⋮ compacto.
- Botão flutuante de mídia com imagens e vídeos, diálogos de permissão tematizados, correção de opções agrupadas em formulários.
- Cofre de senhas no Android Keystore/AES-GCM com integração GeckoView e importação manual de HTML/CSV.
- Documentação, preflight, scripts de ABI, versionCode e release alinhados; execução Android real pendente.

# Baixa Fácil 0.1.0-alpha.58 (versionCode 58) - 2026-10-19

- Corrigido o parâmetro `--code` do coletor de APKs no GitHub Actions (57 esperado na alpha.57, 56 passado indevidamente). Agora a alpha.58 passa 58.
- Testes de pré-validação e pipeline ABI passaram a inspecionar a chamada executável efetiva no YAML, prevenindo divergência entre Gradle, coletor, metadados e pacote.
- Preservados GeckoView 158, ETP, privacidade, motor HTTP, três ABI, assinatura estável/fallback Debug e validações `aapt`, `apksigner` e SHA-256.
- Workflow 27: :app:assembleRelease bem-sucedido; erro exclusivamente na coleta de APK por ABI. Novo build e publicação pendentes.

# Baixa Fácil 0.1.0-alpha.57 (versionCode 57) - 2026-10-19

- Perfis GeckoView ETP: Desligado, Leve (padrão), Forte (rigoroso) e Extremo (rigoroso com proteção social e limpeza de parâmetros).
- GeckoRuntime atualizado ao regressar das Configurações; recarregamento quando o perfil, rastreadores ou cookies mudam. Pop-ups seguem perfil ativo.
- Contadores de recursos bloqueados recebidos do próprio GeckoView em memória, sem registrar as URLs dos eventos.
- Cookies de terceiros isolados por origem quando ETP está ativo e opção de compatibilidade desativada. A compatibilidade de login permite cookies e suspende ETP explicitamente.
- Exceções por domínio continuam SOMENTE cosméticas, sem prometer suspender ETP por site. EasyList/EasyPrivacy locais não bloqueiam rede; Safe Browsing não é desativado.
- Mantidos ícones Internet XP, GeckoView 158 e APKs por ABI ARM32/ARM64/x86_64. Sem build Android executado neste ambiente.

# Baixa Fácil 0.1.0-alpha.56 (versionCode 56) - 2026-10-19

- ícone adaptativo, redondo e monocromático tematizável do Internet XP.
- Versionamento, CI e documentação sincronizados; código do navegador não alterado.

# Baixa Fácil 0.1.0-alpha.55 (versionCode 55) - 2026-10-19

- ícone do Internet XP nas densidades padrão e versão redonda de launcher.
- Versionamento, CI e documentação sincronizados; código do navegador não alterado.

# Baixa Fácil 0.1.0-alpha.54 (versionCode 54) - 2026-10-19

- `GeckoPromptHandler` passou a tratar `FilePrompt` com seletor SAF Android via `GeckoFilePicker`, incluindo seleção múltipla e cancelamento explícito.
- `GeckoSitePermissions`: Android runtime grants para localização, câmera e microfone, prompt de consentimento separado por origem HTTPS; fontes de captura de tela/áudio interno recusadas.
- `GeckoSession.ContentDelegate.onFullScreen`: vídeo HTML5 ocupa a tela, Voltar e transição para Downloads saem da tela cheia corretamente.
- Introduzida regra pura `GeckoPickerPolicy` com teste Java local e verificação de integração pré-CI; Manifest declara recursos de hardware como opcionais.
- Preservados motor HTTP de downloads, GeckoView 158, três ABIs e restrições de privacidade. Compilação real e testes de aparelho pendentes.

# Baixa Fácil 0.1.0-alpha.53 (versionCode 53) - 2026-10-19

- Proteção contra ciclo de recriação do navegador após `GeckoSession.ContentDelegate.onCrash/onKill`: até duas tentativas automáticas a cada 120 segundos; terceira falha exige ação explícita.
- Ao retornar ao navegador de uma aba oculta, recuperação pendente é tratada sem reativar sessão fechada.
- Adicionados `GeckoCrashPolicy` (sem Android, testável), teste JUnit e executor local de contrato Java; sem persistir URL/cookies no contador.
- Resumo de publicação exibe tamanho real dos três APKs e SHA-256, além de rejeitar saída diferente de três ABIs.
- Versionamento/documentação/workflow/testes sincronizados; GeckoView 158, SDK 37, bibliotecas e motor HTTP sem alterações.
- A compilação Android e os testes físicos da alpha.53 ainda precisam ocorrer no GitHub Actions/aparelho.

# Baixa Fácil 0.1.0-alpha.52 (versionCode 52) - 2026-10-19

- **Build 23:** `:app:testDebugUnitTest`, `:app:assembleDebug` e `:app:assembleRelease` concluíram com `BUILD SUCCESSFUL`. Falha depois do build, em `collect_abi_apks.py`: `Bibliotecas nativas de x86 inválidas: set()`.
- GeckoView 158 não inclui `lib/x86/libxul.so` (versão x86 32 bits descontinuada). Removido somente o split `x86` para evitar APK instalável sem o motor do navegador. Permanecem `armeabi-v7a`, `arm64-v8a` e `x86_64`, sem APK universal.
- Coleta, publicação, verificações do CI e testes simulados ajustados para três APKs, exigindo `libxul.so` da ABI correta, assinatura, `aapt`, versionCode, versões e SHA-256 de cada arquivo.
- Dependências, minSdk 29, targetSdk 34, SDK 37.0, GeckoView 158, funcionamento do navegador e motor de downloads preservados. Compilação real da alpha.52 e testes no aparelho pendentes.

# Baixa Fácil 0.1.0-alpha.51 (versionCode 51) - 2026-10-19

- Configurados quatro APKs independentes por ABI: armeabi-v7a, arm64-v8a, x86 e x86_64. APK universal desativado.
- `tools/collect_abi_apks.py` copia saídas declaradas no metadata do Gradle, recusando APK ausente/extra/versão errada ou `.so` de outra CPU.
- Workflow verifica cada APK com apksigner e aapt e publica quatro assets na Release; `tools/publish_apk.sh` verifica os quatro SHA-256 reais após download.
- `tools/test_abi_pipeline.py` e testes mock de publicação cobrem falhas de ABI, metadados, SHA-256, asset extra/ausente.
- Alteradas somente distribuição, validação e versão. Mantidos GeckoView 158, SDK API 37.0, minSdk 29, targetSdk 34 e motor HTTP.
- Compilação Gradle/Android real e tamanho dos pacotes continuam pendentes de CI.

# Baixa Fácil 0.1.0-alpha.50 (versionCode 50) - 2026-10-19

- Corrigido único erro `cannot find symbol` de `BrowserPane.java:440` no workflow **Gerar APK 21**: faltava `GeckoBrowserView.canGoForward()`.
- Nova validação de contrato da fachada GeckoView verifica referências de métodos e atualizações de histórico antes do build.
- Mantidos GeckoView 158, API 37.0, fluxos de download, sessão e configurações.
- Versionamento 50 sincronizado em Gradle, CI, Sobre, User-Agent, testes de publicação e documentação.
- Build Android completo e testes no aparelho ainda pendentes para esta versão.

# Baixa Fácil 0.1.0-alpha.49 (versionCode 49) - 2026-10-19

- Corrigido o workflow **Gerar APK 20**: `run:` dobrado concatenava `sdkmanager` com `test -s`, causando `Failed to find package 'test'` e saída 1 antes da compilação.
- Instalação API 37.0 e validação do `android.jar` agora são comandos distintos no bloco literal YAML `run: |` com verificação de erro.
- Adicionado teste automatizado da etapa com SDK Manager falso para verificar argumentos e falha quando falta a plataforma; o preflight roda antes da instalação no CI.
- Sincronizados versionCode/versionName 49, Sobre, User-Agent, teste de publicação, documentação e metadados do workflow.
- GeckoView, downloads e target/minSdk inalterados. Build Android e validação em dispositivo continuam pendentes.

# Baixa Fácil 0.1.0-alpha.48 (versionCode 48) - 2026-10-19

- Corrigido o erro comprovado do workflow **Gerar APK 19**: `Failed to find package 'platforms;android-37'`. O pacote correto é `platforms;android-37.0`.
- Adicionado check de `android.jar` após instalar a plataforma, para falhar com diagnóstico direto se o SDK estiver ausente.
- Declarada explicitamente a API 37.0 no Gradle com `compileSdk { version = release(37) { minorApiLevel = 0 } }`; preservados GeckoView 158, AGP 9.1.1 e Gradle 9.3.1.
- Versão 48 sincronizada em código, Sobre, User-Agent, workflow, testes de pré-validação e publicação e documentação.
- Ainda sem confirmação de compilação Android completa ou testes no dispositivo.

# Baixa Fácil 0.1.0-alpha.47 (versionCode 47) - 2026-10-19

- Correção do workflow Gerar APK 18: `checkDebugAarMetadata` acusou 5 incompatibilidades entre GeckoView 158, AndroidX Core 1.19, compileSdk 36 e AGP 8.10.1.
- Atualizados compileSdk 37, AGP 9.1.1, Gradle 9.3.1, Build Tools 36.0.0 e instalação do SDK 37 no CI. JDK 17, minSdk 29 e targetSdk 34 preservados.
- Atualizados verificações estáticas, preflight e mock de publicação para versão 47, mantendo o GeckoView 158 e o motor de downloads.
- Pendente execução real do Gradle/SDK Android e testes em dispositivo.

# Baixa Fácil 0.1.0-alpha.46 (versionCode 46)

## Integração GeckoView

- Navegação integrada migrada de `android.webkit.WebView` para `org.mozilla.geckoview.GeckoView` e `GeckoSession`.
- Runtime Firefox Gecko único por processo. Pesquisa na página, navegação, modo desktop, histórico, tratamento de pop-ups e links de download adaptados para delegates do GeckoView.
- Uma WebExtension empacotada e sem requisições externas adicionais faz a detecção básica de fontes MP4/WebM/HLS/DASH e oculta seletores CSS locais.
- Preferências de cookies e limpeza de dados migradas para `StorageController`.
- Regras de domínios EasyList/EasyPrivacy **não são interceptadas no GeckoView** nesta versão; o bloqueio de rastreamento nativo do Gecko está habilitado e ocultação CSS parcial permanece.
- Links que exigem autenticação baseada em cookies da página **podem falhar no gerenciador de downloads**, porque o GeckoView não fornece os cookies ao motor HTTP próprio. URLs diretas continuam suportadas.
- Listagem e exclusão seletiva de armazenamento por origem, miniaturas capturadas da página e detecção de mídia em tráfego de rede não estão disponíveis nesta migração.
- O pacote adiciona o GeckoView, aumentando substancialmente o tamanho do APK.

## Verificação pendente

Não foi possível executar `:app:assembleDebug` ou testes no aparelho neste ambiente sem SDK/Gradle e dependência GeckoView em cache. Valide o workflow de CI e teste navegação, login, arquivos, vídeo e consumo de RAM na instalação real.

# Changelog

## 0.1.0-alpha.45 (versionCode 45) - 2026-10-18

- Corrigida a indicação de **vídeos** para **fontes de vídeo**, sem agrupar CDN/qualidades sem evidência de identidade. Botão **Baixar** disponível nas fontes diretas e playlists HLS, com escolha explícita da qualidade.
- Download experimental de HLS VOD MPEG-TS não criptografado para `.ts`, com parser defensivo, seleção de variante, validação básica de MPEG-TS, limites e integração ao motor/fila/MediaStore. Rejeita DRM, lives, fMP4, byte-ranges, áudio externo e formatos incompatíveis.
- Nova prévia **do player da página** quando visível, sem vinculação falsa a URLs observadas pela rede. Atualizados testes JVM, informações de segurança, CI, versão e notas. Build Android ainda não executado localmente.

## 0.1.0-alpha.44 (versionCode 44) - 2026-10-17

- Corrigido defeito na navegação da tela Downloads: Home e Voltar da barra inferior não faziam nada porque `if(downloadsVisible)return;` bloqueava todas as ações exceto alternar a última aba.
- Introduzido `MainNavigationPolicy` com roteamento explícito dos controles inferiores e do Voltar do Android, garantindo retorno ao navegador sem recarregar a WebView, preservando seleção múltipla e comportamento da Home em navegação.
- Rodapé passou a mostrar rótulos de ações e ícones visíveis/selecionados; página Downloads ganhou botão **Voltar ao navegador** no topo.
- Teclado de pesquisa é recolhido ao voltar à navegação. Mantidos motor HTTP, cookies, fluxo de autenticação, histórico, preferências e downloads.
- Incrementados versionName 0.1.0-alpha.44 e versionCode 44 em Gradle, workflow, app, documentação, publisher mock e pré-validação. Build Android e teste real de interface ainda pendentes.

## 0.1.0-alpha.43 (versionCode 43) - 2026-10-17

- Adicionados **filtros combináveis por tamanho e site de origem** em Meus downloads > Filtrar e ordenar, além dos filtros anteriores por estado, tipo e pesquisa.
- Tamanho: todos, desconhecido, menor que 10 MiB, de 10 até menos de 100 MiB e 100 MiB ou mais. Downloads parciais com total desconhecido não recebem tamanho inventado. Quando concluído e sem total registrado, bytes efetivamente recebidos podem representar seu tamanho.
- Filtro de origem extrai apenas o host HTTP(S) do histórico local, com contagem por site, lista dos 30 hosts mais frequentes e opção sem origem identificada. Nenhum URL completo, assinatura ou token é apresentado nos filtros.
- Faixa discreta de filtros ativos com botão **Limpar**, opção no menu para restaurar status/tipo/tamanho/site/pesquisa, e estado vazio específico para filtros sem resultados.
- Seleção múltipla da alpha.42 respeita o resultado filtrado em "Selecionar visíveis"; ao trocar filtros a seleção é encerrada para não remover itens escondidos acidentalmente. Nenhum download ou arquivo é alterado pelo filtro.
- Implementado `DownloadListFilters` (Java puro), com 10 testes JVM de limites de tamanho, URLs privadas, domínios e agregação; sincronizados versionName/versionCode 43, User-Agent, Sobre, workflow, preflight, mock e documentos.
- Compilação Android completa e testes no aparelho continuam pendentes.

## 0.1.0-alpha.42 (versionCode 42) - 2026-10-17

- Implementada **seleção múltipla** de arquivos concluídos, pelo botão Selecionar ou por toque longo nos cartões. Interface com seleção visível, contador e barra horizontal de ações.
- Incluído **Selecionar visíveis**, respeitando pesquisa/filtros, e **Enviar**, com compartilhamento Android de até 25 arquivos e permissão temporária de leitura para o destino.
- **Remover da lista** e **Apagar arquivos** exigem confirmação, executam fora da UI e revalidam o status e a URI antes de qualquer exclusão. Só downloads concluídos do MediaStore podem ser selecionados.
- Arquivos físicos ficam intocados ao remover registros, falhas de exclusão preservam histórico e os itens pausados/na fila/em andamento não entram na operação.
- Novos testes da política `DownloadSelectionPolicy`, pré-validação do workflow e versionamento 42 sincronizados em Gradle, Sobre, User-Agent e documentação. APK completo pendente no GitHub Actions.

## 0.1.0-alpha.41 (versionCode 41) - 2026-10-17

- Adicionado **Custom Tabs** do AndroidX Browser 1.10.0 com detecção do navegador compatível e pacote explícito, para abrir a página atual a pedido e encaminhar login Google ao navegador externo quando habilitado. Nenhum cookie ou download é transferido para a WebView.
- Nova preferência **Login em Custom Tabs** ativada por padrão, com fallback para o seletor de navegador externo. O navegador WebView permanece o motor padrão e Custom Tabs não equivale a burlar OAuth nem concede acesso aos downloads privados do navegador externo.
- Novo botão **Limpar downloads** no cabeçalho de Meus downloads, com duas ações confirmadas: remover registros finalizados sem tocar nos arquivos, ou excluir arquivos concluídos do MediaStore e seus registros quando autorizado.
- Limpeza fora da thread de interface, com política para preservar fila, downloads ativos, pausados, interrompidos e falhas com arquivo parcial. Exclusões sem permissão falham sem remover o respectivo registro.
- Política de limpeza testável em Java puro e checagens preventivas de CI, com versão e documentação sincronizadas.
- Ainda pendentes build Gradle/Android e testes de Custom Tabs, MediaStore e UI em aparelho.

## 0.1.0-alpha.40 (versionCode 40) - 2026-10-17

- Ações rápidas de arquivos concluídos agora têm ícones **e** rótulos Abrir/Enviar, com espaço entre os botões e alvos de 44 dp.
- Corrigidos `catch` múltiplos incompatíveis no código da alpha.39 (`RuntimeException` junto a subclasses), garantindo que a proteção contra falhas possa compilar.
- Abertura/compartilhamento aceita apenas `content://` com autoridade, protege a construção do Intent, da ClipData e do chooser, e trata indisponibilidade de apps sem encerrar o Baixa Fácil.
- Nova confirmação de URL repetida no campo de download e na WebView: detecta igualdade exata, não duplica uma transferência em fila/pausada e exige confirmação para baixar novamente um arquivo concluído. Não inspeciona nem remove parâmetros de tokens.
- Registros locais opcionais ganham códigos HTTP da navegação principal, códigos de falhas WebView e eventos do renderizador, sem endereços completos ou cookies.
- Testes JVM da `DownloadDuplicatePolicy`, preflight e validações locais; versão, publicação e documentação sincronizados para alpha.40. Build Android completo ainda não validado localmente.

## 0.1.0-alpha.39 (versionCode 39) - 2026-10-17

- Modernizado o diálogo de confirmação de download com visual nativo do app: cartões com ícones, origem, arquivo, destino e aviso claro quando a conexão usa HTTP sem criptografia.
- Corrigido o fechamento do aplicativo ao tocar nos dois atalhos rápidos da lista de downloads (**Abrir** e **Compartilhar**). Agora o app valida a URI, trata ausência de aplicativo compatível e captura falhas sem derrubar a interface.
- Adicionada a seção **Configurações > Diagnóstico e alertas**, com opção para capturar localmente erros, alertas e crashes inesperados, visualizar/copiar os registros e limpar o histórico local.
- Implementado `AppEventLog` com captura opcional local e `UncaughtExceptionHandler` para registrar crashes inesperados de forma privada, sem salvar cookies, tokens nem URLs completas com parâmetros.
- Player e fluxos principais de download agora registram alertas/erros relevantes quando a captura estiver ativada. Versionamento, User-Agent, workflow, mock de publicação, Sobre e documentação foram sincronizados para a alpha.39.
- Build Android completo e testes no aparelho continuam pendentes de execução.

## 0.1.0-alpha.38 (versionCode 38) - 2026-10-17

- Corrigido erro real do workflow **Gerar APK 10**, em `SettingsActivity.java:163`: `incompatible types: Object cannot be converted to Entry<String,Origin>` durante `:app:compileDebugJavaWithJavac`.
- A API `WebStorage.getOrigins` expõe um `Map` bruto no callback. Agora o app converte com `WebStorageOrigins.checked`, validando entradas, tipos e URLs antes de listar origens HTTP(S) e mantendo a exclusão apenas dos dados de WebStorage da origem.
- Acrescentados cinco testes JVM de regressão, inclusive compilação com mapa bruto, sem assumir o tipo genérico da API do Android. O preflight verifica a correção para evitar retorno ao `for` incompatível.
- Versionamento 38 sincronizado em Gradle, About, User-Agent, workflow de APK, mock de publicação, docs e notas.
- Todos os demais recursos da alpha.37 preservados. Build Android completo precisa ser reexecutado no GitHub Actions.

## 0.1.0-alpha.37 (versionCode 37) - 2026-10-17

- Ciclo de vida da WebView: ao alternar para Downloads ou sair do app, cancelam-se as varreduras agendadas e são suspensos os timers JavaScript; ao voltar ao navegador, timers e processamento são restaurados, mantendo a sessão.
- Redução de trabalho em feeds: varreduras de vídeo sob demanda por rolagem com debounce existente e limite de frequência de 3,5 s; qualquer resultado antigo é descartado quando a página muda. Varreduras não rodam quando navegador está oculto, o renderizador está sem resposta, ChatGPT leve ativo ou a RAM está sob pressão.
- Resposta a sinais de pouca memória: interrompe as tarefas opcionais e descarta somente metadados/miniaturas temporários de vídeo, sem limpar cookies, histórico, downloads ou interromper transferência.
- Novo diagnóstico de desempenho e memória acessível no menu Login e diagnóstico, com leitura sob demanda fora da UI da RAM disponível, Java heap, PSS aproximado do processo do app, estado do renderizador e alternativa de navegador externo. Não alega medir todo o processo WebView.
- Políticas puras testadas para agendamento, suspensão, memória baixa e recuperação; sincronizados versionCode 37, workflow, preflight, documentação e release.
- Build Android e medição de desempenho no aparelho ainda pendentes.

## 0.1.0-alpha.36 (versionCode 36) - 2026-10-17

- Nova seção **Configurações > Cookies e sessões** com estado global de cookies (sem ler valores), limpeza global de cookies e armazenamento WebView e seleção por origem dos dados locais listados por `WebStorage.getOrigins`.
- Preferência **Manter logins entre aberturas** ativada por padrão. Se desativada, na próxima inicialização fria do processo o app apaga cookies e WebStorage antes de navegar, preservando favoritos, histórico e downloads. Nenhuma limpeza ocorre ao alternar para Configurações.
- Documentado que a API pública não enumera cookies por site e `deleteOrigin` remove apenas dados de armazenamento web da origem, não todos os cookies/cache do site.
- Barra de endereço com sugestões suspensas de até cinco sites do histórico e favoritos, ordenadas por prefixo de domínio, correspondência de título e recência, sem rede durante a digitação.
- A seleção de sugestão navega apenas para URL HTTP(S) validada; páginas sensíveis e URLs com parâmetros de autenticação não são sugeridas. Sugestões são ocultadas ao navegar ou perder foco.
- Sincronizados versionCode, versionName, User-Agent, Sobre, workflows, documentação e testes preventivos. Build Android completo continua pendente do GitHub Actions.

## 0.1.0-alpha.35 (versionCode 35) - 2026-10-17

- Perfil automático de compatibilidade **ChatGPT**, reconhecendo somente domínios oficiais `chatgpt.com`, `www.chatgpt.com` e `chat.openai.com` sem aceitar domínios parecidos.
- Durante navegação nessas origens, desativados trabalhos opcionais de detecção automática de mídia, análise de bloqueio de anúncios por subrecurso e injeção de CSS de ocultação, que acrescentavam processamento à página e podiam prejudicar sites dinâmicos.
- Adicionada preferência persistente **Compatibilidade com ChatGPT** (ligada por padrão); pode ser desativada a qualquer momento, sem apagar sessões, cookies ou histórico.
- Mensagens de progresso da WebView foram reduzidas a passos de 10%, preservando indicador de carregamento e erros de navegação.
- Monitoramento de WebView `onRenderProcessUnresponsive` e recuperação visual em `onRenderProcessResponsive`, sem matar renderizador ou perder a página automaticamente. Diagnóstico mostra o estado de resposta do renderizador.
- Menu de ferramentas disponibiliza atalho contextual **Abrir ChatGPT no navegador externo** como alternativa para páginas que ainda travam.
- JUnit para classificador de sites confiáveis e verificações preventivas no preflight. Sem alteração de permissões, Media3, cookies, User-Agent de navegação, TLS ou motor de downloads.

## 0.1.0-alpha.34 (versionCode 34) - 2026-10-17

- Detecção de vídeo em Reels/feed melhorada: varredura após rolagem com debounce sem polling contínuo, relacionando metadados apenas ao `<video>` que realmente expõe aquela URL.
- A lista ordena primeiro vídeos associados ao player visível e em reprodução, mantendo fontes observadas na rede identificadas como não verificadas visualmente. Não atribui o vídeo ativo a links arbitrários.
- Captura conservadora de título do próprio player ou de cabeçalho/legenda no artigo HTML correspondente; sanitização e limite de texto, sem incluir tokens de URL.
- Miniaturas tentam obter frame reduzido (96×70) localmente por canvas com política de origem do navegador e fazem fallback para captura da área visível da WebView. Sem requisições de imagem adicionais, scraping de cookies ou persistência de thumbs.
- Deduplicação de URLs reassinadas e espelhos `scontent.*.fbcdn.net` quando o caminho contém identidade forte. Preserva separação de vídeos distintos por caminho ou parâmetros ID; evita agrupamento apenas por domínio.
- Novo `VideoPresentation` e testes JVM de ordenação, títulos, espelhos Facebook e preservação de identidade. Sincronizados versionCode, metadados, CI e documentação.
- Compilação Android completa e testes em aparelho ainda pendentes; Facebook pode bloquear reprodução externa por sessão, assinaturas ou DRM.

## 0.1.0-alpha.33 (versionCode 33) - 2026-10-17

- Navegador remove o efeito de “site dentro de uma caixa”: a WebView agora ocupa a área principal sem moldura arredondada, mantendo o cabeçalho e os controles externos do app.
- O atalho de vídeos virou um **botão flutuante** sobre a página e só aparece quando há resultados detectados. O rodapé fica limpo quando não existe vídeo compatível.
- A lista **Vídeos encontrados** ganhou cards com miniaturas locais sempre que houver um elemento `<video>` visível na tela. A miniatura é recortada da própria WebView, sem buscar cookies nem URLs privadas adicionais.
- Itens de vídeo exibem uma identificação visual melhor e continuam respeitando os limites de segurança: download apenas para arquivos diretos; HLS/DASH apenas reprodução online; formatos não verificados sem download.
- Atualizados versionamento, User-Agent, workflow, mock de publicação, tela Sobre e documentação para a alpha.33.
- Validação Android completa e teste em aparelho continuam pendentes; foram feitas verificações locais de consistência dos arquivos e sincronização de versão.

## 0.1.0-alpha.32 (versionCode 32) - 2026-10-17

- Lista de vídeos agora agrupa URLs repetidas do **mesmo caminho de mídia** em um único resultado, inclusive quando tokens de acesso temporários da URL mudam; a fonte mais recente é mantida apenas na sessão, nunca em histórico/diagnóstico.
- Fontes de qualidade 240p–2160p identificadas explicitamente no caminho são organizadas no mesmo item quando compartilham a mesma identidade do vídeo; o menu permite escolher a fonte antes de reproduzir ou baixar.
- Corrigido contador **Vídeos (N)** para refletir resultados agrupados, não requisições individuais. Resultados de vídeos diferentes continuam separados, mesmo no mesmo CDN; parâmetros de identidade (id, video_id etc.) também impedem fusões indevidas.
- Seleção preserva limites: download apenas de arquivos diretos, HLS/DASH somente reprodução online, formatos não verificados sem download, sem DRM ou cookies externos.
- Menus continuam usando AppMenus e as preferências de vídeos criadas na alpha.31, sem novas permissões nem operações em segundo plano.
- Incluídos 7 novos testes JVM de agrupamento/privacidade, totalizando 11 testes no VideoMenuModelTest. Preflight, documentação, release e versionCode atualizados.

## 0.1.0-alpha.31 (versionCode 31) - 2026-10-17

- Padronizados os menus de navegação, downloads, favoritos, diagnóstico e vídeo com componente Java nativo `AppMenus`: cartões arredondados, ícones vetoriais, descrições curtas, seções, ações e rolagem limitada, compatível com os temas claro/AMOLED e fechamento de teclado.
- Menu ⋮ do navegador reorganizado em 5 grupos: Vídeos e mídia, Favoritos, Página, Proteção e Login/diagnóstico. Submenus substituem a lista longa antiga.
- Nova página dedicada **Configurações > Vídeos e mídia**, incluindo opções persistentes de varredura automática, exibição de arquivos diretos e transmissões HLS/DASH.
- A lista Vídeos agora categoriza arquivos com download possível, playlists apenas para reprodução e fontes de formato não confirmado. Omite scripts/recursos desconhecidos, destaca formato e qualidade do caminho quando disponíveis e não exibe parâmetros de URLs assinadas.
- Menus de favoritos no navegador e nas configurações, formulário Adicionar/Editar, histórico, aparência, filtro e organização de downloads adotam o mesmo padrão. Confirmações de exclusão ainda exigem ação explícita.
- Segurança preservada: arquivos diretos usam downloads existentes; HLS/DASH somente reprodução Media3, sem DRM, sem transferência de cookies do navegador, sem alterar os filtros de anúncio.

## 0.1.0-alpha.30 (versionCode 30) - 2026-10-17

- Bloqueador de anúncios integrado à WebView, com interceptação limitada a requisições HTTP(S) GET de subrecursos; nunca bloqueia documentos principais ou POST.
- Motor local com níveis Desligado/Leve/Forte (padrão)/Extremo e lista local de domínios publicitários/trackers.
- Atualização manual e assíncrona de EasyList e EasyPrivacy por HTTPS, interpretando exclusivamente regras simples de domínio; cache privado, limites de tamanho e preservação do cache anterior em falhas.
- Exceções persistentes por site, contador de requisições bloqueadas nesta sessão, modo de ocultação cosmética CSS local e controles no menu ⋮ e Configurações.
- Tratamento conservador de logins/CAPTCHA, reprodução de vídeo, URLs sem HTTP(S) e downloads do motor próprio. Sem VPN/root e sem coleta de URL de navegação.
- Testes de filtragem pura e pré-validação acrescentados; compilação Android final permanece pendente do GitHub Actions.

## 0.1.0-alpha.29 (versionCode 29) - 2026-10-17

- **Correção do workflow Gerar APK 4:** `:app:checkDebugAarMetadata` encontrou 10 bibliotecas Media3 1.11.1 exigindo `compileSdk >= 36` com o app ainda em API 35.
- `compileSdk` atualizado para 36; Android Gradle Plugin atualizado de 8.9.2 para **8.10.1**, compatível com API 36 e Gradle 8.11.1 (JDK 17).
- GitHub Actions agora instala `platforms;android-36`, preservando Build Tools 35.0.0 para validar assinatura e APK.
- Pré-validação agora verifica API 36, AGP 8.10.1 e correspondência entre a configuração Gradle e o SDK instalado pelo workflow.
- Mantidos `minSdk 29`, `targetSdk 34`, AndroidX WebKit 1.17.1, Media3 1.11.1, miniaturas, navegador, motor HTTP e todas as demais funções.
- Build Android completo precisa ser reexecutado no GitHub Actions; a etapa de metadata não chegou a executar a compilação Java na tentativa anterior.

## 0.1.0-alpha.28 (versionCode 28) - 2026-10-17

- Central de downloads ganhou **miniaturas de vídeos e imagens concluídos**, geradas a partir do MediaStore sem internet.
- Para vídeos locais, lê duração e resolução reais com MediaMetadataRetriever; menu **Detalhes da mídia** exibe os dados quando disponíveis.
- Geração ocorre fora da interface, em fila de uma thread com cache LRU de até 4 MB; formatos sem prévia mantêm o ícone anterior.
- Preservados Media3 e WebKit, evitando consumo extra em arquivos ainda baixando.

## 0.1.0-alpha.27 (versionCode 27) - 2026-10-17

- Adicionado reprodutor de mídia **Media3 1.11.1** para vídeos locais concluídos e links públicos MP4/WebM/HLS/DASH.
- Botão **Reproduzir** na descoberta de vídeos e ação **Reproduzir vídeo** no menu de arquivos concluídos.
- Downloads de arquivo direto continuam pelo motor HTTP existente. HLS/DASH são reproduzidos online; sem download segmentado nesta etapa, sem DRM e sem importar cookies ou tokens do navegador.

## 0.1.0-alpha.26 (versionCode 26) - 2026-10-17

- Integrado AndroidX WebKit 1.17.1. Desativa downloads de favicons quando a WebView disponibiliza a API; mantém fallback e as proteções existentes.
- Trata encerramento do renderer para recriar a tela e evitar falha total do app. Não altera política OAuth do Google, cookies nem o motor HTTP.

## 0.1.0-alpha.25 (versionCode 25) - 2026-10-17

- Adicionado suporte **conservador a janelas secundárias** da WebView, somente quando disparadas por gesto do usuário, com janela de login separada, botão para abrir no navegador externo e liberação de recursos no fechamento.
- A janela de login usa o mesmo armazenamento de sessão WebView, respeita a preferência explícita de cookies de terceiros e preserva bloqueios de arquivo local, conteúdo misto, JavaScript sem permissão e esquemas inseguros; Google OAuth segue sendo encaminhado para navegador externo.
- Novo **Diagnóstico de login** no menu ⋮: informa versão do componente WebView, estado de JavaScript/cookies/modo computador e falhas da navegação principal, sem registrar cookies, senhas nem parâmetros sensíveis da URL.
- Comando **Atualizar WebView** a partir do diagnóstico para procurar atualizações do componente de navegador do Android.
- Tratamento mais claro dos erros HTTP e de rede em páginas principais e janelas de login, inclusive erros que antes eram substituídos por mensagem genérica de página pronta.
- Histórico deixa de armazenar páginas de login e URLs com códigos/tokens OAuth; subframes `about:blank`/`about:srcdoc` são permitidos para compatibilidade sem liberar file:/content:.
- Mantido o uso de navegador externo para Google OAuth, sem importar cookies/sessões de outros aplicativos; logins/captchas do Facebook continuam dependentes da validação do site.
- Build Android completo e testes com serviços reais pendentes do GitHub Actions/aparelho.

## 0.1.0-alpha.24 (versionCode 24) - 2026-10-17

- Analisadas falhas de login relatadas no Google (tela branca) e no Facebook (desafios de imagem). Confirmado bloqueio de OAuth do Google em WebViews; o app informa e oferece uso de navegador externo, sem burlar restrições.
- Adicionados **Abrir em navegador externo** e **Ajuda com login** no menu de ferramentas `⋮` do navegador.
- Fluxos OAuth oficiais `accounts.google.com/o/oauth2/` são reconhecidos por host HTTPS exato, sem encaminhar credenciais, e recebem explicação e opção de navegador externo.
- Em Configurações, nova preferência opt-in **Compatibilidade de login** para cookies de terceiros, inicialmente desativada e com aviso claro do impacto na privacidade.
- Detecção de páginas de verificação do Facebook para orientar quando o fluxo exigir um navegador completo. Detector de vídeos não é executado em telas sensíveis de login/verificação; cookies externos não são importados à WebView.
- Mantidos os bloqueios contra conteúdo misto, acesso a arquivos locais, downloads inválidos e o motor HTTP/UIDT.
- Acrescentados testes para classificação de domínios de autenticação e verificações preventivas do workflow.
- Build Android completo e testes de login reais no aparelho ainda pendentes.

## 0.1.0-alpha.23 (versionCode 23) - 2026-10-17

- Navegador agora identifica fontes de vídeo direto em elementos HTML5 `video/source`, links e URLs MP4/WebM reconhecíveis nas requisições da WebView.
- Novo controle **Vídeos (N)** no rodapé do navegador e opção **Procurar vídeos nesta página** no menu ⋮, com lista de candidatos, **Baixar** e **Copiar link**.
- Busca automática após carregar a página e busca manual para conteúdo dinâmico; detecção não intercepta dados ou cabeçalhos nem utiliza bridge JavaScript.
- URLs `blob:`, HLS/DASH, segmentos e conteúdo protegido não são oferecidos como arquivos diretos; mensagem explica que nem todo vídeo do Facebook é suportado.
- Nova validação `VideoLinks.java`, com testes adicionais e bloqueio de URLs fora de HTTP(S), credenciais e tamanhos excessivos.
- Atualizados versionamento, pré-validação, scripts de Release, User-Agent e documentação. Compilação Android real permanece pendente.

## 0.1.0-alpha.22 (versionCode 22) - 2026-10-16

- Corrigido o erro de compilação visto nos logs do workflow **Gerar APK 17** em `DownloadEngine.java:117`, causado por declaração duplicada da variável `restriction`; a verificação preventiva do preflight foi atualizada.
- Rebranding completo do aplicativo para **Baixa Fácil**: nome visível do app, cabeçalhos, notificações, documentação, nome do projeto, User-Agent e identificador técnico atualizados para `br.com.baixafacil`.
- Estrutura do código alinhada ao novo pacote `br.com.baixafacil`, incluindo fontes principais, testes, scripts e validações do workflow/release.
- Ícone do aplicativo redesenhado com identidade ligada à internet e download, e adoção do ícone integrado `webdown` nas áreas principais da interface.
- Menu de ações dos downloads redesenhado: em vez da lista simples padrão, agora abre um painel visual com ícones, descrições curtas, destaque para ações destrutivas e botão Fechar.
- Comportamentos existentes preservados: navegador integrado, favoritos, retomada HTTP conservadora, UIDT Android 14+, controles de rede/bateria e armazenamento em MediaStore.
- Build Android completo e conferência visual final continuam pendentes de execução no GitHub Actions e no aparelho.

## 0.1.0-alpha.20 (versionCode 20) - 2026-10-16

- Adicionados favoritos persistentes separados do histórico/cookies, com limite de 60 sites e armazenamento local JSON protegido por validação de URLs HTTP/HTTPS.
- Menu ⋮ do navegador oferece abrir favoritos e salvar/remover a página atual; toque longo em links HTTP/HTTPS também permite salvar.
- Configurações > Navegador > Favoritos permite adicionar, abrir, renomear, editar endereço e remover sites, com aviso ao duplicar ou usar esquema inválido.
- Limpar histórico, cookies ou cache preserva explicitamente os favoritos e os downloads concluídos; sem permissões nem dependências de UI adicionais.
- Acrescentados testes JVM para normalização, limites e rejeição de URLs inseguras, e checagens preventivas no preflight.
- Preservados WebView, ferramentas de página, motor HTTP, UIDT Android 14+, fila persistente e restrições de rede/bateria.
- Compilação Android completa e validação visual continuam pendentes do workflow GitHub Actions e dos testes no aparelho.

## 0.1.0-alpha.19 (versionCode 19) - 2026-10-16

- Navegador: novo menu de ferramentas no topo com **Localizar na página**, navegação entre ocorrências, **Aumentar/Diminuir zoom** e alternância rápida do **Modo computador**.
- Busca na página utiliza a API nativa `WebView.findAllAsync`, sem JavaScript injetado; contagem de resultados e encerramento limpam destaques.
- Configurações > Navegador: preferência persistida de Modo computador e tamanho do texto em 75%, 100%, 125% ou 150%, aplicadas à WebView existente sem destruir a página ao alternar entre Navegador e Downloads.
- Modo computador solicita viewport amplo e um User-Agent derivado da versão real do WebView (sem versão fixa); sites podem ignorar a solicitação. Volta ao User-Agent original ao desativar.
- Zoom por gesto de pinça habilitado sem controles flutuantes; não foram adicionadas permissões nem dependências.
- Preservados o motor HTTP, UIDT Android 14+, recuperação manual da fila, controle de rede/bateria, armazenamento MediaStore e as regras de segurança da WebView.
- Compilação Android completa e testes em aparelho ainda pendentes.

## 0.1.0-alpha.18 (versionCode 18) - 2026-10-16

- Adicionada seção **Downloads • rede e bateria** na página Configurações, com Somente Wi-Fi, Permitir dados móveis e proteção opcional quando a bateria atingir 20% sem carregador.
- Restrições verificadas no início e periodicamente durante transferências tanto pelo Foreground Service quanto pelos jobs UIDT Android 14+. Bloqueios pausam mantendo fragmentos e exigem retomada explícita.
- Rede ativa via ConnectivityManager e carga via BatteryManager; regras de segurança conservadoras para VPN/transportes não identificados. Nenhuma permissão nova.
- Incluídos testes JVM para combinações de rede e bateria e verificação preventiva no pré-teste do workflow.
- Sincronizados Gradle, User-Agent, interface Sobre, GitHub Actions, documentação, Release Notes e scripts. Build Android completo e testes físicos permanecem pendentes.

## 0.1.0-alpha.17 (versionCode 17) - 2026-10-16

- Corrigido erro `:app:compileDebugJavaWithJavac` nos logs **Gerar APK 15**: `TransferService.java:95` chamava `notification(...)`, método que foi movido para `DownloadEngine` na alpha.15 e não existe mais em `TransferService`.
- Notificação inicial do Foreground Service agora é criada diretamente com `Notification.Builder`, usando o canal existente, ícone, título, texto, botão de retorno ao aplicativo e flags seguras de `PendingIntent`.
- Incluída checagem preventiva da criação da notificação no pré-teste da CI para evitar retorno da referência ao método removido.
- Mantidos o motor HTTP unificado, a fila recuperável, os jobs UIDT Android 14+, a prévia HTTP HEAD e os filtros/progresso da alpha.16.
- Sincronizados `versionCode 17`, `versionName 0.1.0-alpha.17`, interface Sobre, User-Agent, workflows, scripts e documentação.
- Compilação Android completa e teste físico continuam pendentes do GitHub Actions após esta correção.

## 0.1.0-alpha.16 (versionCode 16) - 2026-10-16

- Adicionada prévia opcional de metadados do arquivo antes de enfileirar links diretos: consulta HTTP `HEAD` em thread dedicada, nome, tipo, tamanho declarado e aviso para HTTP não criptografado. Sem cookies/sessão e sem baixar o conteúdo na consulta.
- Quando o servidor nega `HEAD`, não retorna tamanho ou ocorre erro, a confirmação continua disponível informando ausência de prévia, sem fabricar metadados.
- Filtros por categorias (extensão), ordenação recente/antigo/nome/tamanho, preservando os filtros de status.
- Velocidade suavizada por amostras e previsão aproximada de tempo restante apenas quando tamanho e velocidade são válidos.
- Testes adicionais da classificação e ETA; versionamento, pipeline e documentação sincronizados. Compilação Android final ainda pendente no GitHub Actions.

## 0.1.0-alpha.15 (versionCode 15) - 2026-10-16

- Adicionado menu de toque longo sobre links HTTP(S) na WebView com Abrir, Copiar, Baixar e Compartilhar.
- Persistência da ordem/status de fila; reconhecimento de itens órfãos após processo encerrado e botão Recuperar fila de forma explícita.
- Criado `DownloadEngine` comum ao serviço foreground Android 10–13 e ao `TransferJobService` UIDT Android 14+.
- UIDT: JobScheduler com `setUserInitiated(true)`, rede obrigatória, notificação via JobService, cancelamento e marcação de interrupção; fallback FGS se agendamento falhar.
- Cookies e cabeçalhos de sessão permanecem transitórios, não são salvos na fila persistida; política de retomada HTTP permanece validada.
- Versionamento, UI Sobre, scripts CI, workflow, User-Agent e documentação atualizados.
- APK e testes físicos ainda dependem da compilação no GitHub Actions e validação no aparelho.

## 0.1.0-alpha.14 (versionCode 14) - 2026-10-16

- Refinada a interface do navegador: removido o título superior e a barra de endereço passou para o topo, ao lado do botão de configurações.
- Compactado o módulo de novo download: menos espaço em volta, margens laterais menores e campo de link mais enxuto.
- O botão de colar dentro do campo agora exibe somente o ícone, sem o texto "Colar".
- Removido o botão "Links padrão" da tela principal, já que essa opção continua disponível em Configurações.
- O botão de download foi movido para dentro da mesma linha do campo, ao lado do botão de colar, com rótulo curto "Baixar" e ícone de seta.
- Preservados o navegador integrado, a barra inferior fixa, fila de downloads, retomada e as configurações do navegador.

## 0.1.0-alpha.13 (versionCode 13) - 2026-10-16

- A tela principal foi unificada: navegador e downloads convivem na mesma Activity, com alternância pela barra inferior sem destruir a WebView.
- Barra fixa com Voltar, Avançar, Recarregar, Página inicial e Downloads nas duas visualizações; botões de navegação ficam inativos enquanto Downloads está aberto.
- Área de Downloads mantém campo com Colar, Baixar arquivo, Links padrão, filtros, pesquisa, progresso e ações; removido o botão textual Navegador.
- Nova página completa de Configurações para navegador: página inicial, buscador Google/DuckDuckGo, ativação de JavaScript, histórico recente, limpeza de cache/cookies e histórico, aparência e links padrão do Android.
- Histórico do navegador limitado a 12 URLs HTTP(S) e limpo junto aos dados de navegação, sem apagar registros nem arquivos baixados.
- WebView permanece na memória quando Downloads está aberto, com `onPause()` para reduzir processamento oculto; serviço de downloads continua separado.
- Atualizados `versionCode 13`, User-Agent, workflows, notas e documentação; corrigida inconsistência de versionCode em docs/VERSIONING da alpha.12.
- Compilação Android completa e testes visuais continuam pendentes até o GitHub Actions.

## 0.1.0-alpha.12 (versionCode 12) - 2026-10-16

- Refinado o layout principal conforme pedido do usuário: removido o subtítulo do app, topo mais compacto e campo de link com botão **Colar** embutido.
- Ações **Navegador**, **Links padrão** e **Baixar arquivo** agora ficam na mesma linha; o botão de baixar deixou de usar cor diferente e passou para a extremidade direita.
- Navegador compacto: removido o subtítulo "Baixa Fácil" do cabeçalho, reduzida a altura do topo e mantidos os controles inferiores.
- Barra de endereço do navegador ganhou botão para **copiar o endereço atual** e botão pequeno para abrir a lista do **histórico** recente.
- Histórico simples do navegador salvo localmente (até 12 endereços HTTP/HTTPS), limpo junto com os dados locais do navegador.
- Preservados motor de downloads, fila, retomada, normalização de HTTPS e demais recursos da alpha.11.
- Compilação Android completa e teste visual em aparelho continuam pendentes neste ambiente.

## 0.1.0-alpha.10 (versionCode 10) - 2026-10-16

- Nova interface nativa inspirada nos mockups aprovados: identidade verde-petróleo, cartões arredondados, ações principais destacadas, filtros em chips, pesquisa recolhível e estado vazio centralizado.
- Lista com cartões individuais, indicação de estado, progresso real quando o tamanho é conhecido, bytes reais e ações rápidas de pausa, cancelamento, retomada, abrir e compartilhar.
- Navegador WebView renovado: saída compacta no topo, barra de endereço arredondada, indicador de carregamento, controles inferiores e estado da segurança da conexão.
- Campo de endereço aceita domínios sem esquema (adiciona HTTPS), mantém HTTP explícito com aviso e permite pesquisa web na barra do navegador. Links sandbox:, javascript:, file: e credenciais em URLs não são aceitos como downloads.
- Aparência escolhida entre sistema, claro e escuro AMOLED; não adiciona dependências ou permissões.
- Preservados motor HTTP, armazenamento MediaStore, cookies apenas temporários, fila, política de retomada, workflow de Release com validação SHA-256.
- Validação local de lógica e sintaxe; compilação Android e testes físicos ainda pendentes.

## 0.1.0-alpha.9 (versionCode 9) - 2026-10-16
- Reforçada a verificação do APK no GitHub Actions: testes e build Debug obrigatórios, checagem de assinatura e versão embutida no APK.
- Novo script de publicação que baixa o asset da Release e compara seu SHA-256 ao arquivo compilado.
- Adicionado preflight de CI, prevenção de execuções concorrentes e resumo com link da Release após verificação.
- Preservados os quatro Secrets existentes da alpha.8 e as funções anteriores do aplicativo.


## 0.1.0-alpha.8 (versionCode 8) - 2026-10-16
- GitHub Actions publica APK `.apk` diretamente em GitHub Releases, sem `upload-artifact` nem ZIP de Artifact.
- Assinatura Release estável usando os 4 Secrets do GitHub Manager; sem Secrets, fallback APK Debug assinado pelo runner.
- Adicionadas verificações de integridade e assinatura do APK, 4 tentativas de publicação da Release e limpeza de keystore temporária.
- Preservada correção Java da alpha.7 e as funções existentes do aplicativo.
- Testes locais e documentação atualizados; compilação Android real e instalação ainda pendentes.

## 0.1.0-alpha.7 (versionCode 7) - 2026-10-16
- Corrigida falha `:app:compileDebugJavaWithJavac` do workflow Gerar APK 5: redeclaração de `position` em `MainActivity.DownloadAdapter.getView(int position, ...)`.
- Variável interna renomeada para `queuePosition`, sem alterar a regra de posição da fila.
- README, validação, notas, Gradle, User-Agent e Artifact do GitHub Actions sincronizados.
- Compilação Android completa e teste físico permanecem pendentes; análise sintática isolada não identifica esse tipo de erro semântico.

## 0.1.0-alpha.6 (versionCode 6) - 2026-10-16
- Navegador: adicionado botão persistente **← Voltar aos downloads** acima do campo de URL, com destino explícito à tela principal.
- Tecla/gesto Voltar do sistema fecha o navegador diretamente; histórico das páginas permanece nas setas internas.
- Android 13+: usa callback de Voltar registrado/removido no ciclo de vida; Android 10–12 usa compatibilidade via `onBackPressed`.
- Mensagem de download enfileirado orienta a retornar à tela principal; fechar a WebView não cancela transferências que já foram encaminhadas ao serviço.
- Preservados motor próprio, fila, gerenciamento de arquivos e limitações de interceptação.
- Versionamento/documentação/Artifact sincronizados. Compilação Android e testes de UI em aparelho pendentes.

## 0.1.0-alpha.5 (versionCode 5) - 2026-10-16
- Fila em memória reordenável: prioridade manual dos pendentes sem interromper o item atual.
- Pausar/cancelar pendentes e downloads ativos; cancelamento com descarte confirmado de parciais e proteção de arquivos concluídos.
- Filtros Todos/Fila/Arquivos, pesquisa e ações de renomear, compartilhar, abrir, excluir ou ocultar do histórico.
- Mantido download próprio, navegador experimental, retomada HTTP e separação de cookies por origem.
- `PendingQueue` com dois novos testes locais; documentação, versionCode e Artifact sincronizados.

## 0.1.0-alpha.4 (versionCode 4) - 2026-10-16
- Navegador integrado experimental com WebView.DownloadListener, confirmação de download e botão ChatGPT.
- Transporte de sessão temporária do navegador para o download (cookies/user-agent), sem persistir cookies.
- Redirecionamentos controlados e proteção contra vazamento de cookies e Referer entre origens ou downgrade HTTPS.
- Testes unitários TransferHttpTest e documentações/Artifact atualizados.


## 0.1.0-alpha.3 (2026-10-16)
- Motor de downloads: até três novas tentativas com espera de 2/4/8 segundos para falhas transitórias; HTTP permanente e TLS não entram em repetição automática.
- Retomada segura por `Range`/`If-Range`, validador HTTP confiável e confirmação `206 Content-Range`; respostas `200` reiniciam do zero sem concatenar bytes antigos.
- Fragmentos ficam pendentes no MediaStore para retomar manualmente após interrupção, sem publicar arquivos incompletos.
- Diagnóstico de erro em código + explicação, copiável sem link/token, e controle de retomada no histórico.
- Evita duplicar solicitação em andamento e mantém uma transferência ativa por vez.
- `versionCode` 3, artefato CI e documentação atualizados; dependências e configuração de GitHub Actions preservadas.

## 0.1.0-alpha.2 (2026-10-16)
- GitHub Actions: atualização de `android-actions/setup-android` de v3 para v4 e pacote explícito `platform-tools`, corrigindo a falha `Failed to find package 'tools'` registrada em `Downloads-Gerar-APK-1-logs.zip`.
- Interface: botão `Configurar abertura de links` direciona às configurações de abertura por padrão no Android 12+ e fornece fallback para detalhes do aplicativo.
- Mantidas validação de TLS, gravação pela MediaStore e limitações de interceptação do Android.
- `versionCode` atualizado para 2, sincronizando os nomes dos artefatos e documentações.

## 0.1.0-alpha.1 (2026-10-16)
- Criação do aplicativo Baixa Fácil, download por conexão própria e pasta pública Download/.
- Recebimento de URLs compartilhadas, progresso, histórico, notificações e abertura/compartilhamento de arquivos.
- GitHub Actions com testes e build de APK Debug.
