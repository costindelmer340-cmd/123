import { saveDemoToken } from "../../utils/auth"

Page({
  data: {
    phone: "",
    code: ""
  },
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },
  onCodeInput(e) {
    this.setData({ code: e.detail.value })
  },
  onLogin() {
    saveDemoToken()
    wx.switchTab({ url: "/pages/home/index" })
  }
})
