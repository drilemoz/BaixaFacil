# Baixa Fácil 0.1.0-alpha.37 (versionCode 37)

## Desempenho e estabilidade

- Pause/resume correto do navegador: interrompe tarefas automáticas e temporizadores JavaScript no background, retomando ao abrir a aba novamente.
- Limite de frequência das varreduras automáticas de vídeos. Menos trabalho em feeds rápidos e sites pesados.
- Quando o Android indica pressão de memória, o navegador suspende tarefas opcionais e descarta previews temporárias, sem apagar cookies, downloads nem histórico.
- Diagnóstico de memória sob demanda, com RAM disponível do Android, memória Java do aplicativo, PSS somente do processo Baixa Fácil, estado da WebView e atalho para navegador externo.

## Restrições e validação

- Não há encerramento automático da WebView, limpeza automática de cache ou promessa de memória livre exata do renderizador.
- Build Android completo no GitHub Actions e teste de desempenho no aparelho continuam pendentes.
