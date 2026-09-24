# Arquitetura e regras financeiras

## Fronteira de confiança

O FinFlow separa recomendação de autorização:

```text
dados canônicos -> consolidação -> motor determinístico -> intenção -> aprovação humana
                                                              |
                                                              +-> sem executor bancário no MVP
```

Uma futura camada de IA poderá explicar cenários e sugerir intenções, sem acesso direto às APIs bancárias. A chamada a um conector regulado ficará a cargo de um orquestrador determinístico, depois de validar consentimento, política, limite, risco, idempotência e auditoria.

## Arquitetura hexagonal

O backend continua sendo um monólito modular, com dois módulos Gradle e dependências apontando para o núcleo:

```text
HTTP / Spring Security
        |
        v
portas de entrada -> casos de uso -> domínio
                         |
                         v
                  portas de saída
                         ^
                         |
                adaptadores JPA / segurança
```

- `core/src/main/kotlin/com/finflow/<funcionalidade>/domain`: modelos imutáveis, enums e regras de domínio. Depende apenas de Kotlin/JDK e outros tipos de domínio.
- `core/.../<funcionalidade>/application`: casos de uso Kotlin comuns, sem anotações Spring, JPA ou Jakarta Validation.
- `core/.../application/model`: comandos e resultados independentes de HTTP. Os comandos validam seus próprios limites, inclusive quando chamados sem um controller.
- `core/.../application/port/inbound`: interfaces consumidas pelos controllers e pelos casos de uso de outros módulos.
- `core/.../application/port/outbound`: contratos de persistência, identidade autenticada, hash de senha e autenticação de credenciais.
- `src/main/kotlin/com/finflow/<funcionalidade>/adapter/inbound`: controllers, DTOs HTTP com validação Jakarta e filtros JWT.
- `src/.../adapter/outbound`: entidades e repositórios Spring Data, mapeadores explícitos e implementações de segurança.
- `src/.../composition`: configuração Spring e composição dos casos de uso com suas dependências.

O projeto principal depende de `:core`. O módulo `:core` não tem Spring, Hibernate, Jakarta, Jackson ou JWT em seu classpath de produção; um import desses frameworks no núcleo quebra a compilação. Os testes de arquitetura também impedem dependências de domínio para aplicação e de controllers para implementações de serviços ou persistência.

As entidades JPA são distintas dos modelos de domínio. Os adaptadores convertem os dois sentidos, inclusive relações de conta/transação e plano/intenção. A codificação de escopos, avisos e alocações em colunas textuais pertence à persistência. Os nomes de tabelas, colunas e migrations existentes foram mantidos.

### Transações e composição

`UseCaseConfiguration` instancia os serviços puros e expõe as portas de entrada envolvidas por `TransactionTemplate`, com propagação `REQUIRED`. As chamadas entre casos de uso compartilham a mesma transação: conta e auditoria, importação e idempotência, plano e intenções, perfil e conclusão do onboarding. Nenhum controller instancia serviços diretamente.

O bloqueio pessimista de onboarding está em `UserRepository.lockById`, implementado pelo adaptador JPA. A identidade vem de `CurrentUser`, cuja implementação consulta o contexto autenticado do Spring Security; o núcleo não lê esse contexto nem recebe um `userId` informado pelo cliente.

`Clock` é injetado nos casos de uso. Como é uma abstração do JDK, pode ser substituído por `Clock.fixed` nos testes sem criar uma interface duplicada.

### Testes e manutenção

```powershell
.\gradlew.bat :core:test  # domínio e casos de uso com portas em memória, sem Spring
.\gradlew.bat test        # inclui arquitetura, JPA/H2, HTTP/JWT e rollback
.\gradlew.bat build       # inclui testes e empacotamento do backend
```

Os testes HTTP percorrem registro/login, onboarding, contas, importação idempotente, compromissos, metas, carteira, consentimentos, planejamento, aprovação e relatório. Também verificam isolamento entre usuários. Testes de rollback provocam falha na auditoria para verificar a atomicidade de conta e onboarding.

Para adicionar uma funcionalidade: defina os comandos/resultados e portas no núcleo, implemente o caso de uso com dependências de construtor, implemente os adaptadores e registre a ligação em `UseCaseConfiguration`. Regras financeiras ficam no domínio ou no caso de uso, e formatos HTTP/JPA ficam nos adaptadores.

## Módulos funcionais

Cada pacote em `com.finflow` representa um módulo funcional:

- `profile`: parâmetros do planejamento;
- `account` e `transaction`: dados canônicos consolidados;
- `obligation`, `debt` e `goal`: compromissos e objetivos;
- `portfolio`: posições, alvos e distribuição de novos aportes;
- `planning`: cálculo puro, persistência do plano e intenções;
- `openfinance`: registro local de consentimento e status do provedor;
- `report`: fechamento mensal;
- `authentication` e `onboarding`: autenticação e configuração inicial;
- `audit` e `shared`: auditoria, identidade, valores monetários e erros.

Todos os módulos são implantados juntos. Um módulo só deve virar serviço separado quando volume, organização da equipe ou exigência regulatória trouxerem uma necessidade concreta.

## Cálculo do plano

Para a data de referência, o motor:

1. calcula o próximo recebimento;
2. protege obrigações vencendo até essa data, orçamento variável restante e caixa mínimo;
3. informa o déficit se o saldo operacional não cobrir a proteção;
4. destina o excedente à dívida de alto custo;
5. recompõe a reserva até `despesas essenciais × meses-alvo`, limitada pela taxa mensal;
6. libera aporte somente após quitar a dívida cara e completar a reserva;
7. calcula `saldo livre real ÷ dias até a renda`, arredondando o limite diário para baixo.

O plano nunca usa saldo de contas marcadas como `EMERGENCY_RESERVE`, `GOAL` ou `INVESTMENT` como caixa operacional. Contas em moedas diferentes da moeda do perfil são excluídas e geram aviso.

## Idempotência e auditoria

`POST /api/v1/transactions/imports` exige `Idempotency-Key`. Repetir a chave com o mesmo conteúdo devolve o resultado anterior; repetir com outro conteúdo retorna conflito. `accountId + externalId` também impede duplicação de uma transação.

Mudanças relevantes registram eventos em `audit_events`. Payloads bancários, chaves e segredos não são gravados no log de auditoria.

## Contrato de erros

A API responde com `application/problem+json`. Erros de validação e leitura retornam mensagens próprias para o cliente, sem nomes de classes ou detalhes internos. Violações de unicidade são tratadas como conflito, inclusive quando duas requisições concorrentes passam pela verificação inicial. Falhas inesperadas são registradas no servidor e recebem uma resposta genérica.

## Evolução para Open Finance real

O próximo incremento deve introduzir uma porta de provedor e adaptadores separados para:

- criação/renovação/revogação do consentimento;
- callback OAuth/OIDC com proteção de estado e PKCE quando aplicável;
- sincronização paginada de contas, cartões, saldos e transações;
- webhooks e jobs resilientes com retry/backoff;
- criptografia de tokens, rotação de segredos e trilha de acesso;
- Outbox para eventos e comandos de execução;
- reconciliação e compensação de falhas parciais.

Nenhum endpoint atual deve ser interpretado como integração oficial com uma instituição financeira.
