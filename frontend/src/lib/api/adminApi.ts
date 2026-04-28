import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type {
  AdminBenefit,
  AdminBenefitCreateRequest,
  AdminBenefitSearchParams,
  AdminBenefitUpdateRequest,
  VerificationStatus,
} from "@/types/adminBenefit";
import type {
  AdminBenefitSource,
  AdminBenefitSourceCreateRequest,
  AdminBenefitSourceUpdateRequest,
} from "@/types/adminBenefitSource";
import type {
  AdminBrand,
  AdminBrandCreateRequest,
  AdminBrandSearchParams,
  AdminBrandUpdateRequest,
} from "@/types/adminBrand";
import type { AdminDashboard, AdminReport, ReportStatus, ReportType } from "@/types/report";
import type { VerificationLog } from "@/types/verificationLog";

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

export async function getAdminBrands(params?: AdminBrandSearchParams) {
  const response = await apiClient.get<ApiResponse<AdminBrand[]>>("/api/v1/admin/brands", {
    params: compactParams(params),
  });
  return response.data.data;
}

export async function createAdminBrand(request: AdminBrandCreateRequest) {
  const response = await apiClient.post<ApiResponse<AdminBrand>>("/api/v1/admin/brands", request);
  return response.data.data;
}

export async function updateAdminBrand(brandId: number, request: AdminBrandUpdateRequest) {
  const response = await apiClient.patch<ApiResponse<AdminBrand>>(
    `/api/v1/admin/brands/${brandId}`,
    request
  );
  return response.data.data;
}

export async function updateAdminBrandActive(brandId: number, request: { isActive: boolean }) {
  const response = await apiClient.patch<ApiResponse<AdminBrand>>(
    `/api/v1/admin/brands/${brandId}/active`,
    request
  );
  return response.data.data;
}

export async function getAdminBenefits(params?: AdminBenefitSearchParams) {
  const response = await apiClient.get<ApiResponse<AdminBenefit[]>>("/api/v1/admin/benefits", {
    params: compactParams(params),
  });
  return response.data.data;
}

export async function getAdminBenefit(benefitId: number) {
  const response = await apiClient.get<ApiResponse<AdminBenefit>>(`/api/v1/admin/benefits/${benefitId}`);
  return response.data.data;
}

export async function createAdminBenefit(request: AdminBenefitCreateRequest) {
  const response = await apiClient.post<ApiResponse<AdminBenefit>>("/api/v1/admin/benefits", request);
  return response.data.data;
}

export async function updateAdminBenefit(benefitId: number, request: AdminBenefitUpdateRequest) {
  const response = await apiClient.patch<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}`,
    request
  );
  return response.data.data;
}

export async function updateAdminBenefitStatus(
  benefitId: number,
  request: {
    verificationStatus: VerificationStatus;
    lastVerifiedAt?: string | null;
    memo?: string | null;
  }
) {
  const response = await apiClient.patch<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}/status`,
    request
  );
  return response.data.data;
}

export async function updateAdminBenefitActive(benefitId: number, request: { isActive: boolean }) {
  const response = await apiClient.patch<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}/active`,
    request
  );
  return response.data.data;
}

export async function getAdminBenefitSources(benefitId: number) {
  const response = await apiClient.get<ApiResponse<AdminBenefitSource[]>>(
    `/api/v1/admin/benefits/${benefitId}/sources`
  );
  return response.data.data;
}

export async function createAdminBenefitSource(
  benefitId: number,
  request: AdminBenefitSourceCreateRequest
) {
  const response = await apiClient.post<ApiResponse<AdminBenefitSource>>(
    `/api/v1/admin/benefits/${benefitId}/sources`,
    request
  );
  return response.data.data;
}

export async function updateAdminBenefitSource(
  sourceId: number,
  request: AdminBenefitSourceUpdateRequest
) {
  const response = await apiClient.patch<ApiResponse<AdminBenefitSource>>(
    `/api/v1/admin/sources/${sourceId}`,
    request
  );
  return response.data.data;
}

export async function deleteAdminBenefitSource(sourceId: number) {
  const response = await apiClient.patch<ApiResponse<AdminBenefitSource>>(
    `/api/v1/admin/sources/${sourceId}/delete`
  );
  return response.data.data;
}

export async function addAdminBenefitTag(benefitId: number, request: { tagId: number }) {
  const response = await apiClient.post<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}/tags`,
    request
  );
  return response.data.data;
}

export async function deleteAdminBenefitTag(benefitId: number, tagId: number) {
  const response = await apiClient.delete<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}/tags/${tagId}`
  );
  return response.data.data;
}

export async function getAdminBenefitVerificationLogs(benefitId: number) {
  const response = await apiClient.get<ApiResponse<VerificationLog[]>>(
    `/api/v1/admin/benefits/${benefitId}/verification-logs`
  );
  return response.data.data;
}

export async function getRecentVerificationLogs(limit = 20) {
  const response = await apiClient.get<ApiResponse<VerificationLog[]>>(
    "/api/v1/admin/verification-logs/recent",
    { params: compactParams({ limit }) }
  );
  return response.data.data;
}
