package com.chenzixin.wrongbook.feature.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenzixin.wrongbook.BuildConfig
import com.chenzixin.wrongbook.ui.component.ErrataMark
import com.chenzixin.wrongbook.ui.component.WbDivider
import com.chenzixin.wrongbook.ui.component.WbIcon
import com.chenzixin.wrongbook.ui.component.WbListRow
import com.chenzixin.wrongbook.ui.component.WbSection
import com.chenzixin.wrongbook.ui.component.WbText
import com.chenzixin.wrongbook.ui.component.WbTextButton
import com.chenzixin.wrongbook.ui.component.WbTopBar
import com.chenzixin.wrongbook.ui.theme.wbColors
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * 完整使用手册（用户指南）。
 *
 * 入口固定在 设置 → 帮助 → 使用手册；首次启动的 5 页引导可以在这里重看。
 * 全部内容走语义组件，四套皮肤共用，不按皮肤复制页面。
 */
@Composable
fun UserGuideScreen(
    onBack: () -> Unit,
    onReplayIntro: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        WbTopBar(title = "使用手册", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
        ) {
            GuideHero(onReplayIntro = onReplayIntro)
            QuickStartSection()
            FiveEntriesSection()
            ScanSection()
            OrganizeSection()
            AiSection()
            AiAbilitiesSection()
            GenerateSection()
            DataSection()
            AppearanceSection()
            PrivacySection()
            FaqSection()

            val c = wbColors()
            WbText(
                text = "本手册会随版本更新。入口始终在 设置 → 帮助 → 使用手册。",
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = c.secondaryLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GuideHero(onReplayIntro: (() -> Unit)?) {
    val c = wbColors()
    WbSection {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ErrataMark(size = 72.dp)

            Spacer(Modifier.height(16.dp))

            WbText(
                text = "Errata 使用手册",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = c.label,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            WbText(
                text = "AI Mistake Notebook · v${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = c.secondaryLabel,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            WbText(
                text = "从拍下第一道错题开始，到打标签、问 AI、生成练习卷。",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = c.secondaryLabel,
                textAlign = TextAlign.Center,
            )

            if (onReplayIntro != null) {
                Spacer(Modifier.height(6.dp))
                WbTextButton(
                    text = "重看 5 页快速引导",
                    onClick = onReplayIntro,
                )
            }
        }
    }
}

@Composable
private fun QuickStartSection() {
    WbSection(
        header = "快速开始",
        footer = "第一次使用，照这三步走一遍就熟了。",
    ) {
        GuideStep(
            number = 1,
            title = "扫描入库",
            body = "扫描 → 立即扫描 → 对准整页按快门 → 拖四角把纸拉正 → 点按框选题目 → 存入题库。",
        )
        GuideStep(
            number = 2,
            title = "整理分类",
            body = "在题库点开题目，改题干、补标签；答案与解析可以用 AI 整理生成后编辑保存。分类在设置 → 标签与收藏夹，入库时也能现场新建标签。",
        )
        GuideStep(
            number = 3,
            title = "用起来",
            body = "AI 对话里问思路，生成里按标签出练习卷；数据随时可以在设置里导出备份。",
            showDivider = false,
        )
    }
}

@Composable
private fun FiveEntriesSection() {
    WbSection(header = "五个入口") {
        WbListRow(
            title = "题库",
            subtitle = "浏览、搜索、按标签筛选；右上角可新建纯文字题目或批量选择。",
            icon = MiuixIcons.Notes,
        )
        WbListRow(
            title = "扫描",
            subtitle = "拍照 → 透视矫正 → 识别切题 → 框选入库；配了视觉模型会优先 AI 识图。",
            icon = MiuixIcons.Scan,
        )
        WbListRow(
            title = "AI 对话",
            subtitle = "会话列表；可带图提问、从题库带题；支持流式回复与公式。",
            icon = MiuixIcons.Send,
        )
        WbListRow(
            title = "生成",
            subtitle = "按标签抽题，整理错题或让 AI 出同类题；预览后打印或存 PDF。",
            icon = MiuixIcons.Edit,
        )
        WbListRow(
            title = "设置",
            subtitle = "API 配置、标签与收藏夹、学习统计、界面风格、导入导出、更新。",
            icon = MiuixIcons.Settings,
            showDivider = false,
        )
    }
}

@Composable
private fun ScanSection() {
    WbSection(
        header = "扫描与入库",
        footer = "相机权限被拒绝也没关系：题库右上角「+」可以手动建题。",
    ) {
        GuideBullet("拍之前：铺平纸张、光线均匀，四个角都留在画面里。")
        GuideBullet("快门之后先拖四角把纸拉正；确认后才开始识别。")
        GuideBullet("配了视觉模型就先用 AI 识图切题，失败会自动退回本地 OCR，页面上会标明本次来源。")
        GuideBullet("识别结果里点按框选，也可以全选、反选或重拍；只有选中的题会存进题库。")
        GuideBullet("入库前可以现场新建标签，顺手套上分类。")
        GuideBullet("识别与 OCR 都在本机完成，扫描的图片不会上传。", showDivider = false)
    }
}

@Composable
private fun OrganizeSection() {
    WbSection(header = "标签、搜索与统计") {
        GuideBullet("标签在 设置 → 标签与收藏夹；可改名、改色、调排序。")
        GuideBullet("一题可以打多个标签；题库顶部按标签筛选，并能切换「全部满足 / 任一满足」。")
        GuideBullet("删除标签只解除分类，不会删除题目。")
        GuideBullet("「收藏夹」只是勾选了「作为收藏夹」的标签，不单独建一份数据。")
        GuideBullet("搜索会匹配题干、答案和 OCR 原文。")
        GuideBullet("统计在 设置 → 学习统计：掌握度分布、标签题量前 10。", showDivider = false)
    }
}

@Composable
private fun AiSection() {
    WbSection(
        header = "AI 配置（可选）",
        footer = "Key 用 Android Keystore 加密保存；只有主动使用 AI 时，才会把当前文本或当前附图发给你配置的服务商。",
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            WbText(
                text = "AI 不是必需项：不配置也能拍照、本地 OCR、整理题库和生成「整理错题」练习卷。",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = wbColors().secondaryLabel,
            )
        }
        WbDivider(startIndent = 16.dp)
        GuideStep(
            number = 1,
            title = "申请 Key",
            body = "到 DeepSeek、智谱、硅基流动等服务商控制台申请 API Key；App 不提供共享 Key。",
        )
        GuideStep(
            number = 2,
            title = "添加配置",
            body = "设置 → 模型与 API → 添加 API 配置。不想手填地址，就从「免费服务商」里选一个预设。",
        )
        GuideStep(
            number = 3,
            title = "填模型",
            body = "对话模型必填；想让扫描走 AI 识图，再填视觉模型。视觉模型留空 = 只用本地 OCR。",
        )
        GuideStep(
            number = 4,
            title = "测试连接",
            body = "点「测试连接」确认地址与 Key；配置会自动保存，不用再点保存。",
            showDivider = false,
        )
    }
}

@Composable
private fun AiAbilitiesSection() {
    WbSection(header = "AI 能做什么") {
        GuideBullet("题目详情：AI 讲解、AI 整理（回填题干 / 答案 / 解析，可编辑预览）、生成同类题。")
        GuideBullet("AI 对话：流式回复、思考过程、Markdown 与公式、附图提问、从题库带题。")
        GuideBullet("扫描：视觉模型直接框出题目区域，失败自动回退本地 OCR。")
        GuideBullet("所有模型输出都需要自己核对，尤其是题干与答案。", showDivider = false)
    }
}

@Composable
private fun GenerateSection() {
    WbSection(
        header = "生成练习卷",
        footer = "AI 出题失败会自动回退为「整理错题」，不会给你一张空卷。",
    ) {
        GuideStep(
            number = 1,
            title = "选择范围",
            body = "在「生成」里选择标签，可多选并按「全部满足 / 任一满足」组合；不选标签 = 从全部错题出题。",
        )
        GuideStep(
            number = 2,
            title = "选出题方式",
            body = "「整理错题」抽原题并附解析；「AI 同类题」换数字再练一遍。",
        )
        GuideStep(
            number = 3,
            title = "定题量与答案",
            body = "题量选 5 / 10 / 15 / 20，决定是否附答案解析，练习卷标题也能改。",
        )
        GuideStep(
            number = 4,
            title = "预览并打印",
            body = "生成后先预览，再点「打印 / PDF」，在系统打印面板里选打印机或「另存为 PDF」。",
            showDivider = false,
        )
    }
}

@Composable
private fun DataSection() {
    WbSection(header = "数据与备份") {
        GuideBullet("导出：设置 → 数据 → 导出题库。ZIP 里包含题目、标签、对话和图片，不含 API Key。")
        GuideBullet("导入：设置 → 数据 → 导入题库。按「原文 + 创建时间」判重，同一个包导两次不会翻倍。")
        GuideBullet("换手机：旧机导出 ZIP，新机装好后导入即可。")
        GuideBullet("重新处理题目图片：统一摆正、白底黑字；会覆盖现有题目图，建议先导出备份。")
        GuideBullet("卸载 App 或清除数据会删掉本地题库，系统云备份已关闭；动手前先导出。")
        GuideBullet("版本计划在 设置 → 关于 → 测试版计划：正式版 / Beta / Alpha，默认 Beta。", showDivider = false)
    }
}

@Composable
private fun AppearanceSection() {
    WbSection(header = "界面与主题") {
        GuideBullet("界面风格：设置 → 外观 → 界面风格，可在澎湃 OS、iOS、0.9.0 液态玻璃、MD3 Expressive 之间即时切换。")
        GuideBullet("主题：跟随系统 / 浅色 / 深色，与界面风格是两个独立维度。")
        GuideBullet("开屏动画可以在 设置 → 外观 关掉，启动直接进主界面。", showDivider = false)
    }
}

@Composable
private fun PrivacySection() {
    WbSection(header = "隐私与权限") {
        GuideBullet("题目、图片和对话只保存在这台手机；没有账号系统，也不会上传题库。")
        GuideBullet("不收集使用数据，不含统计 SDK 与广告。")
        GuideBullet("权限只用相机（拍照）、网络（AI 与检查更新）、安装应用（应用内更新）；导出走系统文件选择器，不申请存储权限。")
        GuideBullet("API Key 加密存储，不会出现在日志、界面回显或导出包里；换机需要重新填写。")
        GuideBullet("只有你主动使用 AI 时，当前文本或当前附图才会发给你配置的服务商；对方如何处理以其隐私政策为准。", showDivider = false)
    }
}

@Composable
private fun FaqSection() {
    WbSection(header = "常见问题") {
        GuideFaq(
            question = "没有网络、没有 API Key 能用吗？",
            answer = "能。拍照、透视矫正、本地 OCR、题库、标签、搜索、统计和「整理错题」练习卷都不依赖网络。AI 对话、AI 整理、AI 同类题和检查更新需要网络与 Key。",
        )
        GuideFaq(
            question = "识别不准怎么办？",
            answer = "先把纸铺平、光线打匀、四角留在画面里；拍完把四个角拖到纸的角上再确认。配置视觉模型后可以让 AI 先切题；入库后还能手动改题干。",
        )
        GuideFaq(
            question = "图片会被上传吗？",
            answer = "不会。扫描与 OCR 都在本机完成。只有你主动使用 AI 时，当前文本（以及对话里你附的图）才会发给你配置的服务商。",
        )
        GuideFaq(
            question = "API Key 安全吗？",
            answer = "Key 用 Android Keystore 加密保存，不会出现在日志、界面回显或导出包里。换手机后需要重新填写。",
        )
        GuideFaq(
            question = "怎么备份或换手机？",
            answer = "旧机：设置 → 数据 → 导出题库，保存 ZIP；新机装好后：设置 → 数据 → 导入题库。导出包不含 API Key，新机要重新配一次。",
        )
        GuideFaq(
            question = "卸载后数据还在吗？",
            answer = "不在。删题、删标签、清空数据和卸载都会影响本地数据；系统云备份已关闭，卸载或清除数据前一定先导出 ZIP。",
        )
        GuideFaq(
            question = "AI 收费吗？",
            answer = "App 本身不收费。额度与价格由你选择的服务商决定；内置预设只填地址和模型名，不提供共享 Key。",
            showDivider = false,
        )
    }
}

@Composable
private fun GuideStep(
    number: Int,
    title: String,
    body: String,
    showDivider: Boolean = true,
) {
    val c = wbColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(c.tintSoft),
                contentAlignment = Alignment.Center,
            ) {
                WbText(
                    text = "$number",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                WbText(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp,
                    color = c.label,
                )
                Spacer(Modifier.height(3.dp))
                WbText(
                    text = body,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = c.secondaryLabel,
                )
            }
        }
        if (showDivider) WbDivider(startIndent = 56.dp)
    }
}

@Composable
private fun GuideBullet(
    text: String,
    showDivider: Boolean = true,
) {
    val c = wbColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(c.accent),
            )
            Spacer(Modifier.width(9.dp))
            WbText(
                text = text,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = c.label,
            )
        }
        if (showDivider) WbDivider(startIndent = 16.dp)
    }
}

@Composable
private fun GuideFaq(
    question: String,
    answer: String,
    showDivider: Boolean = true,
) {
    val c = wbColors()
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WbText(
                text = question,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                color = c.label,
            )
            Spacer(Modifier.width(10.dp))
            WbText(
                text = if (expanded) "−" else "＋",
                fontSize = 18.sp,
                color = c.accent,
            )
        }
        if (expanded) {
            WbText(
                text = answer,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = c.secondaryLabel,
            )
        }
        if (showDivider) WbDivider(startIndent = 16.dp)
    }
}
