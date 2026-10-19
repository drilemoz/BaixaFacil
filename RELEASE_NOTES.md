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
