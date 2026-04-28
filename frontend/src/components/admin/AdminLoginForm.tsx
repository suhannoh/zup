"use client";

import { FormEvent, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { loginAdmin } from "@/lib/api/adminApi";
import { storeAdminAuth } from "@/lib/adminAuth";

export function AdminLoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("admin@zup.local");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const auth = await loginAdmin({ email, password });
      storeAdminAuth(auth);
      router.replace(searchParams.get("next") ?? "/admin");
    } catch {
      setError("이메일 또는 비밀번호를 확인해 주세요.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="mx-auto max-w-md rounded-xl border border-border bg-white p-6" onSubmit={handleSubmit}>
      <div className="space-y-2">
        <p className="text-sm font-semibold text-accent">Zup admin</p>
        <h1 className="text-2xl font-bold text-neutral-950">관리자 로그인</h1>
      </div>

      <label className="mt-6 block text-sm font-semibold">
        이메일
        <input
          className="mt-2 h-11 w-full rounded-lg border border-border px-3 text-sm outline-none focus:border-accent"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />
      </label>

      <label className="mt-4 block text-sm font-semibold">
        비밀번호
        <input
          className="mt-2 h-11 w-full rounded-lg border border-border px-3 text-sm outline-none focus:border-accent"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
      </label>

      {error ? <p className="mt-4 text-sm font-medium text-red-600">{error}</p> : null}

      <button
        className="mt-6 h-11 w-full rounded-lg bg-accent px-4 text-sm font-semibold text-white disabled:opacity-60"
        type="submit"
        disabled={loading}
      >
        {loading ? "로그인 중입니다." : "로그인"}
      </button>
    </form>
  );
}
