# FinFlow Backend

API Kotlin para planejamento financeiro pessoal. Ela reúne contas e compromissos, calcula o saldo que pode ser usado, define um limite diário e recomenda a próxima ação nesta ordem: obrigações, dívida cara, reserva de emergência e investimentos.

O MVP registra consentimentos e recebe dados financeiros em um formato único. Ainda não há conexão direta com bancos nem movimentação de dinheiro. Aprovar uma ação muda apenas o estado da intenção; `executionAvailable` permanece `false`.

## O que já funciona

- perfil financeiro, orçamento, dia do salário, reserva-alvo, perfil de risco e modo `OBSERVER`, `COPILOT` ou `AUTOPILOT`;
- contas consolidadas por finalidade: operação, reserva, metas e investimentos;
- importação idempotente de transações, com categorização inicial baseada em regras;
- obrigações, cartões/faturas, dívidas e metas;
- carteira e faixas-alvo de alocação, direcionando novos aportes sem vender posições;
- plano financeiro com déficit projetado, saldo livre real, limite diário e intenções auditáveis;
- relatório mensal com fluxo de caixa, taxa de poupança e prioridades;
- registro de consentimentos Open Finance e consulta do estado da integração;
- autenticação por API key, validação, erros RFC 9457 (`application/problem+json`), auditoria e migrations Flyway.

## Stack

- Java 17, Kotlin 2.3.21 e Spring Boot 4.1.0
- PostgreSQL 17, Spring Data JPA e Flyway
- Gradle Wrapper 9.5.1
- JUnit 5 e H2 em modo PostgreSQL nos testes

## Executar localmente

Pré-requisitos: Java 17+ e Docker.

```powershell
docker compose up -d
$env:FINFLOW_API_KEY = "troque-por-um-segredo-forte"
.\gradlew.bat bootRun
```

A API ficará disponível em `http://localhost:8080`. Envie `X-API-Key` em todas as rotas de negócio. Apenas `/actuator/health` e `/actuator/info` são públicas.

Para testar e gerar o artefato:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

As variáveis disponíveis estão em `.env.example`. O Compose sobe somente o PostgreSQL; a aplicação pode rodar pelo Gradle ou pela imagem criada com o `Dockerfile`.

## Fluxo mínimo

1. Configure `PUT /api/v1/profile`.
2. Cadastre ou sincronize contas em `/api/v1/accounts`.
3. Registre obrigações e dívidas em `/api/v1/obligations` e `/api/v1/debts`.
4. Importe transações por `POST /api/v1/transactions/imports`, sempre com `Idempotency-Key`.
5. Opcionalmente configure carteira, metas e consentimentos.
6. Gere `POST /api/v1/plans` e consulte `GET /api/v1/plans/latest`.
7. Feche o mês por `GET /api/v1/reports/monthly?year=2026&month=7`.

Exemplo de perfil:

```powershell
$headers = @{ "X-API-Key" = "troque-por-um-segredo-forte" }
$body = @{
  monthlyIncome = @{ amount = 7600.00; currency = "BRL" }
  payDay = 5
  essentialMonthlyExpenses = @{ amount = 3500.00; currency = "BRL" }
  variableMonthlyBudget = @{ amount = 1200.00; currency = "BRL" }
  minimumCashBuffer = @{ amount = 500.00; currency = "BRL" }
  emergencyTargetMonths = 6
  reserveContributionRate = 0.10
  investmentContributionRate = 0.10
  riskProfile = "MODERATE"
  autopilotMode = "COPILOT"
} | ConvertTo-Json -Depth 4
Invoke-RestMethod -Method Put -Uri http://localhost:8080/api/v1/profile `
  -Headers $headers -ContentType application/json -Body $body
```

## Endpoints

| Módulo | Operações |
|---|---|
| Perfil | `PUT/GET /api/v1/profile` |
| Contas | `POST/GET /api/v1/accounts`, `PATCH /api/v1/accounts/{id}/balance` |
| Transações | `POST /api/v1/transactions/imports`, `GET /api/v1/transactions` |
| Obrigações | `POST/GET /api/v1/obligations`, `PATCH /api/v1/obligations/{id}/paid` |
| Dívidas | `POST/GET /api/v1/debts`, `PATCH /api/v1/debts/{id}/paid` |
| Metas | `POST/GET /api/v1/goals`, `PATCH /api/v1/goals/{id}/progress` |
| Carteira | `PUT/GET /api/v1/portfolio` |
| Open Finance | `PUT/GET /api/v1/open-finance/consents`, `GET /api/v1/open-finance/status` |
| Plano | `POST /api/v1/plans`, `GET /api/v1/plans/latest`, revisão de ações em `/api/v1/plans/actions/{id}` |
| Relatório | `GET /api/v1/reports/monthly?year={ano}&month={mês}` |

As regras de cálculo e os limites de segurança estão em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Limites do MVP

- `OPEN_FINANCE_PROVIDER=disabled` é o padrão. Uma instituição participante ou agregador autorizado ainda precisa implementar a troca de consentimento e a sincronização real.
- O modo `AUTOPILOT` não concede permissões adicionais. Uma futura execução bancária ainda terá de validar política, limites, consentimento, idempotência e auditoria.
- A API é de usuário único nesta fase. Antes de exposição pública, substitua a API key por autenticação forte, isolamento por usuário e gestão de segredos.
- As recomendações são regras de planejamento, não garantia de rentabilidade nem oferta de produto financeiro.
