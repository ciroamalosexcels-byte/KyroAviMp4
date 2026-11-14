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
import android.provider.MediaStore
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
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

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
