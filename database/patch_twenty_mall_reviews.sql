USE ecommerce_after_sale;

SET NAMES utf8mb4;

INSERT INTO twenty_mall_review (
  order_id, product_id, consumer_account_id, product_score, service_score, content, status, reviewed_at
)
SELECT o.id, p.id, o.consumer_account_id, 4, 5,
       '背包容量合适，日常通勤够用，客服回复也比较及时，希望后续能增加更多颜色选择。',
       'PUBLISHED', '2026-06-28 14:20:00'
FROM twenty_mall_order o
JOIN twenty_mall_product p ON p.id = 2
WHERE o.order_no = 'TM202606270002'
  AND NOT EXISTS (
    SELECT 1
    FROM twenty_mall_review r
    WHERE r.order_id = o.id
      AND r.product_id = p.id
      AND r.deleted = 0
  );
