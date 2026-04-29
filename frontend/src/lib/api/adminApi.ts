import axios from "axios";
import { apiClient } from "./client";
import { clearAdminAuth, getAdminAccessToken } from "@/lib/adminAuth";
import type { ApiResponse } from "@/types/api";
import type { AdminLoginRequest, AdminLoginResponse } from "@/types/adminAuth";
import type {
  AdminBenefit,
  AdminBenefitCreateRequest,
  AdminBenefitDetailItem,
  AdminBenefitDetailItemRequest,
  AdminManualBenefitCreateRequest,
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
import type {
  SourceWatch,
  SourceWatchCollectResponse,
  SourceWatchCreateRequest,
  SourceWatchRegenerateCandidatesResponse,
  SourceWatchUpdateRequest,
} from "@/types/sourceWatch";
import type {
  BenefitCandidate,
  BenefitCandidateApproveRequest,
  BenefitCandidateApproveResponse,
  BenefitCandidateSearchParams,
  BenefitCandidateStatus,
} from "@/types/benefitCandidate";
import type { CollectionRun, CollectionRunSearchParams } from "@/types/collectionRun";
import type { SourceWatchCollectionRunHistory } from "@/types/collectionRun";

type AdminReportParams = {
  status?: ReportStatus;
  reportType?: ReportType;
};

type AdminReportStatusUpdateRequest = {
  status: ReportStatus;
  adminMemo?: string | null;
};

const adminApiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
  timeout: 5000,
});

adminApiClient.interceptors.request.use((config) => {
  const token = getAdminAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

adminApiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && typeof window !== "undefined") {
      clearAdminAuth();
      if (window.location.pathname !== "/admin/login") {
        window.location.href = `/admin/login?next=${encodeURIComponent(window.location.pathname)}`;
      }
    }
    return Promise.reject(error);
  }
);

function compactParams(params?: Record<string, unknown>) {
  if (!params) {
    return undefined;
  }

  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== "")
  );
}

export async function getAdminDashboard() {
  const response = await adminApiClient.get<ApiResponse<AdminDashboard>>("/api/v1/admin/dashboard");
  return response.data.data;
}

export async function loginAdmin(request: AdminLoginRequest) {
  const response = await apiClient.post<ApiResponse<AdminLoginResponse>>("/api/v1/admin/auth/login", request);
  return response.data.data;
}

export async function getAdminReports(params?: AdminReportParams) {
  const response = await adminApiClient.get<ApiResponse<AdminReport[]>>("/api/v1/admin/reports", {
    params: compactParams(params),
  });
  return response.data.data;
}

export async function updateAdminReportStatus(
  reportId: number,
  request: AdminReportStatusUpdateRequest
) {
  const response = await adminApiClient.patch<ApiResponse<AdminReport>>(
    `/api/v1/admin/reports/${reportId}/status`,
    request
  );
  return response.data.data;
}

export async function getAdminBrands(params?: AdminBrandSearchParams) {
  const response = await adminApiClient.get<ApiResponse<AdminBrand[]>>("/api/v1/admin/brands", {
    params: compactParams(params),
  });
  return response.data.data;
}

export async function createAdminBrand(request: AdminBrandCreateRequest) {
  const response = await adminApiClient.post<ApiResponse<AdminBrand>>("/api/v1/admin/brands", request);
  return response.data.data;
}

export async function updateAdminBrand(brandId: number, request: AdminBrandUpdateRequest) {
  const response = await adminApiClient.patch<ApiResponse<AdminBrand>>(
    `/api/v1/admin/brands/${brandId}`,
    request
  );
  return response.data.data;
}

export async function updateAdminBrandActive(brandId: number, request: { isActive: boolean }) {
  const response = await adminApiClient.patch<ApiResponse<AdminBrand>>(
    `/api/v1/admin/brands/${brandId}/active`,
    request
  );
  return response.data.data;
}

export async function getAdminBenefits(params?: AdminBenefitSearchParams) {
  const response = await adminApiClient.get<ApiResponse<AdminBenefit[]>>("/api/v1/admin/benefits", {
    params: compactParams(params),
  });
  return response.data.data;
}

