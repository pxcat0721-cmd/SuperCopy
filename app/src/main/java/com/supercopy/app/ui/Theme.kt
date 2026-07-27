package com.supercopy.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 暗色模式单一判定点（遵循 Mishka CLAUDE.md 规范）：
 * isDark 只在这里解析一次，各屏幕一律读 LocalAppDarkMode.current，
 * 不允许直接调用 isSystemInDarkTheme()。
 */
object ThemeConfig {
    fun resolveIsDark(systemDark: Boolean): Boolean = systemDark // 跟随系统
}

val LocalAppDarkMode = staticCompositionLocalOf { false }

@Composable
fun SuperCopyTheme(content: @Composable () -> Unit) {
    val isDark = ThemeConfig.resolveIsDark(isSystemInDarkTheme())
    CompositionLocalProvider(LocalAppDarkMode provides isDark) {
        MiuixTheme(
            colors = if (isDark) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }
}
