## 0.1.0-alpha.60: editor inline de endereço

- Contrato automático verifica o toque único em `addressDisplay`, `EditText` no mesmo `row` da barra, painel de sugestões incorporado ao `view` após toolbar (não `AlertDialog`), IME no foco e fechamento via Voltar do Android.
- Preflight, parser de importação e mock ABI/publicação devem passar antes do GitHub Actions.
- **Teste manual necessário:** no celular, tocar uma única vez no domínio do Google, verificar seleção do URL/teclado sem diálogo; digitar e conferir sugestões empilhadas abaixo; colar, mic/QR, tecla Ir, voltar/fechar; alternar para Downloads e voltar sem campo travado. Testar com teclado SwiftKey e temas escuro/claro.
- **Não verificado no ambiente local:** compilação Android, comportamento de IME e GeckoView em aparelhos.

## 0.1.0-alpha.59: redesenho do navegador e integrações

- Inspeção dos módulos reais `BrowserPane`, `BrowserPrefs`, `GeckoEngine`, `GeckoPromptHandler`, `GeckoSitePermissions`, mídia WebExtension e fluxo de versão/publicação.
- Preflight atualizado para refletir o editor de endereço, novo menu, permissões, cofre de senhas e mídia; conservar a regressão do workflow 27 na linha executável real do coletor.
- Testar em GitHub Actions: três APKs com GeckoView 158, dependência ZXing e interação de leitura QR, serviços de voz, cofre AES-GCM, login/autofill, HTML/CVS e diferenças de tema.
- Testar em aparelho: menu não ocupar tela, teclado e colagem, QR (permissão/cancelamento), busca manual de imagem/vídeo, popup de consentimento de site, login com autorização, HTTP localhost, tela cheia, favoritos e sites frequentes.
- Limites: favicons PNG/ICO por HTTPS mesmo domínio (outros provedores e `http://127.0.0.1` exibem ícone padrão). Varredura de imagens inclui apenas elementos acessíveis do DOM. Não importa cookies nem gerencia credenciais privadas de outros apps.

## 0.1.0-alpha.58: correção do coletor de APKs (workflow 27)

- Evidência no log: `BUILD SUCCESSFUL in 36s` (47 tarefas) em `:app:assembleRelease`, seguido de `Separação por ABI: Versão incorreta no metadata do APK armeabi-v7a`.
- Causa verificada: a alpha.57 tinha `versionCode 57` no Gradle e nas checagens `aapt`, mas o workflow chamava `collect_abi_apks.py ... --code 56`. Não há evidência de problema nas bibliotecas do GeckoView ou ETP nessa falha.
- Correção alpha.58: todas as validações usam code 58; testes e preflight conferem o argumento `--code` da própria linha executada no CI.
- Execuções locais: `tools/ci_preflight.py`, `tools/test_abi_pipeline.py`, `tools/test_publish_mock.sh`, testes Python/shell sintáticos e integridade do ZIP (registrar resultados da execução).
- Não foram executados neste ambiente: build Gradle/Android completo da alpha.58, coleta de APKs Android reais, publicação GitHub Release e testes físicos do navegador.

## 0.1.0-alpha.57: perfis ETP nativos do GeckoView

- `tools/test_gecko_protection.py` compila a lógica real de perfis com Java 17, testa a matriz modo/rastreadores/cookies, inspeciona o vínculo ao GeckoRuntimeSettings e os callbacks `ContentBlocking.Delegate`.
- `tools/ci_preflight.py` executa a regressão antes do Gradle no GitHub Actions.
- Limites: não há compilação Android neste ambiente, não foi testado o uso real do GeckoView em sites, nem medida redução de tráfego. O contador é de eventos do motor, não de downloads ou anúncios individuais.
- Validar em aparelho: sites normais e login com modo Leve/Forte/Extremo, permitir cookies, alternância entre Configurações e navegador, CSS por domínio, fechar pop-up, Safe Browsing, páginas quebradas em ETP estrito e tamanho das três arquiteturas.

## 0.1.0-alpha.56: ícone adaptativo, redondo e monocromático tematizável do Internet XP

- Validação local das referências de recursos, sintaxe de XML, integridade de bitmaps e versionamento.
- Compilação Android e inspeção no launcher real pendentes.

## 0.1.0-alpha.55: ícone do Internet XP nas densidades padrão e versão redonda de launcher

- Validação local das referências de recursos, sintaxe de XML, integridade de bitmaps e versionamento.
- Compilação Android e inspeção no launcher real pendentes.

## 0.1.0-alpha.54: compatibilidade de sites GeckoView

