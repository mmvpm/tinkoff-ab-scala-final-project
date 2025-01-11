create table orders
(
    id        uuid primary key,
    order_raw jsonb
);

create table order_status
(
    order_id uuid primary key references orders (id),
    status   varchar(16)
);

create table failed_orders
(
    order_id uuid primary key references orders (id),
    reason   text
);

create table order_outbox
(
    order_id uuid primary key
);
