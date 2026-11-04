import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { login } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { buttonClass, inputClass, linkClass } from '../components/ui'

export function LoginPage() {
  const { signIn } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  // Al iniciar sesión, GuestRoute redirige solo al inicio.
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => signIn(data.accessToken),
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate({ email: email.trim(), password })
  }

  const error = mutation.error
  let message: string | null = null
  if (error instanceof ApiError) {
    message = error.status === 401 ? 'Correo o contraseña incorrectos.' : error.message
  } else if (error) {
    message = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
  }

  return (
    <AuthLayout title="Iniciar sesión">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-slate-700">
            Correo electrónico
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClass}
          />
        </div>

        <div>
          <label htmlFor="password" className="block text-sm font-medium text-slate-700">
            Contraseña
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
        </div>

        {message && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {message}
          </p>
        )}

        <button type="submit" disabled={mutation.isPending} className={buttonClass}>
          {mutation.isPending ? 'Entrando...' : 'Entrar'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-600">
        ¿No tienes cuenta?{' '}
        <Link to="/register" className={linkClass}>
          Regístrate
        </Link>
      </p>
    </AuthLayout>
  )
}
