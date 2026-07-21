import test from 'node:test'
import assert from 'node:assert/strict'

import {
  CHARACTER_DATA_FIELDS,
  createEmptyCharacterData,
  getCharacterProfileData,
} from '../src/js/data/characterProfileData.js'

test('maps API character data to the twelve detail-page metrics', () => {
  const characterData = createEmptyCharacterData()
  Object.assign(characterData, {
    hp: '10000',
    throwRange: '0.8',
    jumpSpeed: '4+40+3',
    fastestNormal: '4f',
    sourceName: 'SuperCombo / FAT',
    sourceUrl: 'https://example.com/aki',
  })

  const profile = getCharacterProfileData({ characterData })

  assert.equal(profile.stats.length, 12)
  assert.equal(profile.sourceName, 'SuperCombo / FAT')
  assert.equal(profile.sourceUrl, 'https://example.com/aki')
  assert.equal(profile.stats.find(stat => stat.key === 'hp').value, '10000')
  assert.equal(profile.stats.find(stat => stat.key === 'jumpSpeed').value, '4+40+3')
  assert.equal(profile.stats.find(stat => stat.key === 'forwardWalkSpeed').value, '-')
})

test('new character data templates contain every editable field', () => {
  const template = createEmptyCharacterData()

  assert.equal(CHARACTER_DATA_FIELDS.length, 12)
  for (const field of CHARACTER_DATA_FIELDS) assert.equal(template[field.key], '')
  assert.equal(template.sourceName, '')
  assert.equal(template.sourceUrl, '')
})

test('characters without a database data row do not render the metrics panel', () => {
  assert.equal(getCharacterProfileData({ name: 'RYU' }), null)
})
