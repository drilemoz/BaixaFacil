# Validação 0.1.0-alpha.44

## Bug corrigido

- Alpha.43: `MainActivity.createBottomBar` usava `if(downloadsVisible)return;` para qualquer botão que não fosse o quinto. Assim, Home e Voltar ficavam sem ação na tela Downloads, criando a impressão de navegação bloqueada.
- Alpha.44: `MainNavigationPolicy.bottomAction` define ação explícita para cada botão. Home e Voltar em Downloads retornam ao navegador **sem chamar** `browserPane.home()` ou recriar a WebView; no navegador, Home ainda abre a página inicial.
- A seta Voltar do Android é roteada por `MainNavigationPolicy.systemBack`, preservando a prioridade de encerrar seleção múltipla.
- Rótulos no rodapé e botão **Voltar ao navegador** no cabeçalho facilitam a saída da tela sem depender de gestos.

## Verificações

- `MainNavigationPolicyTest`: testes JVM de comandos do rodapé e botão Voltar em cenários com Downloads, navegador e seleção.
- `python3 tools/ci_preflight.py` e `bash tools/test_publish_mock.sh`.
- Parse Java dos arquivos, validação de XML/YAML e integridade CRC do pacote ZIP.

## Testar no Android real

1. Abra um site no navegador, vá para Downloads e pressione **Navegador**. Verifique que retorna ao mesmo site, sem reiniciar o carregamento.
2. Repita com a seta **Voltar**, com o botão **Downloads** e com a tecla Voltar do sistema.
3. Em Downloads, abra seleção múltipla, pressione Voltar: deve sair da seleção; pressione novamente: deve voltar ao navegador.
4. No navegador, pressione **Início** e confirme que carrega a página inicial das Configurações.
5. Abra a busca na lista com teclado visível, depois volte ao navegador. O teclado não deve cobrir a tela nem perder cookies/histórico.
6. Testar após girar tela/recriar Activity e depois de deixar downloads longos executando.

## Limites

- Não foi executada compilação Android completa nem teste físico no ambiente local. Reexecutar workflow do GitHub Actions e validar comportamento no aparelho.
