"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { 
  ArrowLeft, 
  Save, 
  Eye, 
  EyeOff, 
  CheckCircle, 
  XCircle, 
  Info, 
  Sparkles, 
  FileText, 
  Chrome
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

const DEFAULT_SYSTEM_PROMPT = `You are an elite academic research analyst trained to construct top-tier, authoritative study evidence and logic cards for competitive, postgraduate examinations.
Your task is to review messy, rough text clips and synthesize them into precise, structured academic argument formulations.

Analyze the user's provided raw text clipping and optional context. You MUST classify this piece of evidence into 1 to 3 categories from this selection, depending on which fields represent the absolute best academic fit.

CRITICAL INSTRUCTION - SILENT INTEGRATION:
- Do NOT mention "CSS", "aspirant", "candidate", "exam preparation", "student", or anything meta-textual referring to the exam or how a candidate might use it in an essay. 
- Avoid phrases like "This can be deployed by CSS candidates to argue...", "A student can use this...", or "In CSS essays...".
- Instead, write the argument directly and elegantly as an objective academic truth (e.g., "Structural centralization in governance directly inhibits municipal resource mobilization, as seen in South Asian local governances.").
- Do NOT invent, exaggerate, or fabricate any facts, statistics, names, authors, or study dates. Only synthesize and refine what is present in the source or context.

Format the 'formattedText' to follow this model EXACTLY, including the double-asterisk formatting:
📌 [EVIDENCE TYPE] - [Short Theme/Headline]

**THE ARGUMENT:**
[1-2 sentences of professional academic argument, asserting a strong theoretical or empirical claim derived from the data.]

**THE EVIDENCE:**
[1 paragraph of high-yield synthesized factual prose summarizing the key dates, figures, dynamics, or percentages. Naturally weave the source or background author into the flow (e.g., 'According to World Bank reports on South Asia...', 'As argued by political scientist Dr. Malik...') to maximize readability.]`;

