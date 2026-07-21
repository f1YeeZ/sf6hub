<template>
  <section class="combo-upload-panel" :class="{ embedded, editing: isEditing }" aria-labelledby="combo-upload-title">
    <button v-if="!embedded" class="combo-upload-close" type="button" aria-label="关闭连招上传" @click="close">
            <span class="material-symbols-outlined">close</span>
          </button>

          <div class="combo-upload-head">
            <span>COMBO LAB</span>
            <h2 id="combo-upload-title">{{ isEditing ? '修改连招' : '上传连招' }}</h2>
            <p>{{ isEditing ? '调整被驳回的路线与资源数据，提交后会重新进入审核队列。' : '填写路线、资源消耗和训练要点，保存后进入审核队列。' }}</p>
          </div>

          <div class="combo-upload-form">
            <div v-if="!isEditing" class="upload-field-row">
              <span>上传类型</span>
              <div class="upload-mode-tabs" role="group" aria-label="上传类型">
                <button
                  type="button"
                  :class="['upload-mode-tab', { active: uploadMode === 'normal' }]"
                  :aria-pressed="uploadMode === 'normal'"
                  @click="setUploadMode('normal')"
                >
                  普通连招
                </button>
                <button
                  type="button"
                  :class="['upload-mode-tab', { active: uploadMode === 'followup' }]"
                  :aria-pressed="uploadMode === 'followup'"
                  @click="setUploadMode('followup')"
                >
                  后续压制连招
                </button>
              </div>
            </div>

            <div class="upload-field-row">
              <span>连招类型</span>
              <div class="upload-mode-tabs combo-control-tabs" role="radiogroup" aria-label="连招类型">
                <button
                  v-for="option in comboControlOptions"
                  :key="option.value"
                  type="button"
                  :class="['upload-mode-tab', `is-${option.value}`, { active: form.controlType === option.value }]"
                  :aria-checked="form.controlType === option.value"
                  role="radio"
                  @click="setControlType(option.value)"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>

            <label v-if="!isWorldTour" class="upload-field-row">
              <span>角色</span>
              <select v-model="form.characterId" class="input-sf6" required>
                <option disabled value="">选择角色</option>
                <option v-for="c in characters.characters" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </label>

            <div v-if="isFollowupMode" class="upload-field-row">
              <span>原连招</span>
              <div class="parent-combo-picker">
                <div class="parent-combo-combobox" @focusin="openParentComboDropdown" @focusout="handleParentComboBlur">
                  <div class="parent-combo-input-wrap">
                    <input
                      v-model="parentComboQuery"
                      class="input-sf6 parent-combo-search"
                      type="search"
                      :placeholder="selectedParentComboLabel || '搜索路线或起手式'"
                      role="combobox"
                      :aria-expanded="parentComboDropdownOpen"
                      aria-controls="parent-combo-options"
                      autocomplete="off"
                      @input="handleParentComboInput"
                      @keydown.down.prevent="openParentComboDropdown"
                      @keydown.esc.prevent="closeParentComboDropdown"
                    />
                    <button type="button" class="parent-combo-toggle" :aria-label="parentComboDropdownOpen ? '收起原连招列表' : '展开原连招列表'" @mousedown.prevent @click="toggleParentComboDropdown">
                      <span class="material-symbols-outlined">{{ parentComboDropdownOpen ? 'expand_less' : 'expand_more' }}</span>
                    </button>
                  </div>
                  <div v-if="parentComboDropdownOpen" id="parent-combo-options" class="parent-combo-dropdown">
                    <div v-if="parentCombosLoading" class="parent-combo-state">正在同步连招...</div>
                    <div v-else-if="filteredParentCombos.length" class="parent-combo-list" role="radiogroup" aria-label="选择原连招">
                      <button
                        v-for="comboOption in filteredParentCombos"
                        :key="comboOption.id"
                        type="button"
                        :class="['parent-combo-card', { selected: Number(form.followupParentId) === Number(comboOption.id) }]"
                        role="radio"
                        :aria-checked="Number(form.followupParentId) === Number(comboOption.id)"
                        @mousedown.prevent
                        @click="selectParentCombo(comboOption)"
                      >
                        <span class="parent-combo-card-head">
                          <strong>{{ comboOption.starter || firstMoveText(comboOption) || '未命名起手' }}</strong>
                          <small>{{ Number(form.followupParentId) === Number(comboOption.id) ? '已选择' : '选择' }}</small>
                        </span>
                        <span class="parent-combo-route">{{ comboOption.route || comboOption.comboText || '-' }}</span>
                        <span class="parent-combo-meta">
                          <span>伤害 {{ comboOption.damage || 0 }}</span>
                          <span>Drive {{ comboOption.driveCost || 0 }}</span>
                          <span>SA {{ comboOption.saCost || 0 }}</span>
                          <span>{{ comboOption.difficulty || '未标难度' }}</span>
                          <span>{{ comboControlLabel(comboOption.controlType) }}</span>
                        </span>
                      </button>
                    </div>
                    <div v-else class="parent-combo-state">{{ parentComboEmptyText }}</div>
                  </div>
                </div>
              </div>
            </div>
            <p v-if="isFollowupMode && parentComboLoadError" class="form-error">{{ parentComboLoadError }}</p>

            <div class="upload-field-row">
              <span>连招路线</span>
              <div class="combo-route-builder">
                <div class="combo-route-head">
                  <span>按顺序填写每一招</span>
                  <button type="button" class="route-add" aria-label="增加一个招式" title="增加一个招式" @click="addRouteMove">
                    <span class="material-symbols-outlined">add</span>
                  </button>
                </div>
                <details class="notation-help">
                  <summary>
                    <span class="material-symbols-outlined" aria-hidden="true">translate</span>
                    <strong>字符转图标示例</strong>
                    <small>输入字符后会自动转换为手柄图标</small>
                    <span class="material-symbols-outlined notation-help-chevron" aria-hidden="true">expand_more</span>
                  </summary>
                  <div class="notation-help-content">
                    <section v-for="group in visibleNotationExampleGroups" :key="group.label" class="notation-example-group">
                      <h3>{{ group.label }}</h3>
                      <div class="notation-example-list">
                        <div v-for="example in group.examples" :key="example.code" class="notation-example-item">
                          <code>{{ example.code }}</code>
                          <span aria-hidden="true">→</span>
                          <ComboNotation :route="example.code" size="sm" />
                        </div>
                      </div>
                    </section>
                  </div>
                </details>
                <div class="route-move-list">
                  <template v-for="(move, index) in routeMoves" :key="index">
                    <div class="route-move-field">
                      <span>第 {{ index + 1 }} 招，例如 {{ routeMoveExamples[index] || '236HP' }}</span>
                      <div class="route-move-controls">
                        <select
                          v-if="isWorldTour"
                          v-model="routeCharacterIds[index]"
                          class="input-sf6 route-character-select"
                          :aria-label="`第 ${index + 1} 招角色`"
                          required
                        >
                          <option disabled value="">选择角色</option>
                          <option v-for="character in characters.characters" :key="character.id" :value="character.id">{{ character.name }}</option>
                        </select>
                        <input
                          v-model.trim="routeMoves[index]"
                          :ref="el => setRouteMoveInput(el, index)"
                          class="input-sf6 route-move-input"
                          :aria-label="`第 ${index + 1} 招`"
                          @keydown.enter.prevent="handleRouteMoveEnter(index)"
                        />
                        <button
                          type="button"
                          class="route-remove"
                          :disabled="routeMoves.length <= 1"
                          :aria-label="`删除第 ${index + 1} 招`"
                          :title="routeMoves.length <= 1 ? '至少保留一招' : `删除第 ${index + 1} 招`"
                          @click="removeRouteMove(index)"
                        >
                          <span class="material-symbols-outlined" aria-hidden="true">delete</span>
                        </button>
                      </div>
                    </div>
                    <span v-if="index < routeMoves.length - 1" class="route-separator">></span>
                  </template>
                </div>
                <p class="route-preview">保存为：{{ mergedRoute || '请先填写第 1 招' }}</p>
                <section class="route-detail-preview" aria-label="连招详情页预览">
                  <div class="route-detail-preview-head">
                    <span>DETAIL PREVIEW</span>
                    <strong>详情页效果</strong>
                  </div>
                  <div v-if="mergedRoute" class="route-detail-preview-scroll">
                    <WorldTourComboRoute
                      v-if="isWorldTour"
                      :route="mergedRoute"
                      :character-ids="worldTourRouteCharacterIds"
                      :characters="characters.characters"
                      size="lg"
                    />
                    <ComboNotation v-else :route="mergedRoute" size="lg" layout="vertical" />
                  </div>
                  <div v-else class="route-detail-preview-empty">
                    <span class="material-symbols-outlined" aria-hidden="true">sports_mma</span>
                    <p>填写连招后，这里会直接预览详情页里的输入展示。</p>
                  </div>
                </section>
              </div>
            </div>

            <section
              v-if="duplicateCheckLoading || duplicateCheckError || hasExactDuplicate || duplicateCandidates.length"
              :class="['duplicate-check-panel', { exact: hasExactDuplicate }]"
              aria-live="polite"
            >
              <div class="duplicate-check-head">
                <span class="material-symbols-outlined" aria-hidden="true">{{ hasExactDuplicate ? 'block' : 'content_copy' }}</span>
                <div>
                  <strong>{{ duplicateCheckTitle }}</strong>
                  <p>{{ duplicateCheckDescription }}</p>
                </div>
                <span v-if="duplicateCheckLoading" class="duplicate-check-loading">检查中</span>
              </div>
              <p v-if="duplicateCheckError" class="duplicate-check-error">{{ duplicateCheckError }}</p>
              <div v-if="duplicateCandidates.length" class="duplicate-candidate-list">
                <article v-for="candidate in duplicateCandidates.slice(0, 3)" :key="candidate.combo.id" class="duplicate-candidate">
                  <div class="duplicate-candidate-head">
                    <strong>#{{ candidate.combo.id }} {{ duplicateMatchLabel(candidate.matchType) }}</strong>
                    <span>{{ candidate.similarity }}% 相似</span>
                  </div>
                  <WorldTourComboRoute
                    v-if="comboMatchesControlType(candidate.combo, 'world-tour')"
                    :route="candidate.combo.route || candidate.combo.comboText || ''"
                    :character-ids="candidate.combo.routeCharacterIds"
                    :characters="characters.characters"
                    size="sm"
                  />
                  <ComboNotation v-else :route="candidate.combo.route || candidate.combo.comboText || ''" size="sm" />
                  <p>{{ candidate.combo.route || candidate.combo.comboText || '-' }}</p>
                  <router-link :to="`/combos/${candidate.combo.id}`">查看已有连招</router-link>
                </article>
              </div>
              <p v-if="duplicateCandidates.length > 3" class="duplicate-check-more">另有 {{ duplicateCandidates.length - 3 }} 条候选，可在后台审核时查看。</p>
            </section>

            <label class="upload-field-row">
              <span>伤害（必填），例如 2800</span>
              <input v-model.number="form.damage" class="input-sf6" type="number" min="0" required :aria-invalid="form.damage === ''" />
            </label>

            <label class="upload-field-row">
              <span>Drive 消耗，例如 2.5</span>
              <input v-model.number="form.driveCost" class="input-sf6" type="number" min="0" max="6" step="0.1" />
            </label>

            <label class="upload-field-row">
              <span>SA 消耗，例如 1</span>
              <input v-model.number="form.saCost" class="input-sf6" type="number" min="0" />
            </label>

            <div v-if="!isPressureMode" class="upload-field-row">
              <span>连招标签</span>
              <div class="combo-tag-picker">
                <label v-for="option in comboTagOptions" :key="option.value" class="combo-tag-option">
                  <input
                    v-model="form.tags"
                    type="checkbox"
                    :value="option.value"
                    :disabled="!form.tags.includes(option.value) && form.tags.length >= MAX_COMBO_TAGS"
                  />
                  <span>{{ option.label }}</span>
                </label>
                <p class="tag-limit-hint">已选择 {{ form.tags.length }} / {{ MAX_COMBO_TAGS }} 个标签</p>
              </div>
            </div>

            <div v-else class="upload-field-row tag-field-row">
              <span>压制类型（必选）</span>
              <div class="combo-tag-picker" role="radiogroup" aria-label="压制类型">
                <label v-for="option in pressureTypeOptions" :key="option.value" class="combo-tag-option">
                  <input v-model="form.pressureType" type="radio" name="pressure-type" :value="option.value" />
                  <span>{{ option.label }}</span>
                </label>
                <p class="tag-limit-hint">选择这段路线主要承担的压制目的。</p>
              </div>
            </div>

            <label class="upload-field-row">
              <span>有利帧数（必填），例如 +2</span>
              <input v-model.trim="form.advantageFrames" class="input-sf6" required :aria-invalid="!form.advantageFrames" />
            </label>

            <label class="upload-field-row">
              <span>难度（必填）</span>
              <select v-model="form.difficulty" class="input-sf6" required :aria-invalid="!form.difficulty">
                <option disabled value="">选择难度</option>
                <option value="简单">简单</option>
                <option value="中等">中等</option>
                <option value="困难">困难</option>
              </select>
            </label>

            <div class="upload-field-row">
              <span>演示视频（必填）</span>
              <div class="upload-field-stack">
                <label :class="['video-upload', { uploading: uploadingVideo }]">
                  <input type="file" accept="video/*" class="hidden" :disabled="uploadingVideo" @change="handleVideoUpload" />
                  <span class="material-symbols-outlined">movie</span>
                  <span>{{ videoUploadText }}</span>
                </label>
                <p class="form-hint">请上传能完整展示路线的实战或训练模式视频。</p>

                <div v-if="uploadingVideo" class="video-upload-progress" role="progressbar" :aria-valuenow="videoUploadProgress" aria-valuemin="0" aria-valuemax="100">
                  <span :style="{ width: `${videoUploadProgress}%` }"></span>
                </div>
                <p v-if="videoUploadError" class="form-error">{{ videoUploadError }}</p>

                <video
                  v-if="form.videoUrl"
                  class="video-preview"
                  :src="form.videoUrl"
                  controls
                  preload="metadata"
                ></video>
              </div>
            </div>

            <label class="upload-field-row">
              <span>训练要点，例如确认命中后再绿冲，注意墙距和资源</span>
              <textarea
                v-model.trim="form.trainingNotes"
                class="input-sf6 training-notes-input"
              ></textarea>
            </label>

            <p v-if="saveError" class="form-error">{{ saveError }}</p>

            <button class="btn-primary combo-upload-submit" type="button" :disabled="uploadingVideo || saving || hasExactDuplicate" @click="saveCombo">
              <span class="un-slant block">{{ submitText }}</span>
            </button>
          </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useCharacterStore } from '@/js/stores/characters'
