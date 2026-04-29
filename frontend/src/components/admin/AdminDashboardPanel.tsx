"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { getAdminBenefits, getAdminDashboard, getSourceWatches } from "@/lib/api/adminApi";
import type { AdminBenefit } from "@/types/adminBenefit";
import type { AdminDashboard, CollectionSummary } from "@/types/report";
import type { SourceWatch } from "@/types/sourceWatch";

type DashboardCard = {
  label: string;
  value: number;
  tone?: "default" | "warning" | "success";
};

const emptyCollectionSummary: CollectionSummary = {
  totalSourceWatchCount: 0,
  activeSourceWatchCount: 0,
  pendingCandidateCount: 0,
  recentSuccessRunCount: 0,
  recentFailedRunCount: 0,
  recentSkippedRunCount: 0,
  recentFailedRuns: [],
};

function DashboardMetricCard({ label, value, tone = "default" }: DashboardCard) {
  const toneClass = {
    default: "text-neutral-950",
    warning: "text-amber-700",
    success: "text-green-700",
  }[tone];

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
      <p className="text-sm font-medium text-neutral-500">{label}</p>
      <p className={`mt-3 text-3xl font-bold ${toneClass}`}>{Number(value ?? 0).toLocaleString()}</p>
    </div>
  );
}

