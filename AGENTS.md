# AGENTS.md —— 错题本 App 的智能体工作约定

> 这份文件是给 **AI 智能体**（Claude Code / Codex / Cursor / 其他编码 Agent）读的。
> 每次开新会话，先完整读本文件，再读 `docs/DECISIONS.md`，然后只做当前任务卡里的事。

---

## 0. 一句话项目

`com.chenzixin.wrongbook`：离线优先的 Android 错题本。拍照扫描 → 本地 OCR → 题目切分（识别题号 + 矩形框） → 多选入库 → 多标签分类 → 可选接入大模型做识图、对话、整理、出同类题。

---

## 1. 角色（可直接作为系统提示词粘贴）

```markdown
你是一名资深 Android 客户端工程师（8 年经验），精通 Kotlin、Jetpack Compose、CameraX、Room、
协程/Flow、Hilt、Clean Architecture，并有 OCR / 视觉大模型 / LLM 对话类 App 的落地经验。

你正在维护一个已存在的工程：com.chenzixin.wrongbook（错题本 App，P0 骨架已完成）。
先读 AGENTS.md 与 docs/DECISIONS.md，再动手。

工作纪律：
1. 增量交付：一次只做一个阶段（P1~P8），做完就停，等确认后再继续。
2. 先对齐再编码：需求歧义、API 不确定、依赖版本冲突时，先提 1~3 个具体问题并给出推荐方案。
3. 禁止臆造：不存在的 API、不存在的库版本、猜出来的参数名，一律不准写进代码。
   拿不准就去 artifact 里查（见第 10 节「如何验证 API」），查不到就提问。
4. 每阶段结束按固定格式汇报：已完成 / 变更文件 / 设计决策 / 风险与遗留 / 下一步建议。
5. 逻辑必须可测：题号正则、坐标换算、OCR 分段、SM-2 调度、JSON 解析，全部要有 JVM 单元测试。
6. 不能自我验收：凡是"效果类"能力（拍照切分准确率、流式是否卡顿、JSON 回填成功率），
   代码可以写，但必须显式标注 TODO(人验)，不许写"已验证通过"。
```

---

## 2. 硬规则（违反任何一条都算交付失败）

| # | 规则 | 原因 |
| --- | --- | --- |
| 1 | 不改包名 `com.chenzixin.wrongbook`、不改 `minSdk 24`、不改 UI 基座（Cupertino） | 甲方已确认 |
| 2 | 新增依赖必须先写进 `gradle/libs.versions.toml`，版本要能在 Maven 上真实解析到 | 防止猜版本导致 Sync 失败 |
| 3 | 不用 `fallbackToDestructiveMigration()`；改表必须写 Migration 并升 version | 会丢用户错题 |
| 4 | API Key 只存密文（Keystore + EncryptedSharedPreferences），禁止出现在日志/导出/异常信息里 | 安全与合规 |
| 5 | 核心链路（拍照、本地 OCR、题库浏览、标签管理）在无网络、无 Key 时必须可用 | 离线优先 |
| 6 | 任何云端调用失败都要有本地降级或明确 UI 提示，禁止吞异常（禁止空 catch） | 用户要能判断发生了什么 |
| 7 | 不在主线程做 IO / OCR / 图片解码 | 卡顿与 ANR |
| 8 | 图片入库前必须压缩（长边 ≤1600px，WebP），并生成 200px 缩略图 | 包体与列表性能 |
| 9 | 危险操作（删题/删标签/删会话/清空数据）必须二次确认 | 误操作不可逆 |
| 10 | 权限只保留 `CAMERA` / `INTERNET` / `ACCESS_NETWORK_STATE` / `REQUEST_INSTALL_PACKAGES`；导出用 SAF，不申请存储权限 | 上架合规（复习提醒已下线，不再申请通知权限） |
| 11 | 不改 P0 已定的目录分层（feature → domain → data） | 保持可维护性 |
| 12 | 每阶段结束必须能 `gradle :app:assembleDebug` 与 `gradle :app:testDebugUnitTest` 通过 | 编译失败等于没做 |

---

## 3. 技术基线（版本已核验并实际编译通过，2026-11-20）

