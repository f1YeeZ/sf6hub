<template>
  <div :class="['world-tour-route', { compact }]">
    <template v-for="(move, index) in moves" :key="`${index}-${move}`">
      <div class="world-tour-step">
        <span class="world-tour-character">
          <img v-if="characterAt(index)?.avatar || characterAt(index)?.image" :src="characterAt(index).avatar || characterAt(index).image" alt="" />
          <span v-else class="material-symbols-outlined" aria-hidden="true">person</span>
          <strong>{{ characterAt(index)?.name || '未选择角色' }}</strong>
        </span>
        <ComboNotation :route="move" :size="size" />
      </div>
      <span v-if="index < moves.length - 1" class="world-tour-separator" aria-hidden="true">&gt;</span>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ComboNotation from '@/components/common/ComboNotation.vue'
import { normalizeRouteCharacterIds } from '@/js/utils/helpers'

const props = defineProps({
  route: { type: String, default: '' },
  characterIds: { type: Array, default: () => [] },
  characters: { type: Array, default: () => [] },
  size: { type: String, default: 'sm' },
  compact: Boolean,
})

const moves = computed(() => String(props.route || '')
  .split(/\s*>\s*/)
  .map(move => move.trim())
  .filter(Boolean))
const normalizedCharacterIds = computed(() => normalizeRouteCharacterIds(props.characterIds))
const characterById = computed(() => new Map(props.characters.map(character => [Number(character.id), character])))

function characterAt(index) {
  return characterById.value.get(normalizedCharacterIds.value[index])
}
</script>

<style scoped>
.world-tour-route {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.world-tour-step {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
}

.world-tour-character {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 3px 8px 3px 4px;
  color: var(--accent-cyan-soft);
  background: rgba(49, 213, 230, 0.08);
  border: 1px solid rgba(49, 213, 230, 0.34);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 11px;
}

.world-tour-character img {
  width: 24px;
  height: 24px;
  object-fit: cover;
  border-radius: 3px;
}

.world-tour-character .material-symbols-outlined {
  font-size: 17px;
}

.world-tour-character strong {
  white-space: nowrap;
}

.world-tour-separator {
  color: var(--accent-red-hot);
  font-family: 'JetBrains Mono';
  font-size: 17px;
  font-weight: 900;
}

.compact {
  gap: 7px;
}

.compact .world-tour-step {
  gap: 6px;
}

.compact .world-tour-character {
  min-height: 26px;
  padding: 2px 6px 2px 3px;
  font-size: 10px;
}

.compact .world-tour-character img {
  width: 20px;
  height: 20px;
}
</style>
