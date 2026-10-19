# Checklist de teste real: GeckoView / Alpha.53

Os testes locais confirmam estrutura e regras, **não** substituem o teste do APK instalado.

1. No GitHub Actions, confirmar `:app:testDebugUnitTest`, `:app:assembleDebug` e `:app:assembleRelease` (se houver assinatura de release) e os três APKs por ABI.
2. Confirmar APK selecionado pela arquitetura do Android: `adb shell getprop ro.product.cpu.abilist` (arm64-v8a, armeabi-v7a ou x86_64). Não instalar x86 32 bits.
3. Confirmar a assinatura antes de atualizar a instalação antiga. Certificados diferentes podem impedir atualização preservando dados; **não desinstale** sem backup do histórico.
4. Instalar o APK apropriado, abrir Baixa Fácil, navegar em página HTTPS, buscar na página, voltar/avançar, entrar/sair da aba Downloads, abrir favoritos e configurações.
5. Reproduzir uma mídia sem DRM e baixar um arquivo HTTP(S) público conhecido. Testar pausa/retomada, rede Wi-Fi que cai e bloqueios de rede/bateria com arquivo descartável; conferir conteúdo salvo no MediaStore.
6. Verificar sessão de login apenas quando o site permitir navegador embutido; cookies externos não são importados. Confirmar bloqueio de URL `file:`/`javascript:`/`blob:` para download HTTP.
7. Testar 20+ minutos de navegação e alternância de apps com a tela apagada. Registrar RAM, temperatura e consumo com medições reais; não presumir que GeckoView será mais leve que WebView.
8. Para `onCrash/onKill` do processo de conteúdo Gecko, usar ambiente de teste controlado. `adb shell am force-stop` encerra o aplicativo inteiro e **não** testa esses callbacks. Em falhas reais, confirmar no máximo duas tentativas automáticas em 120 segundos e diálogo de recuperação na terceira falha.
9. Verificar no resumo do GitHub Releases os tamanhos reais dos três APKs e os respectivos SHA-256.
10. Testar com Android 10+ e, se possível, em aparelho com 4 GB de RAM, verificando que DownloadEngine / TransferService não foram interrompidos quando o navegador foi fechado.

Não registrar/capturar publicamente URLs de login, cookies, tokens, histórico privado, senhas ou dados de usuário em relatórios de erro.
