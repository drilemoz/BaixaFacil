# Baixa Fácil 0.1.0-alpha.40 (versionCode 40)

## Novidades

- Na aba **Meus downloads**, ações rápidas **Abrir** e **Enviar** ganharam ícone + texto e foram separadas visualmente.
- Correção de `catch` incompatível introduzido na alpha.39, com validação de URI `content://` e tratamento de falhas ao abrir ou compartilhar.
- Links exatamente iguais já ativos ou pausados não são enfileirados de novo. Se o download estiver concluído, um menu permite baixar novamente somente por escolha explícita.
- O diagnóstico local opcional agora registra erros de páginas principais (sem URL), eventos do renderizador WebView e falhas relevantes de arquivo.

## Observações

- Comparação de URL exata não equivale a comparação de conteúdo ou checksum; links assinados diferentes continuam sendo tratados como endereços distintos.
- Nenhuma sessão de site, cookie ou token de autenticação é armazenado nos eventos de diagnóstico.
- O ZIP contém o código-fonte. O build Android completo e os testes no aparelho ainda precisam ser confirmados via GitHub Actions.
