import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './js/router'
import { installVisitTracking } from '@/js/analytics'
import { useAuthStore } from '@/js/stores/auth'
import { useAdminAuthStore } from '@/js/stores/adminAuth'
import './styles/main.css'

function markMaterialIconsReady() {
  document.documentElement.classList.add('material-icons-ready')
}

if (document.fonts?.load) {
  document.fonts
    .load('24px "Material Symbols Outlined"')
    .then(markMaterialIconsReady)
    .catch(() => {})
} else {
  markMaterialIconsReady()
}

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
installVisitTracking(router)
const auth = useAuthStore(pinia)
const adminAuth = useAdminAuthStore(pinia)
auth.hydrate()
adminAuth.hydrate()
app.mount('#app')
