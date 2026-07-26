package com.supercopy.app.core

import org.json.JSONObject
import java.net.URLDecoder

/**
 * URL 追踪参数清理规则 — tracking-rules.js 的 Kotlin 移植。
 * 规则数据与网页版保持一致；改规则时两边要同步。
 */
object TrackingRules {

    // --- 追踪参数黑名单：这些 query 参数会被移除 ---
    private val TRACKING_PARAMS = listOf(
        // CoolAPK & 通用中文平台
        "s", "spm", "spmid", "from", "source", "track", "trackid",
        "share_id", "shareid", "shared_from", "share_from", "share_medium",
        "share_plat", "share_session_id", "share_source", "share_tag",
        "from_spmid", "spmid_from", "scene", "channel", "refer", "ref",
        // 通用分析
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_source_platform", "utm_creative", "utm_creative_format",
        // Facebook / Instagram
        "fbclid", "fb_action_ids", "fb_action_types", "fb_source", "fb_ref",
        "igshid", "igsh",
        // Google
        "gclid", "gclsrc", "dclid", "gbraid", "wbraid",
        "oq", "gs_lcrp", "sourceid", "ie", "ved", "ei", "usg", "sig2",
        // Twitter / X
        "twclid", "t", "s09", "ref_src", "ref_url",
        // 其他
        "mc_cid", "mc_eid", "mc_tc",
        "_ga", "_gl", "_hsenc", "_hsmi", "_ke",
        "ck_subscriber_id", "oly_anon_id", "oly_enc_id",
        "otc", "rss", "sc_cid", "si",
        // 百度
        "rs", "ruk", "u",
        // 淘宝 / 天猫 / 京东 / 中国电商
        "ali_trackid", "ali_refid", "e", "skuid", "activityid",
        "app", "cpp", "price", "bxsign", "wxsign",
        "shareuniqueid", "share_crt_v", "shareurl", "short_name",
        "sourcetype", "sp_tk", "tk", "tbsocialpopkey",
        "un", "un_site", "ut_sk", "suid",
        "abbucket", "ns", "utparam", "pricetid", "mi_id", "xxc",
        // 拼多多
        "refer_page_name", "refer_page_id", "refer_page_el_sn",
        "page_from", "feeds_pack_id", "duoduo_type",
        "pxq_secret_key", "_oak_share_snapshot_num", "_oak_share_time",
        "share_oak_rcto", "_oak_share_ticket", "share_uin",
        "refer_share_id", "refer_share_uin", "refer_share_channel",
        "refer_share_form", "_x_share_id", "refer_page_sn", "uin",
        // 京东
        "jxsid", "jdsn", "mpja", "mpjb", "mpjc",
        "jd_pop", "jd_track", "ad_od", "utm_user",
        // Bilibili
        "buvid", "buvid3", "buvid4", "buvid5",
        "buvid6", "buvid7", "buvid8",
        "unique_k", "timestamp", "up_id",
        "plat_id", "platform", "plat", "is_story_h5", "story_h5",
        "share_style", "share_redirect_delay",
        // 微信
        "wx_scene", "wx_share_id",
        // 知乎
        "zhid", "utm_oi",
        // 抖音 / TikTok
        "enter_from", "enter_method", "previous_page",
        "sec_uid", "did", "iid", "device_id",
        // 微信公众号
        "xtrack", "req_id", "subscene", "sessionid", "clicktime",
        "enterid", "finder_biz_enter_id", "ranksessionid",
        "jumppath", "jumppathdepth", "ascene",
        "fasttmpl_type", "fasttmpl_fullversion", "fasttmpl_flag",
        "realreporttime", "devicetype", "nettype", "session_us",
        "exportkey", "pass_ticket", "wx_header", "flutter_pos",
        // 豆瓣
        "_spm_id", "_i", "dt_dapp", "dt_platform",
        // 贴吧
        "share", "fr", "see_lz", "sfc", "client_type", "client_version",
        "st", "is_video", "unique",
        // 快手
        "cc", "followrefer", "sharemethod", "docid", "kpn", "subbiz",
        "photoid", "sharetoken", "shareresourcetype", "userid",
        "sharetype", "et", "sharemode", "efid", "originshareid",
        "apptype", "shareobjectid", "shareurlopened", "location",
        // 红果短剧
        "zlink", "gd_label", "share_type", "use_open_launch_app_novel",
        "user_id", "share_timestamp", "source_channel", "entrance",
        "report_params", "ug_token",
        // 小红书
        "xhs_share_id", "xhs_source", "xhsshare", "shareRedId", "shareredid",
        "xsec_source", "app_platform", "app_version", "apptime",
        "share_from_user_hidden", "ignoreengage", "author_share",
        "share_channel", "share_redirect_url",
        // 通用分享 / 导航
        "navhide", "header", "footer", "hidebar",
        "shareto",
        "redirect", "redirect_url", "redir",
        "session_id", "sid", "token", "access_token",
        "country", "region",
    )

