# Button 通用按钮组件

统一的按钮组件，适用于所有按钮场景。

## 使用方式

```vue
<template>
  <!-- 基础用法 -->
  <Button text="按钮" />
  
  <!-- 主要按钮 -->
  <Button variant="primary" text="主要按钮" />
  
  <!-- 次要按钮 -->
  <Button variant="secondary" text="次要按钮" />
  
  <!-- 描边按钮 -->
  <Button variant="outline" text="描边按钮" />
  
  <!-- 幽灵按钮（透明） -->
  <Button variant="ghost" text="幽灵按钮" />
  
  <!-- 危险按钮 -->
  <Button variant="danger" text="危险操作" />
  
  <!-- 不同尺寸 -->
  <Button size="mini" text="迷你" />
  <Button size="small" text="小" />
  <Button size="medium" text="中" />
  <Button size="large" text="大" />
  
  <!-- 不同形状 -->
  <Button shape="square" text="方角" />
  <Button shape="round" text="圆角" />
  <Button shape="circle" text="圆形" />
  
  <!-- 块级按钮（占满整行） -->
  <Button block text="块级按钮" />
  
  <!-- 禁用状态 -->
  <Button disabled text="禁用" />
  
  <!-- 加载状态 -->
  <Button loading text="加载中" />
  
  <!-- 自定义样式 -->
  <Button 
    text="自定义" 
    :custom-style="{ backgroundColor: '#f0f0f0' }" 
  />
  
  <!-- 插槽用法（自定义内容） -->
  <Button variant="primary">
    <text>🎉</text>
    <text>自定义内容</text>
  </Button>
</template>

<script setup lang="ts">
import Button from '@/components/Button.vue'
</script>
```

## Props

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `text` | `string` | - | 按钮文字 |
| `variant` | `'default' \| 'primary' \| 'secondary' \| 'outline' \| 'ghost' \| 'danger'` | `'default'` | 按钮样式变体 |
| `size` | `'mini' \| 'small' \| 'medium' \| 'large'` | `'medium'` | 按钮尺寸 |
| `shape` | `'square' \| 'round' \| 'circle'` | `'square'` | 按钮形状 |
| `block` | `boolean` | `false` | 是否为块级按钮（占满整行） |
| `loading` | `boolean` | `false` | 是否加载中 |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `customStyle` | `Record<string, string \| number>` | - | 自定义内联样式 |
| `showText` | `boolean` | `true` | 是否显示文字（用于纯图标按钮） |

## Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `click` | - | 点击按钮时触发（禁用/加载状态不会触发） |

## Slots

| 插槽名 | 说明 |
|--------|------|
| `default` | 自定义按钮内容（会覆盖 text 属性） |

## 尺寸规格

| 尺寸 | 高度 | 内边距 | 字号 | 适用场景 |
|------|------|--------|------|----------|
| `mini` | 48rpx | 0 16rpx | 20rpx | 标签、小操作 |
| `small` | 56rpx | 0 20rpx | 22rpx | 紧凑布局 |
| `medium` | 72rpx | 0 28rpx | 26rpx | 默认尺寸 |
| `large` | 88rpx | 0 36rpx | 30rpx | 主要操作、模态框 |

## 样式变体

| 变体 | 背景 | 边框 | 文字 | 适用场景 |
|------|------|------|------|----------|
| `default` | 浅灰 | 细边框 | 深色 | 默认、次要操作 |
| `primary` | 深色（主色） | 无 | 白色 | 主要操作、确认 |
| `secondary` | 白色 | 细边框 | 深色 | 取消、返回 |
| `outline` | 透明 | 细边框 | 深色 | 轻量操作 |
| `ghost` | 透明 | 无 | 灰色 | 最轻量、辅助操作 |
| `danger` | 红色 | 无 | 白色 | 删除、危险操作 |

## 形状

- `square`: 方角（默认圆角）
- `round`: 更圆的圆角
- `circle`: 胶囊形/药丸形（完全圆角）

## 常用场景示例

### 模态框底部按钮
```vue
<view class="modal-foot">
  <Button variant="secondary" size="large" shape="round" @click="handleCancel">
    取消
  </Button>
  <Button variant="primary" size="large" shape="round" @click="handleConfirm">
    确认
  </Button>
</view>
```

### 页面底部操作
```vue
<Button variant="danger" size="large" shape="round" block @click="handleLogout">
  退出登录
</Button>
```

### 图标按钮
```vue
<IconBtn icon="✎" variant="bordered" @click="handleEdit" />
```

### 加载状态
```vue
<Button variant="primary" :loading="submitting" @click="handleSubmit">
  {{ submitting ? '提交中...' : '提交' }}
</Button>
```

## 与其他组件配合

### IconBtn（图标按钮）
```vue
<IconBtn icon="✎" variant="bordered" size="md" />
```

### 自定义内容
```vue
<Button variant="primary">
  <text>🚀</text>
  <text>开始</text>
</Button>
```

## 注意事项

1. **禁用状态**：按钮禁用时会自动降低透明度，点击事件不会触发
2. **加载状态**：显示旋转图标，点击事件不会触发
3. **点击反馈**：所有按钮都有点击缩放效果（scale 0.98）
4. **块级按钮**：设置 `block` 后按钮宽度为 100%
5. **形状优先级**：`circle` > `round` > `square`
