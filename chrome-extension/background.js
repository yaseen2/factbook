// Register Context Menu on Install
chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: "capture-evidence",
    title: "Send to Scholar's Ledger",
    contexts: ["selection"]
  });
});

// Handle Context Menu clicks
chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId === "capture-evidence" && tab && tab.id) {
    // Inject content script first
    chrome.scripting.executeScript({
      target: { tabId: tab.id },
      files: ["content.js"]
    }).then(() => {
      // Call initialization inside the page context
      chrome.scripting.executeScript({
        target: { tabId: tab.id },
        func: (selectionText, pageTitle, pageUrl) => {
          if (typeof window.initScholarsLedgerModal === "function") {
            window.initScholarsLedgerModal(selectionText, pageTitle, pageUrl);
          } else {
            console.error("initScholarsLedgerModal is not defined.");
          }
        },
        args: [info.selectionText || "", tab.title || "", tab.url || ""]
      });
    }).catch(err => {
      console.error("Failed to inject Scholar's Ledger content script:", err);
    });
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
      const ledgerUrl = (settings.ledgerUrl || "http://localhost:3000").replace(/\/$/, "");
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
          googleDocId: settings.fb_doc_id ? settings.fb_doc_id.trim() : undefined,
          serviceAccount: settings.fb_sa ? settings.fb_sa.trim() : undefined,
          customPrompt: settings.fb_prompt ? settings.fb_prompt.trim() : undefined,
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
