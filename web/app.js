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
const prefs = Object.assign({ width: 1010, aspect: 'original', night: false, snap: true }, readJSON(PREF_KEY));
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
  const W = even(prefs.width || 1010);
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
const editSource = it => it && (it.out || (it.playable ? it.file : null));
const persistItem = it => DB.putItem({ id: it.id, name: it.name, size: it.size, avi: it.avi, file: it.file, out: it.out || null, dur: it.dur, w: it.w, h: it.h,
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
  if (it.status === 'error') return it.err || 'No se pudo convertir';
  if (it.out) return `MP4 listo · ${fmtBytes(it.out.size)}`;
  return `${fmtBytes(it.size)}${it.w ? ` · ${it.w}×${it.h}` : ''}`;
}
function renderGrid() {
  const g = $('grid');
  if (!lib.items.length) {
    g.innerHTML = '<div class="empty">Todavía no hay videos. Tocá <b>Agregar videos</b>: aparecen acá de a tres, con su miniatura.</div>';
  } else {
    g.innerHTML = lib.items.map(it => {
      const busy = it.status === 'converting' || it.status === 'probing';
      return `<button class="card${lib.picked.has(it.id) ? ' picked' : ''}" data-id="${it.id}">
        <span class="th">${it.thumb ? `<img src="${it.thumb}" alt="">` : `<span class="noimg">${icon('film')}</span>`}
          <span class="tags"><span class="chip${it.avi ? ' red' : ''}">${it.avi ? 'AVI' : 'Video'}</span>${it.out ? '<span class="chip">MP4</span>' : ''}</span>
          <span class="sel" data-pick="${it.id}">${icon('check')}</span>
          ${it.dur ? `<span class="dur">${fmtShort(it.outDur || it.dur)}</span>` : ''}
          ${it.status === 'converting' ? `<span class="busy">${Math.round(it.progress * 100)} %</span><span class="bar"><i style="width:${it.progress * 100}%"></i></span>` : ''}
          ${it.status === 'probing' ? '<span class="busy">…</span>' : ''}
        </span>
        <span class="nm">${esc(it.name)}</span>
        <span class="mt${it.status === 'error' ? ' err' : ''}">${esc(statusLine(it))}</span>
      </button>`;
    }).join('');
  }
  const n = lib.items.length, p = lib.picked.size;
  $('libCount').textContent = n ? `${n} · ${p ? p + ' elegidos' : 'tocá el círculo para elegir'}` : '';
  $('pickAllTxt').textContent = p && p === n ? 'Ninguno' : 'Elegir todos';
  $('pickAllBtn').disabled = !n;
  const target = targets();
  $('removeBtn').disabled = !p || engine.busy;
  $('toEditorBtn').disabled = !target.length || engine.busy;
  const conv = target.filter(i => !i.out);
  $('convertBtn').disabled = !conv.length || engine.busy;
  $('convertTxt').textContent = conv.length > 1 ? `Convertir ${conv.length} a MP4` : 'Convertir a MP4';
  $('navN').hidden = !n; $('navN').textContent = n;
}
const targets = () => lib.picked.size ? lib.items.filter(i => lib.picked.has(i.id)) : lib.items.slice();

$('grid').addEventListener('click', e => {
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
  if (project.clips.some(c => c.assetId === id)) { toast('Ese video está en el editor: se quitó de la línea de tiempo.'); commit(() => { project.clips = project.clips.filter(c => c.assetId !== id); }); }
  lib.items.splice(i, 1); DB.delItem(id);
}

/* ---------- salida: ancho y proporción ---------- */
function renderOut() {
  $('outW').value = prefs.width;
  $('aspectSeg').innerHTML = ASPECTS.map(a => `<button class="${prefs.aspect === a.id ? 'on' : ''}" data-a="${a.id}">${a.label}</button>`).join('');
  const ref = lib.items.find(i => i.w) || {};
  const { W, H } = outSize(ref.w, ref.h);
  $('outH').innerHTML = H ? `× ${H} <small>px</small>` : '× auto <small>px</small>';
  $('aspectHint').textContent = prefs.aspect === 'original' ? 'Mantiene la proporción original de cada video.' : `Ajusta el video al formato ${prefs.aspect} sin recortarlo (agrega bandas negras si hace falta). ${W} × ${H} px.`;
  $('fmtChip').textContent = `${prefs.aspect === 'original' ? 'Original' : prefs.aspect} · ${W}px`;
}
$('aspectSeg').addEventListener('click', e => { const b = e.target.closest('[data-a]'); if (!b) return; prefs.aspect = b.dataset.a; savePrefs(); renderOut(); layoutFrame(); });
$('outW').addEventListener('change', () => { prefs.width = clamp(even(+$('outW').value || 1010), 160, 3840); savePrefs(); renderOut(); });

/* ---------- agregar / soltar ---------- */
$('addBtn').onclick = () => $('fileIn').click();
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
  const src = it.out || (it.playable ? it.file : it.sample);
  const stage = $('mStage');
  const cur = stage.querySelector('video');
  if (src) {
    if (!cur || cur.dataset.src !== urlFor(src)) {
      stage.innerHTML = `<video controls playsinline autoplay data-src="${urlFor(src)}" src="${urlFor(src)}"></video>`;
    }
  } else {
    const busy = engine.busy;
    stage.innerHTML = `<div class="m-msg">${icon('film')}<span>El navegador no abre AVI directamente. Podés generar una muestra de 3 segundos o convertirlo a MP4.</span>
      <button class="pill" id="mSample" ${busy ? 'disabled' : ''}><span>${icon('eye')}</span>${busy ? 'El motor está ocupado…' : 'Ver muestra de 3 s'}</button></div>`;
    const b = $('mSample'); if (b) b.onclick = () => makeSample(it);
  }
  const info = [];
  if (it.w) info.push(`${it.w} × ${it.h} px`);
  if (it.dur) info.push(fmtShort(it.dur));
  info.push(fmtBytes(it.size));
  if (it.out) info.push(`MP4 ${fmtBytes(it.out.size)}`);
  if (it.sample && !it.out && !it.playable) info.push('Mostrando muestra de 3 s');
  $('mInfo').innerHTML = info.map(t => `<span>${esc(t)}</span>`).join('');
  const acts = [];
  acts.push(`<button class="pill" data-m="remove"><span>${icon('trash')}</span>Quitar</button>`);
  if (it.out) acts.push(`<button class="pill" data-m="download"><span>${icon('download')}</span>Descargar MP4</button>`);
  else acts.push(`<button class="pill" data-m="convert" ${engine.busy ? 'disabled' : ''}><span>${icon('convert')}</span>Convertir</button>`);
  acts.push(`<button class="stamp" data-m="editor" ${engine.busy && !editSource(it) ? 'disabled' : ''}><span>${icon('timeline')}</span>Al editor</button>`);
  $('mActions').innerHTML = acts.join('');
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
  const a = document.createElement('a'); a.href = urlFor(blob); a.download = name; document.body.appendChild(a); a.click(); a.remove();
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
$('cancelBtn').onclick = () => { engine.cancel = true; killFF(); toast('Cancelado'); };

function convertArgs(inN, outN, it, noAudio) {
  const { W, H } = outSize(it.w, it.h);
  const vf = prefs.aspect === 'original' ? `scale=${W}:-2,setsar=1`
    : `scale=${W}:${H}:force_original_aspect_ratio=decrease,pad=${W}:${H}:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1`;
  return ['-fflags', '+genpts', '-err_detect', 'ignore_err', '-i', inN, '-map', '0:v:0', ...(noAudio ? ['-an'] : ['-map', '0:a:0?']),
    '-vf', vf, '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '23', '-pix_fmt', 'yuv420p',
    ...(noAudio ? [] : ['-c:a', 'aac', '-b:a', '128k']), '-movflags', '+faststart', '-max_muxing_queue_size', '2048', '-y', outN];
}
async function convertItems(items) {
  items = items.filter(i => !i.out && i.status !== 'converting');
  if (!items.length || engine.busy) return [];
  engine.busy = true; engine.cancel = false;
  items.forEach(i => { i.status = 'queued'; i.err = ''; });
  renderGrid();
  const done = [];
  try {
    let ff = await getFF();
    for (let k = 0; k < items.length && !engine.cancel; k++) {
      const it = items[k];
      it.status = 'converting'; it.progress = 0; renderGrid();
      const label = `Convirtiendo ${k + 1} de ${items.length}: ${it.name}`;
      engine.onProgress = r => { it.progress = r; setProg((k + r) / items.length, label); updateCard(it); };
      const ext = (it.name.match(/\.[^.]+$/) || ['.avi'])[0].toLowerCase();
      const inN = 'in' + ext, outN = 'out.mp4';
      try {
        const bytes = new Uint8Array(await it.file.arrayBuffer());
        ff.FS('writeFile', inN, bytes);
        let data;
        try { await run(ff, convertArgs(inN, outN, it, false)); data = readOut(ff, outN); }
        catch (e) {
          if (engine.cancel) throw e;
          // AVI viejos con audio raro: motor nuevo y reintento sin audio
          killFF(); ff = await getFF(); ff.FS('writeFile', inN, bytes);
          await run(ff, convertArgs(inN, outN, it, true)); data = readOut(ff, outN);
        }
        it.out = new Blob([data], { type: 'video/mp4' });
        const p = await probe(it.out);
        if (p.ok) { if (!it.w) { it.w = p.w; it.h = p.h; } it.outDur = p.dur; if (!it.dur) it.dur = p.dur; if (!it.thumb || !it.playable) it.thumb = p.thumb; }
        it.status = 'done'; done.push(it);
      } catch (e) {
        killFF(); if (!engine.cancel) ff = await getFF();
        it.status = engine.cancel ? 'idle' : 'error';
        it.err = engine.cancel ? '' : 'No se pudo convertir este archivo';
        if (engine.cancel) break;
        console.warn(e);
      } finally { if (engine.ff) { rm(engine.ff, inN); rm(engine.ff, outN); } }
      persistItem(it); renderGrid();
    }
    if (!engine.cancel) toast(done.length ? `Listo: ${done.length} MP4` : 'No se pudo convertir');
  } catch (e) {
    if (!engine.cancel) toast(e.message || 'Error del motor');
    items.forEach(i => { if (i.status !== 'done') { i.status = 'idle'; } });
  } finally {
    engine.busy = false; engine.onProgress = null; hideProg();
    items.forEach(i => { if (i.status === 'queued' || i.status === 'converting') i.status = 'idle'; });
    renderGrid(); if (modalId) renderModal();
  }
  return done;
}
function updateCard(it) {
  const card = document.querySelector(`.card[data-id="${it.id}"]`); if (!card) return;
  const b = card.querySelector('.bar i'); if (b) b.style.width = (it.progress * 100) + '%';
  const s = card.querySelector('.busy'); if (s) s.textContent = Math.round(it.progress * 100) + ' %';
  const m = card.querySelector('.mt'); if (m) m.textContent = statusLine(it);
}
async function makeSample(it) {
  if (engine.busy) return;
  engine.busy = true; engine.cancel = false; renderModal();
  try {
    const ff = await getFF();
    engine.onProgress = r => setProg(r, 'Generando muestra de 3 s…');
    ff.FS('writeFile', 'sin.avi', new Uint8Array(await it.file.arrayBuffer()));
    await run(ff, ['-fflags', '+genpts', '-err_detect', 'ignore_err', '-t', '3', '-i', 'sin.avi', '-an', '-vf', 'scale=480:-2,setsar=1', '-c:v', 'libx264', '-preset', 'ultrafast', '-pix_fmt', 'yuv420p', '-y', 's.mp4']);
    it.sample = new Blob([readOut(ff, 's.mp4')], { type: 'video/mp4' });
    rm(ff, 'sin.avi'); rm(ff, 's.mp4');
    const p = await probe(it.sample);
    if (p.ok && !it.thumb) { it.thumb = p.thumb; it.w = it.w || 0; persistItem(it); }
  } catch (e) { if (!engine.cancel) toast('No se pudo generar la muestra'); console.warn(e); }
  finally { engine.busy = false; engine.onProgress = null; hideProg(); renderGrid(); if (modalId) renderModal(); }
}

/* ================= editor: modelo, historial ================= */
const PROJ_KEY = 'kyroavimp4.project';
const NEUTRAL = { bw: false, sat: 1, exp: 0, con: 0, hi: 0, sh: 0, shHue: 30, shAmt: 0, hiHue: 45, hiAmt: 0 };
const MIN_LEN = 0.2;
let project = Object.assign({ clips: [], music: null }, readJSON(PROJ_KEY));
project.clips = (project.clips || []).filter(c => c && c.assetId);
const hist = { undo: [], redo: [] };
const snap = () => JSON.stringify({ clips: project.clips, music: project.music });
function commit(fn) {
  const before = snap();
  fn();
  if (snap() === before) return;
  hist.undo.push(before); if (hist.undo.length > 80) hist.undo.shift();
  hist.redo.length = 0;
  saveProject(); onProjectChange();
}
function restore(s) { const o = JSON.parse(s); project.clips = o.clips; project.music = o.music; if (project.music) ensureMusic(); saveProject(); onProjectChange(); }
function undo() { if (!hist.undo.length) return; hist.redo.push(snap()); restore(hist.undo.pop()); toast('Deshecho'); }
function redo() { if (!hist.redo.length) return; hist.undo.push(snap()); restore(hist.redo.pop()); toast('Rehecho'); }
function saveProject() { try { localStorage.setItem(PROJ_KEY, JSON.stringify({ clips: project.clips, music: project.music })); } catch (_) {} }
$('undoBtn').onclick = undo; $('redoBtn').onclick = redo;
const clipLen = c => Math.max(0, c.out - c.in);
const totalDur = () => project.clips.reduce((s, c) => s + clipLen(c), 0);
function startOf(i, clips = project.clips) { let s = 0; for (let k = 0; k < i; k++) s += clipLen(clips[k]); return s; }
function locate(t) {
  let s = 0; const cs = project.clips;
  for (let i = 0; i < cs.length; i++) { const L = clipLen(cs[i]); if (t < s + L || i === cs.length - 1) return { i, clip: cs[i], start: s, local: cs[i].in + clamp(t - s, 0, L) }; s += L; }
  return null;
}

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
      const url = await grab(v, t, 120, 0.6);
      a.frames.push({ t, url });
      if (k % 6 === 5 || k === n - 1) scheduleRender();
    }
  } catch (e) { console.warn('fotogramas', e); }
  v.removeAttribute('src'); v.load();
}
const frameAt = (a, t) => { if (!a || !a.frames.length) return ''; let best = a.frames[0]; for (const f of a.frames) if (Math.abs(f.t - t) < Math.abs(best.t - t)) best = f; return best.url; };

