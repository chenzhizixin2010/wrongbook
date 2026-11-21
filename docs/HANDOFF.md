# 交接说明：剩下的活能交给 AI 智能体吗？

**结论：能，但要分清「可全托管」「托管 + 你验收」「只能人做」三类。**
逻辑与数据层可以全交；相机、OCR、大模型这三块，AI 只能写代码，效果必须你真机验。

---

## 1. 任务分级

| 级别 | 内容 | 交给 AI | 你要做的 |
| --- | --- | --- | --- |
| ✅ 全托管 | P5 标签与收藏夹、P7.5 导出导入、P8 的 SDK 清单草稿 | 是 | 看汇报、抽查单测与边界 |
| ⚠️ 托管 + 你验收 | P2 设置与 API、P6 AI 对话、P7 AI 整理 | 能写，但联调要真 Key | 提供 Key、跑一次真机联调 |
| ⚠️ AI 写、你真机验 | P3 拍照 OCR、P4 多选入库、P8 透视矫正 | 是 | 拿真实错题照片判断准确率 |
| ❌ 不建议交 | UI 手感调校（间距/动效/字体）、上架材料定稿、图标版权决策 | 否 | 你自己或找设计师 |

---

## 2. 交接方式

1. 新会话开场：把 `AGENTS.md` 第 1 节的「角色」代码块 + 一句「读 AGENTS.md 与 docs/DECISIONS.md」发给智能体。
2. 然后贴 `docs/task-cards.md` 里的**一张**任务卡（P1 开始）。
3. 它做完会按固定格式汇报；你跑一次 `gradle :app:assembleDebug`（或 Android Studio 的 Build），
   过了再贴下一张。
4. 它如果问问题，答完再让它继续；**不要**让它"顺便把下一阶段也做了"。

关键在任务卡里已经写死的两条：
- 「停手条件」—— 到了人验点必须停；
- 「TODO(人验)」—— 不许把效果类结论写成"已验证"。

---

## 3. 你的验收机制（不做这三步，AI 会自我感觉良好）

| 检查 | 命令/动作 | 说明 |
| --- | --- | --- |
| 单测 | `gradle :app:testDebugUnitTest` | 逻辑层最硬的信号 |
| 编译 | `gradle :app:assembleDebug` | Cupertino / DeepSeek SDK / ML Kit 最容易抄错版本，编译报错是最高频翻车点 |
| 真机 | 拍照切分、流式对话、JSON 回填 | 只有你能判断「能不能用」 |

再加一条：每阶段要它给**变更文件清单**，你 `git diff` 扫一眼，防止它偷偷改了 P0 定好的东西。

---

## 4. 三个必须提前知道的坑

1. **DeepSeek 的多模态要实测。** SDK 文档说明支持图像输入，但「你的账号 + 你的模型 + 你的套餐」是否真的能识图，
   必须在 P2 用真 Key 跑一次。跑不通就退回「纯本地 ML Kit + 只发文本」的方案，别拖到 P7 才发现。
2. **Cupertino 是个移动靶。** 本项目用的 3.3.1 与网上教程差异很大（见 `DECISIONS.md` 第 3 节，已实测）：
   没有 `CupertinoList`、只有 44 个图标、文本组件叫 `CupertinoText`。
   AI 若报「未解析引用」，要求它去 `-sources.jar` 里查，不许自己发明 API。
3. **上架材料别交给 AI 定稿。** 隐私政策正式地址、App 备案号、图标版权替换，必须等功能定型后你拍板，
   AI 只能给草稿。

---

## 5. 本机工具链现状（已装好）

2026-11-20 已在本机装好完整工具链，并跑通了 P0 + P1：

| 工具 | 状态 |
| --- | --- |
| JDK 17 | ✅ `D:\dev-tools\jdk\jdk-17.0.2`（已写入用户环境变量 `JAVA_HOME`） |
| Gradle 9.9.2 | ✅ `D:\dev-tools\gradle-9.9.2\gradle-9.9.2` |
| Android SDK | ✅ `D:\dev-tools\android-sdk`（platform 36/37、build-tools 36/37、platform-tools） |
| git | ❌ 未安装（所以「变更文件清单」比 diff 汇报更重要） |
| 真机 / 模拟器 | ❌ 未配置（仪器测试只能编译，跑不了） |

已验证：

```
gradle :app:assembleDebug            -> BUILD SUCCESSFUL，APK 60.7 MB
gradle :app:testDebugUnitTest        -> 15 tests, 0 failures
gradle :app:assembleDebugAndroidTest -> BUILD SUCCESSFUL
```

**所以现在可以真正交给智能体「写代码 → 编译 → 跑单测」闭环了**，人只需要在效果类环节（拍照、联调、UI 手感）出手。

两个已知环境坑（详见 `DECISIONS.md` D14~D16）：
- 中文路径下 `gradlew.bat` 不可用 → 用 `gradle` 或 `scripts/build.ps1`；
- 依赖下载默认很慢 → 走 `D:\dev-tools\maven-mirrors.gradle`。

---

## 6. 建议推进顺序

```
P1 ✅已完成 → P2 设置/API（AI 写 + 你给 Key 联调）
→ P3 拍照 OCR（AI 写 + 你真机验准确率）
→ P4 多选入库（AI 写 + 你真机验）
→ P5 标签（全托管）
→ P6 AI 对话（AI 写 + 你真机验流式）
→ P7 AI 整理（AI 写 + 你真机验 JSON）
→ P7.5 导出（全托管）
→ P8 巩固/打磨/上架（AI 写代码 + 你定稿材料）
```

预计节奏：全托管阶段 1 次会话 1 个阶段；人验阶段建议「两天一阶段」，因为你得拍照、配 Key、真机看效果。
