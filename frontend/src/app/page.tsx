import Link from "next/link";

export default function HomePage() {
  return (
    <main className="space-y-4">
      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h1 className="text-2xl font-semibold">StateTrail</h1>
        <p className="mt-2 text-sm text-slate-600">
          권한별 UI를 상태/행동 그래프로 컴파일하고, 그 그래프에서 테스트를 생산·운영하는 플랫폼.
        </p>
        <div className="mt-4 flex gap-2">
          <Link
            href="/projects"
            className="inline-flex items-center rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
          >
            프로젝트로 이동
          </Link>
        </div>
      </div>
    </main>
  );
}


