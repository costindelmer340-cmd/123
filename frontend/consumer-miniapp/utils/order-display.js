export const orderDisplayOverrides = {
  TM202606270001: {
    image: "/assets/products/twenty-keyboard-real.png",
    merchant: "极光外设旗舰店",
    orderedAt: "2026.6.26 10:00:00",
    deliveredAt: "2026.6.27 09:16:35",
    policyTags: ["7天无理由退货", "运费险"],
    description: "这款87键机械键盘采用紧凑配列设计，适合桌面空间有限的办公和游戏场景。键帽字符清晰，支持背光效果，白灰配色更偏清爽桌搭风格；热插拔结构方便后续更换轴体，适合作为日常输入、学习和轻度游戏键盘使用。",
    features: ["87键布局", "热插拔", "背光键盘"]
  },
  TM202606270002: {
    image: "/assets/products/twenty-backpack-real.png",
    merchant: "黑曜通勤箱包店",
    orderedAt: "2026.6.26 12:00:00",
    deliveredAt: "",
    policyTags: ["7天无理由退货", "运费险", "15天价格保护"],
    description: "这款城市通勤背包主打简洁商务外观，包身挺括，适合通勤、上课和短途出行。内部可放置日常数码设备、文件和随身物品，深海蓝配色低调耐看，防泼水面料能应对轻微雨水和日常溅水。",
    features: ["防泼水", "通勤背包", "多场景收纳"]
  },
  TM202606270003: {
    image: "/assets/products/twenty-lamp.png",
    merchant: "北欧灯饰生活馆",
    orderedAt: "2026.6.27 14:18:09",
    deliveredAt: "2026.6.28 16:42:31",
    policyTags: ["7天无理由退货", "运费险"],
    description: "这款护眼台灯采用温暖柔和的光源设计，适合卧室、书桌和床头阅读场景。灯罩造型偏北欧复古风格，光线扩散均匀，能减少直射刺眼感，适合夜间阅读、学习和营造居家氛围。",
    features: ["柔和护眼", "北欧风格", "阅读照明"]
  },
  TM202606270004: {
    image: "/assets/products/twenty-cup.png",
    merchant: "米家生活专营店",
    orderedAt: "2026.6.27 15:36:22",
    deliveredAt: "2026.6.28 18:05:47",
    policyTags: ["7天无理由退货", "15天价格保护"],
    description: "这款便携保温杯主打轻量出行和日常通勤使用，480mL 容量适合随身携带。杯身简洁耐看，内外纯钛材质更适合装水、茶饮等日常饮品，适合办公室、校园、旅行和户外短途场景。",
    features: ["480mL", "便携保温", "通勤出行"]
  }
}

function formatDateTime(value) {
  if (!value) return ""
  const text = String(value).trim()
  const matched = text.match(/^(\d{4})[-.](\d{1,2})[-.](\d{1,2})\s+(\d{2}:\d{2}:\d{2})/)
  if (!matched) return text
  return `${matched[1]}.${Number(matched[2])}.${Number(matched[3])} ${matched[4]}`
}

export function enrichOrderDisplay(order) {
  const extra = orderDisplayOverrides[order.no] || {}
  const statusText = extra.status || order.status || ""
  const deliveredAt = extra.deliveredAt || order.deliveredAt || order.receivedAt || ""
  return {
    ...order,
    image: extra.image || order.image,
    merchant: extra.merchant || order.merchant || "20商城演示店铺",
    status: statusText,
    orderedAt: formatDateTime(extra.orderedAt || order.orderedAt || order.orderTime || ""),
    deliveredAt: statusText === "已完成" || statusText === "已收货" ? formatDateTime(deliveredAt) : "",
    policyTags: extra.policyTags || order.policyTags || [],
    description: extra.description || "商品信息来自外部电商平台同步，当前商品支持按订单发起售后申请，可结合订单状态、商品规格和平台规则进行处理。",
    features: extra.features || ["平台订单", "支持售后", "同步商品"]
  }
}
