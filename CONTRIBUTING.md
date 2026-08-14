# 参与贡献

感谢你有兴趣为星图账本（StarLedger）做出贡献！

## 行为准则

请先阅读 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。我们希望每位参与者都能在友好、尊重的环境中交流。

## 如何贡献

### 报告问题

在 GitHub Issues 中提交问题时，请包含：

- 设备型号与 Android 版本
- 复现步骤
- 期望行为与实际行为
- 相关截图（如有）

### 提交代码

1. Fork 仓库并创建分支：`git checkout -b feature/你的功能`
2. 遵循现有代码风格（Kotlin 官方规范）
3. 为纯逻辑代码补充单元测试（如分配引擎、预算计算、星图引擎）
4. 提交前运行构建：`./gradlew assembleDebug testDebugUnitTest`
5. 提交 Pull Request 并描述改动原因

### 设计原则

本项目坚持以下原则，请在贡献时遵守：

- **安静优先**：默认不打扰用户，复杂功能由用户主动触发
- **简单优先**：一次普通支出 5 秒内完成记录
- **不制造羞耻感**：不使用"失败""不自律"等表达
- **允许中断**：用户几个月不用，星星和星座也不会消失
- **本地优先**：所有核心功能不依赖网络与账号

## 技术栈

Kotlin · Jetpack Compose · MVVM · Room · DataStore · Hilt · Coroutines

## 许可证

本项目使用 GPL-3.0-or-later 许可证。提交代码即表示你同意在该许可证下发布你的贡献。