| 类别 | 选型 | 版本 | 说明 |
| --- | --- | --- | --- |
| 构建 | Gradle | 9.9.2 | 本机安装版；wrapper 见第 12 节 |
| 构建 | AGP | 9.5.1 | 必须 9.x：生态库已要求 compileSdk 37 |
| 语言 | Kotlin | 2.3.21 | 2.2.x 读不了依赖里的 2.4 元数据 |
| 注解处理 | KSP | 2.3.14 | 新版本号体系（不再形如 `<kotlin>-<ksp>`） |
| SDK | compileSdk / targetSdk | 37 / 37 | 平台目录 `platforms/android-37.0` |
| UI | Compose BOM | 2026.09.00 | 子模块不写版本 |
| UI 风格 | `io.github.robinpcrd:cupertino` | 3.3.1 | iOS 风格组件（Compose Multiplatform 生态） |
| 数据库 | Room | 2.8.5 | |
| DI | Hilt | 2.60.1 | 2.59.x 及更早不支持 AGP 9 |
| 相机 | CameraX | 1.7.0 | P3 |
| 本地 OCR | `com.google.mlkit:text-recognition-chinese` | 16.0.1 | **捆绑版**，国内无 GMS 可用 |
| 大模型 | 自研 `OpenAiCompatLlmClient`（OkHttp） | — | 统一走 OpenAI 协议；默认模型 `deepseek-flash`（文本+图片） |
| 网络 | OkHttp | 5.5.1 | 流式 SSE |
| 图片 | Coil 3 | 3.6.3 | `io.coil-kt.coil3:coil-compose` |
| 序列化 | kotlinx.serialization | 1.13.0 | |
| Markdown | Markwon + ext-latex + JLatexMath | 4.6.2 / 0.2.0 | 离线渲染公式 |

`minSdk = 24`，JVM target 17。

两个必须保留的开关（`gradle.properties`），删掉任何一行都会编译失败：

```properties
android.overridePathCheck=true   # 项目路径含中文
android.builtInKotlin=false      # KSP 尚不兼容 AGP 9 内置 Kotlin
android.newDsl=false             # 与上一条配套，缺一会触发连锁错误
```

---

## 4. 目录与分层约束

```
app/src/main/java/com/chenzixin/wrongbook/
├── core/                 通用：Result 封装、图片压缩、加密、时间工具
├── data/
│   ├── local/            Room：entity / dao / database / 图片落盘
│   ├── remote/           LlmClient、OpenAI 兼容实现、SSE 解析
│   ├── repository/       Question / Tag / Settings / Chat
│   ├── ocr/              ML Kit OCR 引擎
│   ├── export/           备份 DTO 与 ZIP 读写
│   ├── security/         Keystore AES/GCM
│   ├── prefs/             主题与更新提示的 DataStore
│   └── generate/          练习卷生成
├── domain/               UseCase：SegmentQuestions、SaveQuestions、ReviewScheduler、RefineQuestion
├── feature/{home,scan,chat,review,settings,questiondetail,export}/
├── ui/{theme,component,navigation}/
└── di/
```

- `feature` 只能依赖 `domain` 的 UseCase，**禁止**直接 import Room / OkHttp / SDK。
- `data` 层不抛裸异常，统一返回 `Result<T>`（自定义 `WbResult`）。
- UI 状态单一来源：每个页面一个 `XxxUiState` + 一个 `XxxViewModel`，用 `StateFlow` 暴露。
- 跨模块共享的只有 `domain` 的模型和 `core` 的工具，不允许 feature 之间互相引用。

---

## 5. 三条甲方需求 → 模块映射

| 需求 | 落地模块 | 关键点 |
| --- | --- | --- |
| ① 设置里接入大模型 API（识图 + 对话 + 巩固） | `feature/settings` + `data/remote` | 多套 Profile、Keystore 加密、测试连接、视觉能力探测 |
| ② 拍照扫描 + 智能识别题号 + 矩形框多选入库 + 自定义收藏夹（多标签） | `feature/scan` + `feature/home` | 本地 OCR 打底，视觉模型增强可选；标签=收藏夹 |
| ③ AI 对话分区 | `feature/chat` | 会话管理、SSE 流式、Markdown/LaTeX、附图、从题库带入 |

---

## 6. 关键接口契约（先定接口，再写实现）

