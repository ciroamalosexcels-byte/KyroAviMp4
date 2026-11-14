# Rediseño KyroAviMp4 — plan

Pedido del usuario (13/11/2026). Todo esto tiene que estar sí o sí:

1. **Estética Kyro / neobrutalismo** (manual de marca de KyroNeoBoard y KyroZineGenerator): papel crema, tinta negra, rojo sello,
   amarillo resaltador, verde de mesa de corte, Archivo como única tipografía, píldoras y círculos sin borde, sombra dura de tinta,
   botones que se levantan al pasar y se aplastan al apretar, sin degradés, modo noche.
2. **Preview instantánea**: adelantar, mover o cortar un clip **no** vuelve a generar la preview. Se reproducen los archivos
   originales directamente y se salta al cuadro exacto (scrub en vivo). FFmpeg solo corre al exportar.
3. **Recorte como CapCut / Premiere**: se recorta tirando de las **manijas de los bordes del bloque** en la timeline.
   Nada de sliders para recortar. El bloque se achica o se alarga desde sus propios bordes, con imán y vibración.
4. **Íconos y botones de la marca**: el set SVG de `KyroNeoBoard/web/_app/mesa.js` (estilo Lucide, trazo 2) y los mismos
   comportamientos (`.ib`, `.pill`, `.stamp`, `.round`, menús, toasts, hojas).
5. **"Local convert" pasa a llamarse KyroAviMp4.**
6. **Biblioteca en grilla de 3**: cada video con su miniatura, en filas de 3 que se apilan hacia abajo. Al tocar uno se abre una
   **ventana emergente** con la vista previa.
7. Primero un **index.html** para revisarlo en el navegador; después se lleva a la APK.

## Etapa 1 — `web/index.html` (prototipo funcional)

- `web/index.html` + `web/app.js` + `web/fonts/` (Archivo, sin internet).
- **Videos**: tarjeta "Agregar videos", grilla de 3 con miniatura, tipo (AVI/MP4), duración, estado y progreso.
  Ventana emergente con el reproductor; para AVI que el navegador no abre, ofrece "Muestra de 3 s".
  Ajustes de salida: ancho (1010 por defecto), proporción Original / 1:1 / 3:4 / 4:3 / 16:9 sin recortar, alto calculado.
  Convertir todo con FFmpeg.wasm, de a uno, con reintento sin audio. Descargar MP4. Mandar al editor.
- **Editor**:
  - Preview sobre la mesa verde, dos `<video>` alternados (precarga del clip siguiente), color en vivo con un filtro SVG.
  - Timeline propia: cabezal rojo fijo al centro, arrastrar para recorrer, inercia, zoom con pellizco y botones, regla de tiempo.
  - Bloques con tira de fotogramas; tocar = seleccionar; manijas amarillas para recortar; mantener apretado = reordenar.
  - Pista de música con forma de onda real; manijas para recortar, arrastrar para moverla, volumen y fundidos.
  - Dividir en el cabezal, duplicar, borrar, deshacer y rehacer.
  - Color por clip: blanco y negro, saturación, exposición, contraste, luces, sombras, tinte de sombras y de luces
    (acá sí van sliders: son ajustes, no recortes). El mismo cálculo de curvas sirve para la preview y para la exportación.
  - Exportar el montaje a un MP4 con FFmpeg.wasm.
- Se guarda solo: proyecto en `localStorage` y archivos en IndexedDB.

## Etapa 2 — APK

- Misma interfaz en Compose con los tokens de la marca y Archivo en `res/font`.
- Preview con Media3/ExoPlayer: `ClippingMediaSource` por clip + `ConcatenatingMediaSource2`, seek directo al
  mover la timeline y efectos GL para el color. FFmpeg (`ffmpeg-kit-min`) queda solo para convertir AVI y exportar.
- AVI → proxy MP4 al importar, porque ExoPlayer no abre AVI.
- Timeline en Compose con gestos propios (manijas, pulsación larga, pellizco), igual que la web.
- Carpeta de salida por SAF.
