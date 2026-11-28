# Errata · AI Mistake Notebook

一个**离线优先、AI 增强**的 Android 错题本（原名 WrongBook / 错题整理 App，包名保持不变）：拍照 → 透视矫正 → 本地 OCR 或视觉模型切题 → 矩形多选入库 → 多标签分类 → AI 讲解 / 整理 / 出同类题 → 按标签生成可打印练习卷。

- 包名：`com.chenzixin.wrongbook`
- 系统要求：Android 7.0（API 24）+，编译/目标 API 37
- 视觉风格：**Miuix / iOS / 0.9.0 液态玻璃 / MD3 Expressive 四皮肤**，设置内实时切换；Miuix 纯色 Monet，iOS Cupertino 玻璃，0.9.0 极光 + 悬浮胶囊，MD3E 官方 Material3 组件 + 动态取色 + 药丸导航
- 数据：纯本地（Room + 私有目录图片），手动导出 ZIP

**仓库**：https://github.com/chenzhizixin2010/wrongbook ｜ **官网**：https://chenzhizixin2010.github.io/wrongbook/ ｜ **当前版本**：v0.13.1

**版本计划**（设置 → 关于 → 测试版计划，默认 Beta）：

| 通道 | 收什么 | 放在哪 |
| --- | --- | --- |
| 正式版 | 非 pre-release 的稳定版本 | 主仓库 |
| Beta | 正式版 + Beta 测试版 | 主仓库（标 pre-release） |
| Alpha | 最新内测版 `0.13.A01` 这种 | [wrongbook-alpha](https://github.com/chenzhizixin2010/wrongbook-alpha)（只有 Release，没有源码） |

## 当前状态

P0~P8 **功能全部落地**，编译与单元测试通过；真实模型链路已用 DeepSeek 实测。

```
gradle :app:assembleDebug + :app:testDebugUnitTest
-> BUILD SUCCESSFUL
-> 239 tests, 0 failures
-> app-debug.apk 72.2 MB（含 7.4MB 超分模型 + TFLite 运行时）
```

## 界面

![Errata · Miuix](docs/ui-screens.png)

设计规范见 [docs/UI-MIUIX.md](docs/UI-MIUIX.md)；旧版液态玻璃稿已归档到 [docs/legacy](docs/legacy)。

## 已实现的功能

**扫描与入库**
- CameraX 拍照（ImageProxy → Bitmap，自动按 EXIF 旋转）
- **透视矫正**：拍完先拖四角把试卷拉正（自研单应性变换 + 双线性采样，不依赖 OpenCV）
- **双引擎切题**：
  - 配了视觉模型 → 先让模型直接返回题目 bbox（`deepseek-flash` 实测可用，3 道题 bbox 全中）
  - 失败 / 未配置 → 自动降级本地 ML Kit 中文 OCR + 行距与题号正则
  - UI 会标明本次用的是「AI 识图」还是「本地识别」
- 题号识别：`12.` `（3）` `三、`，含 `3.14` 这类小数的误判防护
- 归一化矩形叠图、点选 / 全选 / 反选、题号角标、长按补框（本地识别路径）
- 裁剪 → 长边 1600px WebP + 200px 缩略图 → 入库

**题库与分类**
- 列表（缩略图 / 文字占位 + 预览 + 标签）、搜索（题干 / OCR 原文）
- 多标签筛选，AND / OR 切换
- 标签即收藏夹（`isFolder`），两级分类
- 标签**改色（10 色板）、调排序**、改名、删除；删标签不删题
- **手动新建题目**（无图纯文字），`source = MANUAL`

**AI**
- 多套 API 配置，Key 用 Android Keystore AES/GCM 加密；「测试连接」显示往返耗时
- 会话列表 + SSE 流式 + Markdown/LaTeX 渲染
- **思考过程展示**：DeepSeek V4 等推理模型会先吐 `reasoning_content`，界面显示「思考中…」并滚动展示，正文开始后自动收起
- **对话附图提问**：从相册选图，随消息发送（只发当前这条，历史消息只发文字，避免超限）
- 上下文超过 40 条自动摘要（失败则截断）
- 题目详情三个动作：AI 讲解（开新会话）、AI 整理（JSON 回填，可编辑预览）、生成同类题（入库）
- **AI 出题质量闸门**：出题走「蓝图 → 出题 → 本地质检 → 返工」四段；判掉依赖插图的题（AI 造不出图）、答案写「略/见解析」的、缺解析的、与原题几乎一样的、同批重复的，被刷掉多少道会在生成后告诉你
- **离线题型风格库**：按学科内置真实考卷的设问方式与干扰项套路（物理要考「漏算重力/方向搞反/单位未换算」这类典型错误），不联网也有效
- **联网搜题**（默认开）：模型支持服务端检索时会先查真实题目，不支持的自动回退本地题型库并提示
- 所有模型输出走「解析 → 重试一次 → 本地兜底」

**生成与数据**
- **生成练习卷**：按标签（AND / OR）筛错题 → 「整理错题」或「AI 生成同类题」→ 5/10/15/20 题 → 附答案 → 预览 → 系统打印（可另存 PDF）
- AI 出题失败会自动回退成「整理错题」，不会生成空卷
- **题目插图**：详情页可加多张图，「高清重绘」默认走**内置 EDSR ×4 超分**（小图分块放大 4 倍，约 5–8 秒；关掉则秒出 Lanczos）；插图会一起进练习卷（打印/PDF/Word）和备份 ZIP
  - 效果对比（左：双三次，右：EDSR ×4，输入是模拟拍糊的小图）：

    ![超分对比](docs/sr-compare.png)
- **答案拍照录入**：详情页「扫答案」直接开相机拍照，自动摆正（四方向试 OCR）后识别，弹校对框改完保存；没相机权限可走相册
- **OCR 推荐标签**：扫描识别完按学科/知识点给出建议标签，一键套用或当场新建
- **学习统计**：掌握度 0~5 分布、标签题量前 10（设置 → 学习统计）
- ZIP 备份：`manifest.json` + `questions.json` + `tags.json` + `chats.json` + `images/` + `figures/`（题目插图）
- 导入幂等：按（原文 + 创建时间）判重；备份里不含 API Key

**外观与交互**
- 深色 / 浅色 / 跟随系统切换（设置 → 外观，DataStore 持久化）
- 按钮统一语义色（主要/次要/危险/文字），修掉了「灰字压蓝底」看不清的问题
- 对话会话列表支持删除；标签可在打标签时现场新建，数量不限

**入门与帮助**
- 首次启动 5 页快速引导（扫描入库 / 标签分类 / AI 可选 / 数据隐私），可跳过，只显示一次
- 设置 → 帮助 → 使用手册：完整用户指南 + 常见问题，可重看快速引导
- 题库空状态提供「查看使用手册」入口；首次启动不检查更新，避免弹窗打断引导

## 实测记录（DeepSeek）

用 `deepseek-flash`（DeepSeek-V4.1-Flash）验证过：

| 能力 | 结果 |
| --- | --- |
| 非流式对话 | ✅ 中文正常 |
| SSE 流式 | ✅ 先 `reasoning_content` 后 `content`，`[DONE]` 结束 |
| JSON mode（`response_format`） | ✅ 返回可直接解析的 JSON |
| 视觉切题 | ✅ 3 道题的 bbox 全部命中，坐标归一化符合预期 |
| 模型清单 | `deepseek-flash` 支持 `text,image`；`deepseek-v4-pro` 仅 text |

默认配置里对话模型与视觉模型都预填 `deepseek-flash`。

## 仍需真机/真 Key 验证的

- 拍照切分的实际准确率（不同纸张、光照、双栏排版）
- 透视矫正后的识别效果与耗时（千元机）——手柄已改为按 dp 绘制、命中半径 44dp
- 打印输出在真机打印服务里的分页效果
- 免费服务商的实际额度与可用性（以各官网为准）
- 插图超分的真机耗时：桌面 CPU 4 线程实测 **646 ms/块**（`ai-edge-litert` 跑同一份 `edsr_x4.tflite`，9 块 ≈ 5.8 秒）；手机上建议进「设置 → 图像增强 → 超分自检」看实际数字
- 答案拍照的识别率（手写、反光、褶皱）——自动摆正的判据是「四方向 OCR 取字最多」，已用单测锁逻辑，准确率仍需人验



### 内置的免费 / 低价服务商

「设置 → 模型与 API → 免费服务商」里每一项都能看到申请直达链接和领取步骤，一键填入地址与模型名。

| 服务商 | 免费政策（以官网为准） | 网络 |
| --- | --- | --- |
| 火山方舟 · 豆包 | 学生认证后 `doubao-seed-1-6` 可长期免费，官方称不限量 | 国内直连 |
| 智谱 GLM | `GLM-4.7-Flash` 永久免费；教育邮箱再送 10 万 Token | 国内直连 |
| 美团 LongCat | 每天 5000 万 Token（Lite）+ 500 万共享 | 国内直连 |
| 阿里云百炼 | 新用户百万级 Token 试用，学生认证可能额外送 | 国内直连 |
| 硅基流动 | 新用户约 2000 万 Token | 国内直连 |
| 魔搭 ModelScope | 免费推理接口（限频） | 国内直连 |
| DeepSeek | 新用户赠送额度；`deepseek-flash` 支持识图 | 国内直连 |
| Groq / OpenRouter / Gemini | 有免费额度或 `:free` 模型 | 需外网 |

## 更新日志

### v0.13.1

见 [`docs/release-notes/v0.13.1.md`](docs/release-notes/v0.13.1.md)。**0.13.0 的紧急修复版**：

- 修复「点开题目详情 / 打开 AI 对话闪退」：行内公式开关缺 `inline-parser` 插件，Markwon 装配时 NPE；并给渲染器加兜底（装配失败退化成纯文本）
- 修复行内公式不渲染：单 `$...$` 升级为 `$$...$$`（Markwon 的 latex 插件只认双美元）
- 修复超分自检误报「推理没结果」：这其实是「128×128 需要 4 块、预算只给了 1 块」
- 修复「重新处理题目图片」把主图覆盖成 200px 缩略图（非 `.webp` 老图的路径推导有 bug），替换改为原子写 + 覆盖前校验可解码

### v0.13.0

完整说明见 [`docs/release-notes/v0.13.0.md`](docs/release-notes/v0.13.0.md)。

- **版本计划**：正式版 / Beta / Alpha 三档（默认 Beta）；Alpha 走独立分发仓库 `wrongbook-alpha`，版本号 `0.13.A01` 这种，正式版永远比同号 Alpha 新
- **AI 出题质量**：蓝图 → 出题 → 本地质检 → 返工；离线题型库 + 可选联网搜题；被刷掉的题会说明原因
- **答案拍照录入**：详情页直接开相机拍答案，自动摆正后 OCR，校对再入库
- **插图超分**：内置 EDSR ×4（Apache-2.0），小图分块放大 4 倍；设置里可关闭、可自检
- **题目插图**：多图上传、随练习卷导出、进备份 ZIP
- **OCR 推荐标签**：按学科/知识点建议，可一键套用
- **首次使用引导 + 完整使用手册**：5 页快速介绍（可跳过、只显示一次）、设置 → 帮助 → 使用手册
- 修复：AI 讲解里的行内公式（`$...$`）不渲染；题干自带题号导致卷子重复编号；选项挤一行被折断
- 安装包从 65MB 增至约 72MB（模型 + 推理运行时），已去掉 32 位 x86 抵掉一部分

### v0.4.0

- **「巩固」换成「生成」**：按标签（AND/OR）出练习卷，支持「整理错题」与「AI 生成同类题」，可打印 / 另存 PDF；AI 失败自动回退
- 学习统计独立成页（设置 → 学习统计）：掌握度分布、标签分布
- AI 对话支持删除会话；标签可在扫描/建题时现场新建，数量不限
- 免费服务商预设（智谱 / 硅基流动 / 魔搭 / Groq / OpenRouter / Gemini），点开有申请直达链接与步骤教程
- 深色 / 浅色 / 跟随系统切换
- 更新提示分级：大版本弹「建议更新」，小版本不打扰；下载显示进度、速度、剩余时间
- 修复：透视矫正手柄按 dp 绘制（原来 18px 在高密度屏上几乎点不中）
- 修复：按钮文字对比度（灰字压蓝底）、清单缺少 INTERNET 权限
- 移除：复习调度、每日提醒、POST_NOTIFICATIONS 权限

### v0.3.1 / v0.3.0

- 首个可用版本：扫描 + 透视矫正 + 双引擎切题 + 多标签 + AI 对话 + AI 整理 + ZIP 备份 + GitHub 自动更新

## 环境（本机已装好）

| 工具 | 路径 |
| --- | --- |
| JDK 17 | `D:\dev-tools\jdk\jdk-17.0.2` |
| Gradle 9.9.2 | `D:\dev-tools\gradle-9.9.2\gradle-9.9.2` |
| Android SDK | `D:\dev-tools\android-sdk`（platforms 36/37、build-tools 36/37） |
| 依赖镜像 | `D:\dev-tools\maven-mirrors.gradle`（腾讯云代理，不入库） |

## 怎么构建

```powershell
gradle :app:assembleDebug :app:testDebugUnitTest   # 本机 Gradle，中文路径可用
.\scripts\build.ps1                                # 或这个脚本
```

### 发版

| 版本 | tag | 发到哪 | 触发 |
| --- | --- | --- | --- |
| 正式版 / Beta | `v0.13.0` | 主仓库 Releases（0.x 全部标 pre-release） | 推 tag → `.github/workflows/release.yml` |
| Alpha 内测 | `v0.13.A01` | `wrongbook-alpha` 仓库 Releases（只有包、没有源码） | 推 tag → `.github/workflows/alpha.yml` |

两个工作流都会校验 `versionName` 与 tag 一致，不一致直接失败。
Alpha 工作流需要仓库 Secret **`ALPHA_TOKEN`**（对 `wrongbook-alpha` 有 `contents:write` 的 PAT）——默认 `GITHUB_TOKEN` 只能操作本仓库，跨仓库发布会失败。

`gradlew.bat` 在中文路径下会报「无法访问 gradle-wrapper.jar」，用 `D:\wb` 目录联接绕过：

```powershell
cmd /c mklink /J D:\wb "D:\错题整理app"
cd D:\wb; .\gradlew.bat :app:assembleDebug
```

## 目录

```
app/src/main/java/com/chenzixin/wrongbook/
├── core/          WbResult、图片处理（透视变换 / Lanczos / 提亮）、Task→协程、LaTeX 归一化
├── data/
│   ├── local/     Room：7 张表（含题目插图）、DAO、图片落盘
│   ├── remote/    LlmClient、OpenAI 兼容实现（SSE / JSON mode / tools 透传）
│   ├── repository/ Question / Tag / Settings / Chat / QuestionImage
│   ├── ocr/       ML Kit OCR、视觉模型切题、自动摆正（OcrOrienter）
│   ├── ml/        本地模型推理：EDSR ×4 超分（分块、CPU）
│   ├── export/    备份 DTO 与 ZIP 读写（含 figures/）
│   ├── generate/  练习卷生成 + AI 出题流水线
│   ├── practice/  练习卷历史（文件 + index.json）
│   ├── update/    GitHub Releases 检查与下载（镜像测速）
│   ├── prefs/     主题 / 更新通道 / 开关（DataStore）
│   ├── security/  Keystore AES/GCM
│   └── maintenance/ 图片批量重处理
├── domain/        ai / chat / generate / qa / scan / sr（分块几何）/ tag / update（通道与版本比较）
├── ui/{theme,component,navigation}/
└── feature/{home,scan,chat,generate,detail,tag,settings,export,stats,guide,update}/
```

分层约束：`feature → domain → data`，feature 不直接碰 Room / OkHttp；跨层共享的只有 domain 模型与 core 工具。

## 文档

| 文件 | 作用 |
| --- | --- |
| [`AGENTS.md`](AGENTS.md) | 给 AI 智能体的主提示词与工程约定 |
| [`docs/USER-GUIDE.md`](docs/USER-GUIDE.md) | 面向用户的完整使用手册（与 App 内「使用手册」同步） |
| [`docs/task-cards.md`](docs/task-cards.md) | P1~P8 任务卡（含验收标准） |
| [`docs/HANDOFF.md`](docs/HANDOFF.md) | 哪些活能交给 AI、怎么验收 |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | 决策、版本核验、踩过的坑 |
| [`docs/release-notes/`](docs/release-notes) | 每个版本的更新日志（CI 直接当 Release 正文） |
| [`docs/release/`](docs/release/CHECKLIST.md) | 隐私政策草稿、SDK 清单、上架检查表 |