- Seleção SAF simples/múltipla via `GeckoFilePicker`, sem permissão de armazenamento geral; política MIME e `content://` exercitadas por Java/JDK local.
- Revisão estrutural do Gecko PermissionDelegate, consentimento por origem HTTPS, `onRequestPermissionsResult`, botões da tela cheia e ciclo de vida; testes automatizados em `tools/test_gecko_sites.py`.
- Checklist em `docs/TESTE_APARELHO_GECKOVIEW.md`. Os cenários de câmera, microfone, localização, seletor Android e vídeo HTML5 requerem aparelho/emulador e não foram executados neste ambiente.
- Build Gradle/Android real pendente; preflight e testes mockados não comprovam compatibilidade ABI ou execução de bibliotecas Gecko.

## 0.1.0-alpha.53: tratamento de falhas Gecko e tamanho por ABI

- Checklist de validação no aparelho: `docs/TESTE_APARELHO_GECKOVIEW.md`.
- `tools/test_gecko_crash_policy.py`: compila a regra Java real com JDK e exercita dois reinícios, terceira falha, cooldown, reboot e contador inconsistente.
- `GeckoCrashPolicyTest`: os mesmos cenários com JUnit na tarefa `:app:testDebugUnitTest` (ainda depende da CI).
- `tools/test_publish_mock.sh`: simulação de três APKs/Release, verificação de SHA-256, rejeição de arquivo ausente/extra e checagem dos três tamanhos no resumo.
- `tools/ci_preflight.py`: contrato da fachada, novo teste de crash, publicação e demais verificações estruturais.
- **Não executados neste ambiente:** compilação Gradle/Android, execução em aparelho, medição de memória/latência/tamanho dos APKs reais. A última CI informada foi o workflow 23 da alpha.51, com compilação bem-sucedida e falha na coleta x86; isso não comprova a alpha.53.

## 0.1.0-alpha.52: corrigida matriz de APKs GeckoView

No **Gerar APK 23**, Gradle concluiu com sucesso a compilação de Debug/Release e os testes unitários; a coleta falhou porque o split `x86` de 32 bits não possuía `.so` do GeckoView 158 (`set()`). O Android permite criar esse APK vazio de bibliotecas, mas o navegador falharia ao executar. GeckoView 158 não oferece binários `x86` 32 bits; a última linha publicada nessa arquitetura é a 144.

Agora geramos exclusivamente **três** APKs suportados (`armeabi-v7a`, `arm64-v8a`, `x86_64`). O coletor confere ABI única por APK e a presença explícita de `lib/<abi>/libxul.so`. O workflow e os testes de publicação verificam três assets e SHA-256, evitando APK universal e arquitetura incompleta. A Release alpha.52 requer novo build real, medição de tamanho e teste Android.

## 0.1.0-alpha.51: APKs separados por arquitetura (GeckoView)

O GeckoView reúne bibliotecas nativas volumosas de quatro CPUs. A compilação agora produz **quatro APKs independentes**, um por ABI: `armeabi-v7a` (ARM 32 bits), `arm64-v8a` (ARM 64 bits), `x86` e `x86_64`; `universalApk false` evita o APK universal. Os quatro arquivos levam o mesmo `applicationId`, `versionCode 51` e a mesma versão, apropriados para instalação direta por arquitetura. A compilação precisa confirmar o tamanho real de cada pacote; nenhuma redução exata é garantida antecipadamente.

O GitHub Actions coleta os APKs a partir do `output-metadata.json` do Gradle em vez de presumir nomes ou ordem, valida que cada APK contém `.so` apenas da ABI correspondente, confere assinatura `apksigner`, identidade/versão `aapt`, publica quatro assets na mesma Release e baixa cada um de volta para conferir SHA-256. Os testes simulados incluem APK ausente, extra, arquitetura errada e hash divergente. A assinatura permanece por Secrets (release) ou Debug temporária, sem misturar chaves.

**Limites:** este ZIP tem apenas código-fonte, não contém os APKs compilados; os quatro APKs reais e seus tamanhos só serão gerados e confirmados no GitHub Actions. `minSdk 29`, `targetSdk 34`, SDK 37.0, GeckoView 158, navegação e downloads permanecem inalterados.

## 0.1.0-alpha.50: método ausente no GeckoBrowserView (workflow Gerar APK 21)

### Causa observada

- O SDK 37.0 instalou corretamente, metadados AAR passaram e `:app:compileDebugJavaWithJavac` falhou com exatamente um erro: `BrowserPane.java:440: cannot find symbol: method canGoForward()` em `GeckoBrowserView`.
- `goForward()`, `canForward` e atualização via `onCanGoForward` já existiam, mas o getter não havia sido implementado. Checagens sintáticas anteriores não fazem resolução semântica de símbolos.

