<template>
  <router-link :to="`/combos/${combo.id}`" :class="['bg-surface-container border border-surface-variant p-4 flex flex-col gap-2 hover:border-tertiary-container/50 transition-colors cursor-pointer group', { 'border-tertiary-container shadow-[0_0_15px_rgba(255,64,84,0.16)]': highlight }]">
    <div class="flex justify-between items-start">
      <div>
        <h4 class="font-body-lg text-body-lg text-primary font-bold">{{ combo.route }}</h4>
        <div class="font-label-mono text-label-mono text-outline flex items-center gap-2 mt-1">
          <WorldTourComboRoute
            v-if="isWorldTour"
            :route="combo.route"
            :character-ids="combo.routeCharacterIds"
            :characters="combo.routeCharacters"
            size="sm"
            compact
          />
          <ComboNotation v-else :route="combo.route" size="sm" />
        </div>
        <p v-if="uploaderMeta" class="combo-uploader-meta">
          <span class="material-symbols-outlined">person</span>
          {{ uploaderMeta }}
        </p>
      </div>
      <div class="combo-tag-list">
        <span
          v-for="tag in comboTags"
          :key="tag"
          :class="['font-label-mono text-label-mono px-2 py-0.5 text-xs slanted', comboTagColor(tag)]"
        >{{ comboTagLabel(tag) }}</span>
      </div>
    </div>
    <div class="combo-stat-strip">
      <div class="combo-stat-card damage">
        <strong>{{ combo.damage || 0 }}</strong>
        <span>DMG</span>
      </div>
      <div class="combo-stat-card drive">
        <strong>{{ combo.driveCost || 0 }}</strong>
        <span>Drive</span>
      </div>
      <div class="combo-stat-card difficulty">
        <strong>{{ combo.difficulty || '-' }}</strong>
        <span>难度</span>
      </div>
      <span v-if="combo.cornerOnly" class="combo-corner-pill">版边</span>
    </div>
    <div class="combo-social-row" @click.stop @keydown.stop>
      <button
        v-if="socialInteractive"
        type="button"
        :class="['combo-social-chip like', { active: combo.liked }]"
        :disabled="isLikeBusy"
        @click.prevent.stop="emit('like', combo)"
      >
        <span class="material-symbols-outlined">thumb_up</span>
        <strong>{{ combo.likes || 0 }}</strong>
      </button>
      <span v-else class="combo-social-chip like">
        <span class="material-symbols-outlined">thumb_up</span>
        <strong>{{ combo.likes || 0 }}</strong>
      </span>
      <button
        v-if="socialInteractive"
        type="button"
        :class="['combo-social-chip favorite', { active: combo.favorited }]"
        :disabled="isFavoriteBusy"
        @click.prevent.stop="emit('favorite', combo)"
      >
        <span class="material-symbols-outlined">{{ combo.favorited ? 'bookmark' : 'bookmark_add' }}</span>
        <strong>{{ combo.favorites || 0 }}</strong>
      </button>
      <span v-else class="combo-social-chip favorite">
        <span class="material-symbols-outlined">bookmark</span>
        <strong>{{ combo.favorites || 0 }}</strong>
      </span>
    </div>
    <div class="text-tertiary-container font-label-mono text-label-mono text-xs mt-2 opacity-0 group-hover:opacity-100 transition-opacity">查看详情 &gt;</div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { comboTagColor, comboTagLabel, normalizeComboControlType, normalizeComboTags } from '@/js/utils/helpers'
import ComboNotation from '@/components/common/ComboNotation.vue'
import WorldTourComboRoute from '@/components/common/WorldTourComboRoute.vue'

const props = defineProps({
  combo: Object,
  highlight: Boolean,
  socialInteractive: Boolean,
  socialBusy: { type: String, default: '' },
})
const emit = defineEmits(['like', 'favorite'])
const comboTags = computed(() => normalizeComboTags(props.combo, props.combo?.type))
const isWorldTour = computed(() => normalizeComboControlType(props.combo?.controlType) === 'world-tour')
const formattedUploadedAt = computed(() => formatUploadTime(props.combo?.createdAt))
const uploaderName = computed(() => props.combo?.authorName || props.combo?.author || '')
const isLikeBusy = computed(() => props.socialBusy === `like:${props.combo?.id}`)
const isFavoriteBusy = computed(() => props.socialBusy === `favorite:${props.combo?.id}`)
const uploaderMeta = computed(() => {
  const parts = []
  if (uploaderName.value) parts.push(`由 ${uploaderName.value} 上传`)
  if (formattedUploadedAt.value) parts.push(formattedUploadedAt.value)
  return parts.join(' · ')
})

