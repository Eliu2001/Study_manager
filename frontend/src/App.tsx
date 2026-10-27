import { useEffect, useState } from 'react'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export default function App() {
  const [status, setStatus] = useState('Comprobando la conexión con la API...')

  useEffect(() => {
    fetch(`${API_URL}/api/users/me`)
        .then((res) =>
            setStatus(
                res.status === 401
                    ? 'La API responde (401 sin token: es lo esperado).'
                    : `La API respondió con el código ${res.status}.`,
            ),
        )
        .catch(() =>
            setStatus('No se pudo conectar con la API. ¿Está corriendo en el puerto 8080?'),
        )
  }, [])

  return (
      <main className="flex min-h-screen items-center justify-center bg-slate-100">
        <div className="rounded-xl bg-white p-8 shadow">
          <h1 className="text-2xl font-bold text-slate-800">Study Manager</h1>
          <p className="mt-2 text-slate-600">{status}</p>
        </div>
      </main>
  )
}