function formatDateTime(value?: string | null) {
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

export function AdminDashboardPanel() {
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [publishedBenefits, setPublishedBenefits] = useState<AdminBenefit[]>([]);
  const [sourceWatches, setSourceWatches] = useState<SourceWatch[]>([]);
  const [publishedKeyword, setPublishedKeyword] = useState("");
  const debouncedPublishedKeyword = useDebouncedValue(publishedKeyword, 300);
  const [loading, setLoading] = useState(true);
  const [publishedLoading, setPublishedLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadDashboard() {
      setLoading(true);
      setError(null);

      try {
        const data = await getAdminDashboard();
        const watches = await getSourceWatches();
        if (active) {
          setDashboard(data);
          setSourceWatches(watches ?? []);
        }
      } catch {
        if (active) {
          setError("대시보드 정보를 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadDashboard();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function loadPublishedBenefits() {
      setPublishedLoading(true);
      try {
        const data = await getAdminBenefits({
          verificationStatus: "PUBLISHED",
          isActive: true,
          keyword: debouncedPublishedKeyword.trim() || undefined,
          limit: debouncedPublishedKeyword.trim() ? 20 : 5,
        });
        if (active) {
          setPublishedBenefits(data);
        }
      } catch {
        if (active) {
          setError("대시보드 정보를 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setPublishedLoading(false);
        }
      }
    }

    loadPublishedBenefits();

    return () => {
      active = false;
    };
  }, [debouncedPublishedKeyword]);

  const collectionSummary = dashboard?.collectionSummary ?? emptyCollectionSummary;
  const recentFailedRuns = collectionSummary.recentFailedRuns ?? [];
  const filteredPublishedBenefits = useMemo(() => publishedBenefits, [publishedBenefits]);
  const manualReviewSources = sourceWatches.filter((source) =>
    ["BLOCKED_BY_ROBOTS", "BLOCKED_BY_TERMS", "LOGIN_REQUIRED", "UNKNOWN_NEEDS_REVIEW", "MANUAL_REVIEW_ONLY"].includes(source.collectionPermissionStatus)
  );
  const termsReviewSources = sourceWatches.filter((source) => source.termsCheckStatus === "NOT_CHECKED" || source.termsCheckStatus === "NEEDS_REVIEW");
  const stalePublishedBenefits = publishedBenefits.filter((benefit) => {
    if (!benefit.lastVerifiedAt) {
      return true;
    }
    const checkedAt = new Date(benefit.lastVerifiedAt);
    const threshold = new Date();
    threshold.setDate(threshold.getDate() - 30);
    return checkedAt <= threshold;
  });

  const operationCards: DashboardCard[] = [
    { label: "전체 수집 URL", value: collectionSummary.totalSourceWatchCount },
    { label: "활성 수집 URL", value: collectionSummary.activeSourceWatchCount },
    { label: "검수 대기 후보", value: collectionSummary.pendingCandidateCount, tone: "warning" },
    { label: "최근 성공", value: collectionSummary.recentSuccessRunCount, tone: "success" },
    { label: "최근 실패", value: collectionSummary.recentFailedRunCount, tone: "warning" },
    { label: "최근 스킵", value: collectionSummary.recentSkippedRunCount },
  ];

  return (
    <div className="space-y-8">
      {loading ? (
        <section className="rounded-2xl border border-neutral-200 bg-white p-5 text-sm text-neutral-600 shadow-sm">
          대시보드를 불러오는 중입니다.
        </section>
      ) : null}

      {error ? (
        <section className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
          {error}
        </section>
      ) : null}

      <section>
        <div className="mb-4">
          <h2 className="text-lg font-bold text-neutral-950">수집 운영 현황</h2>
          <p className="mt-1 text-sm text-neutral-500">공식 출처 수집과 후보 검수 상태를 빠르게 확인합니다.</p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
          {operationCards.map((card) => (
            <DashboardMetricCard key={card.label} {...card} />
          ))}
        </div>
      </section>

      <section className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-bold">빠른 작업</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <QuickAction href="/admin/benefit-candidates" title="새 혜택 검수" description="아직 사용자에게 보이지 않는 새 수집 후보를 검수합니다." />
          <QuickAction href="/admin/benefits/new" title="새 혜택 직접 등록" description="자동 수집 없이 공식 출처를 직접 확인한 혜택을 등록합니다." />
          <QuickAction href="/admin/benefits" title="공개 혜택 수정" description="사용자 화면에 보이는 혜택 문구, 대표 혜택, 공개 상태를 바로 수정합니다." />
          <QuickAction href="/admin/source-watches" title="공식 출처 수집 관리" description="수집 URL을 등록하고 수집 실행을 관리하세요." />
        </div>
      </section>

      <section className="grid gap-5 xl:grid-cols-3">
        <DashboardList
          empty="수동 검수 필요 출처가 없습니다."
          items={manualReviewSources.slice(0, 5).map((source) => ({
            id: source.id,
            title: source.title,
            meta: `${source.brandName} · ${source.collectionPermissionStatus}`,
            href: `/admin/source-watches?sourceWatchId=${source.id}`,
            externalHref: source.url,
            benefitHref: `/admin/benefits?keyword=${encodeURIComponent(source.brandName)}`,
          }))}
          title="수동 검수 필요 출처"
        />
        <DashboardList
          empty="정책 확인 필요 출처가 없습니다."
          items={termsReviewSources.slice(0, 5).map((source) => ({
            id: source.id,
            title: source.title,
            meta: `${source.brandName} · termsCheckStatus=${source.termsCheckStatus}`,
            href: `/admin/source-watches?sourceWatchId=${source.id}`,
            externalHref: source.url,
            benefitHref: `/admin/benefits?keyword=${encodeURIComponent(source.brandName)}`,
          }))}
          title="정책 확인 필요 출처"
        />
        <DashboardList
          empty="재확인 필요 공개 혜택이 없습니다."
          items={stalePublishedBenefits.slice(0, 5).map((benefit) => ({
            id: benefit.id,
            title: benefit.title,
            meta: `${benefit.brandName} · 최근 확인일 ${benefit.lastVerifiedAt ?? "없음"}`,
            href: `/admin/benefits/${benefit.id}`,
          }))}
          title="재확인 필요 혜택"
        />
      </section>

      <section className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-bold">공개 혜택 바로 수정</h2>
            <p className="mt-1 text-sm text-neutral-500">사용자 화면에 노출 중인 혜택을 빠르게 찾아 수정합니다.</p>
          </div>
          <input
            className="h-11 w-full max-w-sm rounded-lg border border-neutral-200 bg-white px-3 text-sm"
            value={publishedKeyword}
            onChange={(event) => setPublishedKeyword(event.target.value)}
            placeholder="브랜드명 또는 혜택명 검색"
          />
        </div>
        {publishedBenefits.length === 0 ? (
          publishedLoading ? (
            <p className="mt-4 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">공개 혜택을 불러오는 중입니다.</p>
          ) : (
          <p className="mt-4 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">
            아직 공개 중인 혜택이 없습니다. 후보 검수에서 혜택을 승인하고 공개 전환해 주세요.
          </p>
          )
        ) : filteredPublishedBenefits.length === 0 ? (
          <p className="mt-4 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">검색 결과가 없습니다.</p>
        ) : (
          <div className="mt-4 space-y-3">
            {filteredPublishedBenefits.map((benefit) => {
              const activeDetailItemCount = (benefit.detailItems ?? []).filter((item) => item.isActive).length;
              return (
                <article key={benefit.id} className="rounded-xl border border-neutral-200 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-neutral-500">{benefit.brandName}</p>
                      <h3 className="mt-1 text-base font-bold text-neutral-950">{benefit.title}</h3>
                      <p className="mt-2 text-sm text-neutral-600">
                        공개 중 · 활성 · 대표 혜택 {activeDetailItemCount}개 · 최근 확인일 {benefit.lastVerifiedAt ?? "-"}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Link
                        className="h-9 rounded-lg border border-neutral-200 px-3 py-2 text-sm font-semibold"
                        href={`/brands/${benefit.brandSlug}`}
                        target="_blank"
                      >
                        사용자 화면 보기
                      </Link>
                      <Link className="h-9 rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white" href={`/admin/benefits/${benefit.id}`}>
                        수정하기
                      </Link>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-bold">오늘 확인할 항목</h2>
          {collectionSummary.pendingCandidateCount > 0 ? (
            <p className="mt-3 text-sm leading-6 text-neutral-700">
              검수 대기 후보 {collectionSummary.pendingCandidateCount.toLocaleString()}개가 있습니다.
              <Link className="ml-2 font-semibold text-blue-600 hover:underline" href="/admin/benefit-candidates">
                검수하러 가기
              </Link>
            </p>
          ) : (
            <p className="mt-3 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">
              오늘 확인할 검수 대기 후보가 없습니다.
            </p>
          )}
        </div>

        <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-bold">최근 수집 실행 현황</h2>
          {recentFailedRuns.length > 0 ? (
            <div className="mt-3 overflow-hidden rounded-lg border border-neutral-200">
              {recentFailedRuns.slice(0, 4).map((run) => (
                <div key={run.runId} className="grid gap-2 border-b border-neutral-100 p-3 text-sm last:border-b-0 md:grid-cols-[1fr_auto]">
                  <div>
                    <p className="font-semibold">{run.sourceWatchTitle ?? "-"}</p>
                    <p className="text-xs text-neutral-500">
                      {run.brandName ?? "-"} · {formatDateTime(run.startedAt)}
                    </p>
                  </div>
                  <span className="w-fit rounded-full bg-red-50 px-2 py-1 text-xs font-semibold text-red-700">
                    {run.failureReason ?? "실패"}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="mt-3 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">
              최근 수집 실행 이력이 없습니다.
            </p>
          )}
        </div>
      </section>
    </div>
  );
}

function QuickAction({ description, href, title }: { description: string; href: string; title: string }) {
  return (
    <Link className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm hover:border-blue-300 hover:bg-blue-50" href={href}>
      <h3 className="text-base font-bold text-neutral-950">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-neutral-600">{description}</p>
    </Link>
  );
}

function DashboardList({
  empty,
  items,
  title,
}: {
  empty: string;
  items: { id: number; title: string; meta: string; href: string; externalHref?: string; benefitHref?: string }[];
  title: string;
}) {
  return (
    <section className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-bold">{title}</h2>
      {items.length === 0 ? (
        <p className="mt-3 rounded-lg bg-neutral-50 p-4 text-sm text-neutral-600">{empty}</p>
      ) : (
        <div className="mt-3 space-y-3">
          {items.map((item) => (
            <article key={item.id} className="rounded-xl border border-neutral-200 p-4">
              <h3 className="text-sm font-bold text-neutral-950">{item.title}</h3>
              <p className="mt-1 text-xs text-neutral-500">{item.meta}</p>
              <div className="mt-3 flex flex-wrap gap-2">
                {item.externalHref ? (
                  <a className="rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-semibold" href={item.externalHref} target="_blank" rel="noreferrer">
                    공식 페이지 열기
                  </a>
                ) : null}
                <Link className="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white" href={item.href}>
                  관리로 이동
                </Link>
                {item.benefitHref ? (
                  <Link className="rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-semibold" href={item.benefitHref}>
                    공개 혜택 관리
                  </Link>
                ) : null}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
