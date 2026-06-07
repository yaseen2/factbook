import { NextRequest, NextResponse } from "next/server";
import { GoogleGenAI, Type } from "@google/genai";
import { google } from "googleapis";
import { kv, isKvConfigured } from "@/lib/kv";

interface CommonTab {
  tabId?: string;
  tabProperties?: {
    tabId: string;
    title: string;
    index?: number;
    nestingLevel?: number;
  };
  childTabs?: CommonTab[];
  documentTab?: {
    body?: {
      content?: any[];
    };
  };
}

// Predefined list of 12 academic categories for CSS studies
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

// Recursive helper to traverse Google Doc tab configurations and locate tab by title
function findTabByTitle(tabs: CommonTab[], title: string): CommonTab | null {
  if (!tabs || tabs.length === 0) return null;
  const normalizedTitle = title.trim();

  // Helper search function to list all tabs flattened
  function flattenTabs(tabsList: CommonTab[]): CommonTab[] {
    let result: CommonTab[] = [];
    for (const t of tabsList) {
      if (t) {
        result.push(t);
        if (t.childTabs && t.childTabs.length > 0) {
          result = result.concat(flattenTabs(t.childTabs));
        }
      }
    }
    return result;
  }

  const allTabs = flattenTabs(tabs);

  const getWords = (str: string) => {
    return str.toLowerCase()
      .replace(/&/g, "and")
      .replace(/[^a-z0-9\s]/g, "")
      .split(/\s+/)
      .filter(w => w.length > 1); // skip single letter noise
  };

  const targetWords = getWords(normalizedTitle);

  let bestMatch: CommonTab | null = null;
  let highestOverlapScore = 0;

  for (const tab of allTabs) {
    const tabTitle = tab.tabProperties?.title || "";
    if (!tabTitle) continue;

    const currentTabTitleLower = tabTitle.toLowerCase().trim();
    const searchLower = normalizedTitle.toLowerCase().trim();

    // 1. Try exact match
    if (currentTabTitleLower === searchLower) {
      return tab;
    }

    // 2. Try normalized exact match (e.g., removing spaces and special symbols)
    const normTabTitle = currentTabTitleLower.replace(/&/g, "and").replace(/[^a-z0-9]/g, "").trim();
    const normSearch = searchLower.replace(/&/g, "and").replace(/[^a-z0-9]/g, "").trim();
    if (normTabTitle === normSearch) {
      return tab;
    }

    // 3. Try substring match (if search category is inside tab title or vice versa)
    if (currentTabTitleLower.includes(searchLower) || searchLower.includes(currentTabTitleLower)) {
      return tab;
    }
    if (normTabTitle.includes(normSearch) || normSearch.includes(normTabTitle)) {
      return tab;
    }

    // 4. Try fuzzy word-tokens overlap matching (for truncated tabs like 'Economy & Develop...')
    const tabWords = getWords(tabTitle);
    const matchingWords = targetWords.filter(w => tabWords.some(tw => tw.startsWith(w) || w.startsWith(tw)));

    if (matchingWords.length > 0) {
      const score = matchingWords.length / Math.max(targetWords.length, tabWords.length);
      if (score > highestOverlapScore) {
        highestOverlapScore = score;
        bestMatch = tab;
      }
    }
  }

  // Fallback to highest overlap if it matches at least 30% of words
  if (highestOverlapScore >= 0.3 && bestMatch) {
    return bestMatch;
  }

  // 5. Last fallback: try a prefix match (first 8 chars) on normalized strings
  const normSearchPrefix = normalizedTitle.toLowerCase().replace(/&/g, "and").replace(/[^a-z0-9]/g, "").trim().substring(0, 8);
  for (const tab of allTabs) {
    const tabTitle = tab.tabProperties?.title || "";
    const normTabTitle = tabTitle.toLowerCase().replace(/&/g, "and").replace(/[^a-z0-9]/g, "").trim();
    if (normTabTitle.substring(0, 8) === normSearchPrefix) {
      return tab;
    }
  }

  return null;
}

interface StyleRange {
  start: number;
  end: number;
}

