# Compromissos mensais

`POST /api/v1/obligations` e `PUT /api/v1/obligations/{id}` aceitam dois formatos:

- Data única: `"dueDate":"2027-01-15"` (com `recurring` ausente ou `false`).
- Recorrente mensal: `"recurring":true,"dueDay":31` (sem `dueDate`).

O dia recorrente deve estar entre 1 e 31. O servidor calcula e devolve `dueDate`
como o próximo vencimento. Em meses curtos, usa o último dia do mês e volta ao
dia escolhido no mês seguinte. A resposta também inclui `recurring` e `dueDay`.

`PATCH /api/v1/obligations/{id}/paid` marca um compromisso único como `PAID`.
Para um compromisso recorrente, registra o pagamento da ocorrência atual,
avança `dueDate` um mês e mantém `status=PENDING` para a próxima ocorrência.
O cadastro representa a série; não armazena um histórico separado de parcelas
pagas. A auditoria registra cada ação.

Planos gerados para datas futuras consideram cada vencimento mensal entre a
próxima data pendente e a próxima renda. O contexto da IA também recebe as
ocorrências projetadas. Compromissos únicos já cadastrados seguem com a mesma
semântica de antes.
