"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  approveBenefitCandidate,
  getBenefitCandidate,
  getSourceWatches,
  updateBenefitCandidateStatus,
} from "@/lib/api/adminApi";
import { CANDIDATE_STATUS_CLASS, CANDIDATE_STATUS_LABELS } from "@/lib/adminLabels";
import {
  BENEFIT_TYPE_LABELS,
  BENEFIT_TYPE_OPTIONS,
  BIRTHDAY_TIMING_LABELS,
  BIRTHDAY_TIMING_OPTIONS,
  OCCASION_TYPE_LABELS,
  OCCASION_TYPE_OPTIONS,
} from "@/lib/benefitLabels";
import type {
  BenefitCandidate,
  BenefitCandidateApproveRequest,
  BenefitCandidateApproveResponse,
} from "@/types/benefitCandidate";
import type { BenefitType, BirthdayTimingType, OccasionType } from "@/types/adminBenefit";
import type { SourceWatch } from "@/types/sourceWatch";

type ApproveFormState = {
  title: string;
  summary: string;
  benefitType: BenefitType;
  occasionType: OccasionType;
  birthdayTimingType: BirthdayTimingType;
  birthdayTimingDescription: string;
  requiresApp: boolean;
  requiresSignup: boolean;
  requiresMembership: boolean;
  minimumPurchaseDescription: string;
  usageCondition: string;
  adminMemo: string;
  detailItems: DetailItemFormState[];
};

type DetailItemFormState = {
  brandName: string;
  title: string;
  description: string;
  conditionText: string;
  imageUrl: string;
  displayOrder: number;
};

type CouponImageSource = {
  coupon: string;
  imgSrc: string;
  imgAlt: string;
  imgTitle: string;
  imgAriaLabel: string;
  parentHref: string;
  parentTitle: string;
  parentAriaLabel: string;
  nearbyText: string;
  possibleBrandName: string;
  confidence: string;
};

const reviewChecklistLabels = [
  "공식 URL에서 직접 확인했는가?",
  "robots.txt/수집 정책 상태를 확인했는가?",
  "약관상 자동 수집 또는 재배포 금지 여부를 확인했는가?",
  "원문 문장을 그대로 복사하지 않았는가?",
  "브랜드 로고, 쿠폰 이미지, 배너 이미지를 사용하지 않았는가?",
  "브랜드명, 혜택명, 핵심 조건 중심으로 짧게 요약했는가?",
  "최종 확인일을 입력했는가?",
  "공식 출처 URL을 입력했는가?",
  "\"공식\", \"제휴\", \"인증\", \"보장\", \"확정\" 같은 오인 표현을 쓰지 않았는가?",
  "사용 전 공식 앱/홈페이지 확인이 필요하다는 안내가 있는가?",
];

const prohibitedTerms = ["공식 쿠폰", "공식 제휴", "제휴 혜택", "인증 혜택", "보장", "확정", "무조건", "반드시 제공", "Zup 단독", "최신 보장", "100% 사용 가능"];

function safeText(value: string | null | undefined, fallback = "") {
  return value ?? fallback;
}

function truncate(value: string | null | undefined, maxLength: number) {
  const text = safeText(value);
  return text.length <= maxLength ? text : text.slice(0, maxLength);
}

function normalizeOptional(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function parseDetailItems(value?: string | null): DetailItemFormState[] {
  if (!value) {
    return [];
  }

  return value
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line, index) => {
        const conditionMatch = line.match(/^(.+?)\s*\((조건|사용 조건):\s*(.+)\)$/);
        return {
          brandName: "",
          title: conditionMatch ? conditionMatch[1].trim() : line,
          description: "",
          conditionText: conditionMatch ? conditionMatch[3].trim() : "",
          imageUrl: "",
          displayOrder: index + 1,
        };
      });
}

