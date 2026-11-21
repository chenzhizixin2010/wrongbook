# Errata · 液态玻璃 UI 重建说明（Liquid Glass）

> 参考 iOS 26 的 Liquid Glass：**先有会流动的光，才有玻璃**。
> 本文件说明这次视觉改版的设计语言、令牌、组件、页面落点与验证结果。
> 品牌：**Errata**（/ɪˈrɑːtə/，勘误表）+ 副标题 **AI Mistake Notebook**；包名与仓库名保持不变。
> 运行截图：[`docs/ui-screens.png`](ui-screens.png)（360×640dp / Android 12 模拟器，浅色）
> 开屏设计稿：[`docs/splash-mockup.png`](splash-mockup.png)（浅色 / 深色）\n> 设计稿：[`docs/ui-mockup.png`](ui-mockup.png) ｜ 可编辑源：[`docs/ui-mockup.html`](ui-mockup.html)
> 单页原图：[`docs/screens/`](screens/)

---

## 1. 目标

原界面是「iOS 分组列表 + 纯色白卡」：信息能读，但缺少层次、没有材质、页面之间长得一样。
这次不改信息架构与业务逻辑，只解决三件事：

1. **建立材质**：半透明、有厚度、有受光边缘的玻璃，取代不透明白卡；
2. **建立空间**：极光画布 + 悬浮标签栏，让内容从玻璃后面穿过去；
3. **建立节奏**：统一的字号/圆角/间距/投影，用同一套组件覆盖所有页面。

UI 基座仍是 Cupertino（`CupertinoTheme` / `CupertinoText` / `CupertinoIcon` / 44 个 outlined 图标），
**没有引入任何新依赖**，`minSdk 24` 不变。

---

## 2. 视觉语言

### 2.1 极光画布

玻璃的「透」必须靠背后的颜色才能被看见，所以背景不再是 `#F2F2F7` 平涂，而是四团低饱和径向渐变：

- 左上蓝 `#6AA6FF`
- 右上紫 `#C08CFF`
- 左下青 `#4FE0D0`
- 右下粉 `#FFB3D1`

四团以 28s 周期缓慢漂移（只走绘制阶段，不触发重组）。深色模式换成深蓝/深紫/墨绿，并加一层四周压暗，
让玻璃边缘的高光有对比。

### 2.2 玻璃材质的 6 层

`GlassSurface` 是所有玻璃的唯一实现，从下往上叠：

| 层 | 作用 | 实现 |
| --- | --- | --- |
| 1 | 投影（带蓝偏，不是死灰） | `Modifier.shadow`，`shadow` token |
| 2 | 上亮下暗的半透明填充（厚度） | 垂直渐变 `glassTop → glassBottom` |
| 3 | 按下整片变亮（触感） | 白色叠加，140ms |
| 4 | 左上 → 右下斜向镜面反光 | 线性渐变 `glassSheen → 透明` |
| 5 | 左上角外溢光斑（「凸」起来的关键） | 径向渐变光斑 |
| 6 | 1dp 受光外描边 + 内圈高光 | 线性描边 + 顶部内描边 |

> 真正的「背景模糊」在 Android 上需要抓取下层内容做 RenderEffect，API 24 不可用，
> 且全屏实时模糊代价高。这里用「极光本身足够模糊 + 玻璃高不透明度」逼近，
> 观感接近，滚动时不掉帧。后续若要更「液态」，见第 7 节。
>
> 代价是：内容从悬浮标签栏后面穿过时，正文会透到栏上抢读。
> 因此在栏与弹窗下面各垫了一层底衬（`barScrim` / `dialogScrim`），
> 并给底部加了一条向上淡出的画布色 scrim（`AppNavHost`），
> 保留玻璃的受光边缘与反光，同时保证文字可读。

### 2.3 形状 / 投影 / 排版

- 圆角：卡片 22–24dp、面板 28–30dp、胶囊 50%、对话框 30dp、标签栏 34dp。
- 投影：卡片 6–10dp、悬浮标签栏 26dp、主按钮 16dp 且**带强调色**（不是灰影）。
- 大标题 34sp / Bold / 字距 -0.7sp；页面 eyebrow 11sp / SemiBold / 字距 1.1sp；
  正文 15–16sp；说明 12–13sp；脚注 11–12sp。

---

## 3. 设计令牌（`ui/theme/Theme.kt`）

