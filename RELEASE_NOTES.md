# Baixa Fácil 0.1.0-alpha.38 (versionCode 38)

## Correção do erro de compilação

- Corrigido o erro de compilação da alpha.37 encontrado no workflow **Gerar APK 10**, em `SettingsActivity.java:163`, na função **Cookies e sessões > Sites com dados locais**.
- O Android fornece mapa sem genéricos via `WebStorage.getOrigins`; o app agora valida chaves e valores antes de montar uma lista tipada, evitando `Object cannot be converted to Entry<String,Origin>`.
- Mantidos histórico, favoritos, downloads, configurações e otimizações da WebView. Não foi alterada a política de cookies; exclusão por origem segue restrita ao WebStorage.

## Validação

- Testes JVM de regressão e verificação preventiva do preflight, incluindo contrato do callback com mapa bruto.
- Simulação de publicação e integridade do ZIP.
- **Pendente:** repetir `:app:testDebugUnitTest :app:assembleDebug` no GitHub Actions; este arquivo é o código-fonte, não o APK.
