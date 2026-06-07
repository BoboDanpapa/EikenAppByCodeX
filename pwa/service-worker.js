const CACHE_VERSION = "eiken-magicwords-pwa-v1.0.0";
const CORE_ASSETS = [
  "./",
  "./index.html",
  "./backend-config.js",
  "./manifest.webmanifest",
  "./eiken_grade4_words.js",
  "./eiken_grade3_words.js",
  "./eiken_pre2_words.js",
  "./eiken_grade2_words.js",
  "./eiken_pre1_words.js",
  "./eiken_grade1_words.js",
  "./icons/app-icon-192.png",
  "./icons/app-icon-512.png",
  "./icons/app-icon.svg",
  "./images/char_0.jpg",
  "./images/char_1.jpg",
  "./images/char_2.jpg",
  "./images/char_3.jpg",
  "./images/char_4.jpg",
  "./images/char_5.jpg",
  "./images/char_6.jpg",
  "./images/char_7.jpg",
  "./images/char_8.jpg",
  "./images/char_9.jpg",
  "./images/char_10.jpg",
  "./images/char_11.jpg",
  "./images/char_12.jpg",
  "./images/char_13.jpg",
  "./images/char_14.jpg",
  "./images/char_15.jpg",
  "./images/char_16.jpg",
  "./images/char_17.jpg",
  "./images/char_18.jpg",
  "./images/char_19.jpg"
];

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_VERSION)
      .then(cache => cache.addAll(CORE_ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(key => key !== CACHE_VERSION).map(key => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", event => {
  if (event.request.method !== "GET") return;
  if (isNavigationRequest(event.request)) {
    event.respondWith(networkFirst(event.request));
    return;
  }
  if (isFreshAssetRequest(event.request)) {
    event.respondWith(networkFirst(event.request));
    return;
  }
  if (isStaticDataRequest(event.request)) {
    event.respondWith(networkFirst(event.request));
    return;
  }
  event.respondWith(cacheFirst(event.request));
});

function isNavigationRequest(request) {
  return request.mode === "navigate" ||
    (request.destination === "document") ||
    new URL(request.url).pathname.endsWith("/index.html");
}

function isFreshAssetRequest(request) {
  const path = new URL(request.url).pathname;
  return path.endsWith("/service-worker.js") ||
    path.endsWith("/backend-config.js") ||
    path.endsWith("/manifest.webmanifest");
}

function isStaticDataRequest(request) {
  return new URL(request.url).pathname.endsWith(".js");
}

async function networkFirst(request) {
  const cache = await caches.open(CACHE_VERSION);
  try {
    const response = await fetch(request, { cache: "no-store" });
    if (response && response.ok) await cache.put(request, response.clone());
    return response;
  } catch (error) {
    const cached = await caches.match(request);
    if (cached) return cached;
    if (isNavigationRequest(request)) return caches.match("./index.html");
    throw error;
  }
}

async function staleWhileRevalidate(request) {
  const cache = await caches.open(CACHE_VERSION);
  const cached = await caches.match(request);
  const fresh = fetch(request).then(response => {
    if (response && response.ok) cache.put(request, response.clone());
    return response;
  });
  if (cached) {
    fresh.catch(() => undefined);
    return cached;
  }
  return fresh;
}

async function cacheFirst(request) {
  const cached = await caches.match(request);
  if (cached) return cached;
  const response = await fetch(request);
  if (response && response.ok) {
    const cache = await caches.open(CACHE_VERSION);
    await cache.put(request, response.clone());
  }
  return response;
}
