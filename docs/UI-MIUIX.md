# Errata UI 规范（Miuix + iOS + 0.9.0 三皮肤）

> 2026-11 起，App 支持三套**真正独立**的界面语言，在「设置 → 外观 → 界面风格」实时切换，不需要重启：
> - **澎湃 OS（Miuix）**：HyperOS 内嵌列表、贴底 NavigationBar、纯色卡片、Monet 动态取色。
> - **iOS（Cupertino）**：SF 字阶、inset grouped 列表、玻璃卡片、Cupertino 开关、iOS TabBar。
> - **0.9.0（液态玻璃）**：极光背景、悬浮胶囊 TabBar、24dp 玻璃卡、蓝紫渐变按钮、30sp 标题。
>
> 三套皮肤共用同一套页面逻辑与 ViewModel，不复制页面。旧版单一液态玻璃稿归档在 `docs/legacy/`。

## 1. 切换方式

1. 打开「设置」。
2. 进入「外观 → 界面风格」。
3. 选择「澎湃 OS」「iOS」或「0.9.0 液态玻璃」，立即生效；选择会持久化到 DataStore。

主题（跟随系统/浅色/深色）与界面风格是**两个独立维度**，可以组合出 6 种常用状态。

## 2. 设计原则

1. **共用结构，分离表现**：页面只用 `WbSection / WbListRow / WbPageHeader / WbTopBar / WbTextField / WbButtons / WbDialogs` 等语义组件；组件内部按 `LocalUiStyle` 分支。
2. **Miuix 侧纯色**：禁止线性/径向渐变、玻璃模糊；颜色只来自 `MiuixTheme.colorScheme`。
3. **iOS 侧轻玻璃**：允许柔和渐变、玻璃高光与投影（仅限 iOS 皮肤），但禁止大面积蓝紫渐变。
4. **字号**：Miuix 最大 28sp（用 `textStyles`）；iOS 用 34/28/22/20/17/16/15/13/12/11 字阶；0.9.0 用 30sp 标题 + 原始字阶。
5. **列表**：一个 Card 只装一组行，行之间用 `WbDivider`；不做「一行一卡」。
6. **底部导航**：Miuix / iOS 贴底；0.9.0 用悬浮胶囊 TabBar（复古皮肤专属）。

## 3. 组件地图

| 场景 | Miuix 分支 | iOS 分支 |
| --- | --- | --- |
| 页面大标题 | `WbPageHeader` 28sp | `WbPageHeader` 34sp largeTitle |
| 二级页顶栏 | 56dp + 居中标题 | 44dp nav bar + 蓝色返回 |
| 分组 | SmallTitle + 16dp Card | uppercase 小标题 + 12dp 玻璃卡 |
| 列表行 | `BasicComponent` | 44dp 行 + 17/13sp + 右侧 chevron |
| 按钮 | 16dp 圆角实心 Button | 胶囊 filled / tinted / plain |
| 开关 | `SwitchPreference` | `CupertinoSwitch` |
| 输入 | surfaceContainer 填充 | 圆角 10 的 iOS 搜索/表单底 |
| 弹窗 | 实心 Card | 玻璃 Alert + 横向文字按钮 |
| 标签 | 纯色胶囊 | tinted 胶囊 |
| 底栏 | `NavigationBar` | 半透明 TabBar |

## 4. 页面

- **题库**：页面头 → 搜索 → 标签筛选 → 每 8 题一张 Card；空状态居中线性图标 + 主按钮。
- **扫描**：极简入口 + 「能力/流程」两组列表。
- **AI 对话**：会话分组列表；会话页顶栏 + 消息流 + 底部输入条。
- **生成**：标签 → 出题方式 → 题量/答案 → 生成按钮（随内容滚动）。
- **设置**：应用 / 模型与 API / 题库分类 / 外观 / 数据 / 关于；外观里含「界面风格」与「主题」。
- **二级页**：统一 `WbTopBar` + `WbSection`；相机/透视页固定深色，不受皮肤影响。

## 5. 截图

![双皮肤预览](ui-screens.png)

实际模拟器截图（1080×1920）：

- `docs/screens/miuix-home-light.png`
- `docs/screens/miuix-settings-light.png`
- `docs/screens/miuix-generate-light.png`
- `docs/screens/ios-home-dark.png`
- `docs/screens/ios-settings-dark.png`
- `docs/screens/ios-chat-dark.png`\n- `docs/screens/v090-home.png`
- `docs/screens/v090-home-dark.png`\n- `docs/screens/v090-settings.png`\n- `docs/screens/v090-scan.png`
- `docs/screens/ios-generate-dark.png`
- `docs/screens/ios-theme-dialog-light.png`
- `docs/screens/style-dialog-dark.png`
- `docs/screens/splash.png`

### 0.9.0 液态玻璃专属\n\n- 极光背景 + 24dp 玻璃卡 + 悬浮胶囊底栏。\n- 主按钮用蓝→紫渐变；这是 0.9.0 的视觉记忆点。\n- 只在 V090 皮肤内启用，Miuix 侧仍严格纯色。\n\n> 截图只用于 UI 走查；相机画面来自模拟器虚拟摄像头，OCR 精度与真机壁纸取色仍需真机验收。
