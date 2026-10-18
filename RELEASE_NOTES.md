# Baixa Fácil 0.1.0-alpha.44 (versionCode 44)

## Correção da navegação Downloads ↔ navegador

- Corrigido o botão **Home/Início** da barra inferior, que antes ignorava toques enquanto Downloads estava aberta.
- Agora, em Downloads, **Navegador** (ícone de casa), **Voltar** e o botão **Downloads** devolvem o usuário ao navegador sem recarregar a página nem encerrar transferências.
- Dentro do navegador, o botão **Início** continua abrindo a página inicial configurada e as setas continuam controlando o histórico.
- Ícones inferiores receberam rótulos visíveis, estado selecionado e desabilitação explícita de ações inválidas na tela Downloads.
- Acrescentado botão **Voltar ao navegador** no cabeçalho da central de downloads. Ao sair da lista, o teclado de pesquisa é recolhido sem apagar o texto ou o histórico.
- A tecla Voltar do Android respeita a seleção múltipla: primeiro fecha a seleção e, depois, retorna ao navegador.

## Segurança e validação

- Nova política Java pura `MainNavigationPolicy`, com testes JVM de regressão dos comandos Home, Voltar, alternância de abas e seleção múltipla.
- Mantidos WebView, Custom Tabs, cookies, downloads e arquivos sem alteração no motor HTTP.
- **Pendente:** compilação `:app:testDebugUnitTest :app:assembleDebug` no GitHub Actions e teste visual no Android real. Este ZIP contém código-fonte, não APK.
