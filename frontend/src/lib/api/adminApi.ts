import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type { AdminDashboard, AdminReport, ReportStatus, ReportType } from "@/types/report";

type AdminReportParams = {
  status?: ReportStatus;
  reportType?: ReportType;
};

type AdminReportStatusUpdateRequest = {
  status: ReportStatus;
  adminMemo?: string | null;
};

function compactParams(params?: Record<string, unknown>) {
  if (!params) {
    return undefined;
  }

  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== "")
  );
}

export async function getAdminDashboard() {
  const response = await apiClient.get<ApiResponse<AdminDashboard>>("/api/v1/admin/dashboard");
  return response.data.data;
}

export async function getAdminReports(params?: AdminReportParams) {
  const response = await apiClient.get<ApiResponse<AdminReport[]>>("/api/v1/admin/reports", {
    params: compactParams(params),
  });
  return response.data.data;
}

export async function updateAdminReportStatus(
  reportId: number,
  request: AdminReportStatusUpdateRequest
) {
  const response = await apiClient.patch<ApiResponse<AdminReport>>(
    `/api/v1/admin/reports/${reportId}/status`,
    request
  );
  return response.data.data;
}
