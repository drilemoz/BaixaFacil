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
