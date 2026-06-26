USE ecommerce_after_sale;

SET NAMES utf8mb4;

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
  (1, 'CONSUMER', '消费者', '消费者端用户'),
  (2, 'MERCHANT_ADMIN', '商家管理员', '商家后台管理员'),
  (3, 'CUSTOMER_SERVICE', '商家客服', '商家客服人员'),
  (4, 'PLATFORM_ADMIN', '平台管理员', '平台后台管理员');

INSERT INTO sys_user (id, username, password_hash, nickname, avatar_url, phone, email, status) VALUES
  (1, 'consumer_demo', '{noop}123456', '演示买家', NULL, '13800000001', 'consumer@example.com', 'ACTIVE'),
  (2, 'merchant_admin_demo', '{noop}123456', '店铺管理员', NULL, '13800000002', 'merchant@example.com', 'ACTIVE'),
  (3, 'service_demo', '{noop}123456', '客服小林', NULL, '13800000003', 'service@example.com', 'ACTIVE'),
  (4, 'admin_demo', '{noop}123456', '平台管理员', NULL, '13800000004', 'admin@example.com', 'ACTIVE');

INSERT INTO sys_user_role (user_id, role_id) VALUES
  (1, 1),
  (2, 2),
  (3, 3),
  (4, 4);

INSERT INTO merchant (id, merchant_name, contact_name, contact_phone, logo_url, status, description) VALUES
  (1, '星河数码售后中心', '张经理', '13900000001', NULL, 'ACTIVE', '主营数码商品的抖音店铺售后与评价分析。');

INSERT INTO merchant_staff (id, merchant_id, user_id, staff_no, staff_name, staff_type, status) VALUES
  (1, 1, 2, 'MGR001', '店铺管理员', 'MERCHANT_ADMIN', 'ACTIVE'),
  (2, 1, 3, 'CS001', '客服小林', 'CUSTOMER_SERVICE', 'ACTIVE');

INSERT INTO external_platform (id, platform_code, platform_name, api_base_url, auth_base_url, enabled, description) VALUES
  (1, 'DOUYIN', '抖音电商', 'https://openapi-fxg.jinritemai.com', 'https://op.jinritemai.com', 1, '首期接入平台，当前演示数据使用模拟授权。');

INSERT INTO external_shop_binding (
  id, merchant_id, platform_id, platform_code, external_shop_id, shop_name, seller_nick, auth_status, last_synced_at
) VALUES
  (1, 1, 1, 'DOUYIN', 'DY_SHOP_10001', '星河数码抖音旗舰店', '星河数码官方', 'ACTIVE', '2026-06-25 10:00:00');

INSERT INTO external_auth_token (
  id, shop_binding_id, access_token_cipher, refresh_token_cipher, access_token_expires_at,
  refresh_token_expires_at, scope_text, status
) VALUES
  (1, 1, 'encrypted_demo_access_token', 'encrypted_demo_refresh_token', '2026-07-25 10:00:00', '2026-12-25 10:00:00', 'order,after_sale,review,logistics', 'ACTIVE');

INSERT INTO sync_task (id, shop_binding_id, task_type, task_name, schedule_cron, enabled, last_run_at, next_run_at) VALUES
  (1, 1, 'ORDER_SYNC', '抖音订单同步', '0 */30 * * * ?', 1, '2026-06-25 10:00:00', '2026-06-25 10:30:00'),
  (2, 1, 'AFTER_SALE_SYNC', '抖音售后同步', '0 */10 * * * ?', 1, '2026-06-25 10:00:00', '2026-06-25 10:10:00'),
  (3, 1, 'REVIEW_SYNC', '抖音评价同步', '0 0 */2 * * ?', 1, '2026-06-25 10:00:00', '2026-06-25 12:00:00');

INSERT INTO sync_cursor (shop_binding_id, cursor_type, cursor_value, last_sync_time) VALUES
  (1, 'ORDER_SYNC', 'cursor_order_demo', '2026-06-25 10:00:00'),
  (1, 'AFTER_SALE_SYNC', 'cursor_after_sale_demo', '2026-06-25 10:00:00'),
  (1, 'REVIEW_SYNC', 'cursor_review_demo', '2026-06-25 10:00:00');

