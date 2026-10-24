CREATE TABLE operator_accounts (
    id      UUID   PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID   NOT NULL UNIQUE REFERENCES users (id),
    balance BIGINT NOT NULL CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0
);