### Correção e prevenção

- Adicionado `GeckoBrowserView.canGoForward()` consultando `canForward`.
- `tools/test_gecko_browser_contract.py` checa todas as chamadas diretas de `BrowserPane` à fachada e a ligação dos callbacks de histórico com o estado da sessão. Integrado ao `tools/ci_preflight.py` para rodar antes de baixar o Android SDK no CI.
- Testes locais: preflight, contrato da fachada, teste de instalação SDK simulado, publicação simulada (inclusive SHA-256 incorreto), análise estática Java/recursos, integridade ZIP.
- Limite: teste de contrato não compila Java/Android nem prova ausência de outros erros semânticos; reexecutar `gradle :app:testDebugUnitTest :app:assembleDebug` no GitHub Actions e testar no aparelho.

## 0.1.0-alpha.49: etapa SDK corrigida (workflow Gerar APK 20)

### Falha confirmada no log

- Etapa `Instalar SDK API 37` executou `sdkmanager --install 'platforms;android-37.0' 'build-tools;36.0.0' test -s ...` na mesma linha, devido ao tratamento YAML do campo `run` sem bloco literal.
- O SDK Manager produziu `Warning: Failed to find package 'test'` e a etapa terminou com exit code 1. O Gradle **não** foi executado e o log não contém erro de compilação Java.

### Correção, prevenção e limitações

- A etapa usa `run: |`, `set -euo pipefail` e comandos individuais para instalação e verificação do JAR da API 37.0.
- `tools/test_sdk_workflow.py` verifica o script da etapa e executa cenários simulados de instalação bem-sucedida e ausência da plataforma; é acionado por `tools/ci_preflight.py` e este é executado antes da instalação real no workflow.
- Executados localmente: **61 verificações** do preflight; testes simulados do SDK (sucesso e plataforma ausente); simulação de Release e falha com SHA-256 divergente; parse de **85 arquivos Java** sem erros sintáticos; parse YAML, XML/JSON e verificação JavaScript; integridade ZIP.
- Testes estáticos, simulação do SDK e simulação de publicação **não** substituem compilação Android. Executar a CI e testar em aparelho.

## 0.1.0-alpha.48: nome do pacote Android SDK 37.0 (workflow Gerar APK 19)

### Falha confirmada nos logs

- Etapa `Instalar SDK API 37` encerrou com código 1, antes do Gradle. `sdkmanager --install 'platforms;android-37' 'build-tools;36.0.0'` produziu `Warning: Failed to find package 'platforms;android-37'`.
- A versão correta do pacote de plataforma é `platforms;android-37.0`, com pasta `platforms/android-37.0`. O nome genérico `android-37` não resolve esse pacote.

### Correção e verificação

- Atualizado o comando de instalação para API 37.0 e verificação `test -s "$ANDROID_HOME/platforms/android-37.0/android.jar"`.
- `compileSdk { version = release(37) { minorApiLevel = 0 } }` seleciona explicitamente API 37.0 no AGP 9.1.1.
- Versionamento 0.1.0-alpha.48 e documentação/validações sincronizados.
- Testes locais de pré-validação e publicador simulado executados; **não** foi realizado build Android, pois o ambiente não oferece SDK 37.0/Gradle. O próximo log poderá revelar erros posteriores, atualmente desconhecidos.

## 0.1.0-alpha.47: correção de AAR metadata (workflow Gerar APK 18)

### Falha verificada

- `:app:checkDebugAarMetadata FAILED`, antes de `:app:compileDebugJavaWithJavac`.
- GeckoView 158.0.20261007201254 exige `compileSdk >= 37`; AndroidX Core e Core-KTX 1.19.0 exigem `compileSdk >= 37` e AGP >= 9.1.0. Cinco problemas detectados na etapa de metadados.
- O projeto anterior usava API 36, AGP 8.10.1 e Gradle 8.11.1.

### Correção

- Projeto passou a `compileSdk 37`, AGP 9.1.1 e Gradle 9.3.1 com JDK 17. GitHub Actions instala API 37 e Build Tools 36.0.0; valida assinatura e pacote com as mesmas Build Tools.
- `minSdk 29`, `targetSdk 34` e a dependência GeckoView permanecem iguais.
- `python3 tools/ci_preflight.py`, `bash tools/test_publish_mock.sh`, validação estrutural, XML, JS e integridade do ZIP.
- Compilação Android real (`gradle :app:testDebugUnitTest :app:assembleDebug`) **não executada localmente** porque SDK Android, Gradle e dependências baixáveis não estão disponíveis. Reexecutar no GitHub Actions; se passar da checagem AAR, eventuais erros Java/API do GeckoView precisam ser avaliados no próximo log.

