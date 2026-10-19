# Versionamento do Baixa Fácil

- Versão atual: `0.1.0-alpha.51`.
- `versionCode`: `51`.
- Próxima versão sequencial: `0.1.0-alpha.52`, `versionCode 52`.
- Toda mudança sincroniza Gradle, UI Sobre, User-Agent, GitHub Actions, README, CHANGELOG, RELEASE_NOTES, docs/VALIDATION e testes de publicação.
- Publicar **quatro APKs** por ABI na mesma GitHub Release, `armeabi-v7a`, `arm64-v8a`, `x86` e `x86_64`, com versionCode 51 idêntico nos quatro. Não criar APK universal e não enviar ZIP em Artifacts.
- Em atualizações futuras de versão manter códigos sequenciais para instalação direta; se publicar na Play Store reavaliar política de versionCode por ABI ou preferir AAB.
- Os 4 Secrets de assinatura são confidenciais e não devem ser versionados.
