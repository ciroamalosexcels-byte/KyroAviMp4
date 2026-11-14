/* KyroAviMp4 — prototipo web.
   Videos: grilla de a 3 con miniaturas, ventana emergente de vista previa, conversión a MP4.
   Editor: preview instantánea (los originales se reproducen y se buscan en vivo, sin re-render),
   recorte desde los bordes del bloque, reordenar con pulsación larga, música, color y exportación. */
(() => {
'use strict';

/* ================= íconos (set de KyroNeoBoard, estilo Lucide) ================= */
const ICONS = {
  plus: '<path d="M12 5v14M5 12h14"/>',
  trash: '<path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>',
  undo: '<path d="M3 7v6h6"/><path d="M21 17a9 9 0 0 0-15-6.7L3 13"/>',
  redo: '<path d="M21 7v6h-6"/><path d="M3 17a9 9 0 0 1 15-6.7L21 13"/>',
  moon: '<path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9z"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M6.3 17.7l-1.4 1.4M19.1 4.9l-1.4 1.4"/>',
  check: '<path d="M20 6L9 17l-5-5"/>',
  x: '<path d="M18 6L6 18M6 6l12 12"/>',
  play: '<path d="m9 5 11 7-11 7V5Z"/>',
  pause: '<rect x="6" y="4.5" width="4" height="15" rx="1"/><rect x="14" y="4.5" width="4" height="15" rx="1"/>',
  skipBack: '<path d="M19 20 9 12l10-8z"/><path d="M5 19V5"/>',
  skipFwd: '<path d="m5 4 10 8-10 8z"/><path d="M19 5v14"/>',
  cut: '<circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M20 4L8.1 15.9M14.5 14.5L20 20M8.1 8.1L12 12"/>',
  dup: '<rect x="8" y="8" width="13" height="13" rx="2"/><path d="M4 16V5a1 1 0 0 1 1-1h11"/><path d="M14.5 11.5v6M11.5 14.5h6"/>',
  sliders: '<path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/>',
  zoomIn: '<circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3M11 8v6M8 11h6"/>',
  zoomOut: '<circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3M8 11h6"/>',
  magnet: '<path d="M6 15l-4-4 6.75-6.77a7.79 7.79 0 0 1 11 11L13 22l-4-4 6.39-6.36a2.14 2.14 0 0 0-3-3L6 15"/><path d="M5 8l4 4M12 15l4 4"/>',
  crop: '<path d="M6 2v14a2 2 0 0 0 2 2h14"/><path d="M18 22V8a2 2 0 0 0-2-2H2"/>',
  grid: '<rect x="3" y="3" width="5" height="5" rx="1"/><rect x="9.5" y="3" width="5" height="5" rx="1"/><rect x="16" y="3" width="5" height="5" rx="1"/><rect x="3" y="9.5" width="5" height="5" rx="1"/><rect x="9.5" y="9.5" width="5" height="5" rx="1"/><rect x="16" y="9.5" width="5" height="5" rx="1"/><rect x="3" y="16" width="5" height="5" rx="1"/><rect x="9.5" y="16" width="5" height="5" rx="1"/>',
  timeline: '<rect x="2" y="5" width="9" height="6" rx="1"/><rect x="13" y="5" width="9" height="6" rx="1"/><rect x="5" y="14" width="13" height="5" rx="1"/><path d="M12 2v20"/>',
  convert: '<path d="M21 12a9 9 0 0 1-15.5 6.2L3 16"/><path d="M3 12A9 9 0 0 1 18.5 5.8L21 8"/><path d="M21 3v5h-5M3 21v-5h5"/>',
  film: '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M7 3v18M17 3v18M3 7.5h4M3 12h18M3 16.5h4M17 7.5h4M17 16.5h4"/>',
  music: '<path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/>',
  volume: '<path d="M11 5 6 9H2v6h4l5 4z"/><path d="M15.5 8.5a5 5 0 0 1 0 7M19 5a10 10 0 0 1 0 14"/>',
  download: '<path d="M12 3v12M7 10l5 5 5-5"/><path d="M5 21h14"/>',
  eye: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  reset: '<path d="M3 12a9 9 0 1 0 9-9c-2.5 0-4.9 1-6.7 2.7L3 8"/><path d="M3 3v5h5"/>',
  gear: '<path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/>',
  checkAll: '<path d="M18 6 7 17l-5-5"/><path d="m22 10-7.5 7.5L13 16"/>',
  type: '<path d="M4 7V5h16v2M9 19h6M12 5v14"/>',
  palette: '<circle cx="13.5" cy="6.5" r="1.6" fill="currentColor"/><circle cx="17.5" cy="10.5" r="1.6" fill="currentColor"/><circle cx="8.5" cy="7.5" r="1.6" fill="currentColor"/><circle cx="6.5" cy="12.5" r="1.6" fill="currentColor"/><path d="M12 2a10 10 0 0 0 0 20c1 0 1.7-.8 1.7-1.7 0-.4-.2-.8-.4-1.1-.3-.3-.4-.7-.4-1.1 0-.9.8-1.7 1.7-1.7h2A5.6 5.6 0 0 0 22 11c0-5-4.5-9-10-9z"/>',
  fade: '<path d="M3 20L12 4l9 16"/><path d="M3 20h18" stroke-dasharray="2 3"/>',
  expand: '<path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/>',
  shrink: '<path d="M4 14h6v6M20 10h-6V4M14 10l7-7M3 21l7-7"/>',
  volumeX: '<path d="M11 5 6 9H2v6h4l5 4z"/><path d="M22 9l-6 6M16 9l6 6"/>',
  eyeOff: '<path d="M9.9 4.2A10 10 0 0 1 12 4c6.5 0 10 8 10 8a17 17 0 0 1-2.2 3.2"/><path d="M6.6 6.6A17 17 0 0 0 2 12s3.5 7 10 7a10 10 0 0 0 5.4-1.6"/><path d="M2 2l20 20"/><path d="M14.1 14.1a3 3 0 0 1-4.2-4.2"/>',
  layers: '<path d="M12 2l10 5-10 5L2 7z"/><path d="M2 12l10 5 10-5"/><path d="M2 17l10 5 10-5"/>',
};
const icon = n => `<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${ICONS[n] || ''}</svg>`;
const $ = id => document.getElementById(id);
const paintIcons = (root = document) => root.querySelectorAll('[data-ico]').forEach(el => {
  if (!el.querySelector(':scope > svg.ico')) el.insertAdjacentHTML('afterbegin', icon(el.dataset.ico));
});
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const uid = () => Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-4);
const clamp = (v, a, b) => Math.min(b, Math.max(a, v));
const even = n => Math.max(2, Math.round(n / 2) * 2);
const buzz = ms => { try { navigator.vibrate && navigator.vibrate(ms); } catch (_) {} };
const fmtT = s => { s = Math.max(0, s || 0); const m = Math.floor(s / 60); const r = s - m * 60; return `${String(m).padStart(2, '0')}:${r.toFixed(1).padStart(4, '0')}`; };
const fmtShort = s => { if (!isFinite(s) || s <= 0) return '—'; const m = Math.floor(s / 60), r = Math.round(s % 60); return m ? `${m}:${String(r).padStart(2, '0')}` : `${(s).toFixed(s < 10 ? 1 : 0)} s`; };
const fmtBytes = b => b > 1e9 ? (b / 1e9).toFixed(2) + ' GB' : b > 1e6 ? (b / 1e6).toFixed(1) + ' MB' : Math.max(1, Math.round(b / 1e3)) + ' KB';
const baseName = n => n.replace(/\.[^.]+$/, '').replace(/[^\w\-áéíóúñÁÉÍÓÚÑ ]+/g, '_').trim() || 'video';
const isAviName = n => /\.avi$/i.test(n);
let toastT = 0;
const toast = msg => { const t = $('toast'); t.textContent = msg; t.classList.remove('show'); void t.offsetWidth; t.classList.add('show'); clearTimeout(toastT); toastT = setTimeout(() => t.classList.remove('show'), 2800); };

/* ================= preferencias ================= */
const PREF_KEY = 'kyroavimp4.prefs';
const prefs = Object.assign({ width: 0, aspect: 'original', night: false, snap: true }, readJSON(PREF_KEY));
if (!prefs.v2) { if (prefs.width === 1010) prefs.width = 0; prefs.v2 = true; }
function readJSON(k) { try { return JSON.parse(localStorage.getItem(k) || 'null') || {}; } catch (_) { return {}; } }
function savePrefs() { try { localStorage.setItem(PREF_KEY, JSON.stringify(prefs)); } catch (_) {} }
const ASPECTS = [
  { id: 'original', label: 'Original', r: null },
  { id: '1:1', label: '1:1', r: 1 },
  { id: '3:4', label: '3:4', r: 3 / 4 },
  { id: '4:3', label: '4:3', r: 4 / 3 },
  { id: '16:9', label: '16:9', r: 16 / 9 },
];
const aspectOf = id => ASPECTS.find(a => a.id === id) || ASPECTS[0];
function outSize(srcW, srcH) {
  const W = even(prefs.width || srcW || 1280);
  const a = aspectOf(prefs.aspect);
  if (a.r) return { W, H: even(W / a.r) };
  if (srcW && srcH) return { W, H: even(W * srcH / srcW) };
  return { W, H: 0 };
}

/* ================= IndexedDB (los archivos sobreviven a recargar) ================= */
const DB = (() => {
  let dbp = null;
  const open = () => dbp || (dbp = new Promise((res, rej) => {
    if (!('indexedDB' in window)) return rej(new Error('sin IndexedDB'));
    const r = indexedDB.open('kyroavimp4', 1);
    r.onupgradeneeded = () => { r.result.createObjectStore('items', { keyPath: 'id' }); r.result.createObjectStore('blobs'); };
    r.onsuccess = () => res(r.result); r.onerror = () => rej(r.error);
  }));
  const tx = async (store, mode, fn) => { try { const db = await open(); return await new Promise((res, rej) => { const t = db.transaction(store, mode); const s = t.objectStore(store); const out = fn(s); t.oncomplete = () => res(out && out.result); t.onerror = () => rej(t.error); }); } catch (e) { console.warn('IndexedDB', e); return null; } };
  return {
    putItem: it => tx('items', 'readwrite', s => s.put(it)),
    delItem: id => tx('items', 'readwrite', s => s.delete(id)),
    allItems: () => tx('items', 'readonly', s => s.getAll()),
    putBlob: (k, b) => tx('blobs', 'readwrite', s => s.put(b, k)),
    getBlob: k => tx('blobs', 'readonly', s => s.get(k)),
    delBlob: k => tx('blobs', 'readwrite', s => s.delete(k)),
  };
})();

/* ================= biblioteca ================= */
const lib = { items: [], picked: new Set() };
const urlCache = new Map(); // blob -> objectURL
const urlFor = blob => { if (!blob) return ''; let u = urlCache.get(blob); if (!u) { u = URL.createObjectURL(blob); urlCache.set(blob, u); } return u; };
const itemById = id => lib.items.find(i => i.id === id);
const editSource = it => it && ((it.playable ? it.file : (it.proxy || it.out)) || null);
/* sin el ícono gris de play que pone Android/Chrome sobre los videos */
const NOPOSTER = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
const fullSource = it => it && (it.out || (it.playable ? it.file : null));
const persistItem = it => DB.putItem({ id: it.id, name: it.name, size: it.size, avi: it.avi, file: it.file, out: it.out || null, proxy: it.proxy || null, dur: it.dur, w: it.w, h: it.h,
  thumb: it.thumb || '', status: it.status === 'converting' || it.status === 'queued' ? 'idle' : it.status, playable: it.playable, err: it.err || '', outDur: it.outDur || 0 });

function probe(blob) {
  return new Promise(res => {
    const v = document.createElement('video');
    v.muted = true; v.preload = 'auto'; v.playsInline = true;
    let done = false;
    const finish = r => { if (done) return; done = true; clearTimeout(to); v.removeAttribute('src'); v.load(); res(r); };
    const to = setTimeout(() => finish({ ok: false }), 7000);
    v.onerror = () => finish({ ok: false });
    v.onloadeddata = async () => {
      const dur = await realDuration(v);
      const w = v.videoWidth, h = v.videoHeight;
      if (!w || !h) return finish({ ok: false });
      let thumb = '';
      try { thumb = await grab(v, Math.min(dur * 0.2, 1.5), 320); } catch (_) {}
      finish({ ok: true, dur, w, h, thumb });
    };
    v.src = urlFor(blob);
  });
}
/* algunos WebM no informan la duración hasta buscar al final */
async function realDuration(v) {
  if (isFinite(v.duration) && v.duration > 0) return v.duration;
  await new Promise(res => { const to = setTimeout(res, 3000); v.addEventListener('durationchange', function f() { if (isFinite(v.duration)) { clearTimeout(to); v.removeEventListener('durationchange', f); res(); } }); v.currentTime = 1e7; });
  const d = isFinite(v.duration) ? v.duration : 0; v.currentTime = 0; return d;
}
function seekTo(v, t) {
  return new Promise((res, rej) => {
    const to = setTimeout(() => rej(new Error('seek')), 4000);
    v.addEventListener('seeked', () => { clearTimeout(to); res(); }, { once: true });
    v.currentTime = Math.max(0, t);
  });
}
async function grab(v, t, size, q = 0.72) {
  await seekTo(v, t);
  const s = size / Math.max(v.videoWidth, v.videoHeight);
  const c = document.createElement('canvas');
  c.width = Math.max(2, Math.round(v.videoWidth * s)); c.height = Math.max(2, Math.round(v.videoHeight * s));
  c.getContext('2d').drawImage(v, 0, 0, c.width, c.height);
  return c.toDataURL('image/jpeg', q);
}

async function addFiles(files, { toEditor = false } = {}) {
  const list = [...files].filter(f => f && (f.type.startsWith('video/') || /\.(avi|mkv|mov|mp4|webm|m4v|3gp)$/i.test(f.name)));
  if (!list.length) { toast('Esos archivos no parecen videos.'); return []; }
  const added = [];
  for (const f of list) {
    const it = { id: uid(), name: f.name, size: f.size, avi: isAviName(f.name), file: f, out: null, dur: 0, w: 0, h: 0, thumb: '', status: 'probing', playable: false, progress: 0 };
    lib.items.push(it); added.push(it);
  }
  renderGrid();
  for (const it of added) {
    const p = await probe(it.file);
    if (p.ok) Object.assign(it, { playable: true, dur: p.dur, w: p.w, h: p.h, thumb: p.thumb });
    it.status = 'idle';
    persistItem(it);
    renderGrid();
    if (!p.ok) queuePreview(it); // AVI: se arma la vista previa completa solo
  }
  toast(added.length === 1 ? 'Video agregado' : `${added.length} videos agregados`);
  if (toEditor) await sendToEditor(added);
  return added;
}

/* ---------- grilla de a 3 ---------- */
function statusLine(it) {
  if (it.status === 'probing') return 'Leyendo…';
  if (it.status === 'queued') return 'En cola';
  if (it.status === 'converting') return `Convirtiendo ${Math.round(it.progress * 100)} %`;
  if (it.status === 'preview') return `Vista previa ${Math.round(it.progress * 100)} %`;
  if (it.status === 'error') return it.err || 'No se pudo convertir';
  if (it.out) return `MP4 listo · ${fmtBytes(it.out.size)}`;
  return `${fmtBytes(it.size)}${it.w ? ` · ${it.w}×${it.h}` : ''}`;
}
/* cada video es una foto impresa apoyada en la mesa: leve giro según su id */
const tilt = id => { let h = 0; for (const ch of id) h = (h * 31 + ch.charCodeAt(0)) | 0; return ((Math.abs(h) % 7) - 3) * 0.45; };
function renderGrid() {
  const g = $('grid');
  const add = `<button class="card add" id="addTile" style="--r:${lib.items.length ? -1.2 : 0}deg"><span class="th"><span class="plus">${icon('plus')}</span></span><span class="nm">Sumar videos</span></button>`;
  g.innerHTML = add + lib.items.map(it => {
    const st = it.status;
    return `<button class="card${lib.picked.has(it.id) ? ' picked' : ''}" data-id="${it.id}" style="--r:${tilt(it.id)}deg">
      <span class="th">${it.thumb ? `<img src="${it.thumb}" alt="">` : `<span class="noimg">${icon('film')}</span>`}
        ${it.out ? `<span class="done-dot" title="MP4 listo">${icon('check')}</span>` : ''}
        <span class="sel" data-pick="${it.id}">${icon('check')}</span>
        ${it.dur ? `<span class="dur">${fmtShort(it.outDur || it.dur)}</span>` : ''}
        ${st === 'converting' || st === 'preview' ? `<span class="busy">${Math.round(it.progress * 100)} %</span><span class="bar"><i style="width:${it.progress * 100}%"></i></span>` : ''}
        ${st === 'queued' ? '<span class="busy">en cola</span>' : ''}
        ${st === 'probing' ? '<span class="busy">…</span>' : ''}
        ${st === 'error' ? `<span class="err-dot" title="${esc(it.err || 'Error')}">!</span>` : ''}
      </span>
      <span class="nm">${esc(baseName(it.name))}</span>
    </button>`;
  }).join('');
  const n = lib.items.length, p = lib.picked.size;
  $('libCount').textContent = n ? String(n) : '';
  $('actbar').hidden = !n;
  $('pickAllBtn').classList.toggle('on', !!p && p === n);
  $('pickAllBtn').title = p && p === n ? 'Ninguno' : 'Elegir todos';
  const target = targets();
  $('removeBtn').disabled = !p || engine.converting;
  $('toEditorBtn').disabled = !target.length || engine.converting;
  const conv = target.filter(i => !i.out);
  $('convertBtn').disabled = !conv.length || engine.converting;
  $('convertN').textContent = conv.length > 1 ? conv.length : ''; $('convertN').hidden = conv.length < 2;
  $('navN').hidden = !n; $('navN').textContent = n;
}
const targets = () => lib.picked.size ? lib.items.filter(i => lib.picked.has(i.id)) : lib.items.slice();

$('grid').addEventListener('click', e => {
  if (e.target.closest('#addTile')) { $('fileIn').click(); return; }
  const pick = e.target.closest('[data-pick]');
  const card = e.target.closest('.card');
  if (!card) return;
  const id = card.dataset.id;
  if (pick) { lib.picked.has(id) ? lib.picked.delete(id) : lib.picked.add(id); buzz(6); renderGrid(); return; }
  openPreview(id);
});
$('pickAllBtn').onclick = () => { if (lib.picked.size === lib.items.length) lib.picked.clear(); else lib.items.forEach(i => lib.picked.add(i.id)); renderGrid(); };
$('removeBtn').onclick = () => { [...lib.picked].forEach(removeItem); lib.picked.clear(); renderGrid(); };
$('toEditorBtn').onclick = () => sendToEditor(targets());
$('convertBtn').onclick = () => convertItems(targets().filter(i => !i.out));
function removeItem(id) {
  const i = lib.items.findIndex(x => x.id === id); if (i < 0) return;
  if (project.clips.some(c => c.assetId === id) || project.layers.some(l => l.items.some(c => c.assetId === id))) {
    toast('También se quitó del editor');
    commit(() => { project.clips = project.clips.filter(c => c.assetId !== id); project.layers.forEach(l => { l.items = l.items.filter(c => c.assetId !== id); }); });
  }
  lib.items.splice(i, 1); DB.delItem(id);
}

/* ---------- salida: ancho y proporción (se ve como chip, se edita en Configuración) ---------- */
function outLabel() {
  const ref = lib.items.find(i => i.w) || {};
  const { W, H } = outSize(ref.w, ref.h);
  if (!prefs.width && !ref.w) return `Ancho original · ${prefs.aspect === 'original' ? 'Original' : prefs.aspect}`;
  return `${W}${H ? '×' + H : ''} · ${prefs.aspect === 'original' ? 'Original' : prefs.aspect}`;
}
function renderOut() {
  $('outChipTx').textContent = outLabel();
}
$('outChip').onclick = () => openFormat();

/* ---------- agregar / soltar ---------- */
$('fileIn').onchange = e => { addFiles(e.target.files); e.target.value = ''; };
const libEl = $('lib');
libEl.addEventListener('dragover', e => { e.preventDefault(); libEl.classList.add('drop'); });
libEl.addEventListener('dragleave', e => { if (e.target === libEl) libEl.classList.remove('drop'); });
libEl.addEventListener('drop', e => { e.preventDefault(); libEl.classList.remove('drop'); addFiles(e.dataTransfer.files); });

/* ================= ventana emergente de vista previa ================= */
let modalId = null;
function openPreview(id) {
  const it = itemById(id); if (!it) return;
  modalId = id;
  $('mName').textContent = it.name;
  $('mType').textContent = it.avi ? 'AVI' : 'Video'; $('mType').className = 'chip' + (it.avi ? ' red' : '');
  renderModal();
  $('ov').classList.add('show');
}
function renderModal() {
  const it = itemById(modalId); if (!it) return closePreview();
  const src = editSource(it);
  const stage = $('mStage');
  const cur = stage.querySelector('video');
  if (src) {
    if (!cur || cur.dataset.src !== urlFor(src)) {
      stage.innerHTML = `<video playsinline autoplay poster="${NOPOSTER}" data-src="${urlFor(src)}" src="${urlFor(src)}"></video>`;
      wirePlayer(stage.querySelector('video'));
    }
    $('mCtrl').hidden = false;
  } else {
    $('mCtrl').hidden = true;
    const err = it.status === 'error';
    stage.innerHTML = `<div class="m-msg">${icon('film')}<span>${err ? 'No se pudo leer este video' : `Preparando vista previa ${Math.round((it.progress || 0) * 100)} %`}</span>
      ${err ? `<button class="round" id="mRetry" title="Reintentar">${icon('reset')}</button>` : `<span class="track m-track"><i style="width:${(it.progress || 0) * 100}%"></i></span>`}</div>`;
    const b = $('mRetry'); if (b) b.onclick = () => { it.status = 'idle'; queuePreview(it); renderModal(); };
  }
  const info = [];
  if (it.w) info.push(`${it.w} × ${it.h} px`);
  if (it.dur) info.push(fmtShort(it.dur));
  info.push(fmtBytes(it.size));
  if (it.out) info.push(`MP4 ${fmtBytes(it.out.size)}`);
  $('mInfo').innerHTML = info.map(t => `<span>${esc(t)}</span>`).join('');
  const acts = [];
  acts.push(`<button class="round" data-m="remove" title="Quitar">${icon('trash')}</button>`);
  if (it.out) acts.push(`<button class="round" data-m="download" title="Descargar MP4">${icon('download')}</button>`);
  else acts.push(`<button class="round" data-m="convert" title="Convertir a MP4" ${engine.converting ? 'disabled' : ''}>${icon('convert')}</button>`);
  acts.push(`<span class="spacer"></span><button class="stamp ic" data-m="editor" title="Al editor" ${!editSource(it) ? 'disabled' : ''}>${icon('timeline')}</button>`);
  $('mActions').innerHTML = acts.join('');
}
/* reproductor propio de la ventana: controles debajo del video, no encima */
function wirePlayer(v) {
  const btn = $('mPlay'), seek = $('mSeek'), tm = $('mTime');
  const sync = () => { btn.innerHTML = icon(v.paused ? 'play' : 'pause'); if (!seek._drag && v.duration) seek.value = Math.round(v.currentTime / v.duration * 1000); tm.textContent = `${fmtShort(v.currentTime) === '—' ? '0.0 s' : fmtShort(v.currentTime)} / ${fmtShort(v.duration)}`; };
  ['play', 'pause', 'timeupdate', 'loadedmetadata', 'ended'].forEach(ev => v.addEventListener(ev, sync));
  btn.onclick = () => v.paused ? v.play().catch(() => {}) : v.pause();
  v.onclick = btn.onclick;
  seek.oninput = () => { seek._drag = true; if (v.duration) v.currentTime = seek.value / 1000 * v.duration; };
  seek.onchange = () => { seek._drag = false; };
  sync();
}
function closePreview() {
  $('ov').classList.remove('show'); modalId = null;
  const v = $('mStage').querySelector('video'); if (v) v.pause();
  $('mStage').innerHTML = '';
}
$('mClose').onclick = closePreview;
$('ov').addEventListener('click', e => { if (e.target === $('ov')) closePreview(); });
$('mActions').addEventListener('click', e => {
  const b = e.target.closest('[data-m]'); if (!b) return;
  const it = itemById(modalId); if (!it) return;
  const a = b.dataset.m;
  if (a === 'remove') { removeItem(it.id); lib.picked.delete(it.id); closePreview(); renderGrid(); }
  if (a === 'download') downloadBlob(it.out, baseName(it.name) + '_mp4.mp4');
  if (a === 'convert') { convertItems([it]); renderModal(); }
  if (a === 'editor') { closePreview(); sendToEditor([it]); }
});
document.addEventListener('keydown', e => { if (e.key === 'Escape') { if ($('ov').classList.contains('show')) closePreview(); else closeSheet(); } });
function downloadBlob(blob, name) {
  if (window.KyroAndroid) { saveAndroid(blob, name); return; }
  const a = document.createElement('a'); a.href = urlFor(blob); a.download = name; document.body.appendChild(a); a.click(); a.remove();
}
/* en la app Android: se guarda en Descargas/KyroAviMp4 por partes */
async function saveAndroid(blob, name) {
  try {
    const id = window.KyroAndroid.saveBegin(name, blob.type || 'video/mp4');
    if (!id) throw new Error('sin permiso');
    const CH = 768 * 1024;
    for (let o = 0; o < blob.size; o += CH) {
      const b64 = await new Promise((res, rej) => { const r = new FileReader(); r.onload = () => res(String(r.result).split(',')[1] || ''); r.onerror = rej; r.readAsDataURL(blob.slice(o, o + CH)); });
      if (!window.KyroAndroid.saveChunk(id, b64)) throw new Error('escritura');
    }
    const where = window.KyroAndroid.saveEnd(id);
    toast(where ? 'Guardado en ' + where : 'No se pudo guardar');
  } catch (e) { console.warn(e); toast('No se pudo guardar el archivo'); }
}

/* ================= motor FFmpeg (solo para convertir y exportar) ================= */
const CORE = 'https://cdn.jsdelivr.net/npm/@ffmpeg/core-st@0.11.1/dist/ffmpeg-core.js';
const engine = { ff: null, busy: false, cancel: false, onProgress: null, log: [] };
async function getFF() {
  if (engine.ff) return engine.ff;
  if (!window.FFmpeg || !window.FFmpeg.createFFmpeg) throw new Error('No se pudo cargar FFmpeg. Revisá la conexión (la primera vez se descarga).');
  const ff = window.FFmpeg.createFFmpeg({ log: false, corePath: CORE, mainName: 'main' });
  ff.setProgress(({ ratio }) => engine.onProgress && isFinite(ratio) && engine.onProgress(clamp(ratio, 0, 1)));
  ff.setLogger(({ message }) => { engine.log.push(message); if (engine.log.length > 400) engine.log.shift(); });
  setProg(0.02, 'Iniciando el motor de video (la primera vez tarda)…');
  await ff.load();
  engine.ff = ff;
  return ff;
}
function killFF() { try { engine.ff && engine.ff.exit(); } catch (_) {} engine.ff = null; }
const okExit = e => e && (Number(e.status) === 0 || /exit\s*\(\s*0\s*\)/i.test(e.message || String(e)));
async function run(ff, args) { try { await ff.run(...args); } catch (e) { if (!okExit(e)) throw e; } }
function readOut(ff, name) { const d = ff.FS('readFile', name); if (!d || !d.length) throw new Error('FFmpeg no generó el archivo.'); return d; }
const rm = (ff, n) => { try { ff.FS('unlink', n); } catch (_) {} };
function setProg(r, txt) {
  $('prog').hidden = false; $('progBar').style.width = (r * 100).toFixed(1) + '%'; if (txt) $('progTxt').textContent = txt;
}
const hideProg = () => { $('prog').hidden = true; };
$('cancelBtn').onclick = () => { engine.cancel = true; killFF(); if (NATIVE) try { window.KyroAndroid.cancel(); } catch (_) {} toast('Cancelado'); };

/* en la APK, FFmpeg nativo del celular: lee AVI y es mucho más rápido */
const NATIVE = !!(window.KyroAndroid && window.KyroAndroid.process);
const fileKey = it => `${it.name}|${it.size}`;
const canNative = it => { try { return NATIVE && window.KyroAndroid.canProcess(fileKey(it)); } catch (_) { return false; } };
const nativeJobs = new Map();
window.kyroNative = (type, job, data) => {
  const j = nativeJobs.get(job); if (!j) return;
  if (type === 'progress') { if (j.onProgress) j.onProgress(clamp(+data || 0, 0, 1)); }
  else if (type === 'info') { try { j.info = JSON.parse(data); } catch (_) {} }
  else if (type === 'done') {
    nativeJobs.delete(job);
    fetch(data).then(r => { if (!r.ok) throw new Error('lectura'); return r.blob(); })
      .then(b => { try { window.KyroAndroid.release(job); } catch (_) {} j.resolve({ blob: new Blob([b], { type: 'video/mp4' }), info: j.info }); })
      .catch(j.reject);
  } else if (type === 'error') { nativeJobs.delete(job); j.reject(new Error(data || 'error')); }
};
function nativeRun(it, mode, vf, onProgress) {
  return new Promise((resolve, reject) => {
    const job = 'j' + uid();
    nativeJobs.set(job, { resolve, reject, onProgress, info: null });
    if (!window.KyroAndroid.process(job, fileKey(it), mode, vf || '')) { nativeJobs.delete(job); reject(new Error('archivo no disponible')); }
  });
}
/* filtro de salida: ancho original o elegido, proporción sin recortar */
function convertVF(it) {
  const a = aspectOf(prefs.aspect);
  if (!prefs.width && !it.w) return a.r ? `scale=1280:${even(1280 / a.r)}:force_original_aspect_ratio=decrease,pad=1280:${even(1280 / a.r)}:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1` : 'scale=trunc(iw/2)*2:-2,setsar=1';
  const { W, H } = outSize(it.w, it.h);
  return a.r ? `scale=${W}:${H}:force_original_aspect_ratio=decrease,pad=${W}:${H}:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1` : `scale=${W}:-2,setsar=1`;
}
function convertArgs(inN, outN, it, noAudio) {
  return ['-fflags', '+genpts', '-err_detect', 'ignore_err', '-i', inN, '-map', '0:v:0', ...(noAudio ? ['-an'] : ['-map', '0:a:0?']),
    '-vf', convertVF(it), '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '23', '-pix_fmt', 'yuv420p',
    ...(noAudio ? [] : ['-c:a', 'aac', '-b:a', '128k']), '-movflags', '+faststart', '-max_muxing_queue_size', '2048', '-y', outN];
}
/* datos del original leídos del registro del motor web */
function infoFromLog() {
  const info = {};
  for (const l of engine.log) {
    const d = l.match(/Duration:\s*(\d+):(\d+):([\d.]+)/); if (d && !info.dur) info.dur = +d[1] * 3600 + +d[2] * 60 + +d[3];
    const v = l.match(/Video:.*?(\d{2,5})x(\d{2,5})/); if (v && !info.w) { info.w = +v[1]; info.h = +v[2]; }
  }
  return info;
}
async function wasmRun(it, argsFor, onProgress) {
  while (engine.busy) await new Promise(r => setTimeout(r, 400));
  engine.busy = true;
  const ext = (it.name.match(/\.[^.]+$/) || ['.avi'])[0].toLowerCase(), inN = 'in' + ext, outN = 'out.mp4';
  try {
    let ff = await getFF();
    const bytes = new Uint8Array(await it.file.arrayBuffer());
    ff.FS('writeFile', inN, bytes);
    engine.log = []; engine.onProgress = onProgress;
    let data;
    try { await run(ff, argsFor(inN, outN, false)); data = readOut(ff, outN); }
    catch (e) {
      if (engine.cancel) throw e;
      killFF(); ff = await getFF(); ff.FS('writeFile', inN, bytes); engine.log = [];
      await run(ff, argsFor(inN, outN, true)); data = readOut(ff, outN);
    }
    const info = infoFromLog();
    rm(ff, inN); rm(ff, outN);
    return { blob: new Blob([data], { type: 'video/mp4' }), info };
  } catch (e) { killFF(); throw e; }
  finally { killFF(); engine.busy = false; engine.onProgress = null; hideProg(); }
}
const proxyArgs = (inN, outN, noAudio) => ['-fflags', '+genpts', '-err_detect', 'ignore_err', '-i', inN, '-map', '0:v:0', ...(noAudio ? ['-an'] : ['-map', '0:a:0?']),
  '-vf', "scale='min(640,iw)':-2,setsar=1", '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '30', '-pix_fmt', 'yuv420p', ...(noAudio ? [] : ['-c:a', 'aac', '-b:a', '96k']), '-y', outN];

/* vista previa completa de los AVI: se arma sola, de a uno */
const previewQ = []; let previewRunning = false;
function queuePreview(it) {
  if (!it || it.proxy || it.playable || it.out || previewQ.includes(it)) return;
  it.status = 'preview'; it.progress = 0; previewQ.push(it); renderGrid(); pumpPreview();
}
async function pumpPreview() {
  if (previewRunning) return; previewRunning = true;
  while (previewQ.length) {
    const it = previewQ.shift();
    if (!lib.items.includes(it) || it.out || it.proxy) continue;
    const onP = r => { it.progress = r; updateCard(it); if (modalId === it.id) renderModal(); };
    try {
      const { blob, info } = canNative(it) ? await nativeRun(it, 'proxy', '', onP) : await wasmRun(it, proxyArgs, onP);
      it.proxy = blob;
      const p = await probe(blob);
      if (p.ok) { it.thumb = p.thumb; it.outDur = p.dur; it.dur = (info && info.dur) || p.dur; it.w = (info && info.w) || it.w || p.w; it.h = (info && info.h) || it.h || p.h; }
      it.status = 'idle';
    } catch (e) { console.warn(e); it.status = 'error'; it.err = 'No se pudo leer'; }
    persistItem(it); renderGrid(); renderOut(); if (modalId === it.id) renderModal();
  }
  previewRunning = false;
}

async function convertItems(items) {
  items = items.filter(i => !i.out && i.status !== 'converting');
  if (!items.length || engine.converting) return [];
  engine.converting = true; engine.cancel = false;
  items.forEach(i => { i.status = 'queued'; i.err = ''; });
  renderGrid();
  const done = [];
  try {
    for (let k = 0; k < items.length && !engine.cancel; k++) {
      const it = items[k];
      it.status = 'converting'; it.progress = 0; renderGrid();
      const label = `Convirtiendo ${k + 1} de ${items.length}`;
      const onP = r => { it.progress = r; setProg((k + r) / items.length, label); updateCard(it); };
      setProg(k / items.length, label);
      try {
        const { blob, info } = canNative(it) ? await nativeRun(it, 'convert', convertVF(it), onP) : await wasmRun(it, (i, o, na) => convertArgs(i, o, it, na), onP);
        it.out = blob;
        if (info) { it.w = info.w || it.w; it.h = info.h || it.h; it.dur = info.dur || it.dur; }
        const p = await probe(it.out);
        if (p.ok) { it.outDur = p.dur; if (!it.dur) it.dur = p.dur; if (!it.thumb) it.thumb = p.thumb; if (!it.w) { it.w = p.w; it.h = p.h; } }
        it.status = 'done'; done.push(it);
      } catch (e) {
        it.status = engine.cancel ? 'idle' : 'error';
        it.err = engine.cancel ? '' : 'No se pudo convertir este archivo';
        console.warn(e);
        if (engine.cancel) break;
      }
      persistItem(it); renderGrid();
    }
    if (!engine.cancel) toast(done.length ? `Listo: ${done.length} MP4` : 'No se pudo convertir');
  } finally {
    engine.converting = false; hideProg();
    items.forEach(i => { if (i.status === 'queued' || i.status === 'converting') i.status = 'idle'; });
    renderGrid(); if (modalId) renderModal();
  }
  return done;
}
function updateCard(it) {
  const card = document.querySelector(`.card[data-id="${it.id}"]`); if (!card) return;
  const b = card.querySelector('.bar i'); if (b) b.style.width = (it.progress * 100) + '%';
  const s = card.querySelector('.busy'); if (s) s.textContent = Math.round(it.progress * 100) + ' %';
}

/* ================= editor: modelo, historial ================= */
/* pistas: V1 (secuencia magnética), capas de video V2.. (libres, se solapan),
   audios A1.. (una fila cada uno) y una fila de texto */
const PROJ_KEY = 'kyroavimp4.project';
const NEUTRAL = { bw: false, sat: 1, exp: 0, con: 0, hi: 0, sh: 0, temp: 0, tint: 0,
  wSh: { h: 30, a: 0, l: 0 }, wMid: { h: 45, a: 0, l: 0 }, wHi: { h: 200, a: 0, l: 0 } };
const newColor = () => JSON.parse(JSON.stringify(NEUTRAL));
const MIN_LEN = 0.2, MAX_LAYERS = 3, MAX_AUDIOS = 6;
function fixColor(c) {
  const o = Object.assign(newColor(), c || {});
  ['wSh', 'wMid', 'wHi'].forEach(k => { o[k] = Object.assign({ h: NEUTRAL[k].h, a: 0, l: 0 }, (c || {})[k] || {}); });
  if (c && c.shAmt) o.wSh = { h: c.shHue || 30, a: c.shAmt, l: 0 };
  if (c && c.hiAmt) o.wHi = { h: c.hiHue || 45, a: c.hiAmt, l: 0 };
  delete o.shAmt; delete o.hiAmt; delete o.shHue; delete o.hiHue;
  return o;
}
const fixVid = c => Object.assign({ fin: 0, fout: 0, vol: 1, op: 1 }, c, { color: fixColor(c.color) });
function loadProject() {
  const p = readJSON(PROJ_KEY);
  const pr = { clips: (p.clips || []).filter(c => c && c.assetId).map(fixVid), layers: (p.layers || []).map(l => ({ id: l.id || uid(), items: (l.items || []).map(fixVid) })),
    audios: p.audios || [], texts: p.texts || [], muted: p.muted || {}, hidden: p.hidden || {} };
  if (p.music) pr.audios.push(Object.assign({ key: 'music:' + p.music.id }, p.music));
  pr.audios.forEach(a => { a.key = a.key || 'audio:' + a.id; });
  return pr;
}
let project = loadProject();
const hist = { undo: [], redo: [] };
const snap = () => JSON.stringify(project);
function commit(fn) {
  const before = snap();
  fn();
  if (snap() === before) return;
  hist.undo.push(before); if (hist.undo.length > 80) hist.undo.shift();
  hist.redo.length = 0;
  saveProject(); onProjectChange();
}
function finishCommit(before) {
  if (snap() !== before) { hist.undo.push(before); hist.redo.length = 0; saveProject(); }
  onProjectChange();
}
function restore(s) { project = JSON.parse(s); project.audios.forEach(ensureAudio); saveProject(); onProjectChange(); }
function undo() { if (!hist.undo.length) return; hist.redo.push(snap()); restore(hist.undo.pop()); }
function redo() { if (!hist.redo.length) return; hist.undo.push(snap()); restore(hist.redo.pop()); }
function saveProject() { try { localStorage.setItem(PROJ_KEY, snap()); } catch (_) {} }
$('undoBtn').onclick = undo; $('redoBtn').onclick = redo;

const clipLen = c => Math.max(0, c.out - c.in);
const totalDur = () => project.clips.reduce((s, c) => s + clipLen(c), 0);
function startOf(i, clips = project.clips) { let s = 0; for (let k = 0; k < i; k++) s += clipLen(clips[k]); return s; }
function locate(t) {
  let s = 0; const cs = project.clips;
  for (let i = 0; i < cs.length; i++) { const L = clipLen(cs[i]); if (t < s + L - 1e-6 || (i === cs.length - 1 && t <= s + L)) return { i, clip: cs[i], start: s, local: cs[i].in + clamp(t - s, 0, L) }; s += L; }
  return null;
}
/* fin del proyecto: lo que termine último (video, capas, audio o texto) */
function projectEnd() {
  let e = totalDur();
  project.layers.forEach(l => l.items.forEach(it => { e = Math.max(e, it.start + clipLen(it)); }));
  project.texts.forEach(t => { e = Math.max(e, t.start + clipLen(t)); });
  return e;
}
const fadeAt = (it, lt, L) => clamp(Math.min(it.fin > 0 ? lt / it.fin : 1, it.fout > 0 ? (L - lt) / it.fout : 1), 0, 1);

/* recursos del editor: url, duración y tira de fotogramas por video */
const assets = new Map();
function assetOf(id) {
  let a = assets.get(id); if (a) return a;
  const it = itemById(id); const src = editSource(it); if (!src) return null;
  a = { id, url: urlFor(src), dur: it.outDur || it.dur || 0, w: it.w, h: it.h, frames: [], framing: false };
  assets.set(id, a); buildFrames(a);
  return a;
}
async function buildFrames(a) {
  if (a.framing) return; a.framing = true;
  const v = document.createElement('video'); v.muted = true; v.preload = 'auto'; v.src = a.url;
  try {
    await new Promise((res, rej) => { v.onloadeddata = res; v.onerror = rej; setTimeout(rej, 8000); });
    const rd = await realDuration(v); if (rd > 0) a.dur = rd;
    a.w = v.videoWidth; a.h = v.videoHeight;
    const n = clamp(Math.ceil(a.dur / 0.75), 6, 72);
    for (let k = 0; k < n; k++) {
      const t = (k + 0.5) * a.dur / n;
      a.frames.push({ t, url: await grab(v, t, 120, 0.6) });
      if (k % 6 === 5 || k === n - 1) { a.fv = a.frames.length; scheduleRender(); }
      await new Promise(r => setTimeout(r, 0)); // deja respirar a la interfaz
    }
  } catch (e) { console.warn('fotogramas', e); }
  v.removeAttribute('src'); v.load();
}
const frameAt = (a, t) => { if (!a || !a.frames.length) return ''; let best = a.frames[0]; for (const f of a.frames) if (Math.abs(f.t - t) < Math.abs(best.t - t)) best = f; return best.url; };

async function prepItems(items) {
  items = items.filter(Boolean);
  const need = items.filter(i => !editSource(i));
  if (need.length) { toast(`Convirtiendo ${need.length} AVI…`); await convertItems(need); }
  return items.filter(i => editSource(i) && (i.outDur || i.dur));
}
async function sendToEditor(items) {
  const ok = await prepItems(items); if (!ok.length) return;
  commit(() => { ok.forEach(it => project.clips.push(fixVid({ id: uid(), assetId: it.id, in: 0, out: it.outDur || it.dur }))); });
  lib.picked.clear(); renderGrid();
  view.fit = true; go('editor');
}
/* capa de video encima: en el primer nivel libre desde el cabezal */
async function addLayerClip(items) {
  const ok = await prepItems(items); if (!ok.length) return;
  let sel = null;
  commit(() => {
    let t = pv.T;
    ok.forEach(it => {
      const L = it.outDur || it.dur;
      const item = fixVid({ id: uid(), assetId: it.id, in: 0, out: L, start: t });
      let layer = project.layers.find(l => !l.items.some(x => x.start < t + L && t < x.start + clipLen(x)));
      if (!layer) { if (project.layers.length >= MAX_LAYERS) { toast('Máximo de capas'); return; } layer = { id: uid(), items: [] }; project.layers.push(layer); }
      layer.items.push(item); sel = item.id; t += L;
    });
  });
  if (sel) select('ov', sel);
  go('editor');
}

/* ================= color (mismo cálculo para preview y exportación) ================= */
function hueRGB(h) { const f = n => { const k = (n + h / 30) % 12; return 0.5 - 0.5 * Math.max(-1, Math.min(k - 3, 9 - k, 1)); }; return [f(0), f(8), f(4)]; }
const isNeutral = c => !c || (!c.bw && c.sat === 1 && !c.exp && !c.con && !c.hi && !c.sh && !c.temp && !c.tint &&
  !c.wSh.a && !c.wMid.a && !c.wHi.a && !c.wSh.l && !c.wMid.l && !c.wHi.l);
function curves(c, n = 11) {
  const W = { wSh: hueRGB(c.wSh.h), wMid: hueRGB(c.wMid.h), wHi: hueRGB(c.wHi.h) };
  const out = [[], [], []];
  for (let k = 0; k < n; k++) {
    const x = k / (n - 1);
    let y = x * Math.pow(2, c.exp);
    y = (y - 0.5) * (1 + c.con) + 0.5;
    const ws = Math.pow(Math.max(0, 1 - x / 0.6), 2), wh = Math.pow(Math.max(0, (x - 0.4) / 0.6), 2), wm = Math.max(0, 1 - Math.abs(x - 0.5) * 2.2);
    y += c.sh * 0.3 * ws + c.hi * 0.3 * wh;
    y += c.wSh.l * 0.25 * ws + c.wMid.l * 0.2 * wm + c.wHi.l * 0.25 * wh;
    for (let ch = 0; ch < 3; ch++) {
      let v = y;
      v += c.temp * 0.09 * [1, 0.15, -1][ch] * (0.3 + 0.7 * wm + 0.3 * wh);
      v += c.tint * 0.07 * [0.5, -1, 0.5][ch];
      for (const [k, w] of [['wSh', ws], ['wMid', wm], ['wHi', wh]]) {
        const rgb = W[k], mean = (rgb[0] + rgb[1] + rgb[2]) / 3;
        v += c[k].a * 0.32 * (rgb[ch] - mean) * w;
      }
      out[ch].push(clamp(v, 0, 1));
    }
  }
  return out;
}
const satOf = c => c.bw ? 0 : c.sat;
/* un filtro SVG por elemento de video (base y cada capa) */
function filterFor(key) {
  let f = document.getElementById('fx-' + key); if (f) return f;
  const NS = 'http://www.w3.org/2000/svg';
  f = document.createElementNS(NS, 'filter'); f.id = 'fx-' + key; f.setAttribute('color-interpolation-filters', 'sRGB');
  f.innerHTML = '<feColorMatrix type="saturate" values="1"/><feComponentTransfer><feFuncR type="table" tableValues="0 1"/><feFuncG type="table" tableValues="0 1"/><feFuncB type="table" tableValues="0 1"/></feComponentTransfer>';
  $('fxDefs').appendChild(f); return f;
}
function applyColor(el, key, c) {
  const sig = c ? JSON.stringify(c) : '';
  if (el._csig === sig) return; el._csig = sig;
  if (!c || isNeutral(c)) { el.style.filter = 'none'; return; }
  const f = filterFor(key), [r, g, b] = curves(c);
  f.querySelector('feColorMatrix').setAttribute('values', satOf(c).toFixed(3));
  const fs = f.querySelectorAll('feComponentTransfer > *');
  [r, g, b].forEach((arr, i) => fs[i].setAttribute('tableValues', arr.map(v => v.toFixed(4)).join(' ')));
  el.style.filter = `url(#fx-${key})`;
}
function colorFilterFF(c) {
  if (isNeutral(c)) return '';
  const f = [];
  if (satOf(c) !== 1) f.push(`hue=s=${satOf(c).toFixed(3)}`);
  const cv = curves(c);
  const pts = ch => cv[ch].map((y, k) => `${(k / (cv[ch].length - 1)).toFixed(3)}/${y.toFixed(4)}`).join(' ');
  f.push(`curves=r='${pts(0)}':g='${pts(1)}':b='${pts(2)}'`);
  return f.join(',');
}

/* ================= texto: el mismo dibujo para preview y exportación ================= */
const TX_COLORS = ['#fffaf0', '#16130f', '#d9381a', '#ffe14d', '#2b6b58'];
const TX_BG = { none: null, ink: '#16130f', paper: '#fffaf0', hl: '#ffe14d', stamp: '#d9381a' };
function drawText(g, t, W, H, alpha = 1) {
  const size = Math.max(6, t.size * H);
  g.save(); g.globalAlpha = alpha;
  g.font = `800 ${size}px Archivo, sans-serif`; g.textBaseline = 'middle'; g.textAlign = 'center';
  const lines = String(t.text || ' ').split('\n');
  const lh = size * 1.15, tw = Math.max(...lines.map(l => g.measureText(l).width)), pad = size * 0.38;
  const bw = tw + pad * 2, bh = lines.length * lh + pad * 0.9;
  const cx = t.x * W, cy = t.y * H, x0 = cx - bw / 2, y0 = cy - bh / 2;
  const bg = TX_BG[t.bg], sh = Math.max(2, size * 0.08);
  if (bg) {
    const r = Math.min(bh / 2, size * 0.5);
    const rr = (x, y) => { g.beginPath(); g.roundRect ? g.roundRect(x, y, bw, bh, r) : g.rect(x, y, bw, bh); g.fill(); };
    g.fillStyle = t.bg === 'ink' ? '#d9381a' : '#16130f'; rr(x0 + sh, y0 + sh);
    g.fillStyle = bg; rr(x0, y0);
  }
  lines.forEach((l, i) => {
    const y = cy - (lines.length - 1) * lh / 2 + i * lh;
    if (!bg) { g.fillStyle = t.color === '#16130f' ? '#fffaf0' : '#16130f'; g.fillText(l, cx + sh, y + sh); }
    g.fillStyle = t.color; g.fillText(l, cx, y);
  });
  g.restore();
  return { x: x0, y: y0, w: bw, h: bh };
}
const txBoxes = new Map();
function drawTexts() {
  const cv = $('txtCv'), fr = $('frame');
  const dpr = Math.min(2, devicePixelRatio || 1), W = fr.clientWidth, H = fr.clientHeight;
  if (cv.width !== Math.round(W * dpr) || cv.height !== Math.round(H * dpr)) { cv.width = Math.round(W * dpr); cv.height = Math.round(H * dpr); }
  const g = cv.getContext('2d'); g.setTransform(dpr, 0, 0, dpr, 0, 0); g.clearRect(0, 0, W, H);
  txBoxes.clear();
  if (project.hidden.T) return;
  project.texts.forEach(t => {
    const lt = pv.T - t.start, L = clipLen(t);
    if (lt < 0 || lt >= L) return;
    const box = drawText(g, t, W, H, fadeAt(t, lt, L));
    txBoxes.set(t.id, box);
    if (isSel('tx', t.id)) { g.save(); g.setLineDash([5, 4]); g.strokeStyle = '#ffe14d'; g.lineWidth = 2; g.strokeRect(box.x - 3, box.y - 3, box.w + 6, box.h + 6); g.restore(); }
  });
}
async function textPNG(t, W, H) {
  const c = document.createElement('canvas'); c.width = W; c.height = H;
  drawText(c.getContext('2d'), t, W, H, 1);
  const blob = await new Promise(r => c.toBlob(r, 'image/png'));
  return new Uint8Array(await blob.arrayBuffer());
}

/* ================= preview instantánea: reloj maestro ================= */
/* El tiempo lo marca un reloj propio (requestAnimationFrame). Cada video y audio
   se acomoda a ese reloj: solo se busca cuando se desfasa, nunca se re-renderiza. */
const vA = $('vA'), vB = $('vB');
const pv = { T: 0, playing: false, act: vA, idx: -1, raf: 0, t0: 0, T0: 0 };
const other = v => v === vA ? vB : vA;
function setSrc(v, a) { if (v._aid === a.id) return; v._aid = a.id; v.src = a.url; }
function seekVideo(v, t) {
  if (v.readyState < 1) { v._want = t; if (!v._lm) { v._lm = true; v.addEventListener('loadedmetadata', () => { v._lm = false; if (v._want != null) { v.currentTime = v._want; v._want = null; } }, { once: true }); } return; }
  if (v.seeking) { v._want = t; return; }
  if (Math.abs(v.currentTime - t) > 0.01) v.currentTime = t;
}
[vA, vB].forEach(v => v.addEventListener('seeked', () => { if (v._want != null) { const t = v._want; v._want = null; v.currentTime = t; } }));
const ready = v => v.readyState >= 2 && !v.seeking;
/* acomoda un elemento de video al tiempo pedido */
function drive(el, want, playing, vol) {
  if (playing) {
    if (el.paused || el.ended) { seekVideo(el, want); el.play().catch(() => {}); }
    else if (Math.abs(el.currentTime - want) > 0.25) seekVideo(el, want);
    el.muted = vol <= 0; el.volume = clamp(vol, 0, 1);
  } else { if (!el.paused) el.pause(); el.muted = true; seekVideo(el, want); }
}
function syncBase(playing) {
  const loc = project.hidden.V1 ? null : locate(pv.T);
  $('frameAdd').hidden = project.clips.length > 0;
  if (!loc) { vA.classList.remove('on'); vB.classList.remove('on'); vA.pause(); vB.pause(); pv.idx = -1; return true; }
  const a = assetOf(loc.clip.assetId); if (!a) return true;
  let v = pv.act;
  if (v._aid !== a.id && other(v)._aid === a.id) v = other(v);
  setSrc(v, a);
  if (pv.act !== v || !v.classList.contains('on')) { pv.act = v; v.classList.add('on'); other(v).classList.remove('on'); other(v).pause(); other(v).muted = true; }
  const L = clipLen(loc.clip), lt = pv.T - loc.start, f = fadeAt(loc.clip, lt, L);
  v.style.opacity = f;
  drive(v, Math.min(loc.local, (a.dur || loc.local) - 0.001), playing, project.muted.V1 ? 0 : loc.clip.vol * f);
  applyColor(v, 'base' + (v === vA ? 'A' : 'B'), loc.clip.color);
  if (pv.idx !== loc.i) { pv.idx = loc.i; preloadNext(loc.i); }
  return ready(v);
}
function preloadNext(i) {
  const nx = project.clips[i + 1], cur = project.clips[i]; if (!nx || (cur && cur.assetId === nx.assetId)) return;
  const a = assetOf(nx.assetId); if (!a) return;
  const o = other(pv.act); setSrc(o, a); seekVideo(o, nx.in);
}
/* capas de video: un elemento por capa, encima de la base */
const ovEls = new Map();
function ovEl(layer) {
  let el = ovEls.get(layer.id);
  if (!el) {
    el = document.createElement('video'); el.playsInline = true; el.muted = true; el.preload = 'auto'; el.poster = NOPOSTER;
    el.addEventListener('seeked', () => { if (el._want != null) { const t = el._want; el._want = null; el.currentTime = t; } });
    ovEls.set(layer.id, el);
  }
  return el;
}
function syncLayers(playing) {
  let ok = true;
  const live = new Set();
  project.layers.forEach((layer, k) => {
    const el = ovEl(layer); live.add(layer.id);
    if (el.parentNode !== $('ovl') || el.style.zIndex != k + 1) { el.style.zIndex = k + 1; $('ovl').appendChild(el); }
    const it = project.hidden['L' + layer.id] ? null : layer.items.find(x => pv.T >= x.start && pv.T < x.start + clipLen(x));
    if (!it) { if (el.style.display !== 'none') { el.style.display = 'none'; el.pause(); } return; }
    const a = assetOf(it.assetId); if (!a) return;
    setSrc(el, a); el.style.display = 'block';
    const lt = pv.T - it.start, L = clipLen(it), f = fadeAt(it, lt, L);
    el.style.opacity = f * (it.op ?? 1);
    drive(el, it.in + lt, playing, project.muted['L' + layer.id] ? 0 : it.vol * f);
    applyColor(el, 'L' + layer.id, it.color);
    ok = ok && ready(el);
  });
  ovEls.forEach((el, id) => { if (!live.has(id)) { el.pause(); el.remove(); ovEls.delete(id); } });
  return ok;
}
/* audios: un <audio> por pista */
const audEls = new Map(), audData = new Map();
async function ensureAudio(a) {
  if (audData.has(a.id)) return;
  audData.set(a.id, { peaks: null });
  const blob = await DB.getBlob(a.key);
  if (!blob) { toast('Falta un audio: volvé a elegirlo'); return; }
  const d = audData.get(a.id); d.blob = blob; d.url = urlFor(blob);
  const el = new Audio(); el.preload = 'auto'; el.src = d.url; audEls.set(a.id, el);
  computePeaks(blob, a.id);
}
async function computePeaks(blob, id) {
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext; const ctx = new Ctx();
    const buf = await ctx.decodeAudioData(await blob.arrayBuffer()); ctx.close && ctx.close();
    const ch = buf.getChannelData(0), N = 3000, step = Math.max(1, Math.floor(ch.length / N)), peaks = new Float32Array(N);
    for (let k = 0; k < N; k++) { let m = 0; const o = k * step; for (let j = 0; j < step; j += 4) { const s = Math.abs(ch[o + j] || 0); if (s > m) m = s; } peaks[k] = m; }
    let mx = 0; peaks.forEach(p => { if (p > mx) mx = p; }); if (mx) for (let k = 0; k < N; k++) peaks[k] /= mx;
    const d = audData.get(id); if (d) { d.peaks = peaks; d.dur = buf.duration; scheduleRender(); }
  } catch (e) { console.warn('onda', e); }
}
function syncAudios(playing) {
  const live = new Set(project.audios.map(a => a.id));
  audEls.forEach((el, id) => { if (!live.has(id)) { el.pause(); audEls.delete(id); audData.delete(id); } });
  project.audios.forEach(a => {
    const el = audEls.get(a.id); if (!el) return;
    const lt = pv.T - a.start, L = clipLen(a);
    if (!playing || lt < 0 || lt >= L || project.muted['A' + a.id]) {
      if (!el.paused) el.pause();
      if (!playing && lt >= 0 && lt < L && el.readyState >= 1 && Math.abs(el.currentTime - (a.in + lt)) > 0.05) el.currentTime = a.in + lt;
      return;
    }
    el.volume = clamp(a.vol * fadeAt(a, lt, L), 0, 1);
    if (el.paused) { el.currentTime = a.in + lt; el.play().catch(() => {}); }
    else if (Math.abs(el.currentTime - (a.in + lt)) > 0.3) el.currentTime = a.in + lt;
  });
}
function syncAll(playing) {
  const b = syncBase(playing), l = syncLayers(playing);
  syncAudios(playing);
  drawTexts();
  return b && l;
}
function setT(t, opts = {}) {
  pv.T = clamp(t, 0, projectEnd());
  if (pv.playing) { pv.T0 = pv.T; pv.t0 = performance.now(); }
  if (opts.follow !== false) ensureVisible();
  placeWorld(); updateTime();
  syncAll(pv.playing);
}
function updateTime() { $('tNow').textContent = fmtT(pv.T); $('tDur').textContent = fmtT(projectEnd()); }
function play() {
  const end = projectEnd(); if (!end) return;
  if (pv.T >= end - 0.05) pv.T = 0;
  pv.playing = true; $('playBtn').innerHTML = icon('pause');
  pv.T0 = pv.T; pv.t0 = performance.now();
  syncAll(true);
  cancelAnimationFrame(pv.raf); pv.raf = requestAnimationFrame(tick);
}
function pause() {
  if (!pv.playing) return;
  pv.playing = false; $('playBtn').innerHTML = icon('play');
  cancelAnimationFrame(pv.raf);
  syncAll(false);
}
function tick(now) {
  if (!pv.playing) return;
  pv.T = pv.T0 + (now - pv.t0) / 1000;
  const end = projectEnd();
  if (pv.T >= end) { pv.T = end; placeWorld(); updateTime(); pause(); return; }
  const ok = syncAll(true);
  // si un video todavía está cargando, el reloj espera: nada se desfasa
  if (!ok) { pv.T0 = pv.T; pv.t0 = now; }
  follow(); placeWorld(); updateTime();
  pv.raf = requestAnimationFrame(tick);
}
$('playBtn').onclick = () => pv.playing ? pause() : play();
$('frameAdd').onclick = () => openPicker('v1');

/* pantalla completa */
$('fsBtn').onclick = () => {
  const fr = $('frame');
  if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
  else if (fr.requestFullscreen) fr.requestFullscreen().catch(() => toast('Pantalla completa no disponible'));
  else if (fr.webkitRequestFullscreen) fr.webkitRequestFullscreen();
};
document.addEventListener('fullscreenchange', () => {
  $('fsBtn').innerHTML = icon(document.fullscreenElement ? 'shrink' : 'expand');
  requestAnimationFrame(() => { layoutFrame(); drawTexts(); });
});
/* tocar el video: mover un texto o reproducir/pausar */
let txDrag = null;
$('txtCv').addEventListener('pointerdown', e => {
  const r = $('txtCv').getBoundingClientRect(), x = e.clientX - r.left, y = e.clientY - r.top;
  let hit = null;
  txBoxes.forEach((b, id) => { if (x >= b.x - 8 && x <= b.x + b.w + 8 && y >= b.y - 8 && y <= b.y + b.h + 8) hit = id; });
  if (!hit) { txDrag = { tap: true, x0: e.clientX, y0: e.clientY }; return; }
  const t = project.texts.find(q => q.id === hit);
  $('txtCv').setPointerCapture(e.pointerId);
  txDrag = { t, x0: e.clientX, y0: e.clientY, bx: t.x, by: t.y, W: r.width, H: r.height, before: snap() };
  if (!isSel('tx', hit)) select('tx', hit);
});
$('txtCv').addEventListener('pointermove', e => {
  if (!txDrag || !txDrag.t) return;
  txDrag.t.x = clamp(txDrag.bx + (e.clientX - txDrag.x0) / txDrag.W, 0.02, 0.98);
  txDrag.t.y = clamp(txDrag.by + (e.clientY - txDrag.y0) / txDrag.H, 0.02, 0.98);
  drawTexts();
});
$('txtCv').addEventListener('pointerup', e => {
  const d = txDrag; txDrag = null; if (!d) return;
  if (d.t) { finishCommit(d.before); return; }
  if (Math.hypot(e.clientX - d.x0, e.clientY - d.y0) < 8 && project.clips.length) pv.playing ? pause() : play();
});

/* ================= línea de tiempo ================= */
const tl = $('tl'), tv = $('tlView');
const PAD = 10, RULER = 22, GAP = 6;
const view = { pps: 60, S: 0, Y: 0, fit: true, sel: null, drag: null };
const isSel = (k, id) => view.sel && view.sel.k === k && view.sel.id === id;
let rq = 0; function scheduleRender() { if (rq) return; rq = requestAnimationFrame(() => { rq = 0; renderTimeline(); }); }
const xOf = t => PAD + (t - view.S) * view.pps;
const tAtX = clientX => view.S + (clientX - tv.getBoundingClientRect().left - PAD) / view.pps;
function maxS() { return Math.max(0, projectEnd() + 2 - (tv.clientWidth - PAD) / view.pps * 0.5); }
function placeWorld() {
  view.S = clamp(view.S, 0, Math.max(maxS(), view.S0 || 0));
  const x = PAD - view.S * view.pps;
  $('ruler').style.transform = `translate3d(${x}px,0,0)`;
  $('rows').style.transform = `translate3d(${x}px,${-view.Y}px,0)`;
  $('heads').style.transform = `translate3d(0,${-view.Y}px,0)`;
  const hx = xOf(pv.T); const h = $('head');
  h.style.display = hx < -2 || hx > tv.clientWidth + 2 ? 'none' : 'block'; h.style.left = hx + 'px';
}
function ensureVisible() {
  const w = tv.clientWidth; if (!w) return;
  const x = xOf(pv.T);
  if (x < PAD) view.S = Math.max(0, pv.T - 0.1 * w / view.pps);
  else if (x > w - 24) view.S = pv.T - 0.7 * w / view.pps;
}
function follow() { const w = tv.clientWidth, x = xOf(pv.T); if (x > w * 0.85) view.S = pv.T - 0.15 * w / view.pps; else if (x < 0) view.S = pv.T; }
function fitZoom() { const d = projectEnd(), w = tv.clientWidth; if (!d || !w) return false; view.pps = clamp((w - PAD - 60) / d, 4, 400); view.S = 0; return true; }

/* filas de la línea de tiempo (de arriba hacia abajo) */
function rowModel() {
  const rows = [];
  if (project.texts.length) rows.push({ key: 'T', kind: 'tx', h: 36, name: 'T', ico: 'type' });
  for (let i = project.layers.length - 1; i >= 0; i--) rows.push({ key: 'L' + project.layers[i].id, kind: 'ov', layer: project.layers[i], h: 50, name: 'V' + (i + 2) });
  rows.push({ key: 'V1', kind: 'v1', h: 58, name: 'V1' });
  project.audios.forEach((a, i) => rows.push({ key: 'A' + a.id, kind: 'au', audio: a, h: 50, name: 'A' + (i + 1) }));
  rows.push({ key: 'ADD', kind: 'add', h: 34 });
  let y = 4; rows.forEach(r => { r.y = y; y += r.h + GAP; });
  rows.total = y;
  return rows;
}
function rulerStep() { const steps = [0.1, 0.25, 0.5, 1, 2, 5, 10, 15, 30, 60, 120, 300, 600]; for (const s of steps) if (s * view.pps >= 64) return s; return 600; }
let rulerKey = '';
function renderRuler(len) {
  const step = rulerStep(), minor = step / 4, end = len + Math.max(10, 6 * step);
  const key = view.pps.toFixed(3) + ':' + Math.ceil(end / step);
  if (key === rulerKey) return; rulerKey = key;
  let h = ''; const count = Math.ceil(end / minor);
  for (let k = 0; k <= count && k < 3000; k++) {
    const t = k * minor, x = t * view.pps, big = k % 4 === 0;
    h += `<i class="${big ? 'big' : ''}" style="left:${x}px"></i>`;
    if (big) h += `<span style="left:${x}px">${t < 60 ? +t.toFixed(2) + 's' : Math.floor(t / 60) + ':' + String(Math.round(t % 60)).padStart(2, '0')}</span>`;
  }
  $('ruler').innerHTML = h; $('ruler').style.width = end * view.pps + 'px';
}
/* posiciones de V1; durante un arrastre se usa la disposición temporal */
function v1Layout() {
  const d = view.drag, cs = project.clips, pos = new Map();
  if (d && d.kind === 'reorder') {
    const rest = cs.filter(c => c.id !== d.id); let s = 0;
    rest.forEach((c, k) => { if (k === d.slot) s += clipLen(d.clip); pos.set(c.id, s); s += clipLen(c); });
    pos.set(d.id, d.ghostT); return pos;
  }
  let s = 0;
  cs.forEach(c => {
    if (d && d.kind === 'trim' && d.k === 'v1' && d.id === c.id && d.side === 'l') { pos.set(c.id, s + (c.in - d.base.in)); s += d.base.out - d.base.in; return; }
    pos.set(c.id, s); s += clipLen(c);
  });
  return pos;
}
/* render con elementos reutilizados: solo se toca lo que cambió */
const els = new Map();
function el(key, cls, build) {
  let e = els.get(key);
  if (!e) { e = document.createElement('div'); e.className = cls; if (build) build(e); els.set(key, e); }
  e._live = true; return e;
}
function stripHTML(c, a, w) {
  const n = Math.min(160, Math.max(1, Math.ceil(w / 52))), tw = w / n; let s = '';
  for (let j = 0; j < n; j++) { const u = frameAt(a, c.in + ((j + 0.5) * tw) / view.pps); s += `<i style="width:${tw}px;${u ? `background-image:url(${u})` : ''}"></i>`; }
  return s;
}
function vidItem(key, c, x, row, kind, idx) {
  const a = assetOf(c.assetId), w = Math.max(4, clipLen(c) * view.pps);
  const e = el(key, 'it ' + (kind === 'ov' ? 'lay' : kind), n => { n.innerHTML = '<div class="strip"></div><span class="fade in"></span><span class="fade out"></span><div class="cap"></div><span class="trim-tip"></span><span class="hdl l" data-h="l"></span><span class="hdl r" data-h="r"></span>'; });
  e.dataset.k = kind; e.dataset.id = c.id;
  e.style.cssText = `left:${x * view.pps}px;top:${row.y}px;width:${w}px;height:${row.h}px`;
  const sk = `${Math.round(w)}|${c.in.toFixed(2)}|${view.pps.toFixed(2)}|${a ? a.frames.length : 0}`;
  if (e._sk !== sk) { e._sk = sk; e.querySelector('.strip').innerHTML = stripHTML(c, a, w); }
  e.querySelector('.fade.in').style.width = Math.min(w, (c.fin || 0) * view.pps) + 'px';
  e.querySelector('.fade.out').style.width = Math.min(w, (c.fout || 0) * view.pps) + 'px';
  const cap = `${idx != null ? `<b>${idx}</b>` : ''}<b>${fmtShort(clipLen(c))}</b>${isNeutral(c.color) ? '' : '<b class="fx">color</b>'}${c.vol !== 1 ? `<b>${Math.round(c.vol * 100)}%</b>` : ''}`;
  if (e._cap !== cap) { e._cap = cap; e.querySelector('.cap').innerHTML = cap; }
  e.querySelector('.trim-tip').textContent = fmtT(clipLen(c));
  return e;
}
function renderTimeline() {
  if (view.fit && projectEnd() && tv.clientWidth) { fitZoom(); view.fit = false; }
  const rows = rowModel(), d = view.drag;
  // alto de la línea de tiempo según las pistas, sin pasarse
  const H = tl.clientHeight || 200;
  view.Y = clamp(view.Y, 0, Math.max(0, RULER + rows.total - H));
  renderRuler(projectEnd());
  els.forEach(e => { e._live = false; });
  const R = $('rows'), HD = $('heads');
  rows.forEach(row => {
    const lane = el('lane:' + row.key, 'lane'); lane.style.cssText = `top:${row.y}px;height:${row.h}px`; if (lane.parentNode !== R) R.appendChild(lane);
    // cabecera de la pista
    if (row.kind !== 'add') {
      const hk = row.kind === 'v1' ? 'V1' : row.key;
      const hd = el('hd:' + row.key, 'hd'); hd.style.cssText = `top:${row.y}px;height:${row.h}px`;
      const isAudio = row.kind === 'au', isText = row.kind === 'tx';
      const off = isAudio ? project.muted[hk] : project.hidden[hk];
      const btn = isAudio ? `<button data-mute="${hk}" class="${off ? 'off' : ''}" title="Silenciar">${icon(off ? 'volumeX' : 'volume')}</button>`
        : `<button data-hide="${hk}" class="${off ? 'off' : ''}" title="Ocultar">${icon(off ? 'eyeOff' : 'eye')}</button>`;
      const extra = (row.kind === 'v1' || row.kind === 'ov') ? `<button data-mute="${hk}" class="${project.muted[hk] ? 'off' : ''}" title="Silenciar">${icon(project.muted[hk] ? 'volumeX' : 'volume')}</button>` : '';
      const html = `${row.name}${btn}${row.h > 40 ? extra : ''}`;
      if (hd._h !== html) { hd._h = html; hd.innerHTML = html; }
      void isText;
      if (hd.parentNode !== HD) HD.appendChild(hd);
    }
    if (row.kind === 'v1') {
      const pos = v1Layout();
      project.clips.forEach((c, k) => {
        const e = vidItem('v1:' + c.id, c, pos.get(c.id), row, 'v1', k + 1);
        e.classList.toggle('sel', isSel('v1', c.id));
        e.classList.toggle('lift', !!(d && d.kind === 'reorder' && d.id === c.id));
        e.classList.toggle('trimming', !!(d && d.kind === 'trim' && d.id === c.id));
        e.classList.toggle('off', !!project.hidden.V1);
        if (e.parentNode !== R) R.appendChild(e);
      });
      const add = el('addslot', 'add-slot', n => { n.dataset.add = 'v1'; n.innerHTML = icon('plus'); });
      add.style.cssText = `left:${totalDur() * view.pps + 14}px;top:${row.y + (row.h - 44) / 2}px;height:44px`;
      if (add.parentNode !== R) R.appendChild(add);
    }
    if (row.kind === 'ov') {
      row.layer.items.forEach(it => {
        const e = vidItem('ov:' + it.id, it, it.start, row, 'ov');
        e.classList.toggle('sel', isSel('ov', it.id));
        e.classList.toggle('trimming', !!(d && d.kind === 'trim' && d.id === it.id));
        e.classList.toggle('off', !!project.hidden[row.key]);
        if (e.parentNode !== R) R.appendChild(e);
      });
    }
    if (row.kind === 'au') {
      const a = row.audio, w = Math.max(6, clipLen(a) * view.pps);
      const e = el('au:' + a.id, 'it au', n => { n.innerHTML = '<div class="wave"><canvas></canvas></div><span class="fade in"></span><span class="fade out"></span><div class="cap"></div><span class="trim-tip"></span><span class="hdl l" data-h="l"></span><span class="hdl r" data-h="r"></span>'; });
      e.dataset.k = 'au'; e.dataset.id = a.id;
      e.style.cssText = `left:${a.start * view.pps}px;top:${row.y}px;width:${w}px;height:${row.h}px`;
      e.querySelector('.fade.in').style.width = Math.min(w, a.fin * view.pps) + 'px';
      e.querySelector('.fade.out').style.width = Math.min(w, a.fout * view.pps) + 'px';
      const cap = `<b>${esc(baseName(a.name))}</b><b>${Math.round(a.vol * 100)}%</b>`;
      if (e._cap !== cap) { e._cap = cap; e.querySelector('.cap').innerHTML = cap; }
      e.querySelector('.trim-tip').textContent = fmtT(clipLen(a));
      e.classList.toggle('sel', isSel('au', a.id));
      e.classList.toggle('trimming', !!(d && d.kind === 'trim' && d.id === a.id));
      e.classList.toggle('off', !!project.muted[row.key]);
      if (e.parentNode !== R) R.appendChild(e);
      const pk = audData.get(a.id), wk = `${Math.round(w)}|${a.in.toFixed(2)}|${a.out.toFixed(2)}|${pk && pk.peaks ? 1 : 0}`;
      if (e._wk !== wk) { e._wk = wk; requestAnimationFrame(() => drawWave(e.querySelector('canvas'), a, w, row.h)); }
    }
    if (row.kind === 'tx') {
      project.texts.forEach(t => {
        const e = el('tx:' + t.id, 'it tx', n => { n.innerHTML = '<span></span><span class="trim-tip"></span><span class="hdl l" data-h="l"></span><span class="hdl r" data-h="r"></span>'; });
        e.dataset.k = 'tx'; e.dataset.id = t.id;
        e.style.cssText = `left:${t.start * view.pps}px;top:${row.y}px;width:${Math.max(6, clipLen(t) * view.pps)}px;height:${row.h}px`;
        e.firstChild.textContent = t.text.replace(/\n/g, ' ');
        e.querySelector('.trim-tip').textContent = fmtT(clipLen(t));
        e.classList.toggle('sel', isSel('tx', t.id));
        e.classList.toggle('trimming', !!(d && d.kind === 'trim' && d.id === t.id));
        e.classList.toggle('off', !!project.hidden.T);
        if (e.parentNode !== R) R.appendChild(e);
      });
    }
    if (row.kind === 'add') {
      const e = el('addrow', 'add-row', n => { n.innerHTML = `<button data-add="au">${icon('music')}Agregar audio</button><button data-add="tx">${icon('type')}Texto</button><button data-add="ov">${icon('layers')}Capa</button>`; });
      e.style.cssText = `left:0;top:${row.y + 1}px`;
      if (e.parentNode !== R) R.appendChild(e);
    }
  });
  els.forEach((e, k) => { if (!e._live) { e.remove(); els.delete(k); } });
  placeWorld(); updateTime(); renderDock();
}
function drawWave(cv, a, w, h) {
  if (!cv) return;
  const dpr = Math.min(2, devicePixelRatio || 1), W = Math.min(8000, Math.max(2, Math.round(w * dpr))), H = Math.max(2, Math.round(h * dpr));
  cv.width = W; cv.height = H;
  const g = cv.getContext('2d'); g.fillStyle = 'rgba(255,250,240,.85)';
  const d = audData.get(a.id), p = d && d.peaks;
  if (!p) { g.fillRect(0, H / 2 - 1, W, 2); return; }
  const dur = d.dur || a.dur;
  for (let x = 0; x < W; x += 2 * dpr) {
    const t = a.in + (x / W) * clipLen(a); const k = Math.floor((t / dur) * p.length);
    const amp = (p[clamp(k, 0, p.length - 1)] || 0) * (H * 0.42) + 1;
    g.fillRect(x, H / 2 - amp, Math.max(1, dpr * 1.2), amp * 2);
  }
}
$('heads').addEventListener('click', e => {
  const m = e.target.closest('[data-mute]'), h = e.target.closest('[data-hide]');
  if (m) commit(() => { project.muted[m.dataset.mute] = !project.muted[m.dataset.mute]; });
  if (h) commit(() => { project.hidden[h.dataset.hide] = !project.hidden[h.dataset.hide]; });
});

/* ---------- selección ---------- */
function getSel() {
  const s = view.sel; if (!s) return null;
  if (s.k === 'v1') { const it = project.clips.find(c => c.id === s.id); return it && { k: 'v1', it, list: project.clips }; }
  if (s.k === 'ov') { for (const l of project.layers) { const it = l.items.find(x => x.id === s.id); if (it) return { k: 'ov', it, list: l.items, layer: l }; } return null; }
  if (s.k === 'au') { const it = project.audios.find(a => a.id === s.id); return it && { k: 'au', it, list: project.audios }; }
  if (s.k === 'tx') { const it = project.texts.find(a => a.id === s.id); return it && { k: 'tx', it, list: project.texts }; }
  return null;
}
function select(k, id) {
  view.sel = k ? { k, id } : null;
  renderTimeline(); drawTexts();
  if (sheetKind && sheetKind !== 'settings' && sheetKind !== 'format' && sheetKind !== 'export' && sheetKind !== 'pick') {
    const s = getSel(); if (!s) closeSheet(); else reopenSheet();
  }
}
function deselect() { select(null); closeSheet(); }

/* ---------- herramientas: una fila, según lo elegido ---------- */
function renderDock() {
  const T = (ic, label, act, extra = '') => `<button class="tool ${extra}" data-act="${act}"><span class="ic">${icon(ic)}</span>${label}</button>`;
  const s = getSel(); let h;
  if (s && (s.k === 'v1' || s.k === 'ov')) h = T('cut', 'Dividir', 'split') + T('palette', 'Color', 'color') + T('fade', 'Fundido', 'fade') + T('volume', 'Sonido', 'sound') + T('dup', 'Duplicar', 'dup') + T('trash', 'Borrar', 'del', 'danger') + '<span class="sep"></span>' + T('check', 'Listo', 'done');
  else if (s && s.k === 'au') h = T('volume', 'Sonido', 'sound') + T('cut', 'Dividir', 'split') + T('dup', 'Duplicar', 'dup') + T('trash', 'Borrar', 'del', 'danger') + '<span class="sep"></span>' + T('check', 'Listo', 'done');
  else if (s && s.k === 'tx') h = T('type', 'Editar', 'text') + T('cut', 'Dividir', 'split') + T('dup', 'Duplicar', 'dup') + T('trash', 'Borrar', 'del', 'danger') + '<span class="sep"></span>' + T('check', 'Listo', 'done');
  else h = T('type', 'Texto', 'addtx') + T('layers', 'Capa', 'addov') + T('music', 'Audio', 'addau') + T('crop', 'Formato', 'format') + T('download', 'Exportar', 'export', 'hot');
  if ($('dock')._h !== h) { $('dock')._h = h; $('dock').innerHTML = h; }
  $('undoBtn').disabled = !hist.undo.length; $('redoBtn').disabled = !hist.redo.length;
}
let audTarget = null;
function addText() {
  const t = { id: uid(), text: 'Texto', start: pv.T >= projectEnd() - 0.1 ? 0 : pv.T, in: 0, out: 3, x: 0.5, y: 0.82, size: 0.085, color: '#fffaf0', bg: 'ink', fin: 0.3, fout: 0.3 };
  commit(() => { project.texts.push(t); });
  select('tx', t.id); openText(true);
}
$('dock').addEventListener('click', e => {
  const b = e.target.closest('[data-act]'); if (!b) return;
  const a = b.dataset.act, s = getSel();
  if (a === 'split') splitAtHead();
  if (a === 'dup' && s) dupSel();
  if (a === 'del' && s) { commit(() => { s.list.splice(s.list.indexOf(s.it), 1); }); view.sel = null; closeSheet(); }
  if (a === 'color') openColor();
  if (a === 'fade') openFade();
  if (a === 'sound') openSound();
  if (a === 'text') openText();
  if (a === 'done') deselect();
  if (a === 'addtx') addText();
  if (a === 'addov') openPicker('ov');
  if (a === 'addau') { audTarget = null; $('musicIn').click(); }
  if (a === 'format') openFormat();
  if (a === 'export') openExport();
  renderTimeline();
});
function dupSel() {
  const s = getSel(); if (!s) return;
  const c = JSON.parse(JSON.stringify(s.it)); c.id = uid();
  commit(() => {
    if (s.k === 'v1') s.list.splice(s.list.indexOf(s.it) + 1, 0, c);
    else { c.start = s.it.start + clipLen(s.it); if (s.k === 'au') { c.key = s.it.key; } s.list.push(c); }
  });
  if (s.k === 'au') ensureAudio(c);
  select(s.k, c.id);
}
function splitAtHead() {
  const s = getSel();
  if (!s || s.k === 'v1') {
    const loc = locate(pv.T); if (!loc) return;
    const c = loc.clip, t = loc.local;
    if (t - c.in < MIN_LEN || c.out - t < MIN_LEN) { toast('Poné el cabezal dentro del clip'); return; }
    const b = JSON.parse(JSON.stringify(c)); b.id = uid(); b.in = t; b.fin = 0;
    commit(() => { c.out = t; c.fout = 0; project.clips.splice(loc.i + 1, 0, b); });
    view.sel = { k: 'v1', id: b.id }; buzz(12); renderTimeline(); return;
  }
  const it = s.it, lt = pv.T - it.start;
  if (lt < MIN_LEN || clipLen(it) - lt < MIN_LEN) { toast('Poné el cabezal dentro del bloque'); return; }
  const b = JSON.parse(JSON.stringify(it)); b.id = uid(); b.in = it.in + lt; b.start = it.start + lt; b.fin = 0;
  commit(() => { it.out = it.in + lt; it.fout = 0; s.list.push(b); });
  if (s.k === 'au') ensureAudio(b);
  view.sel = { k: s.k, id: b.id }; buzz(12); renderTimeline();
}
$('clipIn').onchange = async e => {
  const mode = $('clipIn')._mode || 'v1'; const added = await addFiles(e.target.files); e.target.value = '';
  closeSheet();
  if (mode === 'ov') addLayerClip(added); else sendToEditor(added);
};
$('musicIn').onchange = async e => {
  const files = [...e.target.files]; e.target.value = ''; if (!files.length) return;
  const made = [];
  for (const f of files) {
    if (project.audios.length + made.length >= MAX_AUDIOS) { toast('Máximo de pistas de audio'); break; }
    const dur = await new Promise(res => { const a = new Audio(); a.preload = 'metadata'; a.onloadedmetadata = () => res(a.duration); a.onerror = () => res(0); a.src = URL.createObjectURL(f); });
    if (!dur || !isFinite(dur)) { toast('No se pudo leer ' + f.name); continue; }
    const id = uid(), key = 'audio:' + id;
    await DB.putBlob(key, f);
    made.push({ id, key, name: f.name, dur, in: 0, out: dur, start: project.audios.length || made.length ? pv.T : 0, vol: 0.8, fin: 0, fout: 0 });
  }
  if (!made.length) return;
  commit(() => { made.forEach(a => project.audios.push(a)); });
  made.forEach(ensureAudio);
  select('au', made[made.length - 1].id);
};

/* ---------- gestos: recorrer, pellizcar, regla, seleccionar, recortar, mover, reordenar ---------- */
const ptrs = new Map();
let gest = null, inertia = 0, longT = 0;
function snapT(t, excludeId) {
  if (!prefs.snap) return { t, hit: false };
  const c = [0, pv.T]; let s = 0;
  project.clips.forEach(x => { c.push(s); s += clipLen(x); }); c.push(s);
  const free = [...project.layers.flatMap(l => l.items), ...project.audios, ...project.texts];
  free.forEach(x => { if (x.id !== excludeId) c.push(x.start, x.start + clipLen(x)); });
  const th = 10 / view.pps; let best = null;
  for (const v of c) if (Math.abs(v - t) < th && (best == null || Math.abs(v - t) < Math.abs(best - t))) best = v;
  return best == null ? { t, hit: false } : { t: best, hit: true };
}
let lastSnap = false;
function showSnap(t) {
  const sl = $('snapline');
  if (t == null) { sl.style.display = 'none'; lastSnap = false; return; }
  if (!lastSnap) buzz(8); lastSnap = true;
  sl.style.display = 'block'; sl.style.left = xOf(t) + 'px';
}
const freeOf = (k, id) => { const s = view.sel; const keep = view.sel; view.sel = { k, id }; const r = getSel(); view.sel = s || keep; return r; };

tv.addEventListener('pointerdown', e => {
  tv.setPointerCapture(e.pointerId);
  ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY });
  cancelAnimationFrame(inertia);
  if (ptrs.size === 2) {
    clearTimeout(longT);
    const [p, q] = [...ptrs.values()], mx = (p.x + q.x) / 2;
    gest = { kind: 'pinch', d0: Math.hypot(p.x - q.x, p.y - q.y) || 1, pps0: view.pps, tMid: tAtX(mx), mx };
    view.drag = null; return;
  }
  if (pv.playing) pause();
  const r = tv.getBoundingClientRect();
  const hdl = e.target.closest('.hdl'), itEl = e.target.closest('.it'), add = e.target.closest('[data-add]');
  if (e.clientY - r.top < RULER) { gest = { kind: 'scrub' }; setT(tAtX(e.clientX), { follow: false }); return; }
  if (hdl && itEl) {
    const s = freeOf(itEl.dataset.k, itEl.dataset.id); if (!s) return;
    const it = s.it;
    gest = { kind: 'trim', k: s.k, id: it.id, it, side: hdl.dataset.h, x0: e.clientX, before: snap(), base: { in: it.in, out: it.out, start: it.start },
      start: s.k === 'v1' ? startOf(project.clips.indexOf(it)) : it.start };
    view.drag = gest; tl.classList.add('grab'); scheduleRender(); return;
  }
  gest = { kind: 'press', x0: e.clientX, y0: e.clientY, S0: view.S, Y0: view.Y, k: itEl && itEl.dataset.k, id: itEl && itEl.dataset.id, add: add && add.dataset.add, vx: 0, lx: e.clientX, lt: performance.now() };
  if (itEl && itEl.dataset.k !== 'v1' && isSel(itEl.dataset.k, itEl.dataset.id)) {
    const s = getSel();
    gest = { kind: 'move', k: s.k, it: s.it, x0: e.clientX, before: snap(), base: s.it.start, moved: false };
    view.drag = gest; return;
  }
  if (itEl && itEl.dataset.k === 'v1') {
    longT = setTimeout(() => {
      if (!gest || gest.kind !== 'press') return;
      const c = project.clips.find(x => x.id === gest.id); if (!c) return;
      buzz(20);
      const i = project.clips.indexOf(c);
      gest = { kind: 'reorder', id: c.id, clip: c, before: snap(), grabOff: tAtX(gest.lx) - startOf(i), lx: gest.lx };
      view.drag = { kind: 'reorder', id: c.id, clip: c, slot: i, ghostT: startOf(i) };
      view.sel = { k: 'v1', id: c.id }; tl.classList.add('grab'); scheduleRender(); autoPan();
    }, 380);
  }
});
tv.addEventListener('pointermove', e => {
  if (!ptrs.has(e.pointerId)) return;
  ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY });
  const g = gest; if (!g) return;
  if (g.kind === 'pinch' && ptrs.size >= 2) {
    const [p, q] = [...ptrs.values()];
    view.pps = clamp(g.pps0 * (Math.hypot(p.x - q.x, p.y - q.y) / g.d0), 4, 600);
    view.S = Math.max(0, g.tMid - (g.mx - tv.getBoundingClientRect().left - PAD) / view.pps);
    scheduleRender(); return;
  }
  if (g.kind === 'scrub') { setT(tAtX(e.clientX), { follow: false }); return; }
  if (g.kind === 'press') {
    if (Math.abs(e.clientX - g.x0) > 6 || Math.abs(e.clientY - g.y0) > 6) { clearTimeout(longT); g.kind = 'pan'; tl.classList.add('grab'); }
  }
  if (g.kind === 'pan') {
    const now = performance.now();
    g.vx = (e.clientX - g.lx) / Math.max(1, now - g.lt); g.lx = e.clientX; g.lt = now;
    view.S = Math.max(0, g.S0 - (e.clientX - g.x0) / view.pps);
    view.Y = g.Y0 - (e.clientY - g.y0);
    const rows = rowModel(); view.Y = clamp(view.Y, 0, Math.max(0, RULER + rows.total - tl.clientHeight));
    placeWorld(); return;
  }
  if (g.kind === 'trim') { doTrim(e.clientX); return; }
  if (g.kind === 'move') {
    g.moved = true;
    const L = clipLen(g.it);
    let st = Math.max(0, g.base + (e.clientX - g.x0) / view.pps);
    const s1 = snapT(st, g.it.id), s2 = snapT(st + L, g.it.id);
    if (s1.hit) st = s1.t; else if (s2.hit) st = s2.t - L;
    showSnap(s1.hit ? s1.t : s2.hit ? s2.t : null);
    g.it.start = Math.max(0, st); scheduleRender(); syncAll(false); return;
  }
  if (g.kind === 'reorder') { g.lx = e.clientX; reorderMove(); }
});
function doTrim(x) {
  const g = gest, it = g.it, d = (x - g.x0) / view.pps;
  const srcMax = g.k === 'tx' ? 3600 : g.k === 'au' ? ((audData.get(it.id) || {}).dur || it.dur) : ((assetOf(it.assetId) || {}).dur || g.base.out);
  if (g.side === 'r') {
    let out = clamp(g.base.out + d, it.in + MIN_LEN, srcMax);
    const s = snapT(g.start + out - it.in, it.id); if (s.hit) out = clamp(it.in + s.t - g.start, it.in + MIN_LEN, srcMax);
    showSnap(s.hit ? s.t : null);
    it.out = out;
    previewAt(g, Math.max(it.in, out - 0.04));
  } else {
    let nin = clamp(g.base.in + d, 0, it.out - MIN_LEN);
    if (g.k !== 'v1' && g.base.start + (nin - g.base.in) < 0) nin = g.base.in - g.base.start;
    const s = snapT(g.start + (nin - g.base.in), it.id); if (s.hit) nin = clamp(g.base.in + s.t - g.start, 0, it.out - MIN_LEN);
    showSnap(s.hit ? s.t : null);
    it.in = nin;
    if (g.k !== 'v1') it.start = Math.max(0, g.base.start + (nin - g.base.in));
    previewAt(g, nin);
  }
  scheduleRender();
}
/* mientras se recorta, se ve el cuadro del borde al instante */
function previewAt(g, srcT) {
  if (g.k === 'v1') {
    const a = assetOf(g.it.assetId); if (!a) return;
    let v = pv.act; if (v._aid !== a.id && other(v)._aid === a.id) v = other(v);
    setSrc(v, a); pv.act = v; v.classList.add('on'); other(v).classList.remove('on'); seekVideo(v, srcT); applyColor(v, 'base' + (v === vA ? 'A' : 'B'), g.it.color);
  } else if (g.k === 'ov') {
    pv.T = g.it.start + (srcT - g.it.in); syncAll(false); placeWorld(); updateTime();
  } else if (g.k === 'tx') { drawTexts(); }
}
function reorderMove() {
  const g = gest, d = view.drag; if (!g || g.kind !== 'reorder') return;
  const tp = tAtX(g.lx);
  d.ghostT = Math.max(0, tp - g.grabOff);
  const rest = project.clips.filter(c => c.id !== g.id); let s = 0, slot = rest.length;
  for (let k = 0; k < rest.length; k++) { const L = clipLen(rest[k]); if (tp < s + L / 2) { slot = k; break; } s += L; }
  if (slot !== d.slot) buzz(6);
  d.slot = slot; scheduleRender();
}
function autoPan() {
  if (!gest || gest.kind !== 'reorder') return;
  const r = tv.getBoundingClientRect(), x = gest.lx - r.left, edge = 50;
  if (x < edge) view.S = Math.max(0, view.S - (edge - x) / view.pps * 0.25);
  else if (x > r.width - edge) view.S += (x - r.width + edge) / view.pps * 0.25;
  reorderMove(); placeWorld();
  requestAnimationFrame(autoPan);
}
function endGesture(e) {
  ptrs.delete(e.pointerId);
  clearTimeout(longT);
  tl.classList.remove('grab'); showSnap(null);
  const g = gest; if (!g) return;
  if (g.kind === 'pinch') { if (ptrs.size === 0) gest = null; return; }
  gest = null;
  if (g.kind === 'scrub') return;
  if (g.kind === 'press') {
    if (g.add === 'v1') openPicker('v1');
    else if (g.add === 'au') { audTarget = null; $('musicIn').click(); }
    else if (g.add === 'tx') addText();
    else if (g.add === 'ov') openPicker('ov');
    else if (g.k) select(isSel(g.k, g.id) && g.k === 'v1' ? null : g.k, g.id);
    else { if (view.sel) deselect(); setT(tAtX(e.clientX), { follow: false }); }
    return;
  }
  if (g.kind === 'pan') {
    let v = g.vx * 16; const step = () => { v *= 0.93; if (Math.abs(v) < 0.4) return; view.S = Math.max(0, view.S - v / view.pps); placeWorld(); inertia = requestAnimationFrame(step); };
    if (Math.abs(v) > 2) inertia = requestAnimationFrame(step);
    return;
  }
  if (g.kind === 'trim') {
    view.drag = null;
    if (g.k === 'v1' && g.side === 'l') view.S = Math.max(0, view.S - (g.it.in - g.base.in));
    finishCommit(g.before); setT(pv.T, { follow: false }); return;
  }
  if (g.kind === 'move') { view.drag = null; if (g.moved) finishCommit(g.before); else deselect(); return; }
  if (g.kind === 'reorder') {
    const d = view.drag; view.drag = null;
    const arr = project.clips.filter(c => c.id !== g.id); arr.splice(d.slot, 0, g.clip);
    if (arr.some((c, k) => c !== project.clips[k])) { project.clips = arr; finishCommit(g.before); }
    else renderTimeline();
    setT(startOf(project.clips.indexOf(g.clip)) + 0.001, { follow: false });
  }
}
tv.addEventListener('pointerup', endGesture);
tv.addEventListener('pointercancel', endGesture);
tv.addEventListener('wheel', e => {
  e.preventDefault();
  if (e.ctrlKey || e.metaKey) {
    const t = tAtX(e.clientX); view.pps = clamp(view.pps * Math.exp(-e.deltaY * 0.004), 4, 600);
    view.S = Math.max(0, t - (e.clientX - tv.getBoundingClientRect().left - PAD) / view.pps); scheduleRender();
  } else if (e.shiftKey || Math.abs(e.deltaX) > Math.abs(e.deltaY)) { view.S = Math.max(0, view.S + (e.deltaX || e.deltaY) / view.pps); placeWorld(); }
  else { const rows = rowModel(); view.Y = clamp(view.Y + e.deltaY, 0, Math.max(0, RULER + rows.total - tl.clientHeight)); if (view.Y === 0 || rows.total + RULER <= tl.clientHeight) view.S = Math.max(0, view.S + e.deltaY / view.pps); placeWorld(); }
}, { passive: false });
const syncSnapBtn = () => $('snapBtn').classList.toggle('on', !!prefs.snap);
$('snapBtn').onclick = () => { prefs.snap = !prefs.snap; savePrefs(); syncSnapBtn(); };

