# Versionamento do Baixa Fácil

- Versão atual: `0.1.0-alpha.52`.
- `versionCode`: `52`.
- Próxima versão sequencial: `0.1.0-alpha.53`, `versionCode 53`.
- Toda mudança sincroniza Gradle, UI Sobre, User-Agent, GitHub Actions, README, CHANGELOG, RELEASE_NOTES, docs/VALIDATION e testes de publicação.
- Publicar **três APKs** por ABI na mesma GitHub Release, `armeabi-v7a`, `arm64-v8a` e `x86_64`, com versionCode 52 idêntico nos três. x86 32 bits não tem bibliotecas GeckoView 158. Não criar APK universal e não enviar ZIP em Artifacts.
- Em atualizações futuras de versão manter códigos sequenciais para instalação direta; se publicar na Play Store reavaliar política de versionCode por ABI ou preferir AAB.
- Os 4 Secrets de assinatura são confidenciais e não devem ser versionados.
