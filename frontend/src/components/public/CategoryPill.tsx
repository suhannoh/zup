import Link from "next/link";
import { getCategoryTheme } from "@/lib/categoryTheme";
import type { Category } from "@/types/category";

export function CategoryPill({ category, selected = false }: { category: Category; selected?: boolean }) {
  const theme = getCategoryTheme(category.slug);

  return (
    <Link
      href={`/categories/${category.slug}`}
      className={[
        "inline-flex h-10 items-center rounded-full border px-4 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-blue-200",
        selected
          ? "border-blue-600 bg-blue-600 text-white hover:bg-blue-700"
          : `${theme.badge} hover:border-blue-200 hover:bg-white hover:text-blue-700`,
      ].join(" ")}
    >
      {category.name}
    </Link>
  );
}
