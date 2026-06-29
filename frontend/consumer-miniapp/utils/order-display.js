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
