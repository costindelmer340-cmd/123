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
  { id: 2, afterSaleNo: 'AS202606250002', afterSaleType: 'REFUND_ONLY', reasonType: 'PRODUCT_QUALITY', requestedAmount: 69, status: 'PROCESSING', priority: 'NORMAL', reviewOpinion: '已进入人工复核', writeBackStatus: 'PENDING', createdAt: '2026-06-27 09:30:00' }
]

export const conversations = [
  {
    id: 1,
    conversationNo: 'CV202606250001',
    userId: 1,
    orderNo: 'DY202606250001',
    productName: 'Aurora X1 智能手机',
    merchantName: '星链数码旗舰店',
    afterSaleStatus: '处理中',
    status: 'AGENT_SERVING',
    lastMessage: '退款进度在哪里看？',
    lastMessageAt: '2026-06-25 09:35:00',
    aiIntent: '退货退款'
  },
  {
    id: 2,
    conversationNo: 'CV202606250002',
    userId: 8,
    orderNo: 'DY202606250002',
    productName: 'Breeze Pods 无线耳机',
    merchantName: 'Breeze 声学专营店',
    afterSaleStatus: '未申请',
    status: 'AI_SERVING',
    lastMessage: '快递三天没更新了',
    lastMessageAt: '2026-06-25 12:05:00',
    aiIntent: '物流查询'
  }
]