function formatUploadTime(value) {
  if (!value) return ''
  const rawValue = String(value)
  if (/^\d{4}-\d{2}-\d{2}$/.test(rawValue)) return rawValue
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return rawValue
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}
</script>

<style scoped>
.combo-uploader-meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 8px 0 0;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.combo-uploader-meta .material-symbols-outlined {
  flex: 0 0 auto;
  font-size: 13px;
}

.combo-tag-list {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  max-width: 190px;
}

.combo-stat-strip {
  display: flex;
  align-items: stretch;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
  font-family: 'JetBrains Mono';
}

.combo-stat-card {
  min-width: 112px;
  min-height: 56px;
  display: inline-grid;
  grid-template-columns: auto auto;
  align-items: end;
  justify-content: center;
  gap: 7px;
  padding: 8px 12px;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.03);
}

.combo-stat-card strong {
  font-size: 25px;
  font-weight: 900;
  line-height: 0.95;
  letter-spacing: 0;
}

.combo-stat-card span {
  padding-bottom: 2px;
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
  text-transform: uppercase;
  color: var(--ink-muted);
}

.combo-stat-card.damage {
  border-color: rgba(255, 64, 84, 0.54);
  background: linear-gradient(180deg, rgba(255, 64, 84, 0.16), rgba(12, 9, 10, 0.94));
}

.combo-stat-card.damage strong {
  color: var(--accent-red-hot);
  text-shadow: 0 0 16px rgba(255, 64, 84, 0.24);
}

.combo-stat-card.drive {
  border-color: rgba(49, 213, 230, 0.42);
  background: linear-gradient(180deg, rgba(49, 213, 230, 0.11), rgba(12, 9, 10, 0.94));
}

.combo-stat-card.drive strong {
  color: var(--accent-cyan-soft);
  text-shadow: 0 0 16px rgba(49, 213, 230, 0.2);
}

.combo-stat-card.difficulty {
  border-color: rgba(255, 193, 93, 0.4);
  background: linear-gradient(180deg, rgba(255, 193, 93, 0.1), rgba(12, 9, 10, 0.94));
}

.combo-stat-card.difficulty strong {
  color: var(--ink-text);
  font-size: 21px;
  text-shadow: 0 0 16px rgba(219, 229, 244, 0.1);
}

.combo-corner-pill {
  min-height: 34px;
  align-self: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 11px;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 800;
}

.combo-corner-pill {
  color: #ffe4ad;
  border-color: rgba(255, 193, 93, 0.42);
  background: rgba(255, 193, 93, 0.11);
}

.combo-social-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-family: 'JetBrains Mono';
}

.combo-social-chip {
  min-width: 76px;
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 10px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 900;
  transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
}

button.combo-social-chip:hover:not(:disabled),
button.combo-social-chip:focus-visible {
  color: var(--ink-text);
  border-color: var(--accent-red);
  background: var(--ink-surface-high);
}

.combo-social-chip.active.like,
.combo-social-chip.like:hover:not(:disabled) {
  color: var(--accent-cyan-soft);
  border-color: rgba(91, 214, 199, 0.58);
  background: rgba(91, 214, 199, 0.1);
}

.combo-social-chip.active.favorite,
.combo-social-chip.favorite:hover:not(:disabled) {
  color: var(--accent-amber);
  border-color: rgba(255, 209, 102, 0.58);
  background: rgba(255, 209, 102, 0.1);
}

.combo-social-chip:disabled {
  cursor: wait;
  opacity: 0.68;
}

.combo-social-chip .material-symbols-outlined {
  font-size: 17px;
}

.combo-social-chip strong {
  line-height: 1;
}

@media (max-width: 640px) {
  a {
    padding: 12px;
  }

  a > div:first-child {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .combo-tag-list {
    justify-content: flex-start;
    max-width: none;
  }

  h4 {
    font-size: 15px;
    line-height: 1.35;
    overflow-wrap: anywhere;
  }

  .font-label-mono.flex {
    flex-wrap: wrap;
    gap: 5px;
    font-size: 11px;
  }

  .combo-uploader-meta {
    font-size: 10px;
  }

  .combo-stat-strip {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .combo-stat-card {
    min-width: 0;
  }

  .combo-corner-pill {
    width: 100%;
  }

  .combo-social-chip {
    flex: 1 1 0;
    min-width: 0;
  }

  .combo-social-row + div,
  a > div:last-child {
    opacity: 1;
  }
}
</style>
