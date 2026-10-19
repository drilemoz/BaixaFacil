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
