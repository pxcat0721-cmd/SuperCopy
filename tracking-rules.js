/**
 * SuperCopy — 去除追踪标记规则
 * ================================
 * 此文件定义所有与 URL 追踪参数清理相关的规则和函数。
 * 主工具通过调用 `TrackingRules.strip(text)` 来清除文本中所有 URL 的追踪参数。
 *
 * 添加新规则：
 *   1. 通用追踪参数 → 追加到 TRACKING_PARAMS 数组
 *   2. 新平台激进模式 → 追加域名到 AGGRESSIVE_DOMAINS 数组
 *   3. 保留参数 → 追加到 KEEP_PARAMS
 *   4. 域名特殊规则 → 修改 getDomainParamBlacklist / getDomainKeepParams
 *   5. URL 格式标准化 → 添加 simplify* 函数并在 stripTrackingParams 末尾调用
 */

// --- 追踪参数黑名单：这些 query 参数会被移除 ---
const TRACKING_PARAMS = [
  // CoolAPK & 通用中文平台
  's', 'spm', 'spmid', 'from', 'source', 'track', 'trackid',
  'share_id', 'shareid', 'shared_from', 'share_from', 'share_medium',
  'share_plat', 'share_session_id', 'share_source', 'share_tag',
  'from_spmid', 'spmid_from', 'scene', 'channel', 'refer', 'ref',
  // 通用分析
  'utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content',
  'utm_id', 'utm_source_platform', 'utm_creative', 'utm_creative_format',
  // Facebook / Instagram
  'fbclid', 'fb_action_ids', 'fb_action_types', 'fb_source', 'fb_ref',
  'igshid', 'igsh',
  // Google
  'gclid', 'gclsrc', 'dclid', 'gbraid', 'wbraid',
  'oq', 'gs_lcrp', 'sourceid', 'ie', 'ved', 'ei', 'usg', 'sig2',
  // Twitter / X
  'twclid', 't', 's09', 'ref_src', 'ref_url',
  // 其他
  'mc_cid', 'mc_eid', 'mc_tc',
  '_ga', '_gl', '_hsenc', '_hsmi', '_ke',
  'ck_subscriber_id', 'oly_anon_id', 'oly_enc_id',
  'otc', 'rss', 'sc_cid', 'si',
  // 百度
  'rs', 'ruk', 'u',
  // 淘宝 / 天猫 / 京东 / 中国电商
  'ali_trackid', 'ali_refid', 'e', 'skuid', 'activityid',
  'app', 'cpp', 'price', 'bxsign', 'wxsign',
  'shareuniqueid', 'share_crt_v', 'shareurl', 'short_name',
  'sourcetype', 'sp_tk', 'tk', 'tbsocialpopkey',
  'un', 'un_site', 'ut_sk', 'suid',
  'abbucket', 'ns', 'utparam', 'pricetid', 'mi_id', 'xxc',
  // 拼多多
  'refer_page_name', 'refer_page_id', 'refer_page_el_sn',
  'page_from', 'feeds_pack_id', 'duoduo_type',
  'pxq_secret_key', '_oak_share_snapshot_num', '_oak_share_time',
  'share_oak_rcto', '_oak_share_ticket', 'share_uin',
  'refer_share_id', 'refer_share_uin', 'refer_share_channel',
  'refer_share_form', '_x_share_id', 'refer_page_sn', 'uin',
  // 京东
  'jxsid', 'jdsn', 'mpja', 'mpjb', 'mpjc',
  'jd_pop', 'jd_track', 'ad_od', 'utm_user',
  // Bilibili
  'buvid', 'buvid3', 'buvid4', 'buvid5',
  'buvid6', 'buvid7', 'buvid8',
  'unique_k', 'timestamp', 'up_id',
  'plat_id', 'platform', 'plat', 'is_story_h5', 'story_h5',
  'share_style', 'share_redirect_delay',
  // 微信
  'wx_scene', 'wx_share_id',
  // 知乎
  'zhid', 'utm_oi',
  // 抖音 / TikTok
  'enter_from', 'enter_method', 'previous_page',
  'sec_uid', 'did', 'iid', 'device_id',
  // 微信公众号
  'xtrack', 'req_id', 'subscene', 'sessionid', 'clicktime',
  'enterid', 'finder_biz_enter_id', 'ranksessionid',
  'jumppath', 'jumppathdepth', 'ascene',
  'fasttmpl_type', 'fasttmpl_fullversion', 'fasttmpl_flag',
  'realreporttime', 'devicetype', 'nettype', 'session_us',
  'exportkey', 'pass_ticket', 'wx_header', 'flutter_pos',
  // 豆瓣
  '_spm_id', '_i', 'dt_dapp', 'dt_platform',
  // 贴吧
  'share', 'fr', 'see_lz', 'sfc', 'client_type', 'client_version',
  'st', 'is_video', 'unique',
  // 快手
  'cc', 'followrefer', 'sharemethod', 'docid', 'kpn', 'subbiz',
  'photoid', 'sharetoken', 'shareresourcetype', 'userid',
  'sharetype', 'et', 'sharemode', 'efid', 'originshareid',
  'apptype', 'shareobjectid', 'shareurlopened', 'location',
  // 红果短剧
  'zlink', 'gd_label', 'share_type', 'use_open_launch_app_novel',
  'user_id', 'share_timestamp', 'source_channel', 'entrance',
  'report_params', 'ug_token',
  // 小红书
  'xhs_share_id', 'xhs_source', 'xhsshare', 'shareRedId', 'shareredid',
  'xsec_source', 'app_platform', 'app_version', 'apptime',
  'share_from_user_hidden', 'ignoreengage', 'author_share',
  'share_channel', 'share_redirect_url',
  // 通用分享 / 导航
  'navhide', 'header', 'footer', 'hidebar',
  'shareto',
  'redirect', 'redirect_url', 'redir',
  'session_id', 'sid', 'token', 'access_token',
  'country', 'region',
];