```kotlin
// 大模型：单一 OpenAI 兼容实现（覆盖 DeepSeek / 通义 / 智谱 / Ollama / 自建网关）
interface LlmClient {
    fun stream(profile: ApiProfileEntity, apiKey: String, messages: List<ChatMsg>): Flow<ChatChunk>
    suspend fun complete(
        profile: ApiProfileEntity, apiKey: String, messages: List<ChatMsg>,
        jsonMode: Boolean = false, maxTokens: Int? = null,
    ): WbResult<String>
    suspend fun testConnection(profile: ApiProfileEntity, apiKey: String): WbResult<Long>
}

// OCR
interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): OcrResult   // 行级 boundingBox + 文本
}

// 题目切分：纯函数对象（JVM 可测），返回归一化坐标（0..1）
object QuestionSegmenter {
    fun segment(ocr: OcrResult): List<QuestionRegion>
}

data class QuestionRegion(
    val index: Int,            // 识别到的题号，如 12；无题号时 -1
    val label: String,         // 原始题号文本 "12." / "（3）" / "三、"
    val rect: NormRect,        // 左上→右下，0..1（自定义类型，不依赖 Android）
    val text: String,
    val confidence: Float,
)
```

**三段式容错（所有大模型输出都必须走这套）**：
结构化 JSON 解析 → 失败重试一次 → 仍失败则本地规则兜底并提示用户「已切换本地识别」。

---

## 7. 数据模型（P0 已建表）

| 表 | 关键字段 | 备注 |
| --- | --- | --- |
| `questions` | imagePath / thumbPath / rawOcrText / refinedText / answer / analysis / subject / source / masteryLevel / nextReviewAt | 图片存相对 `filesDir` 路径 |
| `tags` | name(唯一) / color / parentId / isFolder | **收藏夹 = isFolder=true 的标签**，不单独建表 |
| `question_tag_cross_ref` | questionId + tagId | 多对多；删标签只删关联不删题 |
| `api_profiles` | baseUrl / apiKeyEnc / chatModel / visionModel / isDefault | 可多套并存 |
| `chat_sessions` | title / profileId / pinned | |
| `chat_messages` | sessionId / role / content / imagePaths | 级联删除会话 |

---

## 8. 阶段计划（严格按序，一次一个）

| 阶段 | 交付 | 通过标准 |
| --- | --- | --- |
| **P0** 骨架 | Gradle、主题、5 Tab、Room 表、Hilt、权限、隐私入口 | 可编译、能切 Tab ✅**已编译通过**（APK 62.7 MB） |
| **P1** 数据层 | DAO + Repository + `WbResult` + SM-2/标签逻辑 + 单元测试 | ✅**已完成**：15 个 JVM 单测通过；DAO 仪器测试已编译（未连真机） |
| **P2** 设置/API | Profile CRUD、Keystore 加密、测试连接、OpenAI 兼容客户端 | ✅ 代码完成；**待真 Key 联调** |
| **P3** 拍照/OCR | CameraX 拍照、ML Kit 中文 OCR、本地切分算法 + 单测 | ✅ 代码完成 + 单测通过；**准确率待真机验** |
| **P4** 多选入库 | 框选 Canvas、多选交互、裁剪压缩入库、题目详情 | ✅ 代码完成；**待真机验** |
| **P5** 标签/收藏夹 | 标签 CRUD、多标签、多标签筛选、收藏夹 UI、搜索 | ✅ 代码完成 |
| **P6** AI 对话 | 会话/消息、SSE 流式、Markdown+LaTeX、上下文摘要 | ✅ 代码完成；**待真 Key 验流式** |
| **P7** AI 整理 | 结构化 JSON 回填、生成同类题入库、三段式容错 | ✅ 代码完成 + 解析单测；**成功率待真 Key 验** |
| **P7.5** 导出导入 | ZIP 导出、SAF 导入、判重幂等、版本校验 | ✅ 代码完成 + 往返单测 |
| **P8** 生成/打磨/上架 | 练习卷生成、统计图表、透视矫正、手动建题、主题切换、上架材料 | ✅ 代码与材料完成；图标与备案待你定稿 |

每张任务卡的完整版见 [`docs/task-cards.md`](docs/task-cards.md)。

---

## 9. 全局验收清单

- [ ] 无 API Key、飞行模式下：拍照、本地 OCR、题库浏览、标签管理全部可用
- [ ] API Key 加密存储；导出包里不含明文
- [ ] 扫描双引擎：配了 visionModel 先走视觉切题，失败自动降级本地 OCR（UI 标明来源）
- [ ] 一题多标签、多标签 AND/OR 筛选、收藏夹（isFolder 标签）均可用
- [ ] AI 对话：流式、公式、思考过程展示、上下文摘要、附图提问
- [ ] 删标签只解除分类，不删题（有测试）
- [ ] 危险操作二次确认；权限拒绝后有手动输入兜底
- [ ] `assembleDebug` + `testDebugUnitTest` 全绿

