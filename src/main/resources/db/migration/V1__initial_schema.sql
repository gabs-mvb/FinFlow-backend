create table financial_profiles (
    id uuid primary key,
    currency varchar(3) not null,
    monthly_income numeric(19, 2) not null,
    pay_day integer not null check (pay_day between 1 and 28),
    essential_monthly_expenses numeric(19, 2) not null,
    variable_monthly_budget numeric(19, 2) not null,
    minimum_cash_buffer numeric(19, 2) not null,
    emergency_target_months integer not null check (emergency_target_months between 1 and 24),
    reserve_contribution_rate numeric(7, 6) not null,
    investment_contribution_rate numeric(7, 6) not null,
    risk_profile varchar(32) not null,
    autopilot_mode varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table financial_accounts (
    id uuid primary key,
    institution varchar(120) not null,
    external_id varchar(160) not null,
    name varchar(120) not null,
    account_type varchar(32) not null,
    purpose varchar(32) not null,
    available_balance numeric(19, 2) not null,
    currency varchar(3) not null,
    last_synced_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_account_institution_external unique (institution, external_id)
);

create table financial_transactions (
    id uuid primary key,
    account_id uuid not null references financial_accounts(id),
    external_id varchar(180) not null,
    transaction_type varchar(16) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    description varchar(300) not null,
    merchant varchar(180),
    category varchar(40) not null,
    categorization_source varchar(24) not null,
    occurred_at timestamp with time zone not null,
    imported_at timestamp with time zone not null,
    constraint uk_transaction_account_external unique (account_id, external_id)
);

create index idx_transactions_occurred_at on financial_transactions(occurred_at);
create index idx_transactions_category on financial_transactions(category);

create table obligations (
    id uuid primary key,
    name varchar(160) not null,
    obligation_type varchar(40) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    due_date date not null,
    status varchar(24) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_obligations_due_date_status on obligations(due_date, status);

create table debts (
    id uuid primary key,
    name varchar(160) not null,
    debt_type varchar(40) not null,
    outstanding_amount numeric(19, 2) not null,
    monthly_payment numeric(19, 2) not null,
    currency varchar(3) not null,
    annual_effective_rate numeric(9, 6) not null,
    priority varchar(24) not null,
    status varchar(24) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table financial_goals (
    id uuid primary key,
    name varchar(160) not null,
    target_amount numeric(19, 2) not null,
    current_amount numeric(19, 2) not null,
    currency varchar(3) not null,
    target_date date,
    priority integer not null check (priority between 1 and 5),
    status varchar(24) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table portfolio_positions (
    id uuid primary key,
    asset_code varchar(48) not null,
    asset_name varchar(160) not null,
    asset_class varchar(40) not null,
    current_value numeric(19, 2) not null,
    currency varchar(3) not null,
    updated_at timestamp with time zone not null,
    constraint uk_portfolio_asset_code unique (asset_code)
);

create table allocation_targets (
    id uuid primary key,
    asset_class varchar(40) not null,
    target_percentage numeric(7, 4) not null,
    minimum_percentage numeric(7, 4) not null,
    maximum_percentage numeric(7, 4) not null,
    constraint uk_allocation_target_class unique (asset_class)
);

create table open_finance_consents (
    id uuid primary key,
    provider varchar(80) not null,
    external_consent_id varchar(180) not null,
    institution varchar(120) not null,
    scopes varchar(1000) not null,
    status varchar(40) not null,
    expires_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_consent_provider_external unique (provider, external_consent_id)
);

create table idempotency_records (
    id uuid primary key,
    operation varchar(80) not null,
    key_hash varchar(64) not null,
    request_hash varchar(64) not null,
    imported_count integer not null,
    duplicate_count integer not null,
    created_at timestamp with time zone not null,
    constraint uk_idempotency_operation_key unique (operation, key_hash)
);

create table financial_plans (
    id uuid primary key,
    as_of date not null,
    next_income_date date not null,
    currency varchar(3) not null,
    operating_balance numeric(19, 2) not null,
    committed_obligations numeric(19, 2) not null,
    remaining_variable_budget numeric(19, 2) not null,
    minimum_cash_buffer numeric(19, 2) not null,
    debt_payment_recommendation numeric(19, 2) not null,
    reserve_contribution numeric(19, 2) not null,
    investment_contribution numeric(19, 2) not null,
    free_real_balance numeric(19, 2) not null,
    projected_shortfall numeric(19, 2) not null,
    daily_spending_limit numeric(19, 2) not null,
    warnings varchar(2000) not null,
    allocation_plan varchar(2000) not null,
    generated_at timestamp with time zone not null
);

create table action_intents (
    id uuid primary key,
    plan_id uuid not null references financial_plans(id),
    action_type varchar(48) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    risk_level varchar(16) not null,
    requires_approval boolean not null,
    status varchar(24) not null,
    rationale varchar(500) not null,
    created_at timestamp with time zone not null,
    reviewed_at timestamp with time zone
);

create table audit_events (
    id uuid primary key,
    actor varchar(80) not null,
    action varchar(100) not null,
    resource_type varchar(80) not null,
    resource_id varchar(100),
    details varchar(1000),
    occurred_at timestamp with time zone not null
);

create index idx_audit_events_occurred_at on audit_events(occurred_at);