| Token | 浅色 | 深色 | 用途 |
| --- | --- | --- | --- |
| `accent → accentEnd` | `#0A84FF → #6C5CE7` | `#3D8BFF → #9B7BFF` | 主按钮、选中胶囊、环形图 |
| `canvasTop / canvasBottom` | `#F5F7FD / #E9EDF8` | `#08090F / #04050A` | 画布底 |
| `glassTop / glassBottom` | 白 72% / 42% | 白 16% / 5% | 玻璃填充 |
| `glassStrokeTop / Bottom` | 白 95% / 20% | 白 45% / 8% | 玻璃描边 |
| `glassSheen` | 白 80% | 白 30% | 镜面反光 |
| `glassFill` | 白 60% | 白 10% | 输入框、薄玻璃片 |
| `shadow` | `#1B2A5E` 10% | 黑 60% | 投影 |
| `tintSoft` | 蓝 12% | 蓝 18% | 选中底、图标底板 |
| `label / secondaryLabel` | `#0B0B12 / #6B6B7B` | `#F5F5FA / #9C9CB0` | 文本 |
| `success / warning / danger` | `#34C759 / #FF9F0A / #FF3B30` | `#30D158 / #FFB340 / #FF453A` | 语义色 |

业务代码只引用 `wbColors()` 的语义色，不出现裸色值。

---

## 4. 组件清单

| 组件 | 文件 | 说明 |
| --- | --- | --- |
| `WbAmbientBackground` | `ui/component/Glass.kt` | 极光画布；`animated = false` 可关动画 |
| `GlassSurface` | 同上 | 玻璃唯一实现，支持 `tint` / `glow` / `onClick` / 按压回弹 |
| `WbGlassIconButton` | 同上 | 圆形玻璃图标按钮 |
| `WbIconTile` | 同上 | 渐变图标底板（分组表头、空状态） |
| `WbGlassTabBar` | `ui/component/WbTabBar.kt` | 悬浮胶囊标签栏，选中胶囊 spring 滑动 |
| `wbTabBarSpace()` | 同上 | 页面底部预留（68 + 12 + 导航条 + 26） |
| `WbSection` | `ui/component/InsetList.kt` | 玻璃分组卡；header 可带渐变图标 |
| `WbListRow` | 同上 | 行；有 `onClick` 且无 `trailing` 时自动补 chevron |
| `WbSectionLabel` | 同上 | 卡片列表的小节标题 + 计数 |
| `WbChip` / `WbBadge` | `ui/component/WbChip.kt` | 胶囊筛选 / 只读徽标 |
| `WbTextField` | `ui/component/WbTextField.kt` | 玻璃输入框，支持 `leadingIcon` / `trailing` |
| `WbPrimaryButton` 等 | `ui/component/WbButtons.kt` | 主/次/危险/文字四语义，禁止灰字压实心底 |
| `WbAlertDialog` / `WbInputDialog` | `ui/component/WbDialogs.kt` / `WbInputDialog.kt` | 30dp 圆角玻璃弹窗 |
| `WbTopBar` / `WbLargeTitle` / `WbPageHeader` | `ui/component/WbTopBar.kt` | 二级页顶栏 / 大标题 / 主 Tab 页头 |

新增参数全部带默认值，旧调用点不需要改签名。

---

## 5. 页面落点

| 页面 | 主要改动 |
| --- | --- |
| **题库** | 玻璃搜索框 + 筛选胶囊 + 玻璃题卡（缩略图、彩色标签徽标、5 格掌握度圆点）+ 空状态 CTA |
| **扫描** | 取景框插画（四角括号 + 流动扫描线 + 渐变圆钮）+ 三块功能玻璃砖 + 流程说明 |
| **AI 对话（列表）** | 渐变会话头像、玻璃会话卡、置顶徽标、玻璃删除按钮 |
| **AI 对话（会话）** | 渐变用户气泡、玻璃 AI 气泡、欢迎建议卡、玻璃输入条（附图 / 发送 / 停止） |
| **生成** | 两个模式玻璃砖（整理错题 / AI 同类题）、题量胶囊、玻璃开关、全宽渐变生成按钮、A4 预览工具栏 |
| **设置** | 身份卡（图标 / 版本 / 模型状态）+ 带图标的分组列表 + 玻璃主题弹窗 |
| **统计** | 环形掌握度总览（渐进度）+ 三档数量胶囊 + 渐变分布条 |
| **题目详情** | 玻璃包边原图、可选文字、答案/解析分层、标签徽标、掌握度柱、AI 工具行 |
| **标签** | 单卡分组列表（色点 / 收藏夹徽标 / 排序），玻璃编辑弹窗（色板 / 开关 / 步进器） |
| **导入 / 导出 / 更新** | 玻璃说明卡 + 全宽主按钮 + 渐进度条 + 版本徽标 |
| **相机 / 透视** | 全屏出血；顶部压暗 + 返回键 + 四角取景框；黑底胶囊快门；状态栏图标强制浅色并在退出时恢复 |

