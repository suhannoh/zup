export type CategoryTheme = {
  badge: string;
  icon: string;
  accent: string;
};

const defaultTheme: CategoryTheme = {
  badge: "bg-blue-50 text-blue-700 border-blue-100",
  icon: "bg-blue-600 text-white",
  accent: "bg-blue-600",
};

const categoryThemes: Record<string, CategoryTheme> = {
  cafe: {
    badge: "bg-amber-50 text-amber-800 border-amber-100",
    icon: "bg-amber-700 text-white",
    accent: "bg-amber-600",
  },
  bakery: {
    badge: "bg-orange-50 text-orange-800 border-orange-100",
    icon: "bg-orange-500 text-white",
    accent: "bg-orange-500",
  },
  food: {
    badge: "bg-emerald-50 text-emerald-700 border-emerald-100",
    icon: "bg-emerald-600 text-white",
    accent: "bg-emerald-600",
  },
  "movie-culture": {
    badge: "bg-indigo-50 text-indigo-700 border-indigo-100",
    icon: "bg-indigo-600 text-white",
    accent: "bg-indigo-600",
  },
  beauty: {
    badge: "bg-pink-50 text-pink-700 border-pink-100",
    icon: "bg-pink-500 text-white",
    accent: "bg-pink-500",
  },
  "theme-park": {
    badge: "bg-sky-50 text-sky-700 border-sky-100",
    icon: "bg-sky-600 text-white",
    accent: "bg-sky-600",
  },
};

export function getCategoryTheme(slug?: string | null): CategoryTheme {
  if (!slug) {
    return defaultTheme;
  }

  return categoryThemes[slug] ?? defaultTheme;
}
