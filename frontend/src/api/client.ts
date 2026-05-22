import {
  getPlanReadyFixture,
  getSseFixtureFrames,
} from './fixtures'

import type {
  AdjustPlanRequest,
  AdjustPlanResponse,
  ClarifyPlanRequest,
  ClarifyPlanResponse,
  CreatePlanRequest,
  CreatePlanResponse,
  DebugScenarioRequest,
  DebugScenarioResponse,
  ExecutePlanRequest,
  ExecutePlanResponse,
  PlanReadyEvent,
  Scenario,
  SseFixtureFrame,
} from './types'

export type ApiMode = 'mock' | 'real'

export interface PlannerApiClientOptions {
  mode?: ApiMode
  baseUrl?: string
  fetchImpl?: typeof fetch
}

export interface PlannerApiClient {
  mode: ApiMode
  createPlan(request: CreatePlanRequest): Promise<CreatePlanResponse>
  executePlan(
    planId: string,
    request: ExecutePlanRequest,
  ): Promise<ExecutePlanResponse>
  clarifyPlan(
    planId: string,
    request: ClarifyPlanRequest,
  ): Promise<ClarifyPlanResponse>
  adjustPlan(
    planId: string,
    request: AdjustPlanRequest,
  ): Promise<AdjustPlanResponse>
  updateDebugScenario(
    request: DebugScenarioRequest,
  ): Promise<DebugScenarioResponse>
  openPlanStream(planId: string): EventSource
  getPlanReadyFixture(scenario: Scenario): Promise<PlanReadyEvent>
  getSseFixtureFrames(): Promise<SseFixtureFrame[]>
}

export class PlannerApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly body: unknown,
  ) {
    super(message)
    this.name = 'PlannerApiError'
  }
}

const DEFAULT_BASE_URL = 'http://localhost:8000'

export function createPlannerApiClient(
  options: PlannerApiClientOptions = {},
): PlannerApiClient {
  const mode = options.mode ?? readApiMode()
  const baseUrl = trimTrailingSlash(options.baseUrl ?? readApiBaseUrl())
  const fetchImpl = options.fetchImpl ?? fetch

  if (mode === 'mock') {
    return createMockClient()
  }

  async function requestJson<TResponse>(
    path: string,
    init: RequestInit,
  ): Promise<TResponse> {
    const response = await fetchImpl(`${baseUrl}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...init.headers,
      },
    })

    const body = (await response.json()) as unknown

    if (!response.ok) {
      const message =
        isReadableError(body) ? body.message : `API request failed: ${path}`
      throw new PlannerApiError(message, response.status, body)
    }

    return body as TResponse
  }

  return {
    mode,
    createPlan(request) {
      return requestJson<CreatePlanResponse>('/api/plan', {
        method: 'POST',
        body: JSON.stringify(request),
      })
    },
    executePlan(planId, request) {
      return requestJson<ExecutePlanResponse>(
        `/api/plan/${encodeURIComponent(planId)}/execute`,
        {
          method: 'POST',
          body: JSON.stringify(request),
        },
      )
    },
    clarifyPlan(planId, request) {
      return requestJson<ClarifyPlanResponse>(
        `/api/plan/${encodeURIComponent(planId)}/clarify`,
        {
          method: 'POST',
          body: JSON.stringify(request),
        },
      )
    },
    adjustPlan(planId, request) {
      return requestJson<AdjustPlanResponse>(
        `/api/plan/${encodeURIComponent(planId)}/adjust`,
        {
          method: 'PATCH',
          body: JSON.stringify(request),
        },
      )
    },
    updateDebugScenario(request) {
      return requestJson<DebugScenarioResponse>('/api/debug/scenario', {
        method: 'POST',
        body: JSON.stringify(request),
      })
    },
    openPlanStream(planId) {
      return new EventSource(
        `${baseUrl}/api/plan/${encodeURIComponent(planId)}/stream`,
      )
    },
    async getPlanReadyFixture() {
      throw new Error('Fixture access is only available in mock API mode')
    },
    async getSseFixtureFrames() {
      throw new Error('Fixture access is only available in mock API mode')
    },
  }
}

function createMockClient(): PlannerApiClient {
  return {
    mode: 'mock',
    async createPlan(request) {
      const fixture = getPlanReadyFixture(request.scenario)
      return {
        planId: fixture.planId,
        status: 'processing',
      }
    },
    async executePlan(planId) {
      return {
        planId,
        status: 'executing',
        message: 'mock mode: execution events are read from fixture data',
      }
    },
    async clarifyPlan(planId) {
      return {
        planId,
        status: 'processing',
        message: 'mock mode: clarification accepted',
      }
    },
    async adjustPlan(planId) {
      return {
        planId,
        status: 'adjusting',
        message: 'mock mode: adjustment accepted',
      }
    },
    async updateDebugScenario(request) {
      return {
        updated: request,
      }
    },
    openPlanStream() {
      throw new Error('Use getSseFixtureFrames in mock API mode')
    },
    async getPlanReadyFixture(scenario) {
      return getPlanReadyFixture(scenario)
    },
    async getSseFixtureFrames() {
      return getSseFixtureFrames()
    },
  }
}

function readApiMode(): ApiMode {
  return import.meta.env.VITE_API_MODE === 'real' ? 'real' : 'mock'
}

function readApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || DEFAULT_BASE_URL
}

function trimTrailingSlash(value: string): string {
  return value.replace(/\/+$/, '')
}

function isReadableError(value: unknown): value is { message: string } {
  return (
    typeof value === 'object' &&
    value !== null &&
    'message' in value &&
    typeof value.message === 'string'
  )
}
