<template>
  <router-link :to="`/characters/${character.id}`" :class="['select-card', `tone-${tone}`]" :aria-label="`查看 ${character.name} 角色详情`">
    <div class="select-portrait" :style="{ backgroundImage: `url(${character.image})` }"></div>
  </router-link>
</template>

<script setup>
defineProps({
  character: Object,
  tone: { type: String, default: 'red' },
})
</script>

<style scoped>
.select-card {
  --select-glow: rgba(187, 105, 255, 0.88);
  --select-glow-soft: rgba(170, 76, 255, 0.5);
  --select-glow-deep: rgba(78, 25, 138, 0.68);
  position: relative;
  display: block;
  width: 104px;
  aspect-ratio: 1 / 1;
  color: var(--ink-text);
  outline: none;
  filter: drop-shadow(0 0 0 rgba(0, 0, 0, 0));
  transition: transform 0.16s ease, filter 0.16s ease;
}

.select-card::before {
  content: "";
  position: absolute;
  inset: -1px;
  z-index: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.36), rgba(72, 72, 82, 0.28));
  clip-path: polygon(0 18%, 15% 0, 100% 0, 100% 78%, 82% 100%, 0 100%);
  opacity: 0.68;
  transition: background 0.16s ease, opacity 0.16s ease, filter 0.16s ease;
}

.select-card:hover,
.select-card:focus-visible {
  z-index: 2;
  transform: translateY(-4px) scale(1.06);
  filter: drop-shadow(0 0 12px var(--select-glow-soft));
}

.select-card:hover::before,
.select-card:focus-visible::before {
  background: linear-gradient(135deg, var(--select-glow), var(--select-glow-deep));
  opacity: 1;
  filter: drop-shadow(0 0 10px var(--select-glow));
}

.select-portrait {
  position: absolute;
  inset: 2px;
  z-index: 1;
  overflow: hidden;
  background-color: #120c1c;
  background-position: center top;
  background-size: cover;
  border: 0;
  box-shadow: inset 0 0 0 0 transparent;
  clip-path: polygon(0 18%, 15% 0, 100% 0, 100% 78%, 82% 100%, 0 100%);
  filter: grayscale(1) saturate(0.18) contrast(1.18) brightness(0.72);
  transition: box-shadow 0.16s ease, filter 0.18s ease, transform 0.18s ease;
}

.select-portrait::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 28%, rgba(0, 0, 0, 0.4)),
    radial-gradient(circle at 50% 18%, var(--select-glow-soft), transparent 44%);
  mix-blend-mode: screen;
  opacity: 0.38;
  transition: opacity 0.16s ease;
}

.select-card:hover .select-portrait,
.select-card:focus-visible .select-portrait {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--select-glow) 58%, transparent);
  filter: grayscale(0) saturate(1.08) contrast(1.06) brightness(1);
  transform: scale(1.015);
}

.select-card:hover .select-portrait::after,
.select-card:focus-visible .select-portrait::after {
  opacity: 0.78;
}

@media (max-width: 640px) {
  .select-card {
    width: 78px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .select-card,
  .select-card::before,
  .select-portrait,
  .select-portrait::after {
    transition: none !important;
  }

  .select-card:hover,
  .select-card:focus-visible {
    transform: none;
  }
}
</style>
