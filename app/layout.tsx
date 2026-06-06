import "./globals.css";
import React from "react";

export const metadata = {
  title: "Scholarly Synthesis Ledger",
  description: "Intelligent research workspace with advanced evidence categorization and direct document synchronization.",
  manifest: "/manifest.json",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full bg-[#0B0A10]">
      <body className="min-h-screen bg-[#0B0A10] text-[#E4E4E7] font-sans selection:bg-indigo-500/30 antialiased p-0 md:p-3">
        <div className="min-h-[calc(100vh-1.5rem)] md:rounded-2xl md:border border-zinc-800/80 bg-[#0F0E17] flex flex-col relative overflow-x-hidden shadow-2xl">
          {children}
        </div>
      </body>
    </html>
  );
}
