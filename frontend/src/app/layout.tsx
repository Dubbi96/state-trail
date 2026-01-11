import "./globals.css";
import type { Metadata } from "next";
import { Providers } from "@/components/Providers";

export const metadata: Metadata = {
  title: "StateTrail",
  description: "UI 상태/행동 그래프 기반 테스트 생산·운영"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          <div className="min-h-screen bg-slate-50 text-slate-900">
            <div className="mx-auto max-w-7xl px-4 py-6">{children}</div>
          </div>
        </Providers>
      </body>
    </html>
  );
}


