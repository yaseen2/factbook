import { createClient } from "@vercel/kv";

// Auto-parse standard TCP REDIS_URL if provided by Vercel Upstash Redis integration
let parsedUrl = "";
let parsedToken = "";

if (process.env.REDIS_URL) {
  try {
    const rawUrl = process.env.REDIS_URL.trim();
    // Parse protocol properly (handles redis:// and rediss://)
    const u = new URL(rawUrl);
    parsedUrl = `https://${u.hostname}`;
    parsedToken = u.password;
  } catch (e) {
    console.error("Failed to parse REDIS_URL dynamically for REST connection:", e);
  }
}

export const isKvConfigured = !!(
  process.env.KV_REST_API_URL || 
  process.env.REDIS_REST_API_URL ||
  process.env.STORAGE_REST_API_URL ||
  (parsedUrl && parsedToken)
);

export const kv = createClient({
  url: 
    process.env.KV_REST_API_URL || 
    process.env.REDIS_REST_API_URL || 
    process.env.STORAGE_REST_API_URL || 
    parsedUrl || 
    "",
  token: 
    process.env.KV_REST_API_TOKEN || 
    process.env.REDIS_REST_API_TOKEN || 
    process.env.STORAGE_REST_API_TOKEN || 
    parsedToken || 
    "",
});
