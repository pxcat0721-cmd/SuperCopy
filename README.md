# SuperCopy - 剪贴板链接处理工具

> 一键清理追踪参数、展开短链、提取口令 — 覆盖 24+ 主流平台

## 特色功能

### 🔗 追踪参数清理
自动识别并移除 URL 中的追踪标记（UTM、分享追踪、广告参数等），**36 个域名**启用激进模式，保留内容 ID 和安全令牌。

**覆盖平台：** 淘宝/天猫、京东、拼多多、闲鱼、抖音、快手、B站、小红书、微信、豆瓣、贴吧、Lofter、知乎、今日头条、酷安、网易云音乐、红果短剧、Steam、Amazon、YouTube、FANBOX 等 24 个平台

### 🔀 短链展开
将短链接还原为完整长链，支持 HTTP 重定向 + JS 跳转解析。

**支持短链：** b23.tv、xhslink.cn、v.douyin.com、v.kuaishou.com、m.toutiao.com、m.tb.cn、weixin.qq.com/sph、novelquickapp.com/s、bit.ly、reurl.cc、s.team 等

### 📋 口令提取
从抖音/快手等分享口令中自动提取纯链接

### 🔄 BV ⇄ AV 互转
B 站视频番号双向转换（BV1xx411w7KC ↔ av170001）

### 其他功能
- **格式转换** — 网易云音乐 m→#、小红书 /discovery/item→/explore、Steam 去占位符、Amazon 路径精简
- **URL 解码** — 智能解码 %xx 序列，处理多重编码
- **文本过滤** — 去除中文、Emoji、数字、标点、特殊符号
- **空格压缩** — 清理多余空白

## 使用方法

### 启动服务器
```bash
python server.py
```

浏览器访问 `http://localhost:8080/`，或手机访问 `http://你的局域网IP:8080/`

### 使用示例

| 输入 | 输出 |
|------|------|
| `https://b23.tv/Ny9DN5I` | `https://www.bilibili.com/video/BV1Mtz1BuENq/` |
| `8.99 复制打开抖音... https://v.douyin.com/xxxxx/ :2pm` | `https://www.douyin.com/video/7664577502308650427` |
| `https://item.taobao.com/item.htm?id=123&xxc=taobaoSearch` | `https://item.taobao.com/item.htm?id=123` |
| `https://www.amazon.co.jp/产品名/dp/B007V6MQJY/ref=...` | `https://www.amazon.co.jp/dp/B007V6MQJY` |
| `https://y.music.163.com/m/song?id=3392668759&...` | `https://music.163.com/#/song?id=3392668759` |

## 技术栈

- 纯 HTML/CSS/JS 前端，零依赖
- Python 服务端处理短链展开、JS 跳转解析
- 多线程服务器支持并发请求

## 项目结构

```
SuperCopy/
├── index.html   # 前端界面 + 全部处理逻辑
├── server.py    # 本地服务器（短链展开 API）
└── README.md    # 本文件
```

---

> 本项目由 **Deepseek** 制作