import { useAuthStore } from '@/js/stores/auth'
import { useUiStore } from '@/js/stores/ui'
import { api } from '@/js/api'
import { buildPressureTags, comboControlLabel, comboControlOptions, comboListFromResponse, comboMatchesControlType, normalizeComboControlType, comboTagOptions, normalizeComboTags, pressureTypeOptions, splitPressureTags, MAX_COMBO_TAGS } from '@/js/utils/helpers'
import ComboNotation from '@/components/common/ComboNotation.vue'
import WorldTourComboRoute from '@/components/common/WorldTourComboRoute.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  embedded: { type: Boolean, default: false },
  combo: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const route = useRoute()
const characters = useCharacterStore()
const auth = useAuthStore()
const ui = useUiStore()
const routeMoves = ref([''])
const routeCharacterIds = ref([''])
const routeMoveInputs = ref([])
const uploadMode = ref('normal')
const parentCombos = ref([])
const parentComboQuery = ref('')
const parentComboDropdownOpen = ref(false)
const parentCombosLoading = ref(false)
const parentComboLoadError = ref('')
const uploadingVideo = ref(false)
const videoUploadProgress = ref(0)
const videoUploadError = ref('')
const saveError = ref('')
const saving = ref(false)
const duplicateCheckLoading = ref(false)
const duplicateCheckError = ref('')
const duplicateCheckResult = ref({ exactDuplicate: false, candidates: [] })
let duplicateCheckTimer = null
let duplicateCheckSequence = 0
const routeMoveExamples = ['2MK', 'DR', '5HP', '214HK', 'SA3']
const notationExampleGroups = [
  {
    label: '方向与指令',
    modes: ['classic', 'modern', 'world-tour'],
    examples: ['2', '6', '236', '214', '623', '360', '720'].map(code => ({ code })),
  },
  {
    label: '经典按键',
    modes: ['classic', 'world-tour'],
    examples: ['LP', 'MP', 'HP', 'LK', 'MK', 'HK'].map(code => ({ code })),
  },
  {
    label: '现代按键',
    modes: ['modern', 'world-tour'],
    examples: ['ML', 'MM', 'MH', 'SP'].map(code => ({ code })),
  },
  {
    label: '系统动作',
    modes: ['classic', 'modern', 'world-tour'],
    examples: ['DR', 'DRC', 'DI', 'DP', 'SA1'].map(code => ({ code })),
  },
]
const isEditing = computed(() => !!props.combo?.id)
const form = reactive({
  characterId: '',
  controlType: 'classic',
  damage: '',
  driveCost: '',
  saCost: '',
  advantageFrames: '',
  difficulty: '',
  tags: ['counter-hit'],
  pressureType: '',
  cornerOnly: false,
  videoUrl: '',
  trainingNotes: '',
  followupParentId: '',
  status: 'pending',
})

