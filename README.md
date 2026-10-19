## 0.1.0-alpha.49: correção do passo de instalação do SDK no build 20

O workflow **Gerar APK 20** falhou antes do Gradle: o YAML transformou duas linhas do `run:` da etapa de instalação em **um comando só**. O `sdkmanager` recebeu `test` como argumento e informou `Failed to find package 'test'`. Agora a etapa usa `run: |` e executa instalação e validação em linhas separadas, com `set -euo pipefail`. Um teste automatizado usa `sdkmanager` simulado e verifica caminhos de sucesso e de ausência do `android.jar`; o preflight executa antes da instalação real.

Preservados GeckoView 158, SDK 37.0, AGP 9.1.1, Gradle 9.3.1, Build Tools 36.0.0, `minSdk 29`, `targetSdk 34` e funções do Baixa Fácil. **Ainda não há comprovação de compilação Android ou execução em celular.**

## 0.1.0-alpha.48: SDK Android 37.0 no GitHub Actions

O workflow **Gerar APK 19** parou antes do Gradle: `sdkmanager` não encontrou `platforms;android-37`. A plataforma oficial é **`platforms;android-37.0`**. O CI instala o pacote correto e verifica o arquivo `android.jar`. A configuração `compileSdk { version = release(37) { minorApiLevel = 0 } }` usa explicitamente API 37.0, evitando resolver o nome inexistente `android-37`.

Mantidos GeckoView 158, Android Gradle Plugin 9.1.1, Gradle 9.3.1, Build Tools 36.0.0, JDK 17, `minSdk 29` e `targetSdk 34`. Esta alteração resolve o erro de instalação do SDK observado, mas **não comprova** build Java/Android concluído; reexecutar o GitHub Actions.

## 0.1.0-alpha.47: compatibilidade de compilação GeckoView

O workflow **Gerar APK 18** falhou em `:app:checkDebugAarMetadata`: GeckoView 158 requer Android API 37 e AndroidX Core 1.19 exige API 37 com Android Gradle Plugin 9.1 ou superior. Corrigido para **compileSdk 37 + AGP 9.1.1 + Gradle 9.3.1 + Build Tools 36.0.0**, com instalação explícita da API 37 no GitHub Actions. Android `minSdk 29` e `targetSdk 34` permanecem inalterados. O GeckoView continua em 158.0.20261007201254.

Sincronizados os testes de pré-validação, simulação de publicação, validação `aapt`/`apksigner` e número sequencial da versão 47. A causa AAR foi corrigida na configuração; **compilação Android e testes reais ainda precisam passar no GitHub Actions**.

## 0.1.0-alpha.46: migração GeckoView

Navegador integrado GeckoView (Firefox), sessão nativa, detector local de vídeos via WebExtension. Algumas funções dependentes de interceptação de rede do WebView não têm equivalente nesta versão. Consulte RELEASE_NOTES.md.

# Baixa Fácil 0.1.0-alpha.45

## Detecção e download de vídeos (alpha.45)

- A lista mostra **fontes de vídeo**, não afirma que cada URL HLS representa um vídeo diferente. Mantém agrupamento conservador por identidade verificável, sem juntar filmes só porque pertencem ao mesmo CDN.
- Cada arquivo direto MP4/WebM e playlist HLS recebe botão **Baixar** visível. Quando há mais de uma qualidade, a escolha ocorre antes da confirmação.
- Para playlists HLS, o motor agora tenta transferir **VOD MPEG-TS sem criptografia**, em segundo plano, pela fila habitual, salvando `.ts` no MediaStore. Se a URL for master, seleciona a variante com maior banda declarada. Não converte para MP4 nem promete o download de toda fonte HLS.
- O parser rejeita transmissões ao vivo sem `ENDLIST`, DRM/chaves, `EXT-X-MAP`/fMP4, byte-range, discontinuidades, áudio externo e segmentos que não sejam MPEG-TS; verifica assinatura TS e limite de bytes antes de publicar o arquivo. Falhas deixam diagnóstico na fila.
- Se a página expõe um único player visível, a tela pode mostrar **prévia do player na própria página**, mas não a associa a uma URL de CDN não comprovada. Sem frame legível, permanece apenas o ícone neutro.
- A captura depende das restrições da WebView, origem do vídeo e permissões. Não tenta contornar DRM ou coletar credenciais do navegador externo. Retomar HLS começa do zero para evitar juntar segmentos de playlists alteradas.
- **Pendente:** compilação Android completa e testes com playlists HLS reais (incluindo condições de rede, cancelamento, atualização de fontes e espaço em disco).

