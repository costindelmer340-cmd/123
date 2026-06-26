export const metrics = [
  { label: '待处理售后', value: '18', trend: '+6', tone: 'warning' },
  { label: '今日会话', value: '126', trend: '+22', tone: 'primary' },
  { label: '高风险评价', value: '7', trend: '-3', tone: 'danger' },
  { label: '工单按时率', value: '94%', trend: '+4%', tone: 'success' }
]

export const orders = [
  { id: 1, externalOrderNo: 'DY202606250001', buyerMaskedName: '林晓雨', orderStatus: 'COMPLETED', payStatus: 'PAID', logisticsStatus: 'RECEIVED', afterSaleStatus: 'AFTER_SALE', totalAmount: 2999, orderedAt: '2026-06-20 10:00:00' },
  { id: 2, externalOrderNo: 'DY202606250002', buyerMaskedName: '陈明轩', orderStatus: 'SHIPPED', payStatus: 'PAID', logisticsStatus: 'IN_TRANSIT', afterSaleStatus: 'NONE', totalAmount: 399, orderedAt: '2026-06-24 10:50:00' }
]

export const afterSales = [
  { id: 1, afterSaleNo: 'AS202606250001', afterSaleType: 'RETURN_REFUND', reasonType: 'PRODUCT_QUALITY', requestedAmount: 2999, status: 'PROCESSING', priority: 'HIGH', reviewOpinion: '已进入人工复核', writeBackStatus: 'PENDING', createdAt: '2026-06-25 09:30:00' },
  { id: 2, afterSaleNo: 'AS202606250002', afterSaleType: 'REFUND_ONLY', reasonType: 'LOGISTICS_DELAY', requestedAmount: 39, status: 'PENDING_REVIEW', priority: 'NORMAL', reviewOpinion: '待审核', writeBackStatus: 'WAITING', createdAt: '2026-06-25 11:20:00' }
]

export const conversations = [
  { id: 1, conversationNo: 'CV202606250001', userId: 1, status: 'AGENT_SERVING', lastMessage: '好的，我已经帮您提交售后申请。', lastMessageAt: '2026-06-25 09:35:00', aiIntent: '退货退款' },
  { id: 2, conversationNo: 'CV202606250002', userId: 8, status: 'AI_SERVING', lastMessage: '快递三天没更新了', lastMessageAt: '2026-06-25 12:05:00', aiIntent: '物流查询' }
]

export const tickets = [
  { id: 1, ticketNo: 'TK202606250001', title: '手机屏幕划痕退货退款', ticketType: 'AFTER_SALE', status: 'IN_PROGRESS', priority: 'HIGH', assignee: '售后主管', flowRemark: '等待商家复核', dueAt: '2026-06-26 18:00:00' },
  { id: 2, ticketNo: 'TK202606250002', title: '物流长时间未更新', ticketType: 'CONSULT', status: 'OPEN', priority: 'NORMAL', assignee: '客服一组', flowRemark: '待客服接入', dueAt: '2026-06-26 12:00:00' }
]

export const reviews = [
  { id: 1, platformCode: 'DOUYIN', productScore: 2, serviceScore: 5, content: '物流很快，客服态度也好，但是商品屏幕有划痕，希望品控改进。', status: 'PUBLISHED', sentiment: 'NEGATIVE', riskLevel: 'MEDIUM', keywords: '屏幕划痕、品控', analysisSummary: '用户认可物流和客服，但对商品质量不满。', suggestion: '优先联系用户补偿或换货，并同步质检排查。' },
  { id: 2, platformCode: 'DOUYIN', productScore: 5, serviceScore: 5, content: '处理很及时，售后体验不错。', status: 'PUBLISHED', sentiment: 'POSITIVE', riskLevel: 'LOW', keywords: '处理及时、售后体验', analysisSummary: '用户对售后效率和服务态度满意。', suggestion: '可沉淀为优秀服务案例。' }
]

export const articles = [
  { id: 1, title: '手机类商品售后检查标准', category: 'PRODUCT_POLICY', status: 'PUBLISHED', createdAt: '2026-06-25 09:00:00' },
  { id: 2, title: '七天无理由退货政策', category: 'PLATFORM_POLICY', status: 'PUBLISHED', createdAt: '2026-06-24 16:00:00' }
]

export const faqs = [
  { id: 1, question: '退款多久到账？', answer: '退款到账时间以抖音平台退款结果为准。', category: 'REFUND', priority: 10, enabled: true, createdAt: '2026-06-25 09:20:00' },
  { id: 2, question: '商品有质量问题怎么处理？', answer: '请在订单详情中发起售后申请，并上传照片凭证。', category: 'AFTER_SALE', priority: 20, enabled: true, createdAt: '2026-06-25 09:10:00' }
]

export const rules = [
  { id: 1, ruleName: '质量问题优先审核', ruleType: 'PRIORITY', content: '商品质量问题默认标记为高优先级。', enabled: true, createdAt: '2026-06-25 09:00:00' },
  { id: 2, ruleName: '七天无理由基础规则', ruleType: 'RETURN_POLICY', content: '签收七天内符合条件的商品支持无理由退货。', enabled: true, createdAt: '2026-06-24 16:00:00' }
]

export const platformBindings = [
  {
    id: 1,
    platformCode: 'DOUYIN',
    platformName: '抖音电商',
    authStatus: 'ACTIVE',
    externalShopId: 'DY_SHOP_10001',
    shopName: '星河数码抖音旗舰店',
    sellerNick: '星河数码官方',
    lastSyncedAt: '2026-06-25 10:00:00'
  }
]

export const syncTasks = [
  { id: 1, taskType: 'ORDER_SYNC', taskName: '订单同步', enabled: true, lastRunAt: '2026-06-25 10:00:00', nextRunAt: '2026-06-25 10:30:00' },
  { id: 2, taskType: 'AFTER_SALE_SYNC', taskName: '售后同步', enabled: true, lastRunAt: '2026-06-25 10:00:00', nextRunAt: '2026-06-25 10:10:00' },
  { id: 3, taskType: 'REVIEW_SYNC', taskName: '评价同步', enabled: true, lastRunAt: '2026-06-25 10:00:00', nextRunAt: '2026-06-25 12:00:00' }
]
