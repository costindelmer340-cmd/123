import { clearDemoToken, clearPrimaryAccountData, getTwentyMallBindings } from "../../utils/auth"

const defaultProfile = {
  nickname: "consumer_demo",
  phone: "13338907581",
  avatar: "/assets/avatars/user.png",
  address: "",
  bindPlatform: "未绑定电商平台",
  lastConsult: "暂无"
}

Page({
  data: {
    profile: defaultProfile,
    cancelDialogVisible: false,
    cancelCountdown: 5,
    cancelConfirmEnabled: false
  },
  onUnload() {
    this.clearCancelTimer()
  },
  onShow() {
    const profile = wx.getStorageSync("consumerProfile")
    const addresses = wx.getStorageSync("consumerAddresses") || []
    const oldAddress = wx.getStorageSync("consumerAddress")
    const bindings = getTwentyMallBindings()
    const nextProfile = profile ? { ...defaultProfile, ...profile } : { ...defaultProfile }
    nextProfile.phone = "13338907581"
    nextProfile.bindPlatform = bindings.length ? `已绑定 ${bindings.length} 个电商账号` : "未绑定电商平台"
    const defaultAddress = addresses.find((item) => item.isDefault) || addresses[0] || oldAddress
    if (defaultAddress && defaultAddress.fullAddress) {
      nextProfile.address = defaultAddress.fullAddress
    }
    this.setData({ profile: nextProfile })
  },
  editProfile() {
    wx.navigateTo({ url: "/pages/profile-edit/index" })
  },
  manageAddress() {
    wx.navigateTo({ url: "/pages/address/index" })
  },
  logout() {
    wx.showModal({
      title: "退出登录",
      content: "确定要退出当前账号吗？",
      confirmText: "退出",
      success: (res) => {
        if (!res.confirm) return
        clearDemoToken()
        wx.reLaunch({ url: "/pages/login/index" })
      }
    })
  },
  openCancelAccountDialog() {
    this.clearCancelTimer()
    this.setData({
      cancelDialogVisible: true,
      cancelCountdown: 5,
      cancelConfirmEnabled: false
    })
    this.cancelTimer = setInterval(() => {
      const next = this.data.cancelCountdown - 1
      if (next <= 0) {
        this.clearCancelTimer()
        this.setData({
          cancelCountdown: 0,
          cancelConfirmEnabled: true
        })
        return
      }
      this.setData({ cancelCountdown: next })
    }, 1000)
  },
  closeCancelAccountDialog() {
    this.clearCancelTimer()
    this.setData({
      cancelDialogVisible: false,
      cancelCountdown: 5,
      cancelConfirmEnabled: false
    })
  },
  confirmCancelAccount() {
    if (!this.data.cancelConfirmEnabled) return
    clearPrimaryAccountData()
    wx.reLaunch({ url: "/pages/login/index" })
  },
  clearCancelTimer() {
    if (this.cancelTimer) {
      clearInterval(this.cancelTimer)
      this.cancelTimer = null
    }
  }
})