## Navegação corrigida entre Downloads e Home (alpha.44)

- Ao abrir **Meus downloads**, **Home/Navegador** e **Voltar** na barra inferior agora retornam ao navegador sem recarregar a página. O botão **Downloads** continua alternando as telas.
- O cabeçalho da lista passou a apresentar **Voltar ao navegador**. A barra inferior possui rótulos e marcação de aba atual.
- Quando o navegador está na tela, **Início** mantém a ação tradicional de abrir a página inicial configurada; as setas continuam navegando no histórico.
- Voltar do Android sai primeiro da seleção múltipla antes de retornar ao navegador. Teclado de pesquisa é recolhido ao alternar.
- Testes JVM cobrem o roteamento dos controles; build Android e validação em dispositivo permanecem pendentes.

## Filtros inteligentes da central de downloads (alpha.43)

- Em **Meus downloads > Filtrar e ordenar**, escolha tamanho (**Todos**, **Desconhecido**, **Menos de 10 MB**, **10 a 100 MB** ou **100 MB ou mais**) e **Site de origem**. Eles funcionam ao mesmo tempo que status, tipo e busca textual.
- Uma faixa compacta mostra os filtros extras ativos; o botão **Limpar** restaura filtros/status e pesquisa, sem apagar históricos ou arquivos. Quando não há resultados, aparece uma mensagem indicando filtros ativos.
- O filtro por site usa somente o host do histórico do Baixa Fácil, sem contatos de rede; URLs com parâmetros ou credenciais não são expostas. Lista os 30 hosts com mais registros, com opção para origens não identificadas.
- Tamanhos desconhecidos permanecem desconhecidos durante downloads parciais. Apenas o arquivo concluído pode usar a quantidade final de bytes baixados quando o total não foi informado pelo servidor.
- A seleção múltipla e a ação **Selecionar visíveis** respeitam os filtros atuais. Trocar um filtro encerra seleção prévia por segurança.
- **A compilação Android e os testes de interface no dispositivo continuam pendentes do GitHub Actions.**

## Seleção múltipla de downloads (alpha.42)

- A tela **Meus downloads** recebeu um botão **Selecionar arquivos**. Também é possível manter pressionado um cartão concluído para iniciar a seleção.
- A barra de seleção apresenta **Selecionar visíveis** (respeita os filtros ativos), **Enviar**, **Remover da lista**, **Apagar arquivos** e **Sair**. Itens marcados são destacados e possuem indicador visual.
- Compartilhamento de até **25 arquivos** por vez com o seletor Android `ACTION_SEND_MULTIPLE`, URIs públicas do MediaStore e permissão temporária de leitura; sem copiar arquivos ou expor cookies/URLs.
- **Remover da lista** preserva arquivos físicos. **Apagar arquivos** pede confirmação e apaga apenas arquivos concluídos no MediaStore; falhas mantêm os respectivos registros para tentativa posterior. O trabalho é feito fora da interface.
- Itens em andamento, pausados, interrompidos, na fila ou com fragmentos recuperáveis ficam fora da seleção. A seleção é temporária e não sobrevive ao fechamento da tela.
- Build Android completo e testes no celular ainda necessários.


## Navegação compatível e limpeza de downloads (alpha.41)

- **Custom Tabs**: menu ⋮ do navegador > Ferramentas da página > **Abrir com Custom Tabs**. Utiliza um navegador instalado compatível com cookies e sessões próprios; o Baixa Fácil não importa credenciais ou downloads desse navegador. **Configurações > Login em Custom Tabs** permite desativar a opção automática para autenticação Google (padrão ligado). Se não houver serviço Custom Tabs, usa o seletor de navegador externo.
- **Limpar downloads**: ícone de lixeira ao lado dos controles de pesquisa e organização em **Meus downloads**. Permite **Limpar lista (manter arquivos)** ou **Apagar arquivos e limpar lista** com duas confirmações.
- A limpeza só afeta registros de status **Concluído**, **Cancelado** e **Falhou** sem fragmentos. Fila, ativos, pausados, interrompidos e itens com partes recuperáveis permanecem intactos. A opção de apagar arquivos concluídos atua apenas sobre URIs MediaStore e preserva o registro se a exclusão falhar.
- Todo o processo ocorre localmente; nenhum acesso a cookies do navegador externo e nenhum download universal de vídeos é prometido.

