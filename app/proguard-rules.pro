# 默认不开启混淆。上架前再打开并逐条补 keep 规则（Room / Hilt / DeepSeek SDK 都会自动带 consumer rules）。
-keep class com.chenzixin.wrongbook.data.local.entity.** { *; }