export const tickets = [
  { id: 1, ticketNo: 'TK202606250001', title: '手机屏幕划痕退货退款', ticketType: 'AFTER_SALE', status: 'IN_PROGRESS', priority: 'HIGH', assignee: '售后主管', flowRemark: '等待商家复核', createdAt: '2026-06-25 09:30:00', dueAt: '2026-06-26 18:00:00' },
  { id: 2, ticketNo: 'TK202606250002', title: '物流长时间未更新', ticketType: 'CONSULT', status: 'OPEN', priority: 'NORMAL', assignee: '客服一组', flowRemark: '待客服接入', createdAt: '2026-06-25 11:20:00', dueAt: '2026-06-26 12:00:00' }
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
  { id: 201, question: '退款多久到账？', answer: '退款申请审核通过后，款项通常会在1到7个工作日内按原支付渠道退回。具体到账时间会受到支付渠道、银行处理速度和平台结算规则影响，用户可以在订单详情或支付账户中查看退款进度。', category: 'REFUND', priority: 10, enabled: true, createdAt: '2026-06-25 09:20:00' },
  { id: 202, question: '怎么申请退货退款？', answer: '用户可以进入订单详情页，点击申请售后，选择退货退款类型，填写申请原因、退款金额并上传必要凭证。商家审核通过后，系统会展示退货地址和寄回要求，用户按要求寄回商品后等待商家验收退款。', category: 'AFTER_SALE', priority: 20, enabled: true, createdAt: '2026-06-25 09:21:00' },
  { id: 203, question: '商品有质量问题怎么办？', answer: '如果商品存在破损、故障、瑕疵或与描述不符等质量问题，用户可以在订单详情中发起售后申请，并上传商品照片、视频或检测凭证。商家核实后会根据情况提供退货退款、换货或维修服务。', category: 'AFTER_SALE', priority: 30, enabled: true, createdAt: '2026-06-25 09:22:00' },
  { id: 204, question: '可以只退款不退货吗？', answer: '未发货、未收到货、物流异常或商家同意无需退回商品时，可以申请仅退款。如果商品已经签收且需要退回，一般应选择退货退款。具体结果需要商家结合订单状态、物流状态和售后原因审核。', category: 'REFUND', priority: 40, enabled: true, createdAt: '2026-06-25 09:23:00' },
  { id: 205, question: '退货运费谁承担？', answer: '如果退货原因属于商品质量问题、错发、漏发或商家责任，退货运费通常由商家承担。如果是用户个人原因，例如不喜欢、拍错、尺码不合适等，退货运费通常由用户承担；订单包含运费险时可按运费险规则理赔。', category: 'LOGISTICS', priority: 50, enabled: true, createdAt: '2026-06-25 09:24:00' },
  { id: 206, question: '运费险怎么理赔？', answer: '订单包含运费险时，用户在退货寄出后需要填写正确的退货物流单号。保险服务方会根据物流轨迹和理赔规则自动判断是否赔付，赔付金额通常根据收发地址和保险规则计算，不一定等于实际运费。', category: 'LOGISTICS', priority: 60, enabled: true, createdAt: '2026-06-25 09:25:00' },
  { id: 207, question: '申请售后后多久审核？', answer: '商家通常会在24到48小时内完成售后审核。若申请原因复杂、凭证不完整或需要人工复核，处理时间可能延长。用户可以在订单详情或售后详情中查看当前审核状态。', category: 'AFTER_SALE', priority: 70, enabled: true, createdAt: '2026-06-25 09:26:00' },
  { id: 208, question: '售后申请被拒绝怎么办？', answer: '用户可以先查看商家给出的拒绝原因。如果是凭证不完整、原因填写不清楚或商品信息不足，可以补充图片、视频、物流凭证后修改售后申请；如果双方无法协商一致，可以申请平台介入处理。', category: 'AFTER_SALE', priority: 80, enabled: true, createdAt: '2026-06-25 09:27:00' },
  { id: 209, question: '怎么查看售后进度？', answer: '用户可以进入订单详情页查看售后状态，也可以在客服页面咨询对应订单的处理进度。系统会展示待审核、处理中、已结束等状态，并根据商家处理结果更新售后进展。', category: 'AFTER_SALE', priority: 90, enabled: true, createdAt: '2026-06-25 09:28:00' },
  { id: 210, question: '商品已经发货还能退款吗？', answer: '商品已经发货但尚未签收时，可以根据物流状态申请退款或与商家协商拒收。若商品已经在运输途中，商家可能需要等待商品退回后再退款；若已经签收，一般需要按退货退款流程处理。', category: 'REFUND', priority: 100, enabled: true, createdAt: '2026-06-25 09:29:00' },
  { id: 211, question: '商品已经签收还能退吗？', answer: '商品签收后，如果符合7天无理由退货条件，或存在质量问题、错发漏发等情况，可以申请退货退款。特殊商品、已影响二次销售的商品或超过售后期限的商品，可能无法直接退货。', category: 'RETURN', priority: 110, enabled: true, createdAt: '2026-06-25 09:30:00' },
  { id: 212, question: '价保怎么申请？', answer: '如果商品在价保周期内出现降价，用户可以在订单详情中申请价格保护。系统或商家会核验商品规格、购买价格、降价时间和活动条件，符合规则时会按差价进行补偿。', category: 'PRICE_PROTECTION', priority: 120, enabled: true, createdAt: '2026-06-25 09:31:00' },
  { id: 213, question: '换货需要重新下单吗？', answer: '一般情况下，换货不需要用户重新下单。用户提交换货申请并通过商家审核后，按要求寄回原商品，商家验收后会安排发出新商品。具体换货方式以商家审核结果为准。', category: 'EXCHANGE', priority: 130, enabled: true, createdAt: '2026-06-25 09:32:00' },
  { id: 214, question: '维修需要多久？', answer: '维修时间会根据商品类型、故障情况、维修点检测结果和配件供应情况确定。用户提交维修申请后，商家或售后服务方会给出预计处理周期，用户可以在售后详情中查看进度。', category: 'REPAIR', priority: 140, enabled: true, createdAt: '2026-06-25 09:33:00' },
  { id: 215, question: '退货地址在哪里看？', answer: '退货地址会在商家审核通过后显示在售后详情中。用户应按照页面展示的地址、收件人和联系电话寄回商品，并及时填写正确的物流单号，避免影响退款处理。', category: 'LOGISTICS', priority: 150, enabled: true, createdAt: '2026-06-25 09:34:00' },
  { id: 216, question: '退货需要保留包装吗？', answer: '建议用户保留商品原包装、配件、说明书、吊牌、赠品和发票等材料。对于影响二次销售的商品，包装或配件缺失可能导致商家拒绝退货，或与用户协商部分退款。', category: 'RETURN', priority: 160, enabled: true, createdAt: '2026-06-25 09:35:00' },
  { id: 217, question: '赠品需要一起退回吗？', answer: '如果订单退回主商品，赠品通常也需要一并退回。若赠品未退回、损坏或影响再次销售，商家可能根据规则扣除相应金额，或与用户协商处理方式。', category: 'RETURN', priority: 170, enabled: true, createdAt: '2026-06-25 09:36:00' },
  { id: 218, question: '拒收后什么时候退款？', answer: '用户拒收商品后，商家通常需要等待物流将商品退回并完成验收，确认商品状态无异常后再处理退款。若拒收原因属于商品破损、错发或配送异常，商家应优先协助用户完成退款。', category: 'REFUND', priority: 180, enabled: true, createdAt: '2026-06-25 09:37:00' },
  { id: 219, question: '可以修改售后申请吗？', answer: '在售后待审核或处理中阶段，用户通常可以修改售后类型、申请原因、退款金额或补充凭证。若售后已结束，可能无法直接修改，需要重新联系商家或申请平台介入。', category: 'AFTER_SALE', priority: 190, enabled: true, createdAt: '2026-06-25 09:38:00' },
  { id: 220, question: '怎么联系人工客服？', answer: '用户可以在客服页面输入“转人工”“人工客服”等关键词，系统会将当前订单会话从AI客服切换为人工客服。转人工后，商家客服可以在商家端实时查看并回复消息。', category: 'CUSTOMER_SERVICE', priority: 200, enabled: true, createdAt: '2026-06-25 09:39:00' }
]

