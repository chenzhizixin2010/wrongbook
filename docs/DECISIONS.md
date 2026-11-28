# 决策记录（Decision Log）

> 规则：已确认的决策不许智能体擅自推翻；要改必须先问人。
> 核验时间：2026-11-19。

---

## 1. 已确认的甲方决策

| # | 问题 | 决定 |
| --- | --- | --- |
| D1 | 最低兼容版本 | `minSdk 24` 不变 |
| D2 | 目标机型 | 国内机型（多数无 GMS）→ ML Kit 用**中文捆绑版**，不用 Play 服务版 |
| D3 | 透视矫正 | P0~P7 直接拍照；四角矫正/透视变换放 P8，且引入前评估体积 |
| D4 | 大模型供应商 | DeepSeek 为主，兼容任意 OpenAI 协议端点；统一 `LlmClient` 抽象 |
| D5 | 云同步 | 不做。纯本地 + 手动导出 ZIP / 导入 |
| D6 | UI 风格 | 参考 iOS（Cupertino），不用 Material 3 默认视觉 |
| D7 | 上架 | 保留上架可能：最小权限、隐私政策入口、备案预留、AI 生成标识预留 |
| D8 | 包名 | `com.chenzixin.wrongbook` |
| D9 | 图标 | P0 用 Cupertino 内置图标占位；正式图标由人定稿，注意 SF Symbols 版权 |

---

## 2. 依赖版本核验记录

以下版本均在 **2026-11-19** 通过 Maven 仓库元数据实查（`maven-metadata.xml` / `.module`），
不是凭记忆写的。升级前请重新核验。

