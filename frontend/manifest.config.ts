import { defineManifestConfig } from '@uni-helper/vite-plugin-uni-manifest'

export default defineManifestConfig({
  name: 'EqoChat',
  appid: '__UNI__5C6037C',
  description: 'EqoChat uni-app',
  versionName: '1.0.0',
  versionCode: '100',
  locale: 'zh-Hans',
  'app-plus': {
    usingComponents: true,
  },
  h5: {
    title: 'EqoChat',
  },
  vueVersion: '3',
})