async function sendToEditor(items) {
  items = items.filter(Boolean);
  if (!items.length) return;
  const need = items.filter(i => !editSource(i));
  if (need.length) { toast(`Convirtiendo ${need.length} AVI para poder editarlos…`); await convertItems(need); }
  const ok = items.filter(i => editSource(i));
  if (!ok.length) return;
  commit(() => {
    for (const it of ok) {
      const dur = it.outDur || it.dur || 0;
      if (!dur) continue;
      project.clips.push({ id: uid(), assetId: it.id, in: 0, out: dur, color: { ...NEUTRAL } });
    }
  });
  lib.picked.clear(); renderGrid();
  view.fitDone = false;
  go('editor');
  toast(ok.length === 1 ? 'Clip agregado a la línea de tiempo' : `${ok.length} clips agregados`);
}

/* ================= editor: color (mismo cálculo para preview y exportación) ================= */
const HUES = [0, 30, 45, 60, 120, 180, 215, 275];
function hueRGB(h) { const f = n => { const k = (n + h / 30) % 12; return 0.5 - 0.5 * Math.max(-1, Math.min(k - 3, 9 - k, 1)); }; return [f(0), f(8), f(4)]; }
const isNeutral = c => !c || (!c.bw && c.sat === 1 && !c.exp && !c.con && !c.hi && !c.sh && !c.shAmt && !c.hiAmt);
function curves(c, n = 9) {
  const sRGB = hueRGB(c.shHue), hRGB = hueRGB(c.hiHue);
  const out = [[], [], []];
  for (let k = 0; k < n; k++) {
    const x = k / (n - 1);
    let y = x * Math.pow(2, c.exp);
    y = (y - 0.5) * (1 + c.con) + 0.5;
    const ws = Math.pow(Math.max(0, 1 - x / 0.6), 2), wh = Math.pow(Math.max(0, (x - 0.4) / 0.6), 2);
    y += c.sh * 0.3 * ws + c.hi * 0.3 * wh;
    for (let ch = 0; ch < 3; ch++) {
      const v = y + c.shAmt * 0.22 * (sRGB[ch] - 0.5) * ws + c.hiAmt * 0.22 * (hRGB[ch] - 0.5) * wh;
      out[ch].push(clamp(v, 0, 1));
    }
  }
  return out;
}
const satOf = c => c.bw ? 0 : c.sat;
function applyPreviewColor(c) {
  const on = !isNeutral(c);
  [vA, vB].forEach(v => { v.style.filter = on ? 'url(#fx)' : 'none'; });
  if (!on) return;
  const [r, g, b] = curves(c);
  $('fxSat').setAttribute('values', satOf(c).toFixed(3));
  $('fxR').setAttribute('tableValues', r.map(v => v.toFixed(4)).join(' '));
  $('fxG').setAttribute('tableValues', g.map(v => v.toFixed(4)).join(' '));
  $('fxB').setAttribute('tableValues', b.map(v => v.toFixed(4)).join(' '));
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

/* ================= editor: preview instantánea ================= */
const vA = $('vA'), vB = $('vB'), audio = $('music');
const pv = { T: 0, playing: false, act: vA, idx: -1, pending: null, raf: 0 };
const other = v => v === vA ? vB : vA;
function setSrc(v, a) { if (v._aid === a.id) return; v._aid = a.id; v.src = a.url; }
function seekVideo(v, t) {
  if (v.readyState < 1) { v._want = t; v.addEventListener('loadedmetadata', () => { if (v._want != null) { v.currentTime = v._want; v._want = null; } }, { once: true }); return; }
  if (v.seeking) { v._want = t; return; }
  v.currentTime = t;
}
[vA, vB].forEach(v => v.addEventListener('seeked', () => { if (v._want != null) { const t = v._want; v._want = null; v.currentTime = t; } }));
function showVideo(v) { pv.act = v; v.classList.add('on'); other(v).classList.remove('on'); other(v).pause(); other(v).muted = true; }
/* muestra el cuadro exacto del tiempo t: solo busca en el archivo, no genera nada */
function showFrame(t, opts = {}) {
  const loc = locate(t);
  $('framePh').hidden = !!loc;
  if (!loc) { vA.classList.remove('on'); vB.classList.remove('on'); $('clipBadge').hidden = true; return; }
  const a = assetOf(loc.clip.assetId); if (!a) return;
  const local = opts.at != null ? opts.at : loc.local;
  let v = pv.act;
  if (v._aid !== a.id && other(v)._aid === a.id) v = other(v);
  setSrc(v, a);
  showVideo(v);
  seekVideo(v, Math.min(local, (a.dur || local) - 0.001));
  pv.idx = loc.i;
  applyPreviewColor(loc.clip.color);
  const it = itemById(loc.clip.assetId);
  $('clipBadge').hidden = false; $('clipBadge').textContent = `${loc.i + 1}/${project.clips.length} · ${it ? baseName(it.name) : ''}`;
  if (!pv.playing) preloadNext(loc.i);
}
function preloadNext(i) {
  const nx = project.clips[i + 1]; if (!nx) return;
  const cur = project.clips[i]; if (cur && cur.assetId === nx.assetId) return;
  const a = assetOf(nx.assetId); if (!a) return;
  const o = other(pv.act); setSrc(o, a); seekVideo(o, nx.in);
}
function setT(t, opts = {}) {
  pv.T = clamp(t, 0, totalDur());
  placeWorld();
  updateTime();
  if (!pv.playing || opts.force) showFrame(pv.T, opts);
  syncMusic(false);
}
function updateTime() { $('tNow').textContent = fmtT(pv.T); $('tDur').textContent = fmtT(totalDur()); }

function play() {
  if (!project.clips.length) return;
  if (pv.T >= totalDur() - 0.05) setT(0);
  pv.playing = true; $('playBtn').innerHTML = icon('pause');
  showFrame(pv.T);
  const v = pv.act; v.muted = false; v.play().catch(() => {});
  cancelAnimationFrame(pv.raf); pv.raf = requestAnimationFrame(tick);
}
function pause() {
  pv.playing = false; $('playBtn').innerHTML = icon('play');
  vA.pause(); vB.pause(); audio.pause(); cancelAnimationFrame(pv.raf);
}
function tick() {
  if (!pv.playing) return;
  const i = pv.idx, c = project.clips[i], v = pv.act;
  if (!c) { pause(); return; }
  const start = startOf(i);
  if (!v.seeking && v.readyState >= 2) {
    pv.T = start + clamp(v.currentTime - c.in, 0, clipLen(c));
    if (v.currentTime >= c.out - 0.03 || v.ended) {
      const nx = project.clips[i + 1];
      if (!nx) { pv.T = totalDur(); placeWorld(); updateTime(); pause(); return; }
      pv.idx = i + 1;
      applyPreviewColor(nx.color);
      if (nx.assetId === c.assetId && Math.abs(nx.in - c.out) < 0.06) { /* corte en el mismo archivo: sigue sin saltar */ }
      else {
        const a = assetOf(nx.assetId);
        let nv = v;
        if (a.id !== v._aid) { nv = other(v); setSrc(nv, a); }
        seekVideo(nv, nx.in);
        showVideo(nv); nv.muted = false; nv.play().catch(() => {});
      }
      preloadNext(pv.idx);
      $('clipBadge').textContent = `${pv.idx + 1}/${project.clips.length} · ${baseName((itemById(nx.assetId) || {}).name || '')}`;
    }
    placeWorld(); updateTime(); syncMusic(true);
  }
  pv.raf = requestAnimationFrame(tick);
}
$('playBtn').onclick = () => pv.playing ? pause() : play();
$('toStartBtn').onclick = () => { pause(); setT(0); };
$('toEndBtn').onclick = () => { pause(); setT(totalDur()); };

/* música: un <audio> sincronizado al cabezal */
const musicSrc = { id: null, url: '', peaks: null, blob: null };
async function ensureMusic() {
  const m = project.music; if (!m) return;
  if (musicSrc.id === m.id && musicSrc.url) return;
  const blob = await DB.getBlob('music:' + m.id);
  if (!blob) { toast('No se encontró el archivo de música; volvé a elegirlo.'); return; }
  musicSrc.id = m.id; musicSrc.blob = blob; musicSrc.url = urlFor(blob); audio.src = musicSrc.url; musicSrc.peaks = null;
  computePeaks(blob, m.id);
}
async function computePeaks(blob, id) {
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext; const ctx = new Ctx();
    const buf = await ctx.decodeAudioData(await blob.arrayBuffer()); ctx.close && ctx.close();
    const ch = buf.getChannelData(0), N = 3000, step = Math.max(1, Math.floor(ch.length / N)), peaks = new Float32Array(N);
    for (let k = 0; k < N; k++) { let m = 0; const o = k * step; for (let j = 0; j < step; j += 4) { const s = Math.abs(ch[o + j] || 0); if (s > m) m = s; } peaks[k] = m; }
    let mx = 0; peaks.forEach(p => { if (p > mx) mx = p; }); if (mx) for (let k = 0; k < N; k++) peaks[k] /= mx;
    if (musicSrc.id === id) { musicSrc.peaks = peaks; musicSrc.dur = buf.duration; scheduleRender(); }
  } catch (e) { console.warn('onda', e); }
}
const musicLen = m => Math.max(0, m.out - m.in);
function musicGain(m, t) {
  const L = musicLen(m); let g = m.vol;
  if (m.fin > 0 && t < m.fin) g *= t / m.fin;
  if (m.fout > 0 && t > L - m.fout) g *= Math.max(0, (L - t) / m.fout);
  return clamp(g, 0, 1);
}
function syncMusic(playing) {
  const m = project.music;
  if (!m || !musicSrc.url) { audio.pause(); return; }
  const lt = pv.T - m.start;
  if (!playing || lt < 0 || lt >= musicLen(m)) { audio.pause(); if (!playing && lt >= 0 && lt < musicLen(m) && audio.readyState >= 1) audio.currentTime = m.in + lt; return; }
  const target = m.in + lt;
  audio.volume = musicGain(m, lt);
  if (audio.paused) { audio.currentTime = target; audio.play().catch(() => {}); }
  else if (Math.abs(audio.currentTime - target) > 0.3) audio.currentTime = target;
}
$('musicIn').onchange = async e => {
  const f = e.target.files[0]; e.target.value = ''; if (!f) return;
  const dur = await new Promise(res => { const a = new Audio(); a.preload = 'metadata'; a.onloadedmetadata = () => res(a.duration); a.onerror = () => res(0); a.src = URL.createObjectURL(f); });
  if (!dur || !isFinite(dur)) { toast('No se pudo leer ese audio.'); return; }
  const id = uid();
  await DB.putBlob('music:' + id, f);
  const old = project.music;
  commit(() => { project.music = { id, name: f.name, dur, in: 0, out: Math.min(dur, Math.max(totalDur(), 1) || dur), start: 0, vol: 0.35, fin: 0, fout: 0 }; });
  await ensureMusic(); scheduleRender();
  selectMusic(); toast('Música agregada: arrastrá sus bordes para recortarla');
};

