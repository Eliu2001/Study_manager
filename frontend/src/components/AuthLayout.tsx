import type { ReactNode } from 'react'

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-8">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="mb-2 text-2xl font-bold text-slate-800">Study Manager</h1>
        <h2 className="mt-1 mb-6 text-lg text-slate-600">{title}</h2>
        {children}
      </div>
    </main>
  )
}
