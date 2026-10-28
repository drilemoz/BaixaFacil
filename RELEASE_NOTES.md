# Baixa Fácil 0.1.0-alpha.68 (versionCode 68)

## 0.1.0-alpha.69
Novo assistente opcional de primeira abertura para aparência, buscador, privacidade e downloads. Filtro adulto local (desativado por padrão) com bloqueio de domínios conhecidos e pesquisa segura em buscadores compatíveis. Configurações editáveis a qualquer momento. A filtragem não é infalível nem substitui controle parental. Build Android e testes em aparelho ainda pendentes.


Correção de compilação após os logs do workflow **Gerar APK 34**: a rotina de permissões utilizava `pendingAndroidOrigin`, variável inexistente. Agora consulta corretamente `requestedAndroidOrigin`, preservando o cancelamento seguro quando a navegação muda de origem.

Incluído teste `javac` semântico da classe com stubs Android/GeckoView e casos de regressão de origem. Código-fonte validado localmente, mas **APK Android não compilado neste ambiente**. Mantidos três APKs por ABI, GeckoView 158 e políticas de download/assinatura.

# Baixa Fácil 0.1.0-alpha.67 (versionCode 67)

**Barra de endereço corrigida:** um toque no endereço atual abre a edição diretamente na própria barra, com URL completa e teclado. Sugestões aparecem abaixo apenas após modificar o campo ou escolher Histórico no menu, sem bloquear o editor. O botão Ir do teclado continua navegando, e o Voltar encerra a edição.

Preservados GeckoView 158, ETP, cofre de senhas, imagens e mídias, três APKs por ABI e downloads. **Testes estáticos e de regressão local não substituem a compilação Android nem o teste de toque e teclado em dispositivo físico.**

# Baixa Fácil 0.1.0-alpha.66 (versionCode 66)

**Segurança e compatibilidade do navegador.** Requisições de permissões web exigem origem HTTPS correspondente à página principal e são rejeitadas quando a navegação troca de origem antes da resposta. Portas diferentes não compartilham permissão; links internos na mesma origem não interrompem a solicitação. O formulário de opções HTML agora mantém grupos e entradas desativados corretamente, incluindo seletores múltiplos.

Não altera motor HTTP, cookies, dados criptografados ou download existente. Preserva três APKs por ABI e Secrets. **Testes executados localmente não equivalem ao build Gradle nem à validação física de câmera/microfone/permissões.**

# Baixa Fácil 0.1.0-alpha.65 (versionCode 65)

Esta versão diminui o trabalho visual das listas, sem alterar a execução de downloads. O carregador de favicons passou a deduplicar pedidos sob limites fixos, reutilizar o cache em memória e evitar buscas reiteradas em sites sem ícones. Quando o Android sinaliza pressão de RAM, a fila de ícones antigos é descartada sem reciclar bitmaps ainda visíveis. Na central de downloads, a rolagem rápida adia o carregamento de miniaturas até a lista parar e reaproveita a raiz visual dos cartões. Mantidos GeckoView 158, ETP, importação, login, Media3 e três ABIs. Testes de lógica foram executados localmente; a compilação Android e a avaliação no celular continuam pendentes.

# Baixa Fácil 0.1.0-alpha.64 (versionCode 64)

## Estabilidade e economia de recursos

O painel de downloads reduz consultas periódicas enquanto ocioso, mas mantém atualização rápida se há downloads em andamento. A geração de miniaturas locais agora tem um limite explícito de fila, reutiliza trabalhos repetidos e descarta miniaturas do cache sob sinalização de pouca memória do Android, sem interromper downloads.

A versão preserva GeckoView 158, ETP, segurança TLS, downloads e três APKs por arquitetura. **Este ZIP é código-fonte, sem build Android ou medições reais de bateria.** É necessário compilar no GitHub Actions e verificar a interface no aparelho.

# Baixa Fácil 0.1.0-alpha.63 (versionCode 63)

