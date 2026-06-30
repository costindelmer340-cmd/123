import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../layouts/MainLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import PlatformView from '../views/PlatformView.vue'
import AfterSaleView from '../views/AfterSaleView.vue'
import ConversationsView from '../views/ConversationsView.vue'
import ReviewsView from '../views/ReviewsView.vue'
import KnowledgeView from '../views/KnowledgeView.vue'
import { getToken, hasMerchantBinding } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: 'dashboard', component: DashboardView },
        { path: 'platform', component: PlatformView },
        { path: 'after-sales', component: AfterSaleView },
        { path: 'conversations', component: ConversationsView },
        { path: 'reviews', component: ReviewsView },
        { path: 'knowledge', component: KnowledgeView }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to) => {
  const token = getToken()
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return hasMerchantBinding() ? '/dashboard' : '/platform'
  }
  if (token && to.path !== '/platform' && !hasMerchantBinding()) {
    return {
      path: '/platform',
      query: { needBind: '1', redirect: to.fullPath }
    }
  }
  return true
})

export default router
