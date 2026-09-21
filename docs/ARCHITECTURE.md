# Arquitetura e regras financeiras

## Fronteira de confiança

O FinFlow separa recomendação de autorização:

```text
dados canônicos -> consolidação -> motor determinístico -> intenção -> aprovação humana
                                                              |
                                                              +-> sem executor bancário no MVP
```

Uma futura camada de IA poderá explicar cenários e sugerir intenções, sem acesso direto às APIs bancárias. A chamada a um conector regulado ficará a cargo de um orquestrador determinístico, depois de validar consentimento, política, limite, risco, idempotência e auditoria.

## Modular monolith

Cada pacote em `com.finflow` representa um módulo funcional:

- `profile`: parâmetros do planejamento;
- `account` e `transaction`: dados canônicos consolidados;
- `obligation`, `debt` e `goal`: compromissos e objetivos;
- `portfolio`: posições, alvos e distribuição de novos aportes;
- `planning`: cálculo puro, persistência do plano e intenções;
- `openfinance`: registro local de consentimento e status do provedor;
- `report`: fechamento mensal;
- `audit`, `shared.security`, `shared.api` e `shared.domain`: capacidades transversais.

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
