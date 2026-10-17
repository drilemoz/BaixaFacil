# Changelog

## 0.1.0-alpha.29 (versionCode 29) - 2026-10-17

- **Correção do workflow Gerar APK 4:** `:app:checkDebugAarMetadata` encontrou 10 bibliotecas Media3 1.11.1 exigindo `compileSdk >= 36` com o app ainda em API 35.
- `compileSdk` atualizado para 36; Android Gradle Plugin atualizado de 8.9.2 para **8.10.1**, compatível com API 36 e Gradle 8.11.1 (JDK 17).
- GitHub Actions agora instala `platforms;android-36`, preservando Build Tools 35.0.0 para validar assinatura e APK.
- Pré-validação agora verifica API 36, AGP 8.10.1 e correspondência entre a configuração Gradle e o SDK instalado pelo workflow.
- Mantidos `minSdk 29`, `targetSdk 34`, AndroidX WebKit 1.17.1, Media3 1.11.1, miniaturas, navegador, motor HTTP e todas as demais funções.
- Build Android completo precisa ser reexecutado no GitHub Actions; a etapa de metadata não chegou a executar a compilação Java na tentativa anterior.

## 0.1.0-alpha.29 (versionCode 29) - 2026-10-17

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
