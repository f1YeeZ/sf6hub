export const notificationTypeOptions = Object.freeze([
  { value: 'combo_review', label: '连招审核' },
  { value: 'combo_like', label: '点赞' },
  { value: 'combo_favorite', label: '收藏' },
  { value: 'feedback', label: '反馈' },
  { value: 'system', label: '系统' },
])

const notificationTypeLabels = Object.freeze(Object.fromEntries(
  notificationTypeOptions.map(option => [option.value, option.label]),
))

export function notificationTypeLabel(type) {
  return notificationTypeLabels[type] || type || '通知'
}
