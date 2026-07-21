import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import test from 'node:test'

import { parseComboNotation } from '../src/js/utils/comboNotation.js'

test('shows rotation inputs in the character-to-icon direction examples', () => {
  const uploadFormSource = readFileSync(
    resolve('src/components/common/ComboUploadForm.vue'),
    'utf8',
  )

  assert.match(
    uploadFormSource,
    /examples: \['2', '6', '236', '214', '623', '360', '720'\]/,
  )
})

test('parses classic motion and attack buttons', () => {
  const moves = parseComboNotation('2MK > 236HP')

  assert.equal(moves.length, 2)
  assert.deepEqual(moves[0].parts.map(part => part.key), ['2', 'MK'])
  assert.deepEqual(moves[1].parts.map(part => part.key), ['236', 'HP'])
})

test('renders a hyphenated target combo as one arrow-linked sequence', () => {
  const moves = parseComboNotation('MP-LK-HK')

  assert.equal(moves.length, 1)
  assert.deepEqual(moves[0].parts.map(part => part.key), ['MP', '-', 'LK', '-', 'HK'])
  assert.deepEqual(moves[0].parts.filter(part => part.type === 'arrow').map(part => part.key), ['-', '-'])
})

test('parses modern controls and drive actions', () => {
  const moves = parseComboNotation('ML + SP > DRC')

  assert.deepEqual(moves[0].parts.map(part => part.key || part.value), ['ML', '+', 'SP'])
  assert.equal(moves[1].parts[0].key, 'DRC')
  assert.deepEqual(
    moves[0].parts.filter(part => part.images).flatMap(part => part.images),
    [
      '/street-fighter-6/controller/modern_l.png',
      '/street-fighter-6/controller/key-plus.png',
      '/street-fighter-6/controller/modern_sp.png',
    ],
  )
})

test('uses only local image paths for modern system buttons', () => {
  const [move] = parseComboNotation('MM + MH + DI + DP + AUTO + THROW')
  const imagePaths = move.parts.filter(part => part.images).flatMap(part => part.images)
  const actionImagePaths = move.parts.filter(part => part.images && part.key !== '+').flatMap(part => part.images)

  assert.equal(actionImagePaths.length, 6)
  assert.ok(imagePaths.every(path => path.startsWith('/street-fighter-6/controller/')))
})

test('uses official controller assets for separators, rotations and charge moves', () => {
  const moves = parseComboNotation('28 + 360 / 720')
  const imagePaths = moves.flatMap(move => move.parts).filter(part => part.images).flatMap(part => part.images)

  assert.deepEqual(imagePaths, [
    '/street-fighter-6/controller/key-dc.png',
    '/street-fighter-6/controller/key-u.png',
    '/street-fighter-6/controller/key-plus.png',
    '/street-fighter-6/controller/key-circle.png',
    '/street-fighter-6/controller/key-or.png',
    '/street-fighter-6/controller/key-circle.png',
    '/street-fighter-6/controller/key-circle.png',
  ])
})

test('renders official compact OD input codes as two generic attack icons', () => {
  const [punchMove, kickMove] = parseComboNotation('236 + LPMPHP > 214 + LKMKHK')

  assert.deepEqual(punchMove.parts.map(part => part.key || part.value), ['236', '+', 'LPMPHP'])
  assert.deepEqual(kickMove.parts.map(part => part.key || part.value), ['214', '+', 'LKMKHK'])
  assert.deepEqual(punchMove.parts.at(-1).images, [
    '/street-fighter-6/controller/icon_punch.png',
    '/street-fighter-6/controller/icon_punch.png',
  ])
  assert.deepEqual(kickMove.parts.at(-1).images, [
    '/street-fighter-6/controller/icon_kick.png',
    '/street-fighter-6/controller/icon_kick.png',
  ])
})

test('renders Japanese tame charge directions with official yellow arrows', () => {
  const [backCharge, downCharge] = parseComboNotation('4溜め6 + LP > 2 溜\nめ 8 + HK')

  assert.deepEqual(backCharge.parts.map(part => part.key || part.value), ['CHARGE4', '+', '6', '+', 'LP'])
  assert.deepEqual(downCharge.parts.map(part => part.key || part.value), ['CHARGE2', '+', '8', '+', 'HK'])
  assert.deepEqual(backCharge.parts[0].images, ['/street-fighter-6/controller/key-lc.png'])
  assert.deepEqual(downCharge.parts[0].images, ['/street-fighter-6/controller/key-dc.png'])
})

test('renders every normalized token emitted by the official frame crawler', () => {
  const routes = [
    ['CHARGE2 + AUTO + ML + SP + DI + DP + THROW + HD', ['CHARGE2', '+', 'AUTO', '+', 'ML', '+', 'SP', '+', 'DI', '+', 'DP', '+', 'THROW', '+', 'HD']],
    ['NEAR 5 / 6 + MLMM', ['NEAR', '5', '/', '6', '+', 'ML', 'MM']],
    ['236236 + LP', ['236', '236', '+', 'LP']],
    ['720 + SP', ['720', '+', 'SP']],
    ['214 + ATKATK', ['214', '+', 'ATK', 'ATK']],
  ]

  for (const [route, expected] of routes) {
    const [move] = parseComboNotation(route)
    assert.deepEqual(move.parts.map(part => part.key || part.value), expected)
  }
})

test('all mapped controller images exist in public assets', () => {
  const routes = [
    '1 2 3 4 5 6 7 8 9',
    '63214 41236 66 44 236 214 623 28 46',
    'PP KK LP MP HP LK MK HK P K',
    'ML MM MH SP DI DP AUTO THROW',
    'ATK HD CHARGE2 CHARGE4 CHARGE6 360 720 PLUS OR',
  ]
  const imagePaths = routes
    .flatMap(route => parseComboNotation(route))
    .flatMap(move => move.parts)
    .filter(part => part.images)
    .flatMap(part => part.images)

  assert.ok(imagePaths.length > 0)
  assert.ok(imagePaths.every(path => existsSync(resolve('public', path.replace(/^\//, '')))))
})

test('normalizes Chinese-facing route conditions', () => {
  const [move] = parseComboNotation('236P (HOLD OK) (AGAINST WALL)')

  assert.deepEqual(
    move.parts.filter(part => part.key).map(part => part.key),
    ['236', 'P', 'HOLD_OK', 'AGAINST_WALL'],
  )
  assert.equal(move.parts.find(part => part.key === 'HOLD_OK').value, '长按')
  assert.equal(move.parts.find(part => part.key === 'AGAINST_WALL').value, '板边')
})

test('renders normalized official follow-up conditions without raw Japanese text', () => {
  const [move] = parseComboNotation('FOLLOWUP LP / MP / HP')

  assert.deepEqual(move.parts.map(part => part.key || part.value), ['FOLLOWUP', 'LP', '/', 'MP', '/', 'HP'])
  assert.equal(move.parts[0].value, '派生后')
})

test('renders all normalized official frame conditions as Chinese labels', () => {
  const [move] = parseComboNotation('DURING_MOVE FAR DRUNK_1 DRUNK_2 DRUNK_3 DRUNK_4 ON_STRIKE')

  assert.deepEqual(move.parts.map(part => part.value), [
    '招式中',
    '远距离',
    '醉意1级',
    '醉意2级',
    '醉意3级',
    '醉意4级',
    '受击触发',
  ])
})
