package com.supercopy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.supercopy.app.MainViewModel
import com.supercopy.app.R
import com.supercopy.app.core.Filter
import com.supercopy.app.ui.component.blur.BlurredBar
import com.supercopy.app.ui.component.blur.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val LINK_FILTERS = listOf(
    Filter.EXTRACT_URL, Filter.EXPAND, Filter.TRACKING, Filter.BVAV, Filter.URL_DECODE,
)
private val TEXT_FILTERS = listOf(
    Filter.CHINESE, Filter.EMOJI, Filter.NUMBERS, Filter.PUNCTUATION,
    Filter.WHITESPACE, Filter.SPECIAL, Filter.WEIBO,
)

/** 开关标题走字符串资源（CLAUDE.md 第 44 条）；core 层 Filter.label 仅用于处理结果预览 */
@Composable
private fun Filter.title(): String = stringResource(
    when (this) {
        Filter.EXTRACT_URL -> R.string.filter_extract_url
        Filter.EXPAND -> R.string.filter_expand
        Filter.TRACKING -> R.string.filter_tracking
        Filter.BVAV -> R.string.filter_bvav
        Filter.URL_DECODE -> R.string.filter_urldecode
        Filter.CHINESE -> R.string.filter_chinese
        Filter.EMOJI -> R.string.filter_emoji
        Filter.NUMBERS -> R.string.filter_numbers
        Filter.PUNCTUATION -> R.string.filter_punctuation
        Filter.WHITESPACE -> R.string.filter_whitespace
        Filter.SPECIAL -> R.string.filter_special
        Filter.WEIBO -> R.string.filter_weibo
    }
)

/**
 * 主界面 — 遵循项目 CLAUDE.md：
 * TopAppBar(scrollBehavior) + LazyColumn(scrollEndHaptic/overScrollVertical/nestedScroll)、
 * contentPadding 仅 top、TextField 表单不包 Card、Card 统一 horizontal 12 + bottom 12、
 * 末尾 navigationBarsPadding Spacer、Flow 用 collectAsStateWithLifecycle。
 */
@Composable
fun MainScreen(
    vm: MainViewModel,
    onCopy: (String) -> Unit,
    onPaste: () -> String?,
    onAbout: () -> Unit,
) {
    val input by vm.input.collectAsStateWithLifecycle()
    val output by vm.output.collectAsStateWithLifecycle()
    val removedInfo by vm.removedInfo.collectAsStateWithLifecycle()
    val processing by vm.processing.collectAsStateWithLifecycle()
    val activeFilters by vm.activeFilters.collectAsStateWithLifecycle()

    val scrollBehavior = MiuixScrollBehavior()

    // CLAUDE.md 顶栏毛玻璃模式：backdrop 抓取内容区，BlurredBar 包裹 TopAppBar
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                TopAppBar(
                    title = stringResource(R.string.app_name),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    actions = {
                    // CLAUDE.md：操作 IconButton = 35.dp + secondaryContainer 背景；
                    // 位置由 TopAppBar 的 actionIconPadding(16.dp) 控制，不额外加 padding
                    IconButton(
                        onClick = onAbout,
                        backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                        minHeight = 35.dp,
                        minWidth = 35.dp,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Info,
                            contentDescription = stringResource(R.string.main_about),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                    },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                // 供顶栏 BlurredBar 的 textureBlur 抓取内容
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
            contentPadding = PaddingValues(top = padding.calculateTopPadding()),
        ) {
            item { SmallTitle(text = stringResource(R.string.main_input_title)) }
            item {
                TextField(
                    value = input,
                    onValueChange = { vm.setInput(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .heightIn(min = 120.dp),
                    label = stringResource(R.string.main_input_label),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onPaste()?.let { vm.setInput(it) } },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.main_read_clipboard)) }
                    Button(
                        onClick = { vm.clear() },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.common_clear)) }
                }
            }

            item {
                SmallTitle(
                    text = stringResource(
                        if (processing) R.string.main_processing else R.string.main_result_title
                    )
                )
            }
            item {
                TextField(
                    value = output,
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .heightIn(min = 120.dp),
                    label = stringResource(R.string.main_output_label),
                )
            }
            if (removedInfo.isNotEmpty()) {
                item {
                    Text(
                        text = removedInfo,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { if (output.isNotEmpty()) onCopy(output) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.main_copy_result)) }
                    Button(
                        onClick = { vm.useOutputAsInput() },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.main_use_as_input)) }
                }
            }

            item { SmallTitle(text = stringResource(R.string.main_link_section)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    Column {
                        LINK_FILTERS.forEach { filter ->
                            SwitchPreference(
                                title = filter.title(),
                                checked = filter in activeFilters,
                                onCheckedChange = { vm.toggleFilter(filter) },
                            )
                        }
                    }
                }
            }

            item { SmallTitle(text = stringResource(R.string.main_text_section)) }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    Column {
                        TEXT_FILTERS.forEach { filter ->
                            SwitchPreference(
                                title = filter.title(),
                                checked = filter in activeFilters,
                                onCheckedChange = { vm.toggleFilter(filter) },
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp).navigationBarsPadding()) }
        }
    }
}