function parseCouponImageSources(value?: string | null): CouponImageSource[] {
  if (!value) {
    return [];
  }

  return value
      .split(/\n\s*\n/)
      .map((block) => {
        const lines = block.split("\n");
        const read = (label: string) => lines.find((line) => line.startsWith(`${label}: `))?.replace(`${label}: `, "").trim() ?? "";
        return {
          coupon: lines.find((line) => line.startsWith("쿠폰: "))?.replace("쿠폰: ", "").trim() ?? "",
          imgSrc: read("imgSrc"),
          imgAlt: read("imgAlt"),
          imgTitle: read("imgTitle"),
          imgAriaLabel: lines.find((line) => line.startsWith("imgAriaLabel: "))?.replace("imgAriaLabel: ", "").trim() ?? "",
          parentHref: lines.find((line) => line.startsWith("parentHref: "))?.replace("parentHref: ", "").trim() ?? "",
          parentTitle: lines.find((line) => line.startsWith("parentTitle: "))?.replace("parentTitle: ", "").trim() ?? "",
          parentAriaLabel: lines.find((line) => line.startsWith("parentAriaLabel: "))?.replace("parentAriaLabel: ", "").trim() ?? "",
          nearbyText: lines.find((line) => line.startsWith("nearbyText: "))?.replace("nearbyText: ", "").trim() ?? "",
          possibleBrandName: lines.find((line) => line.startsWith("possibleBrandName: "))?.replace("possibleBrandName: ", "").trim() ?? "",
          confidence: lines.find((line) => line.startsWith("confidence: "))?.replace("confidence: ", "").trim() ?? "",
        };
      })
      .filter((item) => item.imgSrc);
}

function toApproveForm(candidate: BenefitCandidate): ApproveFormState {
  const usageCondition = candidate.usageGuideText || candidate.evidenceText || "";

  return {
    title: safeText(candidate.title),
    summary: truncate(candidate.summary, 300),
    benefitType: candidate.benefitType ?? "COUPON",
    occasionType: candidate.occasionType ?? "BIRTHDAY",
    birthdayTimingType: candidate.birthdayTimingType ?? "UNKNOWN",
    birthdayTimingDescription: "",
    requiresApp: Boolean(candidate.requiresApp),
    requiresSignup: Boolean(candidate.requiresSignup),
    requiresMembership: Boolean(candidate.requiresMembership),
    minimumPurchaseDescription: "쿠폰별 최소 구매 금액과 사용 조건은 각 쿠폰 상세 안내를 확인해야 합니다.",
    usageCondition: truncate(usageCondition, 700),
    adminMemo: candidate.benefitDetailImageSources
        ? "일부 쿠폰의 브랜드명은 로고 이미지로 제공되어 imgSrc를 참고해 수동 검수해야 합니다."
        : "",
    detailItems: parseDetailItems(candidate.benefitDetailText),
  };
}

