package com.chenzixin.wrongbook.feature.settings

/**
 * 免费/低价服务商预设。
 *
 * 只预填「接口地址 + 模型名」，API Key 必须用户自己去申请 ——
 * 内置共享 Key 既不合规，也会被瞬间刷爆。
 *
 * 各家的免费政策变动很快，`quota` 只是写个大致方向，最终以官网为准。
 */
data class ProviderPreset(
    val name: String,
    val baseUrl: String,
    val chatModel: String,
    val visionModel: String = "",
    val quota: String = "",
    val applyUrl: String = "",
    val needsVpn: Boolean = false,
    val steps: List<String> = emptyList(),
)

val PROVIDER_PRESETS = listOf(
    ProviderPreset(
        name = "火山方舟 · 豆包",
        baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
        chatModel = "doubao-seed-1-6-250615",
        visionModel = "doubao-seed-1-6-250615",
        quota = "学生认证后 doubao-seed-1-6 可长期免费用，官方称不限次数与 Token；模型 ID 带日期后缀",
        applyUrl = "https://console.volcengine.com/ark",
        steps = listOf(
            "打开方舟控制台，用手机号注册并完成实名认证",
            "如果在读，做一次学生认证（这是「不限量免费」的前提）",
            "进「开通管理」，开通 doubao-seed-1-6（或更新的 seed 系列）",
            "到「API Key 管理」创建 Key 并复制",
            "回填模型 ID：控制台里形如 doubao-seed-1-6-250615，日期后缀要一起复制",
            "保存后点「测试连接」；报 404 多半是模型 ID 不对或没开通",
        ),
    ),
    ProviderPreset(
        name = "智谱 GLM",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4",
        chatModel = "glm-4.7-flash",
        visionModel = "glm-4v-flash",
        quota = "GLM-4.7-Flash 永久免费；教育邮箱认证还能额外领 10 万 Token",
        applyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        steps = listOf(
            "打开申请页，手机号注册 / 登录",
            "完成实名认证（国内平台要求）",
            "在「API Keys」页新建并复制 Key",
            "用教育邮箱做一次学生认证，可再领 10 万 Token",
            "模型填 glm-4.7-flash（识图用 glm-4v-flash）",
            "保存 → 测试连接",
        ),
    ),
    ProviderPreset(
        name = "美团 LongCat",
        baseUrl = "https://api.longcat.chat/openai/v1",
        chatModel = "LongCat-Flash-Lite",
        quota = "每天 5000 万 Token（Lite 独享）+ 500 万（其他模型共享），自然日刷新",
        applyUrl = "https://longcat.chat/platform/api_keys",
        steps = listOf(
            "打开 LongCat 开放平台，手机号登录",
            "进入 API Keys 面板，创建并复制 Key",
            "模型填 LongCat-Flash-Lite（额度最猛）",
            "想用更强模型可改 LongCat-Flash-Chat / Thinking",
            "保存 → 测试连接；报 401 说明 Key 复制不全",
        ),
    ),
    ProviderPreset(
        name = "阿里云百炼（通义）",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        chatModel = "qwen-flash",
        visionModel = "qwen-vl-max",
        quota = "新用户有百万级 Token 试用额度；学生认证可能额外赠送",
        applyUrl = "https://bailian.console.aliyun.com/",
        steps = listOf(
            "登录阿里云，开通「百炼」大模型服务",
            "在 API-KEY 页面创建 Key 并复制",
            "模型填 qwen-flash（识图用 qwen-vl-max）",
            "注意 baseUrl 要用 compatible-mode 这条，不是 dashscope 原生接口",
            "保存 → 测试连接；报 401 检查 Key 是否为北京地域",
        ),
    ),
    ProviderPreset(
        name = "硅基流动 SiliconFlow",
        baseUrl = "https://api.siliconflow.cn/v1",
        chatModel = "Qwen/Qwen2.5-7B-Instruct",
        visionModel = "Qwen/Qwen2.5-VL-7B-Instruct",
        quota = "新用户注册约送 2000 万 Token，部分小模型长期免费",
        applyUrl = "https://cloud.siliconflow.cn/account/ak",
        steps = listOf(
            "打开申请页注册，完成实名",
            "在「API 密钥」页新建密钥并复制",
            "粘贴到上面的 Key 输入框",
            "模型填 Qwen/Qwen2.5-7B-Instruct（识图可试 VL 模型）",
            "保存 → 测试连接；报 401 多半是 Key 没复制全",
        ),
    ),
    ProviderPreset(
        name = "魔搭 ModelScope",
        baseUrl = "https://api-inference.modelscope.cn/v1",
        chatModel = "Qwen/Qwen2.5-7B-Instruct",
        quota = "阿里魔搭的免费推理接口，需绑定阿里云账号，有调用频率限制",
        applyUrl = "https://modelscope.cn/my/myaccesstoken",
        steps = listOf(
            "打开申请页，登录并绑定阿里云账号",
            "在「访问令牌」页创建 SDK 令牌",
            "复制令牌，粘贴到上面的 Key 输入框",
            "模型填 Qwen/Qwen2.5-7B-Instruct",
            "保存 → 测试连接",
        ),
    ),
    ProviderPreset(
        name = "DeepSeek",
        baseUrl = "https://api.deepseek.com/v1",
        chatModel = "deepseek-flash",
        visionModel = "deepseek-flash",
        quota = "新用户有赠送额度（以官网活动为准）；deepseek-flash 支持图片输入",
        applyUrl = "https://platform.deepseek.com/api_keys",
        steps = listOf(
            "打开 DeepSeek 开放平台注册（免绑卡）",
            "在 API keys 页创建并复制 Key",
            "模型填 deepseek-flash（支持文本+识图），或 deepseek-v4-pro（纯文本）",
            "保存 → 测试连接",
        ),
    ),
    ProviderPreset(
        name = "Groq（需外网）",
        baseUrl = "https://api.groq.com/openai/v1",
        chatModel = "llama-3.3-70b-versatile",
        quota = "免费档约 1000 次/天，需网络可达",
        applyUrl = "https://console.groq.com/keys",
        needsVpn = true,
        steps = listOf(
            "确认网络能访问 console.groq.com",
            "注册后进入 API Keys，点 Create API Key",
            "复制 Key（只显示一次），粘贴到上面",
            "模型填 llama-3.3-70b-versatile",
            "保存 → 测试连接；超时通常是网络问题",
        ),
    ),
    ProviderPreset(
        name = "OpenRouter 免费模型（需外网）",
        baseUrl = "https://openrouter.ai/api/v1",
        chatModel = "deepseek/deepseek-chat-v3.1:free",
        quota = "只有带 :free 后缀的模型免费；免费档约 50 次/天，模型 ID 会变",
        applyUrl = "https://openrouter.ai/keys",
        needsVpn = true,
        steps = listOf(
            "确认网络能访问 openrouter.ai",
            "注册后在 Keys 页创建 Key 并复制",
            "粘贴到上面；模型名必须以 :free 结尾",
            "到 openrouter.ai/models 搜「free」挑当前可用的模型",
            "保存 → 测试连接；报 402 说明该模型已不免费",
        ),
    ),
    ProviderPreset(
        name = "Gemini（需外网）",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
        chatModel = "gemini-2.5-flash",
        quota = "免费档每天有限次（约 250 次/天），需网络可达",
        applyUrl = "https://aistudio.google.com/app/apikey",
        needsVpn = true,
        steps = listOf(
            "确认网络能访问 aistudio.google.com",
            "登录 Google 账号，点 Get API key → 创建",
            "复制 Key，粘贴到上面",
            "模型填 gemini-2.5-flash",
            "保存 → 测试连接；注意 baseUrl 结尾的 /openai/ 不能丢",
        ),
    ),
)