| 库 | 坐标 | 选定版本 | 仓库 |
| --- | --- | --- | --- |
| Gradle | 发行包 | 9.9.2 | services.gradle.org（本机用腾讯云镜像下载） |
| AGP | `com.android.tools.build:gradle` | 9.5.1 | Google Maven |
| Kotlin | `org.jetbrains.kotlin:kotlin-gradle-plugin` | 2.3.21 | Maven Central |
| KSP | `com.google.devtools.ksp:symbol-processing-gradle-plugin` | 2.3.14 | Maven Central |
| Compose BOM | `androidx.compose:compose-bom` | 2026.09.00 | Google Maven |
| Room | `androidx.room:room-runtime` | 2.8.5 | Google Maven |
| Navigation | `androidx.navigation:navigation-compose` | 2.10.2 | Google Maven |
| Activity | `androidx.activity:activity-compose` | 1.13.0 | Google Maven |
| Lifecycle | `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 | Google Maven |
| core-ktx | `androidx.core:core-ktx` | 1.19.1 | Google Maven |
| DataStore | `androidx.datastore:datastore-preferences` | 1.3.0 | Google Maven |
| Security | `androidx.security:security-crypto` | 1.1.0 | Google Maven |
| Hilt | `com.google.dagger:hilt-android` | 2.60.1 | Maven Central |
| Hilt Navigation | `androidx.hilt:hilt-navigation-compose` | 1.4.0 | Google Maven |
| WorkManager | `androidx.work:work-runtime-ktx` | 2.12.0 | Google Maven |
| CameraX | `androidx.camera:camera-camera2` | 1.7.0 | Google Maven |
| ML Kit 中文 OCR | `com.google.mlkit:text-recognition-chinese` | 16.0.1 | Google Maven |
| Cupertino | `io.github.robinpcrd:cupertino` | 3.3.1 | Maven Central |
| DeepSeek SDK | `org.oremif:deepseek-kotlin` | 0.5.0 | Maven Central |
| OkHttp | `com.squareup.okhttp3:okhttp` | 5.5.1 | Maven Central |
| Coil 3 | `io.coil-kt.coil3:coil-compose` | 3.6.3 | Maven Central |
| kotlinx.serialization | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.13.0 | Maven Central |
| Markwon | `io.noties.markwon:core` | 4.6.2 | Maven Central |

> 升级提醒：这套版本是**实际编译验证过**的组合（2026-11-20）。
> 单独升某一个（尤其 AGP / Kotlin / KSP / Hilt）很可能立刻编译失败，四个要一起动，并且每次都跑一遍完整构建。

**注意**：ML Kit 的两个坐标容易混——
`com.google.mlkit:text-recognition-chinese` 是捆绑版（模型进 APK，约 +15~20MB，国内必选）；
`com.google.android.gms:play-services-mlkit-text-recognition` 需要 GMS 下载模型，国内会卡住。

---

## 3. Cupertino 3.3.1 实测结论

下载 `cupertino-android-3.3.1-sources.jar` 直接读源码得到，**与网络教程普遍不一致**：

| 项 | 实际情况 |
| --- | --- |
| 包名 | `io.github.robinpcrd.cupertino`（不是 `io.github.alexzhirkevich.*`，后者停在 0.1.0-alpha04） |
| 构建基座 | Compose Multiplatform 1.9.3（不是纯 androidx），Android 变体可用 |
| 主题 | `CupertinoTheme(colorScheme = …, shapes = …, typography = …, content)`；`lightColorScheme()` / `darkColorScheme()` 在 `...cupertino.theme` 包 |
| 文本 | `CupertinoText(text = …, fontSize = …, color = …)`，**没有** `Text` |
| 图标 | `CupertinoIcon(imageVector/bitmap/painter, contentDescription, …)` |
| 列表 | **没有** `CupertinoList` / `CupertinoSection` / `CupertinoListItem` → 项目已自封装 `ui/component/InsetList.kt` |
| 底部导航 | `CupertinoNavigationBar { }` + 其 `RowScope.CupertinoNavigationBarItem(selected, onClick, icon, label, …)` |
| 页面骨架 | `CupertinoScaffold(topBar, bottomBar, content: (PaddingValues) -> Unit)` |
| 图标数量 | outlined 44 个、filled 若干，**没有** Camera / Book / Chart 之类 |
| 图标用法 | 每个图标是扩展属性，需 `import io.github.robinpcrd.cupertino.icons.outlined.House` 才能用 `CupertinoIcons.Outlined.House` |

因此 P0 的 Tab 图标用了语义最近的替代（扫描→矩形取景框、对话→纸飞机、巩固→循环箭头），
在 P8 统一换成正式图标时再调整。

---

## 4. 工程取舍（已落地）

| 取舍 | 决定 | 理由 |
| --- | --- | --- |
| 主题隔离 | 所有 Cupertino 依赖收敛在 `ui/theme` 与 `ui/component`，业务层只用 `wbColors()` | 将来换 UI 基座只改 2 个文件 |
| 收藏夹 | 用 `tags.isFolder` 表示，不建独立表 | 需求原文就是"用打标签的形式"，少一张表少一套同步逻辑 |
| 图片路径 | 数据库存相对 `filesDir` 的路径 | 换机/改目录迁移简单 |
| 备份 | `allowBackup=false` | 数据含 API Key 密文，禁止系统云备份 |
| 外键 | 建库时 `PRAGMA foreign_keys = ON` | 否则 CASCADE 静默失效 |
| 会话删除 | `chat_messages` 对 `sessions` CASCADE | 删会话必须带走消息 |
| 标签删除 | 关联 CASCADE，题目不动 | 误删标签不能丢错题 |

---

## 4.1 构建链路上的四个硬约束（踩过坑，别再踩）

| # | 约束 | 现象与原因 |
| --- | --- | --- |
| D10 | **必须用 AGP 9.x + compileSdk 37** | AGP 8.13 的最高 compileSdk 是 36，而 OkHttp 5.5.1、Compose 2026.09 等依赖已要求 37，编译直接失败（会一次性报 40+ 条 "requires compileSdk 37"） |
| D11 | **必须关闭 AGP 9 的内置 Kotlin** | KSP 2.3.14 与内置 Kotlin 不兼容，报 "KSP is not compatible with AGP's built-in Kotlin"；关掉后要同时设 `android.newDsl=false`，只设一个会连锁报 "Compose Compiler plugin is required" |
| D12 | **Kotlin 不能低于 2.3** | 依赖里的 `kotlin-stdlib` 是 2.4.20（元数据 2.4.0），Kotlin 2.2.21 编译器只能读到 2.3.0，报 "compiled with an incompatible version of Kotlin" |
| D13 | **Hilt 必须 ≥ 2.60** | 2.59.2 及更早的 Gradle 插件硬性要求 AGP 9.0+，与 AGP 8.13 冲突；本项目用 AGP 9.5.1 + Hilt 2.60.1 才同时满足 |

另外两条环境相关：

| # | 约束 | 说明 |
| --- | --- | --- |
| D14 | `android.overridePathCheck=true` 必须保留 | 项目路径含中文，AGP 默认拒绝构建；实测 aapt2/javac/KSP 在中文路径下正常工作 |
| D15 | 命令行别用 `gradlew.bat` | cmd 把中文绝对路径传给 java 时读不到 `gradle-wrapper.jar`（不是文件损坏）。用已安装的 `gradle`，或从 ASCII 目录联接 `D:\wb` 里跑 wrapper |
| D16 | 依赖下载走腾讯云 Maven 代理 | 本机实测：repo1 ≈ 0.1–0.8 MB/s，腾讯云代理 ≈ 6.6 MB/s。镜像通过 `--init-script D:\dev-tools\maven-mirrors.gradle` 注入，**不写进仓库** |

---

## 5. 待定问题（需要甲方决定，AI 不要自行拍板）

| # | 问题 | 现状 | 影响 |
| --- | --- | --- | --- |
| Q1 | 是否允许 http 明文访问本地模型（如局域网 Ollama） | 当前 manifest 未开 `usesCleartextTraffic` | 开了则上架需说明；可改成仅允许用户配置的私网地址 |
| Q2 | LaTeX 公式渲染方案 | Markwon 4.6.2 无公式，需另选（jlatexmath-android 等） | 影响包体与渲染质量，P6 前需定 |
| Q3 | 正式图标来源 | P0 用占位 | SF Symbols 有版权，商用需替换为自绘/开源图标 |
| Q4 | 深色模式是否跟随系统 | 当前跟随系统 | 若要手动切换，改 `WrongBookTheme` 的入参即可 |
| Q5 | 数据库是否需要加密（SQLCipher） | 当前不加密 | 上架合规一般不要求，但用户可能期望 |
| Q6 | 单题是否需要「答案/解析」的手动录入 UI | 未定 | 影响题目详情页复杂度 |
| Q7 | 包体预算 | Debug APK 实测 **60.7 MB**（含 ML Kit 捆绑模型、未混淆、未分 ABI） | 上架前若要压到 50MB 以内，考虑 ABI 拆分 + R8 + resource shrink。先确认这个体积是否可接受 |

---

## 4.2 P2~P8 期间的实现决策

| # | 决策 | 理由 |
| --- | --- | --- |
| D17 | **不引入 DeepSeek 官方 SDK，只做一层 OpenAI 兼容客户端** | DeepSeek / 通义 / 智谱 / Ollama 都兼容 `/chat/completions`；少一层依赖少一处版本冲突，流式解析也完全可控（有单测） |
| D18 | **AI 识图已接入**（2026-11-21 用 `deepseek-flash` 实测通过） | 用户配了 visionModel 就先走视觉切题，失败自动降级本地 OCR；两条路径都有 UI 标识 |
| D19 | **复习提醒的 Worker 不注入 Hilt** | 引入 `hilt-work` 只为了显示「今天 N 道」不划算；提醒文案不需要具体数量 |
| D20 | **备份格式自带版本号 + 幂等导入** | 判重键 `(rawOcrText, createdAt)`；同一包导两次不会翻倍，也不会覆盖已有图片 |
| D21 | **题目图片路径只存相对 `filesDir`** | 导入时把图片重命名为新 UUID，避免与已有文件冲突 |
| D22 | **上下文超过 40 条做一次摘要，失败则截断** | 摘要要多花一次请求；失败时宁可丢早期上下文也不能让请求超限 |
| D23 | **对话与题目的联动通过「创建会话 + 首条用户消息」实现** | 不引入新的表结构，也不做隐式的跨模块状态 |

## 4.3 曾经的未实现清单（大部分已在 2026-11-21 补齐，见 4.4）

| 功能 | 状态 | 影响 |
| --- | --- | --- |
| AI 识图（把题目图发给视觉模型） | ✅ 已接入 | 需要配置 visionModel；失败自动降级本地 OCR |
| 对话附图提问 | ✅ 已实现 | 图片存私有目录，仅当前消息携带 base64 |
| 纯文字手动建题（无图片） | ✅ 已实现 | 首页「新建」入口 |
| 标签颜色 / 排序 / 拖拽编辑 | ⚠️ 部分 | 已支持改色与排序步进；暂不支持拖拽排序 |
| 错题统计图表（标签分布、掌握度） | ✅ 已实现 | 条形图，无时间趋势 |
| 透视矫正（四角拖拽） | ✅ 已实现 | 拍照后自动进入四角调整页 |
| 通知权限的 UI 申请入口 | ⚠️ 未做 | 未授权时提醒静默跳过；后续可在设置页加「开启提醒」按钮 |

---

## 4.4 2026-11-21 追加功能与实测记录

| # | 功能 / 决策 | 说明 |
| --- | --- | --- |
| D24 | **视觉切题协议**：模型返回 `{"questions":[{"index","label","bbox":[l,t,r,b],"text"}]}`，bbox 归一化 0~1 | 解析器兼容像素坐标（任一值 > 1.5 即按像素除以宽高）；解析失败或空数组 → 返回 null → 调用方降级本地 OCR |
| D25 | **推理模型的思考过程要单独展示** | DeepSeek V4 流式先吐 `reasoning_content`、再吐 `content`；若不处理，用户会看到长时间空白。现在 UI 显示「思考中…」并滚动展示思考文本，正文开始后清空 |
| D26 | **透视矫正自研，不引 OpenCV** | 单应性变换用 8×8 高斯消元解出（`domain/scan/Homography`，有单测），反向映射 + 双线性采样在 `ImageUtils.warpQuad`；省掉 ~10MB 依赖，也避免 native 库的 ABI 问题 |
| D27 | **对话历史只发文字，图片只发当前这条** | 每轮重发历史图片会迅速推高 token 与延迟；当前消息仍完整带图 |
| D28 | **手动建题**：`source = MANUAL`，`imagePath` 为空 | 题库列表对空缩略图显示「文字」占位；详情页无图时不渲染图片区 |
| D29 | **统计图表不引图表库** | 掌握度分布、标签分布用 Compose 自绘条形图；避免为一个简单图表引入体积与许可证负担 |

### DeepSeek 实测结论（2026-11-21）

| 项 | 结果 |
| --- | --- |
| 模型 | `deepseek-flash`（DeepSeek-V4.1-Flash，context 1,048,576，`input_modalities: text,image`） |
| 非流式 | ✅ 中文正常 |
| 流式 | ✅ 先 `reasoning_content` 后 `content`，`[DONE]` 结束；`finish_reason=stop` |
| JSON mode | ✅ `response_format={"type":"json_object"}` 生效，返回可直接解析 |
| 视觉切题 | ✅ 3 道题 bbox 全部正确（归一化坐标误差 < 0.01） |
| 另一个模型 | `deepseek-v4-pro` 仅支持 text，不能做视觉切题 |

> 测试用的 API Key 只在当次运行中通过环境变量传递，**没有写进仓库任何文件**。
> 若这把 Key 曾出现在聊天/截图里，建议在控制台轮换。

### 这次补齐的「已知缺口」

| 功能 | 状态 |
| --- | --- |
| AI 识图（视觉切题） | ✅ 已接入，失败自动降级 |
| 对话附图提问 | ✅ 已实现 |
| 透视矫正（四角拖拽） | ✅ 已实现（自研 Homography） |
| 题目手动新建（纯文字） | ✅ 已实现 |
| 标签颜色 / 排序编辑 | ✅ 已实现（10 色板 + 排序步进） |
| 错题统计图表 | ✅ 已实现（掌握度分布 + 标签分布） |

---

## 4.5 2026-11-21（第二轮）改动

| # | 决策 | 说明 |
| --- | --- | --- |
| D30 | **「巩固」下线，换成「生成」** | 用户明确不需要复习调度；改为按标签出可打印练习卷。复习域代码、每日提醒 Worker、通知权限一并删除，避免留下死代码和无用权限 |
| D31 | **练习卷用 HTML + 系统打印** | WebView 渲染 HTML，`PrintManager` 打印（可另存 PDF）；不引 PDF 库，中文断行/分页交给打印框架 |
| D32 | **AI 出题失败要回退** | 模型不可用/解析失败时自动退回「整理错题」，绝不产出空卷 |
| D33 | **免费服务商预设** | 智谱 GLM / 硅基流动 / 魔搭 / Groq / OpenRouter / Gemini；只预填 baseUrl 与模型名，Key 由用户自备，额度以官网为准 |
| D34 | **主题切换存 DataStore** | 不写数据库，避免每次改主题触发 Room 观测；`ThemeMode` 三态（跟随系统/浅色/深色） |
| D35 | **按钮语义化** | 新增 `WbPrimaryButton/WbSecondaryButton/WbDangerButton/WbTextButton`，禁止在实心按钮里塞灰/红字（对比度问题） |
| D36 | **透视手柄尺寸必须用 dp** | Canvas 里写 `18f` 是 18px，高密度屏上几乎点不中；改为 `18.dp.toPx()` + 44dp 命中半径 |
| D37 | **标签可现场新建** | 扫描入库页与手动建题页都能新建标签并自动勾选；标签数量不设上限 |
| D38 | **品牌名定为 Errata** | /ɪˈrɑːtə/，出版术语「勘误表」，与「错题本」语义对应。App 显示名、开屏、桌面图标统一为 Errata，副标题 AI Mistake Notebook；包名 `com.chenzixin.wrongbook`、GitHub 仓库名与备份格式保持不变，避免升级/更新链路断裂 |

---

## 4.6 免费服务商预设扩充（2026-11-21）

| # | 决策 | 说明 |
| --- | --- | --- |
| D38 | **内置 10 个服务商预设，端点逐个探活** | `ark.cn-beijing.volces.com/api/v3`、`api.longcat.chat/openai/v1`、`dashscope.aliyuncs.com/compatible-mode/v1`、`open.bigmodel.cn/api/paas/v4`、`api.siliconflow.cn/v1`、`api.deepseek.com/v1` 全部实测返回 401/400（说明路由存在），不是猜的 |
| D39 | **只预填地址与模型名，绝不内置共享 Key** | 共享 Key 不合规、会被刷爆；界面里写明「Key 需要自己申请」 |
| D40 | **免费政策写进 quota 字段但标注「以官网为准」** | 各家额度变动频繁（GLM-4.5-Flash 已下线换 4.7-Flash、LongCat 额度调整过），代码不做硬性判断 |
| D41 | **UI 采用四皮肤，默认 Miuix** | 新增 `UiStyle`（MIUIX / IOS / V090 / MD3E）存 DataStore；四套皮肤共用页面与 ViewModel，通过 `LocalUiStyle` 分支。iOS 允许柔和玻璃渐变，V090 允许极光/悬浮胶囊/蓝紫渐变，MD3E 用 M3 tonal 色与大圆角，Miuix 保持纯色。切换风格不重置导航 |

> 易错点：LongCat 的 API 域名是 `api.longcat.chat`（`longcat.chat` 是网页端，`/v1/models` 会返回 HTML）；
> 智谱的 `glm-4.5-flash` 已下线，预设改用 `glm-4.7-flash`。

---

## 4.7 首次使用引导与用户手册（2026-11-25）

| # | 决策 | 说明 |
| --- | --- | --- |
| D42 | **首次启动用 5 页全屏引导，完整手册独立成页** | 引导覆盖在主界面之上（与开屏同层），不参与导航，不重置 Tab 栈；完成或跳过后写 DataStore `guide_seen`，只显示一次 |
| D43 | **引导内容不引第三方引导库** | 用 Compose Foundation 的 `HorizontalPager` + 现有 Wb 语义组件；四套皮肤共用，不按皮肤复制页面 |
| D44 | **手册入口固定在 设置 → 帮助 → 使用手册** | 手册内提供「重看 5 页快速引导」，通过 `resetGuideSeen()` 重新置为未读；题库空状态也放一个入口 |
| D45 | **首次启动这一轮不检查更新** | 等 `guide_seen` 读出 true 才调 `checkOnce()`，避免「建议更新」弹窗压在引导上；老用户行为不变 |
| D46 | **升级用户也会看到一次引导** | 老版本没有 `guide_seen` 键，默认 false；可跳过，不阻塞使用，不做「安装时间 / 数据库是否存在」的启发式判断 |

---

## 4.8 Alpha 内测通道与 AI 出题质量（2026-11-28）

| # | 决策 | 说明 |
| --- | --- | --- |
| D47 | **通道从「Beta 开关」升级为三档版本计划，默认 Beta** | `UpdateChannel`（STABLE / BETA / ALPHA）存 DataStore `update_channel`；设置里点「测试版计划」弹单选弹窗。默认 Beta 的原因：仓库当前所有 0.x 都是 pre-release，停在正式版档会永远 404 |
| D48 | **Alpha 版本号形如 `0.13.A01`，第 3 段是 `A` + 内测序号** | 解析时把 `A01` 从未知段里摘出来存成 `alpha`；比较规则「数字段相同则正式版 > Alpha，Alpha 之间比序号」，保证 `0.13.0 > 0.13.A05 > 0.13.A04`，用户从内测升正式能收到提示。旧的 `beta_channel` 布尔键不再读取，Beta 已是默认值 |
| D49 | **CI 增加 versionName 与 tag 一致性校验** | tag 与 `versionName` 不一致时 App 会把自己判成旧版本或永远提示更新；tag 打错时直接让 CI 失败，比发出去再补救便宜 |
| D50 | **AI 出题改四段式：蓝图 → 出题 → 本地质检 → 返工** | 一次性让模型吐 10 道题时后面几道会明显变水；先出蓝图把考点摊开，出题时逐条对齐，再用本地规则闸门判掉不合格的题，把原因当「返工单」回传只补缺口 |
| D51 | **质量闸门用离线规则，不让模型自评** | 模型自评会给自己打高分。规则判「不可作答」（如图、截断、答案=略）、「重复」（与原题 ≥0.90、与同批 ≥0.82）、「选择题残缺」、「无解析」，全在 `QuestionQuality` 里，纯 Kotlin 可单测 |
| D52 | **题风格范本内置离线题型库，联网检索只是可选增强** | `QuestionArchetypes` 按学科写死真实考卷的设问方式与干扰项套路，永远可用；联网搜题通过 OpenAI 兼容的 `tools:[{type:web_search}]` 上送，服务端不认就静默去掉重试并提示已降级。只在第一轮出题时尝试，避免每轮多等一次 |
| D53 | **被闸门刷掉的题必须让用户看见** | 生成完弹「出题提示」：只出了几道、被刷掉几道、为什么；不允许默默少题 |

---

## 4.9 插图超分与答案拍照（2026-11-28）

| # | 决策 | 说明 |
| --- | --- | --- |
| D54 | **超分选 EDSR×4，不用 Real-ESRGAN 这类 GAN** | 生成式超分会脑补笔画，数学图形/公式被改一笔就成错题；EDSR 是 PSNR 导向的，只拉清已有边缘。同时它纯卷积、68 个算子、float32 权重，CPU 就能跑，不依赖 GPU 委托 |
| D55 | **模型内置在 assets，不做「按需下载」** | 原计划是从 GitHub Release 下载；但当前环境没有可用的推送凭据，内置可以保证离线/断网也能用，也不会因为镜像站失效而让功能静默消失。代价是安装包 +7.4MB，已通过去掉 32 位 x86 ABI（约 16MB）抵掉 |
| D56 | **分块推理：128×128 输入 + 8px 重叠，最多 9 块** | 8px 重叠是为了把卷积边界伪影挤进被裁掉的区域，避免拼缝；9 块 ≈ 5 秒是「用户还能忍」的上限，超过就退回 Lanczos。分块几何是纯函数 `TilingPlanner`，有单测锁住「覆盖完整、不越界」 |
| D57 | **超分只作用于小图（≤336px）** | 大图本身不缺像素，放大到 4 倍只会白烧 CPU；插图「高清重绘」对低分辨率裁剪图才有意义 |
| D58 | **答案录入用独立相机，不复用扫描流程** | 扫描页是「整页→切题→多选」，答案只需要「一张照片→一段文字」，复用会把用户绕进切题界面。相机实现同款（CameraX Preview+ImageCapture），并保留相册兜底 |
| D59 | **答案照片先自动摆正再 OCR** | 答案页经常横着拍；`OcrOrienter` 用「四个方向各跑一遍低分辨率 OCR，取识别字最多」作为判据，比图像特征稳（老图上投影轮廓经常分不出来）。扫描页与答案页共用这一份实现 |

---

## 4.10 0.13.0 事故复盘与修复（2026-11-28）

| # | 决策 | 说明 |
| --- | --- | --- |
| D60 | **Markwon 行内公式必须同时装 `inline-parser` 插件** | `JLatexMathPlugin` 打开 `inlinesEnabled(true)` 后，`configure()` 会 `registry.require(MarkwonInlineParserPlugin.class)`；缺插件就在 `Markwon.builder().build()` 抛 NPE，表现为「点开任意题目闪退」。依赖是 `io.noties.markwon:inline-parser`，插件要排在 latex 插件**前面**注册 |
| D61 | **单 `$...$` 必须先升级成 `$$...$$` 才能渲染** | markwon-ext-latex 的行内处理器正则是 `(\${2})([\s\S]+?)\1`，只认双美元；单美元永远当普通字符。所以清洗阶段统一升级（已双美元的不动，`$5 和 $10` 这类首尾有空白的写法跳过） |
| D62 | **渲染器装配失败要降级，不能崩页面** | `MarkdownText` 把 `Markwon.builder()...build()` 包进 `runCatching`，失败时直接把原文塞进 TextView。同类插件装配错误以后只会「公式变成源码」，不会闪退 |
| D63 | **自检要按生产同一条路径跑并给出可诊断的结果** | 超分自检最初给 1 块预算，而 128×128 输入按 112 步长会切 4 块，于是误报「推理没结果」。现在 `upscale` 返回 sealed 结果（Done / Skipped(reason) / Failed(msg)），自检如实报块数与平均耗时 |
| D64 | **重处理图片禁止「先删后改名」，缩略图路径要按扩展名推导** | `replace(".webp", "_thumb.webp")` 对 `.jpg/.png` 老图会得到与主图相同的路径，拿 200px 缩略图覆盖主图；改名失败还会把原图删掉。现在：先验证新图能解码 → 原子替换（失败则覆盖写）→ 缩略图路径与主图同名就跳过 |
