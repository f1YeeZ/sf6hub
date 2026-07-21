<template>
  <div :class="['combo-notation', `combo-notation-${size}`, `combo-notation-${layout}`]" :aria-label="route">
    <template v-for="(move, moveIndex) in parsedMoves" :key="`${move.raw}-${moveIndex}`">
      <img
        v-if="moveIndex > 0"
        :class="['combo-arrow', { 'combo-arrow-vertical': layout === 'vertical' }]"
        src="/street-fighter-6/controller/arrow_3.png"
        alt="下一招"
      />
      <span :class="['combo-move', { 'combo-move-sequence': move.parts.some(part => part.type === 'arrow') }]">
        <template v-for="(part, partIndex) in move.parts" :key="`${moveIndex}-${partIndex}-${part.key || part.value}`">
          <img
            v-if="part.type === 'arrow'"
            class="combo-inline-arrow"
            src="/street-fighter-6/controller/arrow_3.png"
            alt="TC 下一段"
          />
          <template v-else-if="part.type === 'icons'">
            <template v-for="(image, imageIndex) in part.images" :key="`${part.key}-${image}`">
              <span
                v-if="part.highlightedImages?.includes(imageIndex)"
                :class="['combo-icon', 'combo-icon-highlighted', { 'combo-icon-wide': part.wide }]"
                :style="{ '--combo-icon-mask': `url(${image})` }"
                role="img"
                :aria-label="part.label"
                :title="part.label"
              />
              <img
                v-else
                :class="['combo-icon', { 'combo-icon-wide': part.wide }]"
                :src="image"
                :alt="part.label"
                :title="part.label"
                loading="lazy"
              />
            </template>
          </template>
          <span v-else :class="['combo-text-token', { accent: part.accent, perfect: part.tone === 'perfect', spin: part.tone === 'spin' }]">{{ part.value }}</span>
        </template>
      </span>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { parseComboNotation } from '@/js/utils/comboNotation'

const props = defineProps({
  route: { type: String, required: true },
  size: { type: String, default: 'md' },
  layout: { type: String, default: 'inline' },
})

const parsedMoves = computed(() => parseComboNotation(props.route))
</script>

<style scoped>
.combo-notation {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.combo-move {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  min-height: var(--notation-move-height);
  padding: var(--notation-move-padding);
  border: 1px solid #4a3a60;
  background: #111318;
}

.combo-move-sequence {
  flex-wrap: nowrap;
}

.combo-icon {
  display: block;
  width: var(--notation-icon-size);
  height: var(--notation-icon-size);
  object-fit: contain;
}

.combo-icon-wide {
  width: calc(var(--notation-icon-size) * 1.7);
}

.combo-icon-highlighted {
  background: #ffd02a;
  mask: var(--combo-icon-mask) center / contain no-repeat;
  -webkit-mask: var(--combo-icon-mask) center / contain no-repeat;
}

.combo-text-token {
  display: inline-flex;
  align-items: center;
  min-height: var(--notation-icon-size);
  padding: 0 var(--notation-text-padding);
  color: #e2e2e8;
  font-family: 'JetBrains Mono';
  font-size: var(--notation-text-size);
  font-weight: 700;
  line-height: 1;
}

.combo-text-token.accent {
  color: #f0e0ff;
  background: rgba(128, 64, 192, 0.3);
}

.combo-text-token.perfect {
  color: #1a1200;
  background: #f1b63a;
}

.combo-text-token.spin {
  box-sizing: border-box;
  flex: 0 0 calc(var(--notation-icon-size) * 1.5);
  justify-content: center;
  width: calc(var(--notation-icon-size) * 1.5);
  height: calc(var(--notation-icon-size) * 1.5);
  min-height: calc(var(--notation-icon-size) * 1.5);
  padding: 0;
  color: #fff2f2;
  background: rgba(218, 45, 45, 0.72);
  border: 1px solid rgba(255, 190, 190, 0.46);
  border-radius: 999px;
  box-shadow: 0 0 10px rgba(218, 45, 45, 0.28);
  font-size: calc(var(--notation-icon-size) * 0.34);
  line-height: 1;
  white-space: nowrap;
}

.combo-arrow {
  display: block;
  width: var(--notation-arrow-size);
  height: var(--notation-arrow-size);
  object-fit: contain;
}

.combo-inline-arrow {
  display: block;
  flex: 0 0 var(--notation-arrow-size);
  width: var(--notation-arrow-size);
  height: var(--notation-arrow-size);
  object-fit: contain;
}

.combo-arrow-vertical {
  margin-left: 14px;
  transform: rotate(90deg);
}

.combo-notation-vertical {
  width: 100%;
  flex-direction: column;
  align-items: flex-start;
  flex-wrap: nowrap;
  gap: 12px;
}

.combo-notation-vertical .combo-move {
  width: fit-content;
  max-width: 100%;
}

.combo-notation-sm {
  --notation-icon-size: 22px;
  --notation-arrow-size: 15px;
  --notation-text-size: 11px;
  --notation-text-padding: 5px;
  --notation-move-height: 30px;
  --notation-move-padding: 4px 6px;
  gap: 5px;
}

.combo-notation-md {
  --notation-icon-size: 28px;
  --notation-arrow-size: 18px;
  --notation-text-size: 12px;
  --notation-text-padding: 6px;
  --notation-move-height: 38px;
  --notation-move-padding: 5px 8px;
}

.combo-notation-lg {
  --notation-icon-size: 38px;
  --notation-arrow-size: 24px;
  --notation-text-size: 15px;
  --notation-text-padding: 8px;
  --notation-move-height: 52px;
  --notation-move-padding: 7px 10px;
}

@media (max-width: 640px) {
  .combo-notation-lg {
    --notation-icon-size: 28px;
    --notation-arrow-size: 18px;
    --notation-text-size: 12px;
    --notation-text-padding: 6px;
    --notation-move-height: 38px;
    --notation-move-padding: 5px 8px;
  }

  .combo-notation-vertical {
    gap: 8px;
  }
}
</style>
