# Validacion tecnica del editor

Este documento fija las capacidades que deben comprobarse antes de construir la linea de tiempo. La aplicacion estable no cambia durante esta fase.

## Paquete evaluado

- Dependencia: `dev.ffmpegkit-maintained:ffmpeg-kit-min:8.1.7`.
- Licencia declarada: LGPL v3.
- FFmpeg incluido: rama 8.1 con MediaCodec habilitado y sin codecs externos.
- ABI incluidas en el artefacto actual: `arm64-v8a` y `x86_64`.
- Encoders esperados: MPEG-4 Part 2, AAC y `h264_mediacodec`.
- No incluye `libx264` ni `libopenh264`; por lo tanto, no existe un encoder H.264 de software propio dentro de la APK.

El filtro `eq` tampoco forma parte del paquete porque su implementacion en FFmpeg es GPL. No se cambiara a una variante GPL: los controles se construiran con filtros LGPL ya incluidos.

La aplicacion debe conservar avisos, licencia, fuentes correspondientes y datos de compilacion requeridos por LGPL. Las obligaciones de licencia de software son independientes de posibles patentes de codecs y deben revisarse antes de una distribucion comercial.

## Decision de arquitectura

1. FFmpeg `filter_complex` sera el compositor final porque puede leer AVI, recortar, unir, mezclar musica y aplicar todos los ajustes solicitados en un solo grafo.
2. H.264 mediante `h264_mediacodec` sera el encoder preferido solamente en dispositivos que superen una prueba de capacidad.
3. MPEG-4 Part 2 con AAC continuara como respaldo determinista cuando MediaCodec no sea compatible.
4. No se construira un puente manual entre FFmpeg y MediaCodec: el paquete actual ya incluye esa integracion.
5. Media3 se evaluara para reproducir proxies y la linea de tiempo. No sera el exportador principal inicialmente porque no abre AVI y los controles avanzados de luces, sombras y tintes requeririan efectos GL propios.

## Mapeo de controles de imagen

- Blanco y negro y saturacion: `hue`.
- Contraste: curva maestra en `curves`.
- Luces y sombras: puntos bajos y altos de `curves`.
- Iluminacion directa: `exposure`, agregado solo cuando el control no sea neutral.
- Color de sombras y luces: `colorbalance`.
- Orden inicial: `hue`, `exposure` opcional, `curves`, `colorbalance`.

Los filtros con valores neutrales se omiten para reducir conversiones de formato y trabajo de CPU.

## Prueba automatizada

`EditorCapabilityTest.kt` comprueba en Android:

- Configuracion MediaCodec activa y ausencia de componentes GPL/x264.
- Filtros `concat`, `trim`, `atrim`, `amix`, `hue`, `exposure`, `curves` y `colorbalance`.
- Encoders MPEG-4, AAC y H.264 MediaCodec.
- Decoders MJPEG, H.264, MP3 y PCM.
- Un montaje real con dos clips recortados, audio concatenado, musica mezclada y correccion de color.
- Salida MPEG-4/AAC valida, con duracion y dimensiones verificadas por FFprobe.
- Inventario de encoders AVC expuestos por Android para preparar la prueba en telefonos.

Estado de CI de la fase inicial: aprobado en https://github.com/ciroamalosexcels-byte/KyroAviMp4/actions/runs/30270640687. La version 1.6 amplia la prueba para cubrir normalizacion, silencio sintetico, posicion y fundidos de musica, persistencia y construccion del grafo de exportacion.

## Validacion pendiente en telefonos

El emulador confirma integracion, pero no representa el hardware de los celulares. Antes de activar H.264 para usuarios se debe probar:

- `1010 x 1346`, 720p y 1080p a 30 FPS.
- Al menos un dispositivo Qualcomm, Samsung o Tensor y MediaTek.
- APIs antiguas 24-28, intermedias 29-32 y actuales 33-35.
- Archivo real de 30 a 60 segundos con AVI, cortes, musica MP3 y filtros.
- Temperatura, tiempo de exportacion, cancelacion, seek y reproduccion del MP4 terminado.

Hasta completar esa matriz, H.264 queda clasificado como experimental y MPEG-4 sigue siendo el camino estable.

Una codificacion H.264 directa se intento inicialmente en el emulador, pero MediaCodec no completo la operacion dentro de un tiempo razonable. Por eso CI solo comprueba que la integracion y el encoder existen; la ejecucion H.264 se reserva para hardware fisico con timeout y cancelacion.

El primer grafo de color intento usar `eq`; el paquete informo correctamente que no existe, pero el proceso nativo termino con SIGSEGV. El compilador de exportacion debe usar solamente la lista de filtros validada por esta prueba y nunca enviar nombres opcionales no disponibles.

## Fuentes

- Artefacto Maven: https://central.sonatype.com/artifact/dev.ffmpegkit-maintained/ffmpeg-kit-min/8.1.7
- Fuentes mantenidas: https://github.com/ffmpegkit-maintained/ffmpeg/tree/v8.1.7
- Paquetes FFmpegKit: https://github.com/arthenica/ffmpeg-kit/wiki/Packages
- Filtros FFmpeg: https://ffmpeg.org/ffmpeg-filters.html
- Formatos Android: https://developer.android.com/media/platform/supported-formats
- Media3 Transformer: https://developer.android.com/media/media3/transformer
- Cumplimiento FFmpeg: https://ffmpeg.org/legal.html
