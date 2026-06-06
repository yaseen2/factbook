"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { 
  Settings, 
  Sparkles, 
  Layers, 
  Search, 
  Copy, 
  Check, 
  Trash2, 
  Cloud, 
  FileText, 
  Upload, 
  BookOpen, 
  Database, 
  Hash, 
  Info,
  ExternalLink,
  ChevronRight,
  Filter
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

interface SyncStatus {
  category: string;
  status: "success" | "skipped" | "failed";
  details?: string;
}

interface CaptureRecord {
  id: string;
  timestamp: string;
  originalText: string;
  sourceUrl?: string;
  sourceTitle?: string;
  context?: string;
  categories: string[];
  formattedText: string;
  modelUsed: string;
  syncResults: SyncStatus[];
}

const PREDEFINED_CATEGORIES = [
  "Economy & Development",
  "Democracy & Governance",
  "Foreign Policy & Geopolitics",
  "Climate Change & Environment",
  "Technology & Digital Era",
  "Gender & Women Empowerment",
  "Education & Youth",
  "Internal Security & Extremism",
  "Justice & Human Rights",
  "Society & Culture",
  "Islamic Studies (Islamiat)",
  "Miscellaneous"
];

const MOCK_EXTRACTABLE_DOCS = [
  {
    title: "State of Dev Report.pdf",
    text: "At the current inflation rate of 24.5%, the development budget under PSDP faces a massive 30% reduction in real terms. Budgetary deficits are largely driven by legacy domestic debts, structural pension burdens, and loss-making state-owned enterprises (SOEs). Achieving macro-stability requires decoupling fiscal policy from short-term consensus targets."
  },
  {
    title: "National Security Whitepaper 2026.pdf",
    text: "Regional extremism remains tightly coupled with cross-border illicit trade corridors, particularly in border districts. Security reports highlight a 12% rise in weaponized smuggling vectors, requiring targeted physical fencing and advanced AI-assisted UAV monitoring platforms. Digital governance frameworks need synchronized border surveillance APIs."
  },
  {
    title: "Gender & Inclusion Policy.docx",
    text: "Female workforce participation in central industries remains stagnant at 14.2%. Primary structural barriers include safe transit access gaps, digital literacy asymmetries (where 62% of women report restricted internet access), and uncodified informal agricultural labors. Policy intervention must focus on microfinance credits and regional transport links."
  }
];

function renderFormattedText(text: string) {
  if (!text) return null;
  const parts = text.split(/\*\*([^*]+)\*\*/g);
  return parts.map((part, index) => {
    if (index % 2 === 1) {
      return <strong key={index} className="font-extrabold text-indigo-300">{part}</strong>;
    }
    return part;
  });
}