## Downloads e detecção de mídia
- Filtros rápidos Todos, Mídias, Vídeos, Imagens e Áudios, com contagens do histórico local do app. A seleção dos tipos não altera arquivos ou a fila.
- Varredura manual ampliada de imagens responsivas, carregamento tardio, pôsteres de vídeo e CSS inline da página, com limites de segurança. URLs inseguras são rejeitadas; não há download automático ou promessa de acesso a mídias protegidas.
- Identificação por nome de imagens AVIF, HEIF, APNG e JFIF. Contagem se refere a registros, não a arquivos detectados em todo o celular.
- GeckoView 158, ETP, downloads/retomada, assinatura e três APKs por ABI preservados. Os APKs precisam ser compilados no GitHub Actions e testados em Android antes de considerar a versão validada.

# Baixa Fácil 0.1.0-alpha.62 (versionCode 62)

## Migração de dados do navegador
Importação de favoritos exportados em HTML Netscape ou CSV com colunas URL e nome; histórico em CSV com coluna URL. O usuário seleciona o arquivo, vê uma prévia das quantidades e confirma antes de mesclar. Não altera títulos de favoritos existentes nem gera visitas fictícias. A opção de senhas CSV permanece separada e cifrada com Android Keystore.

**Limites:** máximo 400 KB/500 registros por arquivo, 60 favoritos e 120 endereços no histórico. Não acessa cookies, históricos protegidos nem sessões de outros aplicativos. Nenhum APK desta versão foi compilado ou testado em aparelho neste ambiente.

# Baixa Fácil 0.1.0-alpha.61 (versionCode 61)

## Senhas e importação
O gerenciador lista contas por site e usuário, oferece exclusão individual ou de todas as contas com confirmação no tema do app e nunca exibe a senha. Uma falha ao ler o cofre cifrado não será tratada como lista vazia para exclusão. A importação CSV aceita linhas citadas com quebras de linha, aspas e vírgulas, rejeitando CSV inválido.

**Validação:** testes estáticos e JDK locais não substituem build Gradle/GeckoView e testes em Android. Após compilar, validar login real, importação e exclusão com contas de teste, sem usar senhas pessoais.

# Baixa Fácil 0.1.0-alpha.60 (versionCode 60)

**Barra de URL sem pop-up:** tocar no endereço abre imediatamente a edição na própria barra, selecionando o URL e solicitando o teclado. Sugestões, histórico, links copiados e sites frequentes surgem em painel que se expande para baixo, sem exigir segundo toque. O leitor QR e a voz ficam na barra durante edição; Voltar fecha primeiro a edição. Recursos e dados existentes preservados.

**Validação:** testes estáticos da estrutura inline, parser HTML/CSV, pré-validação ABI, simulação de publicação e integridade do ZIP executados localmente. Ainda é preciso compilar e testar o APK no Android.

# Baixa Fácil 0.1.0-alpha.59 (versionCode 59)

**Navegador redesenhado:** barra de navegação com domínio/favicons e menu ⋮, editor de endereço com sugestões verticais, link do clipboard para colar, sites frequentes, voz, leitor QR, progresso GeckoView com porcentagem e botão flutuante de mídias/imagens.

**Formulários e senhas:** seleção de opções agrupadas, consentimento para salvar logins e cofre cifrado com Android Keystore; importação manual de favoritos HTML e senhas CSV. Dados de outros navegadores não são acessados sem exportação do usuário. Permissões do site usam janelas com tema do app.

**Segurança:** somente fontes HTTP(S) expostas pela página, sem contornar DRM, sem sincronizar cookies, sem enviar favicons a serviços de terceiros. Nenhum teste Android real concluído neste ambiente; testar compilação, leitura QR, permissões, sessões e login antes do uso como navegador principal.

# Baixa Fácil 0.1.0-alpha.58 (versionCode 58)

