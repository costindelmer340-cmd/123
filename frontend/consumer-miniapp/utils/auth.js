export function saveDemoToken() {
  wx.setStorageSync('miniapp_token', 'demo-token')
}

export function getDemoToken() {
  return wx.getStorageSync('miniapp_token')
}

export function clearDemoToken() {
  wx.removeStorageSync('miniapp_token')
}