function onProjectChange() {
  project.layers = project.layers.filter(l => l.items.length);
  if (view.sel && !getSel()) view.sel = null;
  pv.T = clamp(pv.T, 0, projectEnd());
  renderTimeline(); layoutFrame();
  if (!pv.playing) syncAll(false);
}
/* cuadro de preview: de borde a borde según el formato de salida */
function layoutFrame() {
  const st = $('stage'), fr = $('frame');
  if (document.fullscreenElement === fr) { fr.style.width = ''; fr.style.height = ''; return; }
  const first = project.clips[0] && assetOf(project.clips[0].assetId);
  const a = aspectOf(prefs.aspect);
  const r = a.r || (first && first.w && first.h ? first.w / first.h : 16 / 9);
  const W = st.clientWidth, H = Math.max(140, ($('editor').clientHeight || 600) * 0.5);
  let w = W, h = w / r; if (h > H) { h = H; w = h * r; }
  fr.style.width = Math.max(40, Math.floor(w)) + 'px'; fr.style.height = Math.max(40, Math.floor(h)) + 'px';
  st.style.height = Math.max(40, Math.floor(h)) + 'px';
  drawTexts();
}
new ResizeObserver(() => { layoutFrame(); placeWorld(); }).observe($('editor'));
new ResizeObserver(() => placeWorld()).observe(tv);

