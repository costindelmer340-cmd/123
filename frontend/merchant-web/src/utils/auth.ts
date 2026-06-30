export const TOKEN_KEY = 'merchant_token'
export const USER_KEY = 'merchant_user'
export const MERCHANT_BINDINGS_KEY = 'merchant_platform_bindings'
export const MERCHANT_BINDINGS_LEGACY_KEY = MERCHANT_BINDINGS_KEY

export type MerchantPlatformBinding = {
  id: number
  platformCode: string
  platformName: string
  authStatus: string
  externalShopId: string
  shopName: string
  sellerNick?: string
  lastSyncedAt?: string
  accountNo?: string
}

const TWENTY_MALL_MERCHANT_NAMES: Record<string, string> = {
  '20230141': '极光外设旗舰店',
  '20230142': '黑曜通勤箱包店',
  '22222223': '晨光数码生活馆',
  '22222224': '云途箱包旗舰店'
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export function isDemoMode() {
  return getToken() === 'demo-token'
}

export function setAuth(token: string, user: unknown) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function getStoredUser<T>() {
  const rawUser = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY)
  if (!rawUser) {
    return null
  }
  try {
    return JSON.parse(rawUser) as T
  } catch {
    return null
  }
}

function getMerchantBindingStorageKey() {
  const user = getStoredUser<{ username?: string; userId?: number }>()
  const accountKey = user?.username || user?.userId || 'anonymous'
  return `${MERCHANT_BINDINGS_KEY}:${accountKey}`
}

export function getMerchantBindings() {
  const storageKey = getMerchantBindingStorageKey()
  const raw = localStorage.getItem(storageKey) || localStorage.getItem(MERCHANT_BINDINGS_LEGACY_KEY)
  if (!raw) {
    return []
  }
  try {
    const bindings = normalizeMerchantBindings(JSON.parse(raw) as MerchantPlatformBinding[])
    localStorage.setItem(storageKey, JSON.stringify(bindings))
    localStorage.removeItem(MERCHANT_BINDINGS_LEGACY_KEY)
    return bindings
  } catch {
    return []
  }
}

export function hasMerchantBinding() {
  return getMerchantBindings().length > 0
}

export function saveMerchantBinding(binding: MerchantPlatformBinding) {
  const bindings = getMerchantBindings()
  const next = bindings.some((item) => item.platformCode === binding.platformCode && item.externalShopId === binding.externalShopId)
    ? bindings.map((item) => item.platformCode === binding.platformCode && item.externalShopId === binding.externalShopId ? binding : item)
    : [...bindings, binding]
  localStorage.setItem(getMerchantBindingStorageKey(), JSON.stringify(next))
  localStorage.removeItem(MERCHANT_BINDINGS_LEGACY_KEY)
}

export function removeMerchantBinding(platformCode: string, externalShopId: string) {
  const next = getMerchantBindings().filter((item) => !(item.platformCode === platformCode && item.externalShopId === externalShopId))
  localStorage.setItem(getMerchantBindingStorageKey(), JSON.stringify(next))
  localStorage.removeItem(MERCHANT_BINDINGS_LEGACY_KEY)
}

export function clearMerchantBindings() {
  localStorage.removeItem(getMerchantBindingStorageKey())
  localStorage.removeItem(MERCHANT_BINDINGS_LEGACY_KEY)
}

function normalizeMerchantBindings(bindings: MerchantPlatformBinding[]) {
  return bindings.map((item) => {
    if (item.platformCode !== 'TWENTY_MALL' || !item.accountNo) {
      return item
    }
    const shopName = TWENTY_MALL_MERCHANT_NAMES[item.accountNo]
    if (!shopName) {
      return item
    }
    return {
      ...item,
      shopName,
      sellerNick: shopName,
      externalShopId: `TM_SHOP_${item.accountNo}`
    }
  })
}

export function getTwentyMallMerchantName(accountNo: string) {
  return TWENTY_MALL_MERCHANT_NAMES[accountNo] || `20商城商家店铺（${accountNo}）`
}
