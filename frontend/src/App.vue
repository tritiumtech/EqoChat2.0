<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('EqoChat App Launch')
})

onShow(() => {
  console.log('EqoChat App Show')
})

onHide(() => {
  console.log('EqoChat App Hide')
})
</script>

<style>
/*每个页面公共css */
</style>