/* ================= hojas inferiores ================= */
let sheetKind = null;
function openSheet(kind, html) {
  sheetKind = kind; $('sheetBody').innerHTML = html; paintIcons($('sheetBody'));
  $('sheet').classList.add('show'); $('sheetOv').classList.add('show');
}
function closeSheet() { sheetKind = null; $('sheet').classList.remove('show'); $('sheetOv').classList.remove('show'); }
function reopenSheet() { ({ color: openColor, fade: openFade, sound: openSound, text: openText })[sheetKind]?.(); }
$('sheetOv').onclick = closeSheet;
const head = (t, extra = '') => `<div class="sh-head"><b>${t}</b>${extra}<button class="round" data-close data-ico="x" title="Cerrar"></button></div>`;
$('sheet').addEventListener('click', e => { if (e.target.closest('[data-close]')) closeSheet(); });
const rng = (k, label, mn, mx, st, v, f) => `<label class="ctl"><span>${label}</span><input type="range" data-k="${k}" min="${mn}" max="${mx}" step="${st}" value="${v}"><output>${f(v)}</output></label>`;
const pct = v => Math.round(v * 100) + ' %', sgn = v => (v > 0 ? '+' : '') + Math.round(v * 100), secs = v => (+v).toFixed(1) + ' s';
const FMT = { sat: pct, exp: v => (v > 0 ? '+' : '') + (+v).toFixed(2), con: sgn, hi: sgn, sh: sgn, temp: sgn, tint: sgn, vol: pct, op: pct, fin: secs, fout: secs, size: v => Math.round(v * 1000) / 10 + '' };
let colorTab = 'luz';
const kindName = { v1: 'clip', ov: 'capa', au: 'audio', tx: 'texto' };