// --- 白名单：绝对不能移除的参数（安全令牌、功能性参数）---
const KEEP_PARAMS = new Set([
  'xsec_token',    // 小红书安全令牌 — 移除后链接无法访问
  'type',          // 内容类型标识
  'id', 'qid', 'pid', 'cid', 'uid', 'gid', 'goods_id', 'encrypt_did', 'schemeParams', 'track_id', // 内容/实体 ID
  'p', 'page',     // 分页
  'q', 'query', 'search', 'keyword', // 搜索
  'sort', 'order', 'dir', // 排序/筛选
  'tab', 'cat', 'category', // 分类导航
  'v', 'version', // API 版本
  'lang', 'locale', // 语言偏好（非追踪）
  '__biz', 'mid', 'idx', 'sn', 'chksm', // 微信公众号必需参数
]);

// --- 激进模式域名：这些域名保留白名单参数，其余全部清除 ---
const AGGRESSIVE_DOMAINS = [
  'item.taobao.com', 'detail.tmall.com', 'h5.m.taobao.com', 'm.tb.cn',
  'item.jd.com', 'item.m.jd.com',
  'h5.m.goofish.com', 'goofish.com',
  'pinduoduo.com', 'mobile.yangkeduo.com',
  'm.bilibili.com', 'www.bilibili.com', 'b23.tv',
  'www.xiaohongshu.com', 'xhslink.com',
  'www.coolapk.com',
  'www.zhihu.com',
  'www.douyin.com', 'v.douyin.com', 'iesdouyin.com', 'music.douyin.com',
  'mp.weixin.qq.com', 'channels.weixin.qq.com',
  'novelquickapp.com',
  'kuaishou.com', 'www.kuaishou.com',
  'tieba.baidu.com',
  'douban.com', 'www.douban.com',
  'lofter.com',
  'toutiao.com', 'www.toutiao.com',
  'music.163.com', 'y.music.163.com',
  'y.qq.com',
  'store.steampowered.com',
  'tiktok.com', 'www.tiktok.com',
  'youtube.com', 'www.youtube.com', 'youtu.be',
  'fanbox.cc',
  'amazon.co.jp', 'amazon.com', 'amazon.cn', 'amazon.co.uk', 'amazon.de',
  'mr.baidu.com', 'ms.mbd.baidu.com',
];

