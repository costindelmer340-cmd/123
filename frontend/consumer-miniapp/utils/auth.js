export function saveDemoToken() {
  wx.setStorageSync('miniapp_token', 'demo-token')
}

export function savePrimaryAccount(phone) {
  wx.setStorageSync("primaryAccount", { phone })
}

export function getPrimaryAccount() {
  return wx.getStorageSync("primaryAccount") || null
}

export function getPrimaryPhone() {
  const account = getPrimaryAccount()
  return account && account.phone ? account.phone : "guest"
}

export function getTwentyMallBindingKey(phone = getPrimaryPhone()) {
  return `twentyMallBinding:${phone}`
}

export function getTwentyMallBindings() {
  const stored = wx.getStorageSync(getTwentyMallBindingKey())
  if (Array.isArray(stored)) {
    return stored.filter((item) => item && item.platform === "20商城")
  }
  if (stored && stored.platform === "20商城") {
    return [stored]
  }
  return []
}

export function getTwentyMallBinding() {
  return getTwentyMallBindings()[0] || null
}

export function saveTwentyMallBinding(binding) {
  const bindings = getTwentyMallBindings()
  const nextBinding = { ...binding, boundAt: Date.now() }
  const nextBindings = bindings.some((item) => item.accountNo === binding.accountNo)
    ? bindings.map((item) => item.accountNo === binding.accountNo ? nextBinding : item)
    : [...bindings, nextBinding]
  wx.setStorageSync(getTwentyMallBindingKey(), nextBindings)
  wx.removeStorageSync("twentyMallBinding")
}

export function removeTwentyMallBinding(accountNo) {
  const nextBindings = getTwentyMallBindings().filter((item) => item.accountNo !== accountNo)
  wx.setStorageSync(getTwentyMallBindingKey(), nextBindings)
  wx.removeStorageSync("twentyMallBinding")
}

export function clearTwentyMallBinding(phone = getPrimaryPhone()) {
  wx.removeStorageSync(getTwentyMallBindingKey(phone))
  wx.removeStorageSync("twentyMallBinding")
}

export function getDemoToken() {
  return wx.getStorageSync('miniapp_token')
}

export function clearDemoToken() {
  wx.removeStorageSync('miniapp_token')
}

export function clearPrimaryAccountData() {
  const phone = getPrimaryPhone()
  clearTwentyMallBinding(phone)
  wx.removeStorageSync("primaryAccount")
  wx.removeStorageSync("consumerProfile")
  wx.removeStorageSync("consumerAddresses")
  wx.removeStorageSync("consumerAddress")
  wx.removeStorageSync("pendingChatOrderNo")
  clearDemoToken()
}
