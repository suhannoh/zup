type SectionHeaderProps = {
  title: string;
  description?: string;
  action?: React.ReactNode;
};

export function SectionHeader({ title, description, action }: SectionHeaderProps) {
  return (
    <div className="mb-6 flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <div>
        <h2 className="text-xl font-bold text-neutral-950">{title}</h2>
        {description ? <p className="mt-2 text-sm leading-6 text-neutral-600">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}
