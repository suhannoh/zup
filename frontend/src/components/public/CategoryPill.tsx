import Link from "next/link";
import type { Category } from "@/types/category";

export function CategoryPill({ category }: { category: Category }) {
  return (
    <Link
      href={`/categories/${category.slug}`}
      className="inline-flex h-10 items-center rounded-full border border-border bg-white px-4 text-sm font-medium text-neutral-700 hover:border-accent hover:text-accent"
    >
      {category.name}
    </Link>
  );
}