    // --- 白名单：绝对不能移除的参数（安全令牌、功能性参数）---
    private val KEEP_PARAMS = setOf(
        "xsec_token",    // 小红书安全令牌 — 移除后链接无法访问
        "type",          // 内容类型标识
        "id", "qid", "pid", "cid", "uid", "gid", "goods_id", "encrypt_did", "schemeParams", "track_id",
        "p", "page",     // 分页
        "q", "query", "search", "keyword", // 搜索
        "sort", "order", "dir",
        "tab", "cat", "category",
        "v", "version",
        "lang", "locale",
        "__biz", "mid", "idx", "sn", "chksm", // 微信公众号必需参数
    )

    // --- 激进模式域名：保留白名单参数，其余全部清除 ---
    private val AGGRESSIVE_DOMAINS = listOf(
        "item.taobao.com", "detail.tmall.com", "h5.m.taobao.com", "m.tb.cn",
        "item.jd.com", "item.m.jd.com",
        "h5.m.goofish.com", "goofish.com",
        "pinduoduo.com", "mobile.yangkeduo.com",
        "m.bilibili.com", "www.bilibili.com", "b23.tv",
        "www.xiaohongshu.com", "xhslink.com",
        "www.coolapk.com",
        "www.zhihu.com",
        "www.douyin.com", "v.douyin.com", "iesdouyin.com", "music.douyin.com",
        "mp.weixin.qq.com", "channels.weixin.qq.com",
        "novelquickapp.com",
        "kuaishou.com", "www.kuaishou.com",
        "tieba.baidu.com",
        "douban.com", "www.douban.com",
        "lofter.com",
        "toutiao.com", "www.toutiao.com",
        "music.163.com", "y.music.163.com",
        "y.qq.com",
        "store.steampowered.com",
        "tiktok.com", "www.tiktok.com",
        "youtube.com", "www.youtube.com", "youtu.be",
        "fanbox.cc",
        "amazon.co.jp", "amazon.com", "amazon.cn", "amazon.co.uk", "amazon.de",
        "mr.baidu.com", "ms.mbd.baidu.com",
    )

    // 排除全角标点：中文文案里 URL 后常紧跟 ，。） 等，不能算进链接
    val URL_REGEX = Regex(
        """(?:https?://|https?%3A%2F%2F|www\.|[a-z0-9][-a-z0-9]*\.[a-z]{2,}/)[^\s<>^`\[\]，。、；：！？（）【】《》「」『』“”‘’…]+""",
        RegexOption.IGNORE_CASE
    )

    private val ENCODED_PREFIX = Regex("^https?%3A", RegexOption.IGNORE_CASE)
    private val HOST_REGEX = Regex("""(?:https?://)?([^/\s?#]+)""", RegexOption.IGNORE_CASE)

    private fun extractDomain(url: String): String {
        var decoded = url
        if (ENCODED_PREFIX.containsMatchIn(url)) {
            try { decoded = URLDecoder.decode(url, "UTF-8") } catch (_: Exception) { }
        }
        return HOST_REGEX.find(decoded)?.groupValues?.get(1)?.lowercase() ?: ""
    }

    private fun isAggressiveDomain(url: String): Boolean {
        val domain = extractDomain(url)
        if (domain.isEmpty()) return false
        return AGGRESSIVE_DOMAINS.any { domain == it || domain.endsWith(".$it") }
    }

