# Roadmap del editor movil

Este documento registra el alcance solicitado para evolucionar el conversor sin perder la estabilidad actual. La edicion sera no destructiva: nunca se modificaran los archivos originales.

## Requisitos registrados

- Unir todos los clips convertidos y exportarlos como un unico MP4.
- Mostrar una linea de tiempo sencilla para celular con fotogramas y cabezal fijo.
- Recortar el comienzo o el final de cada clip.
- Dividir un clip, mover cada segmento y cambiar el orden para construir una historia.
- Agregar una pista MP3 debajo del video y recordar su ultima ubicacion.
- Guardar y reabrir proyectos con sus clips, cortes, orden, musica y ajustes.
- Agregar controles para blanco y negro, contraste, exposicion o iluminacion, luces y sombras.
- Agregar tintes o controles HSL para el color de luces y sombras.
- Exportar el montaje completo a MP4 desde el celular.

## Principios de interfaz

- Tomar patrones conocidos de editores moviles como CapCut y Premiere: filmstrip horizontal, cabezal centrado, manijas de recorte, division en el cabezal, arrastre prolongado para ordenar y barra contextual inferior.
- No copiar marcas, iconos ni recursos propietarios.
- Mantener una sola pista de video y una sola pista de musica en la primera version.
- Incluir deshacer y rehacer desde el primer editor funcional.
- No ejecutar FFmpeg ni lectura pesada de archivos durante un gesto de la linea de tiempo.

## Fase 0: validacion tecnica

- Confirmar codecs, filtros y aceleracion disponibles en el paquete FFmpeg actual.
- Validar `concat`, `trim`, `atrim`, `amix`, `hue`, `exposure`, `curves` y `colorbalance`.
- Definir si la exportacion final usara MPEG-4, H.264 por MediaCodec u otro encoder compatible.
- Revisar licencias antes de cambiar el paquete FFmpeg.

Los criterios, decisiones y resultados de esta fase se registran en `EDITOR_TECHNICAL_GATE.md`.

## Fase 1: proyectos y medios

- Separar la interfaz, estado del editor, acceso SAF, probing y exportacion de `MainActivity.kt`.
- Guardar proyectos y clips en Room; usar DataStore para preferencias y ubicaciones recientes.
- Persistir permisos SAF y ofrecer reconexion cuando un archivo se mueva o pierda acceso.
- Modelo minimo: `Project`, `Asset`, `TimelineClip`, `MusicTrack` y comandos de historial.

Estado: proyecto unico persistente con permisos SAF, clips no destructivos, musica, color y migracion automatica desde la version 1.5. El historial reversible vive en memoria durante la sesion. Proyectos multiples y Room siguen pendientes.

## Fase 2: proxies y recursos visuales

- Detectar metadatos una vez con FFprobe y almacenarlos.
- Crear proxies de baja resolucion y frame rate constante para una preview fluida.
- Generar miniaturas para el filmstrip y forma de onda para el MP3 en segundo plano.
- Limitar la cache y verificar espacio libre antes de procesar.

## Fase 3: linea de tiempo

- Implementar seleccion, recorte de inicio y fin, division en el cabezal y eliminacion.
- Permitir reordenar clips y segmentos con pulsacion prolongada y arrastre.
- Agregar zoom de timeline, ajuste magnetico a bordes y respuesta haptica.
- Representar cada cambio como comando reversible para deshacer y rehacer.

Estado: version 1.6 con timeline horizontal, miniaturas, seleccion, preview, recorte, division en la posicion de reproduccion, eliminacion, deshacer/rehacer y reordenamiento por pulsacion larga y arrastre. Zoom, ajuste magnetico y respuesta haptica siguen pendientes.

La version 1.7 reorganiza la pantalla como un editor movil: preview fija, transporte, timeline de escala temporal comun con cabezal central y dock inferior contextual. El cabezal global se traduce al tiempo fuente de cada segmento para seleccionar y dividir con precision.

## Fase 4: musica y color

- Importar un MP3, recortarlo, mover su inicio, cambiar volumen y aplicar fundidos.
- Mostrar su forma de onda debajo de la pista de video.
- Usar un modelo comun de parametros para que preview y exportacion coincidan.
- Aplicar saturacion y blanco y negro con `hue`; exposicion con `exposure`; contraste, luces y sombras con `curves`; tintes con `colorbalance`.

Estado: version 1.7 con una pista de audio persistente, recorte, posicion, volumen, fundidos y representacion debajo del video. Cada segmento guarda controles de color independientes. Una preview compuesta automatica muestra clips, color y musica antes de exportar usando el mismo grafo final. La preview GPU instantanea y la forma de onda calculada desde el MP3 siguen pendientes.

## Fase 5: exportacion conjunta

- Normalizar resolucion, FPS, formato de pixel, audio y timestamps de todos los segmentos.
- Aplicar `trim`, `setpts`, escala y filtros a cada clip; luego unirlos con `concat`.
- Mezclar audio original y musica con `amix`, generando silencio en clips sin audio.
- Codificar una sola vez, mostrar progreso, permitir cancelacion y borrar temporales si falla.
- Ejecutar exportaciones largas como trabajo en primer plano y copiar el MP4 terminado a la carpeta SAF.

Estado: version 1.6 exporta la secuencia completa en un grafo `filter_complex`, normaliza los segmentos, genera silencio cuando falta audio, mezcla el MP3, aplica color, muestra progreso, permite cancelar y limpia temporales. El servicio en primer plano para continuar con la aplicacion cerrada sigue pendiente.

## Riesgos principales

- AVI puede buscar cuadros con poca precision; los proxies normalizados reducen ese problema.
- Los codecs disponibles varian segun paquete y dispositivo; la fase 0 es obligatoria.
- Preview y exportacion pueden diferir en color; se necesitan comparaciones de cuadros de referencia.
- Proxies y exportaciones consumen espacio; se necesita cuota de cache y control de almacenamiento.
- Los permisos SAF pueden revocarse; cada proyecto debe detectar y reparar archivos inaccesibles.

## Referencias tecnicas

- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
- Android Media3 Transformer: https://developer.android.com/media/media3/transformer
- FFmpeg Filters: https://ffmpeg.org/ffmpeg-filters.html
- FFmpeg Formats y concat: https://ffmpeg.org/ffmpeg-formats.html#concat
