package com.supercopy.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.supercopy.app.ui.AboutScreen
import com.supercopy.app.ui.MainScreen
import com.supercopy.app.ui.SuperCopyTheme

/** 导航路由（单 Activity 分层导航，遵循 Mishka 规范的 sealed 路由） */
sealed interface Route {
    data object Main : Route
    data object About : Route
}

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var clipboardConsumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = pInfo.versionName ?: "1.0"
        @Suppress("DEPRECATION")
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode else pInfo.versionCode.toLong()

        setContent {
            SuperCopyTheme {
                // miuix NavDisplay：自带 miuix 风格页面过渡 + 可预测返回动画
                val backStack = remember { mutableStateListOf<Route>(Route.Main) }
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = { route ->
                        when (route) {
                            Route.Main -> NavEntry(route) {
                                MainScreen(
                                    vm = vm,
                                    onCopy = { text -> copyToClipboard(text) },
                                    onPaste = { readClipboard() },
                                    onAbout = { backStack.add(Route.About) },
                                )
                            }
                            Route.About -> NavEntry(route) {
                                AboutScreen(
                                    versionName = versionName,
                                    versionCode = versionCode,
                                    onBack = { backStack.removeLastOrNull() },
                                    onOpenUrl = { url ->
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    },
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /** 系统分享接收：其他 App 分享的文本直接进入处理管线 */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!shared.isNullOrBlank()) {
                clipboardConsumed = true // 分享进来的内容优先，不再读剪贴板
                vm.setInput(shared)
            }
        }
    }

    /**
     * 打开时自动读剪贴板。
     * Android 10+ 只允许获得焦点的 App 读剪贴板，所以放在
     * onWindowFocusChanged 而不是 onResume。
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !clipboardConsumed && vm.input.value.isEmpty()) {
            readClipboard()?.let {
                clipboardConsumed = true
                vm.setInput(it)
            }
        }
    }

    private fun readClipboard(): String? {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()
        return text?.takeIf { it.isNotBlank() }
    }

    private fun copyToClipboard(text: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("SuperCopy", text))
        // Android 13+ 系统自带复制提示，低版本自己弹
        if (android.os.Build.VERSION.SDK_INT < 33) {
            Toast.makeText(this, getString(R.string.main_copied), Toast.LENGTH_SHORT).show()
        }
    }
}
