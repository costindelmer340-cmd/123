const emptyForm = {
  id: "",
  name: "",
  phone: "",
  region: "",
  detail: ""
}

function normalizeAddress(address) {
  return {
    ...address,
    fullAddress: `${address.region || ""}${address.detail || ""}`
  }
}

Page({
  data: {
    addresses: [],
    form: { ...emptyForm },
    editingId: ""
  },
  onLoad() {
    this.loadAddresses()
  },
  loadAddresses() {
    const addresses = wx.getStorageSync("consumerAddresses") || []
    const oldAddress = wx.getStorageSync("consumerAddress")
    if (!addresses.length && oldAddress && oldAddress.fullAddress) {
      const migrated = [{ ...oldAddress, id: "addr_legacy", isDefault: true }]
      wx.setStorageSync("consumerAddresses", migrated)
      wx.removeStorageSync("consumerAddress")
      this.setData({ addresses: migrated })
      return
    }
    this.setData({ addresses })
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },
  editAddress(e) {
    const id = e.currentTarget.dataset.id
    const address = this.data.addresses.find((item) => item.id === id)
    if (!address) return
    this.setData({
      form: { ...address },
      editingId: id
    })
  },
  deleteAddress(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: "删除地址",
      content: "确定要删除该地址吗？",
      confirmText: "删除",
      confirmColor: "#d92d20",
      success: (res) => {
        if (!res.confirm) return
        const nextAddresses = this.data.addresses.filter((item) => item.id !== id)
        if (nextAddresses.length && !nextAddresses.some((item) => item.isDefault)) {
          nextAddresses[0].isDefault = true
        }
        wx.setStorageSync("consumerAddresses", nextAddresses)
        this.setData({ addresses: nextAddresses })
        if (this.data.editingId === id) {
          this.resetForm()
        }
        wx.showToast({ title: "已删除", icon: "success" })
      }
    })
  },
  setDefault(e) {
    const id = e.currentTarget.dataset.id
    const nextAddresses = this.data.addresses.map((item) => ({
      ...item,
      isDefault: item.id === id
    }))
    wx.setStorageSync("consumerAddresses", nextAddresses)
    this.setData({ addresses: nextAddresses })
    wx.showToast({ title: "已设为默认", icon: "success" })
  },
  resetForm() {
    this.setData({
      form: { ...emptyForm },
      editingId: ""
    })
  },
  saveAddress() {
    const form = this.data.form
    if (!form.name || !form.phone || !form.region || !form.detail) {
      wx.showToast({ title: "请完整填写地址", icon: "none" })
      return
    }
    const current = this.data.addresses
    const id = this.data.editingId || `addr_${Date.now()}`
    const address = normalizeAddress({
      ...form,
      id,
      isDefault: current.length === 0 || form.isDefault
    })
    const exists = current.some((item) => item.id === id)
    let nextAddresses = exists
      ? current.map((item) => item.id === id ? { ...item, ...address } : item)
      : [...current, address]
    if (!nextAddresses.some((item) => item.isDefault)) {
      nextAddresses[0].isDefault = true
    }
    if (address.isDefault) {
      nextAddresses = nextAddresses.map((item) => ({
        ...item,
        isDefault: item.id === id
      }))
    }
    wx.setStorageSync("consumerAddresses", nextAddresses)
    wx.removeStorageSync("consumerAddress")
    this.setData({ addresses: nextAddresses })
    this.resetForm()
    wx.showToast({ title: exists ? "地址已更新" : "地址已新增", icon: "success" })
  }
})
