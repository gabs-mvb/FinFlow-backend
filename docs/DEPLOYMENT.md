# Publicação automática do backend

O workflow `.github/workflows/deploy.yml` roda a cada commit na `main`, incluindo
merges de PRs. Também aceita execução manual pela aba Actions. Nos PRs para
`main`, roda testes e constrói a imagem sem publicá-la.

Após os testes dos módulos raiz e `core`, publica no GitHub Container Registry:

- `ghcr.io/gabs-mvb/finflow-backend:latest`
- `ghcr.io/gabs-mvb/finflow-backend:sha-<SHA completo do commit>`

O nome acompanha automaticamente o proprietário e o nome do repositório.
Execuções manuais em outras branches apenas validam o build.
Os relatórios de testes ficam disponíveis nos artifacts da execução.

## Configuração no GitHub

GitHub Actions precisa estar habilitado. A publicação usa o `GITHUB_TOKEN` da
execução com permissão `packages: write`; não exige cadastrar um PAT para publicar.
Se o pacote GHCR já existir, conceda ao repositório acesso de escrita nas
configurações do pacote. Para exigir validação antes de merges, configure a
proteção da `main` exigindo os checks `test` e `image`.

## Atualização do servidor

Publicar a imagem não atualiza automaticamente o serviço em execução. A etapa
de deploy depende do destino de hospedagem e ainda não está configurada neste
workflow. O serviço pode consumir a imagem do GHCR ou construir o Dockerfile
diretamente, dependendo do provedor. Pacotes privados exigem credenciais de
leitura no servidor. Prefira a tag do commit para identificar e reverter versões.

Configure as variáveis de banco de dados, autenticação, CORS e OpenAI no ambiente
do serviço. Arquivos `.env` não são incluídos na imagem. O container roda como
usuário sem privilégios e usa a porta 8080 por padrão (configurável com `PORT`).
O Dockerfile requer BuildKit, habilitado pelo workflow, e compila o projeto com
Java 17. Os testes são executados antes da construção/publicação da imagem.