export const rules = [
  { id: 101, ruleName: '7天无理由退货', ruleType: 'RETURN_POLICY', content: '消费者自商品签收之日起7天内，在商品完好、不影响二次销售、配件和赠品齐全的情况下，可以申请无理由退货。特殊商品、定制商品、拆封后影响安全或卫生的商品不适用该政策。商家审核通过后，消费者需按要求寄回商品，商家确认收货并验收无误后完成退款。', enabled: true, createdAt: '2026-06-25 09:00:00' },
  { id: 102, ruleName: '质量问题退换货政策', ruleType: 'QUALITY_POLICY', content: '商品存在破损、功能异常、材质瑕疵、与页面描述明显不符等质量问题时，消费者可申请退货退款、换货或维修。消费者需要提供清晰图片、视频或检测凭证，商家应优先审核处理。经核实属于质量问题的，退货运费、换货运费及相关售后成本由商家承担。', enabled: true, createdAt: '2026-06-25 09:05:00' },
  { id: 103, ruleName: '仅退款政策', ruleType: 'REFUND_POLICY', content: '订单未发货、商家超时未履约、物流长期无更新、商品未收到或双方协商无需退回商品时，消费者可申请仅退款。商家应结合订单状态、物流轨迹和沟通记录进行审核。审核通过后，退款将按原支付渠道退回；如商品已发出且消费者已签收，一般不直接适用仅退款。', enabled: true, createdAt: '2026-06-25 09:10:00' },
  { id: 104, ruleName: '退货退款政策', ruleType: 'RETURN_REFUND_POLICY', content: '消费者收到商品后需要退回商品并申请退款的，适用退货退款政策。商家审核通过后，消费者应在规定时间内填写退货物流单号并寄回商品。商家收到退货后需检查商品、包装、配件、赠品和使用痕迹，确认符合退款条件后完成退款；若商品影响二次销售，商家可拒绝或协商部分退款。', enabled: true, createdAt: '2026-06-25 09:15:00' },
  { id: 105, ruleName: '维修政策', ruleType: 'REPAIR_POLICY', content: '电子产品、家电、数码配件等具备质保服务的商品，在质保期内出现非人为损坏的性能故障时，消费者可申请维修。消费者需提供订单信息、故障描述及必要凭证。商家或售后服务方确认故障后安排维修，维修周期、寄送方式、费用承担以商品质保说明和平台规则为准。', enabled: true, createdAt: '2026-06-25 09:20:00' },
  { id: 106, ruleName: '价保政策', ruleType: 'PRICE_PROTECTION_POLICY', content: '消费者购买商品后，在平台或商家承诺的价保周期内，如同一商品、同一规格、同一销售渠道出现实际成交价降低，可申请价格保护。系统或商家需核验订单金额、降价时间、商品规格和活动条件。符合条件的，按差价规则退还差额；秒杀、限时补贴、赠品变化等特殊活动可不纳入价保。', enabled: true, createdAt: '2026-06-25 09:25:00' },
  { id: 107, ruleName: '运费险政策', ruleType: 'FREIGHT_INSURANCE_POLICY', content: '订单包含运费险时，消费者退货寄出并填写有效物流单号后，保险服务方会根据物流信息和理赔规则自动判断是否赔付。运费险赔付金额通常按照收发地、商品类型和保险规则计算，不一定等于消费者实际支付运费。因商家责任导致退货的，超出理赔部分可由商家协商承担。', enabled: true, createdAt: '2026-06-25 09:30:00' },
  { id: 108, ruleName: '特殊商品售后政策', ruleType: 'SPECIAL_GOODS_POLICY', content: '生鲜食品、定制商品、虚拟商品、贴身衣物、已拆封且影响安全或卫生的商品等，属于特殊商品范围。此类商品是否支持退换货，应以商品页面说明、平台规则和法律规定为准。若存在质量问题、错发漏发或商家责任，消费者仍可提交凭证申请售后，由商家按实际情况审核处理。', enabled: true, createdAt: '2026-06-25 09:35:00' },
  { id: 109, ruleName: '拒收商品处理政策', ruleType: 'REJECT_RECEIVE_POLICY', content: '消费者拒收商品后，商家需根据物流退回状态判断处理结果。若商品已退回并验收无误，可按规则退款；若商品未退回、物流异常或因消费者个人原因拒收产生费用，商家可与消费者协商运费承担方式。因商品破损、错发、超时配送等商家或物流原因导致拒收的，应优先保障消费者退款权益。', enabled: true, createdAt: '2026-06-25 09:40:00' },
  { id: 110, ruleName: '平台介入政策', ruleType: 'PLATFORM_INTERVENTION_POLICY', content: '当消费者与商家对售后责任、退款金额、退货条件、凭证有效性等问题无法达成一致时，消费者或商家可申请平台介入。平台将结合订单信息、聊天记录、物流轨迹、商品凭证和售后规则进行判定。平台判定结果作为售后处理依据，商家和消费者应按平台要求完成退款、退货、补偿或关闭售后。', enabled: true, createdAt: '2026-06-25 09:45:00' }
]

export const platformBindings = []

export const syncTasks = [
  { id: 1, taskType: 'ORDER_SYNC', taskName: '订单同步', enabled: true, lastRunAt: '2026-06-25 10:00:00', nextRunAt: '2026-06-25 10:30:00' },
  { id: 2, taskType: 'AFTER_SALE_SYNC', taskName: '售后同步', enabled: true, lastRunAt: '2026-06-25 10:00:00', nextRunAt: '2026-06-25 10:10:00' },
  { id: 3, taskType: 'REVIEW_SYNC', taskName: '评价同步', enabled: true, lastRunAt: '2026-06-25 10:00:00', nextRunAt: '2026-06-25 12:00:00' }
]
