const CACHE = 'yorunge-1e37389f630b';
const FILES = [
  './', './index.html', './manifest.json', './levels_data.js?v=1e37389f630b', './game.js?v=1e37389f630b',
  './icon-192.png', './icon-512.png', './icon-maskable-192.png', './icon-maskable-512.png', './apple-touch-icon.png',
  './assets/orbit.wav', './assets/ion.wav', './assets/drift.wav',
  './assets/turkish_march.wav', './assets/ode.wav', './assets/waltz.wav',
  './assets/pulsar.wav', './assets/nebula.wav', './assets/elise.wav',
  './assets/jump.wav', './assets/turn.wav', './assets/hit.wav',
  './assets/finish.wav', './assets/coin.wav', './assets/frenzy.wav'
];
self.addEventListener('install', e => {
  self.skipWaiting();
  // cache: 'reload' skips the browser's HTTP cache, otherwise a new build can be stored with old files.
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES.map(f => new Request(f, { cache: 'reload' })))));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))));
});
self.addEventListener('fetch', e => {
  // Pages come from the network when online, so a new build shows on the next open; the cache covers offline.
  if (e.request.mode === 'navigate') {
    e.respondWith(fetch(e.request.url, { cache: 'no-cache' }).then(r => {
      if (!r.ok) throw new Error('offline fallback');
      return r;
    }).catch(() => caches.match(e.request, { ignoreSearch: true })));
    return;
  }
  // Versioned scripts must match exactly: a newer page never runs with older scripts.
  const versioned = new URL(e.request.url).searchParams.has('v');
  e.respondWith(caches.match(e.request, { ignoreSearch: !versioned }).then(r => r || fetch(e.request)));
});