**Correção da geração dos APKs:** o workflow 27 compilou a versão anterior, mas não publicou os arquivos porque a etapa de separação por arquitetura validava `versionCode 56` em vez de 57. A nova versão corrige o código esperado para 58 e adiciona testes de sincronização do comando do workflow.

**Sem alteração funcional:** navegador GeckoView, bloqueio ETP, downloads, proteções de sessão, suporte ARM32/ARM64/x86_64 e regras de assinatura continuam inalterados.

**Validação:** compilação e testes da alpha.57 concluíram no workflow 27; coleta/publicação da alpha.58 exigem novo GitHub Actions.

# Baixa Fácil 0.1.0-alpha.57 (versionCode 57)

**Proteção nativa no GeckoView:** os perfis de anúncios passam a configurar ETP padrão ou rigorosa. O modo Extremo habilita proteção social rigorosa e limpeza de parâmetros conhecidos. A contagem de bloqueios vem do motor, sem armazenar endereços.

O controle de cookies de terceiros continua explícito: ETP usa isolamento por origem, e a opção de compatibilidade permite cookies de terceiros ao custo de suspender ETP. As listas EasyList locais não interceptam requisições e as exceções por domínio alteram apenas a ocultação CSS. Não há garantia de bloqueio de 100% dos anúncios.

**Validação:** verificações de código e lógica local; APKs Android e comportamento em sites reais ainda exigem GitHub Actions e teste no aparelho.

# Baixa Fácil 0.1.0-alpha.56 (versionCode 56)

- ícone adaptativo, redondo e monocromático tematizável do Internet XP.
- Mantidos GeckoView 158, downloads e três APKs por arquitetura.
- Build Android e aparência no aparelho ainda não confirmados.

# Baixa Fácil 0.1.0-alpha.55 (versionCode 55)

- ícone do Internet XP nas densidades padrão e versão redonda de launcher.
- Mantidos GeckoView 158, downloads e três APKs por arquitetura.
- Build Android e aparência no aparelho ainda não confirmados.

# Baixa Fácil 0.1.0-alpha.54 (versionCode 54)

**Compatibilidade GeckoView:** formulários web agora abrem o seletor de arquivos do Android (um ou vários), sem solicitar acesso geral ao armazenamento. Vídeos HTML5 podem usar a tela cheia, com saída por Voltar e restauração ao abrir Downloads.

**Permissões:** câmera, microfone e localização somente após solicitação de site HTTPS e consentimento do usuário, com permissões Android quando necessárias. Outros acessos sensíveis não são liberados silenciosamente.

**Não incluído:** seleção de pastas, captura direta pelo `input file`, notificações push/web, captura de tela ou áudio interno. Não altera download por cookies nem oferece DRM.

**Verificação:** testes locais de integração e política MIME; build Android e execução em aparelho ainda pendentes. APKs ARM32/ARM64/x86_64 dependem do GitHub Actions.

---

# Baixa Fácil 0.1.0-alpha.53 (versionCode 53)

**Estabilidade GeckoView:** recuperação automática limitada a duas tentativas durante 120 segundos. Se o processo de conteúdo continuar encerrando, o aplicativo oferece tentar manualmente, abrir o site no navegador externo ou voltar aos downloads. Sessão/cookies não são copiados para o motor HTTP.

**APKs por arquitetura:** permanece a geração `armeabi-v7a`, `arm64-v8a`, `x86_64`, sem universal/x86 32 bits. O resumo do GitHub Actions passa a apresentar tamanhos reais de cada arquivo e SHA-256 após publicação. Não foram gerados APKs neste ambiente.

**Validação local:** testes da regra Java de prevenção de crash, coletor de ABIs, publicador simulado e pré-validação. Gradle/Android e teste em aparelho pendentes.

---

# Baixa Fácil 0.1.0-alpha.52 (versionCode 52)

## APKs por arquitetura validados para GeckoView 158

