# 任务卡（P1 ~ P8）

用法：**一次只贴一张卡**给智能体，做完停下，你跑一次编译/单测，通过后再贴下一张。
每张卡都自带「交付 / 约束 / 自测 / 停手条件 / 汇报格式」，粘贴时连同表头一起给。

---

## P1 · 数据层

> ✅ **已完成（2026-11-20）**：`QuestionDao/TagDao/QuestionTagCrossRefDao`、`QuestionRepository`、`TagRepository`、
> `WbResult`、`ReviewScheduler`（SM-2 简化）、`TagTreeBuilder`、`TagFilter` 全部落地；
> `gradle :app:testDebugUnitTest` → 15 个用例全绿；DAO 仪器测试已在 `androidTest` 编译通过（未连真机）。
> 这一张卡保留作为「验收标准参照」，不需要再交给智能体重做。

```markdown
【任务 P1 · 数据层】只做这一阶段，做完停下等我验证。

交付：
1. Dao：
   - QuestionDao：insert / update / deleteById / observeAll / observeById / search(keyword)
     / observeByTagIds(tagIds, matchAll: Boolean) / observeDue(now)
   - TagDao：insert / update / deleteById / observeAll / observeByParent / countQuestions(tagId)
   - QuestionTagCrossRefDao：insertAll / deleteByQuestionId / deleteByTagId
   - 关系查询：data class QuestionWithTags（@Relation + @Junction）
2. Repository：QuestionRepository / TagRepository 接口 + Impl，返回自定义 WbResult<T>，不抛裸异常。
3. 单元测试（放 src/test，纯 JVM）：
   - 多标签 AND / OR 筛选语义（可用纯函数把 tagIds 转成查询参数，再断言）
   - SM-2 简化调度的间隔计算（先写纯函数 ReviewScheduler.next(mastery, now)）
4. DAO 行为测试（放 src/androidTest，用 Room.inMemoryDatabaseBuilder）：
   - 删标签后题目仍在、关联消失
   - 删题后关联消失

约束：
- 不写 fallbackToDestructiveMigration；本阶段不改表结构。
- Flow 查询要 distinctUntilChanged；不要在 DAO 里返回可空的大对象。
- 加 room-testing 与 junit 依赖时，同步写进 libs.versions.toml。

自测：assembleDebug + testDebugUnitTest 通过，androidTest 代码可编译。

停手条件：编译或单测不过，或发现需要改表结构 —— 停下来说明，不要自行改表。
```

---

## P2 · 设置与大模型接入

> ✅ **已完成（2026-11-20）**：多套 Profile、Keystore 加密、测试连接、OpenAI 兼容客户端、错误码映射；待真实 Key 联调。


```markdown
【任务 P2 · 设置与 API】只做这一阶段。

交付：
1. ApiProfileDao + SettingsRepository（读写 Profile 列表、切换默认）。
2. CryptoStore：Android Keystore（AES/GCM）+ EncryptedSharedPreferences，只落密文；
   提供一个只在内存中短暂持有明文的 ApiKeyProvider，禁止把明文传给任何日志。
3. LlmClient 抽象 + 两个实现：
   - OpenAiCompatLlmClient（OkHttp，/chat/completions，非流式先跑通）
   - DeepSeekLlmClient（baseUrl 命中 api.deepseek.com 时使用官方 SDK）
   统一错误映射：401/403 → Key 无效；429 → 限流；超时 → 网络；5xx → 服务端。
4. 设置页 UI：Profile 列表 / 新增 / 编辑（名称、baseUrl、Key、chatModel、visionModel）
   / 设为默认 / 删除（二次确认）/「测试连接」按钮（显示耗时与结果）。
5. 能力探测：visionModel 为空时，把 profile.canVision 置为 false，供 P3/P7 降级判断。

约束：
- Key 不得出现在 Log、异常 message、导出文件；错误提示只说「Key 无效」，不回显 Key。
- 网络超时 30s，连接超时 10s；流式接口本阶段可以先留 TODO。
- 测试连接发一条 max_tokens=8 的请求，不要把用户的 Key 拼进 URL。

自测：单测覆盖错误映射与 JSON 解析（用本地 JSON 字符串，不联网）；
TODO(人验)：用真实 Key 跑一次测试连接与一次对话，记录耗时。

停手条件：需要真实 Key 才能继续验证时，说明「已到人验点」并停下。
```

---

## P3 · 拍照与本地 OCR 切分

> ✅ **已完成**：CameraX 拍照、ML Kit 中文 OCR、题号正则 + 行距切分，9 个切分单测通过；准确率待真机验。