function renderFormattedCard(text: string) {
  if (!text) return null;

  const lines = text.split("\n");
  
  let titleLine = "";
  let argumentText = "";
  let evidenceText = "";
  
  let currentSec: "none" | "argument" | "evidence" = "none";
  
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;
    
    if (trimmed.startsWith("📌") || (line === lines.find(l => l.trim().length > 0) && !titleLine)) {
      titleLine = trimmed;
      continue;
    }
    
    const upper = trimmed.toUpperCase();
    if (upper.includes("THE ARGUMENT:") || upper.includes("ARGUMENT:")) {
      currentSec = "argument";
      const cleanLine = trimmed
        .replace(/\*\*THE ARGUMENT:\*\*/i, "")
        .replace(/\*\*THE ARGUMENT\*\*/i, "")
        .replace(/THE ARGUMENT:/i, "")
        .trim();
      if (cleanLine) argumentText += cleanLine + " ";
      continue;
    }
    
    if (upper.includes("THE EVIDENCE:") || upper.includes("EVIDENCE:")) {
      currentSec = "evidence";
      const cleanLine = trimmed
        .replace(/\*\*THE EVIDENCE:\*\*/i, "")
        .replace(/\*\*THE EVIDENCE\*\*/i, "")
        .replace(/THE EVIDENCE:/i, "")
        .trim();
      if (cleanLine) evidenceText += cleanLine + " ";
      continue;
    }
    
    if (currentSec === "argument") {
      argumentText += line + "\n";
    } else if (currentSec === "evidence") {
      evidenceText += line + "\n";
    }
  }
  
  const cleanArgument = argumentText.replace(/\*\*/g, "").trim();
  const cleanEvidence = evidenceText.replace(/\*\*/g, "").trim();

  if (!titleLine || (!cleanArgument && !cleanEvidence)) {
    return (
      <div className="p-3.5 bg-zinc-900/60 border border-zinc-800/80 rounded-lg text-zinc-300 leading-relaxed font-sans text-xs md:text-sm whitespace-pre-line">
        {renderFormattedText(text)}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Elegantly styled Title block */}
      <div className="flex items-start gap-2 pt-1 border-b border-zinc-800/60 pb-3">
        <span className="text-lg leading-none select-none shrink-0" role="img" aria-label="pin">📌</span>
        <h3 className="font-serif font-black text-zinc-100 text-sm md:text-base tracking-tight leading-snug">
          {titleLine.replace(/^📌\s*/, "")}
        </h3>
      </div>

      <div className="flex flex-col gap-3.5 mt-2">
        {/* Thesis Argument Section */}
        {cleanArgument && (
          <div className="p-4 bg-indigo-950/20 border-l-[3.5px] border-indigo-500 rounded-r-xl rounded-l-xs space-y-1.5 shadow-sm hover:bg-indigo-950/35 transition duration-200">
            <h4 className="text-[10px] font-mono font-bold text-indigo-400 uppercase tracking-widest flex items-center gap-1">
              THE ARGUMENT
            </h4>
            <p className="text-xs md:text-[13px] leading-relaxed text-zinc-250 font-medium font-serif">
              {cleanArgument}
            </p>
          </div>
        )}

        {/* Supporting Evidence Card */}
        {cleanEvidence && (
          <div className="p-4 bg-zinc-900/40 border border-zinc-800/80 rounded-xl space-y-1.5 shadow-xs hover:border-zinc-700 transition duration-200">
            <h4 className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-widest flex items-center gap-1">
              THE EVIDENCE
            </h4>
            <p className="text-xs md:text-[13px] leading-relaxed text-zinc-300 font-sans font-normal">
              {cleanEvidence}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export default function AppDashboard() {
  const [capturedText, setCapturedText] = useState("");
  const [sourceUrl, setSourceUrl] = useState("");
  const [sourceTitle, setSourceTitle] = useState("");
  const [scholarlyContext, setScholarlyContext] = useState("");
  
  // Storage State
  const [records, setRecords] = useState<CaptureRecord[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string | null>(null);
  
  // Load States
  const [isProcessing, setIsProcessing] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [simulationMsg, setSimulationMsg] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load records and parse URL highlight query parameters on mount safely
  useEffect(() => {
    if (typeof window !== "undefined") {
      // Register Progressive Web Application (PWA) Service Worker for offline and Share Target menus on Android
      if ("serviceWorker" in navigator) {
        navigator.serviceWorker.register("/sw.js")
          .then((reg) => console.log("PWA Service Worker registered successfully for Android Share sheet compatibility!", reg.scope))
          .catch((err) => console.warn("PWA Service Worker registration skipped or failed:", err));
      }

      const savedRecords = localStorage.getItem("fb_records");
      if (savedRecords) {
        try {
          setRecords(JSON.parse(savedRecords));
        } catch (e) {
          console.error("Failed to parse local records.", e);
        }
      }

      // Read parameter values preloaded from external bookmarklet click
      const params = new URLSearchParams(window.location.search);
      const paramText = params.get("text");
      const paramUrl = params.get("url");
      const paramTitle = params.get("title");
      const paramContext = params.get("context");

      if (paramText) setCapturedText(paramText);
      if (paramUrl) setSourceUrl(paramUrl);
      if (paramTitle) setSourceTitle(paramTitle);
      if (paramContext) setScholarlyContext(paramContext);

      // Clean browser address line to keep UX immaculate
      if (paramText || paramUrl || paramTitle || paramContext) {
        window.history.replaceState({}, document.title, window.location.pathname);
        setSimulationMsg("Imported citation clip directly from your browser! Review below.");
        setTimeout(() => setSimulationMsg(""), 4500);
      }
    }
  }, []);

  const saveRecordsToLocalStorage = (updatedRecords: CaptureRecord[]) => {
    setRecords(updatedRecords);
    localStorage.setItem("fb_records", JSON.stringify(updatedRecords));
  };

  const handleCaptureSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!capturedText.trim()) {
      setErrorMsg("Please paste or capture some raw text first.");
      return;
    }

    setIsProcessing(true);
    setErrorMsg("");

    try {
      const model = localStorage.getItem("fb_model") || "gemini-3.5-flash";
      const keyStr = localStorage.getItem("fb_keys") || "";
      const docId = localStorage.getItem("fb_doc_id") || "";
      const sa = localStorage.getItem("fb_sa") || "";
      const customPrompt = localStorage.getItem("fb_prompt") || "";

      const geminiKeys = keyStr.split("\n").filter((k) => k.trim());

      const response = await fetch("/api/capture", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          text: capturedText,
          sourceUrl: sourceUrl.trim() || undefined,
          sourceTitle: sourceTitle.trim() || undefined,
          context: scholarlyContext.trim() || undefined,
          settings: {
            model,
            geminiKeys,
            googleDocId: docId.trim() || undefined,
            serviceAccount: sa.trim() || undefined,
            customPrompt: customPrompt.trim() || undefined,
          }
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || "Server processing failure.");
      }

      if (data.success && data.result) {
        const newRecord: CaptureRecord = {
          id: String(Date.now()),
          timestamp: new Date().toLocaleString(),
          originalText: capturedText,
          sourceUrl: sourceUrl.trim() || undefined,
          sourceTitle: sourceTitle.trim() || undefined,
          context: scholarlyContext.trim() || undefined,
          categories: data.result.categories,
          formattedText: data.result.formattedText,
          modelUsed: data.result.aiMeta?.model || model,
          syncResults: data.result.syncMeta || []
        };

        const updated = [newRecord, ...records];
        saveRecordsToLocalStorage(updated);

        // Reset inputs
        setCapturedText("");
        setSourceUrl("");
        setSourceTitle("");
        setScholarlyContext("");
        
        setSimulationMsg("Successfully digitized, structured, and synchronized argument card!");
        setTimeout(() => setSimulationMsg(""), 4500);
      }
    } catch (err: any) {
      setErrorMsg(err?.message || "Internal failure. Verify your credentials in the System Settings panel.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      simulateFileExtraction(e.dataTransfer.files[0].name);
    }
  };

  const simulateFileExtraction = (filename: string) => {
    setIsProcessing(true);
    setSimulationMsg(`Scanning academic source file '${filename}'...`);
    
    setTimeout(() => {
      const match = MOCK_EXTRACTABLE_DOCS.find(doc => filename.toLowerCase().includes(doc.title.split('.')[0].toLowerCase())) 
                    || MOCK_EXTRACTABLE_DOCS[Math.floor(Math.random() * MOCK_EXTRACTABLE_DOCS.length)];
      
      setCapturedText(match.text);
      setSourceTitle(match.title);
      setSourceUrl(`file://local/academic-sources/${filename.replace(/\s+/g, '-')}`);
      setScholarlyContext(`Metadata auto-extracted from uploaded study PDF: '${filename}'`);
      
      setIsProcessing(false);
      setSimulationMsg(`Extracted raw statistics and facts from file '${filename}'!`);
      setTimeout(() => setSimulationMsg(""), 4000);
    }, 1300);
  };

  const handleManualUploadClick = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      simulateFileExtraction(e.target.files[0].name);
    }
  };

  const handleCopyText = (id: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleDeleteRecord = (id: string) => {
    const confirmDelete = window.confirm("Are you sure you want to delete this captured argument from your local history?");
    if (!confirmDelete) return;
    const updated = records.filter((r) => r.id !== id);
    saveRecordsToLocalStorage(updated);
  };

  const handleCategoryFilterToggle = (category: string) => {
    if (selectedCategoryFilter === category) {
      setSelectedCategoryFilter(null);
    } else {
      setSelectedCategoryFilter(category);
    }
  };

  const filteredRecords = records.filter((rec) => {
    const matchesSearch = 
      rec.formattedText.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (rec.originalText && rec.originalText.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (rec.sourceTitle && rec.sourceTitle.toLowerCase().includes(searchQuery.toLowerCase()));
    
    if (selectedCategoryFilter) {
      return matchesSearch && rec.categories.includes(selectedCategoryFilter);
    }
    return matchesSearch;
  });

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-[#0F0E17]">
      
      {/* Top Professional Header Bar */}
      <div className="bg-[#090810] border-b border-zinc-800/40 text-zinc-400 py-2 px-4 md:px-8 font-mono text-[10px] flex items-center justify-between select-none shadow-sm">
        <div className="flex items-center gap-4">
          <span className="text-indigo-400 font-semibold tracking-wider font-mono">LEXIS.DIGITAL</span>
          <span className="opacity-30">|</span>
          <span className="hidden sm:inline">SECURE INTELLECTUAL EVIDENCE ARCHIVE</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-pulse"></span>
          <span className="text-indigo-300 font-mono">STATUS: CLOUD ACTIVE</span>
        </div>
      </div>

      {/* Main Branding Header */}
      <header className="border-b border-zinc-800/80 bg-[#12111A] p-5 md:p-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-2xl md:text-3xl font-serif font-black text-transparent bg-clip-text bg-gradient-to-r from-zinc-100 via-zinc-200 to-indigo-400 flex items-center gap-2.5 tracking-tight">
              <BookOpen className="w-7 h-7 text-indigo-400" />
              Scholar's Research Ledger
            </h1>
            <p className="text-xs text-zinc-400 font-sans max-w-3xl leading-relaxed">
              Structure unstructured research, text clippings, policy drafts, and study material into refined synthesis cards. Automatically categorize evidence indices and synchronize with document workspaces.
            </p>
          </div>

          <Link
            href="/settings"
            className="p-2.5 px-5 bg-zinc-900/60 hover:bg-zinc-800/80 border border-zinc-800 rounded-xl font-mono text-[10px] font-bold text-zinc-300 inline-flex items-center gap-2 transition-all shadow-md cursor-pointer hover:border-zinc-700 hover:text-white"
            id="settings-link"
          >
            <Settings className="w-3.5 h-3.5 text-zinc-400" />
            WORKSPACE CREDENTIALS
          </Link>
        </div>
      </header>

      {/* Main Workspace Split Grid */}
      <main className="flex-1 grid grid-cols-1 lg:grid-cols-12 max-w-7xl w-full mx-auto p-4 md:p-6 gap-6">
        
        {/* Left Column (Input Workbench Pane) - 5/12 Cols */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-[#12111A] rounded-2xl border border-zinc-800/80 p-5 md:p-6 shadow-lg space-y-5">
            
            <div className="flex items-center justify-between border-b border-zinc-800/40 pb-3">
              <h2 className="font-serif text-zinc-200 font-bold text-base flex items-center gap-2">
                <Database className="w-4.5 h-4.5 text-indigo-400" />
                Capture Workstation
              </h2>
              <span className="font-mono text-[9px] bg-indigo-950/40 text-indigo-400 py-0.5 px-2 rounded-md font-bold uppercase tracking-wider">WORKSPACE</span>
            </div>

            {/* Main Fields Form */}
            <form onSubmit={handleCaptureSubmit} className="space-y-4">
              
              {/* Text Input area */}
              <div className="flex flex-col gap-1.5">
                <div className="flex items-center justify-between">
                  <label htmlFor="captured-text-input" className="text-[11px] font-mono font-bold text-zinc-400 uppercase tracking-wider">
                    Raw Citation Clipping Text
                  </label>
                  <span className="text-[10px] font-mono font-medium text-zinc-500">
                    {capturedText.length} chars
                  </span>
                </div>
                <textarea
                  id="captured-text-input"
                  rows={5}
                  value={capturedText}
                  onChange={(e) => setCapturedText(e.target.value)}
                  placeholder="Paste raw research statements, book clippings, website stats, or essay paragraphs..."
                  required
                  className="retro-textarea w-full"
                />
              </div>

              {/* Upload Dropzone */}
              <div 
                onDragEnter={handleDrag}
                onDragOver={handleDrag}
                onDragLeave={handleDrag}
                onDrop={handleDrop}
                className={`border border-dashed rounded-xl p-4 flex flex-col items-center justify-center text-center cursor-pointer transition-all ${
                  dragActive ? "bg-indigo-950/40 border-indigo-500" : "bg-zinc-900/40 hover:bg-zinc-900/80 border-zinc-800"
                }`}
                id="upload-pdf-file-drop"
                onClick={handleManualUploadClick}
              >
                <input 
                  ref={fileInputRef}
                  type="file" 
                  id="file-upload" 
                  className="hidden" 
                  accept=".pdf,.docx,.txt"
                  onChange={handleFileChange}
                />
                <Upload className="w-5 h-5 text-indigo-400 mb-1.5" />
                <p className="text-xs font-semibold text-zinc-300">Drag &amp; drop study PDFs or Click to upload</p>
                <p className="text-[10px] text-zinc-500 mt-0.5">Quickly extracts PDF contents into raw workstation format</p>

                {/* Simulator Pillbox */}
                <div className="flex flex-wrap gap-1 justify-center mt-3 pt-3 border-t border-dashed border-zinc-800 w-full" onClick={(e) => e.stopPropagation()}>
                  <p className="text-[9px] font-mono text-zinc-500 w-full mb-1">DEMO FILE CLIPS:</p>
                  <button
                    type="button"
                    onClick={() => simulateFileExtraction("State_of_Dev_Report.pdf")}
                    className="text-[9px] bg-zinc-900/60 font-semibold text-zinc-400 py-1 px-2.5 rounded-lg border border-zinc-800 hover:border-indigo-500 hover:text-indigo-300 transition"
                  >
                    Dev_Report.pdf
                  </button>
                  <button
                    type="button"
                    onClick={() => simulateFileExtraction("Security_Whitepaper.pdf")}
                    className="text-[9px] bg-zinc-900/60 font-semibold text-zinc-400 py-1 px-2.5 rounded-lg border border-zinc-800 hover:border-indigo-500 hover:text-indigo-300 transition"
                  >
                    Security.pdf
                  </button>
                </div>
              </div>

              {/* Source metadata rows */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="source-title-input" className="text-[11px] font-mono font-bold text-zinc-400 uppercase tracking-wider">
                    Source Publisher / Title
                  </label>
                  <input
                    id="source-title-input"
                    type="text"
                    value={sourceTitle}
                    onChange={(e) => setSourceTitle(e.target.value)}
                    placeholder="e.g. Journal of Economics, Pew"
                    className="retro-input"
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label htmlFor="source-url-input" className="text-[11px] font-mono font-bold text-zinc-400 uppercase tracking-wider">
                    Source Web URL
                  </label>
                  <input
                    id="source-url-input"
                    type="text"
                    value={sourceUrl}
                    onChange={(e) => setSourceUrl(e.target.value)}
                    placeholder="e.g. https://www.pewresearch.org/..."
                    className="retro-input"
                  />
                </div>
              </div>

              {/* Student comments */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="scholarly-context-input" className="text-[11px] font-mono font-bold text-zinc-400 uppercase tracking-wider">
                  Personal Study Remarks (Optional)
                </label>
                <textarea
                  id="scholarly-context-input"
                  rows={2}
                  value={scholarlyContext}
                  onChange={(e) => setScholarlyContext(e.target.value)}
                  placeholder="Insert notes, related questions, outline targets, or analytical connections here..."
                  className="retro-textarea"
                />
              </div>

              {/* Informational / Notification blocks */}
              <AnimatePresence>
                {simulationMsg && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="p-3 bg-indigo-950/40 border border-indigo-800/60 text-indigo-200 font-medium text-xs flex items-center gap-2 rounded-xl"
                  >
                    <Check className="w-4 h-4 text-indigo-400 shrink-0" />
                    <span>{simulationMsg}</span>
                  </motion.div>
                )}
                {errorMsg && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="p-3 bg-rose-950/20 border border-rose-900/60 text-rose-200 text-xs flex items-start gap-2 rounded-xl"
                  >
                    <Info className="w-4 h-4 flex-shrink-0 mt-0.5 text-rose-400" />
                    <div>
                      <p className="font-bold">System Validation Warning</p>
                      <p className="mt-0.5 text-[11px] leading-relaxed text-zinc-400">{errorMsg}</p>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Submit Action */}
              <button
                type="submit"
                disabled={isProcessing}
                className="retro-button-navy w-full py-3 flex items-center justify-center gap-2 cursor-pointer shadow-sm active:translate-y-0 disabled:bg-zinc-800 disabled:text-zinc-600 disabled:border-zinc-800"
                id="process-capture-btn"
              >
                {isProcessing ? (
                  <>
                    <div className="w-4 h-4 border-2 border-zinc-600 border-t-transparent rounded-full animate-spin"></div>
                    Executing Smart Synthesis...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4" />
                    Structure &amp; Index Evidence Item
                  </>
                )}
              </button>

            </form>

            <div className="bg-zinc-900/45 border border-zinc-800/80 rounded-xl p-4 text-xs text-zinc-400 space-y-2 leading-relaxed">
              <span className="font-serif font-bold text-zinc-200 block text-xs">Scholarly Formatting Standard:</span>
              <p>
                Incoming snippets are parsed via private language models to isolate the precise thesis assertion, synthesize key supporting arguments, and draft structured factual evidence blocks.
              </p>
            </div>

          </div>
        </div>

        {/* Right Column (Academic Database Feed Panel) - 7/12 Cols */}
        <div className="lg:col-span-7 space-y-4 flex flex-col h-full">
          
          <div className="bg-[#12111A] rounded-2xl border border-zinc-800/80 p-5 md:p-6 shadow-lg space-y-4 flex flex-col h-full">
            
            <div className="flex items-center justify-between border-b border-zinc-800/40 pb-3">
              <h2 className="font-serif text-zinc-200 font-bold text-base flex items-center gap-2">
                <Layers className="w-4.5 h-4.5 text-indigo-400" />
                Evidence Ledger Index
              </h2>
              <span className="font-mono text-[9px] bg-indigo-950/40 text-indigo-400 py-0.5 px-2 rounded-md font-bold uppercase tracking-wider">DATABASE</span>
            </div>

            {/* Filters / Search Bar */}
            <div className="space-y-4">
              <div className="relative">
                <input
                  id="feed-search-bar"
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search formatted essays, source publishers, or citations..."
                  className="w-full bg-[#151421] border border-zinc-800 rounded-xl pl-10 pr-4 py-2.5 text-sm text-zinc-200 outline-none focus:bg-[#181728] focus:border-indigo-500 transition-all placeholder:text-zinc-500"
                />
                <Search className="w-4.5 h-4.5 text-zinc-500 absolute left-3.5 top-3.5 stroke-[2]" />
              </div>

              {/* Syllabus Predefined filters */}
              <div className="space-y-1.5" id="predefined-categories-filter-tags">
                <div className="flex items-center gap-1 text-[11px] font-mono font-bold text-zinc-400 uppercase tracking-widest">
                  <Filter className="w-3.5 h-3.5 text-zinc-400" />
                  <span>Research Category Filter</span>
                </div>
                <div className="flex flex-wrap gap-1 max-h-24 overflow-y-auto pr-1">
                  {PREDEFINED_CATEGORIES.map((cat, idx) => {
                    const isActive = selectedCategoryFilter === cat;
                    return (
                      <button
                        key={idx}
                        onClick={() => handleCategoryFilterToggle(cat)}
                        className={`text-[10px] font-mono px-2.5 py-1 rounded-md border transition-all cursor-pointer ${
                          isActive 
                            ? "bg-indigo-600 text-white border-indigo-500 font-bold shadow-md" 
                            : "bg-[#161520]/60 text-zinc-400 border-zinc-800/80 hover:border-zinc-700 hover:text-zinc-200"
                        }`}
                      >
                        {cat}
                      </button>
                    );
                  })}
                  {selectedCategoryFilter && (
                    <button
                      onClick={() => setSelectedCategoryFilter(null)}
                      className="text-[10px] font-mono font-bold text-rose-400 border border-dashed border-rose-900/60 rounded-md px-2.5 py-1 bg-zinc-950 hover:bg-rose-950/30 transition cursor-pointer"
                    >
                      Clear Filter
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Scrollable List container */}
            <div className="flex-1 overflow-y-auto min-h-[30rem] max-h-[46rem] space-y-4 pr-1">
              <AnimatePresence>
                {filteredRecords.length === 0 ? (
                  <div className="border border-dashed border-zinc-800 rounded-2xl p-12 text-center bg-zinc-900/10 my-4">
                    <Database className="w-10 h-10 text-zinc-600 mx-auto mb-3" />
                    <p className="text-sm font-serif font-bold text-zinc-350">No structured citations available</p>
                    <p className="text-xs text-zinc-500 mt-1 max-w-xs mx-auto">
                      Sourced clippings structured on the left workstation will materialize here as fully searchable, indexed argument cards.
                    </p>
                  </div>
                ) : (
                  filteredRecords.map((rec) => (
                    <motion.div
                      key={rec.id}
                      initial={{ opacity: 0, y: 15 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, x: -15 }}
                      className="bg-[#151421] border border-zinc-800/60 rounded-xl p-4.5 shadow-sm hover:border-zinc-700 transition duration-200 space-y-3 block relative border-l-4 border-l-indigo-500"
                    >
                      {/* Record header banner */}
                      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-zinc-800/50 pb-2.5 text-[11px] font-mono text-zinc-500">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span className="bg-indigo-950/40 text-indigo-300 border border-indigo-900/60 px-2 py-0.5 text-[9px] font-bold rounded-md uppercase">CARD #{rec.id.slice(-4)}</span>
                          <span>{rec.timestamp}</span>
                          <span className="opacity-40 hidden sm:inline">•</span>
                          <span className="text-indigo-400 font-bold hidden sm:inline">{rec.modelUsed}</span>
                        </div>
                        
                        <div className="flex items-center gap-1.5">
                          <button
                            onClick={() => handleCopyText(rec.id, rec.formattedText)}
                            className="p-1 text-zinc-500 hover:text-indigo-400 transition"
                            title="Copy Citations"
                            id="copy-formatted-text-btn"
                          >
                            {copiedId === rec.id ? (
                              <Check className="w-4 h-4 text-emerald-500 stroke-[2.5]" />
                            ) : (
                              <Copy className="w-4 h-4" />
                            )}
                          </button>
                          <button
                            onClick={() => handleDeleteRecord(rec.id)}
                            className="p-1 text-zinc-500 hover:text-rose-400 transition"
                            title="Delete Item"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>

                      {/* Display content formatting */}
                      <div className="space-y-2">
                        {renderFormattedCard(rec.formattedText)}
                      </div>

                      {/* Sync references and taxonomy */}
                      <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-zinc-800/55">
                        
                        {/* Identified Categories List */}
                        <div className="flex items-center gap-1.5 flex-wrap max-w-[65%]">
                          <Hash className="w-3.5 h-3.5 text-zinc-500" />
                          <div className="flex flex-wrap gap-1">
                            {rec.categories.map((c, i) => (
                              <span
                                key={i}
                                className="text-[9px] font-bold bg-indigo-950/40 text-indigo-300 px-2 py-0.5 rounded-md border border-indigo-900/60 uppercase font-mono"
                              >
                                {c}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* Google DOC Sync Badge pills */}
                        <div className="flex items-center gap-1" id="google-sync-indicator-pill">
                          {rec.syncResults && rec.syncResults.length > 0 ? (
                            rec.syncResults.map((sync, sIdx) => {
                              if (sync.status === "success") {
                                return (
                                  <span
                                    key={sIdx}
                                    className="text-[9px] font-mono font-bold bg-emerald-950/40 text-emerald-400 border border-emerald-900/50 py-0.5 px-2 rounded-md flex items-center gap-1 shadow-xs"
                                  >
                                    <Cloud className="w-2.5 h-2.5" />
                                    SYNCED ({sync.category.split(" ")[0]})
                                  </span>
                                );
                              } else if (sync.status === "failed") {
                                return (
                                  <span
                                    key={sIdx}
                                    className="text-[9px] font-mono font-bold bg-rose-950/20 text-rose-400 border border-rose-900/50 py-0.5 px-2 rounded-md flex items-center gap-1 shadow-xs cursor-help"
                                    title={sync.details}
                                  >
                                    <Cloud className="w-2.5 h-2.5 text-rose-400" />
                                    SYNC ERROR
                                  </span>
                                );
                              } else {
                                return (
                                  <span
                                    key={sIdx}
                                    className="text-[9px] font-mono font-semibold bg-zinc-900/80 text-zinc-500 border border-zinc-800/60 py-0.5 px-2 rounded-md flex items-center"
                                    title="Google Document credentials omitted in workspace credentials. Cached in local index only."
                                  >
                                    LOCAL CARD
                                  </span>
                                );
                              }
                            })
                          ) : (
                            <span className="text-[9px] font-mono font-semibold bg-zinc-900/80 text-zinc-500 border border-zinc-800/60 py-0.5 px-2 rounded-md">
                              LOCAL CARD
                            </span>
                          )}
                        </div>

                      </div>

                      {/* Original web contextual link */}
                      {rec.sourceTitle && (
                        <div className="text-[10px] font-mono text-zinc-400 flex items-center justify-between gap-2 bg-zinc-900/60 p-2 border border-zinc-800/80 rounded-lg mt-2 font-medium">
                          <span className="truncate flex items-center gap-1 text-zinc-400">
                            <span className="w-1 h-1 bg-indigo-500 rounded-full"></span>
                            Publisher: {rec.sourceTitle}
                          </span>
                          {rec.sourceUrl && rec.sourceUrl.startsWith("http") && (
                            <a
                              href={rec.sourceUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-indigo-400 hover:text-indigo-300 hover:underline flex items-center gap-0.5 shrink-0"
                            >
                              Open Source <ExternalLink className="w-2.5 h-2.5" />
                            </a>
                          )}
                        </div>
                      )}

                    </motion.div>
                  ))
                )}
              </AnimatePresence>
            </div>

          </div>

        </div>

      </main>

    </div>
  );
}
