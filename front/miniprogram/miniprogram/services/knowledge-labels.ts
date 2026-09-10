import { getKnowledgePointApi } from '../api/content'

/** Resolve labels with bounded concurrency; unavailable content keeps a readable fallback. */
export async function withKnowledgeLabels<T extends { knowledgePointId: string }>(records: T[]) {
  const names = new Map<string, string>()
  const ids = [...new Set(records.map((item) => item.knowledgePointId))]
  for (let offset = 0; offset < ids.length; offset += 6) {
    await Promise.all(
      ids.slice(offset, offset + 6).map(async (id) => {
        try {
          names.set(id, (await getKnowledgePointApi(id)).name)
        } catch {
          /* Records remain available. */
        }
      }),
    )
  }
  return records.map((item) => ({
    ...item,
    knowledgePointName: names.get(item.knowledgePointId) || '知识点暂不可用',
  }))
}