export async function getAdminBenefit(benefitId: number) {
  const response = await adminApiClient.get<ApiResponse<AdminBenefit>>(`/api/v1/admin/benefits/${benefitId}`);
  return response.data.data;
}

export async function createAdminBenefit(request: AdminBenefitCreateRequest) {
  const response = await adminApiClient.post<ApiResponse<AdminBenefit>>("/api/v1/admin/benefits", request);
  return response.data.data;
}

export async function createAdminManualBenefit(request: AdminManualBenefitCreateRequest) {
  const response = await adminApiClient.post<ApiResponse<AdminBenefit>>("/api/v1/admin/benefits/manual", request);
  return response.data.data;
}

export async function updateAdminBenefit(benefitId: number, request: AdminBenefitUpdateRequest) {
  const response = await adminApiClient.patch<ApiResponse<AdminBenefit>>(
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
  const response = await adminApiClient.patch<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}/status`,
    request
  );
  return response.data.data;
}

export async function updateAdminBenefitActive(benefitId: number, request: { isActive: boolean }) {
  const response = await adminApiClient.patch<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}/active`,
    request
  );
  return response.data.data;
}

export async function getAdminBenefitSources(benefitId: number) {
  const response = await adminApiClient.get<ApiResponse<AdminBenefitSource[]>>(
    `/api/v1/admin/benefits/${benefitId}/sources`
  );
  return response.data.data;
}

export async function getAdminBenefitDetailItems(benefitId: number) {
  const response = await adminApiClient.get<ApiResponse<AdminBenefitDetailItem[]>>(
    `/api/v1/admin/benefits/${benefitId}/detail-items`
  );
  return response.data.data;
}

export async function createAdminBenefitDetailItem(
  benefitId: number,
  request: AdminBenefitDetailItemRequest
) {
  const response = await adminApiClient.post<ApiResponse<AdminBenefitDetailItem>>(
    `/api/v1/admin/benefits/${benefitId}/detail-items`,
    request
  );
  return response.data.data;
}

export async function updateAdminBenefitDetailItem(
  itemId: number,
  request: AdminBenefitDetailItemRequest
) {
  const response = await adminApiClient.patch<ApiResponse<AdminBenefitDetailItem>>(
    `/api/v1/admin/benefit-detail-items/${itemId}`,
    request
  );
  return response.data.data;
}

export async function updateAdminBenefitDetailItemActive(itemId: number, request: { isActive: boolean }) {
  const response = await adminApiClient.patch<ApiResponse<AdminBenefitDetailItem>>(
    `/api/v1/admin/benefit-detail-items/${itemId}/active`,
    request
  );
  return response.data.data;
}

export async function createAdminBenefitSource(
  benefitId: number,
  request: AdminBenefitSourceCreateRequest
) {
  const response = await adminApiClient.post<ApiResponse<AdminBenefitSource>>(
    `/api/v1/admin/benefits/${benefitId}/sources`,
    request
  );
  return response.data.data;
}

export async function updateAdminBenefitSource(
  sourceId: number,
  request: AdminBenefitSourceUpdateRequest
) {
  const response = await adminApiClient.patch<ApiResponse<AdminBenefitSource>>(
    `/api/v1/admin/sources/${sourceId}`,
    request
  );
  return response.data.data;
}

export async function deleteAdminBenefitSource(sourceId: number) {
  const response = await adminApiClient.patch<ApiResponse<AdminBenefitSource>>(
    `/api/v1/admin/sources/${sourceId}/delete`
  );
  return response.data.data;
}

