<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  description?: string
}>()
</script>

<template>
  <header class="page-header">
    <div>
      <p v-if="eyebrow" class="page-header__eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="description" class="page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.page-header__eyebrow {
  margin-bottom: 7px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  color: var(--app-text-strong);
  font-size: clamp(26px, 3vw, 36px);
  font-weight: 800;
  letter-spacing: -0.03em;
}

.page-header__description {
  max-width: 660px;
  margin-top: 8px;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.page-header__actions {
  flex: none;
}

@media (width <= 640px) {
  .page-header {
    margin-bottom: 18px;
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
