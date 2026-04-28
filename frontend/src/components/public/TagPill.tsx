import Link from "next/link";
import type { Tag } from "@/types/tag";
import type { BenefitTag } from "@/types/benefit";

type TagLike = Tag | BenefitTag;

export function TagPill({ tag }: { tag: TagLike }) {
  return (
    <Link
      href={`/tags/${tag.slug}`}
      className="inline-flex h-8 items-center rounded-full bg-neutral-100 px-3 text-xs font-medium text-neutral-700 hover:bg-blue-50 hover:text-accent"
    >
      #{tag.name}
    </Link>
  );
}
