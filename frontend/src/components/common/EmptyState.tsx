import Link from "next/link";

type EmptyStateProps = {
  title: string;
  description?: string;
  actionHref?: string;
  actionLabel?: string;
  icon?: "gift" | "search" | "check" | "warning" | "inbox";
  compact?: boolean;
};

function EmptyStateIcon({ icon = "inbox" }: { icon?: EmptyStateProps["icon"] }) {
  const paths = {
    gift: (
      <>
        <path d="M20 8h-3.2A3 3 0 1 0 12 4.5 3 3 0 1 0 7.2 8H4v12h16V8Z" />
        <path d="M12 8v12M4 12h16" />
      </>
    ),
    search: (
      <>
        <path d="M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14Z" />
        <path d="m16 16 4 4" />
      </>
    ),
    check: (
      <>
        <path d="M20 7 10 17l-5-5" />
        <path d="M12 21a9 9 0 1 1 8.5-6" />
      </>
    ),
    warning: (
      <>
        <path d="M12 4 3 20h18L12 4Z" />
        <path d="M12 9v4M12 17h.01" />
      </>
    ),
    inbox: (
      <>
        <path d="M4 6h16v12H4V6Z" />
        <path d="M4 13h4l2 3h4l2-3h4" />
      </>
    ),
  };

  return (
    <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-blue-600">
      <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <g stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8">
          {paths[icon ?? "inbox"]}
        </g>
      </svg>
    </div>
  );
}

export function EmptyState({
  actionHref,
  actionLabel,
  compact = false,
  description,
  icon = "inbox",
  title,
}: EmptyStateProps) {
  return (
    <div
      className={[
        "rounded-2xl border border-dashed border-neutral-200 bg-white text-center shadow-sm",
        compact ? "px-4 py-6" : "px-6 py-10",
      ].join(" ")}
    >
      <EmptyStateIcon icon={icon} />
      <p className="mt-4 font-semibold text-neutral-900">{title}</p>
      {description ? <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-neutral-500">{description}</p> : null}
      {actionHref && actionLabel ? (
        <Link
          href={actionHref}
          className="mt-5 inline-flex h-10 items-center justify-center rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
        >
          {actionLabel}
        </Link>
      ) : null}
    </div>
  );
}