    // --- 域名专属黑名单：即使在全平台白名单中也移除 ---
    private fun getDomainParamBlacklist(domain: String): Set<String>? = when {
        Regex("bilibili\\.com$|b23\\.tv$").containsMatchIn(domain) -> setOf("mid")
        Regex("douyin\\.com$|iesdouyin\\.com$").containsMatchIn(domain) -> setOf("mid")
        Regex("mp\\.weixin\\.qq\\.com$").containsMatchIn(domain) -> setOf("version", "lang")
        Regex("novelquickapp\\.com$").containsMatchIn(domain) -> setOf("uid")
        else -> null
    }

    // --- 域名专属保留参数：替换全局白名单 ---
    private fun getDomainKeepParams(domain: String): Set<String>? = when {
        Regex("novelquickapp\\.com$").containsMatchIn(domain) ->
            setOf("encrypt_did", "zlink", "share_type", "schemeParams")
        Regex("douban\\.com$").containsMatchIn(domain) -> setOf("uri")
        Regex("(?:^|\\.)y\\.qq\\.com$").containsMatchIn(domain) ->
            // QQ音乐：只保留歌曲/专辑/歌单 ID，其余（含 type、appshare 等）全清
            setOf("songmid", "songid", "albummid", "albumid", "id", "mid")
        Regex("youtube\\.com$|youtu\\.be$").containsMatchIn(domain) ->
            KEEP_PARAMS + setOf("t", "list", "index") // 追加：时间戳、播放列表
        Regex("bilibili\\.com$|b23\\.tv$").containsMatchIn(domain) ->
            KEEP_PARAMS + setOf("t") // 追加：跳转时间点
        else -> null
    }

    // --- 豆瓣：清理嵌套 uri 参数中的追踪 ---
    private fun simplifyDoubanUri(url: String): String {
        val uriMatch = Regex("[?&]uri=([^&]+)").find(url) ?: return url
        var uriVal = uriMatch.groupValues[1]
        try { uriVal = URLDecoder.decode(uriVal, "UTF-8") } catch (_: Exception) { }

        val qIdx = uriVal.indexOf('?')
        if (qIdx < 0) return url

        val path = uriVal.substring(0, qIdx)
        val qs = uriVal.substring(qIdx + 1)
        val cleanParams = qs.split("&").filter { p ->
            val key = (if (p.contains('=')) p.substringBefore('=') else p).lowercase()
            key !in setOf("_spm_id", "_i", "dt_dapp", "dt_platform")
        }

        var cleanUri = path
        if (cleanParams.isNotEmpty()) cleanUri += "?" + cleanParams.joinToString("&")

        val uriStart = url.indexOf("uri=") + 4
        val uriEnd = url.indexOf('&', uriStart)
        val before = url.substring(0, uriStart)
        val after = if (uriEnd >= 0) url.substring(uriEnd) else ""
        return before + cleanUri + after
    }

    // --- Amazon：只保留 /dp/{ASIN} ---
    private fun simplifyAmazonUrl(url: String): String {
        val re = Regex("""^(?:(?:https?://)?[^/]+)/(?:.+/)?dp/([A-Z0-9]+).*$""", RegexOption.IGNORE_CASE)
        val m = re.find(url) ?: return url
        val asin = m.groupValues[1]
        val prefix = when {
            Regex("^www\\.", RegexOption.IGNORE_CASE).containsMatchIn(url) ->
                "https://" + Regex("^www\\.[^/]+", RegexOption.IGNORE_CASE).find(url)!!.value
            Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(url) ->
                Regex("^https?://[^/]+", RegexOption.IGNORE_CASE).find(url)!!.value
            else -> "https://" + Regex("^[^/]+").find(url)!!.value
        }
        return "$prefix/dp/$asin"
    }

    // --- Steam：移除占位标题 ---
    private fun simplifySteamUrl(url: String): String =
        url.replace(Regex("(store\\.steampowered\\.com/app/\\d+/)_/", RegexOption.IGNORE_CASE), "$1")

    // --- 网易云音乐：移动端 → PC 端格式 ---
    private fun simplifyMusicUrl(url: String): String =
        url.replace(Regex("^https?://(?:y\\.)?music\\.163\\.com/m/", RegexOption.IGNORE_CASE), "https://music.163.com/#/")