const isFollowupMode = computed(() => !isEditing.value && uploadMode.value === 'followup')
const isPressureMode = computed(() => isFollowupMode.value || (isEditing.value && !!form.followupParentId))
const isWorldTour = computed(() => form.controlType === 'world-tour')
const visibleNotationExampleGroups = computed(() => notationExampleGroups.filter(group => group.modes.includes(form.controlType)))
const selectableParentCombos = computed(() => parentCombos.value.filter(combo => (
  Number(combo.id) !== Number(props.combo?.id)
  && comboMatchesControlType(combo, form.controlType)
)))
const selectedParentCombo = computed(() => parentCombos.value.find(combo => (
  Number(combo.id) === Number(form.followupParentId)
  && comboMatchesControlType(combo, form.controlType)
)) || null)
const selectedParentComboLabel = computed(() => parentComboDisplayText(selectedParentCombo.value))
const parentComboEmptyText = computed(() => {
  if (parentComboQuery.value) return '没有匹配的原连招'
  return `该角色暂无可选择的${comboControlLabel(form.controlType)}原连招`
})
const filteredParentCombos = computed(() => {
  const query = normalizeParentComboSearch(parentComboQuery.value)
  const selectedLabel = normalizeParentComboSearch(selectedParentComboLabel.value)
  if (query && selectedLabel && query === selectedLabel) return selectableParentCombos.value
  if (!query) return selectableParentCombos.value
  return selectableParentCombos.value.filter(combo => {
    const text = [
      combo.starter,
      combo.route,
      combo.comboText,
      combo.difficulty,
      combo.damage,
      combo.driveCost,
      combo.saCost,
    ].join(' ').toUpperCase()
    return text.includes(query)
  })
})
const normalizedRouteEntries = computed(() => routeMoves.value
  .map((move, index) => ({
    move: uppercaseMoveText(move.trim()),
    characterId: Number(routeCharacterIds.value[index]) || null,
  }))
  .filter(entry => entry.move))