## Ações rápidas e prevenção de duplicatas (alpha.40)

- Os cartões de arquivos concluídos mostram os atalhos **Abrir** e **Enviar** com ícones e rótulos, áreas de toque de 44 dp e espaço entre os botões.
- Corrigida incompatibilidade do compilador Java na versão anterior: o tratamento de `RuntimeException` não usa `catch` múltiplo com suas subclasses.
- Para abrir ou compartilhar arquivos, a URI deve ser um `content://` com autoridade válida, e todas as chamadas que podem lançar exceções são tratadas para não fechar o app indevidamente.
- Antes de adicionar o download pelo navegador ou pelo campo de endereço, o app detecta somente a **URL exatamente igual** já presente na lista. Se estiver ativa/pausada, direciona para o registro; se concluída, pergunta antes de baixar novamente. Não ignora assinaturas/queries e não deduz duplicata pelo nome.
- O diagnóstico local opcional também registra falhas da página principal e travamentos do renderizador WebView sem incluir os endereços sensíveis.
- Requer validação do workflow e teste no aparelho para confirmar compilação e comportamento das ações.

## Novidades da alpha.39

- Diálogo de confirmação de download redesenhado para ficar mais moderno e coerente com o padrão visual do app.
- Correção do fechamento do aplicativo ao tocar nos botões rápidos **Abrir** e **Compartilhar** na lista de downloads concluídos.
- Nova seção **Configurações > Diagnóstico e alertas** para capturar localmente erros, alertas e crashes inesperados, com visualização, cópia e limpeza dos registros.
- Captura global opcional de crashes com armazenamento local privado, sem cookies, tokens nem URLs completas com parâmetros.
- Mantidos os recursos das versões anteriores: navegador integrado, bloqueio de anúncios, detecção de vídeos, histórico, favoritos, configurações de sessão e restrições de rede/bateria.

## Correção de compilação do gerenciamento de sessões (alpha.38)

- Resolvido erro de compilação `:app:compileDebugJavaWithJavac` do workflow **Gerar APK 10** (SettingsActivity.java:163). A API Android `WebStorage.getOrigins` entrega um `Map` sem genéricos e não pode ser iterada diretamente como `Map.Entry<String,WebStorage.Origin>`.
- Adicionado `WebStorageOrigins.checked`: adapta entradas do callback com validação de chave, tipo e origem HTTP(S) antes de apresentar ou excluir dados locais.
- O gerenciador por origem continua apagando apenas WebStorage da origem escolhida; **cookies não são apagados individualmente**.
- Adicionados testes JVM para mapa bruto, tipos inválidos, URLs indevidas e entradas nulas, além de verificação preventiva na CI.
- O build Android completo ainda precisa ser repetido no GitHub Actions com a versão corrigida.

## Estabilidade e memória do navegador (alpha.37)

- WebView suspende temporizadores quando o navegador fica oculto, restaurando-os quando volta à tela. Downloads independentes seguem funcionando.
- Varredura automática de vídeos com frequência mínima de 3,5 s e cancelamento imediato ao ocultar, trocar de página ou entrar no modo ChatGPT leve.
- O aplicativo reage a sinais reais de pressão de memória do Android suspendendo varreduras e descartando miniaturas temporárias, sem apagar cookies ou arquivos.
- Menu ⋮ > Login e diagnóstico > **Desempenho e memória**: RAM disponível, limite de memória baixa do Android, heap Java, PSS do processo do app e indicador de resposta da WebView. Coleta somente quando solicitada, fora da thread de interface.
- O PSS do app não inclui necessariamente renderizador WebView externo, GPU ou outras aplicações. Testes no aparelho ainda necessários.

## Cookies, sessões e sugestões de endereço (alpha.36)

- Acesse **Configurações > Cookies e sessões** para verificar se existem cookies, limpar todos os cookies, limpar WebStorage ou remover apenas os dados locais de uma origem listada pela WebView. A limpeza por origem **não remove todos os cookies do site**.
- Em **Manter logins entre aberturas**, escolha se o app conservará cookies/dados locais no próximo início completo. Padrão: conservar. Se desligar, a próxima abertura fria limpa globalmente cookies e armazenamento web antes de navegar, sem apagar downloads, histórico ou favoritos.
- Ao começar a digitar no endereço, aparecem sugestões locais de favoritos e histórico, com domínio/título e até cinco resultados, sem pesquisa remota por tecla. URLs de autenticação são filtradas.
- Limitações: a lista de origens usa `WebStorage.getOrigins` e pode não incluir todos os sites que guardaram cookies. A WebView não oferece remoção completa de cookies por site na API pública.

