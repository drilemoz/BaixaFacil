# Baixa Fácil 0.1.0-alpha.41 (versionCode 41)

## Novidades

- **Custom Tabs (AndroidX Browser 1.10.0)**: abrir a página no navegador instalado, pelo menu do navegador. Fluxos Google OAuth oferecem navegador compatível e usam Custom Tabs, se habilitado em Configurações; fallback para navegador externo.
- **Limpar downloads**: novo botão de lixeira em Meus downloads com opções para limpar registros finalizados sem apagar os arquivos ou apagar também arquivos concluídos do armazenamento MediaStore quando houver permissão.
- Downloads na fila, ativos, pausados ou com fragmentos de retomada continuam protegidos. Falhas de exclusão preservam o registro para nova tentativa.
- Novos testes JVM de política de limpeza e verificações preventivas de integração.

## Limitações

- Custom Tabs utiliza a sessão do navegador externo e não fornece cookies, vídeos ou downloads ao motor do Baixa Fácil. Não garante autorização Google para todas as páginas.
- A compilação Android completa e a validação de UI/MediaStore em dispositivo ainda dependem do GitHub Actions e teste físico. Este ZIP contém **código-fonte**, não APK compilado.
