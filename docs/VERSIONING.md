# Versionamento do Baixa Fácil

- Versão atual: `0.1.0-alpha.57`.
- `versionCode`: `57`.
- Próxima versão sequencial: `0.1.0-alpha.58`, `versionCode 58`.
- Toda mudança sincroniza Gradle, UI Sobre, User-Agent, GitHub Actions, README, CHANGELOG, RELEASE_NOTES, docs/VALIDATION e testes de publicação.
- Publicar **três APKs** por ABI na mesma GitHub Release, `armeabi-v7a`, `arm64-v8a` e `x86_64`, com versionCode 57 idêntico nos três. x86 32 bits não tem bibliotecas GeckoView 158. Não criar APK universal e não enviar ZIP em Artifacts.
- Em atualizações futuras de versão manter códigos sequenciais para instalação direta; se publicar na Play Store reavaliar política de versionCode por ABI ou preferir AAB.
- Os 4 Secrets de assinatura são confidenciais e não devem ser versionados.

## Alpha.53: proteção contra ciclos de falha do GeckoView

- No `onCrash`/`onKill`, a sessão Gecko está fechada: não chamar métodos de navegação/ativação nela.
- No máximo duas recriações automáticas em janela monotônica de 120 segundos. A partir da terceira falha, exigir escolha explícita de recuperação, browser externo ou downloads.
- A contagem é mantida localmente sem URL, histórico, tokens ou cookies; a expiração e reinício do relógio monotônico reiniciam a janela.
- O painel de publicação apresenta tamanhos reais por ABI apenas depois da compilação, sem estimar o peso do APK.