/* ================= editor: línea de tiempo ================= */
const tl = $('tl'), world = $('world');
const view = { pps: 60, fitDone: false, sel: null, selMusic: false, drag: null };
const center = () => tl.clientWidth / 2;
function placeWorld() { world.style.transform = `translate3d(${center() - pv.T * view.pps}px,0,0)`; }
function fitZoom() { const d = totalDur(); if (!d || !tl.clientWidth) return false; view.pps = clamp((tl.clientWidth * 0.7) / d, 6, 400); return true; }
let rq = 0; function scheduleRender() { if (rq) return; rq = requestAnimationFrame(() => { rq = 0; renderTimeline(); }); }

function rulerStep() { const steps = [0.1, 0.25, 0.5, 1, 2, 5, 10, 15, 30, 60, 120, 300, 600]; for (const s of steps) if (s * view.pps >= 70) return s; return 600; }
function renderRuler(len) {
  const step = rulerStep(), minor = step / 5, end = len + 4 * step; let h = '';
  const count = Math.ceil(end / minor);
  for (let k = 0; k <= count && k < 4000; k++) {
    const t = k * minor, x = t * view.pps, big = k % 5 === 0;
    h += `<i class="${big ? 'big' : ''}" style="left:${x}px"></i>`;
    if (big) h += `<span style="left:${x}px">${t < 60 ? +t.toFixed(2) + 's' : Math.floor(t / 60) + ':' + String(Math.round(t % 60)).padStart(2, '0')}</span>`;
  }
  $('ruler').innerHTML = h; $('ruler').style.width = end * view.pps + 'px';
}
/* posición de cada bloque; durante un arrastre se usa la disposición temporal */
function layout() {
  const d = view.drag; const cs = project.clips; const pos = new Map();
  if (d && d.kind === 'reorder') {
    const rest = cs.filter(c => c.id !== d.id); let s = 0;
    rest.forEach((c, k) => { if (k === d.slot) s += clipLen(d.clip); pos.set(c.id, { x: s, w: clipLen(c) }); s += clipLen(c); });
    pos.set(d.id, { x: d.ghostT, w: clipLen(d.clip) });
    return pos;
  }
  let s = 0;
  cs.forEach(c => {
    if (d && d.kind === 'trim' && d.id === c.id && d.side === 'l') {
      const baseLen = d.base.out - d.base.in, shift = c.in - d.base.in;
      pos.set(c.id, { x: s + shift, w: clipLen(c) }); s += baseLen; return;
    }
    pos.set(c.id, { x: s, w: clipLen(c) }); s += clipLen(c);
  });
  return pos;
}
function renderTimeline() {
  const len = totalDur();
  if (!view.fitDone && len) view.fitDone = fitZoom();
  renderRuler(Math.max(len, project.music ? project.music.start + musicLen(project.music) : 0));
  const pos = layout();
  const tw = 54;
  let h = '';
  project.clips.forEach((c, k) => {
    const p = pos.get(c.id), w = Math.max(4, p.w * view.pps), a = assetOf(c.assetId);
    const n = Math.min(200, Math.max(1, Math.ceil(w / tw))), tileW = w / n;
    let strip = '';
    for (let j = 0; j < n; j++) { const t = c.in + ((j + 0.5) * tileW) / view.pps; const u = frameAt(a, t); strip += `<i style="width:${tileW}px;${u ? `background-image:url(${u})` : ''}"></i>`; }
    const d = view.drag;
    const cls = ['clip'];
    if (view.sel === c.id) cls.push('sel');
    if (d && d.kind === 'reorder' && d.id === c.id) cls.push('lift');
    if (d && d.kind === 'trim' && d.id === c.id) cls.push('trimming');
    h += `<div class="${cls.join(' ')}" data-clip="${c.id}" style="left:${p.x * view.pps}px;width:${w}px">
      <div class="strip">${strip}</div>
      <div class="cap"><b>${k + 1}</b><b>${fmtShort(clipLen(c))}</b>${isNeutral(c.color) ? '' : '<b class="fx">color</b>'}</div>
      <span class="trim-tip">${fmtT(clipLen(c))}</span>
      <span class="hdl l" data-h="l"></span><span class="hdl r" data-h="r"></span></div>`;
  });
  h += `<button class="add-slot" data-add="clip" title="Agregar clip" style="left:${len * view.pps + 12}px">${icon('plus')}</button>`;
  $('trackV').innerHTML = h;

  const m = project.music; let ha = '';
  if (m) {
    const x = m.start * view.pps, w = Math.max(6, musicLen(m) * view.pps);
    const d = view.drag;
    const cls = ['clip', 'music'];
    if (view.selMusic) cls.push('sel');
    if (d && d.kind && d.id === 'music' && d.kind === 'trim') cls.push('trimming');
    ha = `<div class="${cls.join(' ')}" data-music="1" style="left:${x}px;width:${w}px"><div class="wave"><canvas></canvas></div>
      <div class="cap"><b>${esc(baseName(m.name))}</b><b>${Math.round(m.vol * 100)} %</b></div>
      <span class="trim-tip">${fmtT(musicLen(m))}</span>
      <span class="hdl l" data-h="l"></span><span class="hdl r" data-h="r"></span></div>`;
  } else {
    ha = `<button class="add-music" data-add="music" style="left:0">${icon('music')} Agregar música</button>`;
  }
  $('trackA').innerHTML = ha;
  if (m) drawWave();
  placeWorld(); updateTime(); renderDock();
}
function drawWave() {
  const el = $('trackA').querySelector('canvas'); if (!el) return;
  const m = project.music, p = musicSrc.peaks;
  const r = el.getBoundingClientRect(); const dpr = Math.min(2, devicePixelRatio || 1);
  const W = Math.min(8000, Math.max(2, Math.round(r.width * dpr))), H = Math.max(2, Math.round(r.height * dpr));
  el.width = W; el.height = H;
  const g = el.getContext('2d'); g.fillStyle = 'rgba(255,250,240,.85)';
  if (!p) { g.fillRect(0, H / 2 - 1, W, 2); return; }
  const dur = musicSrc.dur || m.dur;
  for (let x = 0; x < W; x += 2 * dpr) {
    const t = m.in + (x / W) * musicLen(m); const k = Math.floor((t / dur) * p.length);
    const a = (p[clamp(k, 0, p.length - 1)] || 0) * (H * 0.42) + 1;
    g.fillRect(x, H / 2 - a, Math.max(1, dpr * 1.2), a * 2);
  }
}