function parseMarkdownAndBuildStyles(text: string) {
  let plainText = "";
  const boldRanges: StyleRange[] = [];
  
  // Resolve literal \n strings if they exist to assure actual line breaks
  const preparedText = text.replace(/\\n/g, "\n");
  
  let i = 0;
  let inBold = false;
  let boldStart = 0;
  
  while (i < preparedText.length) {
    if (preparedText.substring(i, i + 2) === "**") {
      if (!inBold) {
        inBold = true;
        boldStart = plainText.length;
      } else {
        inBold = false;
        if (plainText.length > boldStart) {
          boldRanges.push({ start: boldStart, end: plainText.length });
        }
      }
      i += 2;
    } else {
      plainText += preparedText[i];
      i++;
    }
  }
  
  if (inBold && plainText.length > boldStart) {
    boldRanges.push({ start: boldStart, end: plainText.length });
  }
  
  // Also, let's automatically bold key labels such as "THE ARGUMENT:" and "THE EVIDENCE:" or any prefix starting with 📌
  const lines = plainText.split("\n");
  let currentOffset = 0;
  
  for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
    const line = lines[lineIndex];
    const trimmed = line.trim();
    
    // Auto-bold the headline (line starting with 📌)
    if (line.length > 0 && (lineIndex === 0 || trimmed.startsWith("📌"))) {
      if (!boldRanges.some(r => r.start <= currentOffset && r.end >= currentOffset + line.length)) {
        boldRanges.push({ start: currentOffset, end: currentOffset + line.length });
      }
    } else if (line.length > 0) {
      // Auto-bold labels like "THE ARGUMENT:" or "THE EVIDENCE:"
      const upper = trimmed.toUpperCase();
      const labels = ["THE ARGUMENT:", "ARGUMENT:", "THE EVIDENCE:", "EVIDENCE:", "THE CLAIMS:", "THE ANALYSIS:"];
      
      for (const label of labels) {
        if (upper.startsWith(label) && label.length > 0) {
          const relativeStart = currentOffset + line.indexOf(trimmed);
          const relativeEnd = relativeStart + label.length;
          if (relativeEnd > relativeStart && !boldRanges.some(r => r.start <= relativeStart && r.end >= relativeEnd)) {
            boldRanges.push({ start: relativeStart, end: relativeEnd });
          }
          break;
        }
      }
    }
    currentOffset += line.length + 1; // +1 for the newline character
  }
  
  return { plainText, boldRanges };
}

