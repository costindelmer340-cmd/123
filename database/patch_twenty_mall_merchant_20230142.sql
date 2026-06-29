USE ecommerce_after_sale;

SET NAMES utf8mb4;

INSERT INTO twenty_mall_account (
  account_no, password_plain, account_role, display_name, phone, bind_status, status
)
SELECT '20230142', '123456', 'MERCHANT', '20商城备用商家', '13900002021', 'UNBOUND', 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1
  FROM twenty_mall_account
  WHERE account_no = '20230142'
    AND account_role = 'MERCHANT'
);