## Compatibilidade com páginas pesadas (alpha.35)

- **ChatGPT:** em `chatgpt.com`, `www.chatgpt.com` e `chat.openai.com`, o navegador evita varredura automática de vídeos, bloqueio extra de anúncios em subrecursos e CSS cosmético. O objetivo é reduzir processamento opcional e falsos positivos sem interferir em JavaScript, cookies, TLS ou login.
- A otimização é ativada por padrão e pode ser desativada em **Configurações > Navegador > Compatibilidade com ChatGPT**.
- O feedback de carregamento passou a ser atualizado em passos, reduzindo notificações visuais repetidas em sites SPA.
- Monitoramento de processos WebView sem resposta (Android 10+), preservando a sessão e oferecendo acesso ao diagnóstico/navegador externo. Não mata o processo automaticamente nem garante desempenho em todos os dispositivos.
- O bloqueador e a detecção de vídeos continuam funcionando normalmente em outros domínios.

## Reels, identificação e duplicatas (alpha.34)

- O detector diferencia fontes simplesmente vistas na rede de fontes **associadas diretamente** a um elemento de vídeo na página. Quando possível, prioriza o vídeo visível/em reprodução, inclusive após rolagem nos feeds e Reels, com varredura adiada e sem polling.
- Títulos são extraídos apenas do próprio player ou da legenda/cabeçalho dentro do mesmo artigo HTML, normalizados e limitados; não atribui indiscriminadamente o título da página às mídias.
- Miniaturas tentam utilizar um quadro pequeno do próprio elemento `<video>` com canvas (somente quando autorizado pelo navegador), usando como alternativa a captura da área renderizada na WebView. Nenhuma imagem é obtida por chamadas HTTP extras ou de outras sessões.
- Fontes renovadas com novas assinaturas e espelhos conhecidos do CDN Facebook são associadas à mesma identidade quando o **caminho forte do arquivo** coincide. Caminhos e identidades de vídeo diferentes continuam separados.
- Quando a relação com o vídeo da tela não puder ser comprovada, aparece indicação de **fonte sem associação visual confirmada**. Isso reduz erros de identificação em páginas de feed com várias mídias.
- O player Media3 e o motor HTTP preservam as limitações atuais: alguns links do Facebook podem não reproduzir fora da página ou exigir sessão; não há download de HLS/DASH segmentado nem contorno de DRM.

## Miniaturas e botão flutuante de vídeos (alpha.33)

- O atalho **Vídeos** agora aparece como **botão flutuante** sobre a página e só fica visível quando a detecção encontra mídia compatível.
- A lista de vídeos passou a mostrar **miniaturas** usando a área visível do vídeo na própria página, quando houver um elemento `<video>` renderizado na WebView.
- A moldura arredondada ao redor dos sites foi removida: a página fica mais aberta e aproveita melhor a tela.
- O agrupamento conservador, a escolha de fonte/qualidade e as limitações de download seguro das versões anteriores foram mantidos.

## Lista de vídeos agrupada (alpha.32)

- Requisições diferentes para um mesmo arquivo ou playlist são agrupadas quando o caminho permite identificação segura da mídia.
- Caminhos com `360p`, `720p` ou `1080p` equivalentes podem exibir um só vídeo com várias fontes; abra o item e escolha a fonte antes de reproduzir ou baixar.
- O botão **Vídeos (N)** contabiliza vídeos agrupados, não todas as requisições capturadas.
- Vídeos diferentes no mesmo servidor não são agrupados indiscriminadamente. Tokens, assinaturas e queries não aparecem nos rótulos.
- O aplicativo não garante que a qualidade indicada pelo nome da URL corresponda à resolução real. Streaming HLS/DASH continua apenas para reprodução.

## Interface organizada e vídeos (alpha.31)

- Ferramentas do navegador agrupadas por função, com **menus de cartões**, ícones, subtítulos e rolagem compacta no estilo do app.
- Nova página **Configurações > Vídeos e mídia**: detecção automática, arquivos diretos e playlists HLS/DASH, cada opção independente.
- Lista de vídeos com **seções por formato**: arquivos baixáveis, transmissões somente online e fontes não confirmadas, com ações correspondentes. Nenhum endereço completo com token é mostrado na lista.
- Favoritos, diagnóstico, histórico, organização e seleção de opções foram modernizados. O formulário de favoritos agora tem visual próprio e não fecha quando o endereço é inválido.
- As configurações não mudam o motor HTTP, o login OAuth, cookies nem as regras do bloqueador. O Media3 continua sendo o reprodutor de mídia compatível.