```markdown
【任务 P3 · 拍照/OCR】只做这一阶段。

交付：
1. 全屏拍照页（CameraX）：预览、快门、闪光灯、点击对焦；权限按需申请，拒绝后给「手动输入题目」兜底。
2. ImageCapture → Bitmap：按 EXIF 旋转矫正；长边先下采样到 ≤2000px 再送 OCR。
3. OcrEngine 实现：ML Kit 中文捆绑版（com.google.mlkit:text-recognition-chinese），
   输出行级数据：text + boundingBox + 置信度（置信度拿不到就置 0.5 并注释说明）。
4. LocalQuestionSegmenter：
   - 按行 Y 排序，用行高/行距中位数识别段落断点
   - 题号正则：^\s*(\d{1,3})\s*[.、．)）]，^\s*[（(]\s*(\d{1,3})\s*[）)]，
     大题号 ^\s*([一二三四五六七八九十]{1,3})\s*[、.．] 作为分组标题
   - 每个题号行到下一个题号行之前合并 boundingBox，输出归一化 RectF（0..1）
5. 单元测试：构造固定的「行 + bbox」列表，断言切出 N 题、题号正确、归一化坐标在 0..1 且不越界。

约束：
- 不引入 OpenCV；透视矫正在 P8。
- 切分算法里不出现 Android 类型（Context/Bitmap 之外），保证可在 JVM 上测。
- 识别跑到 IO 线程；大图处理要注意 OOM（用 inSampleSize 分步解码）。

自测：单测全绿。
TODO(人验)：用 3 张真实错题照片跑，记录「切出题数 / 漏题数 / 误切数」，准确率 <80% 时把失败照片的
OCR 行文本贴回来。

停手条件：单测不过，或需要真机才能继续验证效果 —— 说明并停下。
```

---

## P4 · 多选框选与入库

> ✅ **已完成**：Canvas 多选、裁剪压缩入库、题库列表、题目详情；待真机验。


```markdown
【任务 P4 · 多选入库】只做这一阶段。

交付：
1. 结果页：原图上叠 Canvas 画所有题目矩形框 + 题号角标；
   点框切换选中；全选 / 反选 / 按题号区间选；长按拖拽手动补框（框错兜底）。
   底部固定条：已选 N 题 +「下一步」。
2. 裁剪入库：归一化坐标 × 原图尺寸 → 裁剪 → 长边 ≤1600px 转 WebP 存 filesDir/questions/
   → 生成 200px 缩略图 → 每题一条 QuestionEntity（source=SCAN，rawOcrText 保存）。
3. SaveQuestionsUseCase：写文件 + 写库放在一个事务语义里；任一步失败要清理已写文件。
4. 题库列表接真数据（Coil 加载缩略图）+ 题目详情页（大图 + OCR 文本，可编辑）。

约束：
- 图片路径存相对 filesDir 的路径，不要存绝对路径。
- 入库过程不要阻塞主线程；进度用状态驱动 UI。
- 单测覆盖：归一化→像素坐标换算（含除零、越界裁剪）。

自测：单测绿；手动（人验）：选 3 题入库，题库显示 3 条、缩略图正确、删一条后图片文件也删掉。

停手条件：需要真机看图/看性能时停下。
```

---

## P5 · 标签与收藏夹

> ✅ **已完成**：标签 CRUD、收藏夹标记、多标签 AND/OR 筛选、搜索。


```markdown
【任务 P5 · 标签/收藏夹】只做这一阶段。

交付：
1. 标签管理页：新建 / 重命名 / 改色 / 设为收藏夹（isFolder）/ 二级分组（parentId）/ 排序 / 删除。
2. 打标签：题目详情页与入库后的确认页都能多选标签（底部弹层，可即时新建）。
3. 题库筛选栏：横向标签 Chip，多选；提供 AND / OR 切换；收藏夹作为可展开分组。
4. 收藏夹 = isFolder=true 的标签，列表页里单独一块入口，不新增表。
5. 删除标签：二次确认文案「将从 N 道题中移除该标签，题目不会被删除」。

约束：
- 标签名唯一：冲突时提示已存在并高亮，不静默覆盖。
- 删标签只删关联；有测试保证题目数量不变。
- 筛选逻辑放 domain（TagFilterUseCase），UI 只传参数。

自测：单测覆盖 AND/OR 筛选与「删标签不删题」；TODO(人验)：一题打 3 个标签，用两个标签 AND 筛选命中它。

停手条件：需要改表结构才能实现的功能（如三级分组）——先提问，不要自行改表。
```

---

## P6 · AI 对话

> ✅ **已完成**：会话、SSE 流式、Markdown/LaTeX、上下文截断与摘要、AI 生成标识；待真 Key 验。


```markdown
【任务 P6 · AI 对话】只做这一阶段。

交付：
1. ChatDao + ChatRepository：会话 CRUD、消息按时间排序、置顶、搜索标题。
2. 会话列表页：新建 / 重命名 / 置顶 / 删除（二次确认）。
3. 对话页：
   - SSE 流式输出（OkHttp EventSource 手写解析），逐字追加，可中断/重发
   - Markdown 渲染（Markwon）+ LaTeX 公式（选一个能在 Android 上跑通的方案，
     引入前先说明体积与许可，让我确认）
   - 附图提问（从相册选 / 从题库选一道题带入图片 + OCR 文本）
   - AI 消息角标「AI 生成」，失败重试按钮
4. 上下文管理：保留最近 N 轮 + 系统提示词 + 当前题目上下文；
   超过窗口做摘要压缩（一次额外的非流式请求），摘要失败则直接截断并提示。
5. 未配置 Key：入口显示引导 + 跳转设置，不弹错误对话框。

约束：
- 流式不能用 runBlocking；用 Flow，ViewModel 里 collect。
- 消息先落库再发请求（避免崩溃丢消息），失败时标记 status=failed。
- 不要在 UI 层拼 prompt，放 domain 的 PromptBuilder。

自测：单测覆盖 SSE 解析（多段 chunk、跨包断行、[DONE]、错误帧）。
TODO(人验)：真机看流式是否卡顿、公式渲染、长对话是否触发摘要。

停手条件：LaTeX 方案需要我拍板时停下提问。
```