- Build 23 anterior comprovou compilação Debug e Release, além dos testes unitários; falha posterior identificou que x86 (32 bits) não possui bibliotecas nativas do GeckoView.
- Três APKs: ARM 32 bits (`armeabi-v7a`), ARM 64 bits (`arm64-v8a`) e Intel/AMD 64 bits (`x86_64`). Nenhum APK universal ou x86 32 bits vazio.
- CI e Release conferem nativos `libxul.so`, assinatura, pacote, versão e SHA-256 de cada arquivo.
- GeckoView 158 e downloads preservados.

**Status:** código-fonte corrigido. Geração de APK alpha.52, tamanho de cada APK e execução em dispositivos ainda pendentes.

# Baixa Fácil 0.1.0-alpha.51 (versionCode 51)

## APKs menores separados por CPU

- ARM 32 bits `armeabi-v7a`, ARM 64 bits `arm64-v8a`, Intel 32 bits `x86`, Intel 64 bits `x86_64`.
- Um APK por arquitetura, sem APK universal com todas as bibliotecas do GeckoView.
- Quatro arquivos na mesma Release, validados por identidade, versão, assinatura, ABI nativa e SHA-256 baixado do GitHub.
- Build `Release` com os quatro Secrets de assinatura, ou fallback `Debug` quando nenhum foi fornecido. Secrets parciais causam falha explícita.
- Preservadas as funções de navegador e download; versões da plataforma e dependências não alteradas.

**Status:** testes locais e publicação simulada; ainda não há build Android completo nem medição real do tamanho dos APKs por ABI.

# Baixa Fácil 0.1.0-alpha.50 (versionCode 50)

## Correção do workflow Gerar APK 21

- O Android SDK foi instalado e o Gradle alcançou a compilação de fontes Java. A compilação encontrou apenas `GeckoBrowserView.canGoForward()` ausente, chamado a partir da linha 440 do navegador.
- Adicionado método que consulta o estado `canForward` já alimentado por `onCanGoForward`, preservando o comportamento de navegação do GeckoView.
- Novo teste do contrato da fachada evita novos esquecimentos de métodos invocados pelo `BrowserPane` durante a evolução da migração.
- Dependências, downloads e configurações de segurança sem alterações.

**Status:** validações estáticas locais, publicação simulada e integridade do ZIP; compilação Android completa e execução real ainda não confirmadas nesta versão.

# Baixa Fácil 0.1.0-alpha.49 (versionCode 49)

## Correção do workflow Gerar APK 20

- A instalação da API 37.0 estava formatada como um bloco YAML `run:` dobrado. O GitHub Actions concatenou o comando de instalação e a expressão `test -s ...`, e o SDK Manager tentou localizar um pacote inexistente chamado `test`.
- Corrigido para `run: |`, com comandos separados, parada em caso de falha e teste do arquivo `platforms/android-37.0/android.jar`.
- Adicionado teste automatizado simulando o SDK Manager, executado pelo preflight antes do passo real de instalação.
- Mantidas as configurações Android/GeckoView e os recursos da alpha.48.

**Status:** verificações locais de código/configuração; sem compilação Android completa nem validação no aparelho.

# Baixa Fácil 0.1.0-alpha.48 (versionCode 48)

## Correção do workflow Gerar APK 19

- SDK Android 17/API 37 é distribuído no `sdkmanager` como `platforms;android-37.0`; instalação do pacote `android-37` falhava antes do Gradle.
- O GitHub Actions instala `platforms;android-37.0` e `build-tools;36.0.0`, com verificação do `android.jar`.
- Gradle seleciona explicitamente `release(37)` com `minorApiLevel = 0`, compatível com a plataforma 37.0.
- Mantidos GeckoView 158, `minSdk 29`, `targetSdk 34`, AGP 9.1.1 e Gradle 9.3.1.

**Status:** validações locais do projeto; compilação Android e teste físico pendentes do próximo workflow.

# Baixa Fácil 0.1.0-alpha.47 (versionCode 47)

## Correção da compilação GeckoView

