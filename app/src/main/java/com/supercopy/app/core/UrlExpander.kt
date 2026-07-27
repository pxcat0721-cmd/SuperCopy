package com.supercopy.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

/**
 * 短链展开 — server.py 的 Kotlin 移植，直接在 App 内发请求。
 * HTTP 重定向链 + JS/meta 跳转解析 + 反爬/验证码检测 + 结果缓存。
 */
object UrlExpander {

    data class Result(val finalUrl: String, val blocked: Boolean)

    private val cache = ConcurrentHashMap<String, Result>()

    init {
        // HttpURLConnection 自动使用默认 CookieHandler（短链跳转常需要 Cookie）
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
        }
    }

    // 命中即视为验证码/登录页的 URL 特征
    private val CAPTCHA_URL_PATTERNS = listOf(
        "website-login", "captcha", "verify.", "/login?", "/login/", "verify.xiaohongshu",
    )

    // 正文关键词启发式
    private val CAPTCHA_BODY_INDICATORS = listOf(
        "captcha", "验证码", "滑块验证", "请完成安全验证",
        "please verify you are a human", "are you a robot",
        "website-login", "请登录", "登录后查看",
        "访问受限", "请求太频繁", "过度访问",
        "_sec_verify",
    )

    private fun isCaptchaOrLogin(url: String): Boolean {
        val lower = url.lowercase()
        return CAPTCHA_URL_PATTERNS.any { it in lower }
    }

    private fun bodyLooksLikeCaptcha(body: String): Boolean {
        if (body.isEmpty()) return false
        val lower = body.lowercase()
        return CAPTCHA_BODY_INDICATORS.any { it in lower }
    }

    // JS/meta 跳转的四种提取模式（与 server.py 一致）
    private val JS_REDIRECT_PATTERNS = listOf(
        // <input id="target" value="https://...">  (reurl.cc)
        Regex("""<input[^>]+(?:id|name)\s*=\s*["']target["'][^>]+value\s*=\s*["'](https?://[^"']+)["']"""),
        // var url = 'https://...'  (m.tb.cn 等)
        Regex("""var\s+url\s*=\s*['"](https?://[^'"]+)['"]"""),
        // window.location.href = '...'
        Regex("""(?:window\.location(?:\.href)?|location)\s*=\s*['"](https?://[^'"]+)['"]"""),
    )

    private val META_REFRESH = Regex(
        """<meta[^>]+http-equiv\s*=\s*["']refresh["'][^>]+content\s*=\s*["']\d+\s*;\s*url\s*=\s*(.+?)["'\s>]""",
        RegexOption.IGNORE_CASE
    )

    private fun extractJsRedirect(html: String): String? {
        for (p in JS_REDIRECT_PATTERNS) {
            p.find(html)?.let { return it.groupValues[1] }
        }
        META_REFRESH.find(html)?.let {
            val target = it.groupValues[1].trim().trim('\'', '"')
            if (target.startsWith("http")) return target
        }
        return null
    }

    /** 展开短链；结果缓存，网络异常不缓存以便重试 */
    suspend fun expand(url: String): Result = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }
        try {
            val result = doExpand(url)
            cache[url] = result
            result
        } catch (e: Exception) {
            android.util.Log.w("UrlExpander", "expand failed: $url", e)
            Result(url, blocked = false)
        }
    }

    private fun doExpand(startUrl: String): Result {
        // 第一跳：跟完整条 HTTP 重定向链
        val first = followHttpChain(startUrl, wantBody = true)
        if (first.blocked) return Result(startUrl, true)
        if (first.finalUrl != startUrl) return Result(first.finalUrl, false)

        // 没有 HTTP 跳转 — 短链页本身可能带 JS/meta 跳转
        val body = first.body ?: return Result(startUrl, false)
        if (bodyLooksLikeCaptcha(body)) return Result(startUrl, true)
        val jsTarget = extractJsRedirect(body) ?: return Result(startUrl, false)
        if (jsTarget == startUrl) return Result(startUrl, false)

        // 跟 JS 目标自己的 HTTP 链
        val second = followHttpChain(jsTarget, wantBody = false)
        if (second.blocked) return Result(startUrl, true)
        return Result(second.finalUrl, false)
    }

    private class Chain(val finalUrl: String, val body: String?, val blocked: Boolean)

    private fun followHttpChain(startUrl: String, wantBody: Boolean): Chain {
        var current = startUrl
        repeat(10) {
            if (isCaptchaOrLogin(current)) return Chain(current, null, true)
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(current).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 15000
                    requestMethod = "GET"
                    applyBrowserHeaders(this)
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: return Chain(current, null, false)
                    current = URL(URL(current), loc).toString()
                } else {
                    // 只有整条链一次跳转都没有时才需要读正文做 JS 检查
                    val body = if (wantBody && current == startUrl) readBody(conn) else null
                    return Chain(current, body, false)
                }
            } catch (e: IOException) {
                android.util.Log.w("UrlExpander", "fetch failed: $current", e)
                return Chain(current, null, false)
            } finally {
                conn?.disconnect()
            }
        }
        return Chain(current, null, false)
    }

    private fun applyBrowserHeaders(conn: HttpURLConnection) {
        conn.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        )
        conn.setRequestProperty(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
        )
        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-US;q=0.7")
        conn.setRequestProperty("Accept-Encoding", "gzip")
        conn.setRequestProperty("Cache-Control", "no-cache")
        conn.setRequestProperty("Pragma", "no-cache")
        conn.setRequestProperty("Sec-Ch-Ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
        conn.setRequestProperty("Sec-Ch-Ua-Mobile", "?0")
        conn.setRequestProperty("Sec-Ch-Ua-Platform", "\"Windows\"")
        conn.setRequestProperty("Sec-Fetch-Dest", "document")
        conn.setRequestProperty("Sec-Fetch-Mode", "navigate")
        conn.setRequestProperty("Sec-Fetch-Site", "none")
        conn.setRequestProperty("Sec-Fetch-User", "?1")
        conn.setRequestProperty("Upgrade-Insecure-Requests", "1")
        conn.setRequestProperty("Dnt", "1")
    }

    private const val MAX_BODY_BYTES = 1024 * 1024 // 1MB 上限，防超大页面

    private fun readBody(conn: HttpURLConnection): String {
        val rawStream = if (conn.responseCode >= 400) conn.errorStream else conn.inputStream
        val stream = if ((conn.contentEncoding ?: "").contains("gzip")) {
            GZIPInputStream(rawStream)
        } else rawStream
        val bytes = stream.use { readAtMost(it, MAX_BODY_BYTES) }
        val charset = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE)
            .find(conn.contentType ?: "")?.groupValues?.get(1)
            ?.let { runCatching { Charset.forName(it) }.getOrNull() }
            ?: Charsets.UTF_8
        return String(bytes, charset)
    }

    // InputStream.readNBytes 要 API 33，minSdk 26 不能用，手写等价实现
    private fun readAtMost(stream: java.io.InputStream, limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (total < limit) {
            val n = stream.read(buf, 0, minOf(buf.size, limit - total))
            if (n < 0) break
            out.write(buf, 0, n)
            total += n
        }
        return out.toByteArray()
    }
}
