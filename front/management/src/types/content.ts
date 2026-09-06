import type { PageParams } from '@/types/api'

export type EnabledStatus = 0 | 1
export type QuestionMode = 'FIXED' | 'TEMPLATE'
export type QuestionStatus = 'DRAFT' | 'ENABLED' | 'DISABLED'
export type QuestionType = 'ARITHMETIC' | 'FILL_BLANK' | 'VERTICAL' | 'CHOICE' | 'JUDGEMENT'
export type ErrorCategory =
  'CALCULATION' | 'PLACE_VALUE' | 'CARRY' | 'BORROW' | 'FORMAT' | 'CONCEPT' | 'OTHER'

export interface ContentModule {
  id: string
  code: string
  name: string
  gradeLevel: number
  semester: number
  learningTheme: string
  publisher: string | null
  editionDescription: string | null
  description: string | null
  sortOrder: number
  status: EnabledStatus
  createdBy?: string
  createdAt?: string
  updatedAt?: string
  version?: number
}

export interface KnowledgePoint {
  id: string
  moduleId: string
  parentId: string | null
  code: string
  name: string
  description: string | null
  difficultyLevel: number
  sortOrder: number
  status: EnabledStatus
  createdBy?: string
  createdAt?: string
  updatedAt?: string
  version?: number
}

export interface QuestionSummary {
  id: string
  code: string
  questionMode: QuestionMode
  questionType: QuestionType
  stem: string
  difficulty: number
  estimatedSeconds: number | null
  status: QuestionStatus
  createdBy: string
  reviewedBy: string | null
  reviewedAt: string | null
  updatedAt: string
  version: number
}

export interface QuestionKnowledgePointBinding {
  knowledgePointId: string
  knowledgePointCode: string | null
  knowledgePointName: string | null
  primary: boolean
  weight: number
}

export interface QuestionDetail extends QuestionSummary {
  options: string | null
  standardAnswer: string
  analysis: string | null
  generationRule: string | null
  createdAt: string
  knowledgePoints: QuestionKnowledgePointBinding[]
}

export interface ErrorType {
  id: string
  code: string
  name: string
  category: ErrorCategory
  applicableQuestionType: QuestionType | null
  description: string | null
  explanationTemplate: string | null
  remediationHint: string | null
  status: EnabledStatus
  createdAt: string
  updatedAt: string
  version: number
}

export interface ContentModuleQuery extends PageParams {
  keyword?: string
  gradeLevel?: number
  semester?: number
  status?: EnabledStatus
}

export interface KnowledgePointQuery {
  moduleId?: string
  keyword?: string
  status?: EnabledStatus
}

export interface QuestionQuery extends PageParams {
  keyword?: string
  knowledgePointId?: string
  questionMode?: QuestionMode
  questionType?: QuestionType
  difficulty?: number
  status?: QuestionStatus
}

export interface ErrorTypeQuery {
  keyword?: string
  category?: ErrorCategory
  applicableQuestionType?: QuestionType
  status?: EnabledStatus
}

export interface ContentModuleInput {
  code?: string
  name: string
  gradeLevel: number
  semester: number
  learningTheme: string
  publisher?: string
  editionDescription?: string
  description?: string
  sortOrder: number
  status?: EnabledStatus
  version?: number
}

export interface KnowledgePointInput {
  moduleId?: string
  parentId?: string
  code?: string
  name: string
  description?: string
  difficultyLevel: number
  sortOrder: number
  status?: EnabledStatus
  version?: number
}

export interface QuestionBindingInput {
  knowledgePointId: string
  primary: boolean
  weight: number
}

export interface QuestionInput {
  code?: string
  questionMode: QuestionMode
  questionType: QuestionType
  stem: string
  options?: string
  standardAnswer: string
  analysis?: string
  generationRule?: string
  difficulty: number
  estimatedSeconds?: number
  knowledgePoints: QuestionBindingInput[]
  version?: number
}

export interface ErrorTypeInput {
  code?: string
  name: string
  category: ErrorCategory
  applicableQuestionType?: QuestionType
  description?: string
  explanationTemplate?: string
  remediationHint?: string
  status?: EnabledStatus
  version?: number
}
