import { getTwentyMallBindings, removeTwentyMallBinding, saveTwentyMallBinding } from "../../utils/auth"

function buildPlatforms() {
  return [
    { name: "抖音商城绑定", icon: "/assets/platforms/douyin.png", status: "待绑定" },
    { name: "淘宝绑定", icon: "/assets/platforms/taobao.png", status: "待绑定" },
    { name: "拼多多绑定", icon: "/assets/platforms/pinduoduo.png", status: "待绑定" },
    { name: "京东绑定", icon: "/assets/platforms/jd.png", status: "待绑定" },
    {
      name: "20商城",
      icon: "/assets/platforms/twenty-mall.png",
      status: "本地模拟平台",
      wide: true
    }
  ]
}

function splitAddress(fullAddress) {
  const address = fullAddress || ""
  const match = address.match(/^(.+[省市区县])(.+)$/)
  if (!match) {
    return {
      region: "",
      detail: address
    }
  }
  return {
    region: match[1],
    detail: match[2]
  }
}

Page({
  data: {
    platforms: buildPlatforms(),
    twentyMallBindings: [],
    twentyMallDialogVisible: false,
    twentyMallAccount: "",
    twentyMallPassword: "",
    twentyMallBound: false
  },
  onShow() {
    const bindings = getTwentyMallBindings()
    this.setData({
      platforms: buildPlatforms(),
      twentyMallBindings: bindings,
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
            platforms: buildPlatforms(),
            twentyMallBindings: bindings,
            twentyMallDialogVisible: false,
            twentyMallBound: true,
            twentyMallAccount: "",
            twentyMallPassword: ""
          })
          this.importTwentyMallAddress(accountNo)
          wx.showToast({ title: "20商城绑定成功", icon: "success" })
          return
        }
        wx.showToast({ title: res.data.message || "账号或密码错误", icon: "none" })
      },
      fail: () => {
        wx.showToast({ title: "请先启动后端服务", icon: "none" })
      }
    })
  },
  importTwentyMallAddress(accountNo) {
    wx.request({
      url: `http://localhost:8080/api/twenty-mall/profile?accountNo=${accountNo}&role=CONSUMER`,
      success: (res) => {
        const data = res.data && res.data.data
        if (!data || !data.address) return
        const addresses = wx.getStorageSync("consumerAddresses") || []
        const sourceId = `twenty_mall_${accountNo}`
        const parts = splitAddress(data.address)
        const importedAddress = {
          id: sourceId,
          name: data.displayName || "20商城用户",
          phone: data.phone || "13338907581",
          region: parts.region,
          detail: parts.detail,
          fullAddress: data.address,
          source: "20商城",
          sourceAccountNo: accountNo,
          isDefault: addresses.length === 0
        }
        const exists = addresses.some((item) => item.id === sourceId)
        let nextAddresses = exists
          ? addresses.map((item) => item.id === sourceId ? { ...item, ...importedAddress, isDefault: item.isDefault } : item)
          : [...addresses, importedAddress]
        if (!nextAddresses.some((item) => item.isDefault)) {
          nextAddresses = nextAddresses.map((item, index) => ({
            ...item,
            isDefault: index === 0
          }))
        }
        wx.setStorageSync("consumerAddresses", nextAddresses)
        wx.removeStorageSync("consumerAddress")
      }
    })
  },
  unbindTwentyMall(e) {
    const accountNo = e.currentTarget.dataset.account
    wx.showModal({
      title: "解除绑定",
      content: `确定要解绑20商城账号 ${accountNo} 吗？解绑后该账号订单和客服会话将不再显示。`,
      confirmText: "解绑",
      confirmColor: "#d92d20",
      success: (res) => {
        if (!res.confirm) return
        removeTwentyMallBinding(accountNo)
        const bindings = getTwentyMallBindings()
        this.setData({
          twentyMallBindings: bindings,
          twentyMallBound: !!bindings.length
        })
        wx.showToast({ title: "已解绑", icon: "success" })
      }
    })
  }
})