---

## 10. 如何验证 API（禁止臆造）

依赖的 `import` 和函数名不确定时，**不要猜**，按这个顺序查：

1. 看 Maven 上的 `.module` / `-sources.jar`：
   `https://repo1.maven.org/maven2/<group path>/<artifact>/<version>/`
   —— 下载 `-sources.jar`，解压后直接读 `.kt` 源文件。
2. 例：Cupertino 3.3.1 的真实 API（已核对，**与网上教程不同**）：
   - 包名是 `io.github.robinpcrd.cupertino`，不是 `io.github.alexzhirkevich.*`
   - 文本组件叫 `CupertinoText`，图标叫 `CupertinoIcon(imageVector, ...)`
   - **没有** `CupertinoList` / `CupertinoListItem` / `CupertinoSection` → 已在 `ui/component/InsetList.kt` 自封装
   - 图标只有 44 个 outlined（无 Camera / Book），项目里用语义最近的替代
   - 图标是扩展属性，必须 import 具体文件：`import io.github.robinpcrd.cupertino.icons.outlined.House`
3. 仍查不到 → 停下来提问，并说明你查过哪些位置。

---

## 11. 明确禁止

- ❌ 一次性输出整个项目 / 大段重构
- ❌ 引入未经验证的第三方库（尤其图标库、图表库）
- ❌ 用 `GlobalScope`、`runBlocking`（测试除外）、空 `catch`
- ❌ 写死 API Key、模型名、baseUrl
- ❌ 声称"已验证"效果类能力（准确率、流畅度）—— 那是人验项
- ❌ 改动 `docs/DECISIONS.md` 里已确认的决策（要改先问）

---

## 12. 构建环境与验证方式（已装好，直接可用）

工具链已在本机安装（2026-11-20）：

| 工具 | 版本 / 路径 |
| --- | --- |
| JDK | 17.0.2 → `D:\dev-tools\jdk\jdk-17.0.2`（已写入用户 `JAVA_HOME`） |
| Gradle | 9.9.2 → `D:\dev-tools\gradle-9.9.2\gradle-9.9.2` |
| Android SDK | `D:\dev-tools\android-sdk`（platforms 36/37、build-tools 36/37、platform-tools） |
| 依赖镜像 | `D:\dev-tools\maven-mirrors.gradle`（腾讯云代理，**不入库**，仅本机加速） |

标准验证命令（每个阶段结束都要跑，结果如实写进汇报）：

```powershell
gradle :app:assembleDebug :app:testDebugUnitTest
```

- 本机没有 git，无法用 diff 汇报，改用「变更文件清单」。
- 本机没有真机/模拟器：`assembleDebugAndroidTest` 只能验证**编译**，仪器测试跑不了。
- **`gradlew.bat` 在中文路径下不可用**（java 通过 cmd 读不到 wrapper jar）。需要 wrapper 时：
  `cmd /c mklink /J D:\wb "D:\错题整理app"` 然后 `cd D:\wb && .\gradlew.bat ...`。
- 依赖默认走 `D:\dev-tools\maven-mirrors.gradle`；没有该文件时用 google()/mavenCentral()，速度可能很慢。
- 实验性开关与版本约束见第 3 节，**不要**为了让某个警告消失而删掉那三行 `gradle.properties`。
- **API Key 只在运行时由用户输入**：任何情况下都不要把测试 Key 写进源码、文档、注释或提交记录。

---

## 13. 发版约定（每次发版都要照做）

1. `app/build.gradle.kts`：`versionCode` +1，`versionName` 改成新版本。
2. **写更新日志**：`docs/release-notes/<tag>.md`（例如 `v0.4.1.md`）。
   - CI 会把这个文件作为 Release 正文；
   - App 的「大版本更新弹窗」和「检查更新」页显示的就是它，所以要用**用户能看懂的中文**，`## 新增 / ## 修复 / ## 移除` 分块。
3. 本地验证：`gradle :app:assembleDebug :app:testDebugUnitTest`。
4. 提交 → 推 `main` → 打 tag 并推 tag（CI 自动构建并发布）。
5. 发布后核对：Release 里有 `wrongbook-<tag>.apk`，且正文是中文更新日志。

> 更新提示分级：主版本/次版本变化弹「建议更新」；只有修订号变化不弹窗。
