import type { PageParams } from './api'

export interface GameLevel {
  id: string
  code: string
  name: string
  description: string | null
  gradeLevel: number
  knowledgePointId: string
  mode: string
  questionCount: number
  timeLimitSeconds: number
  passRate: number
  minPlayers: number
  maxPlayers: number
  basePoints: number
  baseExperience: number
  previousLevelId: string | null
  sortOrder: number
  status: string
  ruleConfig: string | null
}

export interface GameSessionSummary {
  id: string
  levelId: string
  mode: string
  creatorStudentId: string
  status: string
  memberCount: number
  questionCountSnapshot: number
  timeLimitSecondsSnapshot: number
  passRateSnapshot: number
  teamProgress: number
  startedAt: string | null
  endedAt: string | null
  resultDetail: string | null
}

export interface SessionMember {
  studentId: string
  seatNo: number
  status: string
  practiceTaskId: string | null
  answeredCount: number
  correctCount: number
  score: number
  contributionValue: number
  maxCombo: number
  joinedAt: string
  finishedAt: string | null
}

export interface SessionQuestion {
  id: string
  sequenceNo: number
  questionId: string
  answerRecordId: string | null
  correct: boolean | null
  progressDelta: number | null
  energyDelta: number | null
  answeredAt: string | null
}

export interface SessionDetail {
  session: GameSessionSummary
  members: SessionMember[]
  myQuestions: SessionQuestion[]
}

export interface PlayerProgress {
  studentId: string
  totalPoints: number
  experience: number
  playerLevel: number
  totalSessions: number
  successSessions: number
  bestCombo: number
}

export interface LevelProgress {
  levelId: string
  status: string
  attemptCount: number
  bestScore: number | null
  bestDurationSeconds: number | null
  firstUnlockedAt: string | null
  firstPassedAt: string | null
  lastPlayedAt: string | null
}

export interface Reward {
  id: string
  sessionId: string
  rewardType: string
  amount: number
  balanceAfter: number
  reason: string
  createdAt: string
}

export interface LevelQuery extends PageParams {
  gradeLevel?: number
  knowledgePointId?: string
  mode?: string
}

export interface SessionQuery extends PageParams {
  status?: string
}

export interface LevelProgressQuery {
  status?: string
}
