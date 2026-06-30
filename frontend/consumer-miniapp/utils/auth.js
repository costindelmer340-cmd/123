export function saveDemoToken() {
  wx.setStorageSync('miniapp_token', 'demo-token')
}

export function savePrimaryAccount(phone) {
  wx.setStorageSync("primaryAccount", { phone })
  const phones = getKnownPrimaryPhones()
  if (!phones.includes(phone)) {
    wx.setStorageSync("knownPrimaryPhones", [...phones, phone])
  }
}

export function getKnownPrimaryPhones() {
  const phones = wx.getStorageSync("knownPrimaryPhones")
  return Array.isArray(phones) ? phones.filter(Boolean) : []
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

export function getTwentyMallBindingOwnerKey(accountNo) {
  return `twentyMallBindingOwner:${accountNo}`
}

export function getConsumerProfileKey(phone = getPrimaryPhone()) {
  return `consumerProfile:${phone}`
}

export function getConsumerAddressesKey(phone = getPrimaryPhone()) {
  return `consumerAddresses:${phone}`
}

export function getConsumerProfile() {
  const phone = getPrimaryPhone()
  return wx.getStorageSync(getConsumerProfileKey(phone)) || null
}

export function saveConsumerProfile(profile) {
  wx.setStorageSync(getConsumerProfileKey(), profile)
}

export function getConsumerAddresses() {
  return wx.getStorageSync(getConsumerAddressesKey()) || []
}

export function saveConsumerAddresses(addresses) {
  wx.setStorageSync(getConsumerAddressesKey(), addresses)
}

export function clearConsumerAccountData(phone = getPrimaryPhone()) {
  wx.removeStorageSync(getConsumerProfileKey(phone))
  wx.removeStorageSync(getConsumerAddressesKey(phone))
  wx.removeStorageSync(`consumerLastConsultAt:${phone}`)
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

export function getTwentyMallBindingOwner(accountNo) {
  return wx.getStorageSync(getTwentyMallBindingOwnerKey(accountNo)) || ""
}

export function canBindTwentyMallAccount(accountNo, phone = getPrimaryPhone()) {
  const owner = getTwentyMallBindingOwner(accountNo)
  if (owner) {
    return owner === phone
  }
  const legacyOwner = getKnownPrimaryPhones().find((knownPhone) => {
    if (knownPhone === phone) {
      return false
    }
    const bindings = wx.getStorageSync(getTwentyMallBindingKey(knownPhone))
    if (Array.isArray(bindings)) {
      return bindings.some((item) => item && item.platform === "20商城" && item.accountNo === accountNo)
    }
    return bindings && bindings.platform === "20商城" && bindings.accountNo === accountNo
  })
  if (legacyOwner) {
    wx.setStorageSync(getTwentyMallBindingOwnerKey(accountNo), legacyOwner)
    return false
  }
  return true
}

export function occupyTwentyMallBinding(accountNo, phone = getPrimaryPhone()) {
  wx.setStorageSync(getTwentyMallBindingOwnerKey(accountNo), phone)
}

export function saveTwentyMallBinding(binding) {
  const phone = getPrimaryPhone()
  const bindings = getTwentyMallBindings()
  const nextBinding = { ...binding, boundAt: Date.now() }
  const nextBindings = bindings.some((item) => item.accountNo === binding.accountNo)
    ? bindings.map((item) => item.accountNo === binding.accountNo ? nextBinding : item)
    : [...bindings, nextBinding]
  wx.setStorageSync(getTwentyMallBindingKey(), nextBindings)
  occupyTwentyMallBinding(binding.accountNo, phone)
  wx.removeStorageSync("twentyMallBinding")
}

export function removeTwentyMallBinding(accountNo) {
  const phone = getPrimaryPhone()
  const nextBindings = getTwentyMallBindings().filter((item) => item.accountNo !== accountNo)
  wx.setStorageSync(getTwentyMallBindingKey(), nextBindings)
  if (getTwentyMallBindingOwner(accountNo) === phone) {
    wx.removeStorageSync(getTwentyMallBindingOwnerKey(accountNo))
  }
  wx.removeStorageSync("twentyMallBinding")
}

export function clearTwentyMallBinding(phone = getPrimaryPhone()) {
  getTwentyMallBindings().forEach((binding) => {
    if (getTwentyMallBindingOwner(binding.accountNo) === phone) {
      wx.removeStorageSync(getTwentyMallBindingOwnerKey(binding.accountNo))
    }
  })
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
  clearConsumerAccountData(phone)
  wx.removeStorageSync("primaryAccount")
  wx.removeStorageSync("consumerProfile")
  wx.removeStorageSync("consumerAddresses")
  wx.removeStorageSync("consumerAddress")
  wx.removeStorageSync("pendingChatOrderNo")
  clearDemoToken()
}
