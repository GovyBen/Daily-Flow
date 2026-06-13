# Secret 与日志安全审计

审计日期：2026-06-13

## Secret 清单

云端 AI Provider 的 API Key 是当前唯一由用户输入并长期保存的 secret：

- OpenAI、DeepSeek、通义千问、Kimi、智谱 GLM；
- Gemini、Anthropic、OpenRouter。

Ollama 与 LM Studio 默认不需要 API Key。

## 旧读写路径

旧实现把 Provider Key 直接保存到 `settings_preferences` DataStore：

- 设置页通过 `SavePreferenceUseCase` 写入；
- `AiRepositoryImpl` 通过 `GetPreferenceUseCase` 读取；
- preference 名称为 `openai_key`、`gemini_key` 等。

业务 JSON/Markdown 备份只导出数据库内容，不导出 DataStore；Android cloud backup
与 device transfer 也已在 `data_extraction_rules.xml` 中排除全部应用数据。尽管如此，
旧 Key 仍以明文存在于本机 DataStore，因此必须迁移。

## 当前实现

- `SecretStore` 是业务层唯一的 Key 存取接口。
- `AndroidKeystoreSecretStore` 在 Android Keystore 中生成不可导出的 AES Key。
- secret 使用 `AES/GCM/NoPadding` 加密。
- DataStore 仅保存 payload 版本、IV 和 ciphertext。
- `LegacySecretMigration` 启动时逐项迁移旧值，验证解密成功后删除明文 preference，
  最后写入幂等迁移标记。
- Keystore payload 无法解密时返回空值并删除失效 payload，要求用户重新输入。
- 云 Provider Key 为空时，`AiRepositoryImpl` 不创建网络执行器。

## 日志与异常

- Ktor Android logger 对 `Authorization`、`api-key` 和 `x-api-key` 头做统一脱敏。
- 代码不得记录 API Key、完整请求头或 SecretStore payload。
- 连接与能力测试使用 fake server 和无副作用 fake tool。

## 验证

- JVM 测试覆盖 Provider 稳定 ID、推荐排序和 `InMemorySecretStore`。
- Android 仪器测试覆盖 Keystore 往返、DataStore 无明文和旧 Key 迁移。
- `rg` 审计确认设置页和 AI 仓库不再通过普通 preference 读写 Key。
