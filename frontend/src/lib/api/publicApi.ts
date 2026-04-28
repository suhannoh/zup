import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type { BrandDetail, BrandListItem } from "@/types/brand";
import type { BenefitListItem, BenefitSearchParams } from "@/types/benefit";
import type { Category } from "@/types/category";
import type { ReportCreateRequest, ReportResponse } from "@/types/report";
import type { Tag } from "@/types/tag";

type BrandSearchParams = {
  categorySlug?: string;
  keyword?: string;
};

function compactParams(params?: Record<string, unknown>) {
  if (!params) {
    return undefined;
  }

  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== "")
  );
}

async function getData<T>(path: string, params?: Record<string, unknown>) {
  const response = await apiClient.get<ApiResponse<T>>(path, {
    params: compactParams(params),
  });
  return response.data.data;
}

export function getCategories() {
  return getData<Category[]>("/api/v1/categories");
}

export function getTags() {
  return getData<Tag[]>("/api/v1/tags");
}

export function getBrands(params?: BrandSearchParams) {
  return getData<BrandListItem[]>("/api/v1/brands", params);
}

export function getBrandDetail(slug: string) {
  return getData<BrandDetail>(`/api/v1/brands/${slug}`);
}

export function getCategoryBrands(slug: string) {
  return getData<BrandListItem[]>(`/api/v1/categories/${slug}/brands`);
}

export function getTagBenefits(slug: string) {
  return getData<BenefitListItem[]>(`/api/v1/tags/${slug}/benefits`);
}

export function getBenefits(params?: BenefitSearchParams) {
  return getData<BenefitListItem[]>("/api/v1/benefits", params);
}

export async function createReport(request: ReportCreateRequest) {
  const response = await apiClient.post<ApiResponse<ReportResponse>>("/api/v1/reports", request);
  return response.data.data;
}

export function isApiNotFound(error: unknown) {
  return (
    typeof error === "object" &&
    error !== null &&
    "response" in error &&
    typeof (error as { response?: { status?: number } }).response === "object" &&
    (error as { response?: { status?: number } }).response?.status === 404
  );
}
