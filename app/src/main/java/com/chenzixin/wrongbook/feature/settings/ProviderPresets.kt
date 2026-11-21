package com.chenzixin.wrongbook.feature.settings

/**
 * 免费/低价服务商预设。
 *
 * 只预填「接口地址 + 模型名」，API Key 必须用户自己去申请 ——
 * 内置共享 Key 既不合规，也会被瞬间刷爆。
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
        name = "智谱 GLM（国内直连）",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4",
        chatModel = "glm-4-flash",
        visionModel = "glm-4v-flash",
        quota = "glm-4-flash 官方长期免费；有速率限制，额度以控制台为准",
        applyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        steps = listOf(
            "打开申请页，用手机号注册 / 登录",
            "按提示完成实名认证（国内平台要求）",
            "在「API Keys」页点「新建」，复制生成的 Key",
            "回到这里，把 Key 粘到上面的输入框",
            "模型保持 glm-4-flash（识图用 glm-4v-flash），保存后点「测试连接」",
        ),
    ),
    ProviderPreset(
        name = "硅基流动 SiliconFlow（国内直连）",
        baseUrl = "https://api.siliconflow.cn/v1",
        chatModel = "Qwen/Qwen2.5-7B-Instruct",
        visionModel = "Qwen/Qwen2.5-VL-7B-Instruct",
        quota = "新用户有免费额度，部分小模型长期免费；具体以控制台为准",
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
        name = "魔搭 ModelScope（国内直连）",
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
        name = "Groq（需外网）",
        baseUrl = "https://api.groq.com/openai/v1",
        chatModel = "llama-3.3-70b-versatile",
        quota = "免费档有每日请求上限（约 1000 次/天），需网络可达",
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
        quota = "免费档每天有次数限制（约 250 次/天），需网络可达",
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
