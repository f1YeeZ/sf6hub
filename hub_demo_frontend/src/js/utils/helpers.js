export const baseBtn = 'font-display-lg text-body-md uppercase tracking-wider scale-95 active:scale-90 transition-transform'
export const navLink = `${baseBtn} text-on-surface hover:text-primary-container transition-colors duration-300 hover:skew-x-[-8deg] hover:bg-primary-container hover:text-on-primary-container transition-all px-4 py-2`
export const navLinkActive = `${navLink} text-primary-container border-b-2 border-primary-container pb-1 font-bold italic`

export const TEX_SVG = "data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.05'/%3E%3C/svg%3E"

export function tierBadgeColor(tier) {
  const map = { S: 'bg-primary-container text-on-primary-container', A: 'bg-surface-variant text-on-surface', B: 'bg-surface-variant text-on-surface' }
  return map[tier] || map.B
}

export function tierBorderColor(tier) {
  const map = { S: 'border-tertiary-container', A: 'border-secondary-container', B: 'border-tertiary-container' }
  return map[tier] || 'border-surface-variant'
}

export function comboTagColor(type) {
  const map = {
    'counter-hit': 'combo-type-tag combo-type-tag-counter',
    'punish-counter': 'combo-type-tag combo-type-tag-punish',
    'drive-impact': 'combo-type-tag combo-type-tag-drive',
    'anti-air': 'combo-type-tag combo-type-tag-counter',
    'air-hit': 'combo-type-tag combo-type-tag-counter',
    'throw-starter': 'combo-type-tag combo-type-tag-punish',
    'low-starter': 'combo-type-tag combo-type-tag-punish',
    'drive-rush': 'combo-type-tag combo-type-tag-drive',
    'di-wall-splat': 'combo-type-tag combo-type-tag-drive',
    'side-switch': 'combo-type-tag combo-type-tag-corner',
    'corner-carry': 'combo-type-tag combo-type-tag-corner',
    oki: 'combo-type-tag combo-type-tag-corner',
    corner: 'combo-type-tag combo-type-tag-corner',
    'meaty-strike': 'combo-type-tag combo-type-tag-punish',
    'throw-pressure': 'combo-type-tag combo-type-tag-punish',
    shimmy: 'combo-type-tag combo-type-tag-counter',
    'safe-jump': 'combo-type-tag combo-type-tag-drive',
    'side-switch-pressure': 'combo-type-tag combo-type-tag-corner',
  }
  return map[type] || 'combo-type-tag combo-type-tag-neutral'
}

export const comboTagOptions = [
  { value: 'counter-hit', label: '打康' },
  { value: 'punish-counter', label: '确反康' },
  { value: 'drive-impact', label: '迸放' },
  { value: 'corner', label: '板边' },
  { value: 'central', label: '中央' },
  { value: 'anti-air', label: '对空' },
  { value: 'air-hit', label: '空中命中' },
  { value: 'throw-starter', label: '投技起手' },
  { value: 'low-starter', label: '下段起手' },
  { value: 'drive-rush', label: '绿冲' },
  { value: 'di-wall-splat', label: '迸放墙崩' },
  { value: 'side-switch', label: '换边' },
  { value: 'corner-carry', label: '搬运' },
  { value: 'oki', label: '压起身' },
  { value: 'knockdown', label: '击倒' },
  { value: 'hit-confirm', label: '确认连' },
  { value: 'fun', label: '娱乐' },
]

export const MAX_COMBO_TAGS = 10

export const pressureTypeOptions = [
  { value: 'meaty-strike', label: '打击压起身' },
  { value: 'throw-pressure', label: '投压起身' },
  { value: 'shimmy', label: '拆投惩罚' },
  { value: 'safe-jump', label: '安全跳' },
  { value: 'side-switch-pressure', label: '换边压制' },
]

export const comboTypeOptions = comboTagOptions
export const typeTagColor = comboTagColor

export const comboControlOptions = [
  { value: 'classic', label: '经典' },
  { value: 'modern', label: '现代' },
  { value: 'world-tour', label: '环球' },
]

export function normalizeComboControlType(value) {
  const text = String(value || '').trim().toLowerCase()
  if (text.includes('world') || text.includes('环球')) return 'world-tour'
  if (text.includes('modern') || text.includes('现代') || ['m', 'mod'].includes(text)) return 'modern'
  return 'classic'
}

export function comboControlLabel(value) {
  return comboControlOptions.find(option => option.value === normalizeComboControlType(value))?.label || '经典'
}

export function comboMatchesControlType(combo, controlType) {
  const value = combo?.controlType ?? combo?.controlMode ?? combo?.inputType ?? combo?.inputMode ?? combo?.controlScheme ?? ''
  if (value === undefined || value === null || String(value).trim() === '') return normalizeComboControlType(controlType) === 'classic'
  return normalizeComboControlType(value) === normalizeComboControlType(controlType)
}

export function comboListFromResponse(result) {
  if (Array.isArray(result)) return result
  if (Array.isArray(result?.list)) return result.list
  if (Array.isArray(result?.records)) return result.records
  return []
}

export function normalizeRouteCharacterIds(value) {
  const values = Array.isArray(value)
    ? value
    : String(value || '').split(',')
  return values
    .map(id => Number(id))
    .filter(id => Number.isInteger(id) && id > 0)
}

export function normalizeComboTags(comboOrTags, fallbackType = '') {
  const rawTags = Array.isArray(comboOrTags)
    ? comboOrTags
    : Array.isArray(comboOrTags?.tags)
      ? comboOrTags.tags
      : typeof comboOrTags?.tags === 'string'
        ? comboOrTags.tags.split(',')
        : typeof comboOrTags === 'string'
          ? comboOrTags.split(',')
          : []
  const values = rawTags
    .map(tag => String(tag || '').trim())
    .filter(Boolean)
  if (!values.length && fallbackType) values.push(String(fallbackType).trim())
  return [...new Set(values.filter(Boolean))]
}

export function splitPressureTags(comboOrTags, fallbackType = '') {
  const tags = normalizeComboTags(comboOrTags, fallbackType)
  const pressureTypeValues = new Set(pressureTypeOptions.map(option => option.value))
  return {
    type: tags.find(tag => pressureTypeValues.has(tag)) || '',
  }
}

export function buildPressureTags(type) {
  const pressureTypeValues = new Set(pressureTypeOptions.map(option => option.value))
  const normalizedType = String(type || '').trim()
  return pressureTypeValues.has(normalizedType)
    ? [normalizedType]
    : []
}

export function comboTagLabel(type) {
  const option = [...comboTagOptions, ...pressureTypeOptions]
    .find(item => item.value === type)
  if (option) return option.label
  return String(type || '').toUpperCase()
}

export const comboTypeLabel = comboTagLabel
