"use client";

import { FormEvent, useMemo, useState } from "react";
import { createReport } from "@/lib/api/publicApi";
import type { BrandListItem } from "@/types/brand";
import type { ReportType } from "@/types/report";

const reportTypeOptions: { value: ReportType; label: string }[] = [
  { value: "WRONG_INFO", label: "정보가 달라요" },
  { value: "BENEFIT_ENDED", label: "혜택이 종료됐어요" },
  { value: "CONDITION_CHANGED", label: "조건이 달라졌어요" },
  { value: "NEW_BENEFIT", label: "새 혜택이 있어요" },
  { value: "OFFICIAL_LINK_FOUND", label: "공식 링크를 찾았어요" },
  { value: "ETC", label: "기타" },
];

type ReportFormProps = {
  brands: BrandListItem[];
  initialBrandId?: number | null;
  initialBenefitId?: number | null;
};

type SubmitStatus = "idle" | "submitting" | "success" | "error";

export function ReportForm({ brands, initialBrandId, initialBenefitId }: ReportFormProps) {
  const [brandId, setBrandId] = useState(initialBrandId?.toString() ?? "");
  const [reportType, setReportType] = useState<ReportType>("WRONG_INFO");
  const [content, setContent] = useState("");
  const [referenceUrl, setReferenceUrl] = useState("");
  const [email, setEmail] = useState("");
  const [status, setStatus] = useState<SubmitStatus>("idle");

  const selectedBrand = useMemo(
    () => brands.find((brand) => brand.id.toString() === brandId),
    [brandId, brands]
  );

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus("submitting");

    try {
      await createReport({
        brandId: brandId ? Number(brandId) : null,
        benefitId: initialBenefitId ?? null,
        reportType,
        content,
        referenceUrl: referenceUrl || null,
        email: email || null,
      });
      setStatus("success");
      setContent("");
      setReferenceUrl("");
      setEmail("");
    } catch {
      setStatus("error");
    }
  }

  if (status === "success") {
    return (
      <div className="rounded-xl border border-green-100 bg-green-50 p-6">
        <h2 className="text-xl font-bold text-green-900">제보가 접수되었습니다.</h2>
        <p className="mt-3 text-sm leading-7 text-green-800">
          공식 출처 확인 후 필요한 경우 혜택 정보를 업데이트하겠습니다.
        </p>
        <button
          type="button"
          className="mt-5 rounded-lg bg-accent px-4 py-2 text-sm font-semibold text-white"
          onClick={() => setStatus("idle")}
        >
          추가 제보하기
        </button>
      </div>
    );
  }

  return (
    <form className="rounded-xl border border-border bg-white p-5 md:p-8" onSubmit={handleSubmit}>
      <div className="grid gap-5">
        <label className="grid gap-2">
          <span className="text-sm font-semibold text-neutral-800">제보 유형</span>
          <select
            className="h-12 rounded-lg border border-border bg-white px-4 text-sm outline-none focus:border-accent"
            value={reportType}
            onChange={(event) => setReportType(event.target.value as ReportType)}
            required
          >
            {reportTypeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="grid gap-2">
          <span className="text-sm font-semibold text-neutral-800">브랜드 선택</span>
          <select
            className="h-12 rounded-lg border border-border bg-white px-4 text-sm outline-none focus:border-accent"
            value={brandId}
            onChange={(event) => setBrandId(event.target.value)}
          >
            <option value="">브랜드를 선택하지 않음</option>
            {brands.map((brand) => (
              <option key={brand.id} value={brand.id}>
                {brand.name}
              </option>
            ))}
          </select>
          {selectedBrand ? (
            <span className="text-xs text-neutral-500">{selectedBrand.categoryName} 브랜드로 제보합니다.</span>
          ) : null}
        </label>

        {initialBenefitId ? (
          <input type="hidden" name="benefitId" value={initialBenefitId} />
        ) : null}

        <label className="grid gap-2">
          <span className="text-sm font-semibold text-neutral-800">제보 내용</span>
          <textarea
            className="min-h-36 rounded-lg border border-border px-4 py-3 text-sm leading-6 outline-none focus:border-accent"
            value={content}
            onChange={(event) => setContent(event.target.value)}
            minLength={5}
            maxLength={2000}
            required
            placeholder="달라진 조건, 종료된 혜택, 새 공식 링크 등을 알려주세요."
          />
        </label>

        <label className="grid gap-2">
          <span className="text-sm font-semibold text-neutral-800">참고 링크</span>
          <input
            className="h-12 rounded-lg border border-border px-4 text-sm outline-none focus:border-accent"
            value={referenceUrl}
            onChange={(event) => setReferenceUrl(event.target.value)}
            maxLength={1000}
            placeholder="https://..."
          />
        </label>

        <label className="grid gap-2">
          <span className="text-sm font-semibold text-neutral-800">이메일</span>
          <input
            className="h-12 rounded-lg border border-border px-4 text-sm outline-none focus:border-accent"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            maxLength={255}
            placeholder="선택 입력"
          />
        </label>
      </div>

      {status === "error" ? (
        <p className="mt-5 rounded-lg bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          제보 접수에 실패했습니다. 잠시 후 다시 시도해 주세요.
        </p>
      ) : null}

      <button
        className="mt-6 h-12 w-full rounded-lg bg-accent px-5 text-sm font-semibold text-white disabled:opacity-60"
        type="submit"
        disabled={status === "submitting"}
      >
        {status === "submitting" ? "접수 중" : "제보하기"}
      </button>
    </form>
  );
}