## Bloqueador de anúncios alpha.30

- Proteção ativada por padrão no modo **Forte** e desligável em Configurações > Bloqueio de anúncios. Modos **Leve / Forte / Extremo**, rastreadores e ocultação CSS configuráveis.
- WebView bloqueia apenas **requisições GET HTTP(S) de subrecursos** para domínios publicitários conhecidos. Não intercepta frames principais, POST, respostas criptografadas ou requisições do motor de download, não usa VPN/root nem coleta URLs de navegação.
- Lista offline de sementes locais e atualização opcional **manual** de EasyList + EasyPrivacy por HTTPS. Só aceita filtros simples `||dominio^` e hosts, mais exceções `@@||dominio^`; **não é um motor ABP/uBlock completo** (regras cosméticas, regex, modificadores complexos e regras de vídeo não são importadas).
- Em ⋮ > Bloqueador de anúncios, veja o contador da sessão e escolha **Desativar proteção neste site** se uma página falhar. Exceções salvas localmente.
- Ocultação CSS usa apenas seletores estáticos seguros e depende de JavaScript ativo; não lê formulário, credenciais, cookies nem altera configurações de segurança.
- Tela de login e CAPTCHA identificável não é filtrada; domínios essenciais de autenticação e vídeo são liberados para reduzir falsos positivos. Não há garantia de remoção de anúncios do YouTube ou Facebook.


## Correção da compilação Media3 (alpha.29)

- Corrigida a falha `:app:checkDebugAarMetadata` do workflow **Gerar APK 4**: dez módulos Media3 1.11.1 exigem **API 36** no `compileSdk`, enquanto a alpha.28 compilava com API 35.
- Atualizado `compileSdk 36`, Android Gradle Plugin para **8.10.1**, e GitHub Actions para instalar `platforms;android-36` antes do build.
- **Não** alterados `minSdk 29`, `targetSdk 34`, mecanismo de download, reprodução Media3, WebKit ou armazenamento; o aplicativo não perde compatibilidade intencionalmente com aparelhos Android antigos.
- Adicionada verificação automática no preflight para impedir regressão na combinação Media3 / compileSdk / plugin / Android SDK.
- Não é um APK pronto: o GitHub Actions deverá executar a compilação final com o SDK Android instalado.

## Novidades alpha.28

- Central de downloads ganhou **miniaturas de vídeos e imagens concluídos**, geradas a partir do MediaStore sem internet.
- Para vídeos locais, lê duração e resolução reais com MediaMetadataRetriever; menu **Detalhes da mídia** exibe os dados quando disponíveis.
- Geração ocorre fora da interface, em fila de uma thread com cache LRU de até 4 MB; formatos sem prévia mantêm o ícone anterior.
- Preservados Media3 e WebKit, evitando consumo extra em arquivos ainda baixando.

## Novidades alpha.27

- Adicionado reprodutor de mídia **Media3 1.11.1** para vídeos locais concluídos e links públicos MP4/WebM/HLS/DASH.
- Botão **Reproduzir** na descoberta de vídeos e ação **Reproduzir vídeo** no menu de arquivos concluídos.
- Downloads de arquivo direto continuam pelo motor HTTP existente. HLS/DASH são reproduzidos online; sem download segmentado nesta etapa, sem DRM e sem importar cookies ou tokens do navegador.

## Novidades alpha.26

- Integrado AndroidX WebKit 1.17.1. Desativa downloads de favicons quando a WebView disponibiliza a API; mantém fallback e as proteções existentes.
- Trata encerramento do renderer para recriar a tela e evitar falha total do app. Não altera política OAuth do Google, cookies nem o motor HTTP.



## Compatibilidade de login (alpha.25)

