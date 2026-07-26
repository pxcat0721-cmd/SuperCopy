package com.supercopy.app.core

import java.net.URLDecoder

/** 可开关的处理功能，对应网页版侧边栏 */
enum class Filter(val label: String) {
    // 链接处理（顺序即执行顺序）
    EXTRACT_URL("口令提取"),
    EXPAND("展开短链"),
    TRACKING("去除追踪标记"),
    BVAV("BV⇄AV 互转"),
    URL_DECODE("URL 解码"),

    // 文本过滤
    CHINESE("去除中文"),
    EMOJI("去除 Emoji"),
    NUMBERS("去除数字"),
    PUNCTUATION("去除标点"),
    WHITESPACE("压缩空格"),
    SPECIAL("去除特殊符号"),
    WEIBO("去除微博格式"),
}

data class ProcessResult(
    val output: String,
    val removedParts: List<String>,
    val blockedUrls: List<String>,
)

/**
 * 文本处理管线 — index.html 中 process() 的 Kotlin 移植。
 * 阶段一链接处理（口令提取 → 展开短链 → 去追踪 → BV/AV），
 * 阶段二文本过滤（URL 用占位符保护），URL 解码最后执行。
 */
object Processor {

    private val PATTERNS: Map<Filter, Regex> = mapOf(
        Filter.CHINESE to Regex("[\\u4e00-\\u9fff\\u3400-\\u4dbf\\uf900-\\ufaff\\u3000-\\u303f\\uff00-\\uffef\\u2e80-\\u2eff\\u31c0-\\u31ef\\u3200-\\u32ff\\u3300-\\u33ff\\ufe10-\\ufe1f\\ufe30-\\ufe4f]"),
        Filter.EMOJI to Regex("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E0}-\\x{1F1FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{1F900}-\\x{1F9FF}\\x{1FA00}-\\x{1FA6F}\\x{1FA70}-\\x{1FAFF}\\x{2300}-\\x{23FF}\\x{2B50}\\x{2B55}\\x{2934}\\x{2935}\\x{25AA}\\x{25AB}\\x{25B6}\\x{25C0}\\x{25FB}-\\x{25FE}\\x{200D}\\x{FE0F}\\x{20E3}\\x{1F004}\\x{1F0CF}\\x{1F18E}]"),
        Filter.NUMBERS to Regex("\\d+"),
        Filter.PUNCTUATION to Regex("[\\p{P}\\p{S}]"),
        Filter.WHITESPACE to Regex("[^\\S\\n]{2,}"), // 行内空白（换行单独处理）
        Filter.SPECIAL to Regex("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]"),
        Filter.WEIBO to Regex("\\[(?:/?cp|图片|表情|视频|链接|语音|音乐|投票)\\]"),
    )

    private val LINK_FILTERS = listOf(Filter.EXTRACT_URL, Filter.EXPAND, Filter.TRACKING, Filter.BVAV)

    // 展开短链用：只匹配 http(s) 开头
    private val HTTP_URL_REGEX = Regex(
        """(?:https?://|https?%3A%2F%2F)[^\s<>^`\[\]，。、；：！？（）【】《》「」『』“”‘’…]+""",
        RegexOption.IGNORE_CASE
    )