---

## P7 · AI 整理题目与同类题

> ✅ **已完成**：JSON 回填、同类题生成入库、三段式容错，解析单测 9 个；成功率待真 Key 验。


```markdown
【任务 P7 · AI 整理】只做这一阶段。

交付：
1. RefineQuestionUseCase：输入题目图（可选）+ OCR 文本，输出严格 JSON：
   {"stem": "...", "answer": "...", "analysis": "...", "subject": "...", "knowledgePoints": ["..."], "confidence": 0.0~1.0}
2. 三段式容错：JSON 解析 → 失败重试一次（附「只输出 JSON」提示）→ 仍失败用本地规则兜底（原文即 stem）+ 提示用户。
3. 回填：题目的 refinedText / answer / analysis / subject，回填前展示可编辑预览，用户确认才写库。
4. 生成同类题：按知识点生成 1~3 道变式题，可在预览里删改，确认后存为新题（source=AI_CHAT）。
5. 单测：JSON 解析要能处理 ```json 包裹、前后废话、字段缺失、截断的半截 JSON。

约束：
- 解析用 kotlinx.serialization + 宽松（ignoreUnknownKeys）+ 手动截取首个 { 到最后一个 }。
- 重试只重试一次，避免烧 token。
- 所有 AI 结果都要能「转存为题目」或「追加为笔记」，不能只显示不落地。

自测：解析单测通过。
TODO(人验)：真实题目跑 10 次，记录成功次数（目标 ≥9/10）。

停手条件：需要视觉能力但当前 Profile 无 visionModel —— 报告并走纯文本路径。
```

---

## P7.5 · 导出与导入

> ✅ **已完成**：ZIP 导出/导入、判重幂等、版本校验，往返单测通过。


```markdown
【任务 P7.5 · 导出/导入】只做这一阶段。

交付：
1. 导出 ZIP（SAF 让用户选目录，不申请存储权限）：
   manifest.json（版本号、导出时间、题目数）+ questions.json + tags.json + images/<uuid>.webp + thumbs/
2. 导入：选 ZIP → 校验 manifest 版本 → 逐条插入（图片写到新文件名，避免覆盖）→ 进度条 → 失败可重试。
3. 冲突策略：按（createdAt + imageHash）判定重复，用户可选「跳过」或「全部新建」。
4. 单测：序列化 round-trip；版本号不兼容时给出明确错误。

约束：
- 导出文件里绝不能出现 API Key。
- 导入要幂等：同一個包导两次，第二次全部跳过。
- 大包（>200MB）要流式写，不要把整个 ZIP 读进内存。

自测：单测绿；TODO(人验)：导出 → 清空数据 → 导入 → 数量与图片一致。

停手条件：需要改表结构（例如加 imageHash 字段）时先提问。
```

---

## P8 · 巩固、打磨与上架准备

> ✅ **代码与材料草稿已完成**：复习队列、每日提醒、隐私政策/SDK 清单/上架检查表；图标、备案、真机验收待你定稿。


```markdown
【任务 P8 · 巩固/打磨】拆成三次交付，每次做完停下。

第一次（复习与提醒）：
- 简化 SM-2：ReviewScheduler.answer(question, correct) → 更新 masteryLevel / nextReviewAt / reviewCount
- 巩固页：今天待复习队列、答对/答错交互、统计（错题数、标签分布）
- WorkManager 每日 9:00 提醒；POST_NOTIFICATIONS 按需申请，拒绝也能用 App

第二次（体验打磨）：
- 冷启动 <1.5s；列表滚动不掉帧（Baseline Profile 可选）
- 空态/错误态/加载态全部补齐；深色模式检查
- 可选：透视矫正（四角拖拽 + 透视变换）——先说明引入 OpenCV 的体积代价，等我确认

第三次（上架材料）：
- 图标替换：把 P0 占位图标换成正式设计稿，评估 cupertino-icons 的版权风险并给替代方案
- 第三方 SDK 清单草稿（ML Kit、DeepSeek SDK、OkHttp、Coil…）
- 隐私政策正文草稿（纯本地存储、无云同步、AI 请求会发送哪些数据）
- App 备案号、包名、目标市场的合规检查表

约束：不引入新的重量级依赖；每个功能都要有失败路径文案。

停手条件：上架相关文案与图标最终定稿必须由人决定，不要自行拍板。
```
