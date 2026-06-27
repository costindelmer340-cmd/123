import { getTwentyMallBindings, saveTwentyMallBinding } from "../../utils/auth"

function buildTwentyMallStatus(bindings) {
  if (!bindings.length) return "待绑定"
  return `已绑定：${bindings.map((item) => item.accountNo).join("、")}`
}

function buildPlatforms(bindings) {
  return [
    { name: "抖音商城绑定", icon: "/assets/platforms/douyin.png", status: "待绑定" },
    { name: "淘宝绑定", icon: "/assets/platforms/taobao.png", status: "待绑定" },
    { name: "拼多多绑定", icon: "/assets/platforms/pinduoduo.png", status: "待绑定" },
    { name: "京东绑定", icon: "/assets/platforms/jd.png", status: "待绑定" },
    {
      name: "20商城",
      icon: "/assets/platforms/twenty-mall.png",
      status: buildTwentyMallStatus(bindings),
      wide: true
    }
  ]
}

Page({
  data: {
    platforms: buildPlatforms([]),
    twentyMallDialogVisible: false,
    twentyMallAccount: "",
    twentyMallPassword: "",
    twentyMallBound: false
  },
  onShow() {
    const bindings = getTwentyMallBindings()
    this.setData({
      platforms: buildPlatforms(bindings),
      twentyMallBound: !!bindings.length
    })
  },
  bindPlatform(e) {
    const name = e.currentTarget.dataset.name
    if (name === "20商城") {
      this.setData({ twentyMallDialogVisible: true })
      return
    }
    wx.showToast({ title: `${name}功能接入中`, icon: "none" })
  },
  onTwentyMallAccountInput(e) {
    this.setData({ twentyMallAccount: e.detail.value })
  },
  onTwentyMallPasswordInput(e) {
    this.setData({ twentyMallPassword: e.detail.value })
  },
  closeTwentyMallDialog() {
    this.setData({ twentyMallDialogVisible: false })
  },
  submitTwentyMallBind() {
    const accountNo = this.data.twentyMallAccount.trim()
    const password = this.data.twentyMallPassword.trim()
    if (!accountNo || !password) {
      wx.showToast({ title: "请输入20商城账号和密码", icon: "none" })
      return
    }
    wx.request({
      url: "http://localhost:8080/api/twenty-mall/bind",
      method: "POST",
      data: {
        accountNo,
        password,
        role: "CONSUMER"
      },
      success: (res) => {
        if (res.data && res.data.code === "200") {
          saveTwentyMallBinding({
            accountNo,
            role: "CONSUMER",
            platform: "20商城"
          })
          const bindings = getTwentyMallBindings()
          this.setData({
            platforms: buildPlatforms(bindings),
            twentyMallDialogVisible: false,
            twentyMallBound: true,
            twentyMallAccount: "",
            twentyMallPassword: ""
          })
          wx.showToast({ title: "20商城绑定成功", icon: "success" })
          return
        }
        wx.showToast({ title: res.data.message || "账号或密码错误", icon: "none" })
      },
      fail: () => {
        wx.showToast({ title: "请先启动后端服务", icon: "none" })
      }
    })
  }
})
