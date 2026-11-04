import type { ReactNode } from 'react'

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow">
        <h1 className="text-2xl font-bold text-slate-800">Study Manager</h1>
        <h2 className="mt-1 mb-6 text-lg text-slate-600">{title}</h2>
        {children}
      </div>
    </main>
  )
}
