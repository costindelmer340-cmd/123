USE ecommerce_after_sale;

SET NAMES utf8mb4;

SET @has_delivered_at := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'twenty_mall_order'
    AND COLUMN_NAME = 'delivered_at'
);

SET @add_delivered_at_sql := IF(
  @has_delivered_at = 0,
  'ALTER TABLE twenty_mall_order ADD COLUMN delivered_at DATETIME NULL AFTER ordered_at',
  'SELECT 1'
);
PREPARE add_delivered_at_stmt FROM @add_delivered_at_sql;
EXECUTE add_delivered_at_stmt;
DEALLOCATE PREPARE add_delivered_at_stmt;

SET @has_policy_tags := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'twenty_mall_order'
    AND COLUMN_NAME = 'policy_tags'
);

SET @add_policy_tags_sql := IF(
  @has_policy_tags = 0,
  'ALTER TABLE twenty_mall_order ADD COLUMN policy_tags JSON NULL AFTER delivered_at',
  'SELECT 1'
);
PREPARE add_policy_tags_stmt FROM @add_policy_tags_sql;
EXECUTE add_policy_tags_stmt;
DEALLOCATE PREPARE add_policy_tags_stmt;

UPDATE twenty_mall_order
SET
  ordered_at = '2026-06-26 10:00:00',
  delivered_at = '2026-06-27 09:16:35',
  policy_tags = JSON_ARRAY('7天无理由退货', '运费险')
WHERE order_no = 'TM202606270001';

UPDATE twenty_mall_order
SET
  ordered_at = '2026-06-26 12:00:00',
  delivered_at = NULL,
  policy_tags = JSON_ARRAY('7天无理由退货', '运费险', '15天价格保护')
WHERE order_no = 'TM202606270002';

UPDATE twenty_mall_order
SET
  delivered_at = DATE_ADD(ordered_at, INTERVAL 1 DAY),
  policy_tags = JSON_ARRAY('7天无理由退货')
WHERE delivered_at IS NULL
  AND (order_status = 'COMPLETED' OR logistics_status = 'RECEIVED');

UPDATE twenty_mall_order
SET policy_tags = CASE
  WHEN policy_tags IS NOT NULL THEN policy_tags
  WHEN MOD(id, 3) = 0 THEN JSON_ARRAY('7天无理由退货', '15天价格保护')
  WHEN MOD(id, 3) = 1 THEN JSON_ARRAY('7天无理由退货', '运费险')
  ELSE JSON_ARRAY('运费险')
END
WHERE policy_tags IS NULL;
