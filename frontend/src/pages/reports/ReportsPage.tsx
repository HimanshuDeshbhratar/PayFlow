import { useQuery } from '@tanstack/react-query'
import { FileBarChart } from 'lucide-react'
import { reportsApi } from '@/api'
import { PageHeader, Card, EmptyState, SkeletonCard } from '@/components/ui'

export function ReportsPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['reports'],
    queryFn: () => reportsApi.list(),
    retry: 1,
  })

  return (
    <div>
      <PageHeader title="Reports" description="Export-ready summaries for volume, fraud, and settlements." />
      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-3">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : isError || !data?.length ? (
        <EmptyState
          title={isError ? 'Reports unavailable' : 'No reports yet'}
          description="Generated reports will appear here once the analytics service is ready."
          icon={<FileBarChart className="h-8 w-8" />}
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-3">
          {data.map((r) => (
            <Card key={r.id}>
              <p className="text-xs uppercase tracking-wider text-muted">{r.type}</p>
              <h3 className="mt-2 font-semibold">{r.name}</h3>
              {r.description && <p className="mt-1 text-sm text-muted">{r.description}</p>}
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
