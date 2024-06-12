create table couriers
(
    id           uuid primary key,
    courier      jsonb
);

create table courier_availability (
    courier_id uuid primary key references couriers(id),
    is_available bool
);

create table order_courier
(
    order_id   uuid primary key,
    courier_id uuid references couriers (id)
);

create table order_outbox
(
    order_id uuid primary key
);
