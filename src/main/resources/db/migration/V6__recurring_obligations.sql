alter table obligations add column recurring boolean not null default false;
alter table obligations add column due_day integer;
alter table obligations add constraint chk_obligations_recurrence
    check ((recurring = false and due_day is null) or (recurring = true and due_day between 1 and 31));
