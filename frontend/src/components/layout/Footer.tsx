export function Footer() {
  return (
    <footer className="bg-neutral-950 px-5 py-10 text-neutral-400 md:px-12">
      <div className="mx-auto max-w-6xl">
        <p className="text-sm font-semibold text-white">Zup</p>
        <p className="mt-3 max-w-2xl border-l border-neutral-700 pl-4 text-sm leading-7">
          Zup은 각 브랜드의 공식 서비스가 아니며, 공개된 공식 출처를 바탕으로 혜택 정보를 정리하는 정보 제공
          서비스입니다. 혜택 정책은 수시로 변경될 수 있으므로 사용 전 공식 페이지 또는 앱에서 최종 확인해 주세요.
        </p>
        <p className="mt-6 text-xs">© 2026 Zup.</p>
      </div>
    </footer>
  );
}