INSERT INTO sync_log (
  id, task_id, shop_binding_id, sync_type, status, start_time, end_time, total_count, success_count, failed_count, error_message
) VALUES
  (1, 1, 1, 'ORDER_SYNC', 'SUCCESS', '2026-06-25 09:59:30', '2026-06-25 10:00:00', 2, 2, 0, NULL),
  (2, 2, 1, 'AFTER_SALE_SYNC', 'SUCCESS', '2026-06-25 09:59:40', '2026-06-25 10:00:00', 1, 1, 0, NULL);

INSERT INTO external_order (
  id, shop_binding_id, merchant_id, platform_code, external_order_no, buyer_masked_name, buyer_masked_phone,
  order_status, pay_status, logistics_status, after_sale_status, total_amount, payable_amount,
  paid_at, ordered_at, completed_at, raw_data, last_synced_at
) VALUES
  (1, 1, 1, 'DOUYIN', 'DY202606250001', '林**', '138****0001', 'COMPLETED', 'PAID', 'RECEIVED', 'AFTER_SALE', 2999.00, 2999.00, '2026-06-20 10:10:00', '2026-06-20 10:00:00', '2026-06-23 09:00:00', JSON_OBJECT('source','mock-douyin','orderType','NORMAL'), '2026-06-25 10:00:00'),
  (2, 1, 1, 'DOUYIN', 'DY202606250002', '林**', '138****0001', 'SHIPPED', 'PAID', 'IN_TRANSIT', 'NONE', 399.00, 399.00, '2026-06-24 11:00:00', '2026-06-24 10:50:00', NULL, JSON_OBJECT('source','mock-douyin','orderType','NORMAL'), '2026-06-25 10:00:00');

INSERT INTO external_order_item (
  id, external_order_id, platform_code, external_item_id, external_product_id, product_name, sku_name,
  product_image_url, unit_price, quantity, total_amount, after_sale_status, product_snapshot
) VALUES
  (1, 1, 'DOUYIN', 'DY_ITEM_10001', 'DY_PRODUCT_90001', 'Aurora X1 智能手机', '曜石黑 256G', 'https://example.com/images/phone-x1.jpg', 2999.00, 1, 2999.00, 'APPLIED', JSON_OBJECT('brand','Aurora','category','手机通讯')),
  (2, 2, 'DOUYIN', 'DY_ITEM_10002', 'DY_PRODUCT_90002', 'Breeze Pods 无线耳机', '白色 标准版', 'https://example.com/images/breeze-pods.jpg', 399.00, 1, 399.00, 'NONE', JSON_OBJECT('brand','Breeze','category','智能配件'));

INSERT INTO external_payment_snapshot (
  id, external_order_id, platform_code, external_payment_no, pay_channel, pay_status, paid_amount, paid_at, raw_data, last_synced_at
) VALUES
  (1, 1, 'DOUYIN', 'DYPAY202606250001', 'DOUYIN_PAY', 'PAID', 2999.00, '2026-06-20 10:10:00', JSON_OBJECT('source','mock-douyin'), '2026-06-25 10:00:00'),
  (2, 2, 'DOUYIN', 'DYPAY202606250002', 'DOUYIN_PAY', 'PAID', 399.00, '2026-06-24 11:00:00', JSON_OBJECT('source','mock-douyin'), '2026-06-25 10:00:00');

INSERT INTO external_logistics_snapshot (
  id, external_order_id, platform_code, logistics_company, tracking_no, logistics_status,
  shipped_at, received_at, tracking_detail, raw_data, last_synced_at
) VALUES
  (1, 1, 'DOUYIN', '顺丰速运', 'SF202606200001', 'RECEIVED', '2026-06-20 16:30:00', '2026-06-23 09:00:00', JSON_ARRAY(JSON_OBJECT('time','2026-06-23 09:00:00','content','已签收')), JSON_OBJECT('source','mock-douyin'), '2026-06-25 10:00:00'),
  (2, 2, 'DOUYIN', '圆通速递', 'YT202606240001', 'IN_TRANSIT', '2026-06-24 18:20:00', NULL, JSON_ARRAY(JSON_OBJECT('time','2026-06-24 18:20:00','content','商家已发货')), JSON_OBJECT('source','mock-douyin'), '2026-06-25 10:00:00');

