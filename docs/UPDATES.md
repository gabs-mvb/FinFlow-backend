# Atualização por PUT

Os endpoints abaixo exigem `Authorization: Bearer <token>` e retornam `200 OK` com o recurso atualizado. O ID é mantido. Registros inexistentes ou de outro usuário retornam `404`; o PUT não cria um recurso quando o ID não existe. Todos os campos obrigatórios devem ser enviados, mesmo quando apenas um valor muda.

## Contas

`PUT /api/v1/accounts/{id}`

```json
{
  "institution": "Meu Banco",
  "externalId": "conta-123",
  "name": "Conta principal",
  "accountType": "CHECKING",
  "purpose": "OPERATING",
  "availableBalance": { "amount": 4500.00, "currency": "BRL" },
  "lastSyncedAt": "2026-09-23T12:00:00Z"
}
```

`lastSyncedAt` é opcional; sua ausência ou `null` remove a data de sincronização. A moeda deve ser igual à moeda atual da conta, preservando a consistência com as transações já importadas. Nome, instituição e identificador externo são normalizados removendo espaços nas extremidades. Uma identidade externa já usada por outra conta do mesmo usuário retorna `409 ACCOUNT_ALREADY_EXISTS`.

## Compromissos

`PUT /api/v1/obligations/{id}`

```json
{
  "name": "Aluguel",
  "type": "HOUSING",
  "amount": { "amount": 1800.00, "currency": "BRL" },
  "dueDate": "2026-10-05",
  "status": "PENDING"
}
```

Todos os campos acima são obrigatórios. O valor deve ser positivo. `status` aceita `PENDING`, `PAID` e `CANCELLED`, permitindo também corrigir o status de um compromisso. A alteração será considerada na próxima geração ou atualização de plano.

## Planos financeiros

Para editar todos os campos da proposta, inclusive planos gerados por IA e planos já revisados, use `PUT /api/v1/plans/{id}/content` conforme [Planos personalizados](PERSONALIZED_PLANS.md). A rota abaixo é o recálculo por data de planos por regras; planos com origem `AI` ou `MANUAL` usam `/content`.

`PUT /api/v1/plans/{id}`

```json
{
  "asOf": "2026-10-01"
}
```

`asOf` é obrigatório. O backend recalcula o plano com o perfil, contas, compromissos, transações, dívidas, carteira e consentimentos atuais do usuário. Mantém o ID do plano e atualiza `generatedAt` para o instante do recálculo. Os valores financeiros derivados são calculados pelo backend.

As intenções de ação também são recalculadas: tipos que continuam no plano mantêm seus IDs; tipos que deixam de ser necessários são removidos; novas recomendações recebem novas intenções. Repetir o PUT com os mesmos dados financeiros não duplica planos ou ações. A data de geração e os eventos de auditoria registram cada recálculo.

Se alguma ação que exige aprovação já foi aprovada ou rejeitada, o PUT retorna `422 PLAN_ALREADY_REVIEWED`. Nesse caso, use `POST /api/v1/plans` para gerar outro plano e preservar as decisões anteriores. Ações aprovadas automaticamente pelo motor não bloqueiam o recálculo.

Recálculo e revisão de ações usam o mesmo bloqueio transacional por usuário, evitando que um recálculo sobrescreva uma aprovação concorrente.

## Validação e atomicidade

Campos inválidos retornam `400` no formato `application/problem+json`. Os endpoints PATCH existentes continuam disponíveis para atualizar saldo, marcar compromisso como pago e revisar intenções. Atualização, reconciliação de ações e auditoria são confirmadas na mesma transação; uma falha desfaz a operação.
