import { appConfig } from '@/config/app'
import { DEFAULT_TOKEN_NAME, STORAGE_KEYS } from '@/constants/storage'
import type {
  AgentAssignmentCreated,
  AgentEventName,
  AgentFeedbackInput,
  AgentKnowledgeDocument,
  AgentKnowledgeInput,
  AgentQualitySummary,
  AgentStreamEvent,
} from '@/types/ai'
import { ApiError, type ApiResult } from '@/types/api'
import { request } from '@/utils/request'
import { storage } from '@/utils/storage'

interface StreamAgentInput {
  conversationId?: string
  role: 'teacher' | 'admin'
  message: string
}

function parseEvent(block: string): AgentStreamEvent | null {
  let event: AgentEventName = 'delta'
  const data: string[] = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim() as AgentEventName
    if (line.startsWith('data:')) data.push(line.slice(5).trim())
  }
  if (!data.length) return null
  return { event, data: JSON.parse(data.join('\n')) as Record<string, unknown> }
}

async function responseError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as ApiResult
    return new ApiError(body.message || 'Agent 请求失败', {
      code: body.code,
      status: response.status,
      details: body.data,
    })
  } catch {
    return new ApiError(`Agent 请求失败（HTTP ${response.status}）`, { status: response.status })
  }
}

/** 使用 fetch 读取 POST SSE；EventSource 不支持请求体和自定义登录头。 */
export async function streamAgentApi(
  input: StreamAgentInput,
  onEvent: (event: AgentStreamEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const token = storage.getString(STORAGE_KEYS.token)
  const tokenName = storage.getString(STORAGE_KEYS.tokenName) || DEFAULT_TOKEN_NAME
  const response = await fetch(`${appConfig.apiBaseUrl}/api/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { [tokenName]: token } : {}),
    },
    body: JSON.stringify(input),
    signal,
  })
  if (!response.ok) throw await responseError(response)
  if (!response.body) throw new ApiError('浏览器不支持 Agent 流式响应')

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value, { stream: !done }).replaceAll('\r\n', '\n')
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const event = parseEvent(buffer.slice(0, boundary))
      if (event) onEvent(event)
      buffer = buffer.slice(boundary + 2)
      boundary = buffer.indexOf('\n\n')
    }
    if (done) break
  }
  const finalEvent = parseEvent(buffer.trim())
  if (finalEvent) onEvent(finalEvent)
}

export const confirmAgentActionApi = (confirmationToken: string) =>
  request<AgentAssignmentCreated>({
    url: '/api/ai/actions/confirm',
    method: 'POST',
    data: { confirmationToken },
  })

export const submitAgentFeedbackApi = (data: AgentFeedbackInput) =>
  request<void>({ url: '/api/ai/feedback', method: 'POST', data })

export const forgetAgentMemoryApi = (conversationId: string) =>
  request<{ forgotten: boolean; deletedMemories: number }>({
    url: `/api/ai/conversations/${conversationId}/memory`,
    method: 'DELETE',
  })

export const listAgentKnowledgeApi = () =>
  request<AgentKnowledgeDocument[]>({ url: '/api/ai/knowledge/documents' })

export const upsertAgentKnowledgeApi = (data: AgentKnowledgeInput) =>
  request<AgentKnowledgeDocument>({ url: '/api/ai/knowledge/documents', method: 'POST', data })

export const deleteAgentKnowledgeApi = (documentId: string) =>
  request<void>({ url: `/api/ai/knowledge/documents/${documentId}`, method: 'DELETE' })

export const getAgentQualityApi = () => request<AgentQualitySummary>({ url: '/api/ai/quality' })
