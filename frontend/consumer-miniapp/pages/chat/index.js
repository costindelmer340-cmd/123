import { getPrimaryPhone, getTwentyMallBindings } from "../../utils/auth"
import { enrichOrderDisplay } from "../../utils/order-display"

const API_BASE = "http://localhost:8080/api/demo-chat"

function formatMessageTime(value) {
  if (!value) return ""
  const text = String(value).replace("T", " ").replace(/\.\d+$/, "")
  const match = text.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\s+(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?/)
  if (!match) return text
  const [, year, month, day, hour, minute, second = "00"] = match
  return `${year}.${Number(month)}.${Number(day)} ${hour.padStart(2, "0")}:${minute.padStart(2, "0")}:${second.padStart(2, "0")}`
}

function normalizeMessage(item) {
  const senderType = item.senderType || "AI"
  const isUser = senderType === "CONSUMER"
  const isStaff = senderType === "STAFF"
  return {
    id: item.id,
    role: isUser ? "user" : (isStaff ? "staff" : "ai"),
    speaker: isUser ? "我" : (isStaff ? "人工客服" : "AI客服"),
    time: formatMessageTime(item.createdAt),
    content: item.content
  }
}

Page({
  data: {
    inputValue: "",
    mode: "AI",
    platformBound: false,
    orders: [],
    activeOrderNo: "",
    consultingOrder: null,
    messages: []
  },
  onLoad() {
    this.applyPlatformBinding()
  },
  onShow() {
    this.applyPlatformBinding()
    this.startPolling()
  },
  onHide() {
    this.stopPolling()
  },
  onUnload() {
    this.stopPolling()
  },
  onInput(e) {
    this.setData({ inputValue: e.detail.value })
  },
  applyPlatformBinding() {
    const bindings = getTwentyMallBindings()
    if (!bindings.length) {
      this.setData({
        platformBound: false,
        orders: [],
        activeOrderNo: "",
        consultingOrder: null,
        messages: [],
        inputValue: "",
        mode: "AI"
      })
      return
    }
    const requests = bindings.map((binding) => new Promise((resolve) => {
      wx.request({
        url: `http://localhost:8080/api/twenty-mall/consumer/orders?accountNo=${binding.accountNo}`,
        success: (res) => {
          const list = (res.data && res.data.data) || []
          resolve(list.map((item) => enrichOrderDisplay({
            no: item.no,
            title: item.title,
            status: item.status,
            afterSale: item.afterSale,
            platform: "20商城",
            accountNo: binding.accountNo,
            merchant: item.merchant,
            price: item.price,
            image: item.image,
            spec: item.spec,
            service: item.afterSale === "未申请" ? "可申请售后" : "售后处理中"
          })))
        },
        fail: () => resolve([])
      })
    }))
    Promise.all(requests).then((result) => {
      const nextOrders = result.reduce((all, list) => all.concat(list), [])
      if (!nextOrders.length) {
        this.setData({
          platformBound: true,
          orders: [],
          activeOrderNo: "",
          consultingOrder: null,
          messages: []
        })
        return
      }
      const pendingOrderNo = wx.getStorageSync("pendingChatOrderNo")
      const activeOrder = nextOrders.find((item) => item.no === pendingOrderNo)
        || nextOrders.find((item) => item.no === this.data.activeOrderNo)
        || nextOrders[0]
      if (pendingOrderNo) {
        wx.removeStorageSync("pendingChatOrderNo")
      }
      this.setData({
        platformBound: true,
        orders: nextOrders,
        activeOrderNo: activeOrder.no,
        consultingOrder: activeOrder
      })
      this.loadConversation()
      this.startPolling()
    })
  },
  goOrderDetail() {
    if (!this.data.consultingOrder) return
    wx.navigateTo({ url: `/pages/product/index?no=${this.data.consultingOrder.no}` })
  },
  switchOrder(e) {
    const no = e.currentTarget.dataset.no
    const order = this.data.orders.find((item) => item.no === no)
    if (!order) return
    this.setData({
      activeOrderNo: no,
      consultingOrder: order,
      mode: "AI",
      inputValue: "",
      messages: []
    })
    this.loadConversation()
  },
  goBind() {
    wx.switchTab({ url: "/pages/home/index" })
  },
  sendMessage() {
    if (!this.data.platformBound || !this.data.activeOrderNo) {
      wx.showToast({ title: "请先绑定电商平台", icon: "none" })
      return
    }
    const value = this.data.inputValue.trim()
    if (!value) return
    const wantsHuman = value.includes("人工") || value.includes("客服")
    const no = this.data.activeOrderNo
    this.setData({ inputValue: "", mode: wantsHuman ? "人工" : this.data.mode })
    wx.request({
      url: `${API_BASE}/conversations/${no}/messages`,
      method: "POST",
      data: {
        senderType: "CONSUMER",
        content: value
      },
      success: () => {
        if (wantsHuman) {
          this.transferToStaff()
          return
        }
        this.loadMessages()
      },
      fail: () => {
        wx.showToast({ title: "消息发送失败，请确认后端已启动", icon: "none" })
      }
    })
  },
  transferToStaff() {
    wx.request({
      url: `${API_BASE}/conversations/${this.data.activeOrderNo}/transfer`,
      method: "POST",
      success: () => {
        this.setData({ mode: "人工" })
        this.loadMessages()
      },
      fail: () => {
        wx.showToast({ title: "转人工失败，请稍后重试", icon: "none" })
      }
    })
  },
  loadConversation() {
    if (!this.data.platformBound || !this.data.activeOrderNo) return
    wx.request({
      url: `${API_BASE}/conversations/${this.data.activeOrderNo}`,
      success: (res) => {
        const data = res.data && res.data.data
        if (data) {
          this.setData({ mode: data.status === "AGENT_SERVING" ? "人工" : "AI" })
        }
        this.loadMessages()
      },
      fail: () => this.loadMessages()
    })
  },
  loadMessages() {
    if (!this.data.platformBound || !this.data.activeOrderNo) return
    wx.request({
      url: `${API_BASE}/conversations/${this.data.activeOrderNo}/messages`,
      success: (res) => {
        const list = (res.data && res.data.data) || []
        const messages = list.map(normalizeMessage)
        const latestUserMessage = messages.filter((item) => item.role === "user" && item.time).pop()
        if (latestUserMessage) {
          wx.setStorageSync(`consumerLastConsultAt:${getPrimaryPhone()}`, latestUserMessage.time)
        }
        this.setData({ messages })
      }
    })
  },
  startPolling() {
    this.stopPolling()
    if (!this.data.platformBound || !this.data.activeOrderNo) return
    this.pollingTimer = setInterval(() => this.loadConversation(), 2500)
  },
  stopPolling() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer)
      this.pollingTimer = null
    }
  }
})
