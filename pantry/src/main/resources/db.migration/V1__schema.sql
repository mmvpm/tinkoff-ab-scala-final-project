create table products
(
    id      uuid primary key,
    product jsonb
);

create table product_count
(
    product_id uuid primary key references products (id),
    count      int
);

create table order_outbox
(
    order_id uuid primary key
);

create table orders
(
    order_id  uuid primary key,
    order_raw jsonb
);
