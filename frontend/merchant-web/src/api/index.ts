import { api, unwrap } from './client'

export async function loadPlatformBindings() {
  return unwrap(await api.get('/merchant/platform/shop-bindings'))
}

export async function loadSyncTasks(bindingId: number | string) {
  return unwrap(await api.get(`/merchant/platform/shop-bindings/${bindingId}/sync-tasks`))
}

export async function triggerSync(bindingId: number | string, syncType: string) {
  return unwrap(await api.post(`/merchant/platform/shop-bindings/${bindingId}/sync/${syncType}/trigger`))
}

export async function loadOrders(params: Record<string, unknown> = {}) {
  return unwrap(await api.get('/merchant/orders', { params }))
}

export async function loadTwentyMallMerchantOrders(accountNo: string) {
  return unwrap(await api.get('/twenty-mall/merchant/orders', { params: { accountNo } }))
}

export async function loadAfterSales(params: Record<string, unknown> = {}) {
  return unwrap(await api.get('/merchant/after-sales', { params }))
}

export async function loadTwentyMallMerchantAfterSales(accountNo: string) {
  return unwrap(await api.get('/twenty-mall/merchant/after-sales', { params: { accountNo } }))
}

export async function reviewTwentyMallAfterSale(afterSaleId: number, result: 'APPROVE' | 'REJECT', reason = '') {
  return unwrap(await api.post('/twenty-mall/merchant/after-sales/review', { afterSaleId, result, reason }))
}

export async function loadConversations() {
  return unwrap(await api.get('/merchant/conversations'))
}

export async function loadDemoChatConversations(merchantAccounts: string[] = []) {
  return unwrap(await api.get('/demo-chat/conversations', { params: { merchantAccounts } }))
}

export async function loadTickets(params: Record<string, unknown> = {}) {
  return unwrap(await api.get('/merchant/tickets', { params }))
}

export async function loadReviews(params: Record<string, unknown> = {}) {
  return unwrap(await api.get('/merchant/reviews', { params }))
}

export async function loadTwentyMallMerchantReviews(accountNo: string) {
  return unwrap(await api.get('/twenty-mall/merchant/reviews', { params: { accountNo } }))
}

export async function loadArticles(params: Record<string, unknown> = {}) {
  return unwrap(await api.get('/merchant/knowledge/articles', { params }))
}

export async function loadFaqs(params: Record<string, unknown> = {}) {
  return unwrap(await api.get('/merchant/knowledge/faqs', { params }))
}

export async function loadRules(params: Record<string, unknown> = {}) {
  return unwrap(await api.get('/merchant/knowledge/rules', { params }))
}