const mergedRoute = computed(() => normalizedRouteEntries.value.map(entry => entry.move).join(' > '))
const firstRouteMove = computed(() => normalizedRouteEntries.value[0]?.move || '')
const worldTourRouteCharacterIds = computed(() => normalizedRouteEntries.value.map(entry => entry.characterId))
const selectedSubmissionTags = computed(() => isPressureMode.value
  ? buildPressureTags(form.pressureType)
  : [...form.tags])
const duplicateCandidates = computed(() => Array.isArray(duplicateCheckResult.value?.candidates)
  ? duplicateCheckResult.value.candidates.filter(candidate => candidate?.combo?.id)
  : [])
const hasExactDuplicate = computed(() => Boolean(duplicateCheckResult.value?.exactDuplicate))
const canCheckDuplicates = computed(() => {
  if (!auth.isLoggedIn || !mergedRoute.value) return false
  if (isWorldTour.value) {
    return worldTourRouteCharacterIds.value.length === normalizedRouteEntries.value.length
      && worldTourRouteCharacterIds.value.every(Boolean)
  }
  if (!form.characterId) return false
  if (isFollowupMode.value && !form.followupParentId) return false
  if (isPressureMode.value && !form.pressureType) return false
  return true
})
const duplicateCheckTitle = computed(() => {
  if (hasExactDuplicate.value) return '已存在相同连招'
  if (duplicateCheckLoading.value) return '正在检查重复路线'
  if (duplicateCheckError.value) return '暂时无法检查重复路线'
  return '发现相似路线'
})
const duplicateCheckDescription = computed(() => {
  if (hasExactDuplicate.value) return '这条路线与现有连招身份一致，请查看已有连招后再调整。'
  if (duplicateCheckLoading.value) return '正在按角色、操作类型和招式顺序比对。'
  if (duplicateCheckError.value) return '仍可继续填写，提交时后端会再次进行强制查重。'
  return '这些路线可能是重复或变体，请确认区别后再提交。'
})
const videoUploadText = computed(() => {
  if (uploadingVideo.value) return `视频上传中 ${videoUploadProgress.value}%`
  return form.videoUrl ? '视频已上传，点击可更换' : '上传连招视频'
})
const submitText = computed(() => {
  if (hasExactDuplicate.value) return '已存在相同连招'
  if (saving.value) return isEditing.value ? '重新提交中...' : '保存中...'
  return isEditing.value ? '修改并重新提交审核' : '保存'
})

watch(() => props.modelValue, async value => {
  if (props.embedded) return
  document.body.style.overflow = value ? 'hidden' : ''
  if (!value) return
  if (!auth.isLoggedIn) {
    emit('update:modelValue', false)
    auth.openLogin()
    return
  }
  if (!characters.hasLoadedList) await characters.loadCharacters()
  await resetForm()
}, { immediate: true })

watch(() => props.embedded, async value => {
  if (!value) return
  if (!characters.hasLoadedList) await characters.loadCharacters()
  await resetForm()
}, { immediate: true })

watch(() => props.combo, async () => {
  await resetForm()
})

watch(() => form.characterId, async (characterId, previousCharacterId) => {
  if (!isFollowupMode.value || !characterId) return
  if (previousCharacterId && String(characterId) !== String(previousCharacterId)) parentComboQuery.value = ''
  const preferredParentId = Number(form.followupParentId) || null
  await loadParentCombos(preferredParentId)
  if (previousCharacterId && String(characterId) !== String(previousCharacterId) && !parentCombos.value.some(combo => Number(combo.id) === Number(form.followupParentId))) {
    form.followupParentId = ''
  }
})

watch(routeCharacterIds, characterIds => {
  if (!isWorldTour.value) return
  form.characterId = Number(characterIds[0]) || ''
}, { deep: true })

watch(uploadMode, async mode => {
  if (mode !== 'followup') {
    form.followupParentId = ''
    parentCombos.value = []
    parentComboQuery.value = ''
    parentComboDropdownOpen.value = false
    parentComboLoadError.value = ''
    return
  }
  if (form.characterId) await loadParentCombos(Number(form.followupParentId) || null)
})

watch(() => JSON.stringify({
  characterId: form.characterId,
  controlType: form.controlType,
  route: mergedRoute.value,
  routeCharacterIds: worldTourRouteCharacterIds.value,
  followupParentId: form.followupParentId,
  tags: selectedSubmissionTags.value,
  cornerOnly: form.cornerOnly,
  excludeId: props.combo?.id || null,
}), scheduleDuplicateCheck)

onBeforeUnmount(() => {
  if (!props.embedded) document.body.style.overflow = ''
  if (duplicateCheckTimer) clearTimeout(duplicateCheckTimer)
})

function uppercaseMoveText(value) {
  return String(value || '').replace(/[A-Za-z]+/g, match => match.toUpperCase())
}

function scheduleDuplicateCheck() {
  if (duplicateCheckTimer) clearTimeout(duplicateCheckTimer)
  duplicateCheckSequence += 1
  duplicateCheckLoading.value = false
  duplicateCheckError.value = ''
  if (!canCheckDuplicates.value) {
    duplicateCheckResult.value = { exactDuplicate: false, candidates: [] }
    return
  }
  const sequence = duplicateCheckSequence
  duplicateCheckLoading.value = true
  duplicateCheckTimer = setTimeout(() => checkDuplicates(sequence), 400)
}

async function checkDuplicates(sequence) {
  try {
    const result = await api.checkComboDuplicates({
      characterId: isWorldTour.value ? null : Number(form.characterId),
      starter: firstRouteMove.value,
      route: mergedRoute.value,
      controlType: normalizeComboControlType(form.controlType),
      routeCharacterIds: isWorldTour.value ? worldTourRouteCharacterIds.value : [],
      type: selectedSubmissionTags.value[0] || '',
      tags: selectedSubmissionTags.value,
      cornerOnly: Boolean(form.cornerOnly),
      followupParentId: (isFollowupMode.value || isEditing.value) && form.followupParentId
        ? Number(form.followupParentId)
        : null,
      excludeId: isEditing.value ? Number(props.combo?.id) || null : null,
    })
    if (sequence !== duplicateCheckSequence) return
    duplicateCheckResult.value = {
      exactDuplicate: Boolean(result?.exactDuplicate),
      candidates: Array.isArray(result?.candidates) ? result.candidates : [],
    }
  } catch (error) {
    if (sequence !== duplicateCheckSequence) return
    duplicateCheckResult.value = { exactDuplicate: false, candidates: [] }
    duplicateCheckError.value = error.message || '重复检测失败'
  } finally {
    if (sequence === duplicateCheckSequence) duplicateCheckLoading.value = false
  }
}