- A WebView aceita janelas de autenticação disparadas explicitamente pelo usuário, com painel temporário e opção de abrir no navegador externo. Popups automáticos continuam bloqueados.
- `⋮ > Diagnóstico de login` mostra versão da WebView, opções de JavaScript/cookies/modo computador e erros de carregamento, sem copiar URL completa, senhas ou tokens; oferece atalho para atualizar WebView.
- Cookies de terceiros continuam bloqueados por padrão, com opção explícita em Configurações. Cookies do Chrome não são importados.
- URLs OAuth com códigos de autenticação e páginas de login não são gravadas no histórico. `about:blank`/`about:srcdoc` são autorizados apenas em subframes internos; `file:`/`content:` e conteúdo misto permanecem bloqueados.
- O Google proíbe OAuth em WebView; login externo é a opção compatível. O Facebook pode exigir verificação adicional sem garantia de sucesso.


## Compatibilidade de login e autenticação (alpha.24)

- O Google bloqueia fluxos OAuth em WebViews. Quando um endereço oficial OAuth é aberto na página principal, o Baixa Fácil informa a limitação e oferece a opção **Abrir navegador**. Não altera o User-Agent para mascarar a WebView nem tenta contornar as regras do Google.
- Em **⋮ > Abrir em navegador externo**, é possível continuar a navegação no navegador compatível instalado, inclusive para desafios do Facebook. **⋮ > Ajuda com login** explica as diferenças de sessão.
- **Configurações > Compatibilidade de login** oferece uma opção **manual e desativada por padrão** para permitir cookies de terceiros. Isso pode ajudar certos logins e desafios, mas reduz a privacidade e não garante autenticação bem-sucedida.
- Ao alterar essa preferência, o navegador incorpora a nova configuração quando retomado e recarrega a página, sem destruir a WebView.
- **Importante:** sessões, cookies e downloads autenticados do navegador externo **não** são transferidos automaticamente para a WebView do app. O login externo pode ser útil para navegar no site, mas não autentica necessariamente downloads internos.
- O detector de vídeo não é executado nas telas sensíveis de login e CAPTCHA do Google/Facebook. A WebView continua bloqueando acesso a arquivos locais e conteúdo misto.

## Detecção de vídeos em páginas (alpha.23)

- O navegador agora mostra **Vídeos** na barra de informações, exibindo **Vídeos (N)** quando encontra URLs diretas elegíveis; também há **⋮ > Procurar vídeos nesta página**.
- A busca identifica URLs de elementos HTML5 `<video>/<source>`, links para MP4, M4V, WebM, MOV, OGV ou 3GP e endereços de arquivos de vídeo diretos observados nas requisições WebView, sem interceptar seu conteúdo.
- Toque em **Vídeos** para pesquisar novamente (útil quando a página carrega vídeo após rolar ou reproduzir). Selecione o arquivo encontrado para **Baixar** ou **Copiar link**.
- O Baixa Fácil usa seu motor de downloads HTTP(S) existente, respeitando condições de rede/bateria, confirmação, autenticação da sessão WebView e as limitações de retomada do servidor.
- **Facebook e outros sites:** a detecção depende de um link direto acessível. `blob:`, DRM, HLS/DASH, segmentos e vídeos inseridos em iframes de outras origens podem não oferecer um arquivo pronto para baixar. O app não converte playlists, não contorna proteções ou autenticações e não promete baixar todo vídeo exibido.
- Links de vídeos e cookies **não** são adicionados ao histórico de navegação nem gravados em arquivo pela detecção: os candidatos ficam somente em memória enquanto a página está aberta.

## Rebranding e novo menu de ações (alpha.22)

- O aplicativo agora se chama **Baixa Fácil** em toda a interface.
- O identificador técnico foi atualizado para **`br.com.baixafacil`**.
- O ícone do app foi redesenhado com uma proposta visual ligada a **internet + download**.
- O antigo menu padrão de ações dos downloads foi substituído por um **painel visual com ícones, descrições curtas e destaque para ações destrutivas**.
- Também foi corrigido o erro de compilação do workflow **Gerar APK 17**, causado por variável Java duplicada em `DownloadEngine.downloadWithRetries`.
- O ZIP contém o código-fonte, não um APK pronto. Execute GitHub Actions para validar a compilação Android completa.


## Favoritos alpha.20

- Use **⋮ > Salvar página nos favoritos** para guardar a página aberta; o mesmo menu permite removê-la quando já estiver salva.
- Use **⋮ > Favoritos** para abrir um site salvo ou tocar em **Gerenciar**.
- Em **Configurações > Navegador > Favoritos**, é possível adicionar manualmente, renomear, editar o endereço e remover. Toque longo sobre um link HTTP(S) também permite salvá-lo.
- Até **60 sites** locais, com título e URL validados; não são salvos cookies nem credenciais ou cabeçalhos. `file:`, `content:`, `javascript:`, `blob:` e links privados não são aceitos.
- Favoritos sobrevivem à limpeza de histórico/cookies/cache do navegador; não há sincronização em nuvem.


