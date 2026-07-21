import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/js/api'
import { normalizeComboControlType, normalizeComboTags, normalizeRouteCharacterIds } from '@/js/utils/helpers'

export const useCharacterStore = defineStore('characters', () => {
  const characters = ref([])
  const loading = ref(false)
  const error = ref('')
  const hasLoadedList = ref(false)
  let listRequest = null

  function normalizeCharacter(character) {
    return {
      stats: { driveEfficiency: 0, parryRate: 0 },
      badges: [],
      frames: [],
      combos: [],
      difficulty: character.archetype || '',
      image: character.image || character.avatar || '',
      easeRating: character.easeRating || 0,
      tier: character.tier || '-',
      nameJa: character.nameJa || '',
      ...character,
    }
  }

  function normalizeCharacterCombo(combo, character) {
    const routeCharacterIds = normalizeRouteCharacterIds(combo.routeCharacterIds)
    return {
      ...combo,
      characterId: combo.characterId || character.id,
      characterName: combo.characterName || character.name || '',
      characterAvatar: combo.characterAvatar || character.avatar || character.image || '',
      starter: combo.starter || String(combo.route || combo.comboText || '').split(' > ')[0]?.trim() || '',
      driveCost: combo.driveCost ?? 0,
      saCost: combo.saCost ?? 0,
      likes: combo.likes ?? 0,
      favorites: combo.favorites ?? 0,
      route: combo.route || combo.comboText || '',
      controlType: normalizeComboControlType(combo.controlType),
      routeCharacterIds,
      tags: normalizeComboTags(combo, combo.type),
    }
  }

  async function loadCharacters(options = {}) {
    const { force = false } = options
    if (!force && hasLoadedList.value) return characters.value
    if (listRequest) return listRequest

    loading.value = true
    error.value = ''
    listRequest = api.getCharacters()
      .then(result => {
        characters.value = result.map(normalizeCharacter)
        hasLoadedList.value = true
        return characters.value
      })
      .catch(e => {
        error.value = e.message || '角色加载失败'
        return characters.value
      })
      .finally(() => {
        loading.value = false
        listRequest = null
      })
    return listRequest
  }

  function findById(id) {
    return characters.value.find(c => c.id === Number(id)) || null
  }

  function normalizeComboPage(result, character) {
    const comboList = Array.isArray(result) ? result : result?.list || []
    return {
      list: comboList.map(combo => normalizeCharacterCombo(combo, character)),
      page: Number(result?.page || 1),
      pageSize: Number(result?.pageSize || comboList.length || 0),
      total: Number(result?.total ?? comboList.length),
    }
  }

  async function loadCharacterDetail(id) {
    loading.value = true
    error.value = ''
    try {
      const [character, frames] = await Promise.all([
        api.getCharacter(id),
        api.getFrames(id),
      ])
      if (!character) return null
      const normalized = normalizeCharacter({
        ...character,
        frames: (frames || [])
          .map(frame => ({
            ...frame,
            move: frame.move || frame.moveName,
            damage: frame.damage || '',
            controlType: normalizeComboControlType(frame.controlType),
          }))
          .sort((a, b) => (a.displayOrder ?? a.id ?? 0) - (b.displayOrder ?? b.id ?? 0)),
        combos: [],
      })
      const index = characters.value.findIndex(c => c.id === Number(id))
      if (index >= 0) characters.value[index] = normalized
      else characters.value.push(normalized)
      return normalized
    } catch (e) {
      error.value = e.message || '角色详情加载失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function loadCharacterCombos(id, params = {}) {
    const character = findById(id) || await api.getCharacter(id)
    const result = await api.getCombos(id, params)
    return normalizeComboPage(result, character)
  }

  async function loadCharacterComboFilterOptions(id, params = {}) {
    const result = await api.getComboFilterOptions(id, params)
    return {
      starters: Array.isArray(result?.starters) ? result.starters : [],
      tags: Array.isArray(result?.tags) ? result.tags : [],
    }
  }

  return { characters, loading, error, hasLoadedList, loadCharacters, loadCharacterDetail, loadCharacterCombos, loadCharacterComboFilterOptions, findById }
})
