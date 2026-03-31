<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import { useChatStore } from '@/store/modules/chat'

const userStore = useUserStore()
const chatStore = useChatStore()

const parseUserIdFromToken = (token?: string | null): number | null => {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const pad = base64.length % 4 === 0 ? '' : '='.repeat(4 - (base64.length % 4))
    const payloadStr = atob(base64 + pad)
    const payload = JSON.parse(payloadStr)
    const raw = payload.userId ?? payload.sub ?? payload.id ?? payload.uid
    if (raw == null) return null
    const n = Number(raw)
    return Number.isNaN(n) ? null : n
  } catch {
    return null
  }
}

const ensureRealtime = () => {
  if (!userStore.token) {
    chatStore.stopRealtime()
    return
  }
  const userId = parseUserIdFromToken(userStore.token)
  chatStore.setCurrentUserId(userId)
  chatStore.startRealtime(userStore.token)
}

onLaunch(() => {
  console.log('EqoChat App Launch')
  ensureRealtime()
})

onShow(() => {
  ensureRealtime()
})
</script>

<style lang="scss">
@import "@/styles/tokens.css";

page {
  background: var(--c-bg);
  color: var(--c-ink);
  font-family: var(--font-display);
}
</style>
