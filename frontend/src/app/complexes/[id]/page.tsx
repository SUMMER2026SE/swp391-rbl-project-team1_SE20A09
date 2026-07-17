import { notFound } from 'next/navigation'
import { Suspense } from 'react'
import { getComplexDetail, getComplexFacilities } from '@/lib/api/complex'
import ComplexDetailClient from './components/ComplexDetailClient'
import { Header } from '@/components/layout/Header'

interface PageProps {
  params: { id: string }
  searchParams: Record<string, string | string[] | undefined>
}

export default async function ComplexDetailPage({ params }: PageProps) {
  const complexId = parseInt(params.id, 10)

  if (isNaN(complexId)) {
    notFound()
  }

  let complex
  let facilities = []
  try {
    complex = await getComplexDetail(complexId)
    facilities = await getComplexFacilities(complexId)
  } catch (error: any) {
    console.error("Failed to fetch complex data on server:", error)
    if (error?.status === 404) {
      notFound()
    }
    // Throw error to trigger Next.js nearest error boundary for server/network errors
    throw error
  }

  if (!complex) {
    notFound()
  }

  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gray-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6 animate-pulse">
          <div className="h-[380px] bg-gray-200 rounded-2xl" />
          <div className="h-12 bg-gray-200 rounded-xl w-2/3" />
          <div className="flex gap-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-20 w-40 bg-gray-200 rounded-xl" />
            ))}
          </div>
          <div className="space-y-3">
            {[1, 2].map((i) => (
              <div key={i} className="h-16 bg-gray-200 rounded-xl" />
            ))}
          </div>
        </div>
      </div>
    }>
      <ComplexDetailClient
        complex={complex}
        initialFacilities={facilities}
      />
    </Suspense>
  )
}
