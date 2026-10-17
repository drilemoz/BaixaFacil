# Baixa Fácil 0.1.0-alpha.24 (versionCode 24)

## Compatibilidade com logins

- Tratamento explícito dos fluxos OAuth do Google, que são bloqueados dentro de WebViews por política do próprio Google.
- Nova ação `⋮ > Abrir em navegador externo`, para abrir sites de autenticação em outro navegador instalado.
- Nova opção `⋮ > Ajuda com login`, com explicações para Google, Facebook e sessões independentes.
- Nova configuração **Compatibilidade de login**, permitindo cookies de terceiros somente quando a pessoa optar por ativá-los, com alerta de privacidade.
- Indicação ao visitar páginas de verificação do Facebook. Detecção de vídeos suspensa nessas telas sensíveis de login/CAPTCHA. O suporte não é universal e CAPTCHA pode continuar rejeitando WebViews.

## Preservado

- Navegador e downloads integrados, favoritos, detecção de vídeos diretos, filas HTTP/UIDT e retomada conservadora.
- Bloqueios de esquemas não HTTP(S), conteúdo misto, acesso local e redirecionamentos inseguros.

## Pendente

- Build Android completo no GitHub Actions.
- Testar acesso a Google, Facebook e outros sites em aparelho; o login externo não autentica a WebView automaticamente.
