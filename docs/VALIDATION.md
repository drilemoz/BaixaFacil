# Validação 0.1.0-alpha.24

## Diagnóstico

- A alpha.23 usa WebView com cookies de terceiros bloqueados, janela múltipla desabilitada, JavaScript configurável e armazenamento DOM ativo.
- O Google restringe fluxos OAuth em WebViews: a correção compatível é oferecer navegador externo, não simular outro user-agent.
- Sites como Facebook podem exigir cookies de terceiros ou recursos de verificação que não funcionam em WebViews; o app não garante login.

## Mudanças

- Nova classe pura `AuthNavigation` identifica URLs OAuth do Google e checkpoints do Facebook usando esquema HTTPS, domínio e caminho verificados.
- Interceptação de fluxo OAuth principal com informação e opção de navegador externo; menu `⋮` também permite abrir manualmente página atual no navegador escolhido e consultar ajuda de login.
- Nova opção desativada por padrão para cookies de terceiros, ativada somente após decisão informada e reaplicada à WebView quando a preferência muda.
- Detector de vídeo suspenso em telas sensíveis de login/CAPTCHA do Google/Facebook. Cookies de sessão externa não são importados; bloqueios de arquivos locais, conteúdo misto e downloads inválidos continuam.

## Verificação necessária

- Executar `python3 tools/ci_preflight.py`, `bash tools/test_publish_mock.sh` e os testes JVM.
- Compilação Gradle/Android completa no GitHub Actions permanece pendente.
- No aparelho: abrir URL OAuth, confirmar navegador externo e que o Baixa Fácil não se torna o navegador do próprio chooser; testar cookies padrão e opt-in com Facebook, e conferir o retorno ao aplicativo sem perda da página.