INSERT INTO after_sale_application (
  id, after_sale_no, external_order_id, external_order_item_id, user_id, merchant_id, shop_binding_id,
  after_sale_type, reason_type, problem_description, requested_amount, status, priority, ai_category,
  reviewer_id, reviewed_at, review_opinion, final_result
) VALUES
  (1, 'AS202606250001', 1, 1, 1, 1, 1, 'RETURN_REFUND', 'PRODUCT_QUALITY', '手机收到后屏幕边缘有明显划痕，希望退货退款。', 2999.00, 'PROCESSING', 'HIGH', '商品质量问题', 3, '2026-06-25 09:40:00', '凭证清晰，先通过申请，等待用户寄回商品。', NULL);

INSERT INTO external_after_sale_mapping (
  after_sale_id, platform_code, external_after_sale_no, external_refund_no, external_status, raw_data, last_synced_at
) VALUES
  (1, 'DOUYIN', 'DYAS202606250001', 'DYRF202606250001', 'SELLER_APPROVED', JSON_OBJECT('source','mock-douyin'), '2026-06-25 10:00:00');

INSERT INTO after_sale_material (after_sale_id, user_id, material_type, material_url, description) VALUES
  (1, 1, 'IMAGE', 'https://example.com/materials/scratch-1.jpg', '屏幕划痕照片');

INSERT INTO refund_record (
  id, refund_no, after_sale_id, external_order_id, merchant_id, platform_code, external_refund_no,
  refund_amount, refund_status, reason, refunded_at, raw_data
) VALUES
  (1, 'RF202606250001', 1, 1, 1, 'DOUYIN', 'DYRF202606250001', 2999.00, 'PROCESSING', '商品质量问题', NULL, JSON_OBJECT('source','mock-douyin'));

INSERT INTO after_sale_write_back_log (
  id, after_sale_id, platform_code, shop_binding_id, action_type, request_payload, response_payload,
  status, error_message, retry_count, created_by
) VALUES
  (1, 1, 'DOUYIN', 1, 'APPROVE_RETURN_REFUND', JSON_OBJECT('afterSaleNo','AS202606250001','amount',2999.00), JSON_OBJECT('success',true,'externalStatus','SELLER_APPROVED'), 'SUCCESS', NULL, 0, 3);

INSERT INTO customer_conversation (
  id, conversation_no, user_id, merchant_id, external_order_id, assigned_staff_id, source, status,
  last_message, last_message_at, ai_intent, ai_summary, transferred_at
) VALUES
  (1, 'CV202606250001', 1, 1, 1, 3, 'MINIAPP', 'AGENT_SERVING', '好的，我已经帮您提交售后申请。', '2026-06-25 09:35:00', '退货退款', '用户反馈手机屏幕划痕，要求退货退款。', '2026-06-25 09:30:00');

INSERT INTO chat_message (
  id, conversation_id, sender_id, sender_type, message_type, content, ai_generated, ai_confidence, read_at, created_at
) VALUES
  (1, 1, 1, 'CONSUMER', 'TEXT', '手机屏幕有划痕，可以退货吗？', 0, NULL, '2026-06-25 09:30:10', '2026-06-25 09:30:00'),
  (2, 1, NULL, 'AI', 'TEXT', '根据售后规则，商品存在质量问题可提交退货退款申请，请上传商品照片作为凭证。', 1, 0.8900, '2026-06-25 09:30:15', '2026-06-25 09:30:05'),
  (3, 1, 3, 'CUSTOMER_SERVICE', 'TEXT', '您好，照片已经看到，我帮您提交售后申请并优先处理。', 0, NULL, NULL, '2026-06-25 09:35:00');

