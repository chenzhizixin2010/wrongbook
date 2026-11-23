package com.chenzixin.wrongbook.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.ui.component.WbIconTile
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.component.WbTextButton
import io.github.robinpcrd.cupertino.icons.CupertinoIcons
import io.github.robinpcrd.cupertino.icons.outlined.Gearshape
import com.chenzixin.wrongbook.ui.component.WbListRow

/**
 * 关于开发者：版本信息、仓库入口、技术栈与致谢。
 *
 * 所有链接都走系统浏览器，避免在应用内塞一个 WebView。
 */
@Composable
fun AboutDeveloperScreen(
    onBack: () -> Unit,
    onOpenUpdate: () -> Unit,
) {
    val context = LocalContext.current
    val repoUrl = "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}"
    val issuesUrl = "$repoUrl/issues"
    val releasesUrl = "$repoUrl/releases"

    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(title = "关于开发者", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
        ) {
            WbSection {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WbIconTile(icon = CupertinoIcons.Outlined.Gearshape, size = 46.dp, corner = 14.dp)
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            WbText(
                                text = "Errata 错题本",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            WbText(
                                text = "版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                                modifier = Modifier.padding(top = 2.dp),
                                fontSize = 12.sp,
                                color = wbSecondary(),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    WbText(
                        text = "离线优先的错题整理工具：拍照识别、多标签分类、AI 讲解与练习卷生成。题目与图片只存在本机。",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    )
                }
            }

            WbSection(header = "开发者") {
                WbListRow(
                    title = "chenzhizixin2010",
                    subtitle = "GitHub · 反馈与问题都走 Issues",
                    showDivider = true,
                    onClick = { openUrl(context, issuesUrl) },
                )
                WbListRow(
                    title = "项目仓库",
                    subtitle = repoUrl,
                    showDivider = false,
                    onClick = { openUrl(context, repoUrl) },
                )
            }

            WbSection(header = "版本与更新", footer = "预发布版本（Beta）需要在设置里打开 Beta 计划才会推送。") {
                WbListRow(
                    title = "检查更新",
                    subtitle = "从 GitHub Releases 拉取",
                    showDivider = true,
                    onClick = onOpenUpdate,
                )
                WbListRow(
                    title = "发布记录",
                    subtitle = releasesUrl,
                    showDivider = false,
                    onClick = { openUrl(context, releasesUrl) },
                )
            }

            WbSection(header = "技术栈", footer = "全部为公开开源项目，采用各自许可证。") {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf(
                        "Kotlin + Jetpack Compose" to "Apache-2.0",
                        "Miuix（澎湃 OS 风格组件）" to "Apache-2.0",
                        "Cupertino（iOS 风格组件）" to "MIT",
                        "Room / Hilt / CameraX" to "Apache-2.0",
                        "ML Kit 中文 OCR" to "Google 条款",
                        "Markwon + JLatexMath" to "Apache-2.0 / MIT",
                        "Coil 3" to "Apache-2.0",
                    ).forEach { (name, license) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            WbText(text = name, fontSize = 13.sp)
                            WbText(text = license, fontSize = 11.sp, color = wbSecondary())
                        }
                    }
                }
            }

            WbSection(header = "隐私") {
                Column(modifier = Modifier.padding(16.dp)) {
                    WbText(
                        text = "不收集任何使用数据；不含统计 SDK 与广告。只有你主动使用 AI 功能时，才会把题目文字或图片发送给你配置的模型服务商。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = wbSecondary(),
                    )
                }
            }
        }
    }
}

@Composable
private fun wbSecondary() = com.chenzixin.wrongbook.ui.theme.wbColors().secondaryLabel
