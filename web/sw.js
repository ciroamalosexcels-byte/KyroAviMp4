/* KyroAviMp4: funciona sin conexión y se actualiza sola.
   La app propia va "primero la red" (siempre la última versión si hay internet);
   el motor FFmpeg del CDN se guarda la primera vez que se usa. */
const CACHE = 'kyroavimp4-v2';
const SHELL = ['./', 'index.html', 'app.js', 'manifest.webmanifest', 'fonts/archivo.css',
  'fonts/k3kPo8UDI-1M0wlSV9XAw6lQkqWY8Q82sLyTOxK-vA.woff2', 'fonts/k3kPo8UDI-1M0wlSV9XAw6lQkqWY8Q82sLydOxI.woff2',
  'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon.svg'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin === location.origin) {
    e.respondWith(fetch(req).then(r => { if (r.ok) { const copy = r.clone(); caches.open(CACHE).then(c => c.put(req, copy)); } return r; })
      .catch(() => caches.match(req).then(r => r || caches.match('index.html'))));
  } else if (url.hostname === 'cdn.jsdelivr.net' && /@ffmpeg\//.test(url.pathname)) {
    e.respondWith(caches.match(req).then(hit => hit || fetch(req).then(r => { if (r.ok) { const copy = r.clone(); caches.open(CACHE).then(c => c.put(req, copy)); } return r; })));
  }
});
