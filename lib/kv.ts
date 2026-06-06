import Redis from "ioredis";

export const isKvConfigured = !!process.env.REDIS_URL;

// Lazy-initialize client to prevent compile-time connection hangs and warnings
let redisInstance: Redis | null = null;

function getRedis() {
  if (!redisInstance && process.env.REDIS_URL) {
    try {
      const rawUrl = process.env.REDIS_URL.trim();
      redisInstance = new Redis(rawUrl, {
        // Optimize connection for serverless/edge environments
        maxRetriesPerRequest: 1,
        connectTimeout: 5000,
        lazyConnect: true
      });
    } catch (e) {
      console.error("Failed to initialize Redis client:", e);
    }
  }
  return redisInstance;
}

// Wrapper to mimic the @vercel/kv API (auto stringify/parse JSON objects)
export const kv = {
  get: async <T = any>(key: string): Promise<T | null> => {
    const client = getRedis();
    if (!client) return null;
    
    // Ensure we are connected
    if (client.status === "wait") {
      await client.connect();
    }
    
    const value = await client.get(key);
    if (!value) return null;
    
    try {
      return JSON.parse(value) as T;
    } catch (e) {
      return value as unknown as T;
    }
  },
  
  set: async (key: string, value: any): Promise<string> => {
    const client = getRedis();
    if (!client) throw new Error("Redis client not initialized");
    
    // Ensure we are connected
    if (client.status === "wait") {
      await client.connect();
    }
    
    const stringValue = typeof value === "string" ? value : JSON.stringify(value);
    const result = await client.set(key, stringValue);
    return result || "OK";
  }
};
