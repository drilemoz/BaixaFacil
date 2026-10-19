# GeckoView no Baixa Fácil (alpha.50)

## Dependências e arquitetura

- Runtime: `org.mozilla.geckoview:geckoview:158.0.20261007201254`, Maven da Mozilla.
- Java 17, `compileSdk` Android API **37.0** (`release(37)` e `minorApiLevel = 0`), Android `minSdk 29` preservado.
- `GeckoEngine`: uma única instância de GeckoRuntime por processo e operações de `StorageController`.
- `GeckoBrowserView`: `GeckoView` + `GeckoSession` + extensão de mídia local.
- `GeckoPromptHandler`: janelas básicas de alerta/confirmação, entrada de texto e seleção simples/múltipla.
- `BrowserPane`: interface e eventos de navegação, downloads, pop-up, pesquisa, status e detector de vídeos.
- `app/src/main/assets/baixafacil_gecko`: extensão local em JavaScript com comunicação nativa da sessão.

## Estado dos recursos

| Recurso | GeckoView alpha.50 |
| --- | --- |
| Navegação, URL, histórico de sessão, recarregar | Adaptados |
| Modo desktop / tamanho de texto / JavaScript | Adaptados |
| Limpeza de cookies, armazenamento e cache | StorageController |
| Proteção contra rastreadores | Gecko nativo, configurável |
| Ocultação cosmética de anúncios | CSS local simples pela WebExtension |
| EasyList / EasyPrivacy para interceptação de requisições | Não integrado |
| Detecção de vídeo DOM (`video`, `source`, links diretos) | WebExtension local |
| Inspeção de requisições de rede / fontes MSE | Não integrado |
| Download de arquivo direto e HLS VOD suportado | Motor antigo conservado; depende de URL acessível |
| Download protegido com cookies do login do Gecko | Não integrado |
| Listagem de cookies / armazenamento por origem | Não integrado |
| Caixa JS de alerta/confirm/prompt/select | Suporte básico; outros prompts dependem de implementação |
| Seletor de arquivos web, autenticação HTTP Basic, DRM | Não integrado |
| Login Google via Custom Tabs / navegador externo | Preservado |

## Segurança e testes

- A extensão é empacotada e não é carregada de servidores remotos. Seus resultados são tratados como não confiáveis, limitados em quantidade e passam pela validação de candidatos existente.
- Nenhum cookie GeckoView é copiado para o motor HTTP do gerenciador de downloads. A falta de suporte a downloads autenticados evita transferir inadvertidamente credenciais.
- A limpeza de sessões desativadas espera a conclusão da remoção de dados antes de navegar. Em erro, a navegação não é liberada automaticamente.
- `tools/ci_preflight.py` verifica dependência, bridge, versionamento, regras de UI, preferências e workflow.
- Em ambiente com Android SDK 37 e Gradle 9.3.1, executar: `gradle :app:testDebugUnitTest :app:assembleDebug`.
- Testes no aparelho: navegação e histórico, pop-up permitido/bloqueado, seleção/alert, detecção MP4/WebM/HLS/DASH, downloads e cookies. Comparar PSS e bateria do Gecko com a versão WebView separadamente.