底部导航从「通栏 `CupertinoNavigationBar`」换成 `WbGlassTabBar`；
`CupertinoScaffold` 一并移除，改成 `Box + NavHost + 悬浮栏`，
安全区由 `WbRoute` 分页处理：主 Tab 只避状态栏、二级页上下都避、相机全出血。

---

## 5.5 开屏与品牌（Errata）

- **系统启动窗口**：`windowBackground` 用「极光渐变 + 品牌 E 标记」的 layer-list；Android 12+ 直接使用原生的
  `windowSplashScreenBackground` / `windowSplashScreenAnimatedIcon` 两个属性，**不引入 core-splashscreen**。
- **Compose 开屏** `ErrataSplash`：128dp 玻璃标记 + 衬线 `Errata` 字标 + `AI MISTAKE NOTEBOOK` + 中文 tagline
  「把每一道错题，改对。」；标记 spring 0.92→1，文字逐层淡入上移，1.2s 后 420ms 淡出。
- **不做初始化**：开屏只是盖在已经渲染好的主界面之上，不阻塞冷启动；`rememberSaveable` 保证旋转/切主题不重播。
- **桌面图标**：同一枚 E 标记（蓝紫渐变 + 白色 E），与开屏、系统启动画面形成同一条视觉路径。
- **命名**：显示名 Errata（/ɪˈrɑːtə/，勘误表），副标题 AI Mistake Notebook；包名、仓库名、备份格式保持不变。

---

## 6. 动效

| 动效 | 参数 |
| --- | --- |
| 极光漂移 | 28s，`LinearEasing`，`RepeatMode.Reverse` |
| 玻璃按压 | 缩放 0.975，spring（damping 0.8 / stiffness 700），140ms 变亮 |
| 标签栏选中胶囊 | spring（damping 0.82 / stiffness 380）横移 |
| 标签栏显隐 | 320ms 滑入 + 220ms 淡入；退出 200ms |
| 页面转场 | 220ms 淡入 / 160ms 淡出（不做横向推，避免和玻璃层叠冲突） |

---

## 7. 约束与取舍

- **没有新增第三方库**：模糊、渐变、噪点全部用 Compose 自带能力；避免引入 Haze 等未验证依赖。
- **API 24 兼容**：`Modifier.shadow` 的彩色投影在 API 28+ 生效，低版本退化为灰色投影；
  没有使用 `Modifier.blur`（API 31+）与 `RenderEffect`。
- **Cupertino 基座保留**：文本、图标、主题色方案不变，自定义的只是容器与按钮外观。
- **深色模式**：玻璃换成暗色磨砂；`values-night/themes.xml` 防止冷启动白闪；
  强制主题时同步系统栏图标颜色（`MainActivity` / 相机页各自处理）。
- **性能**：极光动画只触发绘制；低端机可在 `WbAmbientBackground(animated = false)` 关掉。
  TODO(人验)：中低端机连续滚动题库与对话时的掉帧情况。

---

## 8. 验证

```powershell
gradle :app:assembleDebug :app:testDebugUnitTest
```

- `assembleDebug`：通过。
- `testDebugUnitTest`：通过，83 个 JVM 用例 0 失败。
- **实机（模拟器）已验证**：Android 12 / API 32、360×640dp、1080×1920。
  - 5 个主 Tab、题目详情、手动建题、设置弹窗、深色模式、相机取景页均已截图；
  - 见 [`docs/ui-screens.png`](ui-screens.png) 与 [`docs/screens/`](screens/)。
- 仍未覆盖 / 需人验：
  - TODO(人验)：玻璃在真实阳光下的对比度、低亮度屏幕的可读性；
  - TODO(人验)：极光动画在低端机上的续航/发热与连续滚动掉帧；
  - TODO(人验)：相机拍照后 OCR → 框选 → 入库的整条链路（模拟器相机为虚拟场景）；
  - TODO(人验)：WebView 打印分页与中文字体；
  - TODO(人验)：手势导航/三键导航切换时标签栏与底部 scrim 的间距。

---

## 9. 后续可做

1. 若将来 `minSdk ≥ 31`，可接入真实背景模糊（RenderNode/`RenderEffect`）替换近似玻璃。
2. 打包 SF Pro / 思源黑体可变字体，进一步贴近 iOS 字感。
3. 给主按钮与标签栏选中加轻触觉反馈（`HapticFeedback`，无需新依赖）。
4. 增加「减弱动态效果」开关，联动极光与 spring。
5. 用 Compose screenshot test 固化 5 个主页面，防止后续回归。