/* ---------- selección y herramientas ---------- */
function select(id) { view.sel = id; view.selMusic = false; renderTimeline(); if (sheetKind === 'color') openColor(); }
function selectMusic() { view.sel = null; view.selMusic = true; renderTimeline(); }
function deselect() { view.sel = null; view.selMusic = false; renderTimeline(); closeSheet(); }
const selClip = () => project.clips.find(c => c.id === view.sel);
function renderDock() {
  const T = (ic, label, act, extra = '') => `<button class="tool ${extra}" data-act="${act}"><span class="ic">${icon(ic)}</span>${label}</button>`;
  let h;
  if (selClip()) {
    h = T('cut', 'Dividir', 'split') + T('dup', 'Duplicar', 'dup') + T('sliders', 'Color', 'color') + T('trash', 'Borrar', 'del', 'danger') + '<span class="sep"></span>' + T('check', 'Listo', 'done');
  } else if (view.selMusic && project.music) {
    h = T('volume', 'Volumen', 'mvol') + T('cut', 'Dividir', 'msplit') + T('music', 'Cambiar', 'mchange') + T('trash', 'Borrar', 'mdel', 'danger') + '<span class="sep"></span>' + T('check', 'Listo', 'done');
  } else {
    h = T('plus', 'Clip', 'addclip') + T('music', 'Música', project.music ? 'msel' : 'addmusic') + T('crop', 'Formato', 'format') + T('layers', 'Videos', 'library') + T('download', 'Exportar', 'export', 'hot');
  }
  $('dock').innerHTML = h;
  $('undoBtn').disabled = !hist.undo.length; $('redoBtn').disabled = !hist.redo.length;
}
$('dock').addEventListener('click', e => {
  const b = e.target.closest('[data-act]'); if (!b) return;
  const a = b.dataset.act, c = selClip();
  if (a === 'split') splitAtHead();
  if (a === 'dup' && c) commit(() => { const i = project.clips.indexOf(c); project.clips.splice(i + 1, 0, { ...c, id: uid(), color: { ...c.color } }); });
  if (a === 'del' && c) { commit(() => { project.clips = project.clips.filter(x => x !== c); }); view.sel = null; closeSheet(); }
  if (a === 'color') openColor();
  if (a === 'done') deselect();
  if (a === 'addclip') $('clipIn').click();
  if (a === 'addmusic' || a === 'mchange') $('musicIn').click();
  if (a === 'msel') selectMusic();
  if (a === 'mvol') openMusicSheet();
  if (a === 'msplit') splitMusic();
  if (a === 'mdel') { commit(() => { project.music = null; }); view.selMusic = false; audio.pause(); closeSheet(); }
  if (a === 'format') openFormat();
  if (a === 'library') go('videos');
  if (a === 'export') openExport();
  renderTimeline();
});
$('fmtChip').onclick = openFormat;
$('clipIn').onchange = e => { addFiles(e.target.files, { toEditor: true }); e.target.value = ''; };
function splitAtHead() {
  const loc = locate(pv.T); if (!loc) return;
  const c = loc.clip, t = loc.local;
  if (t - c.in < MIN_LEN || c.out - t < MIN_LEN) { toast('Mové el cabezal adentro del clip para dividirlo.'); return; }
  const b = { ...c, id: uid(), in: t, color: { ...c.color } };
  commit(() => { c.out = t; project.clips.splice(loc.i + 1, 0, b); });
  view.sel = b.id; buzz(12); renderTimeline();
}
function splitMusic() {
  const m = project.music; if (!m) return;
  const lt = pv.T - m.start;
  if (lt < MIN_LEN || musicLen(m) - lt < MIN_LEN) { toast('Poné el cabezal sobre la música para cortarla.'); return; }
  commit(() => { m.out = m.in + lt; }); toast('Música cortada en el cabezal'); buzz(12);
}