    // --- 红果短剧：清理 zlink 中的 schemeParams ---
    private fun simplifyHongguoZlink(url: String): String {
        val zlinkMatch = Regex("[?&]zlink=(.+?)(&|$)").find(url) ?: return url
        val zlinkVal = zlinkMatch.groupValues[1]
        val decodedZlink = try { URLDecoder.decode(zlinkVal, "UTF-8") } catch (_: Exception) { return url }

        val spMatch = Regex("[?&]schemeParams=([^&]+)(&|$)").find(decodedZlink) ?: return url
        var spVal = spMatch.groupValues[1]
        try {
            while (Regex("%[0-9A-Fa-f]{2}").containsMatchIn(spVal)) {
                val next = URLDecoder.decode(spVal, "UTF-8")
                if (next == spVal) break
                spVal = next
            }
        } catch (_: Exception) { return url }

        return try {
            val json = JSONObject(spVal)
            val clean = JSONObject()
            if (json.has("vid")) clean.put("vid", json.get("vid"))
            if (json.has("video_id")) clean.put("video_id", json.get("video_id"))

            val zlinkBase = decodedZlink.replace(Regex("\\?.*$"), "")
            val cleanZlink = "$zlinkBase?schemeParams=$clean"

            val zlinkStart = url.indexOf("zlink=") + 6
            val zlinkEnd = url.indexOf('&', zlinkStart)
            val before = url.substring(0, zlinkStart)
            val after = if (zlinkEnd >= 0) url.substring(zlinkEnd) else ""
            before + cleanZlink + after
        } catch (_: Exception) {
            url
        }
    }

    private val LONG_TOKEN = Regex("^[a-zA-Z0-9_+/=-]+$")
    private val HEX_TOKEN = Regex("^[a-fA-F0-9]{20,}$")
    private val UUID_TOKEN = Regex("^[a-fA-F0-9]{8}-?[a-fA-F0-9]{4}-?[a-fA-F0-9]{4}-?[a-fA-F0-9]{4}-?[a-fA-F0-9]{12}$")

    // --- 核心：清除文本中所有 URL 的追踪参数 ---
    fun strip(text: String): String = URL_REGEX.replace(text) { match ->
        val url = match.value
        var normalized = url
        if (!Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(url) &&
            !ENCODED_PREFIX.containsMatchIn(url)
        ) {
            normalized = "https://$url"
        }

        val hashIdx = url.indexOf('#')
        val hash = if (hashIdx >= 0) url.substring(hashIdx) else ""
        val baseUrl = if (hashIdx >= 0) url.substring(0, hashIdx) else url

        val qIdx = baseUrl.indexOf('?')
        if (qIdx < 0) return@replace url

        val base = baseUrl.substring(0, qIdx)
        val qs = baseUrl.substring(qIdx + 1)
        val params = qs.split("&")

        val aggressive = isAggressiveDomain(normalized)
        val urlDomain = extractDomain(normalized)
        val domainBlacklist = getDomainParamBlacklist(urlDomain)
        val domainKeep = getDomainKeepParams(urlDomain)

        val cleanParams = params.filter { param ->
            val eqIdx = param.indexOf('=')
            val key = (if (eqIdx >= 0) param.substring(0, eqIdx) else param).lowercase()
            val value = if (eqIdx >= 0) param.substring(eqIdx + 1) else ""

            // 域名黑名单优先
            if (domainBlacklist != null && key in domainBlacklist) return@filter false

            // 域名专属保留 或 全局白名单
            if (domainKeep != null) {
                if (key in domainKeep) return@filter true
            } else if (key in KEEP_PARAMS) {
                return@filter true
            }

            // 激进模式：其余全清
            if (aggressive) return@filter false

            // 追踪参数匹配
            if (key in TRACKING_PARAMS) return@filter false

            // 值看起来像追踪 token
            if (eqIdx >= 0) {
                if (value.length > 30 && LONG_TOKEN.matches(value)) return@filter false
                if (value.length > 20 && HEX_TOKEN.matches(value)) return@filter false
                if (UUID_TOKEN.matches(value)) return@filter false
            }

            true
        }

        var clean = base
        if (cleanParams.isNotEmpty()) clean += "?" + cleanParams.joinToString("&")
        clean += hash

        // 平台专属 URL 标准化（仅激进模式域名）
        if (aggressive) {
            clean = simplifyHongguoZlink(clean)
            clean = simplifyDoubanUri(clean)
            clean = simplifyMusicUrl(clean)
            clean = simplifySteamUrl(clean)
            clean = simplifyAmazonUrl(clean)
        }

        clean
    }
}
