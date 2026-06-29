USE ecommerce_after_sale;

SET NAMES utf8mb4;

UPDATE twenty_mall_after_sale a
JOIN (
  SELECT ranked.id,
         CASE
           WHEN MOD(ranked.row_no, 4) = 1 THEN 'PENDING_REVIEW'
           WHEN MOD(ranked.row_no, 4) = 2 THEN 'PROCESSING'
           WHEN MOD(ranked.row_no, 4) = 3 THEN 'COMPLETED'
           ELSE 'REJECTED'
         END AS next_status
  FROM (
    SELECT a2.id, ROW_NUMBER() OVER (ORDER BY a2.created_at DESC, a2.id DESC) AS row_no
    FROM twenty_mall_after_sale a2
    JOIN twenty_mall_order o2 ON o2.id = a2.order_id
    JOIN twenty_mall_account ma2 ON ma2.id = o2.merchant_account_id
    WHERE ma2.account_no = '20230141'
      AND ma2.account_role = 'MERCHANT'
      AND a2.deleted = 0
  ) ranked
) mix ON mix.id = a.id
SET a.status = CASE
    WHEN a.id = 2 THEN 'PROCESSING'
    ELSE mix.next_status
  END,
  a.updated_at = NOW();