function toApproveRequest(form: ApproveFormState): BenefitCandidateApproveRequest {
  return {
    title: form.title.trim(),
    summary: form.summary.trim(),
    benefitType: form.benefitType,
    occasionType: form.occasionType,
    birthdayTimingType: form.birthdayTimingType,
    birthdayTimingDescription: normalizeOptional(form.birthdayTimingDescription),
    requiresApp: form.requiresApp,
    requiresSignup: form.requiresSignup,
    requiresMembership: form.requiresMembership,
    minimumPurchaseDescription: normalizeOptional(form.minimumPurchaseDescription),
    usageCondition: normalizeOptional(form.usageCondition),
    adminMemo: normalizeOptional(form.adminMemo),
    detailItems: form.detailItems
        .filter((item) => item.title.trim())
        .map((item, index) => ({
          brandName: normalizeOptional(item.brandName),
          title: item.title.trim(),
          description: normalizeOptional(item.description),
          conditionText: normalizeOptional(item.conditionText),
          imageUrl: normalizeOptional(item.imageUrl),
          displayOrder: index + 1,
        })),
  };
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function getErrorMessage(error: unknown, fallback: string) {
  if (typeof error === "object" && error !== null && "response" in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    return response?.data?.message ?? fallback;
  }

  return fallback;
}

export function AdminBenefitCandidateDetailPanel({ candidateId }: { candidateId: number }) {
  const [candidate, setCandidate] = useState<BenefitCandidate | null>(null);
  const [sourceWatches, setSourceWatches] = useState<SourceWatch[]>([]);
  const [approveForm, setApproveForm] = useState<ApproveFormState | null>(null);
  const [statusMemo, setStatusMemo] = useState("");
  const [loading, setLoading] = useState(true);
  const [savingStatus, setSavingStatus] = useState(false);
  const [approving, setApproving] = useState(false);
  const [approveResult, setApproveResult] = useState<BenefitCandidateApproveResponse | null>(null);
  const [reviewChecks, setReviewChecks] = useState<boolean[]>(() => reviewChecklistLabels.map(() => false));
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loadAll() {
    setLoading(true);
    setError(null);

    try {
      const [candidateData, sourceWatchData] = await Promise.all([
        getBenefitCandidate(candidateId),
        getSourceWatches(),
      ]);

      setCandidate(candidateData);
      setSourceWatches(sourceWatchData ?? []);
      setApproveForm(toApproveForm(candidateData));
    } catch (loadError) {
      setError(getErrorMessage(loadError, "혜택 후보 상세를 불러오지 못했습니다."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
  }, [candidateId]);

  const sourceWatch = useMemo(
      () => sourceWatches.find((item) => item.id === candidate?.sourceWatchId) ?? null,
      [candidate?.sourceWatchId, sourceWatches]
  );

  const approveText = approveForm ? `${approveForm.title}\n${approveForm.summary}\n${approveForm.usageCondition}\n${approveForm.adminMemo}` : "";
  const foundProhibitedTerms = prohibitedTerms.filter((term) => approveText.includes(term));
  const checklistComplete = reviewChecks.every(Boolean);
  const approveDisabled = !candidate || candidate.status === "REJECTED" || candidate.approvedBenefitId !== null || !checklistComplete || foundProhibitedTerms.length > 0;

  async function reloadCandidate() {
    const next = await getBenefitCandidate(candidateId);
    setCandidate(next);
    return next;
  }

  async function handleStatus(status: "NEEDS_REVIEW" | "REJECTED") {
    setSavingStatus(true);
    setMessage(null);
    setError(null);

    try {
      const updated = await updateBenefitCandidateStatus(candidateId, {
        status,
        reviewMemo: normalizeOptional(statusMemo),
      });

      setCandidate(updated);
      setMessage(`후보 상태가 ${CANDIDATE_STATUS_LABELS[status]}로 변경되었습니다.`);
    } catch (statusError) {
      setError(getErrorMessage(statusError, "후보 상태 변경에 실패했습니다."));
    } finally {
      setSavingStatus(false);
    }
  }

  async function handleApprove(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!approveForm) {
      return;
    }

    if (!approveForm.title.trim() || !approveForm.summary.trim()) {
      setError("승인할 혜택의 제목과 요약은 필수입니다.");
      return;
    }

    setApproving(true);
    setMessage(null);
    setError(null);

    try {
      const result = await approveBenefitCandidate(candidateId, toApproveRequest(approveForm));
      setApproveResult(result);
      setMessage(`Benefit 생성 완료: #${result.benefitId}`);
      await reloadCandidate();
    } catch (approveError) {
      setError(getErrorMessage(approveError, "Candidate 승인에 실패했습니다."));
    } finally {
      setApproving(false);
    }
  }

  function updateDetailItem(index: number, patch: Partial<DetailItemFormState>) {
    setApproveForm((current) => {
      if (!current) {
        return current;
      }

      return {
        ...current,
        detailItems: current.detailItems.map((item, itemIndex) =>
            itemIndex === index ? { ...item, ...patch } : item
        ),
      };
    });
  }

  function addDetailItem() {
    setApproveForm((current) =>
            current && {
              ...current,
              detailItems: [
                ...current.detailItems,
                {
                  brandName: "",
                  title: "",
                  description: "",
                  conditionText: "",
                  imageUrl: "",
                  displayOrder: current.detailItems.length + 1,
                },
              ],
            }
    );
  }

  function removeDetailItem(index: number) {
    setApproveForm((current) =>
            current && {
              ...current,
              detailItems: current.detailItems
                  .filter((_, itemIndex) => itemIndex !== index)
                  .map((item, itemIndex) => ({ ...item, displayOrder: itemIndex + 1 })),
            }
    );
  }

  function moveDetailItem(index: number, direction: -1 | 1) {
    setApproveForm((current) => {
      if (!current) {
        return current;
      }

      const nextIndex = index + direction;

      if (nextIndex < 0 || nextIndex >= current.detailItems.length) {
        return current;
      }

      const detailItems = [...current.detailItems];
      [detailItems[index], detailItems[nextIndex]] = [detailItems[nextIndex], detailItems[index]];

      return {
        ...current,
        detailItems: detailItems.map((item, itemIndex) => ({ ...item, displayOrder: itemIndex + 1 })),
      };
    });
  }

  if (loading) {
    return <EmptyBox>혜택 후보 상세를 불러오는 중입니다.</EmptyBox>;
  }

  if (!candidate || !approveForm) {
    return (
        <section className="w-full space-y-4">
          <Notice tone="error">{error ?? "혜택 후보 상세를 불러오지 못했습니다."}</Notice>
        </section>
    );
  }

  const benefitId = candidate.approvedBenefitId ?? approveResult?.benefitId ?? null;
  const couponImageSources = parseCouponImageSources(candidate.benefitDetailImageSources);

  const benefitLines = (candidate.benefitDetailText ?? "")
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean);

  const guideLines = (candidate.usageGuideText ?? "")
      .split(/\. |\n/)
      .map((line) => line.replace(/\.$/, "").trim())
      .filter(Boolean);
  const warningLines = (candidate.extractionWarnings ?? "")
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean);
  const excludedLines = (candidate.excludedTexts ?? "")
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean);

  return (
      <section className="w-full space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-3xl font-bold tracking-tight text-neutral-950">혜택 후보 검수 상세</h1>
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${CANDIDATE_STATUS_CLASS[candidate.status]}`}>
              {CANDIDATE_STATUS_LABELS[candidate.status]}
            </span>
              {candidate.needsManualReview ? (
                <span className="rounded-full bg-red-50 px-3 py-1 text-xs font-semibold text-red-700">
                  수동 재검토 필요
                </span>
              ) : null}
            </div>
            <p className="mt-2 text-sm text-neutral-600">
              수집 근거를 확인하고 공개 화면에 표시할 혜택 상세 리스트를 정리하세요.
            </p>
          </div>

          {benefitId ? (
              <Link className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white" href={`/admin/benefits/${benefitId}`}>
                생성된 혜택 관리로 이동
              </Link>
          ) : null}
        </div>

        {message ? <Notice tone="success">{message}</Notice> : null}
        {error ? <Notice tone="error">{error}</Notice> : null}
        {candidate.needsManualReview ? (
          <Notice tone="warning">
            이 후보는 현재 자동 수집이 차단된 출처에서 생성된 항목입니다. 공식 페이지를 직접 확인한 뒤 내용을 검수하거나 반려해 주세요.
          </Notice>
        ) : null}

        <div className="grid w-full gap-6 2xl:grid-cols-[minmax(0,1fr)_420px]">
          <div className="min-w-0 space-y-5">
            <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-bold text-neutral-950">출처 정보</h2>
              <dl className="mt-4 grid gap-4 text-sm lg:grid-cols-4">
                <Info label="수집 출처명" value={sourceWatch?.title ?? `#${candidate.sourceWatchId}`} />
              <Info label="브랜드" value={safeText(candidate.brandName, "-")} />
                <Info label="수집 URL" value={sourceWatch?.url ?? "-"} wide />
                <Info label="발견 일시" value={formatDateTime(candidate.createdAt)} />
              <Info label="신뢰도" value={Number(candidate.confidence ?? 0).toFixed(2)} />
              </dl>
            </section>

            <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-bold text-neutral-950">요약</h2>
            <p className="mt-3 max-w-5xl text-sm leading-6 text-neutral-700">{safeText(candidate.summary, "-")}</p>

              <div className="mt-5 grid gap-4 xl:grid-cols-2">
                <ListBox title="구체 혜택 추정" lines={benefitLines} empty="구체 혜택 추정 없음" />
                <ListBox title="이용안내 추정" lines={guideLines} empty="이용안내 추정 없음" />
              </div>
            </section>

            <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-bold text-neutral-950">추출 근거와 경고</h2>
              <dl className="mt-4 grid gap-4 text-sm lg:grid-cols-3">
                <Info label="후보 신뢰도" value={Number(candidate.confidence ?? 0).toFixed(2)} />
                <Info label="생일 문맥 근거" value={candidate.contextEvidence ?? "-"} wide />
              </dl>
              <div className="mt-5 grid gap-4 xl:grid-cols-2">
                <ListBox title="경고 목록" lines={warningLines} empty="추출 경고 없음" />
                <ListBox title="제외된 문구" lines={excludedLines} empty="제외된 문구 없음" />
              </div>
            </section>

            <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
              <details>
                <summary className="cursor-pointer text-lg font-bold text-neutral-950">근거 원문 펼쳐보기</summary>
                <pre className="mt-4 max-h-72 overflow-auto whitespace-pre-wrap rounded-lg bg-neutral-50 p-4 text-sm leading-6 text-neutral-700">
                {safeText(candidate.evidenceText, "-")}
              </pre>
              </details>
            </section>

            <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-bold text-neutral-950">쿠폰 이미지 소스 참고</h2>
              <p className="mt-1 text-sm text-neutral-500">
                이미지는 검수 참고용입니다. public 화면에는 외부 로고/쿠폰 이미지를 노출하지 않습니다. 브랜드명은 이미지 파일명만 보고 확정하지 마세요.
              </p>
              <div className="mt-3">
                <CouponImageSources sources={couponImageSources} />
              </div>
            </section>

            <DetailItemsEditor
                items={approveForm.detailItems}
                onAdd={addDetailItem}
                onMove={moveDetailItem}
                onRemove={removeDetailItem}
                onUpdate={updateDetailItem}
            />
          </div>

          <aside className="min-w-0 space-y-5 2xl:sticky 2xl:top-24 2xl:max-h-[calc(100vh-112px)] 2xl:self-start 2xl:overflow-y-auto">
            <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-bold text-neutral-950">승인 전 체크</h2>
              <p className="mt-2 text-sm leading-6 text-neutral-600">승인하지 않을 후보는 반려 처리하고 메모를 남기세요.</p>

              <TextInput label="검수 메모" value={statusMemo} onChange={setStatusMemo} />

              <div className="mt-3 grid grid-cols-2 gap-2">
                <button
                    className="h-10 rounded-lg border border-neutral-200 px-3 text-sm font-semibold disabled:opacity-60"
                    type="button"
                    disabled={savingStatus || candidate.status === "APPROVED"}
                    onClick={() => handleStatus("NEEDS_REVIEW")}
                >
                  검수 대기
                </button>
                <button
                    className="h-10 rounded-lg border border-red-200 bg-red-50 px-3 text-sm font-semibold text-red-700 disabled:opacity-60"
                    type="button"
                    disabled={savingStatus || candidate.status === "APPROVED"}
                    onClick={() => handleStatus("REJECTED")}
                >
                  반려
                </button>
              </div>
            </section>

            <form className="space-y-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm" onSubmit={handleApprove}>
              <div>
                <h2 className="text-lg font-bold text-neutral-950">혜택으로 승인</h2>
                <p className="mt-2 rounded-lg bg-amber-50 p-3 text-sm leading-6 text-amber-800">
                  승인된 후보는 검증 완료 상태의 혜택으로 생성됩니다. 공개 전환 전까지 사용자 화면에는 노출되지 않습니다.
                </p>
              </div>

              <div className="space-y-2 rounded-lg border border-neutral-200 bg-neutral-50 p-3">
                <p className="text-sm font-bold text-neutral-900">승인 전 필수 체크리스트</p>
                {reviewChecklistLabels.map((label, index) => (
                  <label key={label} className="flex gap-2 text-sm leading-5 text-neutral-700">
                    <input
                      checked={reviewChecks[index]}
                      onChange={(event) => setReviewChecks((checks) => checks.map((checked, itemIndex) => itemIndex === index ? event.target.checked : checked))}
                      type="checkbox"
                    />
                    <span>{label}</span>
                  </label>
                ))}
              </div>

              {foundProhibitedTerms.length > 0 ? (
                <p className="rounded-lg bg-red-50 p-3 text-sm leading-6 text-red-700">
                  공개 금지 표현이 포함되어 승인할 수 없습니다: {foundProhibitedTerms.join(", ")}
                </p>
              ) : null}

              <TextInput
                  label="제목"
                  value={approveForm.title}
                  onChange={(title) => setApproveForm((current) => current && { ...current, title })}
                  required
              />

              <TextArea
                  label="요약"
                  value={approveForm.summary}
                  onChange={(summary) => setApproveForm((current) => current && { ...current, summary })}
                  required
                  minHeightClassName="min-h-28"
              />

              <Select
                  label="혜택 유형"
                  value={approveForm.benefitType}
                  onChange={(benefitType) =>
                      setApproveForm((current) => current && { ...current, benefitType: benefitType as BenefitType })
                  }
              >
                {BENEFIT_TYPE_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {BENEFIT_TYPE_LABELS[option]}
                    </option>
                ))}
              </Select>

              <Select
                  label="적용 시점"
                  value={approveForm.occasionType}
                  onChange={(occasionType) =>
                      setApproveForm((current) => current && { ...current, occasionType: occasionType as OccasionType })
                  }
              >
                {OCCASION_TYPE_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {OCCASION_TYPE_LABELS[option]}
                    </option>
                ))}
              </Select>

              <Select
                  label="생일 지급 시점"
                  value={approveForm.birthdayTimingType}
                  onChange={(birthdayTimingType) =>
                      setApproveForm((current) => current && { ...current, birthdayTimingType: birthdayTimingType as BirthdayTimingType })
                  }
              >
                {BIRTHDAY_TIMING_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {BIRTHDAY_TIMING_LABELS[option]}
                    </option>
                ))}
              </Select>

              <TextInput
                  label="생일 지급 설명"
                  value={approveForm.birthdayTimingDescription}
                  onChange={(birthdayTimingDescription) =>
                      setApproveForm((current) => current && { ...current, birthdayTimingDescription })
                  }
              />

              <div className="space-y-2 rounded-lg bg-neutral-50 p-3">
                <Checkbox
                    label="앱 필요"
                    checked={approveForm.requiresApp}
                    onChange={(requiresApp) => setApproveForm((current) => current && { ...current, requiresApp })}
                />
                <Checkbox
                    label="회원가입 필요"
                    checked={approveForm.requiresSignup}
                    onChange={(requiresSignup) => setApproveForm((current) => current && { ...current, requiresSignup })}
                />
                <Checkbox
                    label="멤버십 필요"
                    checked={approveForm.requiresMembership}
                    onChange={(requiresMembership) => setApproveForm((current) => current && { ...current, requiresMembership })}
                />
              </div>

              <TextInput
                  label="구매 조건"
                  value={approveForm.minimumPurchaseDescription}
                  onChange={(minimumPurchaseDescription) =>
                      setApproveForm((current) => current && { ...current, minimumPurchaseDescription })
                  }
              />

              <TextArea
                  label="이용 조건"
                  value={approveForm.usageCondition}
                  onChange={(usageCondition) => setApproveForm((current) => current && { ...current, usageCondition })}
                  minHeightClassName="min-h-40"
              />

              <TextArea
                  label="검수 메모"
                  value={approveForm.adminMemo}
                  onChange={(adminMemo) => setApproveForm((current) => current && { ...current, adminMemo })}
                  minHeightClassName="min-h-28"
              />
              <p className="rounded-lg bg-neutral-50 p-3 text-xs leading-5 text-neutral-600">
                관리자 메모 예: 2026-04-29 공식 페이지에서 생일 쿠폰 문구 확인. 로고/이미지는 사용하지 않음. 원문 요약함.
              </p>

              <button
                  className="h-12 w-full rounded-lg bg-blue-600 px-4 text-sm font-bold text-white disabled:opacity-60"
                  type="submit"
                  disabled={approveDisabled || approving}
              >
                {approving ? "승인 중" : "혜택으로 승인"}
              </button>

              {approveDisabled ? (
                  <p className="text-sm text-neutral-500">이미 승인/반려되었거나 체크리스트 미완료, 금지어 포함 상태에서는 승인할 수 없습니다.</p>
              ) : null}
            </form>
          </aside>
        </div>
      </section>
  );
}

