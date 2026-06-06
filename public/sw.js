// Lightweight Service Worker to enable PWA offline installability and share_target
const CACHE_NAME = 'factbook-capturer-v1';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  // Pass-through strategy for dynamic pages and API calls
  event.respondWith(fetch(event.request).catch(() => {
    // Optionally return a cached offline response or a basic offline message
    return new Response("Offline mode active. Rough captures are saved to your local offline queue.");
  }));
});