function duplicateMatchLabel(matchType) {
  return {
    exact: '完全重复',
    'same-route': '同路线变体',
    historical: '历史驳回路线',
    similar: '高度相似',
  }[matchType] || '相似路线'
}

async function resetForm() {
  if (duplicateCheckTimer) clearTimeout(duplicateCheckTimer)
  duplicateCheckTimer = null
  duplicateCheckSequence += 1
  const combo = props.combo || null
  const savedTags = combo ? normalizeComboTags(combo, combo.type) : []
  const savedPressureTags = splitPressureTags(savedTags)
  const queryMode = String(route.query.mode || '')
  const queryComboId = Number(route.query.comboId || route.query.parentComboId || 0)
  const queryCharacterId = Number(route.query.characterId || 0)
  const queryControlType = String(route.query.controlType || '')
  uploadMode.value = !combo && queryMode === 'followup' ? 'followup' : 'normal'
  Object.assign(form, {
    characterId: combo?.characterId || (queryCharacterId || characters.characters[0]?.id || ''),
    controlType: normalizeComboControlType(combo?.controlType || queryControlType),
    damage: combo?.damage ?? '',
    driveCost: combo?.driveCost ?? '',
    saCost: combo?.saCost ?? '',
    advantageFrames: combo?.advantageFrames || '',
    difficulty: combo?.difficulty || '',
    tags: combo && !combo.followupParentId ? savedTags : ['counter-hit'],
    pressureType: combo?.followupParentId ? savedPressureTags.type : '',
    cornerOnly: Boolean(combo?.cornerOnly),
    videoUrl: combo?.videoUrl || '',
    trainingNotes: combo?.trainingNotes || '',
    followupParentId: combo?.followupParentId || (uploadMode.value === 'followup' && queryComboId ? queryComboId : ''),
    status: 'pending',
  })
  routeMoves.value = splitRoute(combo?.route || combo?.comboText || '')
  const savedRouteCharacterIds = Array.isArray(combo?.routeCharacterIds) ? combo.routeCharacterIds : []
  routeCharacterIds.value = routeMoves.value.map((_, index) => (
    savedRouteCharacterIds[index]
    || (form.controlType === 'world-tour' && combo ? combo.characterId : '')
    || ''
  ))
  routeMoveInputs.value = []
  videoUploadError.value = ''
  videoUploadProgress.value = 0
  saveError.value = ''
  saving.value = false
  duplicateCheckResult.value = { exactDuplicate: false, candidates: [] }
  duplicateCheckError.value = ''
  duplicateCheckLoading.value = false
  parentComboQuery.value = ''
  if (uploadMode.value === 'followup') {
    await hydrateInitialFollowupParent(queryComboId)
  } else {
    parentCombos.value = []
    parentComboLoadError.value = ''
  }
}

function setUploadMode(mode) {
  uploadMode.value = mode
}

function setControlType(type) {
  const nextType = normalizeComboControlType(type)
  if (form.controlType === nextType) return
  form.controlType = nextType
  if (nextType === 'world-tour') {
    routeCharacterIds.value = routeMoves.value.map(() => '')
    form.characterId = ''
  }
  if (form.followupParentId && !selectedParentCombo.value) {
    form.followupParentId = ''
    parentComboQuery.value = ''
  }
}

function selectParentCombo(combo) {
  form.followupParentId = combo?.id || ''
  parentComboQuery.value = parentComboDisplayText(combo)
  closeParentComboDropdown()
}

function firstMoveText(combo) {
  return String(combo?.route || combo?.comboText || '').split(' > ')[0]?.trim()
}

function parentComboDisplayText(combo) {
  if (!combo) return ''
  return combo.route || combo.comboText || combo.starter || ''
}

function normalizeParentComboSearch(value) {
  return String(value || '').trim().toUpperCase()
}

function openParentComboDropdown() {
  if (!isFollowupMode.value) return
  parentComboDropdownOpen.value = true
}

function handleParentComboInput() {
  openParentComboDropdown()
  if (
    form.followupParentId &&
    normalizeParentComboSearch(parentComboQuery.value) !== normalizeParentComboSearch(selectedParentComboLabel.value)
  ) {
    form.followupParentId = ''
  }
}

function closeParentComboDropdown() {
  parentComboDropdownOpen.value = false
}

function toggleParentComboDropdown() {
  parentComboDropdownOpen.value = !parentComboDropdownOpen.value
}

function handleParentComboBlur(event) {
  if (event.currentTarget.contains(event.relatedTarget)) return
  closeParentComboDropdown()
  if (!parentComboQuery.value.trim() && selectedParentCombo.value) {
    parentComboQuery.value = selectedParentComboLabel.value
  }
}

async function hydrateInitialFollowupParent(parentId) {
  if (!parentId) {
    if (form.characterId) await loadParentCombos()
    return
  }
  try {
    const parentCombo = await api.getCombo(parentId)
    if (parentCombo?.characterId) {
      form.characterId = parentCombo.characterId
      form.controlType = normalizeComboControlType(parentCombo.controlType)
      form.followupParentId = parentCombo.id
    }
  } catch (_) {
    parentComboLoadError.value = '原连招不存在或暂时无法同步'
  }
  if (form.characterId) await loadParentCombos(parentId)
}

async function loadParentCombos(preferredParentId = null) {
  if (!form.characterId) return
  parentCombosLoading.value = true
  parentComboLoadError.value = ''
  try {
    const combos = await api.getComboParentOptions(form.characterId, { controlType: form.controlType })
    parentCombos.value = comboListFromResponse(combos)
    const preferredCombo = parentCombos.value.find(combo => Number(combo.id) === Number(preferredParentId) && comboMatchesControlType(combo, form.controlType))
    if (preferredCombo) {
      form.followupParentId = preferredParentId
      parentComboQuery.value = parentComboDisplayText(preferredCombo)
    } else if (preferredParentId) {
      form.followupParentId = ''
      parentComboQuery.value = ''
    }
  } catch (error) {
    parentCombos.value = []
    parentComboLoadError.value = error.message || '原连招列表同步失败'
  } finally {
    parentCombosLoading.value = false
  }
}

