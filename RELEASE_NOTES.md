# Baixa Fácil 0.1.0-alpha.29 (versionCode 29)

## Correção do Gerar APK 4

- Corrigida incompatibilidade de SDK que impedia a compilação com `androidx.media3` 1.11.1.
- Android API de compilação atualizada para **36** e Android Gradle Plugin para **8.10.1**.
- O GitHub Actions instala a plataforma Android 36 antes de executar a compilação.
- Adicionadas verificações preventivas da compatibilidade entre Gradle, Media3 e SDK.

## O que foi preservado

- Todas as melhorias das alpha.26 a alpha.28: AndroidX WebKit, reprodução Media3 e miniaturas/metadados de mídia.
- `minSdk 29` e `targetSdk 34` sem mudanças, mesmo identificador `br.com.baixafacil`.
- Navegador, reconhecimento de vídeos, lista de downloads e motor de transferências anteriores.

## Validação

- Pré-validação e simulação de publicação em ambiente local; build Android completo pendente de execução no GitHub Actions.