export async function addAdminBenefitTag(benefitId: number, request: { tagId: number }) {
  const response = await adminApiClient.post<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}/tags`,
    request
  );
  return response.data.data;
}

export async function deleteAdminBenefitTag(benefitId: number, tagId: number) {
  const response = await adminApiClient.delete<ApiResponse<AdminBenefit>>(
    `/api/v1/admin/benefits/${benefitId}/tags/${tagId}`
  );
  return response.data.data;
}

export async function getAdminBenefitVerificationLogs(benefitId: number) {
  const response = await adminApiClient.get<ApiResponse<VerificationLog[]>>(
    `/api/v1/admin/benefits/${benefitId}/verification-logs`
  );
  return response.data.data;
}

export async function getRecentVerificationLogs(limit = 20) {
  const response = await adminApiClient.get<ApiResponse<VerificationLog[]>>(
    "/api/v1/admin/verification-logs/recent",
    { params: compactParams({ limit }) }
  );
  return response.data.data;
}

export async function getSourceWatches() {
  const response = await adminApiClient.get<ApiResponse<SourceWatch[]>>("/api/v1/admin/source-watches");
  return response.data.data;
}

export async function createSourceWatch(request: SourceWatchCreateRequest) {
  const response = await adminApiClient.post<ApiResponse<SourceWatch>>("/api/v1/admin/source-watches", request);
  return response.data.data;
}

export async function updateSourceWatch(sourceWatchId: number, request: SourceWatchUpdateRequest) {
  const response = await adminApiClient.patch<ApiResponse<SourceWatch>>(
    `/api/v1/admin/source-watches/${sourceWatchId}`,
    request
  );
  return response.data.data;
}

export async function updateSourceWatchActive(sourceWatchId: number, request: { isActive: boolean }) {
  const response = await adminApiClient.patch<ApiResponse<SourceWatch>>(
    `/api/v1/admin/source-watches/${sourceWatchId}/active`,
    request
  );
  return response.data.data;
}

export async function collectSourceWatch(sourceWatchId: number) {
  const response = await adminApiClient.post<ApiResponse<SourceWatchCollectResponse>>(
    `/api/v1/admin/source-watches/${sourceWatchId}/collect`,
    undefined,
    { timeout: 15000 }
  );
  return response.data.data;
}

export async function regenerateSourceWatchCandidates(sourceWatchId: number) {
  const response = await adminApiClient.post<ApiResponse<SourceWatchRegenerateCandidatesResponse>>(
    `/api/v1/admin/source-watches/${sourceWatchId}/regenerate-candidates`
  );
  return response.data.data;
}

export async function getBenefitCandidates(params?: BenefitCandidateSearchParams) {
  const response = await adminApiClient.get<ApiResponse<BenefitCandidate[]>>("/api/v1/admin/benefit-candidates", {
    params: compactParams(params),
  });
  return response.data.data;
}

export async function getBenefitCandidate(candidateId: number) {
  const response = await adminApiClient.get<ApiResponse<BenefitCandidate>>(
    `/api/v1/admin/benefit-candidates/${candidateId}`
  );
  return response.data.data;
}

export async function updateBenefitCandidateStatus(
  candidateId: number,
  request: { status: Exclude<BenefitCandidateStatus, "APPROVED" | "DETECTED">; reviewMemo?: string | null }
) {
  const response = await adminApiClient.patch<ApiResponse<BenefitCandidate>>(
    `/api/v1/admin/benefit-candidates/${candidateId}/status`,
    request
  );
  return response.data.data;
}

export async function approveBenefitCandidate(
  candidateId: number,
  request: BenefitCandidateApproveRequest
) {
  const response = await adminApiClient.post<ApiResponse<BenefitCandidateApproveResponse>>(
    `/api/v1/admin/benefit-candidates/${candidateId}/approve`,
    request
  );
  return response.data.data;
}

export async function getCollectionRuns(params?: CollectionRunSearchParams) {
  const response = await adminApiClient.get<ApiResponse<CollectionRun[]>>("/api/v1/admin/collection-runs", {
    params: compactParams(params),
  });
  return response.data.data;
}

export async function getSourceWatchCollectionRuns(sourceWatchId: number, limit = 10) {
  const response = await adminApiClient.get<ApiResponse<SourceWatchCollectionRunHistory[]>>(
    `/api/v1/admin/source-watches/${sourceWatchId}/collection-runs`,
    { params: compactParams({ limit }) }
  );
  return response.data.data;
}
