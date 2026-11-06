"""Build the PWA bundle from the shared Java source and audio assets."""
import hashlib, shutil, subprocess, os, struct, zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PWA = ROOT / 'pwa'
ASSETS = PWA / 'assets'
ICONS = ['icon-192.png', 'icon-512.png', 'icon-maskable-192.png', 'icon-maskable-512.png', 'apple-touch-icon.png']

def make_icon(path, size, content=1.0, rounded=False):
    # Pure-Python antialiased icon: the survey robot's head inside the amber tunnel ring.
    # content shrinks the artwork for maskable safe zones; rounded cuts transparent corners.
    BG, FILL, TEAL, AMBER, SUIT = (8, 23, 34), (18, 58, 74), (85, 207, 206), (255, 205, 119), (203, 139, 63)
    VISOR, MINT, HIGHLIGHT = (16, 42, 56), (103, 223, 204), (239, 249, 255)
    root2 = 2 ** 0.5
    def octagon(x, y, r):
        ax, ay = abs(x), abs(y)
        return max(ax, ay, (ax + ay) / root2) - r
    def box(x, y, cx, cy, hw, hh, r):
        qx, qy = abs(x - cx) - hw + r, abs(y - cy) - hh + r
        return (max(qx, 0) ** 2 + max(qy, 0) ** 2) ** 0.5 + min(max(qx, qy), 0) - r
    def disc(x, y, cx, cy, r):
        return ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 - r
    def smile(x, y):
        d = abs(((x ** 2 + (y - 0.005) ** 2) ** 0.5) - 0.085) - 0.016
        return d if y > 0.03 else 1.0
    px = 2.0 / size
    shapes = [
        (FILL, lambda x, y: octagon(x, y, 0.80)),
        (TEAL, lambda x, y: abs(octagon(x, y, 0.80)) - 0.035),
        (AMBER, lambda x, y: abs(octagon(x, y, 0.62)) - 0.05),
        (SUIT, lambda x, y: box(x, y, 0, 0.42, 0.25, 0.13, 0.07)),
        (VISOR, lambda x, y: box(x, y, 0, 0.27, 0.11, 0.05, 0.02)),
        (AMBER, lambda x, y: box(x, y, 0, -0.02, 0.33, 0.25, 0.09)),
        (AMBER, lambda x, y: box(x, y, 0, -0.34, 0.016, 0.09, 0.016)),
        (AMBER, lambda x, y: disc(x, y, 0, -0.44, 0.058)),
        (HIGHLIGHT, lambda x, y: disc(x, y, -0.016, -0.456, 0.018)),
        (VISOR, lambda x, y: box(x, y, 0, -0.02, 0.25, 0.155, 0.055)),
        (MINT, lambda x, y: disc(x, y, -0.115, -0.055, 0.042)),
        (MINT, lambda x, y: disc(x, y, 0.115, -0.055, 0.042)),
        (MINT, smile),
    ]
    raw = bytearray()
    for j in range(size):
        raw.append(0)
        y0 = (j + 0.5) * px - 1
        for i in range(size):
            x0 = (i + 0.5) * px - 1
            x, y = x0 / content, y0 / content
            r, g, b = BG
            for color, sdf in shapes:
                cover = 0.5 - sdf(x, y) * content / px
                if cover <= 0:
                    continue
                cover = min(1.0, cover)
                r += (color[0] - r) * cover
                g += (color[1] - g) * cover
                b += (color[2] - b) * cover
            alpha = 255
            if rounded:
                alpha = int(255 * max(0.0, min(1.0, 0.5 - box(x0, y0, 0, 0, 1, 1, 0.22) / px)))
            raw.extend((int(r), int(g), int(b), alpha))
    def chunk(tag, data):
        return struct.pack('!I', len(data)) + tag + data + struct.pack('!I', zlib.crc32(tag + data) & 0xffffffff)
    ihdr = struct.pack('!IIBBBBB', size, size, 8, 6, 0, 0, 0)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) + chunk(b'IEND', b'')
    path.write_bytes(png)