// --- 辅助函数：从 URL 中提取域名 ---
function extractDomain(url) {
  let decoded = url;
  if (/^https?%3A/i.test(url)) {
    try { decoded = decodeURIComponent(url); } catch { /* keep original */ }
  }
  const host = decoded.match(/(?:https?:\/\/)?([^\/\s?#]+)/i);
  return host ? host[1].toLowerCase() : '';
}

// --- 辅助函数：判断是否为激进模式域名 ---
function isAggressiveDomain(url) {
  try {
    const domain = extractDomain(url);
    if (!domain) return false;
    return AGGRESSIVE_DOMAINS.some(d => domain === d || domain.endsWith('.' + d));
  } catch { return false; }
}

// --- 豆瓣：清理嵌套 uri 参数中的追踪 ---
function simplifyDoubanUri(url) {
  const uriMatch = url.match(/[?&]uri=([^&]+)/);
  if (!uriMatch) return url;

  let uriVal = uriMatch[1];
  try { uriVal = decodeURIComponent(uriVal); } catch { /* keep as-is */ }

  const qIdx = uriVal.indexOf('?');
  if (qIdx < 0) return url;

  const path = uriVal.slice(0, qIdx);
  const qs = uriVal.slice(qIdx + 1);
  const params = qs.split('&');
  const cleanParams = params.filter(p => {
    const key = (p.indexOf('=') >= 0 ? p.slice(0, p.indexOf('=')) : p).toLowerCase();
    if (['_spm_id', '_i', 'dt_dapp', 'dt_platform'].includes(key)) return false;
    return true;
  });

  let cleanUri = path;
  if (cleanParams.length > 0) cleanUri += '?' + cleanParams.join('&');

  const uriStart = url.indexOf('uri=') + 4;
  const uriEnd = url.indexOf('&', uriStart);
  const before = url.slice(0, uriStart);
  const after = uriEnd >= 0 ? url.slice(uriEnd) : '';
  return before + cleanUri + after;
}

// --- Amazon：只保留 /dp/{ASIN} ---
function simplifyAmazonUrl(url) {
  return url.replace(
    /^(?:(?:https?:\/\/)?[^\/]+)\/(?:.+\/)?dp\/([A-Z0-9]+).*$/i,
    (m, asin) => {
      const prefix = /^www\./i.test(url) ? 'https://' + url.match(/^www\.[^\/]+/i)[0] :
                     /^https?:\/\//i.test(url) ? url.match(/^https?:\/\/[^\/]+/i)[0] :
                     'https://' + url.match(/^[^\/]+/i)[0];
      return prefix + '/dp/' + asin;
    }
  );
}

// --- Steam：移除占位标题 ---
function simplifySteamUrl(url) {
  return url.replace(/(store\.steampowered\.com\/app\/\d+\/)_\//gi, '$1');
}

// --- 网易云音乐：移动端 → PC 端格式 ---
function simplifyMusicUrl(url) {
  return url.replace(
    /^https?:\/\/(?:y\.)?music\.163\.com\/m\//i,
    'https://music.163.com/#/'
  );
}

// --- 域名专属黑名单：即使在全平台白名单中也移除 ---
function getDomainParamBlacklist(domain) {
  if (/bilibili\.com$|b23\.tv$/.test(domain)) return new Set(['mid']);
  if (/douyin\.com$|iesdouyin\.com$/.test(domain)) return new Set(['mid']);
  if (/mp\.weixin\.qq\.com$/.test(domain)) return new Set(['version', 'lang']);
  if (/novelquickapp\.com$/.test(domain)) return new Set(['uid']);
  return null;
}

// --- 域名专属保留参数：替换全局 KEEP_PARAMS ---
function getDomainKeepParams(domain) {
  if (/novelquickapp\.com$/.test(domain)) {
    return new Set(['encrypt_did', 'zlink', 'share_type', 'schemeParams']);
  }
  if (/douban\.com$/.test(domain)) {
    return new Set(['uri']);
  }
  if (/(?:^|\.)y\.qq\.com$/.test(domain)) {
    // QQ音乐：只保留歌曲/专辑/歌单 ID，其余（含 type、appshare 等）全清
    return new Set(['songmid', 'songid', 'albummid', 'albumid', 'id', 'mid']);
  }
  if (/youtube\.com$|youtu\.be$/.test(domain)) {
    return new Set([...KEEP_PARAMS, 't', 'list', 'index']); // 追加：时间戳、播放列表
  }
  if (/bilibili\.com$|b23\.tv$/.test(domain)) {
    return new Set([...KEEP_PARAMS, 't']); // 追加：跳转时间点
  }
  return null;
}

// --- 红果短剧：清理 zlink 中的 schemeParams ---
function simplifyHongguoZlink(url) {
  const zlinkMatch = url.match(/[?&]zlink=(.+?)(&|$)/);
  if (!zlinkMatch) return url;

  const zlinkVal = zlinkMatch[1];
  let decodedZlink;
  try { decodedZlink = decodeURIComponent(zlinkVal); } catch { return url; }

  const spMatch = decodedZlink.match(/[?&]schemeParams=([^&]+)(&|$)/);
  if (!spMatch) return url;

  let spVal = spMatch[1];
  try {
    while (/%[0-9A-Fa-f]{2}/.test(spVal)) {
      spVal = decodeURIComponent(spVal);
    }
  } catch { return url; }

  try {
    const json = JSON.parse(spVal);
    const clean = {};
    if (json.vid) clean.vid = json.vid;
    if (json.video_id) clean.video_id = json.video_id;
    const cleanJson = JSON.stringify(clean);

    const zlinkBase = decodedZlink.replace(/\?.*$/, '');
    const cleanZlink = zlinkBase + '?schemeParams=' + cleanJson;

    const zlinkStart = url.indexOf('zlink=') + 6;
    const zlinkEnd = url.indexOf('&', zlinkStart);
    const before = url.substring(0, zlinkStart);
    const after = zlinkEnd >= 0 ? url.substring(zlinkEnd) : '';
    return before + cleanZlink + after;
  } catch {
    return url;
  }
}

// --- 核心函数：清除文本中所有 URL 的追踪参数 ---
function stripTrackingParams(text) {
  // 排除全角标点：中文文案里 URL 后常紧跟 ，。） 等，不能算进链接
  const urlRegex = /(?:https?:\/\/|https?%3A%2F%2F|www\.|[a-z0-9][-a-z0-9]*\.[a-z]{2,}\/)[^\s<>^`\[\]，。、；：！？（）【】《》「」『』“”‘’…]+/gi;

  return text.replace(urlRegex, (url) => {
    let normalized = url;
    if (!/^https?:\/\//i.test(url) && !/^https?%3A/i.test(url)) {
      normalized = 'https://' + url;
    }

    const hashIdx = url.indexOf('#');
    const hash = hashIdx >= 0 ? url.slice(hashIdx) : '';
    const baseUrl = hashIdx >= 0 ? url.slice(0, hashIdx) : url;

    const qIdx = baseUrl.indexOf('?');
    if (qIdx < 0) return url;

    const base = baseUrl.slice(0, qIdx);
    const qs = baseUrl.slice(qIdx + 1);
    const params = qs.split('&');

    const aggressive = isAggressiveDomain(normalized);
    const urlDomain = extractDomain(normalized);
    const domainBlacklist = getDomainParamBlacklist(urlDomain);

    const cleanParams = params.filter(param => {
      const eqIdx = param.indexOf('=');
      const key = (eqIdx >= 0 ? param.slice(0, eqIdx) : param).toLowerCase();
      const val = eqIdx >= 0 ? param.slice(eqIdx + 1) : '';

      // 域名黑名单优先
      if (domainBlacklist && domainBlacklist.has(key)) return false;

      // 域名专属保留 或 全局白名单
      const domainKeep = getDomainKeepParams(urlDomain);
      if (domainKeep) {
        if (domainKeep.has(key)) return true;
      } else if (KEEP_PARAMS.has(key)) {
        return true;
      }

      // 激进模式：其余全清
      if (aggressive) return false;

      // 追踪参数匹配
      if (TRACKING_PARAMS.includes(key)) return false;

      // 值看起来像追踪 token
      if (eqIdx >= 0) {
        if (val.length > 30 && /^[a-zA-Z0-9_+/=-]+$/.test(val)) return false;
        if (val.length > 20 && /^[a-fA-F0-9]{20,}$/.test(val)) return false;
        if (/^[a-fA-F0-9]{8}-?[a-fA-F0-9]{4}-?[a-fA-F0-9]{4}-?[a-fA-F0-9]{4}-?[a-fA-F0-9]{12}$/.test(val)) return false;
      }

      return true;
    });

    let clean = base;
    if (cleanParams.length > 0) {
      clean += '?' + cleanParams.join('&');
    }
    clean += hash;

    // 平台专属 URL 标准化（仅激进模式域名）
    if (aggressive) {
      clean = simplifyHongguoZlink(clean);
      clean = simplifyDoubanUri(clean);
      clean = simplifyMusicUrl(clean);
      clean = simplifySteamUrl(clean);
      clean = simplifyAmazonUrl(clean);
    }

    return clean;
  });
}

// --- 公共 API ---
const TrackingRules = {
  /**
   * 清除文本中所有 URL 的追踪参数
   * @param {string} text - 包含 URL 的文本
   * @returns {string} - 清除追踪参数后的文本
   */
  strip: function(text) {
    return stripTrackingParams(text);
  },

  // 暴露以下数据以便外部检查/调试
  getTrackingParams: function() { return [...TRACKING_PARAMS]; },
  getKeepParams: function() { return new Set(KEEP_PARAMS); },
  getAggressiveDomains: function() { return [...AGGRESSIVE_DOMAINS]; },
};
