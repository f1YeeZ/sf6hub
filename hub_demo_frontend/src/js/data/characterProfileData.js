export const CHARACTER_DATA_FIELDS = Object.freeze([
  { key: 'hp', label: '生命值', sourceLabel: 'HP', placeholder: '例如 10000' },
  { key: 'throwRange', label: '普通投距离', sourceLabel: 'Throw Range', placeholder: '例如 0.8' },
  { key: 'forwardWalkSpeed', label: '前进速度', sourceLabel: 'Fwd Walk Speed', placeholder: '例如 0.047' },
  { key: 'backWalkSpeed', label: '后退速度', sourceLabel: 'Back Walk Speed', placeholder: '例如 0.032' },
  { key: 'forwardDashSpeed', label: '前冲耗时', sourceLabel: 'Fwd Dash Speed', placeholder: '例如 19' },
  { key: 'backDashSpeed', label: '后撤耗时', sourceLabel: 'Back Dash Speed', placeholder: '例如 23' },
  { key: 'forwardDashDistance', label: '前冲距离', sourceLabel: 'Fwd Dash Distance', placeholder: '例如 1.252' },
  { key: 'backDashDistance', label: '后撤距离', sourceLabel: 'Back Dash Distance', placeholder: '例如 0.923' },
  { key: 'jumpSpeed', label: '跳跃帧数', sourceLabel: 'Jump Speed', placeholder: '例如 4+38+3' },
  { key: 'forwardJumpDistance', label: '前跳距离', sourceLabel: 'Fwd Jump Distance', placeholder: '例如 1.9' },
  { key: 'backJumpDistance', label: '后跳距离', sourceLabel: 'Back Jump Distance', placeholder: '例如 1.52' },
  { key: 'fastestNormal', label: '最快普通技', sourceLabel: 'Fastest Normal', placeholder: '例如 4f' },
])

export function createEmptyCharacterData() {
  return {
    ...Object.fromEntries(CHARACTER_DATA_FIELDS.map(field => [field.key, ''])),
    sourceName: '',
    sourceUrl: '',
  }
}

export function getCharacterProfileData(character) {
  const characterData = character?.characterData
  if (!characterData) return null
  return {
    sourceName: characterData.sourceName || '角色数据',
    sourceUrl: characterData.sourceUrl || '',
    stats: CHARACTER_DATA_FIELDS.map(field => ({
      ...field,
      value: String(characterData[field.key] || '').trim() || '-',
    })),
  }
}