    suspend fun process(raw: String, active: Set<Filter>): ProcessResult {
        var result = raw
        val removedParts = mutableListOf<String>()
        val blockedUrls = mutableListOf<String>()

        // 阶段一：链接处理，顺序固定
        for (filter in LINK_FILTERS) {
            if (filter !in active) continue
            when (filter) {
                Filter.EXTRACT_URL -> {
                    val urls = TrackingRules.URL_REGEX.findAll(result).map { it.value }.toList()
                    if (urls.isNotEmpty()) {
                        result = urls.distinct().joinToString("\n")
                        removedParts.add("[口令提取: ${urls.size} 个链接]")
                    }
                }
                Filter.EXPAND -> {
                    val before = result
                    val expanded = expandShortUrls(result, blockedUrls)
                    result = expanded
                    if (before != result) removedParts.add("[短链已展开]")
                    if (blockedUrls.isNotEmpty()) {
                        removedParts.add("[反爬拦截: ${blockedUrls.size} 条短链未能展开]")
                    }
                }
                Filter.TRACKING -> {
                    val before = result
                    result = TrackingRules.strip(result)
                    if (before != result) {
                        removedParts.add("[追踪参数: 去除 ${before.length - result.length} 个字符]")
                    }
                }
                Filter.BVAV -> {
                    val before = result
                    result = BvAv.convert(result)
                    if (before != result) removedParts.add("[BV⇄AV 已转换]")
                }
                else -> Unit
            }
        }

        // 阶段二：文本过滤。先把 URL 换成占位符保护，避免被绞碎。
        // 占位符只含私有区字符 + 小写字母，不会被任何过滤规则命中。
        val textFilters = active.filter { it !in LINK_FILTERS && it != Filter.URL_DECODE }
        val savedUrls = mutableListOf<String>()
        if (textFilters.isNotEmpty()) {
            result = TrackingRules.URL_REGEX.replace(result) { m ->
                val token = "\uE000" + encodeIndex(savedUrls.size) + "\uE000"
                savedUrls.add(m.value)
                token
            }
        }

        for (filter in textFilters) {
            if (filter == Filter.WHITESPACE) {
                val before = result
                result = result.replace(PATTERNS.getValue(Filter.WHITESPACE), " ")
                result = result.replace(Regex("\\n{3,}"), "\n\n")
                result = result.trim()
                if (before != result) removedParts.add("多余空格")
            } else {
                val pattern = PATTERNS[filter] ?: continue
                val matches = pattern.findAll(result).map { it.value }.toList()
                if (matches.isNotEmpty()) {
                    val joined = matches.joinToString("")
                    val preview = joined.take(50)
                    removedParts.add("[${filter.label}: $preview${if (joined.length > 50) "..." else ""}]")
                }
                result = pattern.replace(result, "")
            }
        }

        // 还原被保护的 URL
        if (savedUrls.isNotEmpty()) {
            result = Regex("\uE000([a-j]+)\uE000").replace(result) { m ->
                val idx = decodeIndex(m.groupValues[1])
                savedUrls.getOrNull(idx) ?: m.value
            }
        }

        // URL 解码最后执行，不破坏前面阶段的 query 结构
        if (Filter.URL_DECODE in active) {
            val before = result
            result = urlDecodeText(result)
            if (before != result) removedParts.add("[URL解码]")
        }

        return ProcessResult(result, removedParts, blockedUrls)
    }

    private fun encodeIndex(i: Int): String =
        i.toString().map { "abcdefghij"[it - '0'] }.joinToString("")

    private fun decodeIndex(code: String): Int =
        code.map { "abcdefghij".indexOf(it) }.joinToString("").toIntOrNull() ?: -1

    // --- 短链展开（带缓存的 UrlExpander + 全量替换）---
    private suspend fun expandShortUrls(text: String, blockedOut: MutableList<String>): String {
        val urls = HTTP_URL_REGEX.findAll(text).map { it.value }.toList().distinct()
        if (urls.isEmpty()) return text

        val expansions = mutableListOf<Pair<String, String>>()
        for (url in urls) {
            val r = UrlExpander.expand(url)
            if (r.blocked) {
                blockedOut.add(url)
            } else if (r.finalUrl != url) {
                expansions.add(url to normalizeUrl(r.finalUrl))
            }
        }

        var result = text
        // 长 URL 先替换，避免某条短链恰好是另一条的前缀时误伤
        expansions.sortByDescending { it.first.length }
        for ((from, to) in expansions) {
            result = result.replace(from, to)
        }
        return result
    }

    // 展开结果标准化：小红书移动端格式 → PC 格式
    private fun normalizeUrl(url: String): String {
        val m = Regex(
            """^(https?://www\.xiaohongshu\.com)/discovery/item/([a-f0-9]+)(\?.*)?$""",
            RegexOption.IGNORE_CASE
        ).find(url) ?: return url
        return m.groupValues[1] + "/explore/" + m.groupValues[2] + m.groupValues[3]
    }

    // --- URL 解码：%xx → 字符，最多解 3 层嵌套编码 ---
    fun urlDecodeText(text: String): String {
        var prev = text
        repeat(3) {
            val decoded = tryDecode(prev)
            if (decoded == prev) return prev
            prev = decoded
        }
        return prev
    }

    private fun tryDecode(s: String): String = try {
        // URLDecoder 会把 + 解成空格，先保护起来以对齐 JS decodeURIComponent 语义
        URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
    } catch (_: Exception) {
        // 整体解码失败则只解合法的 %xx 序列
        Regex("%[0-9A-Fa-f]{2}").replace(s) { m ->
            try { URLDecoder.decode(m.value, "UTF-8") } catch (_: Exception) { m.value }
        }
    }
}
