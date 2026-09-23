-- Registros anteriores à autenticação permanecem sem proprietário até uma
-- migração explícita dos dados. Não atribuímos dados antigos a qualquer usuário.
ALTER TABLE financial_profiles ADD COLUMN IF NOT EXISTS user_id integer;
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS user_id integer;
ALTER TABLE obligations ADD COLUMN IF NOT EXISTS user_id integer;
ALTER TABLE debts ADD COLUMN IF NOT EXISTS user_id integer;
ALTER TABLE financial_goals ADD COLUMN IF NOT EXISTS user_id integer;
ALTER TABLE open_finance_consents ADD COLUMN IF NOT EXISTS user_id integer;
ALTER TABLE idempotency_records ADD COLUMN IF NOT EXISTS user_id integer;
ALTER TABLE financial_plans ADD COLUMN IF NOT EXISTS user_id integer;
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS user_id integer;

ALTER TABLE users ADD COLUMN IF NOT EXISTS onboarding_completed boolean NOT NULL DEFAULT FALSE;
