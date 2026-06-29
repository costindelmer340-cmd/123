USE ecommerce_after_sale;

SET NAMES utf8mb4;

INSERT INTO twenty_mall_account (
  account_no, password_plain, account_role, display_name, phone, bind_status, status
)
SELECT '20230142', '123456', 'MERCHANT', '黑曜通勤箱包店', '13900002021', 'UNBOUND', 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1
  FROM twenty_mall_account
  WHERE account_no = '20230142'
    AND account_role = 'MERCHANT'
);

UPDATE twenty_mall_account
SET display_name = '极光外设旗舰店',
    phone = '13900002020',
    updated_at = NOW()
WHERE account_no = '20230141'
  AND account_role = 'MERCHANT';

UPDATE twenty_mall_account
SET display_name = '黑曜通勤箱包店',
    phone = '13900002021',
    updated_at = NOW()
WHERE account_no = '20230142'
  AND account_role = 'MERCHANT';

SET @merchant_keyboard_id := (
  SELECT id FROM twenty_mall_account
  WHERE account_no = '20230141'
    AND account_role = 'MERCHANT'
  LIMIT 1
);

SET @merchant_bag_id := (
  SELECT id FROM twenty_mall_account
  WHERE account_no = '20230142'
    AND account_role = 'MERCHANT'
  LIMIT 1
);

UPDATE twenty_mall_product
SET merchant_account_id = @merchant_keyboard_id
WHERE id = 1;

UPDATE twenty_mall_product
SET merchant_account_id = @merchant_bag_id
WHERE id = 2;

UPDATE twenty_mall_order
SET merchant_account_id = @merchant_keyboard_id
WHERE order_no = 'TM202606270001';

UPDATE twenty_mall_order
SET merchant_account_id = @merchant_bag_id
WHERE order_no = 'TM202606270002';

UPDATE external_shop_binding
SET shop_name = '极光外设旗舰店',
    seller_nick = '极光外设旗舰店',
    updated_at = NOW()
WHERE platform_code = 'TWENTY_MALL'
  AND external_shop_id = 'TM_SHOP_20230141';

INSERT INTO external_shop_binding (
  merchant_id, platform_id, platform_code, external_shop_id, shop_name, seller_nick, auth_status, last_synced_at
)
SELECT 1, 2, 'TWENTY_MALL', 'TM_SHOP_20230142', '黑曜通勤箱包店', '黑曜通勤箱包店', 'ACTIVE', '2026-06-27 17:10:00'
WHERE NOT EXISTS (
  SELECT 1
  FROM external_shop_binding
  WHERE platform_code = 'TWENTY_MALL'
    AND external_shop_id = 'TM_SHOP_20230142'
);
