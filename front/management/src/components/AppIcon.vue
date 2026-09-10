<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 20 })
const paths: Record<string, string> = {
  home: 'M3 10 12 3l9 7v11h-6v-7H9v7H3Z',
  user: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2',
  book: 'M12 6v15M12 6C9 4 6 3 3 4v15c3-1 6 0 9 2 3-2 6-3 9-2V4c-3-1-6 0-9 2Z',
  users:
    'M15 7a3 3 0 1 1-6 0 3 3 0 0 1 6 0ZM6 21v-3a6 6 0 0 1 12 0v3M19 4a3 3 0 0 1 0 6M21 14a4 4 0 0 1 2 4v2M5 4a3 3 0 0 0 0 6M3 14a4 4 0 0 0-2 4v2',
  edit: 'm14 5 5 5M3 21l5-1L21 7l-5-5L3 15v6ZM13 21h8',
  check: 'm7 12 3 3 7-7M21 12a9 9 0 1 1-5-8',
  chart: 'M4 3v18h17M8 16v-5m5 5V7m5 9v-4',
  target: 'M21 12a9 9 0 1 1-9-9M17 12a5 5 0 1 1-5-5M12 12l9-9m-5 0h5v5',
  camera: 'M3 7h4l2-3h6l2 3h4v14H3ZM16 13a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  shield: 'm12 3 9 3v6c0 5-5 8-9 10-4-2-9-5-9-10V6l9-3Zm-4 9 3 3 5-6',
  lock: 'M6 10V7a6 6 0 0 1 12 0v3M4 10h16v12H4Zm8 5v3',
  grid: 'M3 3h7v7H3Zm11 0h7v7h-7ZM3 14h7v7H3Zm11 0h7v7h-7Z',
  document: 'M5 2h9l5 5v15H5Zm9 0v6h5M8 12h8m-8 4h8',
  flag: 'M5 22V3m0 0c5-4 9 4 15 0v11c-6 4-10-4-15 0',
  arrow: 'M4 12h16m-6-6 6 6-6 6',
  chevron: 'm9 5 7 7-7 7',
  menu: 'M4 6h16M4 12h16M4 18h16',
  close: 'm6 6 12 12M6 18 18 6',
  logout: 'M9 3H3v18h6m5-15 6 6-6 6m-7-6h13',
}
const shape = computed(() => paths[props.name] || paths.document)
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.65"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    class="app-icon"
  >
    <path :d="shape" />
  </svg>
</template>

<style scoped lang="scss">
.app-icon {
  display: inline-block;
  flex: none;
  vertical-align: middle;
}
</style>
