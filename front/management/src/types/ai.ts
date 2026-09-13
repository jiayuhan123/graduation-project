export type AgentEventName =
  'connected' | 'metadata' | 'tool_start' | 'tool_result' | 'delta' | 'done' | 'error'

export interface AgentStreamEvent {
  event: AgentEventName
  data: Record<string, unknown>
}

export interface AgentActionDraft {
  draftId: string
  actionType: 'CREATE_ASSIGNMENT'
  title: string
  summary: string
  expiresAt: string
  confirmationToken: string
}

export interface AgentAssignmentCreated {
  assignmentId: string
  classId: string
  title: string
  status: string
  version: number
}

export interface AgentFeedbackInput {
  runId: string
  rating: -1 | 1
  comment?: string
}

export type AgentKnowledgeSourceType = 'TEXT' | 'MARKDOWN' | 'POLICY' | 'LESSON' | 'TEXTBOOK'

export interface AgentKnowledgeDocument {
  id: string
  title: string
  sourceUri: string
  sourceType: AgentKnowledgeSourceType
  checksum: string
  chunkCount: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface AgentKnowledgeInput {
  title: string
  sourceUri: string
  sourceType: AgentKnowledgeSourceType
  content: string
  metadata: Record<string, unknown>
}

export interface AgentQualitySummary {
  windowDays: number
  runCount: number
  successRate: number
  averageDurationMs: number
  averageToolCalls: number
  averageRagHits: number
  feedbackCount: number
  positiveRate: number
}
