import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useMutation } from '@tanstack/react-query'
import { register } from '../api/auth'
import { ApiError } from '../api/client'
import { AuthLayout } from '../components/AuthLayout'
import { buttonClass, inputClass, linkClass } from '../components/ui'
import { validatePassword } from '../utils/password'

export function RegisterPage() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  // El registro confirmado no depende de un inicio de sesión posterior.
  const mutation = useMutation({
    mutationFn: register,
    retry: false,
    onSuccess: () => {
      setPassword('')
      setConfirmation('')
    },
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (mutation.isPending || mutation.isSuccess) return
    if (password !== confirmation) {
      setLocalError('Las contraseñas no coinciden.')
      return
    }
    const passwordError = validatePassword(password)
    if (passwordError) {
      setLocalError(passwordError)
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
        'Revisa los datos: el nombre y el correo son obligatorios y la contraseña debe tener al menos 8 caracteres y como máximo 72 bytes en UTF-8. Las letras como ñ y los emojis pueden ocupar varios bytes.'
    } else {
      serverMessage = error.message
    }
  } else if (error) {
    serverMessage = 'Ocurrió un error inesperado. Inténtalo de nuevo.'
  }
  const message = localError ?? serverMessage

  if (mutation.isSuccess) {
    return (
      <AuthLayout title="Cuenta creada">
        <p role="status" className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-800">
          Tu cuenta se creó correctamente. Inicia sesión para continuar; no necesitas volver a registrarte.
        </p>
        <Link to="/login" className={`${buttonClass} mt-6 block text-center`}>
          Iniciar sesión
        </Link>
      </AuthLayout>
    )
  }

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
            aria-describedby="password-help"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
          <p id="password-help" className="mt-1 text-xs text-slate-500">
            Mínimo 8 caracteres y máximo 72 bytes en UTF-8. Una letra como ñ ocupa 2 bytes;
            un emoji como 😀, 4 bytes.
          </p>
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
