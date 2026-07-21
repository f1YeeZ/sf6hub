import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/js/api'
import { normalizeComboControlType, normalizeComboTags, normalizeRouteCharacterIds } from '@/js/utils/helpers'

export const useComboStore = defineStore('combos', () => {
  const combos = ref([])

  function normalizeCombos(comboList, characterList = []) {
    const characterById = new Map(characterList.map(character => [Number(character.id), character]))
    return comboList.map(combo => {
      const character = characterById.get(Number(combo.characterId))
      const routeCharacterIds = normalizeRouteCharacterIds(combo.routeCharacterIds)
      return {
        ...combo,
        authorName: combo.author || combo.authorName || '',
        authorId: combo.authorId || null,
        characterName: combo.characterName || character?.name || '',
        characterAvatar: combo.characterAvatar || character?.avatar || character?.image || '',
        driveCost: combo.driveCost ?? 0,
        saCost: combo.saCost ?? 0,
        likes: combo.likes ?? 0,
        favorites: combo.favorites ?? 0,
        liked: !!combo.liked,
        favorited: !!combo.favorited,
        route: combo.route || combo.comboText || '',
        controlType: normalizeComboControlType(combo.controlType),
        routeCharacterIds,
        routeCharacters: routeCharacterIds.map(id => characterById.get(id) || { id, name: `角色 #${id}` }),
        tags: normalizeComboTags(combo, combo.type),
      }
    })
  }

  function normalizeComboPage(result, characterList = []) {
    const comboList = Array.isArray(result) ? result : result?.list || []
    return {
      list: normalizeCombos(comboList, characterList),
      page: Number(result?.page || 1),
      pageSize: Number(result?.pageSize || comboList.length || 0),
      total: Number(result?.total ?? comboList.length),
    }
  }

  async function findById(id, options = {}) {
    const cached = combos.value.find(combo => combo.id === Number(id))
    if (cached && !options.force) return cached
    const [rawCombo, characterList] = await Promise.all([
      api.getCombo(id),
      api.getCharacters(),
    ])
    const combo = rawCombo ? normalizeCombos([rawCombo], characterList)[0] : null
    if (combo) {
      const comboId = Number(combo.id)
      const index = combos.value.findIndex(item => Number(item.id) === comboId)
      if (index >= 0) combos.value[index] = { ...combos.value[index], ...combo }
      else combos.value.push(combo)
    }
    return combo
  }

  async function getUserCombos(userId, params = {}) {
    const [result, characterList] = await Promise.all([
      api.getUserCombos(userId, params),
      api.getCharacters(),
    ])
    return normalizeComboPage(result, characterList)
  }

  async function getUserFavoriteCombos(userId, params = {}) {
    const [result, characterList] = await Promise.all([
      api.getUserFavoriteCombos(userId, params),
      api.getCharacters(),
    ])
    return normalizeComboPage(result, characterList)
  }

  async function getWorldTourCombos(params = {}) {
    const [result, characterList] = await Promise.all([
      api.getWorldTourCombos(params),
      api.getCharacters(),
    ])
    return normalizeComboPage(result, characterList)
  }

  async function getWorldTourComboFilterOptions() {
    const result = await api.getWorldTourComboFilterOptions()
    return {
      starters: Array.isArray(result?.starters) ? result.starters : [],
      tags: Array.isArray(result?.tags) ? result.tags : [],
    }
  }

  async function getFollowupCombos(comboId) {
    const [result, characterList] = await Promise.all([
      api.getComboFollowups(comboId),
      api.getCharacters(),
    ])
    return normalizeCombos(Array.isArray(result) ? result : [], characterList)
  }

  function patchCombo(id, patch) {
    const comboId = Number(id)
    combos.value = combos.value.map(combo => (Number(combo.id) === comboId ? { ...combo, ...patch } : combo))
  }

  async function toggleLike(combo) {
    const result = await api.likeCombo(combo.id)
    const patch = { liked: result.liked, likes: result.likes }
    patchCombo(combo.id, patch)
    Object.assign(combo, patch)
    return patch
  }

  async function toggleFavorite(combo) {
    const result = await api.favoriteCombo(combo.id)
    const patch = { favorited: result.favorited, favorites: result.favorites }
    patchCombo(combo.id, patch)
    Object.assign(combo, patch)
    return patch
  }

  return {
    combos,
    findById,
    getUserCombos,
    getUserFavoriteCombos,
    getWorldTourCombos,
    getWorldTourComboFilterOptions,
    getFollowupCombos,
    toggleLike,
    toggleFavorite,
  }
})
