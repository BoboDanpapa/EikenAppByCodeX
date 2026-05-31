const CACHE_VERSION = "eiken-magicwords-pwa-v1.1.21";
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
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) return cached;
      return fetch(event.request).then(response => {
        const copy = response.clone();
        caches.open(CACHE_VERSION).then(cache => cache.put(event.request, copy));
        return response;
      });
    })
  );
});