/* ---------- gestos: recorrer, pellizcar, seleccionar, recortar, reordenar, mover música ---------- */
const ptrs = new Map();
let gest = null, inertia = 0, longT = 0;
const tAtX = clientX => pv.T + (clientX - tl.getBoundingClientRect().left - center()) / view.pps;
function snapT(t, exclude) {
  if (!prefs.snap) return { t, hit: false };
  const cands = [0, pv.T]; let s = 0;
  project.clips.forEach(c => { cands.push(s); s += clipLen(c); }); cands.push(s);
  if (project.music && exclude !== 'music') { cands.push(project.music.start, project.music.start + musicLen(project.music)); }
  const th = 10 / view.pps; let best = null;
  for (const c of cands) if (Math.abs(c - t) < th && (best == null || Math.abs(c - t) < Math.abs(best - t))) best = c;
  return best == null ? { t, hit: false } : { t: best, hit: true };
}
function showSnap(t) {
  const sl = $('snapline');
  if (t == null) { sl.style.display = 'none'; return; }
  sl.style.display = 'block'; sl.style.left = (center() + (t - pv.T) * view.pps - 1) + 'px';
}
let lastSnap = false;
const snapFeedback = hit => { if (hit && !lastSnap) buzz(8); lastSnap = hit; };

tl.addEventListener('pointerdown', e => {
  if (e.target.closest('.zoom')) return;
  tl.setPointerCapture(e.pointerId);
  ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY });
  cancelAnimationFrame(inertia);
  if (ptrs.size === 2) {
    clearTimeout(longT);
    const [p, q] = [...ptrs.values()];
    gest = { kind: 'pinch', d0: Math.hypot(p.x - q.x, p.y - q.y) || 1, pps0: view.pps };
    return;
  }
  const hdl = e.target.closest('.hdl');
  const clipEl = e.target.closest('[data-clip]');
  const musicEl = e.target.closest('[data-music]');
  const add = e.target.closest('[data-add]');
  if (pv.playing) pause();
  if (hdl && (clipEl || musicEl)) {
    const side = hdl.dataset.h;
    if (clipEl) { const c = project.clips.find(x => x.id === clipEl.dataset.clip); gest = { kind: 'trim', id: c.id, clip: c, side, x0: e.clientX, before: snap(), base: { in: c.in, out: c.out }, start: startOf(project.clips.indexOf(c)) }; }
    else { const m = project.music; gest = { kind: 'trim', id: 'music', side, x0: e.clientX, before: snap(), base: { in: m.in, out: m.out, start: m.start } }; }
    view.drag = gest; tl.classList.add('grab'); scheduleRender(); return;
  }
  gest = { kind: 'press', x0: e.clientX, y0: e.clientY, T0: pv.T, t0: performance.now(), clipId: clipEl && clipEl.dataset.clip, music: !!musicEl, add: add && add.dataset.add, vx: 0, lx: e.clientX, lt: performance.now() };
  if (musicEl && view.selMusic) { const m = project.music; gest = { kind: 'mmove', x0: e.clientX, before: snap(), base: { start: m.start }, moved: false }; view.drag = { kind: 'mmove', id: 'music' }; return; }
  if (clipEl) {
    longT = setTimeout(() => {
      if (!gest || gest.kind !== 'press') return;
      const c = project.clips.find(x => x.id === gest.clipId); if (!c) return;
      buzz(20);
      const i = project.clips.indexOf(c);
      gest = { kind: 'reorder', id: c.id, clip: c, before: snap(), grabOff: tAtX(gest.lx) - startOf(i), lx: gest.lx };
      view.drag = { kind: 'reorder', id: c.id, clip: c, slot: i, ghostT: startOf(i) };
      view.sel = c.id; tl.classList.add('grab'); scheduleRender(); autoPan();
    }, 380);
  }
});
tl.addEventListener('pointermove', e => {
  if (!ptrs.has(e.pointerId)) return;
  ptrs.set(e.pointerId, { x: e.clientX, y: e.clientY });
  if (!gest) return;
  if (gest.kind === 'pinch' && ptrs.size >= 2) {
    const [p, q] = [...ptrs.values()];
    view.pps = clamp(gest.pps0 * (Math.hypot(p.x - q.x, p.y - q.y) / gest.d0), 4, 600); scheduleRender(); return;
  }
  if (gest.kind === 'press') {
    const dx = e.clientX - gest.x0;
    if (Math.abs(dx) > 6 || Math.abs(e.clientY - gest.y0) > 10) { clearTimeout(longT); gest.kind = 'pan'; tl.classList.add('grab'); }
  }
  if (gest.kind === 'pan') {
    const now = performance.now();
    gest.vx = (e.clientX - gest.lx) / Math.max(1, now - gest.lt); gest.lx = e.clientX; gest.lt = now;
    setT(gest.T0 - (e.clientX - gest.x0) / view.pps);
    return;
  }
  if (gest.kind === 'trim') { doTrim(e.clientX); return; }
  if (gest.kind === 'mmove') {
    const m = project.music; gest.moved = true;
    let st = Math.max(0, gest.base.start + (e.clientX - gest.x0) / view.pps);
    const s1 = snapT(st, 'music'), s2 = snapT(st + musicLen(m), 'music');
    if (s1.hit) st = s1.t; else if (s2.hit) st = s2.t - musicLen(m);
    snapFeedback(s1.hit || s2.hit); showSnap(s1.hit ? s1.t : s2.hit ? s2.t : null);
    m.start = Math.max(0, st); scheduleRender(); return;
  }
  if (gest.kind === 'reorder') { gest.lx = e.clientX; reorderMove(); }
});
function doTrim(x) {
  const g = gest, d = (x - g.x0) / view.pps;
  if (g.id === 'music') {
    const m = project.music, md = musicSrc.dur || m.dur;
    if (g.side === 'r') {
      let out = clamp(g.base.out + d, m.in + MIN_LEN, md);
      const s = snapT(m.start + out - m.in, 'music'); if (s.hit) out = clamp(m.in + s.t - m.start, m.in + MIN_LEN, md);
      snapFeedback(s.hit); showSnap(s.hit ? s.t : null); m.out = out;
    } else {
      let nin = clamp(g.base.in + d, 0, m.out - MIN_LEN);
      if (g.base.start + (nin - g.base.in) < 0) nin = g.base.in - g.base.start;
      const s = snapT(g.base.start + (nin - g.base.in), 'music');
      if (s.hit) nin = clamp(g.base.in + (s.t - g.base.start), 0, m.out - MIN_LEN);
      snapFeedback(s.hit); showSnap(s.hit ? s.t : null);
      m.in = nin; m.start = Math.max(0, g.base.start + (nin - g.base.in));
    }
    scheduleRender(); return;
  }
  const c = g.clip, a = assetOf(c.assetId), maxD = (a && a.dur) || g.base.out;
  if (g.side === 'r') {
    let out = clamp(g.base.out + d, c.in + MIN_LEN, maxD);
    const s = snapT(g.start + out - c.in); if (s.hit && Math.abs(s.t - g.start - (out - c.in)) > 1e-4) out = clamp(c.in + s.t - g.start, c.in + MIN_LEN, maxD);
    snapFeedback(s.hit); showSnap(s.hit ? s.t : null);
    c.out = out;
    previewClip(c, Math.max(c.in, out - 0.04));
  } else {
    let nin = clamp(g.base.in + d, 0, c.out - MIN_LEN);
    const s = snapT(g.start + (nin - g.base.in)); if (s.hit) nin = clamp(g.base.in + s.t - g.start, 0, c.out - MIN_LEN);
    snapFeedback(s.hit); showSnap(s.hit ? s.t : null);
    c.in = nin; previewClip(c, nin);
  }
  scheduleRender();
}
/* muestra el cuadro del borde que se está recortando, al instante */
function previewClip(c, t) {
  const a = assetOf(c.assetId); if (!a) return;
  let v = pv.act; if (v._aid !== a.id && other(v)._aid === a.id) v = other(v);
  setSrc(v, a); showVideo(v); seekVideo(v, t); applyPreviewColor(c.color); $('framePh').hidden = true;
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
  const r = tl.getBoundingClientRect(), x = gest.lx - r.left, edge = 56;
  if (x < edge) setT(pv.T - (edge - x) / view.pps * 0.25);
  else if (x > r.width - edge) setT(pv.T + (x - r.width + edge) / view.pps * 0.25);
  reorderMove();
  requestAnimationFrame(autoPan);
}
function endGesture(e) {
  ptrs.delete(e.pointerId);
  clearTimeout(longT);
  tl.classList.remove('grab');
  showSnap(null); lastSnap = false;
  const g = gest; if (!g) return;
  if (g.kind === 'pinch') { if (ptrs.size === 0) gest = null; return; }
  gest = null;
  if (g.kind === 'press') {
    if (g.add === 'clip') $('clipIn').click();
    else if (g.add === 'music') $('musicIn').click();
    else if (g.clipId) { select(view.sel === g.clipId ? null : g.clipId); }
    else if (g.music) selectMusic();
    else { if (view.sel || view.selMusic) deselect(); else setT(tAtX(e.clientX)); }
    return;
  }
  if (g.kind === 'pan') {
    let v = g.vx * 16; const step = () => { v *= 0.93; if (Math.abs(v) < 0.4) return; setT(pv.T - v / view.pps); inertia = requestAnimationFrame(step); };
    if (Math.abs(v) > 2) inertia = requestAnimationFrame(step);
    return;
  }
  if (g.kind === 'trim') {
    view.drag = null;
    if (g.id !== 'music' && g.side === 'l') { const shift = g.clip.in - g.base.in; pv.T = Math.max(0, pv.T - shift); }
    finishCommit(g.before);
    setT(pv.T, { force: true }); return;
  }
  if (g.kind === 'mmove') { view.drag = null; if (g.moved) finishCommit(g.before); else deselect(); return; }
  if (g.kind === 'reorder') {
    const d = view.drag; view.drag = null;
    const arr = project.clips.filter(c => c.id !== g.id); arr.splice(d.slot, 0, g.clip);
    if (arr.some((c, k) => c !== project.clips[k])) { project.clips = arr; finishCommit(g.before); toast(`Clip movido a la posición ${d.slot + 1}`); }
    else renderTimeline();
    pv.T = startOf(project.clips.indexOf(g.clip)) + 0.001; setT(pv.T, { force: true });
  }
}
/* el modelo ya cambió durante el gesto: se registra una sola entrada de historial */
function finishCommit(before) {
  if (snap() !== before) { hist.undo.push(before); hist.redo.length = 0; saveProject(); }
  onProjectChange();
}
tl.addEventListener('pointerup', endGesture);
tl.addEventListener('pointercancel', endGesture);
tl.addEventListener('wheel', e => {
  e.preventDefault();
  if (e.ctrlKey || e.metaKey) { view.pps = clamp(view.pps * Math.exp(-e.deltaY * 0.004), 4, 600); scheduleRender(); }
  else { if (pv.playing) pause(); setT(pv.T + (Math.abs(e.deltaX) > Math.abs(e.deltaY) ? e.deltaX : e.deltaY) / view.pps); }
}, { passive: false });
$('zoomIn').onclick = () => { view.pps = clamp(view.pps * 1.5, 4, 600); renderTimeline(); };
$('zoomOut').onclick = () => { view.pps = clamp(view.pps / 1.5, 4, 600); renderTimeline(); };
const syncSnapBtn = () => $('snapBtn').classList.toggle('on', !!prefs.snap);
$('snapBtn').onclick = () => { prefs.snap = !prefs.snap; savePrefs(); syncSnapBtn(); toast(prefs.snap ? 'Imán activado' : 'Imán desactivado'); };

