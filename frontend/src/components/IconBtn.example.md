# IconBtn 图标按钮组件

简洁的图标按钮组件，用于各种操作按钮场景。

## 使用方式

```vue
<template>
  <!-- 基础用法 -->
  <IconBtn icon="✎" />
  
  <!-- 带边框 -->
  <IconBtn icon="＋" variant="bordered" />
  
  <!-- 主色按钮 -->
  <IconBtn icon="✓" variant="primary" />
  
  <!-- 幽灵按钮（透明背景） -->
  <IconBtn icon="⋮" variant="ghost" />
  
  <!-- 小尺寸 -->
  <IconBtn icon="✕" size="sm" />
  
  <!-- 大尺寸 -->
  <IconBtn icon="⚙" size="lg" />
  
  <!-- 禁用状态 -->
  <IconBtn icon="🔒" disabled />
  
  <!-- 自定义内容 -->
  <IconBtn>
    <text>🎨</text>
  </IconBtn>
  
  <!-- 自定义样式 -->
  <IconBtn 
    icon="⚡" 
    :custom-style="{ backgroundColor: '#f0f0f0', borderColor: '#ccc' }" 
  />
</template>

<script setup lang="ts">
import IconBtn from '@/components/IconBtn.vue'
</script>
```

## Props

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `icon` | `string` | - | 图标字符（支持 emoji 和特殊字符） |
| `variant` | `'default' \| 'bordered' \| 'primary' \| 'ghost'` | `'default'` | 按钮样式变体 |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 按钮尺寸 |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `customStyle` | `Record<string, string \| number>` | - | 自定义内联样式 |

## Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `click` | - | 点击按钮时触发（禁用状态不会触发） |

## Slots

| 插槽名 | 说明 |
|--------|------|
| `default` | 自定义按钮内容（不传 icon 时使用） |

## 尺寸规格

| 尺寸 | 宽度 | 高度 | 图标大小 |
|------|------|------|----------|
| `sm` | 56rpx | 56rpx | 24rpx |
| `md` | 72rpx | 72rpx | 32rpx |
| `lg` | 88rpx | 88rpx | 40rpx |

## 样式变体

- **default**: 默认样式，浅灰背景 + 细边框
- **bordered**: 白色背景 + 边框
- **primary**: 主色背景（深色）+ 白色图标
- **ghost**: 透明背景无边框

## 常用图标字符

```
编辑：✎
添加：＋
关闭：✕
确认：✓
设置：⚙
更多：⋮
分享：⤴
刷新：↻
```