/* color: luz, color y ruedas (sombras, medios, luces) */
function wheelSVG(key, w) {
  const R1 = 48, cx = 50, cy = 50; let s = '';
  const sector = (a0, a1, r0, r1) => { const p = (a, r) => [cx + r * Math.cos(a * Math.PI / 180), cy + r * Math.sin(a * Math.PI / 180)]; const [x0, y0] = p(a0, r1), [x1, y1] = p(a1, r1), [x2, y2] = p(a1, r0), [x3, y3] = p(a0, r0); return `M${x0},${y0}A${r1},${r1} 0 0 1 ${x1},${y1}L${x2},${y2}A${r0},${r0} 0 0 0 ${x3},${y3}Z`; };
  [[48, 34, 78], [34, 20, 62], [20, 8, 44]].forEach(([r1, r0, sat]) => { for (let i = 0; i < 24; i++) s += `<path d="${sector(i * 15, i * 15 + 15, r0, r1)}" fill="hsl(${i * 15 + 7.5},${sat}%,${58 + (78 - sat) / 2}%)" stroke="#fffaf0" stroke-width="1.2"/>`; });
  s += `<circle cx="50" cy="50" r="8" fill="#fffaf0" stroke="#16130f" stroke-width="1.2"/>`;
  const ang = w.h * Math.PI / 180, rad = w.a * R1;
  s += `<line x1="50" y1="50" x2="${50 + rad * Math.cos(ang)}" y2="${50 + rad * Math.sin(ang)}" stroke="#16130f" stroke-width="1.5"/>`;
  s += `<circle class="dot" cx="${50 + rad * Math.cos(ang)}" cy="${50 + rad * Math.sin(ang)}" r="5.5" fill="#ffe14d" stroke="#16130f" stroke-width="2"/>`;
  return `<svg viewBox="0 0 100 100" data-wheel="${key}">${s}</svg>`;
}
function openColor() {
  const s = getSel(); if (!s || !s.it.color) return;
  const c = s.it.color;
  const tabs = `<div class="seg cl-tabs">${[['luz', 'Luz'], ['color', 'Color'], ['ruedas', 'Ruedas']].map(([k, l]) => `<button data-ctab="${k}" class="${colorTab === k ? 'on' : ''}">${l}</button>`).join('')}</div>`;
  let body = '';
  if (colorTab === 'luz') body = rng('exp', 'Exposición', -1, 1, 0.01, c.exp, FMT.exp) + rng('con', 'Contraste', -0.8, 0.8, 0.01, c.con, FMT.con) + rng('hi', 'Luces', -1, 1, 0.01, c.hi, FMT.hi) + rng('sh', 'Sombras', -1, 1, 0.01, c.sh, FMT.sh);
  if (colorTab === 'color') body = rng('temp', 'Temperatura', -1, 1, 0.01, c.temp, FMT.temp) + rng('tint', 'Tinte', -1, 1, 0.01, c.tint, FMT.tint) + rng('sat', 'Saturación', 0, 2, 0.01, c.sat, FMT.sat) +
    `<div class="sh-row"><button class="pill ${c.bw ? 'on' : ''}" data-bw>B y N</button></div>`;
  if (colorTab === 'ruedas') body = `<div class="wheels">${[['wSh', 'Sombras'], ['wMid', 'Medios'], ['wHi', 'Luces']].map(([k, l]) => `<div class="wheel">${wheelSVG(k, c[k])}<b>${l}</b><input type="range" data-wl="${k}" min="-1" max="1" step="0.01" value="${c[k].l}"></div>`).join('')}</div>`;
  openSheet('color', head('Color · ' + kindName[s.k], `<button class="round" data-reset data-ico="reset" title="Restablecer"></button><button class="round" data-all data-ico="layers" title="Aplicar a todos"></button>`) + tabs + body);
}
function openFade() {
  const s = getSel(); if (!s) return; const it = s.it, L = clipLen(it);
  openSheet('fade', head('Fundido · ' + kindName[s.k]) + rng('fin', 'Entrada', 0, Math.min(5, L / 2), 0.1, it.fin, FMT.fin) + rng('fout', 'Salida', 0, Math.min(5, L / 2), 0.1, it.fout, FMT.fout) +
    (s.k === 'ov' ? rng('op', 'Opacidad', 0, 1, 0.01, it.op ?? 1, FMT.op) : ''));
}
function openSound() {
  const s = getSel(); if (!s) return; const it = s.it, L = clipLen(it);
  openSheet('sound', head('Sonido · ' + kindName[s.k]) + rng('vol', 'Volumen', 0, s.k === 'au' ? 2 : 2, 0.01, it.vol, FMT.vol) +
    (s.k === 'au' ? rng('fin', 'Entrada', 0, Math.min(10, L / 2), 0.1, it.fin, FMT.fin) + rng('fout', 'Salida', 0, Math.min(10, L / 2), 0.1, it.fout, FMT.fout) : '') +
    `<div class="sh-row"><button class="pill" data-vol="0">${icon('volumeX')}Silencio</button><button class="pill" data-vol="1">100 %</button></div>`);
}
function openText(focus) {
  const s = getSel(); if (!s || s.k !== 'tx') return; const t = s.it, L = clipLen(t);
  openSheet('text', head('Texto') +
    `<textarea class="tx-input" id="txIn" rows="2">${esc(t.text)}</textarea>` +
    rng('size', 'Tamaño', 0.03, 0.25, 0.005, t.size, v => Math.round(v * 400)) +
    `<div class="label sh-sub">Color</div><div class="swatches">${TX_COLORS.map(c => `<button data-txc="${c}" style="background:${c}" class="${t.color === c ? 'on' : ''}"></button>`).join('')}</div>
     <div class="label sh-sub">Fondo</div><div class="seg">${[['none', 'Sin'], ['ink', 'Tinta'], ['paper', 'Papel'], ['hl', 'Amarillo'], ['stamp', 'Rojo']].map(([k, l]) => `<button data-txb="${k}" class="${t.bg === k ? 'on' : ''}">${l}</button>`).join('')}</div>` +
    rng('fin', 'Entrada', 0, Math.min(3, L / 2), 0.1, t.fin, FMT.fin) + rng('fout', 'Salida', 0, Math.min(3, L / 2), 0.1, t.fout, FMT.fout));
  const ta = $('txIn'); let before = null;
  ta.oninput = () => { if (before == null) before = snap(); t.text = ta.value || ' '; drawTexts(); scheduleRender(); };
  ta.onchange = () => { if (before != null) { finishCommit(before); before = null; } };
  if (focus) setTimeout(() => { ta.focus(); ta.select(); }, 350);
}
function openFormat() {
  const ref = (project.clips[0] && assetOf(project.clips[0].assetId)) || lib.items.find(i => i.w) || {};
  const { H } = outSize(ref.w, ref.h);
  openSheet('format', head('Formato') +
    `<div class="seg">${ASPECTS.map(a => `<button class="${prefs.aspect === a.id ? 'on' : ''}" data-a="${a.id}">${a.label}</button>`).join('')}</div>
     <div class="label sh-sub">Ancho</div>
     <div class="seg">${[[0, 'Original'], [1920, '1920'], [1280, '1280'], [1010, '1010'], [720, '720']].map(([w, l]) => `<button class="${prefs.width === w ? 'on' : ''}" data-w="${w}">${l}</button>`).join('')}</div>
     <div class="sh-row"><label class="out-size"><input class="num" id="fmtW" type="number" min="160" max="3840" step="2" placeholder="otro" value="${prefs.width && ![1920, 1280, 1010, 720].includes(prefs.width) ? prefs.width : ''}"></label><span class="out-size">${outLabel()}</span></div>`);
  $('fmtW').onchange = () => { const v = +$('fmtW').value; prefs.width = v ? clamp(even(v), 160, 3840) : 0; savePrefs(); renderOut(); openFormat(); };
}
function openSettings() {
  openSheet('settings', head('Configuración') +
    `<div class="sh-row"><div class="seg ico-only">
       <button data-theme="day" class="${prefs.night ? '' : 'on'}" title="Día">${icon('sun')}</button><button data-theme="night" class="${prefs.night ? 'on' : ''}" title="Noche">${icon('moon')}</button></div>
     <div class="seg ico-only"><button data-snap="1" class="${prefs.snap ? 'on' : ''}" title="Imán">${icon('magnet')}</button><button data-snap="0" class="${prefs.snap ? '' : 'on'}" title="Libre">${icon('x')}</button></div></div>`);
}
$('cfgBtn').onclick = () => sheetKind === 'settings' ? closeSheet() : openSettings();
/* elegir de la biblioteca (grilla de a 3) o de un archivo */
function openPicker(mode) {
  const items = lib.items.filter(i => editSource(i) || i.avi);
  openSheet('pick', head(mode === 'ov' ? 'Capa de video' : 'Agregar clip') +
    `<div class="pick-grid">${items.map(it => `<button class="card" data-pk="${it.id}"><span class="th">${it.thumb ? `<img src="${it.thumb}" alt="">` : `<span class="noimg">${icon('film')}</span>`}${it.dur ? `<span class="dur">${fmtShort(it.outDur || it.dur)}</span>` : ''}</span><span class="nm">${esc(baseName(it.name))}</span></button>`).join('')}
     <button class="card add" data-pkfile><span class="th"><span class="plus">${icon('plus')}</span></span><span class="nm">Archivo</span></button></div>`);
  $('sheet')._pickMode = mode;
}

