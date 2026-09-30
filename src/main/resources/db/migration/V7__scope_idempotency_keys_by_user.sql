-- V1 made idempotency keys global. Replays must be isolated by the
-- authenticated owner, otherwise two users can conflict on the same key.
ALTER TABLE idempotency_records DROP CONSTRAINT IF EXISTS uk_idempotency_operation_key;

ALTER TABLE idempotency_records
    ADD CONSTRAINT uk_idempotency_user_operation_key UNIQUE (user_id, operation, key_hash);
