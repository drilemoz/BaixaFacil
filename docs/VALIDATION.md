# Validação 0.1.0-alpha.35

## Objetivo

Diminuir o trabalho opcional realizado pela WebView em páginas do ChatGPT e disponibilizar monitoramento seguro de renderizador bloqueado.

## Verificações

- `BrowserLoadPolicyTest`: domínios oficiais exatos, variantes de caixa e caminhos válidos, domínios impostores/URLs maliciosas, opt-out da preferência, outros sites preservados.
- `tools/ci_preflight.py`: versão, workflow, segurança, integração do perfil de desempenho e existência dos testes.
- `tools/test_publish_mock.sh`: teste de publicação simulada.
- Confirmar que `shouldInterceptRequest()` retorna sem trabalho adicional quando o perfil está ativo; não manipular respostas HTTP e não interceptar POST.
- Testar no aparelho abertura de ChatGPT, digitação em conversa longa, navegação, teclados, rolagem e retorno de sessão.
- Testar modo desativado, depois retorno ativado e outros sites com vídeos e bloqueador.
- Testar renderizador responsivo/não responsivo em Android 10+ sem encerrá-lo automaticamente.

## Pendente

- Compilação Android e testes físicos no dispositivo pelo GitHub Actions; não há dados medidos de memória ou FPS.
