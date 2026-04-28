import type { AdminLoginResponse } from "@/types/adminAuth";

const ADMIN_AUTH_STORAGE_KEY = "zup_admin_auth";

export function getStoredAdminAuth(): AdminLoginResponse | null {
  if (typeof window === "undefined") {
    return null;
  }

  const rawValue = window.localStorage.getItem(ADMIN_AUTH_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as AdminLoginResponse;
  } catch {
    window.localStorage.removeItem(ADMIN_AUTH_STORAGE_KEY);
    return null;
  }
}

export function getAdminAccessToken() {
  return getStoredAdminAuth()?.accessToken ?? null;
}

export function storeAdminAuth(auth: AdminLoginResponse) {
  window.localStorage.setItem(ADMIN_AUTH_STORAGE_KEY, JSON.stringify(auth));
}

export function clearAdminAuth() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(ADMIN_AUTH_STORAGE_KEY);
}

export function hasAdminAuth() {
  return Boolean(getAdminAccessToken());
}
