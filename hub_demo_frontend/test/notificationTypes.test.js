import assert from 'node:assert/strict'
import test from 'node:test'

import { notificationTypeLabel, notificationTypeOptions } from '../src/js/utils/notificationTypes.js'

test('notification scope matches the current product features', () => {
  assert.deepEqual(
    notificationTypeOptions.map(option => option.value),
    ['combo_review', 'combo_like', 'combo_favorite', 'feedback', 'system'],
  )
  assert.equal(notificationTypeLabel('combo_review'), '连招审核')
  assert.equal(notificationTypeLabel('follow'), 'follow')
  assert.equal(notificationTypeLabel('followed_combo_new'), 'followed_combo_new')
})
