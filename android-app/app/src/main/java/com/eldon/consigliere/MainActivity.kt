package com.eldon.consigliere

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnMenu: ImageButton

    private lateinit var adBlocker: AdBlocker
    private lateinit var contentFilter: ContentFilter

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adBlocker = AdBlocker(this)
        contentFilter = ContentFilter(this)

        initViews()
        setupWebView()
        setupUrlBar()
        setupNavButtons()

        // افتح صفحة البداية
        loadNewTab()
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
        btnMenu = findViewById(R.id.btnMenu)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
            // منع التتبع
            setGeolocationEnabled(false)
            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // فحص المحتوى المحظور
                if (contentFilter.isBlocked(request.url.toString())) {
                    return WebResourceResponse("text/html", "UTF-8", null)
                }
                // فحص الإعلانات
                return adBlocker.shouldBlock(request)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.isVisible = true
                urlBar.setText(url)
                updateNavButtons()

                // فحص الصفحة الكاملة
                if (contentFilter.isBlocked(url)) {
                    view.stopLoading()
                    contentFilter.showBlockedPage(view)
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.isVisible = false
                urlBar.setText(url)
                updateNavButtons()

                // حقن CSS لإخفاء الإعلانات المتبقية
                injectAdBlockCSS(view)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
    }

    private fun injectAdBlockCSS(webView: WebView) {
        val css = """
            [id*='ad'],[class*='ad'],[id*='banner'],[class*='banner'],
            [id*='popup'],[class*='popup'],[id*='overlay'],[class*='overlay'],
            [id*='sponsor'],[class*='sponsor'],iframe[src*='ad'],
            ins.adsbygoogle, #google_ads_frame, .advert, .advertisement {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                width: 0 !important;
                opacity: 0 !important;
            }
        """.replace("\n", " ")

        val js = """
            var style = document.createElement('style');
            style.type = 'text/css';
            style.innerHTML = '$css';
            document.head.appendChild(style);
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun setupUrlBar() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                navigateTo(urlBar.text.toString())
                true
            } else false
        }
    }

    private fun setupNavButtons() {
        btnBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        btnForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        btnRefresh.setOnClickListener { webView.reload() }
        btnHome.setOnClickListener { loadNewTab() }
        btnMenu.setOnClickListener { showMenu() }
    }

    private fun navigateTo(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=${android.net.Uri.encode(input)}"
        }
        webView.loadUrl(url)
    }

    private fun loadNewTab() {
        // صفحة البداية المخصصة
        webView.loadUrl("file:///android_asset/newtab.html")
    }

    private fun updateNavButtons() {
        btnBack.isEnabled = webView.canGoBack()
        btnBack.alpha = if (webView.canGoBack()) 1f else 0.4f
        btnForward.isEnabled = webView.canGoForward()
        btnForward.alpha = if (webView.canGoForward()) 1f else 0.4f
    }

    private fun showMenu() {
        // TODO: قائمة الإعدادات
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