function CouponImageSources({ sources }: { sources: CouponImageSource[] }) {
  if (sources.length === 0) {
    return (
        <div className="space-y-1 rounded-lg border border-neutral-200 bg-neutral-50 p-3 text-sm text-neutral-600">
          <p className="font-medium text-neutral-800">이미지 메타데이터가 없습니다.</p>
          <p>
            현재 후보 스냅샷에 저장된 쿠폰 이미지 소스가 없습니다. 기존 스냅샷은 원본 HTML을 보관하지 않으므로
            후보 재생성만으로 새 이미지 소스를 복구할 수 없습니다.
          </p>
          <p>
            최신 HTML을 다시 수집했는데도 비어 있다면, 해당 사이트가 JavaScript 렌더링 후 로고 이미지를 삽입하거나
            alt/title 같은 접근성 단서를 제공하지 않는 구조일 수 있습니다.
          </p>
        </div>
    );
  }

  async function copy(src: string) {
    if (typeof navigator !== "undefined" && navigator.clipboard) {
      await navigator.clipboard.writeText(src);
    }
  }

  return (
      <details className="rounded-lg border border-neutral-200 bg-neutral-50 p-3 text-sm">
        <summary className="cursor-pointer font-semibold">쿠폰 이미지 소스 {sources.length}개</summary>
        <div className="mt-3 grid max-h-72 gap-3 overflow-y-auto pr-1 md:grid-cols-2">
          {sources.map((source, index) => (
              <div key={`${source.imgSrc}-${index}`} className="rounded-lg border border-neutral-200 bg-white p-3">
                <p className="font-medium text-neutral-800">혜택: {source.coupon || "-"}</p>
                <p className="mt-2 break-all text-neutral-700">
                  img src:{" "}
                  <a className="text-blue-600 underline" href={source.imgSrc} rel="noreferrer" target="_blank">
                    {source.imgSrc}
                  </a>
                </p>
                <p className="mt-1 text-neutral-700">alt: {source.imgAlt || "-"}</p>
                <p className="mt-1 text-neutral-700">title: {source.imgTitle || "-"}</p>
                <p className="mt-1 text-neutral-700">aria-label: {source.imgAriaLabel || "-"}</p>
                <p className="mt-1 break-all text-neutral-700">parent href: {source.parentHref || "-"}</p>
                <p className="mt-1 text-neutral-700">parent title: {source.parentTitle || source.parentAriaLabel || "-"}</p>
                <p className="mt-1 text-neutral-700">주변 텍스트: {source.nearbyText || "-"}</p>
                <p className="mt-1 text-neutral-700">추정 브랜드명: {source.possibleBrandName || "-"}</p>
                <p className="mt-1 text-neutral-700">신뢰도: {source.confidence || "-"}</p>
                <button
                    className="mt-2 rounded-lg border border-neutral-200 px-3 py-1 text-xs font-semibold"
                    type="button"
                    onClick={() => copy(source.imgSrc)}
                >
                  img src 복사
                </button>
              </div>
          ))}
        </div>
      </details>
  );
}

