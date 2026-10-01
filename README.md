# 安全护盾 (Antivirus M3)

一款基于 Material 3 设计的安卓杀毒软件，采用 Jetpack Compose 构建。

## 下载

[📥 下载 APK v1.0.0](https://github.com/chumianyi/antivirus-m3/releases/download/v1.0.0/antivirus-m3-v1.0.0.apk)

## 功能特性

### 三种防护模式

| 模式 | 所需权限 | 防护等级 | 说明 |
|------|----------|----------|------|
| 日常守护 | 悬浮窗 + 无障碍 | 基础 | 拦截已知病毒应用，超风险自动删除，危险应用弹窗确认 |
| 狂轰乱炸 | Shizuku + 基础权限 | 高级 | 更强的检测与拦截，高风险应用自动删除 |
| 灭霸模式 | 无线调试(ADB) | 终极 | 系统级控制，终端风格激活页面，最强防护 |

### 病毒扫描
- 内置超大免费病毒库（2500+ 条恶意应用特征）
- 120+ 条权限风险评估规则
- 扫描所有已安装应用
- 实时显示扫描进度和当前应用
- 风险报告与一键处理

### 进程管理
- 查看所有运行中的进程
- 支持终止选中进程（需 Shizuku 权限）
- 显示 PID、用户、内存占用

### 其他
- Material 3 动态配色主题
- 深色/浅色模式支持
- 无障碍服务实时防护
- 通知栏 RemoteInput 激活码输入
- 终端风格灭霸模式激活页面

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **最低 SDK**: 24 (Android 7.0)
- **目标 SDK**: 34 (Android 14)
- **Shizuku API**: 13.1.5
- **数据存储**: DataStore Preferences
- **序列化**: kotlinx.serialization

## 权限说明

- `SYSTEM_ALERT_WINDOW` - 悬浮窗权限，用于防护提示
- `BIND_ACCESSIBILITY_SERVICE` - 无障碍服务，用于实时检测前台应用
- `POST_NOTIFICATIONS` - 通知权限，用于防护和扫描通知
- `QUERY_ALL_PACKAGES` - 查询所有已安装应用
- `REQUEST_DELETE_PACKAGES` - 请求删除应用
- `KILL_BACKGROUND_PROCESSES` - 终止后台进程

## 构建

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

APK 输出路径: `app/build/outputs/apk/`

## GitHub Actions

推送代码到 main 分支后，GitHub Actions 会自动编译 APK 并上传为 Artifact。

## 许可证

MIT License - 仅供学习研究使用

## 免责声明

本项目仅供学习和研究用途。病毒库数据为模拟数据，不构成真实的安全防护建议。使用本软件造成的任何后果由使用者自行承担。
