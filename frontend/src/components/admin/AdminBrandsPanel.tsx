"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  createAdminBrand,
  getAdminBrands,
  updateAdminBrand,
  updateAdminBrandActive,
} from "@/lib/api/adminApi";
import { getCategories } from "@/lib/api/publicApi";
import type {
  AdminBrand,
  AdminBrandCreateRequest,
  AdminBrandUpdateRequest,
} from "@/types/adminBrand";
import type { Category } from "@/types/category";

type ActiveFilter = "ALL" | "ACTIVE" | "INACTIVE";

type BrandFormState = {
  categoryId: string;
  name: string;
  slug: string;
  description: string;
  officialUrl: string;
  membershipUrl: string;
  appUrl: string;
  brandColor: string;
  logoUrl: string;
  isActive: boolean;
};

const emptyForm: BrandFormState = {
  categoryId: "",
  name: "",
  slug: "",
  description: "",
  officialUrl: "",
  membershipUrl: "",
  appUrl: "",
  brandColor: "",
  logoUrl: "",
  isActive: true,
};

const slugPattern = /^[a-z0-9-]+$/;

function normalizeOptional(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function toCreateRequest(form: BrandFormState): AdminBrandCreateRequest {
  return {
    categoryId: Number(form.categoryId),
    name: form.name.trim(),
    slug: form.slug.trim(),
    description: normalizeOptional(form.description),
    officialUrl: normalizeOptional(form.officialUrl),
    membershipUrl: normalizeOptional(form.membershipUrl),
    appUrl: normalizeOptional(form.appUrl),
    brandColor: normalizeOptional(form.brandColor),
    logoUrl: normalizeOptional(form.logoUrl),
    isActive: form.isActive,
  };
}

function toUpdateRequest(form: BrandFormState): AdminBrandUpdateRequest {
  return toCreateRequest(form);
}

function toFormState(brand: AdminBrand): BrandFormState {
  return {
    categoryId: String(brand.categoryId),
    name: brand.name,
    slug: brand.slug,
    description: brand.description ?? "",
    officialUrl: brand.officialUrl ?? "",
    membershipUrl: brand.membershipUrl ?? "",
    appUrl: brand.appUrl ?? "",
    brandColor: brand.brandColor ?? "",
    logoUrl: brand.logoUrl ?? "",
    isActive: brand.isActive,
  };
}

function getInitial(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || "Z";
}

export function AdminBrandsPanel() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<AdminBrand[]>([]);
  const [keyword, setKeyword] = useState("");
  const [categorySlug, setCategorySlug] = useState("ALL");
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>("ALL");
  const [form, setForm] = useState<BrandFormState>(emptyForm);
  const [editingBrand, setEditingBrand] = useState<AdminBrand | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const params = useMemo(
    () => ({
      keyword: keyword.trim() || undefined,
      categorySlug: categorySlug === "ALL" ? undefined : categorySlug,
      isActive:
        activeFilter === "ALL"
          ? undefined
          : activeFilter === "ACTIVE",
    }),
    [activeFilter, categorySlug, keyword]
  );

  async function loadBrands() {
    setLoading(true);
    setError(null);
    try {
      const data = await getAdminBrands(params);
      setBrands(data);
    } catch {
      setError("브랜드 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let active = true;

    async function loadCategories() {
      try {
        const data = await getCategories();
        if (active) {
          setCategories(data);
        }
      } catch {
        if (active) {
          setError("브랜드 목록을 불러오지 못했습니다.");
        }
      }
    }

    loadCategories();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await getAdminBrands(params);
        if (active) {
          setBrands(data);
        }
      } catch {
        if (active) {
          setError("브랜드 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    load();

    return () => {
      active = false;
    };
  }, [params]);

  function updateForm(next: Partial<BrandFormState>) {
    setForm((current) => ({
      ...current,
      ...next,
    }));
  }

  function resetForm() {
    setEditingBrand(null);
    setForm(emptyForm);
  }

  function startEdit(brand: AdminBrand) {
    setEditingBrand(brand);
    setForm(toFormState(brand));
    setMessage(null);
    setError(null);
  }

  function validateForm() {
    if (!form.categoryId || !form.name.trim() || !form.slug.trim()) {
      return "카테고리, 브랜드명, slug는 필수입니다.";
    }
    if (!slugPattern.test(form.slug.trim())) {
      return "slug는 영문 소문자, 숫자, 하이픈만 입력할 수 있습니다.";
    }
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationMessage = validateForm();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      if (editingBrand) {
        await updateAdminBrand(editingBrand.id, toUpdateRequest(form));
        setMessage("브랜드 정보가 수정되었습니다.");
      } else {
        await createAdminBrand(toCreateRequest(form));
        setMessage("브랜드가 등록되었습니다.");
      }
      resetForm();
      await loadBrands();
    } catch {
      setError("브랜드 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleActive(brand: AdminBrand) {
    setTogglingId(brand.id);
    setMessage(null);
    setError(null);

    try {
      const updated = await updateAdminBrandActive(brand.id, { isActive: !brand.isActive });
      setBrands((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      if (editingBrand?.id === updated.id) {
        setEditingBrand(updated);
        setForm(toFormState(updated));
      }
      setMessage("브랜드 노출 상태가 변경되었습니다.");
    } catch {
      setError("브랜드 저장에 실패했습니다.");
    } finally {
      setTogglingId(null);
    }
  }

  return (
    <section className="space-y-5">
      <div className="rounded-lg border border-border bg-white p-4">
        <div className="grid gap-3 lg:grid-cols-[1fr_220px_160px]">
          <label className="space-y-2 text-sm font-medium">
            <span>검색어</span>
            <input
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="브랜드명 검색"
            />
          </label>

          <label className="space-y-2 text-sm font-medium">
            <span>카테고리</span>
            <select
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={categorySlug}
              onChange={(event) => setCategorySlug(event.target.value)}
            >
              <option value="ALL">전체</option>
              {categories.map((category) => (
                <option key={category.slug} value={category.slug}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>

          <label className="space-y-2 text-sm font-medium">
            <span>활성 상태</span>
            <select
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={activeFilter}
              onChange={(event) => setActiveFilter(event.target.value as ActiveFilter)}
            >
              <option value="ALL">전체</option>
              <option value="ACTIVE">활성</option>
              <option value="INACTIVE">비활성</option>
            </select>
          </label>
        </div>
      </div>

      {message ? (
        <div className="rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700">
          {message}
        </div>
      ) : null}

      {error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      ) : null}

      <div className="grid gap-5 xl:grid-cols-[380px_1fr]">
        <form className="space-y-4 rounded-lg border border-border bg-white p-5" onSubmit={handleSubmit}>
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">
              {editingBrand ? "브랜드 수정" : "브랜드 등록"}
            </h2>
            {editingBrand ? (
              <button className="text-sm font-semibold text-accent" type="button" onClick={resetForm}>
                새로 등록
              </button>
            ) : null}
          </div>

          <label className="space-y-2 text-sm font-medium">
            <span>카테고리</span>
            <select
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={form.categoryId}
              onChange={(event) => updateForm({ categoryId: event.target.value })}
              required
            >
              <option value="">선택</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>

          <label className="space-y-2 text-sm font-medium">
            <span>브랜드명</span>
            <input
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={form.name}
              onChange={(event) => updateForm({ name: event.target.value })}
              required
            />
          </label>

          <label className="space-y-2 text-sm font-medium">
            <span>slug</span>
            <input
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={form.slug}
              onChange={(event) => updateForm({ slug: event.target.value })}
              placeholder="예: starbucks, oliveyoung, movie-culture"
              required
            />
            <span className="block text-xs font-normal text-neutral-500">
              영문 소문자, 숫자, 하이픈만 입력할 수 있습니다.
            </span>
          </label>

          <label className="space-y-2 text-sm font-medium">
            <span>설명</span>
            <textarea
              className="min-h-24 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm leading-6"
              value={form.description}
              onChange={(event) => updateForm({ description: event.target.value })}
            />
          </label>

          <div className="grid gap-3">
            <UrlInput label="공식 홈페이지 URL" value={form.officialUrl} onChange={(value) => updateForm({ officialUrl: value })} />
            <UrlInput label="멤버십 URL" value={form.membershipUrl} onChange={(value) => updateForm({ membershipUrl: value })} />
            <UrlInput label="앱 URL" value={form.appUrl} onChange={(value) => updateForm({ appUrl: value })} />
          </div>

          <label className="space-y-2 text-sm font-medium">
            <span>브랜드 컬러</span>
            <input
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={form.brandColor}
              onChange={(event) => updateForm({ brandColor: event.target.value })}
              placeholder="#3B6FE8"
            />
          </label>

          <label className="space-y-2 text-sm font-medium">
            <span>logoUrl</span>
            <input
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={form.logoUrl}
              onChange={(event) => updateForm({ logoUrl: event.target.value })}
              placeholder="https://..."
            />
            <span className="block rounded-lg bg-amber-50 p-3 text-xs font-normal leading-5 text-amber-800">
              로고 이미지는 저작권 이슈가 있을 수 있으므로 MVP에서는 사용하지 않는 것을 권장합니다.
            </span>
          </label>

          <label className="flex items-center gap-2 text-sm font-medium">
            <input
              checked={form.isActive}
              onChange={(event) => updateForm({ isActive: event.target.checked })}
              type="checkbox"
            />
            활성 상태로 노출
          </label>

          <button
            className="h-11 w-full rounded-lg bg-accent px-4 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
            type="submit"
            disabled={saving}
          >
            {saving ? "저장 중" : editingBrand ? "수정 저장" : "브랜드 등록"}
          </button>
        </form>

        <div className="space-y-4">
          {loading ? (
            <div className="rounded-lg border border-border bg-white p-8 text-center text-sm text-neutral-600">
              브랜드를 불러오는 중입니다.
            </div>
          ) : null}

          {!loading && brands.length === 0 && !error ? (
            <div className="rounded-lg border border-border bg-white p-8 text-center text-sm text-neutral-600">
              아직 등록된 브랜드가 없습니다.
            </div>
          ) : null}

          {brands.map((brand) => (
            <article key={brand.id} className="rounded-lg border border-border bg-white p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="flex min-w-0 items-start gap-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-neutral-100 text-base font-bold text-neutral-700">
                    {getInitial(brand.name)}
                  </div>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-lg font-semibold">{brand.name}</h2>
                      <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                        {brand.categoryName}
                      </span>
                      <span
                        className={`rounded-full px-3 py-1 text-xs font-semibold ${
                          brand.isActive ? "bg-green-50 text-green-700" : "bg-neutral-100 text-neutral-700"
                        }`}
                      >
                        {brand.isActive ? "활성" : "비활성"}
                      </span>
                    </div>
                    <p className="mt-1 text-sm text-neutral-500">/{brand.slug}</p>
                  </div>
                </div>

                <div className="flex gap-2">
                  <button
                    className="h-9 rounded-lg border border-border px-3 text-sm font-semibold"
                    type="button"
                    onClick={() => startEdit(brand)}
                  >
                    수정
                  </button>
                  <button
                    className="h-9 rounded-lg border border-border px-3 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60"
                    type="button"
                    disabled={togglingId === brand.id}
                    onClick={() => handleToggleActive(brand)}
                  >
                    {brand.isActive ? "비활성" : "활성"}
                  </button>
                </div>
              </div>

              <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-neutral-700">
                {brand.description || "설명 없음"}
              </p>

              <dl className="mt-4 grid gap-3 text-sm md:grid-cols-2">
                <BrandLink label="공식 홈페이지" value={brand.officialUrl} />
                <BrandLink label="멤버십" value={brand.membershipUrl} />
                <BrandLink label="앱 링크" value={brand.appUrl} />
                <div>
                  <dt className="font-medium text-neutral-500">브랜드 컬러</dt>
                  <dd className="mt-1 flex items-center gap-2">
                    {brand.brandColor ? (
                      <>
                        <span
                          className="h-4 w-4 rounded-full border border-border"
                          style={{ backgroundColor: brand.brandColor }}
                        />
                        <span>{brand.brandColor}</span>
                      </>
                    ) : (
                      "-"
                    )}
                  </dd>
                </div>
              </dl>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function UrlInput({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      <input
        className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="https://..."
        type="url"
      />
    </label>
  );
}

function BrandLink({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt className="font-medium text-neutral-500">{label}</dt>
      <dd className="mt-1 break-all">
        {value ? (
          <a className="text-accent hover:underline" href={value} target="_blank" rel="noreferrer">
            {value}
          </a>
        ) : (
          "-"
        )}
      </dd>
    </div>
  );
}