function ListBox({ empty, lines, title }: { empty: string; lines: string[]; title: string }) {
  return (
      <div className="rounded-xl border border-neutral-200 bg-neutral-50 p-4">
        <h3 className="text-sm font-bold text-neutral-800">{title}</h3>

        <div className="mt-3 max-h-72 overflow-y-auto pr-1">
          {lines.length > 0 ? (
              <ul className="space-y-2 text-sm leading-6 text-neutral-700">
                {lines.map((line, index) => (
                    <li key={`${line}-${index}`} className="flex gap-2">
                      <span className="shrink-0 text-neutral-400">-</span>
                      <span className="break-words">{line}</span>
                    </li>
                ))}
              </ul>
          ) : (
              <p className="text-sm text-neutral-500">{empty}</p>
          )}
        </div>
      </div>
  );
}

function DetailItemsEditor({
                             items,
                             onAdd,
                             onMove,
                             onRemove,
                             onUpdate,
                           }: {
  items: DetailItemFormState[];
  onAdd: () => void;
  onMove: (index: number, direction: -1 | 1) => void;
  onRemove: (index: number) => void;
  onUpdate: (index: number, patch: Partial<DetailItemFormState>) => void;
}) {
  return (
      <section className="rounded-2xl border-2 border-blue-100 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h3 className="text-lg font-bold text-neutral-950">혜택 상세 리스트</h3>
            <p className="mt-1 text-sm text-neutral-500">
              공개 화면에 표시될 대표 혜택 목록입니다. 외부 이미지 URL은 public에 노출하지 않으며, 브랜드명은 확실할 때만 입력하세요.
            </p>
          </div>
          <button className="rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white" type="button" onClick={onAdd}>
            항목 추가
          </button>
        </div>

        <div className="mt-4 space-y-3">
          {items.length === 0 ? (
              <div className="rounded-lg bg-neutral-50 p-4 text-sm text-neutral-500">등록된 상세 항목이 없습니다.</div>
          ) : null}

          {items.map((item, index) => (
              <div key={`${item.displayOrder}-${index}`} className="rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                  <span className="text-sm font-semibold">{index + 1}번 혜택</span>
                  <div className="flex gap-2">
                    <button
                        className="rounded-lg border border-neutral-200 px-2 py-1 text-xs disabled:opacity-50"
                        type="button"
                        onClick={() => onMove(index, -1)}
                        disabled={index === 0}
                    >
                      위로
                    </button>
                    <button
                        className="rounded-lg border border-neutral-200 px-2 py-1 text-xs disabled:opacity-50"
                        type="button"
                        onClick={() => onMove(index, 1)}
                        disabled={index === items.length - 1}
                    >
                      아래로
                    </button>
                    <button className="rounded-lg border border-neutral-200 px-2 py-1 text-xs" type="button" onClick={() => onRemove(index)}>
                      삭제
                    </button>
                  </div>
                </div>

                <div className="grid gap-3 md:grid-cols-2">
                  <TextInput label="브랜드명" value={item.brandName} onChange={(brandName) => onUpdate(index, { brandName })} />
                  <TextInput label="혜택명/내용" value={item.title} onChange={(title) => onUpdate(index, { title })} required />
                  <TextArea label="추가 설명" value={item.description} onChange={(description) => onUpdate(index, { description })} minHeightClassName="min-h-20" />
                  <TextArea label="사용 조건" value={item.conditionText} onChange={(conditionText) => onUpdate(index, { conditionText })} minHeightClassName="min-h-20" />
                  <div className="md:col-span-2">
                    <TextInput label="이미지 URL" value={item.imageUrl} onChange={(imageUrl) => onUpdate(index, { imageUrl })} />
                  </div>
                </div>
              </div>
          ))}
        </div>
      </section>
  );
}