function splitRoute(routeText) {
  const moves = String(routeText || '')
    .split(/\s*>\s*/)
    .map(move => move.trim())
    .filter(Boolean)
  return moves.length ? moves : ['']
}

function setRouteMoveInput(el, index) {
  if (el) routeMoveInputs.value[index] = el
}

function focusRouteMove(index) {
  nextTick(() => routeMoveInputs.value[index]?.focus())
}

function addRouteMove() {
  routeMoves.value.push('')
  routeCharacterIds.value.push('')
  focusRouteMove(routeMoves.value.length - 1)
}

function removeRouteMove(index) {
  if (routeMoves.value.length <= 1) return
  routeMoves.value.splice(index, 1)
  routeCharacterIds.value.splice(index, 1)
  routeMoveInputs.value.splice(index, 1)
  focusRouteMove(Math.min(index, routeMoves.value.length - 1))
}

function handleRouteMoveEnter(index) {
  if (!routeMoves.value[index]?.trim()) return
  const nextIndex = index + 1
  if (routeMoves.value[nextIndex] === '') {
    focusRouteMove(nextIndex)
    return
  }
  routeMoves.value.splice(nextIndex, 0, '')
  routeCharacterIds.value.splice(nextIndex, 0, '')
  focusRouteMove(nextIndex)
}

async function handleVideoUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  uploadingVideo.value = true
  videoUploadProgress.value = 0
  videoUploadError.value = ''
  try {
    const result = await api.uploadFile(file, 'combo-video', {
      onProgress: value => { videoUploadProgress.value = value },
    })
    form.videoUrl = result.url
    videoUploadProgress.value = 100
  } catch (error) {
    videoUploadError.value = error.message || '视频上传失败'
  } finally {
    uploadingVideo.value = false
  }
}

async function saveCombo() {
  if (!auth.isLoggedIn) {
    saveError.value = '请先登录后再上传连招'
    return
  }
  if (saving.value || uploadingVideo.value) return
  if (hasExactDuplicate.value) {
    saveError.value = '已存在相同连招，请先查看重复候选'
    return
  }
  if (!isWorldTour.value && !form.characterId) {
    saveError.value = '请选择角色'
    return
  }
  if (!mergedRoute.value) {
    saveError.value = '请填写连招路线'
    return
  }
  if (isWorldTour.value && worldTourRouteCharacterIds.value.some(characterId => !characterId)) {
    saveError.value = '请为每一招选择角色'
    return
  }
  if (isFollowupMode.value && !form.followupParentId) {
    saveError.value = '请选择这个后续压制连招要接在哪个原连招后面'
    return
  }
  if (isPressureMode.value && !form.pressureType) {
    saveError.value = '请选择这条路线的压制类型'
    return
  }
  if (form.damage === '' || form.damage === null || !Number.isFinite(Number(form.damage))) {
    saveError.value = '请填写连招伤害'
    return
  }
  if (!String(form.advantageFrames || '').trim()) {
    saveError.value = '请填写有利帧'
    return
  }
  if (!form.difficulty) {
    saveError.value = '请选择连招难度'
    return
  }
  if (!form.videoUrl) {
    saveError.value = '请先上传连招演示视频'
    videoUploadError.value = '上传连招必须提供演示视频'
    return
  }
  saving.value = true
  saveError.value = ''
  const submissionTags = selectedSubmissionTags.value
  const { pressureType: _pressureType, ...requestForm } = form
  const payload = {
    ...requestForm,
    characterId: isWorldTour.value ? null : Number(form.characterId),
    controlType: normalizeComboControlType(form.controlType),
    routeCharacterIds: isWorldTour.value ? worldTourRouteCharacterIds.value : [],
    type: submissionTags[0] || 'counter-hit',
    tags: submissionTags,
    starter: firstRouteMove.value,
    route: mergedRoute.value,
    comboText: mergedRoute.value,
    damage: Number(form.damage),
    driveCost: Number(form.driveCost),
    saCost: Number(form.saCost),
    cornerOnly: !!form.cornerOnly,
    followupParentId: (isFollowupMode.value || isEditing.value) && form.followupParentId ? Number(form.followupParentId) : null,
    status: 'pending',
  }
  try {
    const savedCombo = isEditing.value
      ? await api.updateCombo(props.combo.id, payload)
      : await api.createCombo(payload)
    emit('saved', savedCombo || payload)
    emit('update:modelValue', false)
    await ui.alertDialog({
      title: isEditing.value ? '连招已重新提交' : '连招已上传',
      message: isEditing.value ? '修改后的连招已重新进入审核队列。' : '连招已上传，待审核。',
      tone: 'success',
    })
  } catch (error) {
    saveError.value = error.message || '保存失败'
  } finally {
    saving.value = false
  }
}

function close() {
  if (saving.value || uploadingVideo.value) return
  emit('update:modelValue', false)
}
</script>

<style scoped>
.combo-upload-panel {
  position: relative;
  width: min(620px, 100%);
  max-height: calc(100dvh - 40px);
  overflow-y: auto;
  padding: 28px;
  color: var(--ink-text);
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.combo-upload-panel.editing:not(.embedded) {
  width: min(780px, 100%);
}

.combo-upload-panel.embedded {
  width: 100%;
  max-height: none;
}

.combo-upload-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  color: var(--ink-muted);
  border: 1px solid var(--ink-line);
  border-radius: 999px;
}

.combo-upload-head {
  padding-right: 48px;
  margin-bottom: 22px;
}

.combo-upload-head span,
.combo-route-head,
.route-preview,
.form-hint,
.form-error {
  font-family: 'JetBrains Mono';
}

.combo-upload-head span {
  color: var(--accent-red);
  font-size: 12px;
  letter-spacing: 0;
}

.combo-upload-head h2 {
  margin: 8px 0 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
}

.combo-upload-head p {
  max-width: 52ch;
  margin: 10px 0 0;
  color: var(--ink-text-soft);
  line-height: 1.6;
}

.combo-upload-form {
  display: grid;
  gap: 14px;
}

