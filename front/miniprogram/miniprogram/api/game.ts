import { request } from '../services/request'
import type { PageParams, PageResult } from '../types/api'
import type {
  GameAnswerFeedback,
  GameLevel,
  GameSessionSummary,
  LevelProgress,
  LevelProgressQuery,
  LevelQuery,
  MatchStatus,
  PkRuleInput,
  PlayerProgress,
  Reward,
  SessionDetail,
  SessionQuery,
} from '../types/game'

const GAME_BASE_URL = '/api/games'

export const pageLevelsApi = (query: LevelQuery = {}) =>
  request<PageResult<GameLevel>>({ path: `${GAME_BASE_URL}/levels`, query })

export const getLevelApi = (levelId: string) =>
  request<GameLevel>({ path: `${GAME_BASE_URL}/levels/${levelId}` })

export const createPkRoomApi = (data: PkRuleInput) =>
  request<SessionDetail, PkRuleInput>({
    path: `${GAME_BASE_URL}/pk/rooms`,
    method: 'POST',
    data,
  })

export const joinPkRoomApi = (inviteCode: string) =>
  request<SessionDetail>({
    path: `${GAME_BASE_URL}/pk/rooms/${encodeURIComponent(inviteCode)}/join`,
    method: 'POST',
  })

export const requestRandomMatchApi = (data: PkRuleInput) =>
  request<MatchStatus, PkRuleInput>({
    path: `${GAME_BASE_URL}/pk/matches`,
    method: 'POST',
    data,
  })

export const cancelRandomMatchApi = (ticketId: string) =>
  request<void>({ path: `${GAME_BASE_URL}/pk/matches/${ticketId}`, method: 'DELETE' })

export const startGameSessionApi = (sessionId: string) =>
  request<SessionDetail>({ path: `${GAME_BASE_URL}/sessions/${sessionId}/start`, method: 'POST' })

export const submitGameAnswerApi = (
  sessionId: string,
  questionId: string,
  data: { requestId: string; answer: string; responseMs: number },
) =>
  request<GameAnswerFeedback>({
    path: `${GAME_BASE_URL}/sessions/${sessionId}/questions/${questionId}/answers`,
    method: 'POST',
    data,
  })

export const finishGameSessionApi = (sessionId: string) =>
  request<SessionDetail>({ path: `${GAME_BASE_URL}/sessions/${sessionId}/finish`, method: 'POST' })

export const cancelGameSessionApi = (sessionId: string) =>
  request<SessionDetail>({ path: `${GAME_BASE_URL}/sessions/${sessionId}`, method: 'DELETE' })

export const pageSessionsApi = (query: SessionQuery = {}) =>
  request<PageResult<GameSessionSummary>>({ path: `${GAME_BASE_URL}/sessions`, query })

export const getSessionApi = (sessionId: string) =>
  request<SessionDetail>({ path: `${GAME_BASE_URL}/sessions/${sessionId}` })

export const getPlayerProgressApi = () =>
  request<PlayerProgress>({ path: `${GAME_BASE_URL}/progress` })

export const listLevelProgressApi = (query: LevelProgressQuery = {}) =>
  request<LevelProgress[]>({ path: `${GAME_BASE_URL}/level-progress`, query })

export const pageRewardsApi = (query: PageParams = {}) =>
  request<PageResult<Reward>>({ path: `${GAME_BASE_URL}/rewards`, query })
