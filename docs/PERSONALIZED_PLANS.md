# Planos personalizados com OpenAI

O backend pode gerar propostas com a Responses API e saída estruturada. O agente usa instruções versionadas e o contexto de cada cliente; não há fine-tuning ou treinamento de um modelo próprio. Nenhuma sugestão executa movimentações financeiras.

## Configuração

Defina estas variáveis no processo da aplicação ou no gerenciador de segredos:

```dotenv
PLANNING_AI_ENABLED=true
OPENAI_API_KEY=<chave-do-projeto-openai>
OPENAI_MODEL=<modelo-disponivel-com-responses-e-structured-outputs>
PLANNING_AI_TIMEOUT_SECONDS=60
```

`.env.example` documenta as variáveis; um arquivo `.env` sozinho não é carregado pelo Spring. A chave fica exclusivamente no servidor. O modelo é configurável, sem um nome fixo no código. O timeout aceita 1 a 180 segundos. A migration Flyway V5 adiciona os detalhes, snapshots financeiros e histórico de revisões.

Com IA habilitada, `POST /api/v1/plans?asOf=2026-09-24` gera uma proposta personalizada. Com IA desabilitada, essa rota mantém o cálculo por regras, identificado por `details.source=RULE_BASED`. A rota explícita `/personalized` sempre exige IA configurada. Falhas da IA não acionam fallback silencioso.

## Gerar e consultar

Todas as rotas exigem o JWT do cliente. O dono dos dados vem da autenticação, nunca do corpo da requisição.

```http
POST /api/v1/plans/personalized
Content-Type: application/json
Authorization: Bearer <token>

{
  "asOf": "2026-09-24",
  "preferences": "Quero priorizar a reserva e reduzir lazer sem cortar educação."
}
```

`preferences` é opcional, aceita até 2000 caracteres e é enviado à OpenAI. Evite incluir identificadores pessoais nesse texto. `asOf` é obrigatório nesta rota. O resultado mantém os campos financeiros anteriores e acrescenta:

| Campo | Uso |
|---|---|
| `details.source` | `AI`, `MANUAL` ou `RULE_BASED` |
| `details.model`, `details.promptVersion` | Origem da geração por IA; preservados após edição manual |
| `details.summary`, `details.analysis` | Resumo e justificativa da proposta |
| `details.categoryBudgets` | Orçamento por categoria com justificativa |
| `content` | Todos os campos editáveis da proposta |
| `revision` | Versão usada para evitar que uma edição sobrescreva outra |
| `updatedAt` | Momento da última edição nos planos personalizados |

Use `GET /api/v1/plans/{id}` para consultar um plano e `GET /api/v1/plans/latest` para o mais recentemente gerado. Uma edição manual mantém `generatedAt`; não transforma um plano antigo no último plano gerado.

## Editar integralmente

`PUT /api/v1/plans/{id}/content` recebe `expectedRevision` e o objeto `content` completo. É possível alterar datas, resumo, análise, meta da reserva, orçamento variável, caixa mínimo, pagamentos de dívida, aportes, limite diário, categorias, alocações, ações e avisos. Listas vazias removem os itens. Não há chamada à OpenAI na edição.

Exemplo em PowerShell usando o contrato retornado pela API:

```powershell
$headers = @{ Authorization = 'Bearer <token>' }
$plan = Invoke-RestMethod -Headers $headers -Uri "http://localhost:8080/api/v1/plans/<id>"
$plan.content.summary = 'Plano ajustado por mim'
$plan.content.analysis = 'Priorizar a reserva neste período e revisar os gastos no próximo mês.'
$body = @{ expectedRevision = $plan.revision; content = $plan.content } | ConvertTo-Json -Depth 20
Invoke-RestMethod -Method Put -Headers $headers -ContentType application/json `
  -Uri "http://localhost:8080/api/v1/plans/$($plan.id)/content" -Body $body
