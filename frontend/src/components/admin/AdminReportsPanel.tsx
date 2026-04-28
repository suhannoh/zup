"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  REPORT_STATUS_LABELS,
  REPORT_STATUS_OPTIONS,
  REPORT_TYPE_LABELS,
  REPORT_TYPE_OPTIONS,
} from "@/lib/reportLabels";
import { getAdminReports, updateAdminReportStatus } from "@/lib/api/adminApi";
import type { AdminReport, ReportStatus, ReportType } from "@/types/report";

type FilterValue<T extends string> = T | "ALL";

type StatusFormState = {
  status: ReportStatus;
  adminMemo: string;
};

const statusBadgeClass: Record<ReportStatus, string> = {
  RECEIVED: "bg-blue-50 text-blue-700",
  REVIEWING: "bg-amber-50 text-amber-700",
  RESOLVED: "bg-green-50 text-green-700",
  REJECTED: "bg-neutral-100 text-neutral-700",
};

function formatDate(value: string | null) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function buildFormState(report: AdminReport): StatusFormState {
  return {
    status: report.status,
    adminMemo: report.adminMemo ?? "",
  };
}

export function AdminReportsPanel() {
  const [statusFilter, setStatusFilter] = useState<FilterValue<ReportStatus>>("ALL");
  const [typeFilter, setTypeFilter] = useState<FilterValue<ReportType>>("ALL");
  const [reports, setReports] = useState<AdminReport[]>([]);
  const [formStateById, setFormStateById] = useState<Record<number, StatusFormState>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const params = useMemo(
    () => ({
      status: statusFilter === "ALL" ? undefined : statusFilter,
      reportType: typeFilter === "ALL" ? undefined : typeFilter,
    }),
    [statusFilter, typeFilter]
  );

  useEffect(() => {
    let active = true;

    async function loadReports() {
      setLoading(true);
      setError(null);
      setMessage(null);

      try {
        const data = await getAdminReports(params);
        if (!active) {
          return;
        }
        setReports(data);
        setFormStateById(
          Object.fromEntries(data.map((report) => [report.id, buildFormState(report)]))
        );
      } catch {
        if (active) {
          setError("제보 목록을 불러오지 못했습니다.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadReports();

    return () => {
      active = false;
    };
  }, [params]);

  function updateFormState(reportId: number, nextState: Partial<StatusFormState>) {
    setFormStateById((current) => ({
      ...current,
      [reportId]: {
        ...current[reportId],
        ...nextState,
      },
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>, reportId: number) {
    event.preventDefault();
    const currentForm = formStateById[reportId];

    if (!currentForm) {
      return;
    }

    setSavingId(reportId);
    setMessage(null);
    setError(null);

    try {
      const updatedReport = await updateAdminReportStatus(reportId, {
        status: currentForm.status,
        adminMemo: currentForm.adminMemo.trim() || null,
      });

      setReports((current) =>
        current.map((report) => (report.id === updatedReport.id ? updatedReport : report))
      );
      setFormStateById((current) => ({
        ...current,
        [updatedReport.id]: buildFormState(updatedReport),
      }));
      setMessage("제보 상태를 저장했습니다.");
    } catch {
      setError("상태 변경에 실패했습니다.");
    } finally {
      setSavingId(null);
    }
  }

  return (
    <section className="space-y-5">
      <div className="rounded-lg border border-border bg-white p-4">
        <div className="grid gap-3 md:grid-cols-2">
          <label className="space-y-2 text-sm font-medium">
            <span>상태</span>
            <select
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as FilterValue<ReportStatus>)}
            >
              <option value="ALL">전체</option>
              {REPORT_STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {REPORT_STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </label>

          <label className="space-y-2 text-sm font-medium">
            <span>유형</span>
            <select
              className="h-11 w-full rounded-lg border border-border bg-white px-3 text-sm"
              value={typeFilter}
              onChange={(event) => setTypeFilter(event.target.value as FilterValue<ReportType>)}
            >
              <option value="ALL">전체</option>
              {REPORT_TYPE_OPTIONS.map((reportType) => (
                <option key={reportType} value={reportType}>
                  {REPORT_TYPE_LABELS[reportType]}
                </option>
              ))}
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

      {loading ? (
        <div className="rounded-lg border border-border bg-white p-8 text-center text-sm text-neutral-600">
          제보를 불러오는 중입니다.
        </div>
      ) : null}

      {!loading && reports.length === 0 && !error ? (
        <div className="rounded-lg border border-border bg-white p-8 text-center text-sm text-neutral-600">
          아직 접수된 제보가 없습니다.
        </div>
      ) : null}

      <div className="space-y-4">
        {reports.map((report) => {
          const currentForm = formStateById[report.id] ?? buildFormState(report);

          return (
            <article key={report.id} className="rounded-lg border border-border bg-white p-5">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                  {REPORT_TYPE_LABELS[report.reportType]}
                </span>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusBadgeClass[report.status]}`}>
                  {REPORT_STATUS_LABELS[report.status]}
                </span>
              </div>

              <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_280px]">
                <div className="space-y-4">
                  <div>
                    <p className="text-sm font-semibold">
                      {report.brandName ?? "브랜드 미지정"}
                    </p>
                    <p className="mt-1 text-sm text-neutral-600">
                      {report.benefitTitle ?? "혜택 미지정"}
                    </p>
                  </div>

                  <p className="whitespace-pre-wrap rounded-lg bg-neutral-50 p-4 text-sm leading-6 text-neutral-800">
                    {report.content}
                  </p>

                  <dl className="grid gap-3 text-sm sm:grid-cols-2">
                    <div>
                      <dt className="font-medium text-neutral-500">참고 링크</dt>
                      <dd className="mt-1 break-all">
                        {report.referenceUrl ? (
                          <a
                            className="text-accent hover:underline"
                            href={report.referenceUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            {report.referenceUrl}
                          </a>
                        ) : (
                          "-"
                        )}
                      </dd>
                    </div>
                    <div>
                      <dt className="font-medium text-neutral-500">이메일</dt>
                      <dd className="mt-1">{report.email ?? "-"}</dd>
                    </div>
                    <div>
                      <dt className="font-medium text-neutral-500">접수일</dt>
                      <dd className="mt-1">{formatDate(report.createdAt)}</dd>
                    </div>
                    <div>
                      <dt className="font-medium text-neutral-500">처리일</dt>
                      <dd className="mt-1">{formatDate(report.resolvedAt)}</dd>
                    </div>
                    <div className="sm:col-span-2">
                      <dt className="font-medium text-neutral-500">관리자 메모</dt>
                      <dd className="mt-1 whitespace-pre-wrap text-neutral-800">
                        {report.adminMemo || "-"}
                      </dd>
                    </div>
                  </dl>
                </div>

                <form className="space-y-3 rounded-lg border border-border p-4" onSubmit={(event) => handleSubmit(event, report.id)}>
                  <label className="space-y-2 text-sm font-medium">
                    <span>처리 상태</span>
                    <select
                      className="h-10 w-full rounded-lg border border-border bg-white px-3 text-sm"
                      value={currentForm.status}
                      onChange={(event) =>
                        updateFormState(report.id, { status: event.target.value as ReportStatus })
                      }
                    >
                      {REPORT_STATUS_OPTIONS.map((status) => (
                        <option key={status} value={status}>
                          {REPORT_STATUS_LABELS[status]}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="space-y-2 text-sm font-medium">
                    <span>관리자 메모</span>
                    <textarea
                      className="min-h-28 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm leading-6"
                      value={currentForm.adminMemo}
                      onChange={(event) => updateFormState(report.id, { adminMemo: event.target.value })}
                      placeholder="공식 페이지 확인 후 반영 완료"
                    />
                  </label>

                  <button
                    className="h-10 w-full rounded-lg bg-accent px-4 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
                    type="submit"
                    disabled={savingId === report.id}
                  >
                    {savingId === report.id ? "저장 중" : "저장"}
                  </button>
                </form>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
