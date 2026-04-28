type EmptyStateProps = {
  title: string;
  description?: string;
};

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <div className="rounded-xl border border-dashed border-border bg-white px-6 py-10 text-center">
      <p className="font-semibold text-neutral-900">{title}</p>
      {description ? <p className="mt-2 text-sm leading-6 text-neutral-500">{description}</p> : null}
    </div>
  );
}
