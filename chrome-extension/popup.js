// Helper to display visual alert banners
function showBanner(message, type) {
  const banner = document.getElementById("status-banner");
  banner.textContent = message;
  banner.className = `alert ${type}`;
  banner.style.display = "flex";
  
  // Fade out banner after 4 seconds
  setTimeout(() => {
    banner.style.display = "none";
  }, 4000);
}

// Load current configuration from extension storage on mount
document.addEventListener("DOMContentLoaded", () => {
  chrome.storage.local.get(["ledgerUrl", "fb_sa"], (result) => {
    const urlInput = document.getElementById("ledger-url-input");
    urlInput.value = result.ledgerUrl || "https://factbook-orcin.vercel.app";
    if (result.fb_sa) {
      showBanner("Using custom synced credentials", "info");
    } else {
      showBanner("Using Vercel server environment variables", "info");
    }
  });
});

// Manual Save Handler
document.getElementById("save-url-btn").addEventListener("click", () => {
  const urlInput = document.getElementById("ledger-url-input").value.trim();
  if (!urlInput) {
    showBanner("Domain URL cannot be empty.", "error");
    return;
  }
  
  try {
    // Validate syntax
    new URL(urlInput);
  } catch (e) {
    showBanner("Please enter a valid URL (e.g. http://localhost:3000).", "error");
    return;
  }

  chrome.storage.local.set({ ledgerUrl: urlInput }, () => {
    showBanner("Target Ledger domain saved successfully!", "success");
  });
});

// Sync from Web Page LocalStorage
document.getElementById("sync-credentials-btn").addEventListener("click", () => {
  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    const activeTab = tabs[0];
    if (!activeTab || typeof activeTab.id !== "number" || activeTab.id < 0) {
      showBanner("No active browser tab found.", "error");
      return;
    }

    // Run scraping inside the tab context
    chrome.scripting.executeScript({
      target: { tabId: activeTab.id },
      func: () => {
        return {
          origin: window.location.origin,
          fb_keys: localStorage.getItem("fb_keys"),
          fb_doc_id: localStorage.getItem("fb_doc_id"),
          fb_sa: localStorage.getItem("fb_sa"),
          fb_prompt: localStorage.getItem("fb_prompt"),
          fb_model: localStorage.getItem("fb_model")
        };
      }
    }, (results) => {
      if (chrome.runtime.lastError || !results || !results[0]) {
        showBanner("Authentication sync failed. Are you on your Ledger page?", "error");
        return;
      }

      const data = results[0].result;
      if (data && (data.fb_keys || data.fb_doc_id || data.fb_sa || data.fb_model)) {
        // Persist to extension local storage
        chrome.storage.local.set({
          ledgerUrl: data.origin,
          fb_keys: data.fb_keys || "",
          fb_doc_id: data.fb_doc_id || "",
          fb_sa: data.fb_sa || "",
          fb_prompt: data.fb_prompt || "",
          fb_model: data.fb_model || "gemini-3.5-flash"
        }, () => {
          document.getElementById("ledger-url-input").value = data.origin;
          showBanner("Synced credentials & domain URL successfully!", "success");
        });
      } else {
        showBanner("No ledger credentials found on this page. Navigate to your Ledger settings tab first.", "error");
      }
    });
  });
});

// Clear credentials to fall back directly to Vercel environment variables
document.getElementById("clear-credentials-btn").addEventListener("click", () => {
  chrome.storage.local.remove(["fb_keys", "fb_doc_id", "fb_sa", "fb_prompt", "fb_model"], () => {
    showBanner("Cleared! Extension will now use Vercel server environment variables.", "success");
  });
});
