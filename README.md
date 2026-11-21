# 错题整理 App · WrongBook

一个**离线优先、AI 增强**的 Android 错题本：拍照 → 透视矫正 → 本地 OCR 或视觉模型切题 → 矩形多选入库 → 多标签分类 → AI 讲解 / 整理 / 出同类题 → 按掌握度复习。

- 包名：`com.chenzixin.wrongbook`
- 系统要求：Android 7.0（API 24）+，编译/目标 API 37
- 视觉风格：iOS / Cupertino
- 数据：纯本地（Room + 私有目录图片），手动导出 ZIP

**仓库**：https://github.com/chenzhizixin2010/wrongbook ｜ **最新发布**：v0.3.0（Actions 自动构建，APK 62.8 MB）

## 当前状态

P0~P8 **功能全部落地**，编译与单元测试通过；真实模型链路已用 DeepSeek 实测。

```
gradle :app:assembleDebug + :app:testDebugUnitTest
-> BUILD SUCCESSFUL
-> 66 tests, 0 failures（10 个测试类）
-> app-debug.apk 62.9 MB
```

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

**复习与数据**
- 简化 SM-2：答对 1/2/4/7/15/30 天，答错 10 分钟后重做
- **统计图表**：掌握度分布（0~5 级条形图）、标签分布（题量前 8）
- 每日 9:00 复习提醒（WorkManager）
- ZIP 备份：`manifest.json` + `questions.json` + `tags.json` + `chats.json` + `images/`
- 导入幂等：按（原文 + 创建时间）判重，同一包导两次不会翻倍；备份里不含 API Key

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
- 透视矫正后的识别效果与耗时（千元机）
- 真实错题跑「AI 整理」的成功率
- 长对话下的摘要触发是否顺畅


## 更新与发布（GitHub）

App 内「设置 → 检查更新」读取 **GitHub Releases**：比对 `tag_name` 与当前版本，有新版就下载 APK 并调系统安装器。直连失败会自动依次尝试镜像前缀（默认 `ghproxy.net` / `gh-proxy.com` / `hub.gitmirror.com`）。

### 发布一个新版本

```bash
git tag v0.3.1
git push origin v0.3.1
```

推 tag 后 GitHub Actions（[`.github/workflows/release.yml`](.github/workflows/release.yml)）会自动跑单测、构建 APK、创建 Release 并上传 `wrongbook-v0.3.1.apk`。

已实测：`v0.3.0` 的 tag 触发 CI 成功，Release 页有 `wrongbook-v0.3.0.apk`（62.8 MB）；直连与 `ghproxy.net` 镜像均可下载。

也可以手动触发：仓库 → Actions → Build & Release APK → Run workflow。

### 需要改的地方

| 位置 | 作用 |
| --- | --- |
| `app/build.gradle.kts` 的 `versionName` / `versionCode` | 版本号；`versionCode` 必须递增 |
| `GITHUB_OWNER` / `GITHUB_REPO` 的 `buildConfigField` | 更新源仓库，改成你的账号 |
| `UPDATE_MIRRORS` 的 `buildConfigField` | 镜像前缀，逗号分隔；留空则只走直连 |

### 注意

- 发布的 APK 与已安装版本必须**同签名**才能覆盖安装。仓库里提交了固定开发签名 `keystore/wrongbook-dev.jks`（密码公开，仅内测用），本地与 CI 共用，所以内测包之间可以互相升级；**上架前请换成自己的正式证书**并删掉这个 jks。
- 私有仓库的 Release API 需要 token，**不要**把 token 打进 App；要自动更新就用公开仓库。
- 首次安装需要用户在系统里允许「安装未知应用」；更新页提供了一键跳转。

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
