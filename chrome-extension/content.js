(function() {
  // Define initialization function on window to be triggered by service worker
  window.initScholarsLedgerModal = function(selectionText, pageTitle, pageUrl) {
    // 1. Remove existing modal if any
    const existingRoot = document.getElementById("scholars-ledger-modal-root");
    if (existingRoot) {
      existingRoot.remove();
    }

    // 2. Create Modal Root and Shadow DOM
    const root = document.createElement("div");
    root.id = "scholars-ledger-modal-root";
    document.body.appendChild(root);

    const shadow = root.attachShadow({ mode: "open" });

    // 3. Inject CSS styling
    const style = document.createElement("style");
    style.textContent = `
      :host {
        all: initial;
        display: block;
      }
      .backdrop {
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        background: rgba(8, 7, 12, 0.7);
        backdrop-filter: blur(4px);
        z-index: 2147483647;
        display: flex;
        align-items: center;
        justify-content: center;
        font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        opacity: 0;
        transition: opacity 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      }
      .backdrop.show {
        opacity: 1;
      }
      .modal-card {
        background: #12111A;
        border: 1px solid rgba(99, 102, 241, 0.25);
        box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 10px 10px -5px rgba(0, 0, 0, 0.4);
        width: 100%;
        max-width: 520px;
        border-radius: 20px;
        overflow: hidden;
        transform: scale(0.95) translateY(10px);
        transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
        display: flex;
        flex-direction: column;
        max-height: 90vh;
      }
      .backdrop.show .modal-card {
        transform: scale(1) translateY(0);
      }
      
      /* Header Styling */
      .modal-header {
        background: #090810;
        padding: 18px 24px;
        border-b: 1px solid rgba(255, 255, 255, 0.05);
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
      .header-title {
        display: flex;
        align-items: center;
        gap: 10px;
        color: #F8FAFC;
        font-size: 16px;
        font-weight: 700;
        letter-spacing: -0.01em;
      }
      .header-logo {
        color: #818CF8;
        display: flex;
        align-items: center;
      }
      .header-badge {
        font-family: monospace;
        font-size: 9px;
        background: rgba(99, 102, 241, 0.15);
        color: #818CF8;
        padding: 2.5px 8px;
        border-radius: 6px;
        text-transform: uppercase;
        font-weight: 700;
        letter-spacing: 0.05em;
      }
      .close-btn {
        background: transparent;
        border: none;
        color: #94A3B8;
        cursor: pointer;
        padding: 5px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
      }
      .close-btn:hover {
        background: rgba(255, 255, 255, 0.08);
        color: #F1F5F9;
      }

      /* Form Content */
      .modal-body {
        padding: 24px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 16px;
      }
      .form-group {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }
      .form-label {
        font-family: monospace;
        font-size: 10px;
        font-weight: 700;
        color: #94A3B8;
        text-transform: uppercase;
        letter-spacing: 0.08em;
      }
      .text-input, .textarea-input {
        background: #171622;
        border: 1px solid #2D2B3F;
        border-radius: 10px;
        color: #F8FAFC;
        padding: 10px 12px;
        font-size: 13px;
        outline: none;
        transition: all 0.2s;
        font-family: inherit;
      }
      .text-input:focus, .textarea-input:focus {
        border-color: #6366F1;
        box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.15);
        background: #1B1A2A;
      }
      .textarea-input {
        resize: vertical;
        line-height: 1.5;
      }

      /* Feedback Messages */
      .status-banner {
        padding: 12px 16px;
        border-radius: 12px;
        font-size: 12px;
        line-height: 1.4;
        display: none;
        align-items: flex-start;
        gap: 10px;
      }
      .status-banner.error {
        display: flex;
        background: rgba(244, 63, 94, 0.1);
        border: 1px solid rgba(244, 63, 94, 0.2);
        color: #FDA4AF;
      }
      .status-banner.success {
        display: flex;
        background: rgba(16, 185, 129, 0.1);
        border: 1px solid rgba(16, 185, 129, 0.2);
        color: #6EE7B7;
      }

      /* Footer Buttons */
      .modal-footer {
        padding: 16px 24px 24px 24px;
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 12px;
        border-t: 1px solid rgba(255, 255, 255, 0.05);
      }
      .btn {
        padding: 10px 20px;
        border-radius: 10px;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        border: none;
      }
      .btn-cancel {
        background: #252433;
        color: #94A3B8;
        border: 1px solid #2D2B3F;
      }
      .btn-cancel:hover:not(:disabled) {
        background: #2D2C3F;
        color: #F1F5F9;
        border-color: #3B3856;
      }
      .btn-submit {
        background: linear-gradient(135deg, #4F46E5 0%, #4338CA 100%);
        color: #FFFFFF;
        box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
      }
      .btn-submit:hover:not(:disabled) {
        background: linear-gradient(135deg, #5A52F9 0%, #4B3FE0 100%);
        box-shadow: 0 4px 16px rgba(79, 70, 229, 0.35);
      }
      .btn:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      /* Spinner */
      .spinner {
        width: 14px;
        height: 14px;
        border: 2px solid rgba(255, 255, 255, 0.3);
        border-top-color: #FFFFFF;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }
      @keyframes spin {
        to { transform: rotate(360deg); }
      }
    `;
    shadow.appendChild(style);

    // 4. Create Modal Markup
    const backdrop = document.createElement("div");
    backdrop.className = "backdrop";
    
    // SVG icons helper
    const bookOpenIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>`;
    const sparklesIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/></svg>`;
    const closeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;

    backdrop.innerHTML = `
      <div class="modal-card">
        <div class="modal-header">
          <div class="header-title">
            <span class="header-logo">${bookOpenIcon}</span>
            <span>Scholar's Ledger</span>
            <span class="header-badge">Capture Mode</span>
          </div>
          <button class="close-btn" id="close-modal-x" type="button" title="Close Panel">${closeIcon}</button>
        </div>

        <form id="capture-inline-form">
          <div class="modal-body">
            
            <div class="status-banner" id="status-display-banner"></div>

            <div class="form-group">
              <label class="form-label" for="evidence-text">Highlighted Text Evidence</label>
              <textarea class="textarea-input" id="evidence-text" rows="4" required></textarea>
            </div>

            <div class="form-group">
              <label class="form-label" for="evidence-publisher">Source Publisher / Title</label>
              <input class="text-input" type="text" id="evidence-publisher" required />
            </div>

            <div class="form-group">
              <label class="form-label" for="evidence-url">Source Web URL</label>
              <input class="text-input" type="url" id="evidence-url" required />
            </div>

            <div class="form-group">
              <label class="form-label" for="evidence-remarks">Study Remarks / Explanatory Context (Optional)</label>
              <textarea class="textarea-input" id="evidence-remarks" rows="2" placeholder="e.g. key connection, questions, outline target..."></textarea>
            </div>

          </div>

          <div class="modal-footer">
            <button class="btn btn-cancel" id="close-modal-btn" type="button">Cancel</button>
            <button class="btn btn-submit" id="submit-capture-btn" type="submit">
              <span class="btn-icon">${sparklesIcon}</span>
              <span class="btn-text">Structure &amp; Sync</span>
            </button>
          </div>
        </form>
      </div>
    `;

    shadow.appendChild(backdrop);

    // 5. Fill fields with payload
    const textInput = shadow.getElementById("evidence-text");
    const publisherInput = shadow.getElementById("evidence-publisher");
    const urlInput = shadow.getElementById("evidence-url");
    const remarksInput = shadow.getElementById("evidence-remarks");

    textInput.value = selectionText;
    publisherInput.value = pageTitle || "";
    urlInput.value = pageUrl || "";

    // 6. Transition in (trigger browser layout first)
    requestAnimationFrame(() => {
      backdrop.classList.add("show");
    });

    // 7. Define Close modal logic
    const closeModal = function() {
      backdrop.classList.remove("show");
      setTimeout(() => {
        root.remove();
      }, 250);
    };

    // Event listeners
    backdrop.addEventListener("click", function(e) {
      if (e.target === backdrop) {
        closeModal();
      }
    });

    shadow.getElementById("close-modal-x").addEventListener("click", closeModal);
    shadow.getElementById("close-modal-btn").addEventListener("click", closeModal);

    // Escape Key listener
    const escListener = function(e) {
      if (e.key === "Escape") {
        document.removeEventListener("keydown", escListener);
        closeModal();
      }
    };
    document.addEventListener("keydown", escListener);

    // Form submission
    const form = shadow.getElementById("capture-inline-form");
    const submitBtn = shadow.getElementById("submit-capture-btn");
    const statusBanner = shadow.getElementById("status-display-banner");

    form.addEventListener("submit", function(e) {
      e.preventDefault();

      // Disable inputs
      textInput.disabled = true;
      publisherInput.disabled = true;
      urlInput.disabled = true;
      remarksInput.disabled = true;
      submitBtn.disabled = true;
      shadow.getElementById("close-modal-btn").disabled = true;

      // Update button visual to spinner state
      submitBtn.querySelector(".btn-icon").innerHTML = `<div class="spinner"></div>`;
      submitBtn.querySelector(".btn-text").textContent = "Structuring & Syncing...";

      // Clear previous banner state
      statusBanner.style.display = "none";
      statusBanner.className = "status-banner";

      // Message background worker to route the API POST
      chrome.runtime.sendMessage({
        action: "SUBMIT_CAPTURE",
        payload: {
          text: textInput.value,
          sourceTitle: publisherInput.value,
          sourceUrl: urlInput.value,
          context: remarksInput.value
        }
      }, function(response) {
        if (chrome.runtime.lastError) {
          showError(`Extension communication failure: ${chrome.runtime.lastError.message}`);
          return;
        }

        if (response && response.success) {
          showSuccess(response.data);
        } else {
          showError(response ? response.error : "Failed to establish server connection. Verify your target URL in Settings.");
        }
      });
    });

    function showError(errText) {
      // Re-enable input controls
      textInput.disabled = false;
      publisherInput.disabled = false;
      urlInput.disabled = false;
      remarksInput.disabled = false;
      submitBtn.disabled = false;
      shadow.getElementById("close-modal-btn").disabled = false;

      // Reset button layout
      submitBtn.querySelector(".btn-icon").innerHTML = sparklesIcon;
      submitBtn.querySelector(".btn-text").textContent = "Structure & Sync";

      // Show banner error
      statusBanner.className = "status-banner error";
      statusBanner.innerHTML = `
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="shrink: 0; margin-top: 1.5px;"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
        <div>
          <span style="font-weight: 700;">Submission Error:</span>
          <span style="margin-top: 2px; font-size: 11.5px; opacity: 0.95; display: block;">${errText}</span>
        </div>
      `;
    }

    function showSuccess(data) {
      // Show success layout on button
      submitBtn.querySelector(".btn-icon").innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
      submitBtn.querySelector(".btn-text").textContent = "Captured & Synced!";
      submitBtn.style.background = "linear-gradient(135deg, #059669 0%, #047857 100%)";
      submitBtn.style.boxShadow = "0 4px 12px rgba(5, 150, 105, 0.2)";

      // Setup success message details
      const categoriesList = data.result?.categories || [];
      const syncMeta = data.result?.syncMeta || [];
      
      let syncText = "";
      if (syncMeta.length > 0) {
        const successes = syncMeta.filter(s => s.status === "success").map(s => s.category);
        const fails = syncMeta.filter(s => s.status === "failed").map(s => s.category);
        if (successes.length > 0) syncText += ` Synced to Google Doc tabs: ${successes.join(", ")}.`;
        if (fails.length > 0) syncText += ` (Failed to sync to: ${fails.join(", ")})`;
      }

      // Show success banner
      statusBanner.className = "status-banner success";
      statusBanner.innerHTML = `
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="shrink: 0; margin-top: 1px;"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
        <div>
          <span style="font-weight: 700;">Success!</span>
          <span style="margin-top: 2px; font-size: 11.5px; opacity: 0.95; display: block;">Digitized into categories: <strong>${categoriesList.join(", ")}</strong>.${syncText}</span>
        </div>
      `;

      // Auto close after 2.5 seconds
      setTimeout(closeModal, 2500);
    }
  };
})();
