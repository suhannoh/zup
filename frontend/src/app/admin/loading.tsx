function SkeletonCard() {
  return <div className="h-32 animate-pulse rounded-2xl border border-neutral-200 bg-white shadow-sm" />;
}

export default function AdminLoading() {
  return (
    <div className="space-y-6">
      <div className="space-y-3">
        <div className="h-4 w-24 animate-pulse rounded bg-blue-100" />
        <div className="h-10 w-64 animate-pulse rounded bg-neutral-200" />
        <div className="h-4 w-96 max-w-full animate-pulse rounded bg-neutral-100" />
      </div>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SkeletonCard />
        <SkeletonCard />
        <SkeletonCard />
        <SkeletonCard />
      </div>
      <div className="grid gap-4">
        <div className="h-64 animate-pulse rounded-2xl border border-neutral-200 bg-white shadow-sm" />
        <div className="h-64 animate-pulse rounded-2xl border border-neutral-200 bg-white shadow-sm" />
      </div>
    </div>
  );
}