- Corrigidas incompatibilidades de metadados AAR observadas no workflow **Gerar APK 18**.
- `compileSdk 37`, Android Gradle Plugin **9.1.1**, Gradle **9.3.1** e Android Build Tools **36.0.0**; CI instala `platforms;android-37`.
- GeckoView 158 e recursos da alpha.46 mantidos; `minSdk 29`/`targetSdk 34` não foram alterados.
- Versão 47 sincronizada no app, GitHub Actions, pré-validação, teste de publicação e documentação.

**Status:** preflight e testes locais disponíveis; compilação Android completa e testes em aparelho ainda pendentes. O log fornecido comprova a falha anterior, não a conclusão deste build corrigido.

# Baixa Fácil 0.1.0-alpha.46 (versionCode 46)

## Integração GeckoView

- Navegação integrada migrada de `android.webkit.WebView` para `org.mozilla.geckoview.GeckoView` e `GeckoSession`.
- Runtime Firefox Gecko único por processo. Pesquisa na página, navegação, modo desktop, histórico, tratamento de pop-ups, prompts JavaScript básicos e links de download adaptados para delegates do GeckoView.
- Uma WebExtension empacotada e sem requisições externas adicionais faz a detecção básica de fontes MP4/WebM/HLS/DASH e oculta seletores CSS locais.
- Preferências de cookies e limpeza de dados migradas para `StorageController`.
- Regras de domínios EasyList/EasyPrivacy **não são interceptadas no GeckoView** nesta versão; o bloqueio de rastreamento nativo do Gecko está habilitado e ocultação CSS parcial permanece.
- Links que exigem autenticação baseada em cookies da página **podem falhar no gerenciador de downloads**, porque o GeckoView não fornece os cookies ao motor HTTP próprio. URLs diretas continuam suportadas.
- Listagem e exclusão seletiva de armazenamento por origem, miniaturas capturadas da página e detecção de mídia em tráfego de rede não estão disponíveis nesta migração.
- O pacote adiciona o GeckoView, aumentando substancialmente o tamanho do APK.

## Verificação pendente

Não foi possível executar `:app:assembleDebug` ou testes no aparelho neste ambiente sem SDK/Gradle e dependência GeckoView em cache. Valide o workflow de CI e teste navegação, login, arquivos, vídeo e consumo de RAM na instalação real.

# Baixa Fácil 0.1.0-alpha.45 (versionCode 45)

## Fontes de vídeo e download HLS experimental

- O painel diferencia **fontes HLS/MP4** de vídeos efetivamente identificados. URLs diferentes não são necessariamente vídeos diferentes; só são agrupadas quando compartilham identidade comprovável.
- Adicionado botão **Baixar** nos cartões de arquivos diretos e HLS. Fontes com várias qualidades exigem escolher qual usar. DASH continua apenas reprodução.
- HLS VOD simples agora pode salvar arquivos MPEG-TS (`.ts`) na pasta pública Downloads usando a fila e o processamento em segundo plano existentes. Playlist master escolhe a maior banda informada, mas não mede qualidade real.
- O suporte é deliberadamente limitado: sem DRM/criptografia, HLS ao vivo, fMP4, áudio em playlist separada, byte ranges ou interrupções de continuidade. URLs expiradas ou sem autorização ainda podem falhar. Sem conversão para MP4. Reinício da transferência a partir do começo após pausa.
- Quando um player DOM visível fornece dados suficientes, a janela pode apresentar **prévia da página** separada, sem fingir que corresponde necessariamente a cada fonte de CDN.
- A transferência exige confirmação específica, sem transferir cookies a outra origem. Segmentos MPEG-TS passam por checagens básicas de formato, limites de quantidade/tamanho e publicação MediaStore somente após finalização.

## Validação

- Testes JVM do parser HLS VOD e modelos de agrupamento, pré-validação e simulação de Release.
- **Pendente:** build Android pelo GitHub Actions e teste no aparelho. ZIP contém fontes, não APK compilado.
