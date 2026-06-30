USE ecommerce_after_sale;
SET NAMES utf8mb4;

INSERT INTO twenty_mall_account (
  account_no, password_plain, account_role, display_name, phone, bind_status, status
) VALUES (
  '22222222', '123456', 'CONSUMER', '20商城用户22222222', NULL, 'UNBOUND', 'ACTIVE'
) ON DUPLICATE KEY UPDATE
  password_plain = VALUES(password_plain),
  display_name = VALUES(display_name),
  phone = VALUES(phone),
  status = 'ACTIVE',
  updated_at = NOW();

INSERT INTO twenty_mall_account (
  account_no, password_plain, account_role, display_name, phone, bind_status, status
) VALUES
  ('22222223', '123456', 'MERCHANT', '晨光数码生活馆', NULL, 'UNBOUND', 'ACTIVE'),
  ('22222224', '123456', 'MERCHANT', '云途箱包旗舰店', NULL, 'UNBOUND', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  password_plain = VALUES(password_plain),
  display_name = VALUES(display_name),
  phone = VALUES(phone),
  status = 'ACTIVE',
  updated_at = NOW();

SET @consumer_id := (
  SELECT id FROM twenty_mall_account WHERE account_no = '22222222' AND account_role = 'CONSUMER' LIMIT 1
);
SET @merchant_digital := (
  SELECT id FROM twenty_mall_account WHERE account_no = '22222223' AND account_role = 'MERCHANT' LIMIT 1
);
SET @merchant_bag := (
  SELECT id FROM twenty_mall_account WHERE account_no = '22222224' AND account_role = 'MERCHANT' LIMIT 1
);

INSERT INTO twenty_mall_product (
  merchant_account_id, product_no, product_name, product_image_url, price, stock, category, description, status
) VALUES
  (
    @merchant_digital, 'TM-P-DIGITAL-222201', '20商城 无线机械键盘',
    '/assets/products/twenty-keyboard-real.png', 459.00, 200, '数码外设',
    '无线机械键盘，适合办公输入、学习和轻度游戏场景。', 'ON_SALE'
  ),
  (
    @merchant_digital, 'TM-P-DIGITAL-222202', '20商城 北欧护眼台灯',
    '/assets/products/twenty-lamp.png', 129.00, 180, '家居数码',
    '北欧风格护眼台灯，适合卧室阅读、书桌学习和夜间照明。', 'ON_SALE'
  ),
  (
    @merchant_digital, 'TM-P-DIGITAL-222203', '20商城 便携钛杯',
    '/assets/products/twenty-cup.png', 69.00, 240, '生活数码',
    '便携钛杯，适合通勤、校园和短途旅行使用。', 'ON_SALE'
  ),
  (
    @merchant_bag, 'TM-P-BAG-222201', '20商城 旅行收纳包',
    '/assets/products/twenty-backpack-real.png', 59.00, 300, '箱包配件',
    '轻量旅行收纳包，适合短途出行、通勤收纳和行李分类整理。', 'ON_SALE'
  ),
  (
    @merchant_bag, 'TM-P-BAG-222202', '20商城 防泼水电脑内胆包',
    '/assets/products/twenty-backpack-real.png', 89.00, 260, '数码收纳',
    '防泼水电脑内胆包，适合笔记本电脑和日常数码配件收纳。', 'ON_SALE'
  )
ON DUPLICATE KEY UPDATE
  merchant_account_id = VALUES(merchant_account_id),
  product_name = VALUES(product_name),
  product_image_url = VALUES(product_image_url),
  price = VALUES(price),
  stock = VALUES(stock),
  category = VALUES(category),
  description = VALUES(description),
  status = 'ON_SALE',
  updated_at = NOW();

SET @product_keyboard := (SELECT id FROM twenty_mall_product WHERE product_no = 'TM-P-DIGITAL-222201' LIMIT 1);
SET @product_lamp := (SELECT id FROM twenty_mall_product WHERE product_no = 'TM-P-DIGITAL-222202' LIMIT 1);
SET @product_cup := (SELECT id FROM twenty_mall_product WHERE product_no = 'TM-P-DIGITAL-222203' LIMIT 1);
SET @product_travel_bag := (SELECT id FROM twenty_mall_product WHERE product_no = 'TM-P-BAG-222201' LIMIT 1);
SET @product_laptop_sleeve := (SELECT id FROM twenty_mall_product WHERE product_no = 'TM-P-BAG-222202' LIMIT 1);

INSERT INTO twenty_mall_order (
  order_no, consumer_account_id, merchant_account_id, order_status, pay_status, logistics_status,
  after_sale_status, total_amount, paid_at, ordered_at, delivered_at, policy_tags
) VALUES
  ('TM222222220001', @consumer_id, @merchant_digital, 'COMPLETED', 'PAID', 'RECEIVED', 'NONE', 459.00,
   '2026-06-29 09:12:18', '2026-06-29 09:10:42', '2026-06-29 14:26:35',
   JSON_ARRAY('7天无理由退货', '运费险')),
  ('TM222222220002', @consumer_id, @merchant_bag, 'SHIPPED', 'PAID', 'IN_TRANSIT', 'NONE', 59.00,
   '2026-06-29 10:08:11', '2026-06-29 10:06:28', NULL,
   JSON_ARRAY('7天无理由退货')),
  ('TM222222220003', @consumer_id, @merchant_digital, 'COMPLETED', 'PAID', 'RECEIVED', 'NONE', 129.00,
   '2026-06-29 11:33:04', '2026-06-29 11:31:52', '2026-06-29 16:05:19',
   JSON_ARRAY('7天无理由退货', '15天价格保护')),
  ('TM222222220004', @consumer_id, @merchant_bag, 'SHIPPED', 'PAID', 'SHIPPED', 'NONE', 89.00,
   '2026-06-29 13:45:30', '2026-06-29 13:43:57', NULL,
   JSON_ARRAY('运费险')),
  ('TM222222220005', @consumer_id, @merchant_digital, 'COMPLETED', 'PAID', 'RECEIVED', 'NONE', 69.00,
   '2026-06-29 15:02:21', '2026-06-29 15:00:44', '2026-06-29 18:21:09',
   JSON_ARRAY('7天无理由退货', '运费险', '15天价格保护'))
ON DUPLICATE KEY UPDATE
  consumer_account_id = VALUES(consumer_account_id),
  merchant_account_id = VALUES(merchant_account_id),
  order_status = VALUES(order_status),
  pay_status = VALUES(pay_status),
  logistics_status = VALUES(logistics_status),
  after_sale_status = VALUES(after_sale_status),
  total_amount = VALUES(total_amount),
  paid_at = VALUES(paid_at),
  ordered_at = VALUES(ordered_at),
  delivered_at = VALUES(delivered_at),
  policy_tags = VALUES(policy_tags),
  deleted = 0,
  updated_at = NOW();

DELETE oi FROM twenty_mall_order_item oi
JOIN twenty_mall_order o ON o.id = oi.order_id
WHERE o.order_no IN (
  'TM222222220001', 'TM222222220002', 'TM222222220003', 'TM222222220004', 'TM222222220005'
);

INSERT INTO twenty_mall_order_item (
  order_id, product_id, product_name, sku_name, product_image_url, unit_price, quantity, total_amount, after_sale_status
)
SELECT o.id, @product_keyboard, p.product_name, '白灰色｜87键｜热插拔', p.product_image_url, 459.00, 1, 459.00, 'NONE'
FROM twenty_mall_order o JOIN twenty_mall_product p ON p.id = @product_keyboard
WHERE o.order_no = 'TM222222220001'
UNION ALL
SELECT o.id, @product_travel_bag, p.product_name, '米白色｜大容量｜旅行收纳', p.product_image_url, 59.00, 1, 59.00, 'NONE'
FROM twenty_mall_order o JOIN twenty_mall_product p ON p.id = @product_travel_bag
WHERE o.order_no = 'TM222222220002'
UNION ALL
SELECT o.id, @product_lamp, p.product_name, '暖光｜床头阅读｜北欧风', p.product_image_url, 129.00, 1, 129.00, 'NONE'
FROM twenty_mall_order o JOIN twenty_mall_product p ON p.id = @product_lamp
WHERE o.order_no = 'TM222222220003'
UNION ALL
SELECT o.id, @product_laptop_sleeve, p.product_name, '灰黑色｜13-14英寸｜防泼水', p.product_image_url, 89.00, 1, 89.00, 'NONE'
FROM twenty_mall_order o JOIN twenty_mall_product p ON p.id = @product_laptop_sleeve
WHERE o.order_no = 'TM222222220004'
UNION ALL
SELECT o.id, @product_cup, p.product_name, '480mL｜钛色｜便携保温', p.product_image_url, 69.00, 1, 69.00, 'NONE'
FROM twenty_mall_order o JOIN twenty_mall_product p ON p.id = @product_cup
WHERE o.order_no = 'TM222222220005';
