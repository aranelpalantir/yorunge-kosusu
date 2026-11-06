const CACHE = 'yorunge-18be1548dd41';
const FILES = [
  './', './index.html', './manifest.json', './levels_data.js', './game.js',
  './icon-192.png', './icon-512.png', './icon-maskable-192.png', './icon-maskable-512.png', './apple-touch-icon.png',
  './assets/orbit.wav', './assets/ion.wav', './assets/drift.wav',
  './assets/turkish_march.wav', './assets/ode.wav', './assets/waltz.wav',
  './assets/pulsar.wav', './assets/nebula.wav', './assets/elise.wav',
  './assets/jump.wav', './assets/turn.wav', './assets/hit.wav',
  './assets/finish.wav', './assets/coin.wav', './assets/frenzy.wav'
];
self.addEventListener('install', e => {
  self.skipWaiting();
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES)));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))));
});
self.addEventListener('fetch', e => {
  e.respondWith(caches.match(e.request, { ignoreSearch: true }).then(r => r || fetch(e.request)));
});