// Safe loop-fallback Gemini caller
async function executeGeminiWithFallback(
  keys: string[],
  model: string,
  promptInput: string,
  systemInstruction?: string,
  responseSchema?: any
) {
  // Extract custom user keys, or fall back to platform's GEMINI_API_KEY
  const userKeys = keys.map((k) => k.trim()).filter(Boolean);
  const defaultEnvKeys = process.env.GEMINI_API_KEY
    ? process.env.GEMINI_API_KEY.split(/[\n,;]+/).map((k) => k.trim()).filter(Boolean)
    : [];
  const activeKeys = userKeys.length > 0 ? userKeys : defaultEnvKeys;

  if (activeKeys.length === 0 || !activeKeys[0]) {
    throw new Error("No Gemini API Keys configured. Please paste your API Key in the Settings page.");
  }

  let lastError: any = null;

  for (let i = 0; i < activeKeys.length; i++) {
    const key = activeKeys[i];
    try {
      const ai = new GoogleGenAI({
        apiKey: key,
        httpOptions: {
          headers: {
            "User-Agent": "aistudio-build",
          },
        },
      });

      const response = await ai.models.generateContent({
        model: model || "gemini-3.5-flash",
        contents: promptInput,
        config: {
          systemInstruction: systemInstruction,
          responseMimeType: "application/json",
          responseSchema: responseSchema || undefined,
          temperature: 0.2, // Low temperature for academic accuracy
        },
      });

      if (response && response.text) {
        return {
          text: response.text.trim(),
          keyUsedIndex: i + 1,
          totalKeys: activeKeys.length
        };
      }
    } catch (e: any) {
      console.error(`Gemini call failed with key #${i + 1} (${key.slice(0, 6)}...):`, e.message || e);
      lastError = e;
    }
  }

  throw new Error(
    `Failed after trying ${activeKeys.length} Gemini API keys. Last error: ${lastError?.message || lastError || "Unknown error"}`
  );
}

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { text, sourceUrl, sourceTitle, context, settings, id } = body;

    // Detect dummy integration test values and respond immediately
    if (text === "VERIFY_INTEGRATION_TEST_DUMMY_CLIP_PING") {
      const docId = settings?.googleDocId || "";
      const serviceAccountJsonStr = settings?.serviceAccount || "";
      if (!docId || !serviceAccountJsonStr) {
        return NextResponse.json({ error: "Missing verification inputs" }, { status: 400 });
      }
      // Simple credentials check
      const sa = JSON.parse(serviceAccountJsonStr);
      const docAuth = new google.auth.GoogleAuth({
        credentials: sa,
        scopes: ["https://www.googleapis.com/auth/documents"],
      });
      const docsClient = google.docs({ version: "v1", auth: docAuth });
      await docsClient.documents.get({
        documentId: docId.trim(),
        includeTabsContent: false,
      });
      return NextResponse.json({ success: true });
    }

    if (!text || !text.trim()) {
      return NextResponse.json({ error: "Captured text is required" }, { status: 400 });
    }

    const modelToUse = settings?.model || process.env.MODEL || "gemini-3.5-flash";
    const envKeys = process.env.GEMINI_API_KEY
      ? process.env.GEMINI_API_KEY.split(/[\n,;]+/).map((k) => k.trim()).filter(Boolean)
      : [];
    const userKeys = (settings?.geminiKeys && settings.geminiKeys.length > 0) ? settings.geminiKeys : envKeys;
    const docId = (settings?.googleDocId && settings.googleDocId.trim()) ? settings.googleDocId : (process.env.GOOGLE_DOC_ID || "");
    const serviceAccountJsonStr = (settings?.serviceAccount && settings.serviceAccount.trim()) ? settings.serviceAccount : (process.env.GOOGLE_SERVICE_ACCOUNT || process.env.SERVICE_ACCOUNT || "");

    // Deduplication check: if id is provided, check if it already exists in KV store
    if (id && docId && isKvConfigured) {
      try {
        const key = `factbook:records:${docId.trim()}`;
        const existingRecords = (await kv.get<any[]>(key)) || [];
        const duplicate = existingRecords.find((r: any) => String(r.id) === String(id));
        if (duplicate) {
          console.log(`[Deduplication] Request with ID ${id} already synced. Skipping Doc write and returning success.`);
          return NextResponse.json({
            success: true,
            result: {
              categories: duplicate.categories,
              formattedText: duplicate.formattedText,
              aiMeta: {
                model: duplicate.modelUsed,
                cached: true
              },
              syncMeta: duplicate.syncResults,
            }
          });
        }
      } catch (kvError: any) {
        console.warn("KV deduplication check bypassed safely:", kvError?.message || kvError);
      }
    }

    // 1. Setup Gemini Prompting & Response Schemas
    const defaultSystemPrompt = `You are an elite academic research analyst trained to construct top-tier, authoritative study evidence and logic cards for competitive, postgraduate examinations.
Your task is to review messy, rough text clips and synthesize them into precise, structured academic argument formulations.

Analyze the user's provided raw text clipping and optional context. You MUST classify this piece of evidence into 1 to 3 categories from this selection, depending on which fields represent the absolute best academic fit:
Categories to select from:
${JSON.stringify(PREDEFINED_CATEGORIES, null, 2)}

You will structure the evidence elegantly. Do NOT invent or fabricate facts, statistics, authors, dates, or study associations if they are not explicitly present in the input. If dates or sources are provided, weave them smoothly in.

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

    const customPromptSetting = settings?.customPrompt || "";
    const systemPrompt = (customPromptSetting && customPromptSetting.trim()) ? customPromptSetting.trim() : defaultSystemPrompt;

    const userPrompt = `RAW CAPTURED TEXT:
"""
${text}
"""

SOURCE METADATA:
URL: ${sourceUrl || "Not specified"}
Page Title: ${sourceTitle || "Not specified"}