```

Os valores monetários dentro de `content` são números, como `200.00`, na moeda do plano. Ao alterar valores, ajuste também as ações e alocações correspondentes. Os campos obrigatórios de `content` são:

```json
{
  "asOf": "2026-09-24",
  "nextIncomeDate": "2026-10-05",
  "summary": "Meu plano",
  "analysis": "Explicação das escolhas para o período.",
  "emergencyReserveTarget": 21000.00,
  "remainingVariableBudget": 1200.00,
  "minimumCashBuffer": 500.00,
  "debtPaymentRecommendation": 0.00,
  "reserveContribution": 100.00,
  "investmentContribution": 0.00,
  "dailySpendingLimit": 0.00,
  "categoryBudgets": [{ "category": "FOOD", "amount": 600.00, "reason": "Alimentação no período" }],
  "allocations": [],
  "actions": [{ "type": "TRANSFER_TO_EMERGENCY_RESERVE", "amount": 100.00, "riskLevel": "LOW", "rationale": "Avançar na reserva" }],
  "warnings": []
}
```

Esse exemplo só será aceito se os dados reais do cliente comportarem os valores. O backend recalcula os saldos e compromissos na data escolhida, incluindo obrigações pendentes atrasadas. Saldos observados, transações, moeda e identificadores continuam sendo alterados em seus recursos de origem.

Cada edição incrementa `revision`, muda a origem para `MANUAL`, arquiva o conteúdo anterior e recria as ações como `PROPOSED`, com novos IDs e sem aprovações antigas. Isso também permite editar planos já revisados. Consulte versões anteriores com `GET /api/v1/plans/{id}/revisions`, em ordem decrescente. O plano atual fica na consulta por ID. Histórico, ações, plano e auditoria são gravados na mesma transação.

O PUT antigo `/{id}` com apenas `asOf` continua disponível para planos por regras e também arquiva a versão anterior. Para planos de IA ou editados manualmente, ele retorna `422 PERSONALIZED_PLAN_RECALCULATION`; use `/content` para manter e editar a proposta ou `/personalized` para pedir uma nova análise.

## Contexto, validações e falhas

- Contexto: perfil, saldos, gastos por mês e categoria nos últimos 90 dias, fluxo de caixa, dívidas ativas, metas, compromissos, classes da carteira e situação do consentimento. Transferências não entram nos totais de consumo; moedas diferentes são excluídas e contabilizadas. Meses parciais e ausência de histórico são indicados no contexto.
- Privacidade: o contexto financeiro não inclui nomes, e-mails, IDs de contas, nomes das metas, estabelecimentos ou descrições das transações. O texto livre de preferências é enviado como informado. A requisição usa `store=false`; isso não equivale a afirmar retenção zero pelo provedor.
- Consistência: valores não negativos com até duas casas decimais; aportes e pagamentos não podem exceder o caixa após compromissos, orçamento e buffer. Pagamentos não podem superar a dívida ativa. As alocações, se presentes, somam o aporte. O limite diário cabe no saldo livre do período. Categorias e classes não podem se repetir. `CUSTOM` permite orientações textuais com valor zero.
- Aprovação: ações da IA e das edições sempre exigem revisão. A aprovação registra uma decisão e não executa transações bancárias.
- Concorrência: a IA é chamada fora da transação do banco. Antes de salvar, o contexto é relido; mudanças detectadas retornam `409 PLANNING_DATA_CHANGED`. Uma revisão desatualizada retorna `409 PLAN_REVISION_CONFLICT`.
- Falhas: configuração ausente, timeout, recusa, resposta incompleta ou proposta inválida retornam `503`, sem salvar plano parcial. Conteúdo inválido na edição retorna `400`. Recursos inexistentes ou de outro usuário retornam `404`.

O teste do adaptador usa um servidor HTTP local; os testes de integração simulam a proposta da IA. Uma verificação real requer chave e modelo disponíveis no ambiente.

Referências oficiais: [Responses API](https://developers.openai.com/api/docs/guides/migrate-to-responses) e [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).

## Diagnóstico da integração

### Respostas objetivas e escopo financeiro

O prompt `finflow-personal-planner-v2` gera resumo de até 350 caracteres,
análise com 3 a 5 tópicos (`- `, até 180 caracteres por linha), até 5 ações,
8 categorias e 3 avisos essenciais. As justificativas de categorias têm até 120
caracteres; as de ações e os avisos, até 160. O contrato HTTP continua usando
`analysis` como string; planos existentes e edições manuais continuam compatíveis.

Observações e textos do contexto são dados não confiáveis. Pedidos de scripts,
mudança de papel, exposição de instruções e outros assuntos devem ser ignorados;
preferências financeiras legítimas continuam sendo consideradas. O adaptador
valida os limites e tópicos, além de rejeitar padrões de código, HTML e links
em todos os campos textuais antes de devolver uma proposta para persistência.
Essas violações retornam `AI_PLAN_INVALID` ou `AI_PLAN_OUT_OF_SCOPE`, sem salvar
um plano parcial e sem devolver o conteúdo rejeitado.

As verificações locais de código são uma defesa complementar, não um classificador
semântico universal. Prompt e schema reduzem desvios, mas não garantem imunidade
a toda injeção. Os testes com provedor simulado cobrem isolamento das instruções,
rejeição de código e limites; avaliações com o modelo real continuam necessárias.
Referência: [segurança de agentes](https://developers.openai.com/api/docs/guides/agent-builder-safety).

O backend mantém HTTP 503 para falhas dessa dependência, mas retorna códigos específicos:

| Código | Verificação necessária |
|---|---|
| `AI_AUTHENTICATION_FAILED` | Chave inválida, expirada ou revogada no processo do backend |
| `AI_MODEL_UNAVAILABLE` | Nome do modelo e acesso do projeto |
| `AI_ACCESS_DENIED` | Permissões da chave/projeto |
| `AI_QUOTA_EXCEEDED` | Créditos ou limites de uso da API; repetir a chamada não resolve |
| `AI_RATE_LIMITED` | Limite temporário de chamadas; aguarde |
| `AI_REQUEST_REJECTED` | Compatibilidade do modelo com Responses/Structured Outputs e contrato enviado |
| `AI_TIMEOUT` | Tempo de resposta do provedor |
| `AI_CONNECTION_ERROR` | Rede, DNS, proxy ou certificados do servidor |

Quando a OpenAI responde, `providerStatus` informa o status HTTP original e `providerRequestId`, quando disponível, permite correlacionar a requisição no provedor. Os logs `event=planning.ai.failed` registram esses mesmos metadados, sem a chave, o contexto financeiro ou mensagens brutas da OpenAI. O frontend apresenta o detalhe específico devolvido pelo backend.

Espaços externos e aspas externas pareadas nos valores de chave/modelo são normalizados pelo adaptador. Alterações nas variáveis exigem reiniciar o backend. Não há fallback automático que disfarce falhas de configuração ou cobrança.
