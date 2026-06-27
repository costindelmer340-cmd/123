import { clearTwentyMallBinding, saveDemoToken, savePrimaryAccount } from "../../utils/auth"

Page({
  data: {
    phone: "",
    code: "",
    demoPhone: "13338907581",
    demoCode: "123456"
  },
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },
  onCodeInput(e) {
    this.setData({ code: e.detail.value })
  },
  onLogin() {
    const phone = this.data.phone.trim()
    const code = this.data.code.trim()
    if (phone !== this.data.demoPhone || code !== this.data.demoCode) {
      wx.showToast({ title: "手机号或验证码错误", icon: "none" })
      return
    }
    saveDemoToken()
    savePrimaryAccount(phone)
    if (!wx.getStorageSync("demoAccountBindingReset20260627")) {
      clearTwentyMallBinding(phone)
      wx.setStorageSync("demoAccountBindingReset20260627", true)
    }
    wx.switchTab({ url: "/pages/home/index" })
  },
  onWechatLogin() {
    wx.showToast({ title: "微信一键登录后续接入", icon: "none" })
  }
})
