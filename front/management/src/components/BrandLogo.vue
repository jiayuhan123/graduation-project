<script setup lang="ts">
withDefaults(defineProps<{ compact?: boolean }>(), { compact: false })
</script>

<template>
  <div class="brand" :class="{ 'brand--compact': compact }" aria-label="数学成长空间">
    <span class="brand__mark" aria-hidden="true">
      <span class="brand__symbol">π</span>
    </span>
    <span v-if="!compact" class="brand__copy">
      <strong>数学成长空间</strong>
      <small>Math Growth Space</small>
    </span>
  </div>
</template>

<style scoped lang="scss">
.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand__mark {
  position: relative;
  display: grid;
  width: 40px;
  height: 40px;
  flex: none;
  place-items: center;
  background: var(--app-primary);
  border-radius: 10px;
  box-shadow: none;
  color: #fff;
}

.brand__symbol {
  font-family: Georgia, serif;
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}

.brand__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.brand__copy strong {
  color: #252b30;
  font-size: 16px;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.brand__copy small {
  margin-top: 2px;
  color: #a0a6ad;
  font-size: 10px;
  letter-spacing: 0.08em;
}

.brand--compact .brand__mark {
  width: 40px;
  height: 40px;
  border-radius: 13px;
}

.brand--compact .brand__symbol {
  font-size: 25px;
}
</style>