INSERT INTO quick_reply (merchant_id, title, content, scene, enabled, created_by) VALUES
  (1, '退货退款说明', '您好，若商品存在质量问题，请上传商品照片和包装照片，我们会尽快审核处理。', 'AFTER_SALE', 1, 2),
  (1, '物流查询说明', '您好，您可以在订单详情中查看最新物流轨迹，如超过预计时间未更新，我们会协助联系快递。', 'LOGISTICS', 1, 2);

INSERT INTO service_evaluation (conversation_id, user_id, merchant_id, staff_id, rating, comment) VALUES
  (1, 1, 1, 3, 5, '客服处理很及时。');

INSERT INTO ticket (
  id, ticket_no, after_sale_id, conversation_id, external_order_id, user_id, merchant_id, assigned_staff_id,
  ticket_type, title, description, status, priority, ai_category, ai_confidence, due_at
) VALUES
  (1, 'TK202606250001', 1, 1, 1, 1, 1, 3, 'AFTER_SALE', '手机屏幕划痕退货退款', '用户反馈手机屏幕边缘划痕，申请退货退款。', 'IN_PROGRESS', 'HIGH', '商品质量问题', 0.9100, '2026-06-26 18:00:00');

INSERT INTO ticket_record (ticket_id, operator_id, action_type, from_status, to_status, content) VALUES
  (1, 3, 'APPROVE_AFTER_SALE', 'OPEN', 'IN_PROGRESS', '售后申请通过，等待用户寄回商品。');

INSERT INTO review (
  id, external_order_id, external_order_item_id, user_id, merchant_id, platform_code, external_review_id,
  review_source, product_score, logistics_score, service_score, content, image_urls, anonymous, status, reviewed_at
) VALUES
  (1, 1, 1, 1, 1, 'DOUYIN', 'DYRV202606250001', 'EXTERNAL_PLATFORM', 2, 5, 5, '物流很快，客服态度也好，但是商品屏幕有划痕，希望品控改进。', JSON_ARRAY('https://example.com/reviews/review-1.jpg'), 0, 'PUBLISHED', '2026-06-24 20:00:00');

INSERT INTO review_append (review_id, content, image_urls, appended_at) VALUES
  (1, '商家已经受理售后，处理速度还可以。', NULL, '2026-06-25 10:20:00');

INSERT INTO review_analysis (
  review_id, sentiment, sentiment_score, topics, keywords, risk_level, summary
) VALUES
  (1, 'NEGATIVE', 0.7200, JSON_ARRAY('商品质量', '客服态度', '物流速度'), JSON_ARRAY('划痕', '物流快', '客服态度好'), 'MEDIUM', '评价整体偏负面，主要问题集中在商品质量。');

INSERT INTO knowledge_article (merchant_id, title, content, category, tags, status, created_by) VALUES
  (1, '手机类商品售后检查标准', '手机类商品退货前需核验外观、屏幕、序列号和配件完整性。', 'PRODUCT_POLICY', JSON_ARRAY('手机', '售后', '质检'), 'PUBLISHED', 2),
  (NULL, '平台七天无理由退货政策', '用户签收商品后七天内，符合条件的商品可申请七天无理由退货。', 'PLATFORM_POLICY', JSON_ARRAY('七天无理由', '退货'), 'PUBLISHED', 4);

INSERT INTO faq_item (merchant_id, question, answer, category, priority, enabled, created_by) VALUES
  (1, '退款多久到账？', '退款到账时间以抖音平台退款结果为准，本系统会同步展示最新退款状态。', 'REFUND', 10, 1, 2),
  (1, '商品有质量问题怎么处理？', '请在订单详情中发起售后申请，选择质量问题并上传照片凭证。', 'AFTER_SALE', 20, 1, 2);

INSERT INTO after_sale_rule (merchant_id, rule_name, rule_type, conditions_json, action_json, content, enabled, created_by) VALUES
  (1, '质量问题优先审核', 'PRIORITY', JSON_OBJECT('reasonType','PRODUCT_QUALITY'), JSON_OBJECT('priority','HIGH'), '商品质量问题默认标记为高优先级。', 1, 2),
  (NULL, '七天无理由基础规则', 'RETURN_POLICY', JSON_OBJECT('days',7), JSON_OBJECT('allowReturn',true), '签收七天内符合条件的商品支持无理由退货。', 1, 4);