function onProjectChange() {
  if (view.sel && !selClip()) view.sel = null;
  if (!project.music) view.selMusic = false;
  pv.T = clamp(pv.T, 0, totalDur());
  renderTimeline(); layoutFrame();
  if (!pv.playing) showFrame(pv.T);
  if (sheetKind === 'color' && !selClip()) closeSheet();
}

/* tamaño del cuadro de preview según el formato de salida */
function layoutFrame() {
  const st = $('stage'), fr = $('frame');
  const first = project.clips[0] && assetOf(project.clips[0].assetId);
  const a = aspectOf(prefs.aspect);
  const r = a.r || (first && first.w && first.h ? first.w / first.h : 16 / 9);
  const W = st.clientWidth - 24, H = st.clientHeight - 24;
  let w = W, h = w / r; if (h > H) { h = H; w = h * r; }
  fr.style.width = Math.max(40, w) + 'px'; fr.style.height = Math.max(40, h) + 'px';
}
new ResizeObserver(() => { layoutFrame(); placeWorld(); }).observe($('stage'));
new ResizeObserver(() => placeWorld()).observe(tl);

/* ================= hojas inferiores ================= */
let sheetKind = null;
function openSheet(kind, html) {
  sheetKind = kind; $('sheetBody').innerHTML = html; paintIcons($('sheetBody'));
  $('sheet').classList.add('show'); $('sheetOv').classList.add('show');
}
function closeSheet() { sheetKind = null; $('sheet').classList.remove('show'); $('sheetOv').classList.remove('show'); }
$('sheetOv').onclick = closeSheet;
const head = (t, extra = '') => `<div class="sh-head"><b>${t}</b>${extra}<button class="round" data-close data-ico="x" title="Cerrar"></button></div>`;
$('sheet').addEventListener('click', e => { if (e.target.closest('[data-close]')) closeSheet(); });

