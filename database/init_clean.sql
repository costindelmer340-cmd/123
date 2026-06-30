CREATE DATABASE IF NOT EXISTS ecommerce_after_sale
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ecommerce_after_sale;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS ai_call_log;
DROP TABLE IF EXISTS ai_training_sample;
DROP TABLE IF EXISTS ai_model_version;
DROP TABLE IF EXISTS ai_config;
DROP TABLE IF EXISTS after_sale_rule;
DROP TABLE IF EXISTS faq_item;
DROP TABLE IF EXISTS knowledge_article;
DROP TABLE IF EXISTS review_analysis;
DROP TABLE IF EXISTS review_append;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS ticket_record;
DROP TABLE IF EXISTS ticket;
DROP TABLE IF EXISTS service_evaluation;
DROP TABLE IF EXISTS quick_reply;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS customer_conversation;
DROP TABLE IF EXISTS after_sale_write_back_log;
DROP TABLE IF EXISTS refund_record;
DROP TABLE IF EXISTS after_sale_material;
DROP TABLE IF EXISTS external_after_sale_mapping;
DROP TABLE IF EXISTS after_sale_application;
DROP TABLE IF EXISTS external_logistics_snapshot;
DROP TABLE IF EXISTS external_payment_snapshot;
DROP TABLE IF EXISTS external_order_item;
DROP TABLE IF EXISTS external_order;
DROP TABLE IF EXISTS sync_log;
DROP TABLE IF EXISTS sync_cursor;
DROP TABLE IF EXISTS sync_task;
DROP TABLE IF EXISTS external_api_call_log;
DROP TABLE IF EXISTS external_auth_token;
DROP TABLE IF EXISTS external_shop_binding;
DROP TABLE IF EXISTS external_platform;
DROP TABLE IF EXISTS twenty_mall_review;
DROP TABLE IF EXISTS twenty_mall_after_sale;
DROP TABLE IF EXISTS twenty_mall_order_item;
DROP TABLE IF EXISTS twenty_mall_order;
DROP TABLE IF EXISTS twenty_mall_product;
DROP TABLE IF EXISTS twenty_mall_account;
DROP TABLE IF EXISTS merchant_staff;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NULL,
  nickname VARCHAR(64) NOT NULL,
  avatar_url VARCHAR(512) NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_phone (phone),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  description VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_role (role_id),
  CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE merchant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64) NULL,
  contact_phone VARCHAR(32) NULL,
  logo_url VARCHAR(512) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  description VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_merchant_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE merchant_staff (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  staff_no VARCHAR(64) NULL,
  staff_name VARCHAR(64) NOT NULL,
  staff_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_merchant_staff_user (merchant_id, user_id),
  KEY idx_merchant_staff_type (merchant_id, staff_type),
  CONSTRAINT fk_merchant_staff_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_merchant_staff_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_platform (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform_code VARCHAR(32) NOT NULL,
  platform_name VARCHAR(64) NOT NULL,
  api_base_url VARCHAR(512) NULL,
  auth_base_url VARCHAR(512) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  description VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_external_platform_code (platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE twenty_mall_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_no VARCHAR(64) NOT NULL,
  password_plain VARCHAR(128) NOT NULL,
  account_role VARCHAR(32) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  phone VARCHAR(32) NULL,
  bind_status VARCHAR(32) NOT NULL DEFAULT 'UNBOUND',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_twenty_mall_account_no_role (account_no, account_role),
  KEY idx_twenty_mall_account_role (account_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE twenty_mall_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_account_id BIGINT NOT NULL,
  product_no VARCHAR(64) NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  product_image_url VARCHAR(512) NULL,
  price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  stock INT NOT NULL DEFAULT 0,
  category VARCHAR(64) NULL,
  description TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ON_SALE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_twenty_mall_product_no (product_no),
  KEY idx_twenty_mall_product_merchant (merchant_account_id),
  CONSTRAINT fk_twenty_mall_product_merchant FOREIGN KEY (merchant_account_id) REFERENCES twenty_mall_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE twenty_mall_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  consumer_account_id BIGINT NOT NULL,
  merchant_account_id BIGINT NOT NULL,
  order_status VARCHAR(32) NOT NULL,
  pay_status VARCHAR(32) NOT NULL,
  logistics_status VARCHAR(32) NULL,
  after_sale_status VARCHAR(32) NULL,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  paid_at DATETIME NULL,
  ordered_at DATETIME NULL,
  delivered_at DATETIME NULL,
  policy_tags JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_twenty_mall_order_no (order_no),
  KEY idx_twenty_mall_order_consumer (consumer_account_id),
  KEY idx_twenty_mall_order_merchant (merchant_account_id),
  CONSTRAINT fk_twenty_mall_order_consumer FOREIGN KEY (consumer_account_id) REFERENCES twenty_mall_account (id),
  CONSTRAINT fk_twenty_mall_order_merchant FOREIGN KEY (merchant_account_id) REFERENCES twenty_mall_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE twenty_mall_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  sku_name VARCHAR(255) NULL,
  product_image_url VARCHAR(512) NULL,
  unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  quantity INT NOT NULL DEFAULT 1,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  after_sale_status VARCHAR(32) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_twenty_mall_order_item_order (order_id),
  CONSTRAINT fk_twenty_mall_order_item_order FOREIGN KEY (order_id) REFERENCES twenty_mall_order (id),
  CONSTRAINT fk_twenty_mall_order_item_product FOREIGN KEY (product_id) REFERENCES twenty_mall_product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE twenty_mall_after_sale (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  after_sale_no VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL,
  after_sale_type VARCHAR(32) NOT NULL,
  reason_type VARCHAR(64) NOT NULL,
  description TEXT NULL,
  requested_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_twenty_mall_after_sale_no (after_sale_no),
  KEY idx_twenty_mall_after_sale_order (order_id),
  CONSTRAINT fk_twenty_mall_after_sale_order FOREIGN KEY (order_id) REFERENCES twenty_mall_order (id),
  CONSTRAINT fk_twenty_mall_after_sale_item FOREIGN KEY (order_item_id) REFERENCES twenty_mall_order_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE twenty_mall_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  consumer_account_id BIGINT NOT NULL,
  product_score INT NOT NULL,
  service_score INT NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_twenty_mall_review_order (order_id),
  CONSTRAINT fk_twenty_mall_review_order FOREIGN KEY (order_id) REFERENCES twenty_mall_order (id),
  CONSTRAINT fk_twenty_mall_review_product FOREIGN KEY (product_id) REFERENCES twenty_mall_product (id),
  CONSTRAINT fk_twenty_mall_review_consumer FOREIGN KEY (consumer_account_id) REFERENCES twenty_mall_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_shop_binding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  platform_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  external_shop_id VARCHAR(128) NOT NULL,
  shop_name VARCHAR(128) NOT NULL,
  seller_nick VARCHAR(128) NULL,
  auth_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_synced_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_external_shop_binding (platform_code, external_shop_id),
  KEY idx_external_shop_binding_merchant (merchant_id),
  KEY idx_external_shop_binding_status (auth_status),
  CONSTRAINT fk_external_shop_binding_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_external_shop_binding_platform FOREIGN KEY (platform_id) REFERENCES external_platform (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_auth_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shop_binding_id BIGINT NOT NULL,
  access_token_cipher TEXT NOT NULL,
  refresh_token_cipher TEXT NULL,
  access_token_expires_at DATETIME NULL,
  refresh_token_expires_at DATETIME NULL,
  scope_text VARCHAR(1024) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_external_auth_token_binding (shop_binding_id),
  KEY idx_external_auth_token_status (status),
  CONSTRAINT fk_external_auth_token_binding FOREIGN KEY (shop_binding_id) REFERENCES external_shop_binding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_api_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform_code VARCHAR(32) NOT NULL,
  shop_binding_id BIGINT NULL,
  api_name VARCHAR(128) NOT NULL,
  business_type VARCHAR(64) NULL,
  business_id BIGINT NULL,
  request_summary TEXT NULL,
  response_summary TEXT NULL,
  success TINYINT(1) NOT NULL DEFAULT 1,
  error_message VARCHAR(1024) NULL,
  latency_ms INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_external_api_call_platform (platform_code, api_name),
  KEY idx_external_api_call_business (business_type, business_id),
  KEY idx_external_api_call_binding (shop_binding_id),
  CONSTRAINT fk_external_api_call_binding FOREIGN KEY (shop_binding_id) REFERENCES external_shop_binding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE sync_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shop_binding_id BIGINT NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  task_name VARCHAR(128) NOT NULL,
  schedule_cron VARCHAR(128) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  last_run_at DATETIME NULL,
  next_run_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_sync_task_binding (shop_binding_id),
  KEY idx_sync_task_type (task_type),
  CONSTRAINT fk_sync_task_binding FOREIGN KEY (shop_binding_id) REFERENCES external_shop_binding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE sync_cursor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shop_binding_id BIGINT NOT NULL,
  cursor_type VARCHAR(64) NOT NULL,
  cursor_value VARCHAR(512) NULL,
  last_sync_time DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sync_cursor (shop_binding_id, cursor_type),
  CONSTRAINT fk_sync_cursor_binding FOREIGN KEY (shop_binding_id) REFERENCES external_shop_binding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE sync_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NULL,
  shop_binding_id BIGINT NOT NULL,
  sync_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  total_count INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  failed_count INT NOT NULL DEFAULT 0,
  error_message VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sync_log_task (task_id),
  KEY idx_sync_log_binding (shop_binding_id, sync_type),
  KEY idx_sync_log_status (status),
  CONSTRAINT fk_sync_log_task FOREIGN KEY (task_id) REFERENCES sync_task (id),
  CONSTRAINT fk_sync_log_binding FOREIGN KEY (shop_binding_id) REFERENCES external_shop_binding (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shop_binding_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  external_order_no VARCHAR(128) NOT NULL,
  buyer_masked_name VARCHAR(128) NULL,
  buyer_masked_phone VARCHAR(64) NULL,
  order_status VARCHAR(32) NOT NULL,
  pay_status VARCHAR(32) NULL,
  logistics_status VARCHAR(32) NULL,
  after_sale_status VARCHAR(32) NULL,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  payable_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  paid_at DATETIME NULL,
  ordered_at DATETIME NULL,
  completed_at DATETIME NULL,
  raw_data JSON NULL,
  last_synced_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_external_order_no (platform_code, external_order_no),
  KEY idx_external_order_binding (shop_binding_id),
  KEY idx_external_order_merchant_status (merchant_id, order_status),
  KEY idx_external_order_ordered_at (ordered_at),
  CONSTRAINT fk_external_order_binding FOREIGN KEY (shop_binding_id) REFERENCES external_shop_binding (id),
  CONSTRAINT fk_external_order_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_order_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  external_item_id VARCHAR(128) NULL,
  external_product_id VARCHAR(128) NULL,
  product_name VARCHAR(255) NOT NULL,
  sku_name VARCHAR(255) NULL,
  product_image_url VARCHAR(512) NULL,
  unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  quantity INT NOT NULL DEFAULT 1,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  after_sale_status VARCHAR(32) NULL,
  product_snapshot JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_external_order_item_order (external_order_id),
  KEY idx_external_order_item_product (external_product_id),
  CONSTRAINT fk_external_order_item_order FOREIGN KEY (external_order_id) REFERENCES external_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_payment_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_order_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  external_payment_no VARCHAR(128) NULL,
  pay_channel VARCHAR(64) NULL,
  pay_status VARCHAR(32) NOT NULL,
  paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  paid_at DATETIME NULL,
  raw_data JSON NULL,
  last_synced_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_external_payment_order (external_order_id),
  CONSTRAINT fk_external_payment_order FOREIGN KEY (external_order_id) REFERENCES external_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_logistics_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_order_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  logistics_company VARCHAR(128) NULL,
  tracking_no VARCHAR(128) NULL,
  logistics_status VARCHAR(32) NOT NULL,
  shipped_at DATETIME NULL,
  received_at DATETIME NULL,
  tracking_detail JSON NULL,
  raw_data JSON NULL,
  last_synced_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_external_logistics_order (external_order_id),
  KEY idx_external_logistics_tracking (tracking_no),
  CONSTRAINT fk_external_logistics_order FOREIGN KEY (external_order_id) REFERENCES external_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE after_sale_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  after_sale_no VARCHAR(64) NOT NULL,
  external_order_id BIGINT NOT NULL,
  external_order_item_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  shop_binding_id BIGINT NOT NULL,
  after_sale_type VARCHAR(32) NOT NULL,
  reason_type VARCHAR(64) NOT NULL,
  problem_description TEXT NULL,
  requested_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
  priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  ai_category VARCHAR(64) NULL,
  reviewer_id BIGINT NULL,
  reviewed_at DATETIME NULL,
  review_opinion VARCHAR(512) NULL,
  final_result VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_after_sale_no (after_sale_no),
  KEY idx_after_sale_order (external_order_id),
  KEY idx_after_sale_merchant_status (merchant_id, status),
  KEY idx_after_sale_user (user_id),
  CONSTRAINT fk_after_sale_external_order FOREIGN KEY (external_order_id) REFERENCES external_order (id),
  CONSTRAINT fk_after_sale_external_item FOREIGN KEY (external_order_item_id) REFERENCES external_order_item (id),
  CONSTRAINT fk_after_sale_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_after_sale_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_after_sale_binding FOREIGN KEY (shop_binding_id) REFERENCES external_shop_binding (id),
  CONSTRAINT fk_after_sale_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE external_after_sale_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  after_sale_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  external_after_sale_no VARCHAR(128) NULL,
  external_refund_no VARCHAR(128) NULL,
  external_status VARCHAR(64) NULL,
  raw_data JSON NULL,
  last_synced_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_external_after_sale_mapping (platform_code, external_after_sale_no),
  KEY idx_external_after_sale_after_sale (after_sale_id),
  CONSTRAINT fk_external_after_sale_mapping_after_sale FOREIGN KEY (after_sale_id) REFERENCES after_sale_application (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE after_sale_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  after_sale_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  material_type VARCHAR(32) NOT NULL,
  material_url VARCHAR(512) NOT NULL,
  description VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_after_sale_material_after_sale (after_sale_id),
  CONSTRAINT fk_after_sale_material_after_sale FOREIGN KEY (after_sale_id) REFERENCES after_sale_application (id),
  CONSTRAINT fk_after_sale_material_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE refund_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  refund_no VARCHAR(64) NOT NULL,
  after_sale_id BIGINT NOT NULL,
  external_order_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  external_refund_no VARCHAR(128) NULL,
  refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  refund_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  reason VARCHAR(255) NULL,
  refunded_at DATETIME NULL,
  raw_data JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_refund_record_no (refund_no),
  KEY idx_refund_record_after_sale (after_sale_id),
  KEY idx_refund_record_order (external_order_id),
  CONSTRAINT fk_refund_record_after_sale FOREIGN KEY (after_sale_id) REFERENCES after_sale_application (id),
  CONSTRAINT fk_refund_record_order FOREIGN KEY (external_order_id) REFERENCES external_order (id),
  CONSTRAINT fk_refund_record_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE after_sale_write_back_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  after_sale_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NOT NULL,
  shop_binding_id BIGINT NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  request_payload JSON NULL,
  response_payload JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  error_message VARCHAR(1024) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_write_back_after_sale (after_sale_id),
  KEY idx_write_back_status (status),
  CONSTRAINT fk_write_back_after_sale FOREIGN KEY (after_sale_id) REFERENCES after_sale_application (id),
  CONSTRAINT fk_write_back_binding FOREIGN KEY (shop_binding_id) REFERENCES external_shop_binding (id),
  CONSTRAINT fk_write_back_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE customer_conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  external_order_id BIGINT NULL,
  assigned_staff_id BIGINT NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'MINIAPP',
  status VARCHAR(32) NOT NULL DEFAULT 'AI_SERVING',
  last_message VARCHAR(512) NULL,
  last_message_at DATETIME NULL,
  ai_intent VARCHAR(64) NULL,
  ai_summary VARCHAR(512) NULL,
  transferred_at DATETIME NULL,
  closed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_customer_conversation_no (conversation_no),
  KEY idx_customer_conversation_user (user_id),
  KEY idx_customer_conversation_merchant_status (merchant_id, status),
  KEY idx_customer_conversation_staff (assigned_staff_id),
  CONSTRAINT fk_customer_conversation_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_customer_conversation_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_customer_conversation_order FOREIGN KEY (external_order_id) REFERENCES external_order (id),
  CONSTRAINT fk_customer_conversation_staff FOREIGN KEY (assigned_staff_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NULL,
  sender_type VARCHAR(32) NOT NULL,
  message_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
  content TEXT NULL,
  media_url VARCHAR(512) NULL,
  ai_generated TINYINT(1) NOT NULL DEFAULT 0,
  ai_confidence DECIMAL(5,4) NULL,
  read_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_chat_message_conversation (conversation_id, created_at),
  KEY idx_chat_message_sender (sender_id),
  CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES customer_conversation (id),
  CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE quick_reply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  scene VARCHAR(64) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_quick_reply_merchant (merchant_id),
  CONSTRAINT fk_quick_reply_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_quick_reply_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE service_evaluation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  staff_id BIGINT NULL,
  rating INT NOT NULL,
  comment VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_service_evaluation_conversation (conversation_id),
  KEY idx_service_evaluation_staff (staff_id),
  CONSTRAINT fk_service_evaluation_conversation FOREIGN KEY (conversation_id) REFERENCES customer_conversation (id),
  CONSTRAINT fk_service_evaluation_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_service_evaluation_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_service_evaluation_staff FOREIGN KEY (staff_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(64) NOT NULL,
  after_sale_id BIGINT NULL,
  conversation_id BIGINT NULL,
  external_order_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  assigned_staff_id BIGINT NULL,
  ticket_type VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  ai_category VARCHAR(64) NULL,
  ai_confidence DECIMAL(5,4) NULL,
  due_at DATETIME NULL,
  closed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ticket_no (ticket_no),
  KEY idx_ticket_after_sale (after_sale_id),
  KEY idx_ticket_conversation (conversation_id),
  KEY idx_ticket_merchant_status (merchant_id, status),
  KEY idx_ticket_assigned_staff (assigned_staff_id),
  CONSTRAINT fk_ticket_after_sale FOREIGN KEY (after_sale_id) REFERENCES after_sale_application (id),
  CONSTRAINT fk_ticket_conversation FOREIGN KEY (conversation_id) REFERENCES customer_conversation (id),
  CONSTRAINT fk_ticket_order FOREIGN KEY (external_order_id) REFERENCES external_order (id),
  CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_ticket_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_ticket_assigned_staff FOREIGN KEY (assigned_staff_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE ticket_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  operator_id BIGINT NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  from_status VARCHAR(32) NULL,
  to_status VARCHAR(32) NULL,
  content TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ticket_record_ticket (ticket_id),
  CONSTRAINT fk_ticket_record_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
  CONSTRAINT fk_ticket_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_order_id BIGINT NULL,
  external_order_item_id BIGINT NULL,
  user_id BIGINT NULL,
  merchant_id BIGINT NOT NULL,
  platform_code VARCHAR(32) NULL,
  external_review_id VARCHAR(128) NULL,
  review_source VARCHAR(32) NOT NULL DEFAULT 'EXTERNAL_PLATFORM',
  product_score INT NULL,
  logistics_score INT NULL,
  service_score INT NULL,
  content TEXT NULL,
  image_urls JSON NULL,
  anonymous TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_review_external (platform_code, external_review_id),
  KEY idx_review_order_item (external_order_item_id),
  KEY idx_review_merchant (merchant_id),
  CONSTRAINT fk_review_order FOREIGN KEY (external_order_id) REFERENCES external_order (id),
  CONSTRAINT fk_review_order_item FOREIGN KEY (external_order_item_id) REFERENCES external_order_item (id),
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_review_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE review_append (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  review_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  image_urls JSON NULL,
  appended_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_review_append_review (review_id),
  CONSTRAINT fk_review_append_review FOREIGN KEY (review_id) REFERENCES review (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE review_analysis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  review_id BIGINT NOT NULL,
  sentiment VARCHAR(32) NOT NULL,
  sentiment_score DECIMAL(5,4) NULL,
  topics JSON NULL,
  keywords JSON NULL,
  risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
  summary VARCHAR(512) NULL,
  analyzed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_review_analysis_review (review_id),
  CONSTRAINT fk_review_analysis_review FOREIGN KEY (review_id) REFERENCES review (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE knowledge_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  category VARCHAR(64) NOT NULL,
  tags JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_knowledge_article_merchant (merchant_id),
  KEY idx_knowledge_article_category (category),
  CONSTRAINT fk_knowledge_article_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_knowledge_article_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE faq_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NULL,
  question VARCHAR(255) NOT NULL,
  answer TEXT NOT NULL,
  category VARCHAR(64) NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_faq_item_merchant (merchant_id),
  KEY idx_faq_item_category (category),
  CONSTRAINT fk_faq_item_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_faq_item_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE after_sale_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NULL,
  rule_name VARCHAR(128) NOT NULL,
  rule_type VARCHAR(64) NOT NULL,
  conditions_json JSON NULL,
  action_json JSON NULL,
  content TEXT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_after_sale_rule_merchant (merchant_id),
  KEY idx_after_sale_rule_type (rule_type),
  CONSTRAINT fk_after_sale_rule_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_after_sale_rule_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE ai_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NULL,
  config_name VARCHAR(128) NOT NULL,
  provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
  model_name VARCHAR(128) NULL,
  model_path VARCHAR(512) NULL,
  temperature DECIMAL(3,2) NOT NULL DEFAULT 0.70,
  max_tokens INT NOT NULL DEFAULT 1024,
  prompt_template TEXT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_ai_config_merchant (merchant_id),
  CONSTRAINT fk_ai_config_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_ai_config_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE ai_model_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_id BIGINT NULL,
  model_code VARCHAR(128) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  version_no VARCHAR(64) NOT NULL,
  model_path VARCHAR(512) NULL,
  metrics_json JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  trained_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_ai_model_version (model_code, version_no),
  KEY idx_ai_model_version_task (task_type),
  CONSTRAINT fk_ai_model_version_config FOREIGN KEY (config_id) REFERENCES ai_config (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE ai_training_sample (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  model_version_id BIGINT NULL,
  task_type VARCHAR(64) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  source_id BIGINT NULL,
  input_text TEXT NOT NULL,
  label_json JSON NOT NULL,
  sample_status VARCHAR(32) NOT NULL DEFAULT 'LABELED',
  labeled_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_ai_training_sample_task (task_type),
  KEY idx_ai_training_sample_model (model_version_id),
  CONSTRAINT fk_ai_training_sample_model FOREIGN KEY (model_version_id) REFERENCES ai_model_version (id),
  CONSTRAINT fk_ai_training_sample_labeled_by FOREIGN KEY (labeled_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE ai_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NULL,
  user_id BIGINT NULL,
  business_type VARCHAR(64) NOT NULL,
  business_id BIGINT NULL,
  task_type VARCHAR(64) NOT NULL,
  model_version_id BIGINT NULL,
  request_text TEXT NULL,
  response_text TEXT NULL,
  confidence DECIMAL(5,4) NULL,
  success TINYINT(1) NOT NULL DEFAULT 1,
  error_message VARCHAR(512) NULL,
  latency_ms INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ai_call_log_business (business_type, business_id),
  KEY idx_ai_call_log_merchant (merchant_id),
  CONSTRAINT fk_ai_call_log_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id),
  CONSTRAINT fk_ai_call_log_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_ai_call_log_model FOREIGN KEY (model_version_id) REFERENCES ai_model_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NULL,
  merchant_id BIGINT NULL,
  module VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  business_type VARCHAR(64) NULL,
  business_id BIGINT NULL,
  content VARCHAR(1024) NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_log_operator (operator_id),
  KEY idx_operation_log_business (business_type, business_id),
  KEY idx_operation_log_merchant (merchant_id),
  CONSTRAINT fk_operation_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id),
  CONSTRAINT fk_operation_log_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ;
