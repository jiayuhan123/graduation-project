import type { PageParams } from './api'

export interface ContentModule {
  id: string
  code: string
  name: string
  gradeLevel: number
  semester: number
  learningTheme: string | null
  publisher: string | null
  editionDescription: string | null
  description: string | null
  sortOrder: number
  status: number
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
  status: number
}

export interface ContentModuleQuery extends PageParams {
  gradeLevel?: number
  semester?: number
}

export interface KnowledgePointQuery {
  moduleId: string
  parentId?: string
}
