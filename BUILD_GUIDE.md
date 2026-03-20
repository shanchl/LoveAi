# LoveAI Android App 编译指南

## 🎵 添加背景音乐

在编译前，请将《告白气球》MP3 文件放入以下位置：

```
loveai/app/src/main/res/raw/bg_music.mp3
```

**注意事项：**
- 文件名必须是 `bg_music.mp3`（小写，下划线）
- Android 资源文件名只允许小写字母、数字和下划线
- 如果不添加音乐文件，App 仍可正常运行，只是没有背景音乐

---

## 🔨 编译方式一：Android Studio（推荐）

### 步骤 1：安装 Android Studio

1. 访问 [Android Studio 官网](https://developer.android.com/studio)
2. 下载并安装最新版 Android Studio
3. 安装时选择标准安装，会自动安装 Android SDK

### 步骤 2：打开项目

1. 启动 Android Studio
2. 选择 `File → Open`
3. 导航到 `C:\Users\chang\WorkBuddy\Claw\loveai` 目录
4. 点击 `OK`

### 步骤 3：等待 Gradle 同步

首次打开项目，Android Studio 会自动下载依赖并同步项目，这可能需要几分钟。

### 步骤 4：编译 APK

**方式 A：编译 Debug APK（快速测试）**
1. 点击菜单 `Build → Build Bundle(s) / APK(s) → Build APK(s)`
2. 等待编译完成
3. 点击通知中的 `locate` 链接，或在以下目录找到 APK：
   ```
   loveai/app/build/outputs/apk/debug/app-debug.apk
   ```

**方式 B：编译 Release APK（正式发布）**
1. 点击菜单 `Build → Generate Signed Bundle / APK`
2. 选择 `APK` → `Next`
3. 创建或选择密钥库（Keystore）
4. 选择 `release` 构建变体
5. APK 输出在 `loveai/app/build/outputs/apk/release/app-release.apk`

---

## 🔨 编译方式二：命令行

### 前提条件

需要安装：
1. **Java JDK 17** 或更高版本
2. **Android SDK**

### 设置环境变量

```powershell
# 设置 ANDROID_HOME（根据实际安装路径调整）
$env:ANDROID_HOME = "C:\Users\chang\AppData\Local\Android\Sdk"
$env:Path += ";$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\tools"
```

### 编译命令

```powershell
# 进入项目目录
cd C:\Users\chang\WorkBuddy\Claw\loveai

# 编译 Debug APK
.\gradlew assembleDebug

# APK 输出位置
# loveai\app\build\outputs\apk\debug\app-debug.apk
```

---

## 📱 安装到手机

### 方式 A：通过 USB 数据线

1. 手机开启 **开发者选项** 和 **USB 调试**
2. 连接手机到电脑
3. 在 Android Studio 中点击 `Run` 按钮（绿色三角形）
4. 或使用命令：`adb install app/build/outputs/apk/debug/app-debug.apk`

### 方式 B：直接传输 APK

1. 将编译好的 APK 文件传输到手机
2. 在手机上打开 APK 文件
3. 允许安装来自未知来源的应用
4. 按提示完成安装

---

## 🎁 项目功能总结

| 功能 | 描述 |
|------|------|
| **启动页** | 3秒倒计时动画 |
| **效果展示** | 随机展示 3 个效果（从 50 个变体中随机选择） |
| **50种效果** | 心形飘落、烟花、星空、花瓣、泡泡、打字机、爱心脉冲、水波纹（各有多个变体） |
| **背景音乐** | 循环播放《告白气球》（需放入 bg_music.mp3） |
| **收藏功能** | 点击❤️收藏喜欢的效果 |
| **重播功能** | 展示结束后可点击"再看一轮" |
| **音乐控制** | 可随时开关背景音乐 |

---

## ⚠️ 常见问题

### 1. Gradle 同步失败
- 检查网络连接，确保能访问 Google Maven 仓库
- 尝试使用 VPN 或配置国内镜像源

### 2. 编译错误：找不到 Android SDK
- 在 Android Studio 中配置 SDK 路径：`File → Settings → Appearance & Behavior → System Settings → Android SDK`

### 3. 安装失败：INSTALL_FAILED_UPDATE_INCOMPATIBLE
- 先卸载旧版本 App 再安装新版本

### 4. 背景音乐不播放
- 确认 `bg_music.mp3` 文件已正确放入 `app/src/main/res/raw/` 目录
- 检查文件名是否正确（小写）

---

## 📂 项目结构

```
loveai/
├── app/
│   ├── src/main/
│   │   ├── java/com/loveai/
│   │   │   ├── manager/
│   │   │   │   └── MusicManager.kt      # 音乐播放管理
│   │   │   ├── model/
│   │   │   │   └── Effect.kt            # 效果模型（50个变体）
│   │   │   ├── repository/
│   │   │   │   ├── EffectRepository.kt  # 效果仓库
│   │   │   │   ├── EffectVariants.kt    # 50个效果变体配置
│   │   │   │   └── FavoriteRepository.kt
│   │   │   ├── viewmodel/
│   │   │   │   └── LoveViewModel.kt
│   │   │   └── ui/
│   │   │       ├── SplashActivity.kt    # 启动页
│   │   │       ├── MainActivity.kt      # 主界面
│   │   │       ├── FavoriteActivity.kt
│   │   │       └── effects/             # 8种效果视图
│   │   ├── res/
│   │   │   ├── raw/
│   │   │   │   └── bg_music.mp3         # 👈 放这里
│   │   │   └── ...
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

---

祝你们生日快乐！💕
