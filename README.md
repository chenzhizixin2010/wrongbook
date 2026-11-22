# Errata · AI Mistake Notebook

一个**离线优先、AI 增强**的 Android 错题本（原名 WrongBook / 错题整理 App，包名保持不变）：拍照 → 透视矫正 → 本地 OCR 或视觉模型切题 → 矩形多选入库 → 多标签分类 → AI 讲解 / 整理 / 出同类题 → 按标签生成可打印练习卷。

- 包名：`com.chenzixin.wrongbook`
- 系统要求：Android 7.0（API 24）+，编译/目标 API 37
- 视觉风格：**Miuix / iOS / 0.9.0 液态玻璃 三皮肤**，设置内实时切换；Miuix 纯色 Monet，iOS Cupertino 玻璃，0.9.0 极光 + 悬浮胶囊
- 数据：纯本地（Room + 私有目录图片），手动导出 ZIP

**仓库**：https://github.com/chenzhizixin2010/wrongbook ｜ **当前版本**：v0.10.1

## 当前状态

P0~P8 **功能全部落地**，编译与单元测试通过；真实模型链路已用 DeepSeek 实测。

```
gradle :app:assembleDebug + :app:testDebugUnitTest
-> BUILD SUCCESSFUL
-> 139 tests, 0 failures
-> app-debug.apk 65.0 MB
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
- 所有模型输出走「解析 → 重试一次 → 本地兜底」

**生成与数据**
- **生成练习卷**：按标签（AND / OR）筛错题 → 「整理错题」或「AI 生成同类题」→ 5/10/15/20 题 → 附答案 → 预览 → 系统打印（可另存 PDF）
- AI 出题失败会自动回退成「整理错题」，不会生成空卷
- **学习统计**：掌握度 0~5 分布、标签题量前 10（设置 → 学习统计）
- ZIP 备份：`manifest.json` + `questions.json` + `tags.json` + `chats.json` + `images/`
- 导入幂等：按（原文 + 创建时间）判重；备份里不含 API Key

**外观与交互**
- 深色 / 浅色 / 跟随系统切换（设置 → 外观，DataStore 持久化）
- 按钮统一语义色（主要/次要/危险/文字），修掉了「灰字压蓝底」看不清的问题
- 对话会话列表支持删除；标签可在打标签时现场新建，数量不限

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

`gradlew.bat` 在中文路径下会报「无法访问 gradle-wrapper.jar」，用 `D:\wb` 目录联接绕过：

```powershell
cmd /c mklink /J D:\wb "D:\错题整理app"
cd D:\wb; .\gradlew.bat :app:assembleDebug
```

## 目录

```
app/src/main/java/com/chenzixin/wrongbook/
├── core/                    WbResult、图片处理（含透视变换）、Task/Future→协程
├── data/
│   ├── local/               Room：6 张表、DAO、图片落盘（题目图/对话图）
│   ├── remote/              LlmClient、OpenAI 兼容实现、SSE 解析
│   ├── repository/          Question / Tag / Settings / Chat
│   ├── ocr/                 ML Kit OCR、视觉模型切题
│   ├── export/              备份 DTO 与 ZIP 读写
│   ├── security/            Keystore AES/GCM
│   └── work/                复习提醒
├── domain/                  review / tag / scan（切分、单应性）/ chat / ai
├── ui/{theme,component,navigation}/
└── feature/{home,scan,chat,review,settings,tag,detail,export}/
```

## 文档

| 文件 | 作用 |
| --- | --- |
| [`AGENTS.md`](AGENTS.md) | 给 AI 智能体的主提示词与工程约定 |
| [`docs/task-cards.md`](docs/task-cards.md) | P1~P8 任务卡（含验收标准） |
| [`docs/HANDOFF.md`](docs/HANDOFF.md) | 哪些活能交给 AI、怎么验收 |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | 决策、版本核验、踩过的坑 |
| [`docs/release/`](docs/release/CHECKLIST.md) | 隐私政策草稿、SDK 清单、上架检查表 |
