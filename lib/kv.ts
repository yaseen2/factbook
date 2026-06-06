import { createClient } from "@vercel/kv";

export const isKvConfigured = !!(
  process.env.KV_REST_API_URL || 
  process.env.REDIS_REST_API_URL ||
  process.env.STORAGE_REST_API_URL
);

export const kv = createClient({
  url: 
    process.env.KV_REST_API_URL || 
    process.env.REDIS_REST_API_URL || 
    process.env.STORAGE_REST_API_URL || 
    "",
  token: 
    process.env.KV_REST_API_TOKEN || 
    process.env.REDIS_REST_API_TOKEN || 
    process.env.STORAGE_REST_API_TOKEN || 
    "",
});
