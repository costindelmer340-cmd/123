import { orders } from "../../utils/mock"
import { getTwentyMallBinding } from "../../utils/auth"

Page({
  data: {
    product: null
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
              product: {
                no: item.no,
                title: item.title,
                status: item.status,
                afterSale: item.afterSale,
                platform: "20商城",
                merchant: "20商城演示店铺",
                price: item.price,
                image: item.image,
                spec: item.spec,
                service: item.afterSale === "未申请" ? "可申请售后" : "售后处理中"
              }
            })
          }
        }
      })
      return
    }
    const product = orders.find((item) => item.no === options.no) || orders[0]
    this.setData({ product })
  },
  applyAfterSale() {
    wx.showModal({
      title: "发起售后",
      content: "当前将从订单页面进入售后申请流程，真实提交接口后续接入。",
      confirmText: "确认",
      showCancel: false
    })
  }
})