/* color: acá sí hay sliders porque son ajustes de imagen, no recortes */
const COLOR_CTL = [
  ['sat', 'Saturación', 0, 2, 0.01, v => Math.round(v * 100) + ' %'],
  ['exp', 'Exposición', -1, 1, 0.01, v => (v > 0 ? '+' : '') + v.toFixed(2)],
  ['con', 'Contraste', -0.8, 0.8, 0.01, v => (v > 0 ? '+' : '') + Math.round(v * 100)],
  ['hi', 'Luces', -1, 1, 0.01, v => (v > 0 ? '+' : '') + Math.round(v * 100)],
  ['sh', 'Sombras', -1, 1, 0.01, v => (v > 0 ? '+' : '') + Math.round(v * 100)],
];
function openColor() {
  const c = selClip(); if (!c) { toast('Tocá un clip para ajustar su color.'); return; }
  const col = c.color;
  const here = locate(pv.T), ci = project.clips.indexOf(c);
  if (!here || here.clip !== c) { pause(); setT(startOf(ci) + Math.min(0.05, clipLen(c) / 2), { force: true }); }
  const rng = ([k, label, mn, mx, st, f]) => `<label class="ctl"><span>${label}</span><input type="range" data-k="${k}" min="${mn}" max="${mx}" step="${st}" value="${col[k]}"><output>${f(col[k])}</output></label>`;
  const hues = k => `<div class="hues">${HUES.map(h => `<button style="background:hsl(${h},80%,55%)" data-hue="${k}" data-v="${h}" class="${col[k] === h ? 'on' : ''}"></button>`).join('')}</div>`;
  openSheet('color', head('Color del clip ' + (project.clips.indexOf(c) + 1), `<button class="pill ${col.bw ? 'on' : ''}" data-bw>B y N</button>`) +
    COLOR_CTL.map(rng).join('') +
    `<div class="label sh-sub">Tinte de sombras</div>${hues('shHue')}<label class="ctl"><span>Intensidad</span><input type="range" data-k="shAmt" min="0" max="1" step="0.01" value="${col.shAmt}"><output>${Math.round(col.shAmt * 100)} %</output></label>
     <div class="label sh-sub">Tinte de luces</div>${hues('hiHue')}<label class="ctl"><span>Intensidad</span><input type="range" data-k="hiAmt" min="0" max="1" step="0.01" value="${col.hiAmt}"><output>${Math.round(col.hiAmt * 100)} %</output></label>
     <div class="sh-row"><button class="pill" data-reset><span data-ico="reset"></span>Restablecer</button><button class="pill" data-all><span data-ico="layers"></span>Aplicar a todos</button></div>
     <p class="sh-note">La preview cambia en el momento, sin volver a generar nada. La exportación usa exactamente las mismas curvas.</p>`);
}
let colorBefore = null;
$('sheetBody').addEventListener('input', e => {
  const k = e.target.dataset.k; if (!k) return;
  if (sheetKind === 'color') {
    const c = selClip(); if (!c) return;
    if (colorBefore == null) colorBefore = snap();
    c.color[k] = +e.target.value;
    const ctl = COLOR_CTL.find(x => x[0] === k); const o = e.target.parentElement.querySelector('output');
    o.textContent = ctl ? ctl[5](c.color[k]) : Math.round(c.color[k] * 100) + ' %';
    const here = locate(pv.T);
    if (here && here.clip === c) applyPreviewColor(c.color); else previewClip(c, c.in);
  }
  if (sheetKind === 'music') {
    const m = project.music; if (!m) return;
    if (colorBefore == null) colorBefore = snap();
    m[k] = +e.target.value;
    const o = e.target.parentElement.querySelector('output');
    o.textContent = k === 'vol' ? Math.round(m.vol * 100) + ' %' : m[k].toFixed(1) + ' s';
    if (!pv.playing) syncMusic(false); else audio.volume = musicGain(m, pv.T - m.start);
  }
});
$('sheetBody').addEventListener('change', e => {
  if (!e.target.dataset.k || colorBefore == null) return;
  finishCommit(colorBefore); colorBefore = null;
});
$('sheetBody').addEventListener('click', e => {
  const c = selClip();
  if (sheetKind === 'color' && c) {
    if (e.target.closest('[data-bw]')) { commit(() => { c.color.bw = !c.color.bw; }); openColor(); }
    const hb = e.target.closest('[data-hue]');
    if (hb) { commit(() => { c.color[hb.dataset.hue] = +hb.dataset.v; const amt = hb.dataset.hue === 'shHue' ? 'shAmt' : 'hiAmt'; if (!c.color[amt]) c.color[amt] = 0.4; }); openColor(); }
    if (e.target.closest('[data-reset]')) { commit(() => { c.color = { ...NEUTRAL }; }); openColor(); }
    if (e.target.closest('[data-all]')) { commit(() => { project.clips.forEach(x => { x.color = { ...c.color }; }); }); toast('Color aplicado a todos los clips'); }
    showFrame(pv.T);
  }
  if (sheetKind === 'format') {
    const b = e.target.closest('[data-a]');
    if (b) { prefs.aspect = b.dataset.a; savePrefs(); renderOut(); layoutFrame(); openFormat(); }
  }
  if (sheetKind === 'export') {
    if (e.target.closest('[data-export]')) exportMontage();
    if (e.target.closest('[data-dl]') && lastExport) downloadBlob(lastExport, 'KyroAviMp4_montaje.mp4');
    if (e.target.closest('[data-cancel]')) { engine.cancel = true; killFF(); }
  }
});
function openMusicSheet() {
  const m = project.music; if (!m) return;
  openSheet('music', head('Música · ' + esc(baseName(m.name))) +
    `<label class="ctl"><span>Volumen</span><input type="range" data-k="vol" min="0" max="1" step="0.01" value="${m.vol}"><output>${Math.round(m.vol * 100)} %</output></label>
     <label class="ctl"><span>Fundido entrada</span><input type="range" data-k="fin" min="0" max="5" step="0.1" value="${m.fin}"><output>${m.fin.toFixed(1)} s</output></label>
     <label class="ctl"><span>Fundido salida</span><input type="range" data-k="fout" min="0" max="5" step="0.1" value="${m.fout}"><output>${m.fout.toFixed(1)} s</output></label>
     <p class="sh-note">Para recortarla tirá de las manijas amarillas del bloque. Con el bloque elegido, arrastralo para moverlo en el tiempo.</p>`);
}
function openFormat() {
  const first = project.clips[0] && assetOf(project.clips[0].assetId);
  const { W, H } = outSize(first && first.w, first && first.h);
  openSheet('format', head('Formato de salida') +
    `<div class="seg">${ASPECTS.map(a => `<button class="${prefs.aspect === a.id ? 'on' : ''}" data-a="${a.id}">${a.label}</button>`).join('')}</div>
     <div class="sh-row"><label class="out-size">Ancho <input class="num" id="fmtW" type="number" min="160" max="3840" step="2" value="${prefs.width}"></label><span class="out-size">× ${H || 'auto'} <small>px</small></span></div>
     <p class="sh-note">Es el mismo ajuste que en Videos. Sin recortar: si la proporción no coincide se agregan bandas negras. ${W}px de ancho.</p>`);
  $('fmtW').onchange = () => { prefs.width = clamp(even(+$('fmtW').value || 1010), 160, 3840); savePrefs(); renderOut(); openFormat(); };
}

