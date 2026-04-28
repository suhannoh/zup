type PlaceholderPageProps = {
  title: string;
  description: string;
};

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <section className="rounded-lg border border-border bg-white p-6 md:p-8">
      <p className="text-sm font-medium text-accent">MVP placeholder</p>
      <h1 className="mt-3 text-2xl font-bold">{title}</h1>
      <p className="mt-4 leading-7 text-neutral-600">{description}</p>
    </section>
  );
}