let editBefore = null;
$('sheetBody').addEventListener('input', e => {
  const k = e.target.dataset.k, wl = e.target.dataset.wl; if (!k && !wl) return;
  const s = getSel();
  if (editBefore == null) editBefore = snap();
  if (sheetKind === 'color' && s) { if (wl) s.it.color[wl].l = +e.target.value; else s.it.color[k] = +e.target.value; }
  else if (s && k) s.it[k] = +e.target.value;
  const o = e.target.parentElement.querySelector('output'); if (o && k) o.textContent = (FMT[k] || (v => v))(+e.target.value);
  if (k === 'size') o.textContent = Math.round(+e.target.value * 400);
  if (s && s.k === 'v1' && sheetKind === 'color') { const st = startOf(project.clips.indexOf(s.it)); if (pv.T < st || pv.T >= st + clipLen(s.it)) { pv.T = st + 0.02; ensureVisible(); placeWorld(); } }
  if (s && s.k === 'ov' && (pv.T < s.it.start || pv.T >= s.it.start + clipLen(s.it))) { pv.T = s.it.start + 0.02; ensureVisible(); placeWorld(); }
  syncAll(pv.playing); scheduleRender();
});
$('sheetBody').addEventListener('change', e => {
  if (!(e.target.dataset.k || e.target.dataset.wl) || editBefore == null) return;
  finishCommit(editBefore); editBefore = null;
});
/* ruedas: arrastrar el punto (ángulo = tono, distancia = intensidad); doble toque vuelve al centro */
let wheelDrag = null;
$('sheetBody').addEventListener('pointerdown', e => {
  const svg = e.target.closest('[data-wheel]'); if (!svg) return;
  const s = getSel(); if (!s) return;
  svg.setPointerCapture(e.pointerId);
  wheelDrag = { svg, key: svg.dataset.wheel, c: s.it.color, before: snap() };
  wheelMove(e);
});
function wheelMove(e) {
  const w = wheelDrag; if (!w) return;
  const r = w.svg.getBoundingClientRect(), x = (e.clientX - r.left) / r.width * 100 - 50, y = (e.clientY - r.top) / r.height * 100 - 50;
  const a = clamp(Math.hypot(x, y) / 48, 0, 1), h = (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
  w.c[w.key].a = a < 0.06 ? 0 : a; w.c[w.key].h = Math.round(h);
  w.svg.outerHTML; // el svg se redibuja abajo
  const fresh = new DOMParser().parseFromString(wheelSVG(w.key, w.c[w.key]), 'image/svg+xml').documentElement;
  w.svg.innerHTML = fresh.innerHTML;
  syncAll(false);
}
$('sheetBody').addEventListener('pointermove', e => { if (wheelDrag) wheelMove(e); });
$('sheetBody').addEventListener('pointerup', () => { if (wheelDrag) { finishCommit(wheelDrag.before); wheelDrag = null; } });
$('sheetBody').addEventListener('dblclick', e => {
  const svg = e.target.closest('[data-wheel]'); const s = getSel(); if (!svg || !s) return;
  commit(() => { s.it.color[svg.dataset.wheel] = { ...NEUTRAL[svg.dataset.wheel] }; }); openColor();
});
$('sheetBody').addEventListener('click', e => {
  const s = getSel();
  if (sheetKind === 'color' && s) {
    const tb = e.target.closest('[data-ctab]'); if (tb) { colorTab = tb.dataset.ctab; openColor(); return; }
    if (e.target.closest('[data-bw]')) { commit(() => { s.it.color.bw = !s.it.color.bw; }); openColor(); }
  }
  if (e.target.closest('[data-reset]') && s) { commit(() => { s.it.color = newColor(); }); openColor(); }
  if (e.target.closest('[data-all]') && s) { const c = JSON.stringify(s.it.color); commit(() => { project.clips.forEach(x => { x.color = JSON.parse(c); }); project.layers.forEach(l => l.items.forEach(x => { x.color = JSON.parse(c); })); }); toast('Color en todos'); }
  const vb = e.target.closest('[data-vol]'); if (vb && s) { commit(() => { s.it.vol = +vb.dataset.vol; }); openSound(); }
  const tc = e.target.closest('[data-txc]'); if (tc && s) { commit(() => { s.it.color = tc.dataset.txc; }); openText(); }
  const tb2 = e.target.closest('[data-txb]'); if (tb2 && s) { commit(() => { s.it.bg = tb2.dataset.txb; }); openText(); }
  if (sheetKind === 'format') {
    const b = e.target.closest('[data-a]'); if (b) { prefs.aspect = b.dataset.a; savePrefs(); renderOut(); layoutFrame(); openFormat(); }
    const w = e.target.closest('[data-w]'); if (w) { prefs.width = +w.dataset.w; savePrefs(); renderOut(); openFormat(); }
  }
  if (sheetKind === 'settings') {
    const th = e.target.closest('[data-theme]'), sn = e.target.closest('[data-snap]');
    if (th) { prefs.night = th.dataset.theme === 'night'; savePrefs(); syncNight(); openSettings(); }
    if (sn) { prefs.snap = sn.dataset.snap === '1'; savePrefs(); syncSnapBtn(); openSettings(); }
  }
  if (sheetKind === 'pick') {
    const mode = $('sheet')._pickMode, pk = e.target.closest('[data-pk]');
    if (pk) { closeSheet(); const it = itemById(pk.dataset.pk); mode === 'ov' ? addLayerClip([it]) : sendToEditor([it]); }
    if (e.target.closest('[data-pkfile]')) { $('clipIn')._mode = mode; $('clipIn').click(); }
  }
  if (sheetKind === 'export') {
    if (e.target.closest('[data-export]')) exportMontage();
    if (e.target.closest('[data-dl]') && lastExport) downloadBlob(lastExport, 'KyroAviMp4_montaje.mp4');
    if (e.target.closest('[data-cancel]')) { engine.cancel = true; killFF(); }
  }
});

/* ================= exportación del montaje ================= */
let lastExport = null;
function exportSize() {
  const first = project.clips[0] && itemById(project.clips[0].assetId);
  const { W, H } = outSize(first && first.w, first && first.h);
  return { W, H: H || even(W * 9 / 16) };
}
function openExport(state = {}) {
  const { W, H } = exportSize();
  openSheet('export', head('Exportar') +
    `<div class="m-info"><span>${fmtT(projectEnd())}</span><span>${W} × ${H}</span><span>${project.clips.length + project.layers.reduce((s, l) => s + l.items.length, 0)} clips</span>${project.audios.length ? `<span>${project.audios.length} audio</span>` : ''}${project.texts.length ? `<span>${project.texts.length} texto</span>` : ''}</div>
     ${state.busy ? `<div class="prog sh-row" style="color:var(--text)"><span id="exTxt">Preparando…</span><span class="track" style="background:var(--bg)"><i id="exBar"></i></span><button class="round" data-cancel data-ico="x" title="Cancelar"></button></div>` : ''}
     <div class="sh-row">
       ${lastExport && !state.busy ? `<button class="pill" data-dl><span data-ico="download"></span>${fmtBytes(lastExport.size)}</button>` : ''}
       <button class="stamp" data-export ${!project.clips.length || engine.busy ? 'disabled' : ''}><span data-ico="download"></span>Exportar MP4</button>
     </div>`);
}
/* ¿el archivo tiene sonido? lo decide el navegador, sin ocupar el motor */
const audioCache = new Map();
async function hasAudio(blob) {
  if (audioCache.has(blob)) return audioCache.get(blob);
  let ok = false;
  try { const Ctx = window.OfflineAudioContext || window.webkitOfflineAudioContext; const ctx = new Ctx(1, 44100, 44100); const buf = await ctx.decodeAudioData(await blob.arrayBuffer()); ok = !!buf && buf.length > 0; } catch (_) { ok = false; }
  audioCache.set(blob, ok); return ok;
}
const n3 = v => (+v).toFixed(3);
async function exportMontage() {
  if (engine.busy || !project.clips.length) return;
  pause();
  const usados = [...new Set([...project.clips.map(c => c.assetId), ...project.layers.flatMap(l => l.items.map(i => i.assetId))])].map(itemById).filter(Boolean);
  const faltan = usados.filter(i => !fullSource(i));
  if (faltan.length) { closeSheet(); toast('Convirtiendo los AVI en calidad final…'); await convertItems(faltan); if (faltan.some(i => !fullSource(i))) { toast('No se pudieron convertir todos los AVI'); return; } }
  engine.busy = true; engine.cancel = false;
  openExport({ busy: true });
  const bar = r => { const b = $('exBar'); if (b) b.style.width = (r * 100).toFixed(1) + '%'; };
  const txt = t => { const x = $('exTxt'); if (x) x.textContent = t; };
  const files = [];
  try {
    const ff = await getFF(); hideProg();
    const { W, H } = exportSize(), D = projectEnd();
    const args = ['-hide_banner'], f = [], mix = [];
    let nIn = 0;
    const addIn = (name, pre = []) => { args.push(...pre, '-i', name); files.push(name); return nIn++; };
    // videos usados (V1 y capas)
    const vids = project.hidden.V1 ? [] : project.clips;
    const ovs = project.layers.flatMap((l, k) => project.hidden['L' + l.id] ? [] : l.items.map(it => ({ it, l, k })));
    const ids = [...new Set([...project.clips.map(c => c.assetId), ...ovs.map(o => o.it.assetId)])];
    const idx = {}, aud = {};
    for (let k = 0; k < ids.length; k++) {
      txt(`Cargando ${k + 1}/${ids.length}…`); bar(k / ids.length * 0.08);
      const blob = fullSource(itemById(ids[k]));
      aud[ids[k]] = await hasAudio(blob);
      const name = `v${k}.mp4`; ff.FS('writeFile', name, new Uint8Array(await blob.arrayBuffer()));
      idx[ids[k]] = addIn(name);
    }
    // V1: secuencia con fundidos a negro, color y su audio
    const segs = []; let base = 0;
    project.clips.forEach((c, k) => {
      const L = clipLen(c), ix = idx[c.assetId], col = colorFilterFF(c.color);
      let vf = `[${ix}:v]trim=start=${n3(c.in)}:end=${n3(c.out)},setpts=PTS-STARTPTS,fps=30,scale=${W}:${H}:force_original_aspect_ratio=decrease,pad=${W}:${H}:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1,format=yuv420p${col ? ',' + col : ''}`;
      if (c.fin > 0) vf += `,fade=t=in:st=0:d=${n3(c.fin)}`;
      if (c.fout > 0) vf += `,fade=t=out:st=${n3(Math.max(0, L - c.fout))}:d=${n3(c.fout)}`;
      if (project.hidden.V1) vf = `color=c=black:s=${W}x${H}:r=30:d=${n3(L)},format=yuv420p,setsar=1`;
      f.push(vf + `[v${k}]`);
      if (aud[c.assetId] && !project.muted.V1 && c.vol > 0) {
        let af = `[${ix}:a]atrim=start=${n3(c.in)}:end=${n3(c.out)},asetpts=PTS-STARTPTS,aresample=44100,aformat=channel_layouts=stereo,volume=${n3(c.vol)}`;
        if (c.fin > 0) af += `,afade=t=in:st=0:d=${n3(c.fin)}`;
        if (c.fout > 0) af += `,afade=t=out:st=${n3(Math.max(0, L - c.fout))}:d=${n3(c.fout)}`;
        f.push(af + `[a${k}]`);
      } else f.push(`anullsrc=r=44100:cl=stereo,atrim=0:${n3(L)}[a${k}]`);
      segs.push(`[v${k}][a${k}]`); base += L;
    });
    void vids;
    if (D - base > 0.02) { // el resto en negro (capas, texto o audio que siguen después)
      const g = D - base, k = project.clips.length;
      f.push(`color=c=black:s=${W}x${H}:r=30:d=${n3(g)},format=yuv420p,setsar=1[v${k}]`, `anullsrc=r=44100:cl=stereo,atrim=0:${n3(g)}[a${k}]`);
      segs.push(`[v${k}][a${k}]`);
    }
    f.push(`${segs.join('')}concat=n=${segs.length}:v=1:a=1[base][abase]`);
    mix.push('[abase]');
    let cur = '[base]', n = 0;
    // capas de video encima
    ovs.forEach(({ it, l }, j) => {
      const L = clipLen(it), ix = idx[it.assetId], col = colorFilterFF(it.color), s = it.start;
      let vf = `[${ix}:v]trim=start=${n3(it.in)}:end=${n3(it.out)},setpts=PTS-STARTPTS,fps=30,scale=${W}:${H}:force_original_aspect_ratio=decrease,setsar=1${col ? ',' + col : ''},format=rgba`;
      if ((it.op ?? 1) < 1) vf += `,colorchannelmixer=aa=${n3(it.op)}`;
      if (it.fin > 0) vf += `,fade=t=in:st=0:d=${n3(it.fin)}:alpha=1`;
      if (it.fout > 0) vf += `,fade=t=out:st=${n3(Math.max(0, L - it.fout))}:d=${n3(it.fout)}:alpha=1`;
      f.push(vf + `,setpts=PTS+${n3(s)}/TB[o${j}]`);
      f.push(`${cur}[o${j}]overlay=(W-w)/2:(H-h)/2:eof_action=pass:enable='between(t,${n3(s)},${n3(s + L)})'[c${++n}]`); cur = `[c${n}]`;
      if (aud[it.assetId] && !project.muted['L' + l.id] && it.vol > 0) {
        const ms = Math.round(s * 1000);
        let af = `[${ix}:a]atrim=start=${n3(it.in)}:end=${n3(it.out)},asetpts=PTS-STARTPTS,aresample=44100,aformat=channel_layouts=stereo,volume=${n3(it.vol)}`;
        if (it.fin > 0) af += `,afade=t=in:st=0:d=${n3(it.fin)}`;
        if (it.fout > 0) af += `,afade=t=out:st=${n3(Math.max(0, L - it.fout))}:d=${n3(it.fout)}`;
        f.push(af + `,adelay=${ms}|${ms}[oa${j}]`); mix.push(`[oa${j}]`);
      }
    });
    // textos: se dibujan con el mismo código que la preview y se superponen
    if (!project.hidden.T) for (let j = 0; j < project.texts.length; j++) {
      const t = project.texts[j], L = clipLen(t), s = t.start, name = `t${j}.png`;
      ff.FS('writeFile', name, await textPNG(t, W, H));
      const ix = addIn(name, ['-loop', '1', '-framerate', '30', '-t', n3(L)]);
      let vf = `[${ix}:v]format=rgba`;
      if (t.fin > 0) vf += `,fade=t=in:st=0:d=${n3(t.fin)}:alpha=1`;
      if (t.fout > 0) vf += `,fade=t=out:st=${n3(Math.max(0, L - t.fout))}:d=${n3(t.fout)}:alpha=1`;
      f.push(vf + `,setpts=PTS-STARTPTS+${n3(s)}/TB[t${j}]`);
      f.push(`${cur}[t${j}]overlay=0:0:eof_action=pass:enable='between(t,${n3(s)},${n3(s + L)})'[c${++n}]`); cur = `[c${n}]`;
    }
    // pistas de audio
    for (let j = 0; j < project.audios.length; j++) {
      const a = project.audios[j]; if (project.muted['A' + a.id] || a.vol <= 0) continue;
      const blob = (audData.get(a.id) || {}).blob || await DB.getBlob(a.key); if (!blob) continue;
      const name = `a${j}.bin`; ff.FS('writeFile', name, new Uint8Array(await blob.arrayBuffer()));
      const ix = addIn(name), L = clipLen(a), ms = Math.round(a.start * 1000);
      let af = `[${ix}:a]atrim=start=${n3(a.in)}:end=${n3(a.out)},asetpts=PTS-STARTPTS,aresample=44100,aformat=channel_layouts=stereo`;
      if (a.fin > 0) af += `,afade=t=in:st=0:d=${n3(a.fin)}`;
      if (a.fout > 0) af += `,afade=t=out:st=${n3(Math.max(0, L - a.fout))}:d=${n3(a.fout)}`;
      f.push(af + `,volume=${n3(a.vol)},adelay=${ms}|${ms}[au${j}]`); mix.push(`[au${j}]`);
    }
    f.push(`${cur}format=yuv420p[vout]`);
    if (mix.length > 1) f.push(`${mix.join('')}amix=inputs=${mix.length}:duration=first:dropout_transition=0,volume=${mix.length}[aout]`);
    else f.push(`[abase]anull[aout]`);
    engine.log = []; engine.onProgress = null;
    const timer = setInterval(() => {
      const l = engine.log.slice().reverse().find(x => /time=\d+:\d+:\d+/.test(x));
      if (l) { const [, hh, mm, ss] = l.match(/time=(\d+):(\d+):([\d.]+)/); const t = +hh * 3600 + +mm * 60 + +ss; bar(0.08 + 0.92 * clamp(t / D, 0, 1)); txt(`Exportando ${Math.round(clamp(t / D, 0, 1) * 100)} %`); }
    }, 400);
    txt('Exportando…');
    try {
      await run(ff, [...args, '-filter_complex', f.join(';'), '-map', '[vout]', '-map', '[aout]', '-t', n3(D), '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '22', '-pix_fmt', 'yuv420p',
        '-c:a', 'aac', '-b:a', '160k', '-movflags', '+faststart', '-max_muxing_queue_size', '4096', '-y', 'montaje.mp4']);
    } finally { clearInterval(timer); }
    lastExport = new Blob([readOut(ff, 'montaje.mp4')], { type: 'video/mp4' });
    killFF();
    toast('Montaje exportado');
    downloadBlob(lastExport, 'KyroAviMp4_montaje.mp4');
  } catch (e) {
    console.warn(e, engine.log.slice(-25).join('\n'));
    if (!engine.cancel) toast('No se pudo exportar');
    killFF();
  } finally {
    engine.busy = false; engine.onProgress = null;
    if (sheetKind === 'export') openExport();
  }
}

/* ================= navegación, tema, atajos ================= */
function go(name) {
  document.querySelectorAll('.screen').forEach(s => s.classList.toggle('show', s.id === name));
  document.querySelectorAll('[data-go]').forEach(b => b.classList.toggle('on', b.dataset.go === name));
  if (name === 'editor') requestAnimationFrame(() => { layoutFrame(); renderTimeline(); syncAll(false); });
  else { pause(); if (sheetKind !== 'settings') closeSheet(); }
}
document.querySelectorAll('[data-go]').forEach(b => b.onclick = () => go(b.dataset.go));
function syncNight() { document.documentElement.classList.toggle('night', !!prefs.night); document.querySelector('meta[name=theme-color]').content = prefs.night ? '#16130f' : '#2b6b58'; }
document.addEventListener('keydown', e => {
  if (e.target.matches('input,textarea')) return;
  const inEditor = $('editor').classList.contains('show');
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') { e.preventDefault(); e.shiftKey ? redo() : undo(); return; }
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') { e.preventDefault(); redo(); return; }
  if (!inEditor) return;
  if (e.code === 'Space') { e.preventDefault(); pv.playing ? pause() : play(); }
  if (e.key.toLowerCase() === 's') splitAtHead();
  if (e.key.toLowerCase() === 'f') $('fsBtn').click();
  if ((e.key === 'Delete' || e.key === 'Backspace') && getSel()) { const s = getSel(); commit(() => { s.list.splice(s.list.indexOf(s.it), 1); }); }
  if (e.key === 'ArrowLeft') { pause(); setT(pv.T - (e.shiftKey ? 1 : 1 / 30)); }
  if (e.key === 'ArrowRight') { pause(); setT(pv.T + (e.shiftKey ? 1 : 1 / 30)); }
  if (e.key === 'Home') { pause(); setT(0); }
});

/* ================= arranque ================= */
async function init() {
  paintIcons(); syncNight(); syncSnapBtn(); renderOut();
  vA.poster = NOPOSTER; vB.poster = NOPOSTER;
  $('playBtn').innerHTML = icon('play');
  const saved = await DB.allItems();
  if (saved && saved.length) lib.items = saved.map(s => ({ ...s, progress: 0 }));
  const okAsset = id => itemById(id) && editSource(itemById(id));
  project.clips = project.clips.filter(c => okAsset(c.assetId));
  project.layers.forEach(l => { l.items = l.items.filter(c => okAsset(c.assetId)); });
  project.layers = project.layers.filter(l => l.items.length);
  renderGrid();
  lib.items.filter(i => !editSource(i)).forEach(i => { i.status = 'idle'; queuePreview(i); });
  project.audios.forEach(ensureAudio);
  try { await document.fonts.load('800 20px Archivo'); } catch (_) {}
  renderTimeline();
}
window.__kyro = { project: () => project, view, pv, assets, hist, exp: () => lastExport };
init();
/* botón Atrás de Android: cierra lo que esté abierto antes de salir */
window.kyroBack = () => {
  if (document.fullscreenElement) { document.exitFullscreen().catch(() => {}); return true; }
  if ($('ov').classList.contains('show')) { closePreview(); return true; }
  if (sheetKind) { closeSheet(); return true; }
  if (view.sel) { deselect(); return true; }
  if ($('editor').classList.contains('show')) { go('videos'); return true; }
  return false;
};
window.kyroPause = () => pause();
if (!window.KyroAndroid && 'serviceWorker' in navigator && location.protocol !== 'file:') navigator.serviceWorker.register('sw.js').catch(e => console.warn('sw', e));
})();
