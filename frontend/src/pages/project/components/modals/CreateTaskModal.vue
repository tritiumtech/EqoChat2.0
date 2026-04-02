<template>
  <view v-if="open" class="modal-mask modal-mask-center" @click="emit('close')">
    <view class="modal-card modal-card-md" @click.stop>
      <view class="modal-head">
        <text class="sheet-title">{{ t('page.project.modals.create_task.title') }}</text>
        <text class="sheet-close" @click="emit('close')">✕</text>
      </view>

      <view class="modal-body modal-body-spaced">
        <view class="field">
          <text class="field-label">{{ t('page.project.modals.create_task.task_title') }}</text>
          <input
            class="field-input"
            :value="taskTitle"
            :placeholder="t('page.project.placeholders.task_title')"
            @input="onTitleInput"
          />
        </view>

        <view class="field">
          <text class="field-label">{{ t('page.project.modals.create_task.task_value') }}</text>
          <view class="field-input-wrap">
            <text class="field-prefix">$</text>
            <input
              class="field-input field-input--with-prefix"
              type="number"
              :value="taskValue"
              :placeholder="t('page.project.placeholders.task_value')"
              @input="onValueInput"
            />
          </view>
          <text class="field-hint">{{ t('page.project.modals.create_task.value_hint') }}</text>
        </view>

        <view class="field">
          <text class="field-label">{{ t('page.project.modals.create_task.deadline') }}</text>
          <picker
            mode="date"
            :value="taskDeadline"
            @change="onDeadlineChange"
          >
            <view class="field-picker">
              <text :class="{ 'field-picker-placeholder': !taskDeadline }">
                {{ taskDeadline || t('page.project.placeholders.deadline') }}
              </text>
              <text class="field-picker-icon">▼</text>
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label">{{ t('page.project.modals.create_task.priority') }}</text>
          <view class="priority-row">
            <view
              v-for="p in priorityOptions"
              :key="p.value"
              class="priority-btn"
              :class="[p.class, { active: taskPriority === p.value }]"
              @click="taskPriority = p.value"
            >
              <text>{{ p.label }}</text>
            </view>
          </view>
        </view>

        <view class="info-note">
          <text>{{ t('page.project.modals.create_task.info_note') }}</text>
        </view>
      </view>

      <view class="modal-foot">
        <Button variant="secondary" size="large" shape="round" @click="emit('close')">
          {{ t('toast.cancel') }}
        </Button>
        <Button variant="primary" size="large" shape="round" :disabled="!canCreate" @click="handleSubmit">
          {{ t('page.project.modals.create_task.confirm') }}
        </Button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from '@/components/Button.vue'

defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

const props = defineProps<{
  open: boolean
  projectId: number | null
  creating: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', payload: { title: string; value: number; deadline: string; priority: string }): void
}>()

const { t } = useI18n({ useScope: 'global' })

const taskTitle = ref('')
const taskValue = ref('')
const taskDeadline = ref('')
const taskPriority = ref<'low' | 'medium' | 'high'>('medium')

const priorityOptions = computed(() => [
  { value: 'low', label: t('page.project.task_priority.low'), class: 'prio-low' },
  { value: 'medium', label: t('page.project.task_priority.medium'), class: 'prio-medium' },
  { value: 'high', label: t('page.project.task_priority.high'), class: 'prio-high' },
])

const canCreate = computed(() => {
  const titleOk = taskTitle.value.trim().length > 0
  const valueOk = Number(taskValue.value) > 0
  const deadlineOk = taskDeadline.value.length > 0
  return titleOk && valueOk && deadlineOk
})

watch(() => props.open, (v) => {
  if (v) {
    taskTitle.value = ''
    taskValue.value = ''
    taskDeadline.value = ''
    taskPriority.value = 'medium'
  }
})

function onTitleInput(payload: any) {
  taskTitle.value = typeof payload?.detail?.value === 'string' ? payload.detail.value : ''
}

function onValueInput(payload: any) {
  taskValue.value = typeof payload?.detail?.value === 'string' ? payload.detail.value : ''
}

function onDeadlineChange(payload: any) {
  taskDeadline.value = payload?.detail?.value || ''
}

function handleSubmit() {
  if (!canCreate.value || props.creating) return
  emit('submit', {
    title: taskTitle.value.trim(),
    value: Math.round(Number(taskValue.value) * 100),
    deadline: taskDeadline.value,
    priority: taskPriority.value,
  })
}
</script>

<style scoped src="../../project.styles.css"></style>

<style scoped>
.field-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.field-prefix {
  position: absolute;
  left: 24rpx;
  font-size: 28rpx;
  color: var(--c-muted);
  z-index: 1;
}

.field-input--with-prefix {
  padding-left: 56rpx !important;
}

.field-hint {
  display: block;
  font-size: 20rpx;
  color: var(--c-muted);
  margin-top: 8rpx;
}

.field-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
}

.field-picker-placeholder {
  color: var(--c-muted);
}

.field-picker-icon {
  font-size: 20rpx;
  color: var(--c-muted);
}

.priority-row {
  display: flex;
  gap: 12rpx;
}

.priority-btn {
  flex: 1;
  padding: 18rpx 0;
  text-align: center;
  border-radius: var(--radius-lg);
  font-size: 26rpx;
  font-weight: 500;
  border: 2rpx solid transparent;
  transition: all 160ms ease;
}

.priority-btn.prio-low {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
  border-color: rgba(16, 185, 129, 0.2);
}

.priority-btn.prio-low.active {
  background: rgba(16, 185, 129, 0.2);
  border-color: #10b981;
}

.priority-btn.prio-medium {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
  border-color: rgba(245, 158, 11, 0.2);
}

.priority-btn.prio-medium.active {
  background: rgba(245, 158, 11, 0.2);
  border-color: #f59e0b;
}

.priority-btn.prio-high {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
  border-color: rgba(239, 68, 68, 0.2);
}

.priority-btn.prio-high.active {
  background: rgba(239, 68, 68, 0.2);
  border-color: #ef4444;
}

.info-note {
  padding: 16rpx 20rpx;
  background: rgba(14, 165, 233, 0.08);
  border: 1rpx solid rgba(14, 165, 233, 0.2);
  border-radius: var(--radius-lg);
}

.info-note text {
  font-size: 22rpx;
  color: #0284c7;
  line-height: 1.4;
}
</style>