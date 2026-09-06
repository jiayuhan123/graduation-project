import { request } from '../services/request'
import type { PageParams, PageResult } from '../types/api'
import type {
  GameLevel,
  GameSessionSummary,
  LevelProgress,
  LevelProgressQuery,
  LevelQuery,
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
