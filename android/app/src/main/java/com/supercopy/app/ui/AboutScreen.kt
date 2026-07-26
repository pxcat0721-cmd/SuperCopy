package com.supercopy.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supercopy.app.R
import com.supercopy.app.ui.component.blur.BlurredBar
import com.supercopy.app.ui.component.blur.ColorBlendToken
import com.supercopy.app.ui.component.blur.rememberBlurBackdrop
import com.supercopy.app.ui.component.blur.rememberBlurEnabled
import com.supercopy.app.ui.component.effect.BgEffectBackground
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 关于页 — 移植自用户参考实现：澎湃OS BgEffect 流体背景 + hero 视差 +
 * 卡片 textureBlur 毛玻璃 + BlurredBar 顶栏 + 标题渐显。
 * 按 CLAUDE.md 调整：LocalAppDarkMode、squircleClip 图标、heightIn(min = 视口高)。
 */
@Composable
fun AboutScreen(
    versionName: String,
    versionCode: Long,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }

    // 背景是否可见：滚到底后 BgEffect 动画暂停，不再 60fps 空转
    val heroVisible by remember { derivedStateOf { scrollProgress < 1f } }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null && scrollProgress == 1f
    val barColor = if (blurActive) Color.Transparent else if (scrollProgress == 1f) colorScheme.surface else Color.Transparent

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                SmallTopAppBar(
                    title = stringResource(R.string.about_title),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    titleColor = colorScheme.onSurface.copy(alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)),
                    defaultWindowInsetsPadding = false,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.systemBars.union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    ),
                    navigationIcon = {
                        val layoutDirection = LocalLayoutDirection.current
                        // 位置由 TopAppBar 的 navigationIconPadding(16.dp) 控制，不额外加 padding
                        IconButton(
                            onClick = onBack,
                            minHeight = 35.dp,
                            minWidth = 35.dp,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.common_back),
                                tint = colorScheme.onSurface,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                                },
                            )
                        }
                    },
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            AboutContent(
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                lazyListState = lazyListState,
                // 以 lambda 传进 draw 阶段读取：hero 滚动不再逐帧重组整个内容区
                scrollProgress = { scrollProgress },
                heroVisible = heroVisible,
                versionName = versionName,
                versionCode = versionCode,
                onOpenUrl = onOpenUrl,
            )
        }
    }
}

