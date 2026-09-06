<script setup lang="ts">
import { Card as TCard, Empty as TEmpty } from 'tdesign-vue-next'

defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <t-card :title="title" :bordered="true">
    <t-empty :description="description" />
  </t-card>
</template>
