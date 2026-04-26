package com.eldon.consigliere

import android.content.Context
import android.webkit.WebView

class ContentFilter(private val context: Context) {

    private val blockedSites = setOf(
        // مراهنات
        "bet365.com", "williamhill.com", "betway.com", "unibet.com",
        "bwin.com", "betfair.com", "888sport.com", "paddypower.com",
        "coral.co.uk", "ladbrokes.com", "skybet.com", "betvictor.com",
        // إباحي
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com",
        "youporn.com", "tube8.com", "xhamster.com", "brazzers.com",
        "onlyfans.com", "chaturbate.com", "cam4.com", "myfreecams.com",
        // خمور
        "wine.com", "drizly.com", "totalwine.com", "vivino.com"
    )

    fun isBlocked(url: String): Boolean {
        val host = try {
            android.net.Uri.parse(url).host?.lowercase() ?: return false
        } catch (e: Exception) { return false }

        return blockedSites.any { host.contains(it) }
    }

    fun showBlockedPage(webView: WebView, reason: String = "محتوى محظور") {
        val html = """
            <!DOCTYPE html>
            <html dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        background: #0a0a0f;
                        color: #e0e0e0;
                        font-family: 'Cairo', sans-serif;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        text-align: center;
                        padding: 20px;
                    }
                    .container {
                        max-width: 400px;
                    }
                    .icon {
                        font-size: 64px;
                        margin-bottom: 20px;
                    }
                    h1 {
                        font-size: 24px;
                        color: #c0392b;
                        margin-bottom: 12px;
                    }
                    p {
                        color: #888;
                        font-size: 16px;
                        line-height: 1.6;
                    }
                    .back-btn {
                        margin-top: 30px;
                        padding: 12px 30px;
                        background: #1a1a2e;
                        color: #e0e0e0;
                        border: 1px solid #333;
                        border-radius: 8px;
                        font-size: 16px;
                        cursor: pointer;
                        text-decoration: none;
                        display: inline-block;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">🚫</div>
                    <h1>$reason</h1>
                    <p>هذا الموقع محظور في Consigliere لحمايتك</p>
                    <a class="back-btn" onclick="history.back()">العودة</a>
                </div>
            </body>
            </html>
        """.trimIndent()
        webView.loadData(html, "text/html", "UTF-8")
    }
}
