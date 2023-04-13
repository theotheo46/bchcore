create table if not exists last_seen_block
(
    identity_name varchar not null primary key,
    block_number bigint not null
);

create table if not exists id_x_wallet_events
(
    identity_name varchar not null primary key,
    block_number bigint not null,
    events bytea not null
);

create table if not exists id_x_wallet_events_processed
(
    identity_name varchar not null primary key,
    block_number bigint not null,
    events bytea not null
);