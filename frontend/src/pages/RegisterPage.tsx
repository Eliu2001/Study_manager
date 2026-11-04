import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { login, register, type RegisterData } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { buttonClass, inputClass, linkClass } from '../components/ui'

export function RegisterPage() {
  const { signIn } = useAuth()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  // Crea la cuenta e inicia sesión de inmediato; GuestRoute redirige al inicio.
  const mutation = useMutation({
    mutationFn: async (data: RegisterData) => {
      await register(data)
      return login({ email: data.email, password: data.password })
    },
    onSuccess: (data) => signIn(data.accessToken),
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (password !== confirmation) {
      setLocalError('Las contraseñas no coinciden.')
      return
    }
    setLocalError(null)
    mutation.mutate({ name: name.trim(), email: email.trim(), password })
  }

  const error = mutation.error
  let serverMessage: string | null = null
  if (error instanceof ApiError) {
    if (error.status === 409) {
      serverMessage = 'Ya existe una cuenta con ese correo.'
    } else if (error.status === 400) {
      serverMessage =
        'Revisa los datos: el nombre y el correo son obligatorios y la contraseña debe tener entre 8 y 72 caracteres.'
    } else {
      serverMessage = error.message
    }
  } else if (error) {
    serverMessage = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
  }
  const message = localError ?? serverMessage

  return (
    <AuthLayout title="Crear cuenta">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-slate-700">
            Nombre
          </label>
          <input
            id="name"
            type="text"
            autoComplete="name"
            required
            maxLength={100}
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClass}
          />
        </div>

        <div>
          <label htmlFor="email" className="block text-sm font-medium text-slate-700">
            Correo electrónico
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            maxLength={150}
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
            autoComplete="new-password"
            required
            minLength={8}
            maxLength={72}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
          <p className="mt-1 text-xs text-slate-500">Entre 8 y 72 caracteres.</p>
        </div>

        <div>
          <label htmlFor="confirmation" className="block text-sm font-medium text-slate-700">
            Repite la contraseña
          </label>
          <input
            id="confirmation"
            type="password"
            autoComplete="new-password"
            required
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
            className={inputClass}
          />
        </div>

        {message && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {message}
          </p>
        )}

        <button type="submit" disabled={mutation.isPending} className={buttonClass}>
          {mutation.isPending ? 'Creando cuenta...' : 'Crear cuenta'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-600">
        ¿Ya tienes cuenta?{' '}
        <Link to="/login" className={linkClass}>
          Inicia sesión
        </Link>
      </p>
    </AuthLayout>
  )
}
