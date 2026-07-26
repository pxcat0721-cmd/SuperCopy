# SuperCopy - 剪贴板链接处理工具

> 一键清理追踪参数、展开短链、提取口令 — 覆盖 25+ 主流平台
> 网页版 + Android 原生 App 双端

## 特色功能

### 🔗 追踪参数清理
自动识别并移除 URL 中的追踪标记（UTM、分享追踪、广告参数等），准确率还挺高。

  **覆盖平台：** 淘宝/天猫、京东、拼多多、闲鱼、抖音、快手、B站、小红书、微信、豆瓣、贴吧、Lofter、知乎、今日头条、酷安、网易云音乐、QQ音乐、红果短剧、Steam、Amazon、YouTube、FANBOX、TapTap 等 25 个平台

### 🔀 短链展开
将短链接还原为完整长链，支持 HTTP 重定向 + JS 跳转解析。(基本上通杀)

**支持短链：** b23.tv,xhslink.cn,v.douyin.com,v.kuaishou.com,m.toutiao.com,m.tb.cn,weixin.qq.com/sph,novelquickapp.com,bit.ly,reurl.cc,s.team, 等

### 📋 口令提取
从抖音/快手/淘宝等分享口令中自动提取纯链接

### 🔄 BV ⇄ AV 互转
B站视频ID双向转换（BV1xx411w7KC ↔ av170001）

### 其他功能
- **格式转换** — 自动将部分平台转换为PC版链接
- **URL 解码** — 智能解码 %xx 序列，处理多重编码
- **文本过滤** — 去除中文、Emoji、数字、标点、特殊符号（URL 自动保护不受影响）
- **空格压缩** — 清理多余空白

## 网页版

### 启动服务器
```bash
python server.py
```

浏览器访问 `http://localhost:8080/`

### 使用示例

| 输入 | 输出 |
|------|------|
| `https://b23.tv/S14iBmv` | `https://www.bilibili.com/video/av2/?p=1` |
| `8.99 复制打开抖音... https://v.douyin.com/xxxxx/ :2pm` | `https://www.douyin.com/video/xxxxx` |
| `https://item.taobao.com/item.htm?id=123&xxc=taobaoSearch` | `https://item.taobao.com/item.htm?id=123` |
| `https://i2.y.qq.com/...playsong/index.html?ADTAG=...&songmid=002KYwxn39DSSf&type=0` | `https://i2.y.qq.com/...playsong/index.html?songmid=002KYwxn39DSSf` |
| `https://y.music.163.com/m/song?id=3392668759&...` | `https://music.163.com/#/song?id=3392668759` |

## Android App

Kotlin + Jetpack Compose + [miuix](https://github.com/compose-miuix-ui/miuix) 原生实现，澎湃OS 风格：

- 网页版全部处理能力（短链展开直接在 App 内完成，无需服务器）
- **系统分享接收** — 在任意 App 里分享文本给 SuperCopy 即自动处理
- **文本选择菜单** — 长按选中文本，菜单里直接选 SuperCopy
- **打开自动读剪贴板**，处理完一键复制回去
- 澎湃OS 流体光背景关于页、毛玻璃顶栏、可预测返回
- 多语言：简体中文 / 繁體中文 / English（支持 Android 13+ 按应用设置语言）

### 构建
```bash
cd android
gradle assembleRelease   # 需要 JDK 17+ / Android SDK 37
```

## 项目结构

```
SuperCopy/
├── index.html          # 网页版界面 + 处理逻辑
├── tracking-rules.js   # 追踪参数清理规则引擎
├── server.py           # 本地服务器（短链展开 API）
├── CLAUDE.md           # UI 设计规范（miuix / Mishka 风格）
└── android/            # Android App（Kotlin + Compose + miuix）
    └── app/src/main/java/com/supercopy/app/
        ├── core/       # 规则引擎移植：TrackingRules / UrlExpander / Processor / BvAv
        └── ui/         # MainScreen / AboutScreen / 澎湃OS BgEffect / 毛玻璃组件
```

---

> 本项目由 **Claude Fable 5** 协助制作
