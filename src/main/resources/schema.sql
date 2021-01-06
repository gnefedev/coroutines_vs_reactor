CREATE TABLE account
(
    id      serial primary key,
    amount  bigint,
    version int
);

CREATE TABLE transaction
(
    id              serial primary key,
    amount          bigint,
    from_account_id long references account (id),
    to_account_id   long references account (id),
    unique_key      varchar
);

CREATE UNIQUE INDEX transaction_unique_key ON transaction (unique_key);