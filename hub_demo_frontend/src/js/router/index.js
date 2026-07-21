import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'CharactersHome',
    component: () => import('@/views/CharactersPage.vue'),
  },
  {
    path: '/characters',
    name: 'Characters',
    redirect: '/',
  },
  {
    path: '/characters/:id',
    name: 'CharacterDetail',
    component: () => import('@/views/CharacterDetailPage.vue'),
  },
  {
    path: '/combos/:id',
    name: 'ComboDetail',
    component: () => import('@/views/ComboDetailPage.vue'),
  },
  {
    path: '/upload',
    name: 'ComboUpload',
    component: () => import('@/views/ComboUploadPage.vue'),
  },
  {
    path: '/combo-review',
    name: 'ComboReview',
    redirect: '/',
  },
  {
    path: '/combo-favorites',
    name: 'ComboFavorites',
    component: () => import('@/views/FavoriteCombosPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfilePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/my-combos',
    name: 'MyCombos',
    component: () => import('@/views/MyCombosPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/NotificationsPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/guide',
    name: 'Guide',
    component: () => import('@/views/GuidePage.vue'),
  },
  {
    path: '/tier-list',
    name: 'TierList',
    component: () => import('@/views/TierListPage.vue'),
  },
  {
    path: '/feedback',
    name: 'Feedback',
    component: () => import('@/views/FeedbackPage.vue'),
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/AdminPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash }
    return { left: 0, top: 0 }
  },
})

export default router
