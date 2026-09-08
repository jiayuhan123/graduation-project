import type { PageParams } from './api'

export interface PkRuleInput {
  gradeLevel: number
  knowledgePointId: string
  difficulty: number
  questionCount?: number
  timeLimitSeconds?: number
}

export interface GameLevel {
  id: string
  code: string
  name: string
  description: string | null
  gradeLevel: number
  knowledgePointId: string
  difficulty: number
  questionSource: string
  mode: string
  questionCount: number
  timeLimitSeconds: number
  passRate: number | null
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
  levelId: string | null
  mode: 'LEVEL' | 'PK'
  creatorStudentId: string
  matchMode: 'ROOM' | 'RANDOM' | null
  inviteCode: string | null
  status: 'WAITING' | 'READY' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
  memberCount: number
  gradeLevelSnapshot: number
  knowledgePointIdSnapshot: string
  difficultySnapshot: number
  questionCountSnapshot: number
  timeLimitSecondsSnapshot: number
  passRateSnapshot: number | null
  teamProgress: number
  winnerStudentId: string | null
  resultType: string | null
  startedAt: string | null
  endedAt: string | null
  expiresAt: string | null
  settledAt: string | null
  resultDetail: string | null
}

export interface SessionMember {
  studentId: string
  seatNo: number
  memberRole: 'CREATOR' | 'OPPONENT'
  status: string
  result: 'PENDING' | 'WIN' | 'LOSE' | 'DRAW' | 'CANCELLED' | null
  practiceTaskId: string | null
  answeredCount: number
  correctCount: number
  score: number
  contributionValue: number
  currentCombo: number
  maxCombo: number
  totalResponseMs: number
  ratingBefore: number | null
  ratingAfter: number | null
  pointsReward: number
  experienceReward: number
  joinedAt: string
  lastAnsweredAt: string | null
  finishedAt: string | null
}

export interface SessionQuestion {
  id: string
  sequenceNo: number
  questionId: string
  questionType: string
  stem: string
  options: string | null
  gradingStatus: string
  studentAnswer: string | null
  answerRecordId: string | null
  correct: boolean | null
  score: number | null
  responseMs: number | null
  comboAfter: number | null
  progressDelta: number | null
  energyDelta: number | null
  answeredAt: string | null
}

export interface SessionDetail {
  session: GameSessionSummary
  members: SessionMember[]
  myQuestions: SessionQuestion[]
}

export interface MatchStatus {
  ticketId: string | null
  status: 'WAITING' | 'MATCHED' | 'CANCELLED' | 'EXPIRED'
  expiresAt: string | null
  sessionId: string | null
  session: SessionDetail | null
}

export interface GameAnswerFeedback {
  sessionId: string
  sessionQuestionId: string
  accepted: boolean
  correct: boolean | null
  score: number
  combo: number
  progress: number
  answeredCount: number
  questionCount: number
  nextSessionQuestionId: string | null
  sessionStatus: string
  resultType: string | null
  winnerStudentId: string | null
  idempotentReplay: boolean
}

export interface PlayerProgress {
  studentId: string
  totalPoints: number
  experience: number
  playerLevel: number
  totalSessions: number
  successSessions: number
  levelPassedCount: number
  pkSessions: number
  pkWins: number
  pkLosses: number
  pkDraws: number
  pkRating: number
  currentWinStreak: number
  bestWinStreak: number
  bestCombo: number
  lastPlayedAt: string | null
}

export interface LevelProgress {
  levelId: string
  status: string
  attemptCount: number
  bestScore: number | null
  bestCorrectCount: number | null
  bestStarCount: number | null
  bestDurationSeconds: number | null
  lastSessionId: string | null
  firstUnlockedAt: string | null
  firstPassedAt: string | null
  lastPlayedAt: string | null
}

export interface Reward {
  id: string
  sessionId: string
  businessType: string
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
