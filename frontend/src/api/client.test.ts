import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, setUnauthorizedHandler } from './client'

const TOKEN_KEY = 'study-manager.token'

/** Los tests corren sin navegador, así que se simula sessionStorage. */
function stubStorage(initial: Record<string, string> = {}) {
  const data: Record<string, string> = { ...initial }
  vi.stubGlobal('sessionStorage', {
    getItem: (key: string) => data[key] ?? null,
    setItem: (key: string, value: string) => {
      data[key] = value
    },
    removeItem: (key: string) => {
      delete data[key]
    },
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
  setUnauthorizedHandler(null)
})

describe('apiFetch', () => {
  it('envía el token en la cabecera Authorization', async () => {
    stubStorage({ [TOKEN_KEY]: 'abc' })
    const fetchMock = vi.fn<typeof fetch>(async () => new Response('[]', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/api/terms')

    const headers = fetchMock.mock.calls[0][1]?.headers as Record<string, string>
    expect(headers['Authorization']).toBe('Bearer abc')
  })

  it('no envía el token cuando se pide auth: false', async () => {
    stubStorage({ [TOKEN_KEY]: 'abc' })
    const fetchMock = vi.fn<typeof fetch>(async () => new Response('{}', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/api/auth/login', { method: 'POST', body: {}, auth: false })

    const headers = fetchMock.mock.calls[0][1]?.headers as Record<string, string>
    expect(headers['Authorization']).toBeUndefined()
  })

  it('cierra la sesión cuando el servidor responde 401 con un token vigente', async () => {
    stubStorage({ [TOKEN_KEY]: 'abc' })
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 401 })))

    await expect(apiFetch('/api/terms')).rejects.toBeInstanceOf(ApiError)

    expect(handler).toHaveBeenCalledOnce()
  })

  it('no cierra la sesión por un 401 de login (no hay token)', async () => {
    stubStorage()
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 401 })))

    await expect(
      apiFetch('/api/auth/login', { method: 'POST', body: {}, auth: false }),
    ).rejects.toBeInstanceOf(ApiError)

    expect(handler).not.toHaveBeenCalled()
  })

  it('indica los minutos de espera cuando llega un 429 con Retry-After', async () => {
    stubStorage()
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(null, { status: 429, headers: { 'Retry-After': '600' } })),
    )

    await expect(
      apiFetch('/api/auth/login', { method: 'POST', body: {}, auth: false }),
    ).rejects.toMatchObject({ status: 429, message: expect.stringContaining('10 minutos') })
  })

  it('devuelve un error de conexión cuando el servidor no responde', async () => {
    stubStorage()
    vi.stubGlobal('fetch', vi.fn(async () => {
      throw new TypeError('Failed to fetch')
    }))

    await expect(apiFetch('/api/terms')).rejects.toMatchObject({ status: 0 })
  })

  it('devuelve undefined cuando la respuesta es 204', async () => {
    stubStorage({ [TOKEN_KEY]: 'abc' })
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 204 })))

    await expect(apiFetch<void>('/api/terms/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
