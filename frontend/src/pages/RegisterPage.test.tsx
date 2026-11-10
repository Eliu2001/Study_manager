import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { register, login } from '../api/auth'
import { ApiError } from '../api/client'
import { RegisterPage } from './RegisterPage'

// Renderizado sin DOM: se controlan los estados de TanStack sin añadir librerías.
const { mutationHook } = vi.hoisted(() => ({ mutationHook: vi.fn() }))
vi.mock('@tanstack/react-query', () => ({ useMutation: mutationHook }))
vi.mock('../api/auth', () => ({ register: vi.fn(), login: vi.fn() }))

function renderPage() {
  return renderToStaticMarkup(<MemoryRouter><RegisterPage /></MemoryRouter>)
}

beforeEach(() => {
  vi.resetAllMocks()
  mutationHook.mockReturnValue({
    isPending: false,
    isSuccess: false,
    error: null,
    mutate: vi.fn(),
  })
})

describe('RegisterPage', () => {
  it('registra sin intentar iniciar sesión ni reintentar automáticamente', async () => {
    const user = { id: 1, name: 'Ana', email: 'ana@example.com' }
    const data = { name: user.name, email: user.email, password: 'password123' }
    vi.mocked(register).mockResolvedValue(user)
    vi.mocked(login).mockRejectedValue(new Error('Login no disponible'))
    renderPage()

    const options = mutationHook.mock.calls[0][0]
    await expect(options.mutationFn(data)).resolves.toEqual(user)

    expect(register).toHaveBeenCalledExactlyOnceWith(data)
    expect(login).not.toHaveBeenCalled()
    expect(options.retry).toBe(false)
  })

  it('sustituye el formulario por confirmación y un enlace a iniciar sesión', () => {
    mutationHook.mockReturnValue({ isSuccess: true, isPending: false, error: null })

    const html = renderPage()

    expect(html).toContain('Tu cuenta se creó correctamente.')
    expect(html).toContain('no necesitas volver a registrarte')
    expect(html).toContain('role="status"')
    expect(html).toContain('href="/login"')
    expect(html).not.toContain('<form')
    expect(html).not.toContain('type="password"')
  })

  it('conserva el formulario y el error cuando el registro falla', () => {
    mutationHook.mockReturnValue({
      isSuccess: false,
      isPending: false,
      error: new ApiError(409, 'Conflicto'),
    })

    const html = renderPage()

    expect(html).toContain('<form')
    expect(html).toContain('Ya existe una cuenta con ese correo.')
    expect(html).not.toContain('Tu cuenta se creó correctamente.')
  })
})