INSERT INTO ai_config (
  id, merchant_id, config_name, provider, model_name, model_path, temperature, max_tokens, prompt_template, enabled, created_by
) VALUES
  (1, NULL, '平台默认售后 AI 配置', 'LOCAL', 'after-sale-rule-engine', NULL, 0.70, 1024, '请根据知识库、售后规则和订单上下文生成客服参考回复。', 1, 4),
  (2, 1, '星河数码售后 AI 配置', 'LOCAL', 'after-sale-specialist-v1', '/models/after-sale-specialist-v1', 0.60, 1024, '请以耐心、准确、合规的语气回复数码商品售后问题。', 1, 2);

INSERT INTO ai_model_version (
  id, config_id, model_code, model_name, task_type, version_no, model_path, metrics_json, status, trained_at
) VALUES
  (1, 2, 'intent-classifier', '售后意图识别模型', 'INTENT_RECOGNITION', 'v1.0.0', '/models/intent-classifier/v1', JSON_OBJECT('accuracy',0.92,'f1',0.90), 'ACTIVE', '2026-06-25 08:00:00'),
  (2, 2, 'review-sentiment', '评价情感分析模型', 'SENTIMENT_ANALYSIS', 'v1.0.0', '/models/review-sentiment/v1', JSON_OBJECT('accuracy',0.89,'f1',0.87), 'ACTIVE', '2026-06-25 08:10:00');

INSERT INTO ai_training_sample (
  model_version_id, task_type, source_type, source_id, input_text, label_json, sample_status, labeled_by
) VALUES
  (1, 'INTENT_RECOGNITION', 'CHAT_MESSAGE', 1, '手机屏幕有划痕，可以退货吗？', JSON_OBJECT('intent','RETURN_REFUND','category','PRODUCT_QUALITY'), 'LABELED', 3),
  (2, 'SENTIMENT_ANALYSIS', 'REVIEW', 1, '物流很快，客服态度也好，但是商品屏幕有划痕，希望品控改进。', JSON_OBJECT('sentiment','NEGATIVE','topics',JSON_ARRAY('商品质量')), 'LABELED', 3);

INSERT INTO ai_call_log (
  merchant_id, user_id, business_type, business_id, task_type, model_version_id,
  request_text, response_text, confidence, success, latency_ms
) VALUES
  (1, 1, 'CONVERSATION', 1, 'INTENT_RECOGNITION', 1, '手机屏幕有划痕，可以退货吗？', 'RETURN_REFUND / PRODUCT_QUALITY', 0.8900, 1, 35),
  (1, 1, 'REVIEW', 1, 'SENTIMENT_ANALYSIS', 2, '物流很快，客服态度也好，但是商品屏幕有划痕，希望品控改进。', 'NEGATIVE / 商品质量', 0.7200, 1, 42);

INSERT INTO external_api_call_log (
  platform_code, shop_binding_id, api_name, business_type, business_id, request_summary,
  response_summary, success, error_message, latency_ms
) VALUES
  ('DOUYIN', 1, 'mock.order.list', 'SYNC_TASK', 1, '同步最近订单', '同步成功，订单数 2', 1, NULL, 120),
  ('DOUYIN', 1, 'mock.after_sale.approve', 'AFTER_SALE', 1, '回写同意退货退款', '回写成功', 1, NULL, 180);

INSERT INTO operation_log (
  operator_id, merchant_id, module, action, business_type, business_id, content, ip_address
) VALUES
  (3, 1, 'AFTER_SALE', 'APPROVE', 'AFTER_SALE', 1, '客服审核通过售后申请，并回写抖音。', '127.0.0.1'),
  (2, 1, 'PLATFORM_BINDING', 'BIND_DOUYIN_SHOP', 'SHOP_BINDING', 1, '商家绑定抖音店铺。', '127.0.0.1');
