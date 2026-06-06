"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
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
  Smartphone, 
  QrCode, 
  Copy, 
  Check, 
  Chrome,
  CloudLightning,
  Coins
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

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
  
  // Real-time client-side sync
  const [appOrigin, setAppOrigin] = useState("");
  const [copiedSyncLink, setCopiedSyncLink] = useState(false);
  const [syncCode, setSyncCode] = useState("");

  // Load settings from LocalStorage & detect Hashtag Sync
  useEffect(() => {
    if (typeof window !== "undefined") {
      setAppOrigin(window.location.origin);
      
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

      // Check if hash sync contains encoded credentials
      const handleHashSync = () => {
        const hash = window.location.hash;
        if (hash && hash.startsWith("#sync=")) {
          const encoded = hash.slice(6);
          try {
            const decodedJsonStr = decodeURIComponent(escape(atob(encoded)));
            const settingsObj = JSON.parse(decodedJsonStr);
            
            const confirmImport = window.confirm(
              "Import Settings Sync?\n\nWe detected Factbook setup credentials shared from your other device. Restoring these will instantly overwrite your current API Keys and Google Doc configuration on this browser."
            );
            
            if (confirmImport) {
              if (settingsObj.model) setModel(settingsObj.model);
              if (settingsObj.apiKeys) setApiKeys(settingsObj.apiKeys);
              if (settingsObj.docId) setDocId(settingsObj.docId);
              if (settingsObj.serviceAccount) setServiceAccount(settingsObj.serviceAccount);
              if (settingsObj.customPrompt) setCustomPrompt(settingsObj.customPrompt);
              
              localStorage.setItem("fb_model", settingsObj.model || "gemini-3.5-flash");
              localStorage.setItem("fb_keys", settingsObj.apiKeys || "");
              localStorage.setItem("fb_doc_id", settingsObj.docId || "");
              localStorage.setItem("fb_sa", settingsObj.serviceAccount || "");
              localStorage.setItem("fb_prompt", settingsObj.customPrompt || "");
              
              setSaveStatus("success");
              setTimeout(() => setSaveStatus("idle"), 3000);
              
              // Wipe hash from URL
              window.history.replaceState({}, document.title, window.location.pathname);
            }
          } catch (e) {
            console.error("Hash decode failed", e);
            alert("Unable to read shared settings. The synchronization code is corrupt or incomplete.");
          }
        }
      };

      handleHashSync();
    }
  }, []);

  // Update dynamic base64 sync payload in real-time
  useEffect(() => {
    const syncObj = {
      model,
      apiKeys,
      docId,
      serviceAccount,
      customPrompt
    };
    try {
      const serialized = JSON.stringify(syncObj);
      const b64 = btoa(unescape(encodeURIComponent(serialized)));
      setSyncCode(b64);
    } catch (e) {
      console.error("Error generating live sync values", e);
    }
  }, [model, apiKeys, docId, serviceAccount, customPrompt]);

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

  const handleCopySyncLink = () => {
    if (!appOrigin) return;
    const fullsyncUrl = `${appOrigin}/settings#sync=${syncCode}`;
    navigator.clipboard.writeText(fullsyncUrl);
    setCopiedSyncLink(true);
    setTimeout(() => setCopiedSyncLink(false), 2000);
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

  const syncUrl = appOrigin ? `${appOrigin}/settings#sync=${syncCode}` : "";
  const qrCodeUrl = syncUrl ? `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(syncUrl)}` : "";
  const appRootMobileQr = appOrigin ? `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(appOrigin)}` : "";

  return (
    <main className="flex-1 min-h-screen bg-[#F8FAFC] text-slate-800 pb-16">
      
      {/* Upper Premium Context Header */}
      <div className="bg-[#1E1B4B] text-[#EEF2F6] py-2 px-4 md:px-8 font-mono text-[11px] flex justify-between items-center select-none shadow-sm">
        <div className="flex items-center gap-4">
          <span className="text-[#818CF8] font-semibold tracking-wider font-serif italic">FACTBOOK.ACADEMICS</span>
          <span className="opacity-40">|</span>
          <span className="hidden sm:inline">CSS CONFIG PREPARATION PANELS</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-indigo-400 animate-pulse"></span>
          <span className="text-indigo-200">LOCAL CRYPTO SECURITY ACTIVE</span>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-4 md:px-8 mt-6 space-y-6">
        
        {/* Navigation & Title Block */}
        <header className="bg-white rounded-2xl border border-slate-200/80 p-5 md:p-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4 shadow-[0_4px_20px_-4px_rgba(148,163,184,0.12)]">
          <div className="flex items-center gap-4">
            <Link
              href="/"
              className="p-3 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl text-slate-700 transition-all hover:border-slate-300 shadow-sm inline-flex items-center justify-center cursor-pointer"
              id="back-home-button"
            >
              <ArrowLeft className="w-5 h-5" />
            </Link>
            <div>
              <p className="text-[11px] font-mono font-bold text-indigo-600 uppercase tracking-widest">Configuration Console</p>
              <h1 className="text-2xl md:text-3xl font-serif font-bold text-slate-900 tracking-tight">System Settings</h1>
            </div>
          </div>
          
          <div className="flex items-center gap-2">
            <button
              onClick={handleSave}
              disabled={saveStatus === "saving"}
              className="flex items-center gap-2 bg-indigo-900 hover:bg-indigo-800 text-white font-semibold text-sm py-2.5 px-6 rounded-xl transition-all cursor-pointer shadow-sm disabled:bg-slate-300"
              id="save-settings-btn"
            >
              <Save className="w-4 h-4" />
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
              className="flex items-center gap-3 p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-800 text-sm font-medium shadow-sm border-l-4 border-l-emerald-500"
            >
              <CheckCircle className="w-5 h-5 text-emerald-600 flex-shrink-0" />
              <p>System settings cached successfully! Ready to capture academic citations seamlessly.</p>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Setup Panes - Two Columns */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          
          {/* Left Column (Primary Field Configuration) - 7 Cols */}
          <div className="lg:col-span-7 space-y-6">
            
            {/* Section 1: AI Engine Configuration */}
            <section className="bg-white rounded-2xl border border-slate-200/80 p-5 md:p-6 space-y-5 shadow-[0_4px_20px_-4px_rgba(148,163,184,0.12)]">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
                <Sparkles className="w-4.5 h-4.5 text-indigo-600" />
                <h2 className="text-base font-serif font-bold text-slate-900">Gemini Academic Engine</h2>
              </div>

              <div className="space-y-4">
                {/* Dropdown for Model */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-[11px] font-mono font-bold text-slate-500 uppercase tracking-wider" htmlFor="model-select">
                    Intelligence Model Selection
                  </label>
                  <select
                    id="model-select"
                    value={model}
                    onChange={(e) => setModel(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-800 font-mono outline-none focus:border-indigo-500 focus:bg-white transition-all cursor-pointer"
                  >
                    <option value="gemini-3.5-flash">gemini-3.5-flash (Fast & Accurate — Recommended)</option>
                    <option value="gemini-3.1-pro-preview">gemini-3.1-pro-preview (Advanced Synthesis)</option>
                    <option value="gemini-3.1-flash-lite">gemini-3.1-flash-lite (Ultra-Economical)</option>
                  </select>
                  <p className="text-[11px] text-slate-500 flex items-center gap-1.5 mt-0.5">
                    <span className="w-1.5 h-1.5 bg-indigo-500 rounded-full shrink-0"></span>
                    Standard Flash yields highly accurate, structured citations and fast summaries.
                  </p>
                </div>

                {/* Textarea for Multi Keys Fallback */}
                <div className="flex flex-col gap-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-[11px] font-mono font-bold text-slate-500 uppercase tracking-wider" htmlFor="api-keys">
                      Gemini API Key Fallback Stack (One per line)
                    </label>
                    <button
                      type="button"
                      onClick={() => setShowKeys(!showKeys)}
                      className="flex items-center gap-1 text-[11px] font-mono text-slate-400 hover:text-slate-700 transition-all cursor-pointer"
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
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 font-mono text-sm text-slate-800 outline-none focus:border-indigo-500 focus:bg-white transition-all placeholder:text-slate-300 leading-relaxed"
                    style={{ WebkitTextSecurity: showKeys ? "none" : "disc" } as React.CSSProperties}
                  />
                  <div className="bg-indigo-50/50 border border-indigo-100 rounded-xl p-3 text-xs text-indigo-900 leading-relaxed">
                    <span className="font-semibold block mb-0.5 text-[#1e1b4b]">✦ Robust Study Flow Protection:</span>
                    If empty, our server uses a backup platform key. If multiple keys are provided, the system automatically cycles keys in the stack in case of rate limits, guaranteeing zero interruptions while studying.
                  </div>
                </div>

                {/* Custom Gemini Prompt Editor */}
                <div className="flex flex-col gap-1.5 pt-2">
                  <div className="flex items-center justify-between">
                    <label className="text-[11px] font-mono font-bold text-slate-500 uppercase tracking-wider" htmlFor="custom-prompt">
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
                        className="text-[10px] font-mono font-bold text-indigo-500 hover:text-indigo-700 transition cursor-pointer"
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
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 font-mono text-xs text-slate-800 outline-none focus:border-indigo-500 focus:bg-white transition-all placeholder:text-slate-400 leading-relaxed"
                  />
                  <div className="text-[10px] text-slate-400 mt-0.5 leading-relaxed">
                    Personalize your system instructions to modify how Gemini processes inputs (e.g., asking for specific exam tags, outline markers, syllabus links, or word limits).
                  </div>
                </div>

              </div>
            </section>

            {/* Section 2: Google Docs Sync Configuration */}
            <section className="bg-white rounded-2xl border border-slate-200/80 p-5 md:p-6 space-y-5 shadow-[0_4px_20px_-4px_rgba(148,163,184,0.12)]">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
                <FileText className="w-4.5 h-4.5 text-indigo-600" />
                <h2 className="text-base font-serif font-bold text-slate-900">Google Docs Synchronization</h2>
              </div>

              <div className="space-y-4">
                {/* Doc ID Input */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-[11px] font-mono font-bold text-slate-500 uppercase tracking-wider" htmlFor="doc-id">
                    Target Google Doc ID
                  </label>
                  <input
                    id="doc-id"
                    type="text"
                    value={docId}
                    onChange={(e) => setDocId(e.target.value)}
                    placeholder="e.g. 1a2b3c4d5e6f7G8h9i..."
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 font-mono text-sm text-slate-800 outline-none focus:border-indigo-500 focus:bg-white transition-all placeholder:text-slate-300"
                  />
                  <p className="text-[11px] text-slate-500 leading-relaxed mt-0.5 flex gap-1 items-start">
                    <Info className="w-3.5 h-3.5 shrink-0 text-indigo-500 mt-0.5" />
                    <span>Copy this string from your Google Doc URL bar. (For example, inside: /d/<strong>[DOC_ID_HERE]</strong>/edit)</span>
                  </p>
                </div>

                {/* Service Account JSON Textarea */}
                <div className="flex flex-col gap-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-[11px] font-mono font-bold text-slate-500 uppercase tracking-wider" htmlFor="service-account">
                      Google Service Account Account JSON
                    </label>
                    <button
                      type="button"
                      onClick={() => setShowSA(!showSA)}
                      className="flex items-center gap-1 text-[11px] font-mono text-slate-400 hover:text-slate-700 transition-all cursor-pointer"
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
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl p-3 font-mono text-xs text-slate-800 outline-none focus:border-indigo-500 focus:bg-white transition-all placeholder:text-slate-300 leading-relaxed"
                    style={{ WebkitTextSecurity: showSA ? "none" : "disc" } as React.CSSProperties}
                  />
                  <p className="text-[11px] text-slate-500 leading-relaxed flex gap-1 items-start mt-0.5">
                    <Info className="w-3.5 h-3.5 shrink-0 text-indigo-500 mt-0.5" />
                    <span>Generate a credentials key file inside your Google Cloud IAM Console and paste the JSON string above. Make sure your Doc is shared with the service account email.</span>
                  </p>
                </div>

                {/* Connection Validation */}
                <div className="border-t border-slate-100 pt-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div className="text-[11px] text-slate-500 max-w-sm leading-relaxed">
                    Instantly test the connection to verify that authorized access is active before committing changes.
                  </div>
                  <button
                    type="button"
                    onClick={handleValidateConnection}
                    disabled={validationStatus === "validating"}
                    className="bg-white border border-slate-200 hover:bg-slate-50 hover:border-slate-300 text-slate-700 text-xs font-semibold py-2.5 px-4 rounded-xl shadow-sm transition-all flex items-center justify-center gap-1.5 disabled:bg-slate-100 cursor-pointer"
                    id="validate-doc-credentials-btn"
                  >
                    {validationStatus === "validating" ? (
                      <>
                        <div className="w-3.5 h-3.5 border-2 border-slate-400 border-t-transparent rounded-full animate-spin"></div>
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
                      className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-xs font-medium flex items-center gap-2"
                    >
                      <CheckCircle className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                      <span>{validationMsg}</span>
                    </motion.div>
                  )}
                  {validationStatus === "invalid" && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto" }}
                      exit={{ opacity: 0, height: 0 }}
                      className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-xs font-medium flex items-center gap-2"
                    >
                      <XCircle className="w-4 h-4 text-rose-600 flex-shrink-0" />
                      <span>{validationMsg}</span>
                    </motion.div>
                  )}
                </AnimatePresence>

              </div>
            </section>

          </div>

          {/* Right Column (Mobile Sync & Guidelines) - 5 Cols */}
          <div className="lg:col-span-5 space-y-6">
            
            {/* Section 3: Instant Phone Sync QR Code */}
            <section className="bg-white rounded-2xl border border-slate-200/80 p-5 md:p-6 space-y-4 shadow-[0_4px_20px_-4px_rgba(148,163,184,0.12)]">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
                <Smartphone className="w-4.5 h-4.5 text-indigo-600" />
                <h2 className="text-base font-serif font-bold text-slate-900">Mobile Integration Sync</h2>
              </div>
              
              <div className="text-xs text-slate-600 space-y-3 leading-relaxed">
                <p>
                  To sync credentials or open the capturer on your <strong>mobile phone</strong>, scan the relevant QR code below:
                </p>

                {/* Tabs for Mobile Sync or Just Clean App Link */}
                <div className="space-y-4">
                  
                  {/* Option A: Sync Settings QR */}
                  <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 flex flex-col items-center text-center space-y-3 relative group">
                    <span className="absolute top-2 left-2 bg-indigo-900 text-white text-[9px] font-mono px-2 py-0.5 rounded-md font-semibold">1-SCAN CONFIG INSTANT SYNC</span>
                    <div className="bg-white p-2 rounded-lg border border-slate-100 shadow-sm mt-3">
                      {qrCodeUrl ? (
                        <Image 
                          src={qrCodeUrl} 
                          alt="Sync Settings QR Code" 
                          width={160} 
                          height={160}
                          className="rounded"
                          referrerPolicy="no-referrer"
                        />
                      ) : (
                        <div className="w-[160px] h-[160px] bg-slate-100 flex items-center justify-center text-slate-300">
                          <QrCode className="w-10 h-10" />
                        </div>
                      )}
                    </div>
                    <div>
                      <h4 className="font-bold text-slate-800 text-[11px] font-mono uppercase tracking-wider">Sync Credentials with Phone</h4>
                      <p className="text-[10px] text-slate-500 mt-1 max-w-[240px]">
                        Scan this QR with your phone camera to open the app on your mobile device with your API keys, Doc ID, and Service Account preloaded.
                      </p>
                    </div>
                    <button
                      onClick={handleCopySyncLink}
                      className="bg-white border border-slate-200 font-semibold hover:bg-slate-100 text-slate-700 text-[11px] py-1.5 px-3.5 rounded-lg shadow-xs transition-all flex items-center gap-1 cursor-pointer"
                    >
                      {copiedSyncLink ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-emerald-600" /> Copied Setup URL!
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5 text-slate-500" /> Copy Setup Share Link
                        </>
                      )}
                    </button>
                  </div>

                  {/* Option B: Direct App QR  */}
                  <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 flex flex-col items-center text-center space-y-3 relative">
                    <span className="absolute top-2 left-2 bg-slate-700 text-white text-[9px] font-mono px-2 py-0.5 rounded-md font-semibold">DIRECT MOBILE LINK</span>
                    <div className="bg-white p-2 rounded-lg border border-slate-100 shadow-sm mt-3">
                      {appRootMobileQr ? (
                        <Image 
                          src={appRootMobileQr} 
                          alt="Mobile App QR Code" 
                          width={140} 
                          height={140}
                          className="rounded"
                          referrerPolicy="no-referrer"
                        />
                      ) : (
                        <div className="w-[140px] h-[140px] bg-slate-100 flex items-center justify-center text-slate-300">
                          <QrCode className="w-10 h-10" />
                        </div>
                      )}
                    </div>
                    <div>
                      <h4 className="font-bold text-slate-800 text-[11px] font-mono uppercase tracking-wider">Quick App URL</h4>
                      <p className="text-[10px] text-slate-500 mt-1">
                        Scan to load a clean Factbook work-station on your phone inside any mobile browser.
                      </p>
                    </div>
                  </div>

                </div>
              </div>
            </section>

            {/* Section 4: Chrome Extension Integration */}
            <section className="bg-white rounded-2xl border border-slate-200/80 p-5 md:p-6 space-y-4 shadow-[0_4px_20px_-4px_rgba(148,163,184,0.12)]">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
                <Chrome className="w-4.5 h-4.5 text-indigo-600" />
                <h2 className="text-base font-serif font-bold text-slate-900">Chrome Extension Inline Capture</h2>
              </div>

              <div className="text-xs text-slate-600 space-y-3 leading-relaxed">
                <p>
                  Deploy our robust **Manifest V3 Chrome Extension** directly from your local project files to capture evidence in a floating overlay modal without leaving your active tab!
                </p>

                <div className="bg-indigo-50/40 p-4 border border-indigo-100 rounded-xl space-y-3.5">
                  <div className="space-y-1">
                    <span className="font-bold text-slate-800 text-[11px] font-mono block">HOW TO LOAD THE EXTENSION:</span>
                    <ol className="list-decimal list-inside space-y-1.5 mt-2 text-[10.5px] text-slate-600">
                      <li>Open Chrome and navigate to <code className="bg-slate-100 text-slate-800 px-1 py-0.5 rounded font-mono text-[9.5px]">chrome://extensions/</code></li>
                      <li>Toggle <strong>Developer mode</strong> in the top-right corner.</li>
                      <li>Click the <strong>Load unpacked</strong> button in the top-left corner.</li>
                      <li>Select the <code className="bg-slate-100 text-slate-800 px-1.5 py-0.5 rounded font-mono text-[9.5px]">chrome-extension</code> folder inside your root project folder.</li>
                    </ol>
                  </div>

                  <div className="border-t border-indigo-100/60 pt-3 space-y-2">
                    <span className="font-bold text-slate-800 text-[11px] font-mono block">CONNECT &amp; SYNC:</span>
                    <ol className="list-decimal list-inside space-y-1.5 text-[10.5px] text-slate-600" start={5}>
                      <li>Click the extension icon in your Chrome toolbar.</li>
                      <li>Click the <strong>Sync Settings from Page</strong> button to automatically copy your API Keys and Google Doc configurations.</li>
                    </ol>
                  </div>

                  <div className="border-t border-indigo-100/60 pt-3 bg-indigo-950/5 p-3 rounded-lg border border-dashed border-indigo-200">
                    <span className="font-bold text-indigo-900 text-[10.5px] font-mono block">✦ SEAMLESS WORKFLOW:</span>
                    <p className="text-[10px] text-indigo-950 mt-1">
                      Highlight any text on any website, right-click, and select <strong>&quot;Send to Scholar&apos;s Ledger&quot;</strong>. A beautiful floating card will slide in directly on the page, letting you edit details and sync evidence instantly!
                    </p>
                  </div>
                </div>
              </div>
            </section>


            {/* Section 5: Google Cloud (Setup and Pricing) Info */}
            <section className="bg-[#1E1B4B] text-slate-200 rounded-2xl p-5 md:p-6 space-y-4 shadow-[0_4px_24px_rgba(30,27,75,0.15)] select-none">
              <div className="flex items-center gap-2 border-b border-indigo-950 pb-3">
                <CloudLightning className="w-4.5 h-4.5 text-indigo-400" />
                <h2 className="text-base font-serif font-bold text-white">Google Cloud Deployment</h2>
              </div>

              <div className="text-[11px] space-y-3.5 leading-relaxed text-slate-300">
                <div className="space-y-1 bg-indigo-950/50 p-3 rounded-xl border border-indigo-900">
                  <div className="flex items-center gap-1.5 font-sans font-bold text-slate-100">
                    <Coins className="w-3.5 h-3.5 text-amber-400" />
                    <span>How much will this cost?</span>
                  </div>
                  <p className="font-sans text-[10.5px] mt-1 text-slate-300">
                    <strong>99.9% Free / Zero Charges!</strong> Google Cloud Run offers an incredibly generous <strong>Free Tier</strong> every month:
                  </p>
                  <ul className="list-disc pl-4 space-y-1 text-[10px] mt-1.5 text-slate-400 font-mono">
                    <li>First 2,000,000 requests/month: $0.00</li>
                    <li>First 180,000 vCPU-seconds/code run: $0.00</li>
                    <li>First 360,000 GiB-seconds Memory: $0.00</li>
                  </ul>
                  <p className="font-sans text-[10px] mt-1.5 text-slate-300">
                    Since you are utilizing it for personal CSS studies, you will never exceed this free tier, resulting in <strong>$0.00 charges</strong>!
                  </p>
                </div>

                <div className="space-y-1 bg-indigo-950/50 p-3 rounded-xl border border-indigo-900">
                  <p className="font-mono font-bold text-slate-100 uppercase tracking-wider">How to Host in 3 Steps:</p>
                  <ol className="list-decimal pl-4 mt-2 space-y-1.5 text-slate-300 text-[10px] font-sans">
                    <li>
                      <strong>Get the code:</strong> Click the <strong>Settings (Gear Icon) on the top right bar</strong> of this AI Studio sandbox window, and select <strong>Export to GitHub</strong> or <strong>Download ZIP</strong>.
                    </li>
                    <li>
                      <strong>Login to Google Cloud:</strong> Open <a href="https://console.cloud.google.com" target="_blank" rel="noopener noreferrer" className="text-indigo-300 hover:underline">console.cloud.google.com</a>, create a free project, and search for <strong>Cloud Run</strong>.
                    </li>
                    <li>
                      <strong>Deploy!</strong> Click &quot;Deploy Container&quot; from Cloud Run, hook up your exported GitHub repo, choose port <strong>3000</strong>, and select &quot;Allow unauthenticated invocations.&quot; Cloud Cloud Run compiles your Docker container automatically and gives you a permanent, secure live URL!
                    </li>
                  </ol>
                </div>
              </div>
            </section>

          </div>

        </div>

        {/* Categories helper guide footer */}
        <section className="bg-white rounded-2xl border border-slate-200/80 p-5 md:p-6 text-xs text-slate-600 font-sans shadow-[0_4px_20px_-4px_rgba(148,163,184,0.12)]">
          <h3 className="font-serif font-bold text-slate-900 mb-1 flex items-center gap-1.5 leading-none">
            <Info className="w-4 h-4 text-indigo-500" />
            Standard Subject Sync Mapping Reference
          </h3>
          <p className="leading-relaxed">
            Your Google Document tabs should preferably name-match the predefined Syllabus Categories to guarantee instant, automated routing. Supported categories are: <em>{PREDEFINED_CATEGORIES.join(", ")}</em>. Matching is extremely smart and automatically ignores casing, symbols, or spacing variances.
          </p>
        </section>

      </div>
    </main>
  );
}
