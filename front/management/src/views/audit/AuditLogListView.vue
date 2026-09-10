<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Button as TButton, Loading as TLoading, Tag as TTag } from 'tdesign-vue-next'
import { pageAdminAuditLogsApi } from '@/api/operations'
import PageHeader from '@/components/PageHeader.vue'
import type { AuditLogItem } from '@/types/operations'

const loading = ref(true)
const records = ref<AuditLogItem[]>([])
const total = ref(0)
const pages = ref(0)
const query = reactive({
  page: 1,
  size: 15,
  operatorId: '',
  source: '',
  action: '',
  targetType: '',
  targetId: '',
  occurredFrom: '',
  occurredTo: '',
})

function asOffset(value: string): string | undefined {
  return value || undefined
}

function formatTime(value: string): string {
  return new Date(value).toLocaleString()
}

async function load() {
  loading.value = true
  try {
    const result = await pageAdminAuditLogsApi({
      page: query.page,
      size: query.size,
      operatorId: query.operatorId || undefined,
      source: query.source.trim() || undefined,
      action: query.action.trim() || undefined,
      targetType: query.targetType.trim() || undefined,
      targetId: query.targetId.trim() || undefined,
      occurredFrom: asOffset(query.occurredFrom),
      occurredTo: asOffset(query.occurredTo),
    })
    records.value = result.records
    total.value = result.total
    pages.value = result.pages
    query.page = result.page
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  void load()
}

function reset() {
  Object.assign(query, {
    page: 1,
    operatorId: '',
    source: '',
    action: '',
    targetType: '',
    targetId: '',
    occurredFrom: '',
    occurredTo: '',
  })
  void load()
}

function changePage(delta: number) {
  const next = query.page + delta
  if (next < 1 || next > pages.value) return
  query.page = next
  void load()
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      eyebrow="平台管理"
      title="关键数据审计"
      :description="`跨服务查询用户操作和关键数据变更，目前共 ${total} 条记录。`"
    />
    <section class="surface-card audit-card">
      <div class="filters">
        <input v-model="query.operatorId" inputmode="numeric" placeholder="操作人 ID" />
        <input v-model="query.source" placeholder="来源服务，如 learning" />
        <input v-model="query.action" placeholder="操作动作" />
        <input v-model="query.targetType" placeholder="对象类型" />
        <input v-model="query.targetId" placeholder="对象 ID" />
        <input v-model="query.occurredFrom" type="datetime-local" title="开始时间" />
        <input v-model="query.occurredTo" type="datetime-local" title="结束时间" />
        <div class="filter-actions">
          <t-button variant="outline" @click="reset">重置</t-button>
          <t-button @click="search">查询</t-button>
        </div>
      </div>

      <div v-if="loading" class="loading"><t-loading text="正在查询审计记录" /></div>
      <div v-else-if="records.length" class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>操作人</th>
              <th>来源</th>
              <th>动作</th>
              <th>对象</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in records" :key="item.id">
              <td>{{ formatTime(item.occurredAt) }}</td>
              <td>#{{ item.operatorId }}</td>
              <td>
                <t-tag variant="light">{{ item.source }}</t-tag>
              </td>
              <td>
                <strong>{{ item.action }}</strong>
              </td>
              <td>
                {{ item.targetType }}<small>#{{ item.targetId }}</small>
              </td>
              <td class="detail-cell" :title="item.detail || ''">{{ item.detail || '--' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty">没有找到符合条件的审计记录。</p>
      <footer class="pagination">
        <span>第 {{ query.page }} / {{ Math.max(1, pages) }} 页</span>
        <div>
          <t-button
            size="small"
            variant="outline"
            :disabled="query.page <= 1"
            @click="changePage(-1)"
            >上一页</t-button
          ><t-button
            size="small"
            variant="outline"
            :disabled="query.page >= pages"
            @click="changePage(1)"
            >下一页</t-button
          >
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped lang="scss">
.audit-card {
  padding: 22px;
}

.filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.filter-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.filters input {
  min-width: 0;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid var(--app-border-color);
  border-radius: 9px;
  color: inherit;
  font: inherit;
}

.loading,
.empty {
  display: grid;
  min-height: 300px;
  color: var(--app-text-muted);
  place-items: center;
}

.table-scroll {
  margin-top: 20px;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 10px;
  border-bottom: 1px solid var(--app-border-color);
  text-align: left;
  white-space: nowrap;
}

th {
  color: var(--app-text-muted);
  font-size: 11px;
}

td small {
  display: block;
  color: var(--app-text-muted);
}

.detail-cell {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pagination {
  display: flex;
  margin-top: 18px;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text-muted);
}

.pagination div {
  display: flex;
  gap: 7px;
}

@media (width <= 1250px) {
  .filters {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (width <= 680px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