## 0.1.0-alpha.46: migração GeckoView

Navegador integrado GeckoView (Firefox), sessão nativa, detector local de vídeos via WebExtension. Algumas funções dependentes de interceptação de rede do WebView não têm equivalente nesta versão. Consulte RELEASE_NOTES.md.

# Validação 0.1.0-alpha.45

## Problema reproduzido na captura

- Em alguns sites há várias URLs HLS, mas a WebView só observa requisições de rede e não pode afirmar que representam vídeos distintos. A UI anterior mostrava "Vídeos encontrados (4)".
- Requisições HLS não possuem uma relação obrigatória com elementos DOM, portanto não é seguro inserir uma thumbnail qualquer em cada CDN. Agora a imagem opcional do player é separada e identificada como **prévia da página**.
- A alpha.44 não baixava playlists, apenas executava mídia HLS via Media3. A alpha.45 oferece download limitado para HLS VOD com segmentos MPEG-TS não criptografados. Outras playlists falham explicitamente e não viram arquivos falsamente concluídos.

## Testes de segurança e integridade

- HlsVodPlaylistTest: playlist TS finalizada, master e maior variante, rejeição de live, DRM, fMP4, outros formatos, downgrade HTTPS, playlist incompleta.
- VideoMenuModelTest: agrupamento de qualidades por identidade, mas sem mesclar vídeos de caminhos distintos nem mostrar tokens na tela.
- Java syntax e Java puro; `python3 tools/ci_preflight.py`; `bash tools/test_publish_mock.sh`; validação do arquivo ZIP.

## Limitações

- `.ts` não é `.mp4` e nem todos os reprodutores suportam contêiner MPEG-TS. A alpha.45 não remuxa codecs nem executa FFmpeg.
- Playlists HLS criptografadas, ao vivo, fMP4 ou com áudio externo são recusadas. URL com sessão expirada e bloqueio de servidor pode falhar.
- Reiniciar download HLS interrompido descarta bytes parciais antigos e transfere tudo novamente, preservando as regras de cancelamento do motor existente.
- Necessários GitHub Actions e teste físico, especialmente downloads grandes, rede instável, pausa/cancelamento e tamanho de arquivo.

# Validação 0.1.0-alpha.44

## Bug corrigido

- Alpha.43: `MainActivity.createBottomBar` usava `if(downloadsVisible)return;` para qualquer botão que não fosse o quinto. Assim, Home e Voltar ficavam sem ação na tela Downloads, criando a impressão de navegação bloqueada.
- Alpha.44: `MainNavigationPolicy.bottomAction` define ação explícita para cada botão. Home e Voltar em Downloads retornam ao navegador **sem chamar** `browserPane.home()` ou recriar a WebView; no navegador, Home ainda abre a página inicial.
- A seta Voltar do Android é roteada por `MainNavigationPolicy.systemBack`, preservando a prioridade de encerrar seleção múltipla.
- Rótulos no rodapé e botão **Voltar ao navegador** no cabeçalho facilitam a saída da tela sem depender de gestos.

## Verificações

- `MainNavigationPolicyTest`: testes JVM de comandos do rodapé e botão Voltar em cenários com Downloads, navegador e seleção.
- `python3 tools/ci_preflight.py` e `bash tools/test_publish_mock.sh`.
- Parse Java dos arquivos, validação de XML/YAML e integridade CRC do pacote ZIP.

## Testar no Android real

1. Abra um site no navegador, vá para Downloads e pressione **Navegador**. Verifique que retorna ao mesmo site, sem reiniciar o carregamento.
2. Repita com a seta **Voltar**, com o botão **Downloads** e com a tecla Voltar do sistema.
3. Em Downloads, abra seleção múltipla, pressione Voltar: deve sair da seleção; pressione novamente: deve voltar ao navegador.
4. No navegador, pressione **Início** e confirme que carrega a página inicial das Configurações.
5. Abra a busca na lista com teclado visível, depois volte ao navegador. O teclado não deve cobrir a tela nem perder cookies/histórico.
6. Testar após girar tela/recriar Activity e depois de deixar downloads longos executando.

## Limites

- Não foi executada compilação Android completa nem teste físico no ambiente local. Reexecutar workflow do GitHub Actions e validar comportamento no aparelho.
