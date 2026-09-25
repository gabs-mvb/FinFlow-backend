# Carteira e saldo no onboarding

A carteira registra somente os ativos atuais. `PUT /api/v1/portfolio` recebe `positions`; `GET /api/v1/portfolio` retorna `positions`. Metas percentuais de alocação (`targets`, percentual desejado, mínimo e máximo) foram removidas do contrato e do cálculo por regras.

```json
{
  "positions": [
    {
      "assetCode": "CDB",
      "assetName": "CDB do banco",
      "assetClass": "FIXED_INCOME",
      "currentValue": { "amount": 1000.00, "currency": "BRL" }
    }
  ]
}
```

`positions: []` permite esvaziar a carteira. A tabela histórica `allocation_targets` permanece no banco, mas não é mais consultada nem gravada pela aplicação; nenhum dado histórico foi apagado. A IA ainda pode sugerir alocações no plano editável, sem exigir percentuais previamente cadastrados. Objetivos financeiros pessoais de `/goals` continuam independentes da carteira.

O saldo disponível vem exclusivamente de `availableBalance` das contas cadastradas pelo cliente, na moeda do perfil. O plano consolida todas essas contas e usa contas `OPERATING` no caixa do dia a dia; contas de reserva são contabilizadas separadamente. O valor dos ativos da carteira não é somado novamente ao saldo em conta.

O primeiro passo do frontend não solicita saldo mínimo. Novos perfis usam `minimumCashBuffer=0`; perfis existentes preservam a margem previamente configurada. Essa margem é um valor protegido, não um saldo observado, e continua editável nas configurações do perfil. A revisão do onboarding exibe a soma das contas, sem um segundo campo para informar o saldo.
