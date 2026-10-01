CREATE TABLE user_onboarding (
    user_id integer PRIMARY KEY REFERENCES users(id),
    status varchar(32) NOT NULL DEFAULT 'NOT_STARTED',
    current_step varchar(40) NOT NULL DEFAULT 'WELCOME',
    payload text NOT NULL DEFAULT '{}',
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'))
);

-- Preserve completed legacy journeys only when their own financial context exists.
-- Ownerless legacy rows remain ownerless and are never copied to new users.
INSERT INTO user_onboarding (user_id, status, current_step, completed_at)
SELECT u.id,
    CASE WHEN u.onboarding_completed AND EXISTS (SELECT 1 FROM financial_profiles p WHERE p.user_id = u.id)
        AND EXISTS (SELECT 1 FROM financial_accounts a WHERE a.user_id = u.id) THEN 'COMPLETED' ELSE 'NOT_STARTED' END,
    CASE WHEN u.onboarding_completed AND EXISTS (SELECT 1 FROM financial_profiles p WHERE p.user_id = u.id)
        AND EXISTS (SELECT 1 FROM financial_accounts a WHERE a.user_id = u.id) THEN 'COMPLETED' ELSE 'WELCOME' END,
    CASE WHEN u.onboarding_completed AND EXISTS (SELECT 1 FROM financial_profiles p WHERE p.user_id = u.id)
        AND EXISTS (SELECT 1 FROM financial_accounts a WHERE a.user_id = u.id) THEN CURRENT_TIMESTAMP ELSE NULL END
FROM users u;

UPDATE users SET onboarding_completed = FALSE
WHERE id IN (SELECT user_id FROM user_onboarding WHERE status <> 'COMPLETED');