export default function SettingsPage() {
  const [model, setModel] = useState("gemini-3.5-flash");
  const [apiKeys, setApiKeys] = useState("");
  const [docId, setDocId] = useState("");
  const [serviceAccount, setServiceAccount] = useState("");
  const [customPrompt, setCustomPrompt] = useState("");
  
  const [showKeys, setShowKeys] = useState(false);
  const [showSA, setShowSA] = useState(false);
  const [saveStatus, setSaveStatus] = useState<"idle" | "saving" | "success" | "error">("idle");
  const [validationStatus, setValidationStatus] = useState<"idle" | "validating" | "valid" | "invalid">("idle");
  const [validationMsg, setValidationMsg] = useState("");

  // Load settings from LocalStorage
  useEffect(() => {
    if (typeof window !== "undefined") {
      const savedModel = localStorage.getItem("fb_model") || "gemini-3.5-flash";
      const savedKeys = localStorage.getItem("fb_keys") || "";
      const savedDocId = localStorage.getItem("fb_doc_id") || "";
      const savedSA = localStorage.getItem("fb_sa") || "";
      const savedPrompt = localStorage.getItem("fb_prompt") || "";

      setModel(savedModel);
      setApiKeys(savedKeys);
      setDocId(savedDocId);
      setServiceAccount(savedSA);
      setCustomPrompt(savedPrompt);
    }
  }, []);

  const handleSave = () => {
    setSaveStatus("saving");
    try {
      localStorage.setItem("fb_model", model);
      localStorage.setItem("fb_keys", apiKeys);
      localStorage.setItem("fb_doc_id", docId);
      localStorage.setItem("fb_sa", serviceAccount);
      localStorage.setItem("fb_prompt", customPrompt);
      
      setSaveStatus("success");
      setTimeout(() => setSaveStatus("idle"), 3000);
    } catch (e) {
      setSaveStatus("error");
      setTimeout(() => setSaveStatus("idle"), 3000);
    }
  };

  const handleValidateConnection = async () => {
    if (!docId.trim()) {
      setValidationStatus("invalid");
      setValidationMsg("Please insert your Target Google Doc ID first.");
      return;
    }
    if (!serviceAccount.trim()) {
      setValidationStatus("invalid");
      setValidationMsg("Please insert your Service Account JSON credentials.");
      return;
    }

    try {
      JSON.parse(serviceAccount);
    } catch (e) {
      setValidationStatus("invalid");
      setValidationMsg("Google credentials input is not valid JSON.");
      return;
    }

    setValidationStatus("validating");
    setValidationMsg("");

    try {
      const res = await fetch("/api/capture", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          text: "VERIFY_INTEGRATION_TEST_DUMMY_CLIP_PING",
          settings: {
            googleDocId: docId.trim(),
            serviceAccount: serviceAccount.trim(),
          }
        })
      });

      const data = await res.json();
      
      if (res.ok && data.success) {
        setValidationStatus("valid");
        setValidationMsg("Secure Sync verified! Successfully parsed metadata and authenticated with Google Doc.");
      } else {
        setValidationStatus("invalid");
        setValidationMsg(data.error || "Authentication failed. Make sure your Service Account email has 'Editor' permissions on your Doc.");
      }
    } catch (err: any) {
      setValidationStatus("invalid");
      setValidationMsg(`Connection check timed out or failed: ${err?.message || String(err)}`);
    }
  };

  return (
    <main className="flex-1 min-h-screen bg-[#09090B] text-zinc-200 pb-16">
      
      {/* Upper Premium Context Header */}
      <div className="bg-[#040406] border-b border-zinc-900 text-zinc-500 py-2.5 px-4 md:px-8 font-mono text-[10px] flex justify-between items-center select-none shadow-sm">
        <div className="flex items-center gap-4">
          <span className="text-zinc-400 font-semibold tracking-wider font-mono">FACTBOOK.ACADEMICS</span>
          <span className="opacity-20">|</span>
          <span>CONFIGURATION CONTROL INTERFACE</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse"></span>
          <span className="text-blue-400 font-mono">LOCAL DATA SECURITY ACTIVE</span>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-4 md:px-8 mt-8 space-y-6">
        
        {/* Navigation & Title Block */}
        <header className="bg-[#121216] rounded-2xl border border-zinc-800/60 p-5 md:p-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4 shadow-xl">
          <div className="flex items-center gap-4">
            <Link
              href="/"
              className="p-3 bg-zinc-900/60 hover:bg-zinc-850 border border-zinc-800 rounded-xl text-zinc-300 transition-all hover:border-zinc-700 shadow-sm inline-flex items-center justify-center cursor-pointer"
              id="back-home-button"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <div>
              <p className="text-[10px] font-mono font-bold text-blue-400 uppercase tracking-widest">Workspace Dashboard</p>
              <h1 className="text-xl md:text-2xl font-serif font-black text-white tracking-tight">System Settings</h1>
            </div>
          </div>
          
          <div className="flex items-center gap-2">
            <button
              onClick={handleSave}
              disabled={saveStatus === "saving"}
              className="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-500 hover:to-cyan-400 text-white font-semibold text-xs py-2.5 px-6 rounded-xl transition-all cursor-pointer shadow-md shadow-blue-500/10 disabled:bg-zinc-800 disabled:from-zinc-800 disabled:to-zinc-800 disabled:text-zinc-500"
              id="save-settings-btn"
            >
              <Save className="w-3.5 h-3.5" />
              {saveStatus === "saving" ? "Saving..." : "Save Config"}
            </button>
          </div>
        </header>

        {/* Saved Alert Banner */}
        <AnimatePresence>
          {saveStatus === "success" && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="flex items-center gap-3 p-4 bg-emerald-950/20 border border-emerald-900/50 rounded-2xl text-emerald-300 text-xs shadow-md border-l-4 border-l-emerald-500"
            >
              <CheckCircle className="w-4 h-4 text-emerald-400 flex-shrink-0" />
              <p>System settings cached successfully! Ready to capture academic citations seamlessly.</p>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Setup Panes - Two Columns */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          
          {/* Left Column (Primary Field Configuration) - 7 Cols */}
          <div className="lg:col-span-7 space-y-6">
            
            {/* Section 1: AI Engine Configuration */}
            <section className="bg-[#121216] rounded-2xl border border-zinc-800/60 p-5 md:p-6 space-y-5 shadow-lg">
              <div className="flex items-center gap-2 border-b border-zinc-900 pb-3">
                <Sparkles className="w-4 h-4 text-blue-400" />
                <h2 className="text-sm font-mono font-bold text-white uppercase tracking-wider">Gemini Academic Engine</h2>
              </div>

              <div className="space-y-4">
                {/* Dropdown for Model */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-wider" htmlFor="model-select">
                    Intelligence Model Selection
                  </label>
                  <select
                    id="model-select"
                    value={model}
                    onChange={(e) => setModel(e.target.value)}
                    className="w-full bg-[#18181F] border border-zinc-800 rounded-xl p-3 text-xs text-zinc-300 font-mono outline-none focus:border-blue-500/50 focus:bg-[#1E1E26] transition-all cursor-pointer"
                  >
                    <option value="gemini-3.5-flash">gemini-3.5-flash (Fast & Accurate — Recommended)</option>
                    <option value="gemini-3.1-pro-preview">gemini-3.1-pro-preview (Advanced Synthesis)</option>
                    <option value="gemini-3.1-flash-lite">gemini-3.1-flash-lite (Ultra-Economical)</option>
                  </select>
                  <p className="text-[10px] text-zinc-500 flex items-center gap-1.5 mt-0.5">
                    <span className="w-1.5 h-1.5 bg-blue-500/80 rounded-full shrink-0"></span>
                    Standard Flash yields highly accurate, structured citations and fast summaries.
                  </p>
                </div>

                {/* Textarea for Multi Keys Fallback */}
                <div className="flex flex-col gap-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-wider" htmlFor="api-keys">
                      Gemini API Key Fallback Stack (One per line)
                    </label>
                    <button
                      type="button"
                      onClick={() => setShowKeys(!showKeys)}
                      className="flex items-center gap-1 text-[10px] font-mono text-zinc-500 hover:text-zinc-350 transition-all cursor-pointer"
                    >
                      {showKeys ? (
                        <>
                          <EyeOff className="w-3.5 h-3.5" /> Hide stack
                        </>
                      ) : (
                        <>
                          <Eye className="w-3.5 h-3.5" /> Show stack
                        </>
                      )}
                    </button>
                  </div>
                  <textarea
                    id="api-keys"
                    rows={3}
                    value={apiKeys}
                    onChange={(e) => setApiKeys(e.target.value)}
                    placeholder="Paste Gemini API keys here (fallback stack)..."
                    className="w-full bg-[#18181F] border border-zinc-800 rounded-xl p-3 font-mono text-xs text-zinc-200 outline-none focus:border-blue-500/50 focus:bg-[#1E1E26] transition-all placeholder:text-zinc-700 leading-relaxed"
                    style={{ WebkitTextSecurity: showKeys ? "none" : "disc" } as React.CSSProperties}
                  />
                  <div className="bg-blue-950/20 border border-blue-900/40 rounded-xl p-3.5 text-[11px] text-blue-300 leading-relaxed">
                    <span className="font-semibold block mb-0.5 text-zinc-200">✦ Robust Study Flow Protection:</span>
                    If empty, our server uses a backup platform key. If multiple keys are provided, the system automatically cycles keys in the stack in case of rate limits, guaranteeing zero interruptions while studying.
                  </div>
                </div>

                {/* Custom Gemini Prompt Editor */}
                <div className="flex flex-col gap-1.5 pt-2">
                  <div className="flex items-center justify-between">
                    <label className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-wider" htmlFor="custom-prompt">
                      Custom Gemini Translation Prompt (Optional)
                    </label>
                    {customPrompt && (
                      <button
                        type="button"
                        onClick={() => {
                          if (window.confirm("Are you sure you want to restore the default academic syllabus prompt? This will clear your custom text.")) {
                            setCustomPrompt("");
                          }
                        }}
                        className="text-[10px] font-mono font-bold text-blue-400 hover:text-blue-300 transition cursor-pointer"
                      >
                        Reset to Default
                      </button>
                    )}
                  </div>
                  <textarea
                    id="custom-prompt"
                    rows={6}
                    value={customPrompt}
                    onChange={(e) => setCustomPrompt(e.target.value)}
                    placeholder={`If blank, the default CSS academic syllabus prompt below is active:\n\n${DEFAULT_SYSTEM_PROMPT}`}
                    className="w-full bg-[#18181F] border border-zinc-800 rounded-xl p-3 font-mono text-xs text-zinc-300 outline-none focus:border-blue-500/50 focus:bg-[#1E1E26] transition-all placeholder:text-zinc-600 leading-relaxed"
                  />
                  <div className="text-[10px] text-zinc-500 mt-0.5 leading-relaxed">
                    Personalize your system instructions to modify how Gemini processes inputs (e.g., asking for specific exam tags, outline markers, syllabus links, or word limits).
                  </div>
                </div>

              </div>
            </section>

            {/* Section 2: Google Docs Sync Configuration */}
            <section className="bg-[#121216] rounded-2xl border border-zinc-800/60 p-5 md:p-6 space-y-5 shadow-lg">
              <div className="flex items-center gap-2 border-b border-zinc-900 pb-3">
                <FileText className="w-4.5 h-4.5 text-blue-400" />
                <h2 className="text-sm font-mono font-bold text-white uppercase tracking-wider">Google Docs Synchronization</h2>
              </div>

              <div className="space-y-4">
                {/* Doc ID Input */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-wider" htmlFor="doc-id">
                    Target Google Doc ID
                  </label>
                  <input
                    id="doc-id"
                    type="text"
                    value={docId}
                    onChange={(e) => setDocId(e.target.value)}
                    placeholder="e.g. 1a2b3c4d5e6f7G8h9i..."
                    className="w-full bg-[#18181F] border border-zinc-800 rounded-xl p-3 font-mono text-xs text-zinc-200 outline-none focus:border-blue-500/50 focus:bg-[#1E1E26] transition-all placeholder:text-zinc-700"
                  />
                  <p className="text-[10px] text-zinc-500 leading-relaxed mt-0.5 flex gap-1 items-start">
                    <Info className="w-3.5 h-3.5 shrink-0 text-zinc-500 mt-0.5" />
                    <span>Copy this string from your Google Doc URL bar. (For example, inside: /d/<strong>[DOC_ID_HERE]</strong>/edit)</span>
                  </p>
                </div>

                {/* Service Account JSON Textarea */}
                <div className="flex flex-col gap-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-[10px] font-mono font-bold text-zinc-500 uppercase tracking-wider" htmlFor="service-account">
                      Google Service Account Account JSON
                    </label>
                    <button
                      type="button"
                      onClick={() => setShowSA(!showSA)}
                      className="flex items-center gap-1 text-[10px] font-mono text-zinc-500 hover:text-zinc-355 transition-all cursor-pointer"
                    >
                      {showSA ? (
                        <>
                          <EyeOff className="w-3.5 h-3.5" /> Hide JSON
                        </>
                      ) : (
                        <>
                          <Eye className="w-3.5 h-3.5" /> Show JSON
                        </>
                      )}
                    </button>
                  </div>
                  <textarea
                    id="service-account"
                    rows={5}
                    value={serviceAccount}
                    onChange={(e) => setServiceAccount(e.target.value)}
                    placeholder='{ "type": "service_account", "project_id": "...", "private_key": "..." }'
                    className="w-full bg-[#18181F] border border-zinc-800 rounded-xl p-3 font-mono text-xs text-zinc-200 outline-none focus:border-blue-500/50 focus:bg-[#1E1E26] transition-all placeholder:text-zinc-700 leading-relaxed"
                    style={{ WebkitTextSecurity: showSA ? "none" : "disc" } as React.CSSProperties}
                  />
                  <p className="text-[10px] text-zinc-500 leading-relaxed flex gap-1 items-start mt-0.5">
                    <Info className="w-3.5 h-3.5 shrink-0 text-zinc-500 mt-0.5" />
                    <span>Generate a credentials key file inside your Google Cloud IAM Console and paste the JSON string above. Make sure your Doc is shared with the service account email.</span>
                  </p>
                </div>

                {/* Connection Validation */}
                <div className="border-t border-zinc-900 pt-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div className="text-[10px] text-zinc-500 max-w-sm leading-relaxed">
                    Instantly test the connection to verify that authorized access is active before committing changes.
                  </div>
                  <button
                    type="button"
                    onClick={handleValidateConnection}
                    disabled={validationStatus === "validating"}
                    className="bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold py-2.5 px-4 rounded-xl shadow-sm transition-all flex items-center justify-center gap-1.5 disabled:bg-zinc-950 disabled:text-zinc-600 cursor-pointer"
                    id="validate-doc-credentials-btn"
                  >
                    {validationStatus === "validating" ? (
                      <>
                        <div className="w-3 h-3 border-2 border-zinc-400 border-t-transparent rounded-full animate-spin"></div>
                        Testing Connection...
                      </>
                    ) : (
                      "Test Doc Connection"
                    )}
                  </button>
                </div>

                {/* Validation Status Badges */}
                <AnimatePresence mode="wait">
                  {validationStatus === "valid" && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto" }}
                      exit={{ opacity: 0, height: 0 }}
                      className="p-3 bg-emerald-950/20 border border-emerald-900/50 rounded-xl text-emerald-300 text-xs flex items-center gap-2"
                    >
                      <CheckCircle className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                      <span>{validationMsg}</span>
                    </motion.div>
                  )}
                  {validationStatus === "invalid" && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto" }}
                      exit={{ opacity: 0, height: 0 }}
                      className="p-3 bg-rose-950/20 border border-rose-900/50 rounded-xl text-rose-300 text-xs flex items-center gap-2"
                    >
                      <XCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
                      <span>{validationMsg}</span>
                    </motion.div>
                  )}
                </AnimatePresence>

              </div>
            </section>

          </div>

          {/* Right Column (Chrome Extension Guidelines) - 5 Cols */}
          <div className="lg:col-span-5 space-y-6">
            
            {/* Section 4: Chrome Extension Integration */}
            <section className="bg-[#121216] rounded-2xl border border-zinc-800/60 p-5 md:p-6 space-y-4 shadow-lg">
              <div className="flex items-center gap-2 border-b border-zinc-900 pb-3">
                <Chrome className="w-4 h-4 text-blue-400" />
                <h2 className="text-sm font-mono font-bold text-white uppercase tracking-wider">Chrome Extension</h2>
              </div>

              <div className="text-xs text-zinc-400 space-y-3 leading-relaxed">
                <p>
                  Deploy our robust **Manifest V3 Chrome Extension** directly from your local project files to capture evidence in a floating overlay modal without leaving your active tab!
                </p>

                <div className="bg-[#18181F] p-4 border border-zinc-800/80 rounded-xl space-y-3.5">
                  <div className="space-y-1">
                    <span className="font-bold text-zinc-200 text-[10px] font-mono block">1. LOAD THE EXTENSION:</span>
                    <ol className="list-decimal list-inside space-y-1.5 mt-2 text-[10.5px] text-zinc-400">
                      <li>Open Chrome and navigate to <code className="bg-zinc-900 text-zinc-300 px-1 py-0.5 rounded font-mono text-[9px]">chrome://extensions/</code></li>
                      <li>Toggle <strong>Developer mode</strong> in the top-right corner.</li>
                      <li>Click the <strong>Load unpacked</strong> button in the top-left corner.</li>
                      <li>Select the <code className="bg-zinc-900 text-zinc-300 px-1.5 py-0.5 rounded font-mono text-[9px]">chrome-extension</code> folder inside your root project folder.</li>
                    </ol>
                  </div>

                  <div className="border-t border-zinc-800/80 pt-3 space-y-2">
                    <span className="font-bold text-zinc-200 text-[10px] font-mono block">2. CONNECT &amp; SYNC:</span>
                    <ol className="list-decimal list-inside space-y-1.5 text-[10.5px] text-zinc-400" start={5}>
                      <li>Click the extension icon in your Chrome toolbar.</li>
                      <li>Click the <strong>Sync Settings from Page</strong> button to automatically copy your API Keys and Google Doc configurations.</li>
                    </ol>
                  </div>

                  <div className="border-t border-zinc-800/80 pt-3 bg-zinc-950/20 p-3 rounded-lg border border-dashed border-zinc-800">
                    <span className="font-bold text-blue-400 text-[10.5px] font-mono block">✦ SEAMLESS WORKFLOW:</span>
                    <p className="text-[10px] text-zinc-400 mt-1">
                      Highlight any text on any website, right-click, and select <strong>&quot;Send to Scholar&apos;s Ledger&quot;</strong>. A beautiful floating card will slide in directly on the page, letting you edit details and sync evidence instantly!
                    </p>
                  </div>
                </div>
              </div>
            </section>

          </div>

        </div>

      </div>
    </main>
  );
}