/* ================= exportación del montaje ================= */
let lastExport = null;
function openExport(state = {}) {
  const d = totalDur();
  const first = project.clips[0] && assetOf(project.clips[0].assetId);
  const { W, H } = outSize(first && first.w, first && first.h);
  openSheet('export', head('Exportar montaje') +
    `<div class="m-info"><span>${project.clips.length} clips</span><span>${fmtT(d)}</span><span>${W} × ${H || 'auto'} px</span><span>${project.music ? 'con música' : 'sin música'}</span></div>
     ${state.busy ? `<div class="prog sh-row"><span id="exTxt">Preparando…</span><span class="track"><i id="exBar"></i></span><button class="round" data-cancel data-ico="x" title="Cancelar"></button></div>` : ''}
     <div class="sh-row">
       ${lastExport && !state.busy ? `<button class="pill" data-dl><span data-ico="download"></span>Descargar último (${fmtBytes(lastExport.size)})</button>` : ''}
       <button class="stamp" data-export ${!project.clips.length || engine.busy ? 'disabled' : ''}><span data-ico="download"></span>Exportar MP4</button>
     </div>
     <p class="sh-note">El motor de video solo trabaja acá, al final. Mientras editás, todo se ve en vivo sobre los archivos originales.</p>`);
}
/* ¿el archivo tiene sonido? lo decide el navegador, sin ocupar el motor */
const audioCache = new Map();
async function hasAudio(blob) {
  if (audioCache.has(blob)) return audioCache.get(blob);
  let ok = false;
  try {
    const Ctx = window.OfflineAudioContext || window.webkitOfflineAudioContext;
    const ctx = new Ctx(1, 44100, 44100);
    const buf = await ctx.decodeAudioData(await blob.arrayBuffer());
    ok = !!buf && buf.length > 0;
  } catch (_) { ok = false; }
  audioCache.set(blob, ok); return ok;
}
async function exportMontage() {
  if (engine.busy || !project.clips.length) return;
  engine.busy = true; engine.cancel = false; pause();
  openExport({ busy: true });
  const bar = r => { const b = $('exBar'); if (b) b.style.width = (r * 100).toFixed(1) + '%'; };
  const txt = t => { const x = $('exTxt'); if (x) x.textContent = t; };
  try {
    const ff = await getFF(); hideProg();
    const ids = [...new Set(project.clips.map(c => c.assetId))];
    const inputs = []; const audioOf = {};
    for (let k = 0; k < ids.length; k++) {
      txt(`Cargando video ${k + 1} de ${ids.length}…`); bar(k / ids.length * 0.1);
      const it = itemById(ids[k]); const name = `v${k}.mp4`;
      audioOf[ids[k]] = await hasAudio(editSource(it));
      ff.FS('writeFile', name, new Uint8Array(await editSource(it).arrayBuffer()));
      inputs.push(name);
    }
    const m = project.music;
    let withMusic = false;
    if (m) { const blob = musicSrc.blob || await DB.getBlob('music:' + m.id); if (blob) { ff.FS('writeFile', 'music.bin', new Uint8Array(await blob.arrayBuffer())); withMusic = true; } }
    const first = assetOf(project.clips[0].assetId);
    const { W } = outSize(first.w, first.h); const H = outSize(first.w, first.h).H || even(W * 9 / 16);
    const f = []; let cat = '';
    project.clips.forEach((c, k) => {
      const ix = ids.indexOf(c.assetId), L = clipLen(c);
      const col = colorFilterFF(c.color);
      f.push(`[${ix}:v]trim=start=${c.in.toFixed(3)}:end=${c.out.toFixed(3)},setpts=PTS-STARTPTS,scale=${W}:${H}:force_original_aspect_ratio=decrease,pad=${W}:${H}:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1,fps=30,format=yuv420p${col ? ',' + col : ''}[v${k}]`);
      if (audioOf[c.assetId]) f.push(`[${ix}:a]atrim=start=${c.in.toFixed(3)}:end=${c.out.toFixed(3)},asetpts=PTS-STARTPTS,aresample=44100,aformat=channel_layouts=stereo[a${k}]`);
      else f.push(`anullsrc=r=44100:cl=stereo,atrim=0:${L.toFixed(3)}[a${k}]`);
      cat += `[v${k}][a${k}]`;
    });
    f.push(`${cat}concat=n=${project.clips.length}:v=1:a=1[vout][a0]`);
    let amap = '[a0]';
    const args = ['-hide_banner'];
    inputs.forEach(n => args.push('-i', n));
    if (withMusic) {
      args.push('-i', 'music.bin');
      const L = musicLen(m), ms = Math.round(m.start * 1000);
      let mf = `[${inputs.length}:a]atrim=start=${m.in.toFixed(3)}:end=${m.out.toFixed(3)},asetpts=PTS-STARTPTS,aresample=44100,aformat=channel_layouts=stereo`;
      if (m.fin > 0) mf += `,afade=t=in:st=0:d=${m.fin.toFixed(2)}`;
      if (m.fout > 0) mf += `,afade=t=out:st=${Math.max(0, L - m.fout).toFixed(2)}:d=${m.fout.toFixed(2)}`;
      mf += `,volume=${m.vol.toFixed(3)},adelay=${ms}|${ms}[mu]`;
      f.push(mf);
      f.push(`[a0][mu]amix=inputs=2:duration=first:dropout_transition=0,volume=2[aout]`);
      amap = '[aout]';
    }
    const total = totalDur();
    engine.log = [];
    engine.onProgress = null;
    const timer = setInterval(() => {
      const l = engine.log.slice().reverse().find(x => /time=\d+:\d+:\d+/.test(x));
      if (l) { const [, hh, mm, ss] = l.match(/time=(\d+):(\d+):([\d.]+)/); const t = +hh * 3600 + +mm * 60 + +ss; bar(0.1 + 0.9 * clamp(t / total, 0, 1)); txt(`Exportando ${Math.round(clamp(t / total, 0, 1) * 100)} %`); }
    }, 400);
    txt('Exportando…');
    try {
      await run(ff, [...args, '-filter_complex', f.join(';'), '-map', '[vout]', '-map', amap, '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '22', '-pix_fmt', 'yuv420p',
        '-c:a', 'aac', '-b:a', '160k', '-movflags', '+faststart', '-max_muxing_queue_size', '4096', '-y', 'montaje.mp4']);
    } finally { clearInterval(timer); }
    lastExport = new Blob([readOut(ff, 'montaje.mp4')], { type: 'video/mp4' });
    inputs.forEach(n => rm(ff, n)); rm(ff, 'music.bin'); rm(ff, 'montaje.mp4');
    killFF(); // libera la memoria del motor
    toast('Montaje exportado');
    downloadBlob(lastExport, 'KyroAviMp4_montaje.mp4');
  } catch (e) {
    console.warn(e, engine.log.slice(-20));
    if (!engine.cancel) toast('No se pudo exportar: ' + (e.message || 'error del motor'));
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
  if (name === 'editor') { requestAnimationFrame(() => { layoutFrame(); renderTimeline(); showFrame(pv.T); }); }
  else { pause(); closeSheet(); }
}
document.querySelectorAll('[data-go]').forEach(b => b.onclick = () => go(b.dataset.go));
function syncNight() { document.documentElement.classList.toggle('night', !!prefs.night); $('nightBtn').innerHTML = icon(prefs.night ? 'sun' : 'moon'); }
$('nightBtn').onclick = () => { prefs.night = !prefs.night; savePrefs(); syncNight(); };
document.addEventListener('keydown', e => {
  if (e.target.matches('input')) return;
  const inEditor = $('editor').classList.contains('show');
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') { e.preventDefault(); e.shiftKey ? redo() : undo(); return; }
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') { e.preventDefault(); redo(); return; }
  if (!inEditor) return;
  if (e.code === 'Space') { e.preventDefault(); pv.playing ? pause() : play(); }
  if (e.key.toLowerCase() === 's') splitAtHead();
  if ((e.key === 'Delete' || e.key === 'Backspace') && selClip()) { const c = selClip(); commit(() => { project.clips = project.clips.filter(x => x !== c); }); }
  if (e.key === 'ArrowLeft') { pause(); setT(pv.T - (e.shiftKey ? 1 : 1 / 30)); }
  if (e.key === 'ArrowRight') { pause(); setT(pv.T + (e.shiftKey ? 1 : 1 / 30)); }
});

/* ================= arranque ================= */
async function init() {
  paintIcons(); syncNight(); syncSnapBtn(); renderOut();
  $('playBtn').innerHTML = icon('play');
  const saved = await DB.allItems();
  if (saved && saved.length) {
    lib.items = saved.map(s => ({ ...s, progress: 0 }));
    // un archivo guardado sin MP4 ni reproducción directa sigue disponible para convertir
  }
  project.clips = project.clips.filter(c => itemById(c.assetId) && editSource(itemById(c.assetId)));
  renderGrid();
  if (project.music) ensureMusic();
  renderTimeline();
}
window.__kyro = { project: () => project, view, pv, assets, hist };
init();
if ('serviceWorker' in navigator && location.protocol !== 'file:') navigator.serviceWorker.register('sw.js').catch(e => console.warn('sw', e));
})();
