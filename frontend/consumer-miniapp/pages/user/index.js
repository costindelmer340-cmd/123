import { getTwentyMallBinding } from "../../utils/auth"

const defaultProfile = {
  nickname: "consumer_demo",
  phone: "13338907581",
  avatar: "/assets/avatars/user.png",
  address: "",
  bindPlatform: "未绑定",
  lastConsult: "暂无"
}

Page({
  data: {
    profile: defaultProfile
  },
  onShow() {
    const profile = wx.getStorageSync("consumerProfile")
    const address = wx.getStorageSync("consumerAddress")
    const binding = getTwentyMallBinding()
    const nextProfile = profile ? { ...defaultProfile, ...profile } : { ...defaultProfile }
    nextProfile.phone = "13338907581"
    if (address && address.fullAddress) {
      nextProfile.address = address.fullAddress
    }
    this.setData({ profile: nextProfile })
    if (binding && binding.platform === "20商城") {
      wx.request({
        url: `http://localhost:8080/api/twenty-mall/profile?accountNo=${binding.accountNo}&role=CONSUMER`,
        success: (res) => {
          const data = res.data && res.data.data
          if (data) {
            this.setData({
              profile: {
                ...this.data.profile,
                nickname: data.displayName,
                phone: data.phone,
                avatar: data.avatar || "/assets/avatars/twenty-user.png",
                address: data.address || "四川省成都市高新区20商城模拟社区 2023号",
                bindPlatform: "20商城",
                lastConsult: "2026-06-27 11:20:00"
              }
            })
          }
        }
      })
    }
  },
  editProfile() {
    wx.navigateTo({ url: "/pages/profile-edit/index" })
  },
  manageAddress() {
    wx.navigateTo({ url: "/pages/address/index" })
  }
})
