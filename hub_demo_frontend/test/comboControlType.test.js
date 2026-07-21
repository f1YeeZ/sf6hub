import assert from 'node:assert/strict'
import test from 'node:test'

import {
  buildPressureTags,
  comboControlLabel,
  comboListFromResponse,
  comboMatchesControlType,
  normalizeComboControlType,
  normalizeRouteCharacterIds,
  comboTagOptions,
  pressureTypeOptions,
  splitPressureTags,
} from '../src/js/utils/helpers.js'

test('normalizes world tour combo control type aliases', () => {
  assert.equal(normalizeComboControlType('world-tour'), 'world-tour')
  assert.equal(normalizeComboControlType('环球游历'), 'world-tour')
  assert.equal(comboControlLabel('world'), '环球')
})

test('includes the expanded combo tags without role switching', () => {
  const labels = comboTagOptions.map(option => option.label)

  assert.equal(comboTagOptions.length, 17)
  assert.equal(labels.includes('角色切换'), false)
  assert.equal(labels.includes('无资源'), false)
  assert.equal(labels.includes('低耗'), false)
  assert.equal(labels.includes('烧尽可用'), false)
  assert.equal(labels.includes('中央'), true)
  assert.equal(labels.includes('确认连'), true)
})

test('normalizes world tour route character ids', () => {
  assert.deepEqual(normalizeRouteCharacterIds(['10', 20, '', 0]), [10, 20])
  assert.deepEqual(normalizeRouteCharacterIds('10,20'), [10, 20])
})

test('keeps pressure types separate from normal combo tags', () => {
  assert.equal(comboTagOptions.some(option => option.value === 'meaty-strike'), false)
  assert.equal(pressureTypeOptions.some(option => option.label === '安全跳'), true)
  assert.equal(pressureTypeOptions.some(option => option.label === '帧消耗'), false)
  assert.deepEqual(buildPressureTags('safe-jump'), ['safe-jump'])
  assert.deepEqual(splitPressureTags(['safe-jump']), {
    type: 'safe-jump',
  })
})

test('matches world tour combos without falling back to classic', () => {
  assert.equal(comboMatchesControlType({ controlType: 'world-tour' }, '环球'), true)
  assert.equal(comboMatchesControlType({ controlType: 'classic' }, 'world-tour'), false)
})

test('keeps parent combos when the character endpoint returns a paged response', () => {
  const pagedResponse = {
    list: [{ id: 101, route: '5MP > 236LP' }],
    page: 1,
    pageSize: 12,
    total: 1,
  }

  assert.deepEqual(comboListFromResponse(pagedResponse), pagedResponse.list)
  assert.deepEqual(comboListFromResponse([{ id: 102 }]), [{ id: 102 }])
  assert.deepEqual(comboListFromResponse({ list: null }), [])
})
