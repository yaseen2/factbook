// Register Context Menu on Install
chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: "capture-evidence",
    title: "Send to Scholar's Ledger",
    contexts: ["selection"]
  });
});

// Handle Context Menu clicks
chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== "capture-evidence") return;

  let targetTab = tab;
  // In Edge / Chromium, tab can be missing or tab.id can be -1 (chrome.tabs.TAB_ID_NONE)
  if (!targetTab || typeof targetTab.id !== "number" || targetTab.id < 0) {
    try {
      const [activeTab] = await chrome.tabs.query({ active: true, currentWindow: true });
      targetTab = activeTab;
    } catch (e) {
      console.warn("Could not query active tab:", e);
    }
  }

  // If still no valid tab or tab ID is negative/invalid, abort gracefully
  if (!targetTab || typeof targetTab.id !== "number" || targetTab.id < 0) {
    console.warn("Scholar's Ledger: No valid active tab found with a valid tab ID.");
    return;
  }

  const url = targetTab.url || "";
  // Cannot inject content scripts into internal browser pages or web extension stores
  if (
    url.startsWith("edge://") ||
    url.startsWith("chrome://") ||
    url.startsWith("about:") ||
    url.startsWith("chrome-extension://") ||
    url.startsWith("edge-extension://") ||
    url.startsWith("devtools://") ||
    url.startsWith("view-source:") ||
    url.includes("microsoftedge.microsoft.com") ||
    url.includes("chromewebstore.google.com")
  ) {
    console.warn("Scholar's Ledger: Cannot inject content scripts into browser internal or store pages:", url);
    return;
  }

  try {
    // Inject content script first
    await chrome.scripting.executeScript({
      target: { tabId: targetTab.id },
      files: ["content.js"]
    });

    // Call initialization inside the page context
    await chrome.scripting.executeScript({
      target: { tabId: targetTab.id },
      func: (selectionText, pageTitle, pageUrl) => {
        if (typeof window.initScholarsLedgerModal === "function") {
          window.initScholarsLedgerModal(selectionText, pageTitle, pageUrl);
        } else {
          console.error("initScholarsLedgerModal is not defined.");
        }
      },
      args: [info.selectionText || "", targetTab.title || "", targetTab.url || ""]
    });
  } catch (err) {
    if (targetTab.url?.startsWith("file:///")) {
      console.warn("Scholar's Ledger: To capture text from local files or PDFs, you must enable 'Allow access to file URLs' in edge://extensions (Click 'Details' on Scholar's Ledger -> toggle ON 'Allow access to file URLs').");
    }
    console.error("Failed to inject Scholar's Ledger content script:", err);
  }
});

// Route capture API calls via background script to bypass CORS and CSP restrictions
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === "SUBMIT_CAPTURE") {
    // Fetch settings from extension storage
    chrome.storage.local.get([
      "ledgerUrl",
      "fb_keys",
      "fb_doc_id",
      "fb_sa",
      "fb_prompt",
      "fb_model"
    ], (settings) => {
      const ledgerUrl = (settings.ledgerUrl || "https://factbook-orcin.vercel.app").replace(/\/$/, "");
      const keysStr = settings.fb_keys || "";
      const geminiKeys = keysStr.split("\n").map(k => k.trim()).filter(Boolean);

      const requestBody = {
        text: message.payload.text,
        sourceUrl: message.payload.sourceUrl,
        sourceTitle: message.payload.sourceTitle,
        context: message.payload.context || undefined,
        settings: {
          model: settings.fb_model || "gemini-3.5-flash",
          geminiKeys: geminiKeys.length > 0 ? geminiKeys : undefined,
          googleDocId: (settings.fb_doc_id && settings.fb_doc_id.trim()) ? settings.fb_doc_id.trim() : undefined,
          serviceAccount: (settings.fb_sa && settings.fb_sa.trim()) ? settings.fb_sa.trim() : undefined,
          customPrompt: (settings.fb_prompt && settings.fb_prompt.trim()) ? settings.fb_prompt.trim() : undefined,
        }
      };

      fetch(`${ledgerUrl}/api/capture`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(requestBody)
      })
      .then(async (response) => {
        const data = await response.json();
        if (!response.ok) {
          sendResponse({ success: false, error: data.error || `Server returned status ${response.status}` });
        } else {
          sendResponse({ success: true, data });
        }
      })
      .catch((error) => {
        console.error("Capture API request failed:", error);
        sendResponse({ success: false, error: error.message || "Network error. Is your Ledger server running and accessible?" });
      });
    });

    return true; // Keep message channel open for asynchronous response
  }
});
