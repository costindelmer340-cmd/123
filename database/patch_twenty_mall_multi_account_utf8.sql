USE ecommerce_after_sale;

SET @old_account_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'twenty_mall_account'
    AND index_name = 'uk_twenty_mall_account_no'
);
SET @drop_old_account_index_sql := IF(
  @old_account_index_exists > 0,
  'ALTER TABLE twenty_mall_account DROP INDEX uk_twenty_mall_account_no',
  'SELECT 1'
);
PREPARE drop_old_account_index_stmt FROM @drop_old_account_index_sql;
EXECUTE drop_old_account_index_stmt;
DEALLOCATE PREPARE drop_old_account_index_stmt;

SET @new_account_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'twenty_mall_account'
    AND index_name = 'uk_twenty_mall_account_no_role'
);
SET @add_new_account_index_sql := IF(
  @new_account_index_exists = 0,
  'ALTER TABLE twenty_mall_account ADD UNIQUE KEY uk_twenty_mall_account_no_role (account_no, account_role)',
  'SELECT 1'
);
PREPARE add_new_account_index_stmt FROM @add_new_account_index_sql;
EXECUTE add_new_account_index_stmt;
DEALLOCATE PREPARE add_new_account_index_stmt;

INSERT INTO twenty_mall_account (id, account_no, password_plain, account_role, display_name, phone, bind_status, status, deleted)
VALUES (3, '20230141', '123456', 'CONSUMER', '20商城学生买家', '13338907581', 'UNBOUND', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE
  password_plain = VALUES(password_plain),
  display_name = VALUES(display_name),
  phone = VALUES(phone),
  status = VALUES(status),
  deleted = 0;

INSERT INTO twenty_mall_product (id, merchant_account_id, product_no, product_name, product_image_url, price, stock, category, description, status, deleted)
VALUES
  (3, 2, 'TM-P-10003', '20商城 护眼台灯', '/assets/products/twenty-lamp.png', 129.00, 180, '生活电器', '20商城本地数据库中的模拟护眼台灯商品。', 'ON_SALE', 0),
  (4, 2, 'TM-P-10004', '20商城 便携保温杯', '/assets/products/twenty-cup.png', 69.00, 360, '日用百货', '20商城本地数据库中的模拟保温杯商品。', 'ON_SALE', 0)
ON DUPLICATE KEY UPDATE
  product_name = VALUES(product_name),
  product_image_url = VALUES(product_image_url),
  price = VALUES(price),
  stock = VALUES(stock),
  category = VALUES(category),
  description = VALUES(description),
  status = VALUES(status),
  deleted = 0;

INSERT INTO twenty_mall_order (id, order_no, consumer_account_id, merchant_account_id, order_status, pay_status, logistics_status, after_sale_status, total_amount, paid_at, ordered_at, deleted)
VALUES
  (3, 'TM202606270003', 3, 2, 'COMPLETED', 'PAID', 'RECEIVED', 'NONE', 129.00, '2026-06-27 08:20:00', '2026-06-27 08:10:00', 0),
  (4, 'TM202606270004', 3, 2, 'SHIPPED', 'PAID', 'IN_TRANSIT', 'AFTER_SALE', 69.00, '2026-06-27 09:40:00', '2026-06-27 09:30:00', 0)
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
  deleted = 0;

INSERT INTO twenty_mall_order_item (id, order_id, product_id, product_name, sku_name, product_image_url, unit_price, quantity, total_amount, after_sale_status, deleted)
VALUES
  (3, 3, 3, '20商城 护眼台灯', '暖白光｜三档调光｜USB供电', '/assets/products/twenty-lamp.png', 129.00, 1, 129.00, 'NONE', 0),
  (4, 4, 4, '20商城 便携保温杯', '米白色｜500ml｜弹盖款', '/assets/products/twenty-cup.png', 69.00, 1, 69.00, 'APPLIED', 0)
ON DUPLICATE KEY UPDATE
  product_name = VALUES(product_name),
  sku_name = VALUES(sku_name),
  product_image_url = VALUES(product_image_url),
  unit_price = VALUES(unit_price),
  quantity = VALUES(quantity),
  total_amount = VALUES(total_amount),
  after_sale_status = VALUES(after_sale_status),
  deleted = 0;

INSERT INTO twenty_mall_after_sale (id, after_sale_no, order_id, order_item_id, after_sale_type, reason_type, description, requested_amount, status, deleted)
VALUES (2, 'TMAS202606270002', 4, 4, 'RETURN_REFUND', 'WRONG_GOODS', '保温杯颜色与下单页面不一致，申请退货退款。', 69.00, 'PROCESSING', 0)
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  requested_amount = VALUES(requested_amount),
  status = VALUES(status),
  deleted = 0;

INSERT INTO twenty_mall_review (id, order_id, product_id, consumer_account_id, product_score, service_score, content, status, reviewed_at, deleted)
VALUES (2, 3, 3, 3, 5, 5, '台灯亮度柔和，晚上学习使用比较舒服。', 'PUBLISHED', '2026-06-27 10:30:00', 0)
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  product_score = VALUES(product_score),
  service_score = VALUES(service_score),
  status = VALUES(status),
  reviewed_at = VALUES(reviewed_at),
  deleted = 0;
