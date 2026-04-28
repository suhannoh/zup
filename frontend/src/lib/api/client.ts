import axios from "axios";
import type { ApiResponse } from "@/types/api";

export type HealthResponse = {
  status: "OK";
};

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
  timeout: 5000,
});

export async function getHealth() {
  const response = await apiClient.get<ApiResponse<HealthResponse>>("/api/v1/health");
  return response.data;
}
