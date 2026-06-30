import { getConsumerProfile, getPrimaryPhone, saveConsumerProfile } from "../../utils/auth"

Page({
  data: {
    form: {
      nickname: "",
      avatar: ""
    },
    phone: ""
  },
  onLoad() {
    const profile = getConsumerProfile()
    const phone = getPrimaryPhone()
    this.setData({ phone: phone === "guest" ? "" : phone })
    if (profile) {
      this.setData({
        form: {
          nickname: profile.nickname || "",
          avatar: profile.avatar || this.data.form.avatar
        }
      })
    }
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },
  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0]
        if (file && file.tempFilePath) {
          this.setData({ "form.avatar": file.tempFilePath })
        }
      },
      fail: () => {
        wx.showToast({ title: "暂未选择头像", icon: "none" })
      }
    })
  },
  saveProfile() {
    saveConsumerProfile(this.data.form)
    wx.showToast({ title: "资料已保存", icon: "success" })
    setTimeout(() => wx.navigateBack(), 500)
  }
})
