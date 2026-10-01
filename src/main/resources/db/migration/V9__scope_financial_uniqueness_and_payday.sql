-- Business identities belong to an authenticated owner. Keep ownerless legacy
-- rows intact, and allow different users to use the same bank/asset identifiers.
ALTER TABLE financial_accounts DROP CONSTRAINT IF EXISTS uk_account_institution_external;
ALTER TABLE financial_accounts ADD CONSTRAINT uk_account_user_institution_external UNIQUE (user_id, institution, external_id);
ALTER TABLE portfolio_positions DROP CONSTRAINT IF EXISTS uk_portfolio_asset_code;
ALTER TABLE portfolio_positions ADD CONSTRAINT uk_portfolio_user_asset_code UNIQUE (user_id, asset_code);
ALTER TABLE allocation_targets DROP CONSTRAINT IF EXISTS uk_allocation_target_class;
ALTER TABLE allocation_targets ADD CONSTRAINT uk_allocation_user_class UNIQUE (user_id, asset_class);
ALTER TABLE open_finance_consents DROP CONSTRAINT IF EXISTS uk_consent_provider_external;
ALTER TABLE open_finance_consents ADD CONSTRAINT uk_consent_user_provider_external UNIQUE (user_id, provider, external_consent_id);

-- PostgreSQL's generated name for the original V1 inline CHECK constraint.
ALTER TABLE financial_profiles DROP CONSTRAINT IF EXISTS financial_profiles_pay_day_check;
ALTER TABLE financial_profiles ADD CONSTRAINT ck_profile_pay_day CHECK (pay_day BETWEEN 1 AND 31);

CREATE INDEX IF NOT EXISTS idx_accounts_user ON financial_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_profiles_user ON financial_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_obligations_user ON obligations(user_id);
CREATE INDEX IF NOT EXISTS idx_debts_user ON debts(user_id);
CREATE INDEX IF NOT EXISTS idx_goals_user ON financial_goals(user_id);
CREATE INDEX IF NOT EXISTS idx_consents_user ON open_finance_consents(user_id);
CREATE INDEX IF NOT EXISTS idx_plans_user ON financial_plans(user_id);
