export default function BrandDetailLoading() {
  return (
    <div className="space-y-10">
      <section className="rounded-xl border border-border bg-white p-6">
        <div className="h-4 w-24 animate-pulse rounded bg-neutral-100" />
        <div className="mt-3 h-10 w-56 animate-pulse rounded bg-neutral-200" />
        <div className="mt-4 h-20 max-w-2xl animate-pulse rounded bg-neutral-100" />
        <div className="mt-5 flex flex-wrap gap-2">
          <div className="h-10 w-32 animate-pulse rounded-lg bg-neutral-200" />
          <div className="h-10 w-28 animate-pulse rounded-lg bg-neutral-100" />
        </div>
      </section>

      <section className="space-y-4">
        <div className="h-6 w-40 animate-pulse rounded bg-neutral-200" />
        <div className="h-4 w-72 animate-pulse rounded bg-neutral-100" />
        <div className="space-y-4">
          <div className="h-48 animate-pulse rounded-xl border border-border bg-white" />
          <div className="h-48 animate-pulse rounded-xl border border-border bg-white" />
        </div>
      </section>
    </div>
  );
}
