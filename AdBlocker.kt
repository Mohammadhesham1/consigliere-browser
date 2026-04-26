package com.eldon.consigliere

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

class AdBlocker(private val context: Context) {

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val blockedPatterns = mutableListOf<Regex>()
    private val client = OkHttpClient()

    // قوائم الفلترة
    private val filterLists = listOf(
        // إعلانات عامة
        "https://easylist.to/easylist/easylist.txt",
        // تتبع
        "https://easylist.to/easylist/easyprivacy.txt",
        // إعلانات يوتيوب وجوجل
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
        // ريدايركت وصفحات وسيطة
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/unbreak.txt",
        "https://raw.githubusercontent.com/nicehash/NiceHashAdBlock/master/adblock.txt",
        // مراهنات
        "https://raw.githubusercontent.com/nicehash/NiceHashAdBlock/master/gambling.txt",
        // محتوى إباحي
        "https://raw.githubusercontent.com/4skinSkywalker/Anti-Porn-uBlock-Origin-Filters/master/APUOF.txt",
        // AdGuard
        "https://filters.adtidy.org/android/filters/2_optimized.txt",
        "https://filters.adtidy.org/android/filters/3_optimized.txt"
    )

    // دومينات محجوبة يدوياً
    private val hardcodedBlockedDomains = setOf(
        // إعلانات يوتيوب
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "googletagservices.com",
        // تتبع
        "facebook.net", "facebook.com/tr", "analytics.twitter.com",
        "ads.twitter.com", "advertising.com", "adnxs.com",
        // مراهنات
        "bet365.com", "williamhill.com", "betway.com", "888casino.com",
        "pokerstars.com", "betfair.com", "draftkings.com", "fanduel.com",
        // إباحي
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com",
        "youporn.com", "tube8.com", "xhamster.com", "brazzers.com",
        // خمور
        "wine.com", "drizly.com", "totalwine.com"
    )

    init {
        blockedDomains.addAll(hardcodedBlockedDomains)
        loadFilterLists()
    }

    private fun loadFilterLists() {
        CoroutineScope(Dispatchers.IO).launch {
            filterLists.forEach { url ->
                try {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    response.body?.string()?.let { parseFilterList(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun parseFilterList(content: String) {
        content.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("!") || trimmed.startsWith("[") || trimmed.isEmpty() -> return@forEach
                trimmed.startsWith("||") -> {
                    val domain = trimmed
                        .removePrefix("||")
                        .substringBefore("^")
                        .substringBefore("/")
                        .substringBefore("$")
                    if (domain.isNotEmpty() && !domain.contains("*")) {
                        blockedDomains.add(domain)
                    }
                }
            }
        }
    }

    fun shouldBlock(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        val host = request.url.host ?: return null

        // فحص الدومين
        if (isBlocked(host)) return emptyResponse()

        // فحص الريدايركت والصفحات الوسيطة
        if (isRedirectPage(url)) return emptyResponse()

        // فحص المحتوى الممنوع
        if (isRestrictedContent(url)) return emptyResponse()

        return null
    }

    private fun isBlocked(host: String): Boolean {
        if (blockedDomains.contains(host)) return true
        // فحص السبدومينات
        val parts = host.split(".")
        for (i in 1 until parts.size - 1) {
            val domain = parts.drop(i).joinToString(".")
            if (blockedDomains.contains(domain)) return true
        }
        return false
    }

    private fun isRedirectPage(url: String): Boolean {
        val redirectPatterns = listOf(
            "adf.ly", "adfly.com", "linkbucks.com", "shorte.st",
            "ouo.io", "bc.vc", "sh.st", "festyy.com", "ceesty.com",
            "/redirect", "/go.php", "/out.php", "/track/click",
            "click.php?url=", "redirect.php?url="
        )
        return redirectPatterns.any { url.contains(it) }
    }

    private fun isRestrictedContent(url: String): Boolean {
        val restrictedKeywords = listOf(
            "porn", "xxx", "adult", "casino", "gambling", "betting",
            "poker", "slot", "alcohol", "whiskey", "vodka", "beer-ads"
        )
        val lowerUrl = url.lowercase()
        return restrictedKeywords.any { lowerUrl.contains(it) }
    }

    private fun emptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain", "utf-8", 200, "OK",
            mapOf("Access-Control-Allow-Origin" to "*"),
            ByteArrayInputStream("".toByteArray())
        )
    }

    fun refreshLists() {
        loadFilterLists()
    }
}