STUDENT EXPLANATORY CONTEXT / FIELD WORK:
"""
${context || "No context provided."}
"""`;

    const responseSchema = {
      type: Type.OBJECT,
      properties: {
        categories: {
          type: Type.ARRAY,
          items: {
            type: Type.STRING,
          },
          description: "List of 1 to 3 categories matching predefined subjects."
        },
        formattedText: {
          type: Type.STRING,
          description: "Synthesized academic note containing the argument, evidence, and visual pin title."
        }
      },
      required: ["categories", "formattedText"],
    };

    // 2. RUN AI ENGINE
    const aiResult = await executeGeminiWithFallback(
      userKeys,
      modelToUse,
      userPrompt,
      systemPrompt,
      responseSchema
    );

    let parsedResult: { categories: string[]; formattedText: string };
    try {
      parsedResult = JSON.parse(aiResult.text);
    } catch (err) {
      // Inline recovery if response schema was stripped or parsed with errors
      console.warn("Retrying direct parse of Gemini fallback string", aiResult.text);
      parsedResult = {
        categories: ["Miscellaneous"],
        formattedText: aiResult.text || "Failed to digest evidence text."
      };
    }

    // Filter categories to ensure they strictly map to predefined subjects
    const matchedCategories = parsedResult.categories.filter((cat) =>
      PREDEFINED_CATEGORIES.includes(cat)
    );
    if (matchedCategories.length === 0) {
      matchedCategories.push("Miscellaneous");
    }

    // Clean up escaping backslashes but keep markdown for the client to render formattedText beautifully
    const formattedOutputText = parsedResult.formattedText
      .replace(/\\n/g, "\n");

    // 3. SECURE SYNC DIRECTLY TO GOOGLE DOCS (ONLY IF ACCOUNT AND DOC ID ARE SECURED)
    let syncResults: Array<{ category: string; status: "success" | "skipped" | "failed"; details?: string }> = [];

    if (docId && serviceAccountJsonStr) {
      try {
        const sa = JSON.parse(serviceAccountJsonStr);
        const docAuth = new google.auth.GoogleAuth({
          credentials: sa,
          scopes: ["https://www.googleapis.com/auth/documents"],
        });

        const docsClient = google.docs({ version: "v1", auth: docAuth });

        // Retrieve document including entire content and modern tabs
        const doc = await docsClient.documents.get({
          documentId: docId.trim(),
          includeTabsContent: true,
        });

        const tabs = doc.data.tabs as CommonTab[] || [];
        const hasTabs = tabs.length > 0;

        // Push facts into each identified category
        for (const cat of matchedCategories) {
          try {
            let selectedTabId: string | null = null;
            let targetIndex = 1;

            if (hasTabs) {
              const matchedTab = findTabByTitle(tabs, cat);
              if (matchedTab) {
                selectedTabId = matchedTab.tabProperties?.tabId || matchedTab.tabId || null;
                const bodyContent = matchedTab.documentTab?.body?.content || [];
                if (bodyContent.length > 0) {
                  const lastElement = bodyContent[bodyContent.length - 1];
                  if (lastElement && lastElement.endIndex) {
                    targetIndex = Math.max(1, lastElement.endIndex - 1);
                  }
                }
              } else {
                // If specific category tab isn't found, try to fallback to the first tab in the Doc
                const firstTab = tabs[0];
                const firstTabId = firstTab?.tabProperties?.tabId || firstTab?.tabId || null;
                if (firstTab && firstTabId) {
                  selectedTabId = firstTabId;
                  const firstTabContent = firstTab.documentTab?.body?.content || [];
                  if (firstTabContent.length > 0) {
                    const lastElement = firstTabContent[firstTabContent.length - 1];
                    if (lastElement && lastElement.endIndex) {
                      targetIndex = Math.max(1, lastElement.endIndex - 1);
                    }
                  }
                }
              }
            } else {
              // Legacy non-tab Google Doc: append directly to active single body
              const bodyContent = doc.data.body?.content || [];
              if (bodyContent.length > 0) {
                const lastElement = bodyContent[bodyContent.length - 1];
                if (lastElement && lastElement.endIndex) {
                  targetIndex = Math.max(1, lastElement.endIndex - 1);
                }
              }
            }

            // Parse Markdown style annotations and produce clean plainText for Doc insertion
            const { plainText, boldRanges } = parseMarkdownAndBuildStyles(formattedOutputText);

            // Format for inserting beautifully (double paragraph spacing) - Timestamp prefix removed per User Request
            const payloadToInsert = `\n\n${plainText}\n\n`;
            const shiftOffset = 2; // Offset of the leading "\n\n"

            // 1. Perform core text insertion first (critical step)
            await docsClient.documents.batchUpdate({
              documentId: docId.trim(),
              requestBody: {
                requests: [
                  {
                    insertText: {
                      text: payloadToInsert,
                      location: {
                        index: targetIndex,
                        tabId: selectedTabId || undefined,
                      },
                    },
                  },
                ],
              },
            });

            // 2. Perform styling inside a safe, isolated block to protect sync workflow from range/index updates mistakes
            try {
              const styleRequests: any[] = [];

              // Build style requests for bold ranges
              for (const range of boldRanges) {
                if (range.end > range.start) {
                  styleRequests.push({
                    updateTextStyle: {
                      textStyle: {
                        bold: true,
                      },
                      fields: "bold",
                      range: {
                        startIndex: targetIndex + range.start + shiftOffset,
                        endIndex: targetIndex + range.end + shiftOffset,
                        tabId: selectedTabId || undefined,
                      },
                    },
                  });
                }
              }

              // Also make the first line (Headline Title) elegant (12pt, deep navy Indigo color)
              const lines = plainText.split("\n");
              const firstLineLength = lines[0]?.length || 0;
              if (firstLineLength > 0) {
                styleRequests.push({
                  updateTextStyle: {
                    textStyle: {
                      fontSize: {
                        size: 12,
                        unit: "PT",
                      },
                      bold: true,
                      foregroundColor: {
                        color: {
                          rgbColor: {
                            red: 0.11, // Matching indigo palette of the workstation app
                            green: 0.10,
                            blue: 0.29,
                          }
                        }
                      }
                    },
                    fields: "fontSize,bold,foregroundColor",
                    range: {
                      startIndex: targetIndex + shiftOffset,
                      endIndex: targetIndex + firstLineLength + shiftOffset,
                      tabId: selectedTabId || undefined,
                    },
                  },
                });
              }

              if (styleRequests.length > 0) {
                await docsClient.documents.batchUpdate({
                  documentId: docId.trim(),
                  requestBody: {
                    requests: styleRequests,
                  },
                });
              }
            } catch (styleError: any) {
              console.warn("Rich text styling formatting bypassed safely:", styleError?.message || styleError);
            }

            syncResults.push({
              category: cat,
              status: "success",
              details: selectedTabId ? `Synchronized cleanly within TabId: '${selectedTabId}'` : "Appended to Doc Main Body"
            });

          } catch (error: any) {
            console.error(`Doc sync failure for category '${cat}':`, error);
            syncResults.push({
              category: cat,
              status: "failed",
              details: error.message || String(error)
            });
          }
        }
      } catch (err: any) {
        console.error("Auth / Doc fetching failure:", err);
        syncResults = matchedCategories.map(cat => ({
          category: cat,
          status: "failed",
          details: `Doc communication failed: ${err.message || String(err)}`
        }));
      }
    } else {
      // Account credentials not saved client-side, skip push step
      syncResults = matchedCategories.map(cat => ({
        category: cat,
        status: "skipped",
        details: "Google Doc authorization details missing in client settings."
      }));
    }

    // 4. SAVE TO CLOUD LEDGER DATABASE IF KV IS ACTIVE AND A GOOGLE DOC ID IS PROVIDED
    if (docId && isKvConfigured) {
      try {
        const key = `factbook:records:${docId.trim()}`;
        const newRecord = {
          id: id ? String(id) : String(Date.now()),
          timestamp: new Date().toLocaleString(),
          originalText: text,
          sourceUrl: sourceUrl?.trim() || undefined,
          sourceTitle: sourceTitle?.trim() || undefined,
          context: context?.trim() || undefined,
          categories: matchedCategories,
          formattedText: formattedOutputText,
          modelUsed: modelToUse,
          syncResults: syncResults
        };

        const existingRecords = (await kv.get<any[]>(key)) || [];
        const updatedRecords = [newRecord, ...existingRecords];
        await kv.set(key, updatedRecords);
      } catch (kvError: any) {
        console.warn("Vercel KV record caching bypassed safely:", kvError?.message || kvError);
      }
    }

    return NextResponse.json({
      success: true,
      result: {
        categories: matchedCategories,
        formattedText: formattedOutputText,
        aiMeta: {
          model: modelToUse,
          keyUsedIndex: aiResult.keyUsedIndex,
          totalKeys: aiResult.totalKeys,
        },
        syncMeta: syncResults,
      }
    });

  } catch (error: any) {
    console.error("Critical Exception in Factbook capture:", error);
    return NextResponse.json(
      { error: error?.message || "Critical capture process failure." },
      { status: 500 }
    );
  }
}