def main():
    PWA.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)
    for wav in (ROOT / 'assets').glob('*.wav'):
        shutil.copy2(wav, ASSETS / wav.name)
    make_icon(PWA / 'icon-192.png', 192, rounded=True)
    make_icon(PWA / 'icon-512.png', 512, rounded=True)
    make_icon(PWA / 'icon-maskable-192.png', 192, content=0.8)
    make_icon(PWA / 'icon-maskable-512.png', 512, content=0.8)
    make_icon(PWA / 'apple-touch-icon.png', 180, content=0.88)

    java_home = os.environ.get('JAVA_HOME', '')
    java_exe = str(Path(java_home) / 'bin' / 'java.exe') if java_home else 'java'
    out = subprocess.check_output([java_exe, '-Dfile.encoding=UTF-8', '-cp', str(ROOT / 'build' / 'classes'), 'com.yorunge.runner.ExportData'])
    (PWA / 'levels_data.js').write_bytes(out)
    # Content hash invalidates the offline cache whenever the game changes.
    icons = b''.join((PWA / name).read_bytes() for name in ICONS)
    stamp = hashlib.sha256(out + (PWA / 'game.js').read_bytes() + icons + Path(__file__).read_bytes()).hexdigest()[:12]

    (PWA / 'manifest.json').write_text('''{
  "id": "./",
  "name": "Yörünge Koşusu",
  "short_name": "Yörünge Koşusu",
  "lang": "tr",
  "scope": "./",
  "description": "Özgün çevrimdışı 3B tünel koşusu",
  "start_url": "./index.html",
  "display": "fullscreen",
  "orientation": "landscape",
  "background_color": "#081722",
  "theme_color": "#081722",
  "icons": [
    { "src": "icon-192.png", "sizes": "192x192", "type": "image/png", "purpose": "any" },
    { "src": "icon-512.png", "sizes": "512x512", "type": "image/png", "purpose": "any" },
    { "src": "icon-maskable-192.png", "sizes": "192x192", "type": "image/png", "purpose": "maskable" },
    { "src": "icon-maskable-512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable" }
  ]
}
''', encoding='utf-8')

    (PWA / 'sw.js').write_text('''const CACHE = 'yorunge-STAMP';
const FILES = [
  './', './index.html', './manifest.json', './levels_data.js?v=STAMP', './game.js?v=STAMP',
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
'''.replace('STAMP', stamp), encoding='utf-8')

    (PWA / 'index.html').write_text('''<!DOCTYPE html>
<html lang="tr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="Yörünge Koşusu">
  <link rel="icon" href="icon-192.png">
  <link rel="apple-touch-icon" href="apple-touch-icon.png">
  <link rel="manifest" href="manifest.json">
  <title>Yörünge Koşusu</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; -webkit-user-select: none; -webkit-touch-callout: none; }
    html, body { width: 100%; height: 100%; overflow: hidden; background: #081722; touch-action: none; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
    canvas { display: block; position: fixed; inset: 0; width: 100%; height: 100%; }
    #rotate-notice { display: none; position: fixed; inset: 0; background: #081722; color: #e9f6f3; align-items: center; justify-content: center; text-align: center; padding: 24px; font-size: 20px; z-index: 10; }
    @media (orientation: portrait) { #rotate-notice { display: flex; } }
    #safe-probe { position: fixed; width: 0; height: 0; visibility: hidden; padding: 0 env(safe-area-inset-right) env(safe-area-inset-bottom) env(safe-area-inset-left); }
  </style>
</head>
<body>
  <div id="rotate-notice">Lütfen cihazınızı yatay (landscape) konuma çevirin.</div>
  <div id="safe-probe"></div>
  <canvas id="game"></canvas>
  <script src="levels_data.js?v=STAMP" charset="utf-8"></script>
  <script src="game.js?v=STAMP" charset="utf-8"></script>
  <script>
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').then(reg => {
        document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') reg.update().catch(() => {}); });
      }).catch(() => {}));
    }
  </script>
</body>
</html>
'''.replace('STAMP', stamp), encoding='utf-8')
    print('Synchronized PWA files in pwa/')

if __name__ == '__main__':
    main()
