import { getTwentyMallBindings } from "../../utils/auth"

Page({
  data: {
    orders: [],
    orderGroups: [],
    platformBound: false
  },
  onShow() {
    const bindings = getTwentyMallBindings()
    if (!bindings.length) {
      this.setData({ orders: [], orderGroups: [], platformBound: false })
      return
    }
    const requests = bindings.map((binding) => new Promise((resolve) => {
      wx.request({
        url: `http://localhost:8080/api/twenty-mall/consumer/orders?accountNo=${binding.accountNo}`,
        success: (res) => {
          const list = (res.data && res.data.data) || []
          const groupOrders = list.map((item) => ({
            no: item.no,
            title: item.title,
            status: item.status,
            afterSale: item.afterSale,
            platform: "20商城",
            accountNo: binding.accountNo,
            merchant: "20商城演示店铺",
            price: item.price,
            image: item.image,
            spec: item.spec,
            service: item.afterSale === "未申请" ? "可申请售后" : "售后处理中"
          }))
          resolve({
            platform: "20商城",
            accountNo: binding.accountNo,
            orders: groupOrders
          })
        },
        fail: () => {
          resolve({
            platform: "20商城",
            accountNo: binding.accountNo,
            orders: []
          })
        }
      })
    }))
    Promise.all(requests).then((groups) => {
      this.setData({
        orderGroups: groups,
        orders: groups.reduce((all, group) => all.concat(group.orders), []),
        platformBound: true
      })
    })
  },
  goDetail(e) {
    if (!this.data.platformBound) return
    const no = e.currentTarget.dataset.no
    wx.navigateTo({ url: `/pages/product/index?no=${no}` })
  },
  goBind() {
    wx.switchTab({ url: "/pages/home/index" })
  }
})
