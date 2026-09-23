-- Add user_id columns to portfolio_positions and allocation_targets to match JPA entities
ALTER TABLE portfolio_positions ADD COLUMN IF NOT EXISTS user_id integer;
CREATE INDEX IF NOT EXISTS idx_portfolio_user_id ON portfolio_positions(user_id);

ALTER TABLE allocation_targets ADD COLUMN IF NOT EXISTS user_id integer;
CREATE INDEX IF NOT EXISTS idx_allocation_target_user_id ON allocation_targets(user_id);