## Navegador alpha.19

- Menu **⋮** no topo com localizar na página (resultado anterior/próximo), aumentar/diminuir zoom e alternar modo computador.
- Preferências persistentes em Configurações > Navegador: modo computador e tamanho de texto (75 a 150%).
- Zoom com gesto de pinça; sem controles de zoom flutuantes. A solicitação de layout desktop usa viewport amplo e User-Agent derivado da WebView, sem garantir o comportamento dos sites.
- A versão da WebView instalada é preservada no User-Agent, sem usar um Chrome antigo fixo; ao desligar o modo, retorna-se ao User-Agent original.
- O motor de downloads e seus bloqueios de segurança não foram alterados.

Aplicativo Android nativo em Java com navegador WebView e motor de downloads HTTP/HTTPS independente do `DownloadManager` do Android. Downloads concluídos vão para a pasta pública **Download/**, usando MediaStore (Android 10+).

## Interface unificada (alpha.13)

A tela inicial é o navegador. Uma **barra inferior fixa** oferece: página anterior, próxima página, recarregar, página inicial e **Downloads**. O último ícone alterna para a lista de downloads sem recriar a WebView. Na área de downloads os quatro controles web permanecem visíveis, porém inativos. O botão Voltar do sistema retorna dos downloads ao navegador; no navegador percorre as páginas e, depois, fecha o app.

A aba Downloads contém um cartão compacto com campo inteligente, botão de colar por ícone e botão **Baixar**, ambos dentro da mesma linha. Links padrão fica exclusivamente em Configurações. Inclui filtros Todos/Em fila/Concluídos, pesquisa, cartões de progresso e gerenciamento.

Na engrenagem de cada área abre a **página de Configurações**, que oferece:

- Página inicial personalizada (padrão Google; URLs sem protocolo recebem HTTPS).
- Buscador para texto livre: Google ou DuckDuckGo.
- JavaScript opcional (ligado por padrão); recarregar a página para aplicar.
- Ver histórico recente local (até 12 URLs), reabrir uma visita e limpar histórico/cookies/cache.
- Aparência sistema, claro ou escuro AMOLED, e atalho Links padrão do Android.

A WebView permanece em memória ao alternar, mas o Android pode encerrar o processo em situações de pressão de memória. Com a aba Downloads visível, a WebView é pausada até retornar ao navegador.

## Novidades alpha.18: controles de rede e bateria

Em **Configurações > Downloads • rede e bateria**, existem três chaves independentes:

- **Somente Wi-Fi**: exige que a rede ativa seja identificada pelo Android como Wi-Fi; se a rede mudar, pausa sem apagar os bytes recebidos.
- **Permitir dados móveis**: ativada por padrão. Quando desativada, permite Wi-Fi/Ethernet e bloqueia redes celulares ou não classificadas. Se "Somente Wi-Fi" estiver ligado, esse modo tem precedência.
- **Proteger bateria abaixo de 20%**: desativada por padrão. Quando ligada, pausa em 20% ou menos se o aparelho não estiver carregando. Não há bloqueio quando a leitura da bateria está indisponível.

O aplicativo verifica as restrições no início, ao iniciar um item da fila e periodicamente durante a transferência, para ambos os motores: Foreground Service e UIDT. Quando há bloqueio, marca **Pausado** com o motivo e exige retomada manual. Não bloqueia a navegação web, não solicita permissões extras e não força uma troca de rede. Redes VPN podem não expor o transporte subjacente, portanto são bloqueadas quando não é possível verificar a condição solicitada. Mudanças de rede podem ser percebidas com um pequeno atraso durante leituras bloqueadas pelo sistema; os controles reduzem uso indesejado, mas não são um firewall.

## Correção alpha.17

- Resolvida falha de compilação da alpha.16 identificada em `Gerar APK 15`: a notificação de início de `TransferService` agora é montada diretamente com `Notification.Builder`, sem chamar método removido da classe.
- Os recursos da alpha.16 foram preservados e o pré-teste inclui validação preventiva da inicialização do Foreground Service.
- A compilação real do APK ainda precisa ser repetida no GitHub Actions para confirmar o resultado em Android.

## Novidades alpha.15

- **Toque longo** em links e imagens vinculadas na WebView: abre menu para abrir/copiar/compartilhar ou **Baixar link**. Só URLs HTTP(S) diretas são encaminhadas ao motor. `blob:`, `data:` e links internos não são baixados automaticamente.
- **Fila recuperável:** estado e ordem da fila ficam no histórico local (SharedPreferences, sem armazenar cookies). Ao reabrir, itens interrompidos são marcados e podem ser recolocados tocando em **Recuperar fila (N)**. Retomar é manual, inclusive após reinício do aparelho, e URLs expiradas/cookies de sessão podem exigir novo login.
- **Android 14+ (API 34):** solicita um **user-initiated data transfer job (UIDT)** por gesto explícito do usuário; JobService executa a fila serial usando o mesmo `DownloadEngine`. O job exige notificação, rede e permissão `RUN_USER_INITIATED_JOBS`; não pode ser criado silenciosamente quando o app está em segundo plano. Se o agendamento falhar enquanto a Activity está visível, há fallback para Foreground Service tradicional.
- **Android 10–13:** continua usando Foreground Service com notificação e o mesmo motor HTTP. Nenhuma garantia de que Android manterá o app ativo após parada forçada ou desligamento.
- **Preservação de dados:** arquivo parcial nunca é aceito sem validação de ETag forte/Last-Modified e `206/Content-Range`; se faltar validador, reinicia o arquivo. Cookies WebView só permanecem em memória, nunca na fila persistida.

## Novidades alpha.16

- **Prévia de link direto** antes da confirmação: consulta `HEAD` com tempo limite, não lê o corpo e não envia cookies. Exibe nome sugerido, tipo MIME e tamanho somente se o servidor informar; redirects HTTP(S) são verificados para bloquear downgrade HTTPS→HTTP. Se o site bloquear `HEAD`, ainda é possível confirmar o download sem prévia. Valores consultados são apenas indicativos e não garantem o tamanho/conteúdo do GET.
- **Organização** em Meus downloads: toque no funil para filtrar por tipo de arquivo (pela extensão), ordenar por data, nome ou tamanho e mostrar/ocultar os chips de status.
- **Estimativa de tempo restante** nos cartões ativos quando total e velocidade amostrada forem válidos, com média suavizada para reduzir oscilações. Caso contrário, apenas mostra medição em andamento, sem tempo inventado.
- Nada mudou no motor HTTP/UIDT, na fila persistente ou nas regras de validação de arquivos parciais.

## Downloads e segurança

- URLs digitadas no formato `site.com/arquivo.zip` recebem `https://`. HTTP explícito segue com alerta quando necessário. Não há fallback automático para HTTP.
- O motor aceita apenas URLs HTTP/HTTPS diretas; não intercepta gerenciadores de outros apps nem suporta `blob:`, `sandbox:`, `data:` ou cookies/sessões externos.
- Downloads iniciados pelo navegador usam apenas cookies temporários da WebView, limitados pela origem. Cookies nunca entram no histórico ou diagnósticos; redirecionamentos HTTPS→HTTP são bloqueados pelo motor.
- A WebView mantém bloqueados acesso a arquivos locais (`file://`, `content://`), mixed content e cookies de terceiros.
- Fila serial com pausa, cancelamento, priorização de pendentes, recuperação manual após processo morto, validação de retomada Range + ETag/Last-Modified + Content-Range 206, retries conservadores e diagnósticos.
- Histórico de downloads é diferente do histórico do navegador; limpar dados do navegador não apaga arquivos nem registros de downloads.
- Arquivos são gerenciados pelo MediaStore, com abrir, compartilhar, renomear, excluir e remover do histórico. O app não varre todos os arquivos de Download/.

## Construção

- Java nativo, JDK 17; minSdk 29, targetSdk 34, compileSdk 36; sem biblioteca extra de UI.
- `versionName 0.1.0-alpha.26`, `versionCode 26`.
- `.github/workflows/android.yml` executa testes, compila APK, valida assinatura e pacote/versão e publica **somente o APK** em Releases. O script baixa o asset e compara SHA-256.
- Para APK Release assinado, definir **todos** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`. Sem nenhum Secret, fallback Debug; com apenas alguns, o workflow interrompe para evitar assinatura inconsistente.
- Os Secrets e o arquivo `.p12` privado não devem ser incluídos no projeto ou no repositório.
- O build real do GitHub Actions e os testes de interface em aparelho ainda são necessários para validar esta versão.
