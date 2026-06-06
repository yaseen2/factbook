import { Inter } from "next/font/google";
import "./globals.css";
import React from "react";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-sans",
});

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
    <html lang="en" className={`${inter.variable} h-full bg-[#050508]`}>
      <body className="min-h-screen bg-[#050508] text-[#E4E4E7] font-sans selection:bg-blue-500/30 antialiased p-0">
        <div className="min-h-screen flex flex-col relative overflow-x-hidden">
          {children}
        </div>
      </body>
    </html>
  );
}