function Info({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
      <div className={wide ? "lg:col-span-2" : undefined}>
        <dt className="font-medium text-neutral-500">{label}</dt>
        <dd className="mt-1 whitespace-pre-wrap break-words text-neutral-800">{value}</dd>
      </div>
  );
}

function Select({
                  children,
                  label,
                  onChange,
                  value,
                }: {
  children: React.ReactNode;
  label: string;
  onChange: (value: string) => void;
  value: string;
}) {
  return (
      <label className="block space-y-2 text-sm font-medium">
        <span>{label}</span>
        <select
            className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm"
            value={value}
            onChange={(event) => onChange(event.target.value)}
        >
          {children}
        </select>
      </label>
  );
}

function TextInput({
                     label,
                     onChange,
                     required,
                     value,
                   }: {
  label: string;
  onChange: (value: string) => void;
  required?: boolean;
  value: string;
}) {
  return (
      <label className="block space-y-2 text-sm font-medium">
        <span>{label}</span>
        <input
            className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm"
            value={value}
            onChange={(event) => onChange(event.target.value)}
            required={required}
        />
      </label>
  );
}

function TextArea({
                    label,
                    minHeightClassName = "min-h-24",
                    onChange,
                    required,
                    value,
                  }: {
  label: string;
  minHeightClassName?: string;
  onChange: (value: string) => void;
  required?: boolean;
  value: string;
}) {
  return (
      <label className="block space-y-2 text-sm font-medium">
        <span>{label}</span>
        <textarea
            className={`${minHeightClassName} w-full rounded-lg border border-neutral-200 bg-white px-3 py-2 text-sm leading-6`}
            value={value}
            onChange={(event) => onChange(event.target.value)}
            required={required}
        />
      </label>
  );
}

function Checkbox({ checked, label, onChange }: { checked: boolean; label: string; onChange: (value: boolean) => void }) {
  return (
      <label className="flex items-center gap-2 text-sm font-medium">
        <input checked={checked} onChange={(event) => onChange(event.target.checked)} type="checkbox" />
        {label}
      </label>
  );
}

function EmptyBox({ children }: { children: React.ReactNode }) {
  return <div className="rounded-2xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-600 shadow-sm">{children}</div>;
}

function Notice({ children, tone }: { children: React.ReactNode; tone: "success" | "error" | "warning" }) {
  const className = tone === "success"
      ? "border-green-200 bg-green-50 text-green-700"
      : tone === "warning"
        ? "border-amber-200 bg-amber-50 text-amber-800"
        : "border-red-200 bg-red-50 text-red-700";

  return <div className={`rounded-2xl border p-4 text-sm ${className}`}>{children}</div>;
}
