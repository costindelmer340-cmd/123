import { orders } from "../../utils/mock"
import { getTwentyMallBinding } from "../../utils/auth"
import { enrichOrderDisplay } from "../../utils/order-display"

Page({
  data: {
    product: null,
    afterSaleDetail: null,
    afterSaleDetailVisible: false,
    reviewVisible: false,
    reviewScore: 5,
    reviewText: "",
    reviewStars: [
      { value: 1, active: true },
      { value: 2, active: true },
      { value: 3, active: true },
      { value: 4, active: true },
      { value: 5, active: true }
    ]
  },
  onLoad(options) {
    const binding = getTwentyMallBinding()
    if (binding && binding.platform === "20商城" && options.no && options.no.startsWith("TM")) {
      wx.request({
        url: `http://localhost:8080/api/twenty-mall/consumer/orders/detail?orderNo=${options.no}`,
        success: (res) => {
          const item = res.data && res.data.data
          if (item) {
            this.setData({
              product: enrichOrderDisplay({
                no: item.no,
                title: item.title,
                status: item.status,
                afterSale: item.afterSale,
                platform: "20商城",
                merchant: item.merchant || "20商城演示店铺",
                price: item.price,
                image: item.image,
                spec: item.spec,
                orderedAt: item.orderedAt,
                deliveredAt: item.deliveredAt,
                policyTags: item.policyTags,
                service: item.afterSale === "未申请" ? "可申请售后" : "售后处理中"
              }),
              afterSaleDetail: this.buildStoredAfterSaleDetail(item.no, item.afterSale)
            })
          }
        }
      })
      return
    }
    const product = enrichOrderDisplay(orders.find((item) => item.no === options.no) || orders[0])
    this.setData({ product, afterSaleDetail: this.buildStoredAfterSaleDetail(product.no, product.afterSale) })
  },
  handleAfterSalePrimary() {
    if (!this.data.product) return
    if (this.hasAppliedAfterSale()) {
      this.setData({ afterSaleDetailVisible: true })
      return
    }
    this.submitAfterSale(false)
  },
  modifyAfterSale() {
    this.submitAfterSale(true)
  },
  closeAfterSaleDetail() {
    this.setData({ afterSaleDetailVisible: false })
  },
  submitAfterSale(isModify) {
    if (!this.data.product) return
    wx.showActionSheet({
      itemList: ["仅退款", "退货退款", "价保"],
      success: (res) => {
        const type = ["仅退款", "退货退款", "价保"][res.tapIndex]
        const typeMap = {
          "仅退款": "REFUND_ONLY",
          "退货退款": "RETURN_REFUND",
          "价保": "PRICE_PROTECTION"
        }
        const reasonMap = {
          "仅退款": "PRODUCT_QUALITY",
          "退货退款": "PRODUCT_QUALITY",
          "价保": "PRICE_PROTECTION"
        }
        wx.showLoading({ title: "提交中" })
        wx.request({
          url: "http://localhost:8080/api/twenty-mall/consumer/after-sales",
          method: "POST",
          header: { "Content-Type": "application/json" },
          data: {
            orderNo: this.data.product.no,
            afterSaleType: typeMap[type],
            reasonType: reasonMap[type],
            description: `消费者选择${type}，由消费者端小程序提交售后申请。`
          },
          success: (response) => {
            const payload = response.data || {}
            if (payload.code !== "200") {
              wx.showToast({ title: payload.message || "提交失败", icon: "none" })
              return
            }
            const product = {
              ...this.data.product,
              afterSale: "处理中",
              service: "售后处理中"
            }
            const afterSaleDetail = {
              orderNo: product.no,
              productName: product.title,
              merchantName: product.merchant,
              status: product.afterSale,
              type,
              appliedAt: this.formatNow()
            }
            wx.setStorageSync(`afterSaleDetail:${product.no}`, afterSaleDetail)
            this.setData({ product, afterSaleDetail })
            wx.showToast({ title: isModify ? "售后已修改" : "售后已提交", icon: "success" })
          },
          fail: () => {
            wx.showToast({ title: "请先启动后端服务", icon: "none" })
          },
          complete: () => {
            wx.hideLoading()
          }
        })
      }
    })
  },
  hasAppliedAfterSale() {
    return this.data.product && this.data.product.afterSale && this.data.product.afterSale !== "未申请"
  },
  buildStoredAfterSaleDetail(orderNo, afterSaleStatus) {
    const stored = wx.getStorageSync(`afterSaleDetail:${orderNo}`)
    if (stored) {
      return stored
    }
    if (!afterSaleStatus || afterSaleStatus === "未申请") {
      return null
    }
    return {
      orderNo,
      productName: "",
      merchantName: "",
      status: afterSaleStatus,
      type: "退货退款",
      appliedAt: "2026.6.29 10:35:23"
    }
  },
  formatNow() {
    const date = new Date()
    const pad = (value) => String(value).padStart(2, "0")
    return `${date.getFullYear()}.${date.getMonth() + 1}.${date.getDate()} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  },
  contactMerchant() {
    if (!this.data.product) return
    wx.setStorageSync("pendingChatOrderNo", this.data.product.no)
    wx.switchTab({ url: "/pages/chat/index" })
  },
  openReviewDialog() {
    this.setData({
      reviewVisible: true,
      reviewScore: 5,
      reviewText: "",
      reviewStars: this.buildStars(5)
    })
  },
  closeReviewDialog() {
    this.setData({ reviewVisible: false })
  },
  setReviewScore(event) {
    const score = Number(event.currentTarget.dataset.score)
    this.setData({
      reviewScore: score,
      reviewStars: this.buildStars(score)
    })
  },
  onReviewInput(event) {
    this.setData({ reviewText: event.detail.value })
  },
  submitReview() {
    if (!this.data.reviewText.trim()) {
      wx.showToast({ title: "请输入评价内容", icon: "none" })
      return
    }
    wx.showToast({ title: "评价已提交", icon: "success" })
    this.setData({ reviewVisible: false })
  },
  buildStars(score) {
    return [1, 2, 3, 4, 5].map((value) => ({
      value,
      active: value <= score
    }))
  }
})
