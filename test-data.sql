------------------------- delivery-db -------------------------

truncate table couriers, courier_availability, order_courier, order_outbox;

insert into couriers (id, courier) values
    ('a2697c1e-eb55-4cc5-85eb-7e6a19632289', '{"id": "a2697c1e-eb55-4cc5-85eb-7e6a19632289", "name": "Вася"}'),
    ('0889699f-c17c-46b5-b0c2-65aac592bea2', '{"id": "0889699f-c17c-46b5-b0c2-65aac592bea2", "name": "Петя"}'),
    ('e37389fa-8b45-462f-9f11-322a396cd436', '{"id": "e37389fa-8b45-462f-9f11-322a396cd436", "name": "Маша"}'),
    ('047baad9-8cd1-4831-9142-190a071eda47', '{"id": "047baad9-8cd1-4831-9142-190a071eda47", "name": "Олег"}');

insert into courier_availability (courier_id, is_available) values
    ('a2697c1e-eb55-4cc5-85eb-7e6a19632289', true),
    ('0889699f-c17c-46b5-b0c2-65aac592bea2', true),
    ('e37389fa-8b45-462f-9f11-322a396cd436', true),
    ('047baad9-8cd1-4831-9142-190a071eda47', false);

-------------------------- pantry-db --------------------------

truncate table products, product_count, orders, order_outbox;

insert into products (id, product) values
    ('c3864dbe-b301-4591-a3c6-7a3af4e0e77a', '{"id": "c3864dbe-b301-4591-a3c6-7a3af4e0e77a", "name": "Хлеб"}'),
    ('70588a14-f904-42f5-a1d7-14b8e89c56c1', '{"id": "70588a14-f904-42f5-a1d7-14b8e89c56c1", "name": "Молоко"}'),
    ('4f8d6b5e-975b-4d42-83e7-0326c50d5ac6', '{"id": "4f8d6b5e-975b-4d42-83e7-0326c50d5ac6", "name": "Квас"}'),
    ('0ad53341-648e-4d40-99f5-1798ed433e40', '{"id": "0ad53341-648e-4d40-99f5-1798ed433e40", "name": "Яблоко"}'),
    ('a6ea7ece-8ba1-41e6-86d5-7558a3f1e112', '{"id": "a6ea7ece-8ba1-41e6-86d5-7558a3f1e112", "name": "Морковь"}'),
    ('5765c613-ea76-4a2d-9f00-df8c9865d471', '{"id": "5765c613-ea76-4a2d-9f00-df8c9865d471", "name": "Овсянка"}'),
    ('39430033-7851-4849-a77e-4affa9659836', '{"id": "39430033-7851-4849-a77e-4affa9659836", "name": "Макароны"}'),
    ('ef25c1b9-d4c0-42d2-9b1d-05d245c686a2', '{"id": "ef25c1b9-d4c0-42d2-9b1d-05d245c686a2", "name": "Шоколад"}'),
    ('e041a345-ab60-4b66-9bcd-9f6fbd718e7b', '{"id": "e041a345-ab60-4b66-9bcd-9f6fbd718e7b", "name": "Чай"}'),
    ('25d8f9d1-2073-42f1-a342-addff1f1835b', '{"id": "25d8f9d1-2073-42f1-a342-addff1f1835b", "name": "Печенье"}');

insert into product_count (product_id, count) values
    ('c3864dbe-b301-4591-a3c6-7a3af4e0e77a', 5),
    ('70588a14-f904-42f5-a1d7-14b8e89c56c1', 12),
    ('4f8d6b5e-975b-4d42-83e7-0326c50d5ac6', 4),
    ('0ad53341-648e-4d40-99f5-1798ed433e40', 89),
    ('a6ea7ece-8ba1-41e6-86d5-7558a3f1e112', 66),
    ('5765c613-ea76-4a2d-9f00-df8c9865d471', 14),
    ('39430033-7851-4849-a77e-4affa9659836', 8),
    ('ef25c1b9-d4c0-42d2-9b1d-05d245c686a2', 1),
    ('e041a345-ab60-4b66-9bcd-9f6fbd718e7b', 17),
    ('25d8f9d1-2073-42f1-a342-addff1f1835b', 41);

------------------------ foodmarket-db ------------------------

truncate table orders, order_status, order_outbox, failed_orders;

insert into orders (id, order_raw) values
    -- Хлеб, Молоко, Квас, Яблоко, Морковь
    ('42dd9612-ae3c-4e03-bead-806c3a3b695d', '{
     "id": "42dd9612-ae3c-4e03-bead-806c3a3b695d",
     "products": [
       "c3864dbe-b301-4591-a3c6-7a3af4e0e77a",
       "70588a14-f904-42f5-a1d7-14b8e89c56c1",
       "4f8d6b5e-975b-4d42-83e7-0326c50d5ac6",
       "0ad53341-648e-4d40-99f5-1798ed433e40",
       "a6ea7ece-8ba1-41e6-86d5-7558a3f1e112"
     ]
    }'),
    -- Шоколад (2) - не будет такого количества на складе
    ('c6725c27-e6ab-4430-9f9a-916ed8933a2b', '{
     "id": "c6725c27-e6ab-4430-9f9a-916ed8933a2b",
     "products": [
       "ef25c1b9-d4c0-42d2-9b1d-05d245c686a2",
       "ef25c1b9-d4c0-42d2-9b1d-05d245c686a2"
     ]
    }'),
    -- Яблоко (3), Морковь(2) - не будет свободного курьера
    ('c4943a1c-5494-43bb-9c71-724c663491cb', '{
     "id": "c4943a1c-5494-43bb-9c71-724c663491cb",
     "products": [
       "0ad53341-648e-4d40-99f5-1798ed433e40",
       "0ad53341-648e-4d40-99f5-1798ed433e40",
       "0ad53341-648e-4d40-99f5-1798ed433e40",
       "a6ea7ece-8ba1-41e6-86d5-7558a3f1e112",
       "a6ea7ece-8ba1-41e6-86d5-7558a3f1e112"
     ]
    }');

insert into order_status (order_id, status) values
    ('42dd9612-ae3c-4e03-bead-806c3a3b695d', 'Pending'),
    ('c6725c27-e6ab-4430-9f9a-916ed8933a2b', 'Pending'),
    ('c4943a1c-5494-43bb-9c71-724c663491cb', 'Pending');

insert into order_outbox (order_id) values
    ('42dd9612-ae3c-4e03-bead-806c3a3b695d'),
    ('c6725c27-e6ab-4430-9f9a-916ed8933a2b'),
    ('c4943a1c-5494-43bb-9c71-724c663491cb');
