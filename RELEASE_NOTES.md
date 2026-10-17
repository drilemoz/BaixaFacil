# Baixa Fácil 0.1.0-alpha.25 (versionCode 25)

## Melhoria de compatibilidade de login

- Janelas adicionais de autenticação abertas por toque passam a ter uma WebView secundária com os mesmos cookies da sessão interna (quando permitidos), botão de abrir no navegador externo e fechamento seguro.
- Novo item **⋮ > Diagnóstico de login** com versão da WebView, configurações e erros HTTP/rede, sem coletar senhas ou tokens.
- A partir do diagnóstico é possível abrir a atualização do componente WebView instalado.
- Cookies já aceitos são sincronizados com o armazenamento da WebView ao pausar o navegador; URLs de login e tokens ficam fora do histórico recente.
- `about:blank` e `about:srcdoc` em subframes temporários são aceitos; continua proibido acesso a `file:`, `content:`, esquemas inseguros e conteúdo misto.

## Limites

- A política do Google continua proibindo OAuth dentro de WebViews: usar navegador externo. Login externo não transfere automaticamente a sessão para o Baixa Fácil.
- O Facebook pode rejeitar CAPTCHA ou desafios mesmo após estas melhorias; a validação real depende do serviço e da versão da WebView instalada.
- Sem modificação no motor de download HTTP/UIDT, na fila ou nos dados dos downloads.

## Testes pendentes

- Compilação Android completa pelo GitHub Actions.
- Teste físico de login e abertura de janelas, tanto em tema claro quanto escuro.
