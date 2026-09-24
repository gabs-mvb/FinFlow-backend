alter table financial_plans add column revision integer not null default 0;
alter table financial_plans add column updated_at timestamp with time zone;
alter table financial_plans add column plan_details text;
alter table financial_plans add column total_balance_snapshot numeric(19, 2);
alter table financial_plans add column reserve_balance_snapshot numeric(19, 2);
alter table financial_plans add column reserve_target_snapshot numeric(19, 2);
alter table financial_plans alter column warnings type text;

create table financial_plan_revisions (
    id uuid primary key,
    plan_id uuid not null references financial_plans(id),
    user_id integer not null references users(id),
    revision integer not null,
    captured_at timestamp with time zone not null,
    snapshot text not null,
    constraint uk_plan_revision unique (plan_id, revision)
);
create index ix_plan_revision_owner on financial_plan_revisions(user_id, plan_id, revision);
