# Baixa Fácil 0.1.0-alpha.24


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

- Java nativo, JDK 17; minSdk 29, targetSdk 34, compileSdk 35; sem biblioteca extra de UI.
- `versionName 0.1.0-alpha.24`, `versionCode 24`.
- `.github/workflows/android.yml` executa testes, compila APK, valida assinatura e pacote/versão e publica **somente o APK** em Releases. O script baixa o asset e compara SHA-256.
- Para APK Release assinado, definir **todos** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`. Sem nenhum Secret, fallback Debug; com apenas alguns, o workflow interrompe para evitar assinatura inconsistente.
- Os Secrets e o arquivo `.p12` privado não devem ser incluídos no projeto ou no repositório.
- O build real do GitHub Actions e os testes de interface em aparelho ainda são necessários para validar esta versão.