.upload-field-row {
  display: grid;
  grid-template-columns: minmax(150px, 220px) minmax(0, 1fr);
  align-items: start;
  gap: 14px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.upload-field-row > span {
  min-height: 44px;
  display: flex;
  align-items: center;
  color: var(--ink-muted);
  line-height: 1.45;
}

.upload-field-row > .input-sf6,
.upload-field-row > .combo-route-builder,
.upload-field-row > .upload-field-stack,
.upload-field-row > .upload-mode-tabs,
.upload-field-row > .parent-combo-picker,
.upload-field-row > .combo-tag-picker {
  min-width: 0;
}

.upload-mode-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 4px;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.combo-control-tabs {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.combo-control-tabs .upload-mode-tab {
  min-width: 0;
  padding-inline: 6px;
}

.upload-mode-tab {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  color: var(--ink-text-soft);
  border: 1px solid transparent;
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 900;
}

.upload-mode-tab.active {
  color: #190607;
  border-color: var(--accent-red);
  background: var(--accent-red);
}

.upload-mode-tab.is-classic.active,
.upload-mode-tab.is-modern.active,
.upload-mode-tab.is-world-tour.active {
  color: var(--ink-text);
}

.upload-mode-tab.is-classic.active {
  background: rgba(139, 92, 246, 0.34);
  border-color: rgba(167, 139, 250, 0.82);
  box-shadow: inset 0 0 0 1px rgba(221, 214, 254, 0.14);
}

.upload-mode-tab.is-modern.active {
  background: rgba(249, 115, 22, 0.34);
  border-color: rgba(251, 146, 60, 0.82);
  box-shadow: inset 0 0 0 1px rgba(255, 237, 213, 0.14);
}

.upload-mode-tab.is-world-tour.active {
  background: rgba(49, 213, 230, 0.2);
  border-color: rgba(49, 213, 230, 0.72);
  box-shadow: inset 0 0 0 1px rgba(207, 250, 254, 0.1);
}

.combo-tag-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.combo-tag-option {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 11px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  cursor: pointer;
}

.combo-tag-option:has(input:checked) {
  color: #190607;
  border-color: var(--accent-red);
  background: var(--accent-red);
}

.combo-tag-option:has(input:disabled) {
  cursor: not-allowed;
  opacity: 0.46;
}

.combo-tag-option input {
  width: 14px;
  height: 14px;
  accent-color: var(--accent-red);
}

.tag-limit-hint {
  flex: 1 0 100%;
  margin: 2px 0 0;
  color: var(--ink-muted);
  font-size: 11px;
}

.upload-field-stack {
  display: grid;
  gap: 10px;
}

.parent-combo-picker {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.parent-combo-combobox {
  position: relative;
  min-width: 0;
}

.parent-combo-input-wrap {
  position: relative;
}

.parent-combo-search {
  width: 100%;
  padding-right: 46px;
}

.parent-combo-toggle {
  position: absolute;
  top: 50%;
  right: 6px;
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-muted);
  border: 0;
  border-radius: 4px;
  transform: translateY(-50%);
}

.parent-combo-toggle:hover,
.parent-combo-toggle:focus-visible {
  color: var(--ink-text);
  background: var(--ink-surface-high);
}

.parent-combo-toggle .material-symbols-outlined {
  font-size: 20px;
}

.parent-combo-dropdown {
  position: absolute;
  z-index: 30;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  padding: 10px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  box-shadow: 0 20px 44px rgba(0, 0, 0, 0.42);
}

.parent-combo-list {
  display: grid;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 4px;
}

.parent-combo-card {
  width: 100%;
  display: grid;
  gap: 8px;
  padding: 12px;
  color: var(--ink-text-soft);
  text-align: left;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.parent-combo-card:hover,
.parent-combo-card:focus-visible {
  color: var(--ink-text);
  border-color: rgba(255, 64, 84, 0.62);
  background: rgba(255, 64, 84, 0.08);
}

.parent-combo-card.selected {
  color: var(--ink-text);
  border-color: var(--accent-red);
  background: rgba(255, 64, 84, 0.14);
  box-shadow: inset 0 0 0 1px rgba(255, 64, 84, 0.2);
}

.parent-combo-card-head,
.parent-combo-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.parent-combo-card-head strong {
  color: var(--ink-text);
  font-size: 14px;
  overflow-wrap: anywhere;
}

.parent-combo-card-head small {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  color: var(--accent-red);
  border: 1px solid rgba(255, 64, 84, 0.42);
  border-radius: 4px;
  font-size: 11px;
}

.parent-combo-card.selected .parent-combo-card-head small {
  color: #190607;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

.parent-combo-route {
  color: var(--ink-text);
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.parent-combo-meta {
  justify-content: flex-start;
}

.parent-combo-meta span {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  color: var(--ink-muted);
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  font-size: 11px;
}

.parent-combo-state {
  min-height: 72px;
  display: grid;
  place-items: center;
  padding: 12px;
  color: var(--ink-muted);
  background: var(--ink-surface-low);
  border: 1px dashed var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.combo-route-builder {
  display: grid;
  gap: 10px;
  padding: 12px;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.combo-route-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--ink-text-soft);
  font-size: 13px;
}

.route-add {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #190607;
  border: 0;
  border-radius: 4px;
  background: var(--accent-red);
}

.route-add .material-symbols-outlined {
  font-size: 20px;
}

.route-move-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
}

.notation-help {
  background: rgba(49, 213, 230, 0.045);
  border: 1px solid rgba(49, 213, 230, 0.24);
  border-radius: 4px;
}

.notation-help summary {
  min-height: 42px;
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  color: var(--ink-text-soft);
  cursor: pointer;
  list-style: none;
}

.notation-help summary::-webkit-details-marker {
  display: none;
}

.notation-help summary > .material-symbols-outlined:first-child {
  color: var(--accent-cyan-soft);
  font-size: 19px;
}

.notation-help summary strong {
  color: var(--ink-text);
  font-size: 12px;
}

.notation-help summary small {
  color: var(--ink-muted);
  font-size: 11px;
  text-align: right;
}

.notation-help-chevron {
  font-size: 18px;
  transition: transform 0.18s ease;
}

.notation-help[open] .notation-help-chevron {
  transform: rotate(180deg);
}

.notation-help-content {
  display: grid;
  gap: 13px;
  padding: 12px;
  border-top: 1px solid rgba(49, 213, 230, 0.18);
}

.notation-example-group {
  display: grid;
  gap: 7px;
}

.notation-example-group h3 {
  margin: 0;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
  font-weight: 800;
}

.notation-example-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.notation-example-item {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 7px;
  color: var(--ink-muted);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 3px;
}

.notation-example-item code {
  color: var(--accent-cyan-soft);
  font-family: 'JetBrains Mono';
  font-size: 11px;
  font-weight: 900;
}

@media (prefers-reduced-motion: reduce) {
  .notation-help-chevron {
    transition: none;
  }
}

.route-move-field {
  display: grid;
  grid-template-columns: minmax(120px, 180px) minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.route-move-field > span {
  color: var(--ink-muted);
  line-height: 1.45;
}

.route-move-input {
  flex: 1 1 0;
  min-width: 0;
  width: auto;
}

.route-move-controls {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.route-character-select {
  flex: 0 0 138px;
  width: 138px;
}

.route-remove {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 38px;
  padding: 0;
  color: #ff8c96;
  border: 1px solid rgba(255, 77, 90, 0.42);
  border-radius: 4px;
  background: rgba(255, 77, 90, 0.08);
  cursor: pointer;
}

.route-remove:hover:not(:disabled) {
  color: #fff;
  border-color: var(--accent-red);
  background: rgba(255, 77, 90, 0.24);
}

.route-remove:disabled {
  cursor: not-allowed;
  opacity: 0.3;
}

.route-remove .material-symbols-outlined {
  font-size: 19px;
}

.route-separator {
  color: var(--accent-red);
  font-family: 'JetBrains Mono';
  font-size: 14px;
}

.route-preview {
  color: var(--ink-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.route-detail-preview {
  display: grid;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.026);
  border: 1px solid rgba(150, 164, 184, 0.18);
  border-radius: 4px;
}

.route-detail-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}

.route-detail-preview-head span {
  color: var(--accent-red);
  font-weight: 900;
}

.route-detail-preview-head strong {
  color: var(--ink-text-soft);
  font-size: 12px;
}

.route-detail-preview-scroll {
  max-height: 250px;
  min-height: 96px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 4px 8px 4px 0;
}

.route-detail-preview-scroll::-webkit-scrollbar {
  width: 6px;
}

.route-detail-preview-scroll::-webkit-scrollbar-track {
  background: var(--ink-surface-low);
}

.route-detail-preview-scroll::-webkit-scrollbar-thumb {
  background: var(--ink-line);
  border-radius: 999px;
}

.route-detail-preview-scroll:hover::-webkit-scrollbar-thumb {
  background: var(--accent-red);
}

.route-detail-preview-empty {
  min-height: 96px;
  display: grid;
  place-items: center;
  gap: 8px;
  padding: 14px;
  color: var(--ink-muted);
  border: 1px dashed var(--ink-line);
  border-radius: 4px;
  text-align: center;
}

.route-detail-preview-empty .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 30px;
}

.route-detail-preview-empty p {
  max-width: 34ch;
  margin: 0;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  line-height: 1.55;
}

.duplicate-check-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  color: #ffe7b3;
  background: rgba(245, 158, 11, 0.09);
  border: 1px solid rgba(245, 158, 11, 0.42);
  border-radius: 4px;
}

.duplicate-check-panel.exact {
  color: #ffd3d9;
  background: rgba(255, 64, 84, 0.1);
  border-color: rgba(255, 64, 84, 0.52);
}

.duplicate-check-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.duplicate-check-head > .material-symbols-outlined {
  flex: 0 0 auto;
  margin-top: 1px;
  font-size: 21px;
}

.duplicate-check-head div {
  min-width: 0;
  flex: 1;
}

.duplicate-check-head strong {
  color: currentColor;
  font-size: 14px;
}

.duplicate-check-head p,
.duplicate-check-error,
.duplicate-check-more,
.duplicate-candidate p {
  margin: 4px 0 0;
  color: var(--ink-text-soft);
  font-size: 12px;
  line-height: 1.55;
}

.duplicate-check-loading {
  flex: 0 0 auto;
  color: currentColor;
  font-family: 'JetBrains Mono';
  font-size: 11px;
}

.duplicate-check-error {
  color: #ffd7a1;
}

.duplicate-candidate-list {
  display: grid;
}

.duplicate-candidate {
  min-width: 0;
  display: grid;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  overflow-x: auto;
}

.duplicate-candidate:last-child {
  padding-bottom: 0;
}

.duplicate-candidate-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--ink-text);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}

.duplicate-candidate-head > span {
  flex: 0 0 auto;
  color: currentColor;
}

.duplicate-candidate a {
  width: max-content;
  color: currentColor;
  font-size: 12px;
  font-weight: 900;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.video-upload {
  min-height: 48px;
  border: 1px dashed var(--ink-line-strong);
  border-radius: 4px;
  color: var(--ink-text-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  font-family: 'JetBrains Mono';
  font-size: 13px;
  transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
}

.video-upload:hover {
  border-color: var(--accent-red);
  color: var(--ink-text);
  background: rgba(91, 214, 199, 0.08);
}

.video-upload.uploading {
  cursor: wait;
  pointer-events: none;
  border-color: rgba(91, 214, 199, 0.62);
  background: rgba(91, 214, 199, 0.1);
}

.video-upload-progress {
  height: 6px;
  overflow: hidden;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 999px;
}

.video-upload-progress span {
  display: block;
  height: 100%;
  background: var(--accent-red);
  transition: width 0.16s ease;
}

.video-preview {
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  background: var(--ink-surface-low);
}

.training-notes-input {
  min-height: 96px;
  resize: vertical;
}

.form-hint {
  margin: 0;
  color: var(--ink-muted);
  font-size: 12px;
}

.form-error {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid rgba(255, 118, 135, 0.28);
  border-radius: 4px;
  background: rgba(255, 86, 105, 0.11);
  color: var(--state-error);
  font-size: 12px;
}

.combo-upload-submit:disabled {
  opacity: 0.62;
  cursor: progress;
}

.combo-upload-enter-active,
.combo-upload-leave-active {
  transition: opacity 0.18s ease;
}

.combo-upload-enter-active .combo-upload-panel,
.combo-upload-leave-active .combo-upload-panel {
  transition: transform 0.18s ease;
}

.combo-upload-enter-from,
.combo-upload-leave-to {
  opacity: 0;
}

.combo-upload-enter-from .combo-upload-panel,
.combo-upload-leave-to .combo-upload-panel {
  transform: translateY(10px);
}

@media (max-width: 560px) {
  .combo-upload-panel {
    max-height: calc(100dvh - 20px);
    padding: 20px;
  }

  .combo-upload-head h2 {
    font-size: 28px;
  }

  .upload-field-row,
  .route-move-field {
    grid-template-columns: 1fr;
  }

  .route-move-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .route-remove {
    align-self: flex-end;
  }

  .notation-help summary {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .notation-help summary small {
    display: none;
  }

  .route-character-select {
    flex-basis: auto;
    width: 100%;
  }

  .upload-field-row > span {
    min-height: auto;
  }

  .parent-combo-dropdown {
    position: static;
    margin-top: 8px;
  }

  .parent-combo-list {
    max-height: 280px;
  }
}
</style>
