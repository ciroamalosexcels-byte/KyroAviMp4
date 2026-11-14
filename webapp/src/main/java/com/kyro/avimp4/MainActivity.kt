package com.kyro.avimp4

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.webkit.WebViewAssetLoader
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * KyroAviMp4: la interfaz web (carpeta web/) dentro de una app Android.
 * Los archivos se sirven desde la propia APK con un origen https seguro,
 * así funcionan IndexedDB, el motor de video y la reproducción.
 */
class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private lateinit var chrome: KyroChrome
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    // archivos elegidos: "nombre|tamaño" -> uri, para que FFmpeg nativo los pueda leer
    private val picked = ConcurrentHashMap<String, Uri>()
    private val work by lazy { File(cacheDir, "kyro").apply { mkdirs() } }
    private val srcDir by lazy { File(cacheDir, "kyro-src").apply { mkdirs() } }
    private val jobs = Executors.newSingleThreadExecutor()
    @Volatile private var cancelled = false

    private val pickFiles = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = fileCallback ?: return@registerForActivityResult
        fileCallback = null
        val uris = mutableListOf<Uri>()
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val clip = data.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
            } else {
                data.data?.let { uris.add(it) }
            }
        }
        remember(uris)
        callback.onReceiveValue(if (uris.isEmpty()) null else uris.toTypedArray())
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/kyro/", WebViewAssetLoader.InternalStoragePathHandler(this, work))
            .build()

        web.setBackgroundColor(Color.parseColor("#2B6B58"))
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == HOST) return false
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                return true
            }
        }
        chrome = KyroChrome()
        web.webChromeClient = chrome
        web.addJavascriptInterface(Bridge(), "KyroAndroid")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (chrome.isFullscreen) {
                    chrome.onHideCustomView()
                    return
                }
                web.evaluateJavascript("window.kyroBack ? window.kyroBack() : false") { handled ->
                    if (handled != "true") finish()
                }
            }
        })

        web.loadUrl("https://$HOST/assets/index.html")
    }

    override fun onPause() {
        web.evaluateJavascript("window.kyroPause && window.kyroPause()", null)
        super.onPause()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    private fun remember(uris: List<Uri>) {
        for (u in uris) runCatching {
            contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) picked["${c.getString(0)}|${c.getLong(1)}"] = u
            }
        }
    }

    private fun srcFile(key: String) = File(srcDir, Integer.toHexString(key.hashCode()) + "-" + key.length + ".src")

    private fun emit(type: String, job: String, data: String) = runOnUiThread {
        web.evaluateJavascript("window.kyroNative && window.kyroNative(${JSONObject.quote(type)},${JSONObject.quote(job)},${JSONObject.quote(data)})", null)
    }

    /** Copia el original una sola vez, lo analiza y genera la vista previa o el MP4 final. */
    private fun runJob(job: String, key: String, mode: String, vf: String) {
        cancelled = false
        try {
            val src = srcFile(key)
            if (!src.exists() || src.length() == 0L) {
                val uri = picked[key] ?: throw IllegalStateException("archivo no disponible")
                val tmp = File(srcDir, src.name + ".part")
                contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it, 1 shl 16) } }
                    ?: throw IllegalStateException("no se pudo leer")
                tmp.renameTo(src)
            }
            trimCache(src)
            val info = FFprobeKit.getMediaInformation(src.absolutePath)?.mediaInformation
            val dur = info?.duration?.toDoubleOrNull() ?: 0.0
            val video = info?.streams?.firstOrNull { it.type == "video" }
            emit("info", job, JSONObject().put("dur", dur).put("w", video?.width ?: 0L).put("h", video?.height ?: 0L).toString())
            val out = File(work, "$job.mp4")
            val proxy = mode == "proxy"
            val mid = File(work, "$job.mid.mp4")
            val filter = if (proxy) "scale='trunc(min(640,iw)/16)*16':-16,setsar=1" else vf.ifBlank { "scale=trunc(iw/2)*2:-2,setsar=1" }
            fun args(audio: Boolean): Array<String> = buildList {
                addAll(listOf("-y", "-hide_banner", "-loglevel", "warning", "-fflags", "+genpts+discardcorrupt", "-err_detect", "ignore_err",
                    "-i", src.absolutePath, "-map", "0:v:0", "-vf", filter, "-pix_fmt", "yuv420p", "-c:v", "mpeg4", "-q:v", if (proxy) "5" else "2", "-g", "15", "-bf", "0"))
                if (audio) addAll(listOf("-map", "0:a:0?", "-c:a", "aac", "-b:a", if (proxy) "96k" else "160k", "-ar", "44100")) else add("-an")
                addAll(listOf("-avoid_negative_ts", "make_zero", "-max_muxing_queue_size", "1024", "-movflags", "+faststart", mid.absolutePath))
            }.toTypedArray()
            var ok = ffmpeg(args(true), dur, job, 0.0, 0.55)
            if (!ok && !cancelled) ok = ffmpeg(args(false), dur, job, 0.0, 0.55)
            if (!ok || mid.length() == 0L) { mid.delete(); emit("error", job, if (cancelled) "cancelado" else "FFmpeg no pudo leer el video"); return }
            val h264 = !cancelled && toH264(mid, out, job)
            if (h264) { mid.delete(); emit("done", job, "/kyro/$job.mp4") }
            else { out.delete(); mid.delete(); emit("error", job, if (cancelled) "cancelado" else "Android no pudo pasar el video a H.264") }
        } catch (e: Exception) {
            emit("error", job, e.message ?: "error")
        }
    }

    /** Android convierte a H.264/AAC con su codificador de hardware (Media3 Transformer). */
    private fun toH264(input: File, output: File, job: String): Boolean {
        val main = Handler(Looper.getMainLooper())
        val latch = CountDownLatch(1)
        var ok = false
        var transformer: Transformer? = null
        main.post {
            try {
                val t = Transformer.Builder(this)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) { ok = true; latch.countDown() }
                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            emit("log", job, exportException.message ?: "error")
                            latch.countDown()
                        }
                    })
                    .build()
                transformer = t
                t.start(MediaItem.fromUri(Uri.fromFile(input)), output.absolutePath)
                val holder = ProgressHolder()
                val poll = object : Runnable {
                    override fun run() {
                        if (latch.count == 0L) return
                        if (cancelled) { t.cancel(); latch.countDown(); return }
                        if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) emit("progress", job, (0.55 + holder.progress / 100.0 * 0.45).toString())
                        main.postDelayed(this, 300)
                    }
                }
                main.postDelayed(poll, 300)
            } catch (e: Exception) {
                emit("log", job, e.message ?: "error")
                latch.countDown()
            }
        }
        latch.await()
        return ok && output.length() > 0
    }

    private fun ffmpeg(args: Array<String>, dur: Double, job: String, from: Double = 0.0, span: Double = 1.0): Boolean {
        val latch = CountDownLatch(1)
        var ok = false
        var last = -1
        FFmpegKit.executeWithArgumentsAsync(args, { session ->
            ok = ReturnCode.isSuccess(session.returnCode)
            latch.countDown()
        }, null, { stats ->
            if (dur > 0) {
                val pct = ((stats.time.toDouble() / 1000.0 / dur) * 100).toInt().coerceIn(0, 100)
                if (pct != last) { last = pct; emit("progress", job, (from + pct / 100.0 * span).toString()) }
            }
        })
        latch.await()
        return ok
    }

    /** Guarda como máximo ~2 GB de originales copiados. */
    private fun trimCache(keep: File) {
        val files = srcDir.listFiles()?.filter { it.isFile && it != keep }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() } + keep.length()
        for (f in files) { if (total < 2_000_000_000L) break; total -= f.length(); f.delete() }
    }

    /** Selector de archivos del sistema (varios a la vez) y pantalla completa del video. */
    private inner class KyroChrome : WebChromeClient() {
        private var customView: View? = null
        private var customCallback: CustomViewCallback? = null
        val isFullscreen get() = customView != null

        override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
            fileCallback?.onReceiveValue(null)
            fileCallback = callback
            val accepts = params.acceptTypes.flatMap { it.split(',') }.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val mimes = accepts.filter { it.contains('/') }.toMutableSet()
            // los AVI a veces llegan sin tipo de video declarado
            if (accepts.any { it == ".avi" }) mimes.addAll(listOf("video/x-msvideo", "video/avi", "application/x-troff-msvideo", "application/octet-stream"))
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = if (mimes.size == 1) mimes.first() else "*/*"
                if (mimes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, mimes.toTypedArray())
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
            }
            return try {
                pickFiles.launch(intent)
                true
            } catch (e: Exception) {
                fileCallback = null
                false
            }
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (customView != null) {
                callback.onCustomViewHidden()
                return
            }
            customView = view
            customCallback = callback
            view.setBackgroundColor(Color.BLACK)
            root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            WindowCompat.getInsetsController(window, root).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        override fun onHideCustomView() {
            val view = customView ?: return
            root.removeView(view)
            customView = null
            customCallback?.onCustomViewHidden()
            customCallback = null
            WindowCompat.getInsetsController(window, root).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Guardar MP4 exportados o convertidos en Descargas/KyroAviMp4, por partes. */
    private inner class Bridge {
        private val streams = ConcurrentHashMap<String, Pair<OutputStream, Uri?>>()

        @JavascriptInterface
        fun canProcess(key: String): Boolean = picked.containsKey(key) || srcFile(key).exists()

        @JavascriptInterface
        fun process(job: String, key: String, mode: String, vf: String): Boolean {
            if (!canProcess(key)) return false
            jobs.execute { runJob(job, key, mode, vf) }
            return true
        }

        @JavascriptInterface
        fun release(job: String) { File(work, "$job.mp4").delete() }

        @JavascriptInterface
        fun cancel() { cancelled = true; FFmpegKit.cancel() }

        @JavascriptInterface
        fun saveBegin(name: String, mime: String): String {
            return try {
                val id = System.nanoTime().toString()
                if (Build.VERSION.SDK_INT >= 29) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, mime)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KyroAviMp4")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return ""
                    val out = contentResolver.openOutputStream(uri) ?: return ""
                    streams[id] = out to uri
                } else {
                    val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
                    dir.mkdirs()
                    streams[id] = FileOutputStream(File(dir, name)) to null
                }
                id
            } catch (e: Exception) {
                ""
            }
        }

        @JavascriptInterface
        fun saveChunk(id: String, base64: String): Boolean {
            val stream = streams[id] ?: return false
            return try {
                stream.first.write(Base64.decode(base64, Base64.DEFAULT))
                true
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun saveEnd(id: String): String {
            val stream = streams.remove(id) ?: return ""
            return try {
                stream.first.close()
                val uri = stream.second
                if (uri != null && Build.VERSION.SDK_INT >= 29) {
                    contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                    "Descargas/KyroAviMp4"
                } else {
                    (getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir).absolutePath
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    companion object {
        private const val HOST = "appassets.androidplatform.net"
    }
}
