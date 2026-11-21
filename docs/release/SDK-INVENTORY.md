# 第三方 SDK / 依赖清单（上架材料草稿）

> 国内应用市场（华为/小米/OPPO/vivo/应用宝）与 Google Play 都要求申报第三方 SDK。
> 下表按「上架必需」的粒度整理，含用途与是否收集个人信息。**最终以实际打包的依赖为准**，
> 每次升级依赖后跑一遍：

```powershell
gradle :app:dependencies --configuration releaseRuntimeClasspath > dependencies.txt
```

## 一、会发起网络请求 / 可能涉及个人信息的

| SDK / 库 | 版本 | 用途 | 是否收集个人信息 | 备注 |
| --- | --- | --- | --- | --- |
| OkHttp | 5.5.1 | 访问用户自己配置的大模型接口 | 否（只发送用户主动输入的内容） | 请求目标由用户配置，不是我们固定的服务器 |
| 用户配置的大模型服务 | 用户决定 | AI 对话 / 整理 / 出题 | 是（发送文本与对话历史） | DeepSeek、通义、智谱、自建网关等；需在政策中说明 |

**本项目没有接入**：统计（友盟/神策）、崩溃上报（Bugly/Firebase Crashlytics）、推送（极光/个推）、广告、支付、地图。

> 如果后续要加崩溃上报，必须先在隐私政策里披露、并提供关闭开关，否则上架审核会被打回。

## 二、设备能力类（不联网、不上传）

| SDK / 库 | 版本 | 用途 | 说明 |
| --- | --- | --- | --- |
| ML Kit Text Recognition（中文） | `com.google.mlkit:text-recognition-chinese:16.0.1` | 本地 OCR | **捆绑版**，模型随 APK 安装，识别完全在本机，不联网、不上传 |
| CameraX | 1.7.0 | 拍照 | 仅相机预览与拍摄 |
| Coil | 3.6.3 | 本地图片加载 | 只读本机文件 |
| Room | 2.8.5 | 本地数据库 | 数据不出设备 |
| Hilt / Dagger | 2.60.1 | 依赖注入 | 无运行时网络行为 |
| androidx.work | 2.12.0 | 每日提醒 | 本地定时任务 |
| Markwon + JLatexMath | 4.6.2 / 0.2.0 | Markdown / 公式渲染 | 纯本地渲染 |
| kotlinx.serialization | 1.13.0 | JSON 解析 | 纯本地 |

## 三、许可证

以上依赖以 Apache-2.0 / MIT 为主，允许闭源商用。
上架前建议在「设置 → 关于」加一页「开源许可」，可用
[`cashapp/licensee`](https://github.com/cashapp/licensee) 自动生成；此项未包含在当前版本中。

## 四、待办

- [ ] 跑一次 `dependencies` 命令，核对最终清单与上表一致
- [ ] 确认 ML Kit 的 Google 条款在你目标市场是否需要额外声明
- [ ] 若加图标库（Lucide 等），确认许可证并加入本表
- [ ] 生成「开源许可」页面
