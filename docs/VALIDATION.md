# Validação 0.1.0-alpha.29

## Erro diagnosticado

- Arquivo enviado: `BaixaFacil-Gerar-APK-4-logs.zip`.
- Tarefa com falha: `:app:checkDebugAarMetadata`.
- **10** módulos da dependência `androidx.media3` 1.11.1 exigem compile API 36 ou superior.
- Configuração anterior `compileSdk 35` não atende os requisitos declarados nos AARs.

## Correção implementada

- `app/build.gradle`: `compileSdk 36`; manter `minSdk 29` e `targetSdk 34`.
- `build.gradle`: AGP 8.10.1 para suporte oficial a API 36, Gradle 8.11.1 e JDK 17.
- `.github/workflows/android.yml`: instala `platforms;android-36`; mantém Build Tools 35.0.0 para assinatura/verificação.
- `tools/ci_preflight.py`: verificação de versões do SDK, AGP e dependências Media3 para prevenir futuras regressões de AAR metadata.

## Testes

- `python3 tools/ci_preflight.py` e `bash tools/test_publish_mock.sh`.
- Verificação de regressão: preflight deve rejeitar `compileSdk 35` e workflow com plataforma `android-35`.
- Inspeção de sintaxe Java e validação de YAML/XML/ZIP, se executadas, não substituem o build Android.
- O workflow deve executar `:app:testDebugUnitTest :app:assembleDebug` e gerar release após validação de assinatura.

## Pendente

- Build Android/Gradle completo no GitHub Actions após publicar a alpha.29.
- Validação da reprodução Media3 em aparelho físico, incluindo MP4/WebM e HLS/DASH públicos.