@Composable
private fun AboutContent(
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    lazyListState: LazyListState,
    scrollProgress: () -> Float,
    heroVisible: Boolean,
    versionName: String,
    versionCode: Long,
    onOpenUrl: (String) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val backdrop = rememberLayerBackdrop()

    val isDark = LocalAppDarkMode.current
    val blurEnabled by rememberBlurEnabled()
    val effectBackground = remember(blurEnabled) { isRuntimeShaderSupported() && blurEnabled }

    val cardBlendColors = remember(isDark) {
        if (isDark) ColorBlendToken.Overlay_Thin_Light else ColorBlendToken.Pured_Regular_Light
    }
    val logoBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1.toInt()), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af500.toInt()), BlurBlendMode.Lab),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a.toInt()), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f.toInt()), BlurBlendMode.LinearLight),
                BlendColorEntry(Color(0xff1af200.toInt()), BlurBlendMode.Lab),
            )
        }
    }

    var logoHeightDp by remember { mutableStateOf(300.dp) }

    val scrollPadding = PaddingValues(
        top = innerPadding.calculateTopPadding(),
        start = innerPadding.calculateStartPadding(layoutDirection),
        end = innerPadding.calculateEndPadding(layoutDirection),
    )
    val logoPadding = PaddingValues(
        top = innerPadding.calculateTopPadding() + 40.dp,
        start = innerPadding.calculateStartPadding(layoutDirection),
        end = innerPadding.calculateEndPadding(layoutDirection),
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight

        BgEffectBackground(
            dynamicBackground = effectBackground && heroVisible,
            modifier = Modifier.fillMaxSize(),
            bgModifier = Modifier.layerBackdrop(backdrop),
            isFullSize = true,
            effectBackground = effectBackground,
            alpha = { 1f - scrollProgress() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = logoPadding.calculateTopPadding() + 52.dp,
                        start = logoPadding.calculateStartPadding(layoutDirection),
                        end = logoPadding.calculateEndPadding(layoutDirection),
                    )
                    .onSizeChanged { size -> with(density) { logoHeightDp = size.height.toDp() } },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            val p = ((scrollProgress() - 0.35f) / 0.15f).coerceIn(0f, 1f)
                            alpha = 1 - p
                            scaleX = 1 - (p * 0.05f)
                            scaleY = 1 - (p * 0.05f)
                        }
                        // CLAUDE.md 第 4 条：图片裁剪用 squircleClip
                        .squircleClip(24.dp)
                        .background(colorResource(R.color.ic_launcher_background)),
                ) {
                    Image(
                        modifier = Modifier.size(132.dp),
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "icon",
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 5.dp)
                        .graphicsLayer {
                            val p = ((scrollProgress() - 0.20f) / 0.15f).coerceIn(0f, 1f)
                            alpha = 1 - p
                            scaleX = 1 - (p * 0.05f)
                            scaleY = 1 - (p * 0.05f)
                        }
                        .then(
                            if (blurEnabled) {
                                Modifier.textureBlur(
                                    backdrop = backdrop,
                                    shape = RoundedCornerShape(16.dp),
                                    blurRadius = 150f,
                                    colors = BlurColors(blendColors = logoBlend),
                                    contentBlendMode = BlendMode.DstIn,
                                    enabled = true,
                                )
                            } else Modifier
                        ),
                    text = stringResource(R.string.app_name),
                    color = colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp,
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = ((scrollProgress() - 0.05f) / 0.15f).coerceIn(0f, 1f)
                            alpha = 1 - p
                            scaleX = 1 - (p * 0.05f)
                            scaleY = 1 - (p * 0.05f)
                        },
                    color = colorScheme.onSurfaceVariantSummary,
                    text = "v$versionName ($versionCode)",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = scrollPadding.calculateTopPadding(),
                    start = scrollPadding.calculateStartPadding(layoutDirection),
                    end = scrollPadding.calculateEndPadding(layoutDirection),
                ),
            ) {
                item(key = "logoSpacer") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(logoHeightDp + 52.dp + logoPadding.calculateTopPadding() - scrollPadding.calculateTopPadding() + 126.dp),
                        contentAlignment = Alignment.TopCenter,
                        content = { },
                    )
                }

                item(key = "about") {
                    // CLAUDE.md 第 37 条：heightIn(min = 视口高) 而非 fillParentMaxHeight
                    Column(modifier = Modifier.heightIn(min = viewportHeight).padding(bottom = 12.dp)) {
                        SmallTitle(text = stringResource(R.string.about_info))
                        BlurCard(blurEnabled, backdrop, cardBlendColors) {
                            BasicComponent(title = stringResource(R.string.about_app_version), summary = versionName)
                            BasicComponent(title = stringResource(R.string.about_build_version), summary = "$versionCode")
                        }

                        SmallTitle(text = stringResource(R.string.about_developer))
                        BlurCard(blurEnabled, backdrop, cardBlendColors) {
                            ArrowPreference(
                                title = stringResource(R.string.about_github),
                                summary = "github.com/pxcat0721-cmd",
                                onClick = { onOpenUrl("https://github.com/pxcat0721-cmd") },
                            )
                            ArrowPreference(
                                title = stringResource(R.string.about_project),
                                summary = "github.com/pxcat0721-cmd/SuperCopy",
                                onClick = { onOpenUrl("https://github.com/pxcat0721-cmd/SuperCopy") },
                            )
                        }

                        SmallTitle(text = stringResource(R.string.about_licenses))
                        BlurCard(blurEnabled, backdrop, cardBlendColors) {
                            ArrowPreference(
                                title = "miuix",
                                summary = "github.com/compose-miuix-ui/miuix",
                                onClick = { onOpenUrl("https://github.com/compose-miuix-ui/miuix") },
                            )
                            ArrowPreference(
                                title = "Compose Multiplatform",
                                summary = "github.com/JetBrains/compose-multiplatform",
                                onClick = { onOpenUrl("https://github.com/JetBrains/compose-multiplatform") },
                            )
                        }

                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
                    }
                }
            }
        }
    }
}

/** 毛玻璃卡片：与参考实现相同的 textureBlur 参数，blur 不可用时退化为普通 miuix Card */
@Composable
private fun BlurCard(
    blurEnabled: Boolean,
    backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
    blendColors: List<BlendColorEntry>,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .then(
                if (blurEnabled) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(16.dp),
                        blurRadius = 60f,
                        colors = BlurColors(blendColors = blendColors),
                        enabled = true,
                    )
                } else Modifier
            ),
        colors = CardDefaults.defaultColors(
            if (blurEnabled) Color.Transparent else colorScheme.surfaceContainer,
            Color.Transparent,
        ),
    ) {
        content()
    }
}
