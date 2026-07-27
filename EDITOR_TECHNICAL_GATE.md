# Validacion tecnica del editor

Este documento fija las capacidades que deben comprobarse antes de construir la linea de tiempo. La aplicacion estable no cambia durante esta fase.

## Paquete evaluado

- Dependencia: `dev.ffmpegkit-maintained:ffmpeg-kit-min:8.1.7`.
- Licencia declarada: LGPL v3.
- FFmpeg incluido: rama 8.1 con MediaCodec habilitado y sin codecs externos.
- ABI incluidas en el artefacto actual: `arm64-v8a` y `x86_64`.
- Encoders esperados: MPEG-4 Part 2, AAC y `h264_mediacodec`.
- No incluye `libx264` ni `libopenh264`; por lo tanto, no existe un encoder H.264 de software propio dentro de la APK.

La aplicacion debe conservar avisos, licencia, fuentes correspondientes y datos de compilacion requeridos por LGPL. Las obligaciones de licencia de software son independientes de posibles patentes de codecs y deben revisarse antes de una distribucion comercial.

## Decision de arquitectura

1. FFmpeg `filter_complex` sera el compositor final porque puede leer AVI, recortar, unir, mezclar musica y aplicar todos los ajustes solicitados en un solo grafo.
2. H.264 mediante `h264_mediacodec` sera el encoder preferido solamente en dispositivos que superen una prueba de capacidad.
3. MPEG-4 Part 2 con AAC continuara como respaldo determinista cuando MediaCodec no sea compatible.
4. No se construira un puente manual entre FFmpeg y MediaCodec: el paquete actual ya incluye esa integracion.
5. Media3 se evaluara para reproducir proxies y la linea de tiempo. No sera el exportador principal inicialmente porque no abre AVI y los controles avanzados de luces, sombras y tintes requeririan efectos GL propios.

## Prueba automatizada

`EditorCapabilityTest.kt` comprueba en Android:

- Configuracion MediaCodec activa y ausencia de componentes GPL/x264.
- Filtros `concat`, `trim`, `atrim`, `amix`, `eq`, `exposure`, `curves` y `colorbalance`.
- Encoders MPEG-4, AAC y H.264 MediaCodec.
- Decoders MJPEG, H.264, MP3 y PCM.
- Un montaje real con dos clips recortados, audio concatenado, musica mezclada y correccion de color.
- Salida MPEG-4/AAC valida, con duracion y dimensiones verificadas por FFprobe.
- Codificacion H.264 a traves de un encoder AVC expuesto por Android.

Estado de CI: pendiente de la primera ejecucion de esta prueba.

## Validacion pendiente en telefonos

El emulador confirma integracion, pero no representa el hardware de los celulares. Antes de activar H.264 para usuarios se debe probar:

- `1010 x 1346`, 720p y 1080p a 30 FPS.
- Al menos un dispositivo Qualcomm, Samsung o Tensor y MediaTek.
- APIs antiguas 24-28, intermedias 29-32 y actuales 33-35.
- Archivo real de 30 a 60 segundos con AVI, cortes, musica MP3 y filtros.
- Temperatura, tiempo de exportacion, cancelacion, seek y reproduccion del MP4 terminado.

Hasta completar esa matriz, H.264 queda clasificado como experimental y MPEG-4 sigue siendo el camino estable.

## Fuentes

- Artefacto Maven: https://central.sonatype.com/artifact/dev.ffmpegkit-maintained/ffmpeg-kit-min/8.1.7
- Fuentes mantenidas: https://github.com/ffmpegkit-maintained/ffmpeg/tree/v8.1.7
- Paquetes FFmpegKit: https://github.com/arthenica/ffmpeg-kit/wiki/Packages
- Filtros FFmpeg: https://ffmpeg.org/ffmpeg-filters.html
- Formatos Android: https://developer.android.com/media/platform/supported-formats
- Media3 Transformer: https://developer.android.com/media/media3/transformer
- Cumplimiento FFmpeg: https://ffmpeg.org/legal.html
