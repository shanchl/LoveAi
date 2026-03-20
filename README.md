# LoveAI

LoveAI 是一个以“浪漫表达”为主题的 Android 单应用项目。应用通过多种自定义动态特效、情感文案和背景音乐组合，生成一段可自动播放的沉浸式展示流程，适合做表白、小礼物、纪念日互动演示等场景。

项目整体采用典型的 Android 分层方式组织代码：`ui` 负责页面与交互，`viewmodel` 负责状态编排，`repository` 负责效果与收藏数据，`manager` 负责音乐播放，自定义特效则集中在 `ui/effects` 中完成绘制与动画。

## 项目定位

- 平台：Android
- 语言：Kotlin
- 构建：Gradle / Android Application
- 最低版本：Android 5.0（API 21）
- 目标版本：Android 14（API 34）
- 核心体验：启动倒计时 -> 随机浪漫特效轮播 -> 收藏/音乐控制 -> 结束页回放与分享

## 系统架构

### 分层说明

```text
SplashActivity / MainActivity / FavoriteActivity / EndingActivity
                    |
                    v
               LoveViewModel
                    |
         +----------+-----------+
         |                      |
         v                      v
  EffectRepository       FavoriteRepository
         |                      |
         v                      v
   EffectVariants         SharedPreferences
         |
         v
  Effect / EffectVariant / LoveMessages

同时并行存在：
MusicManager -> 负责背景音乐、歌单、切歌、播放模式
BaseEffectView + 各 EffectView -> 负责动态绘制与逐帧动画
PageTransitionManager -> 负责 ViewPager 页面切换动画
```

### 核心模块

- `app/src/main/java/com/loveai/ui`
  负责四个页面的生命周期、控件绑定、导航跳转和交互事件。
- `app/src/main/java/com/loveai/viewmodel/LoveViewModel.kt`
  负责当前特效列表、当前页索引、自动播放状态、收藏数据的统一管理。
- `app/src/main/java/com/loveai/repository`
  负责特效数据生成、变体配置和收藏持久化。
- `app/src/main/java/com/loveai/manager/MusicManager.kt`
  负责扫描 `res/raw` 音频资源并构建播放列表，控制播放/暂停/上下首/播放模式。
- `app/src/main/java/com/loveai/ui/effects`
  负责不同浪漫特效的 Canvas 绘制、粒子动画、文案展示以及文本表现效果。

## 业务功能

### 1. 启动页

- `SplashActivity` 负责 3 秒倒计时进入主页面。
- 页面包含渐变背景、漂浮爱心、数字动画和 loading 提示。
- 启动时初始化背景音乐，保证进入主界面后音乐可无缝继续。

### 2. 主展示页

- `MainActivity` 是主业务入口。
- 使用 `ViewPager2` 承载多页动态特效，每次从效果池中随机生成 6 到 8 个页面。
- 页面支持自动轮播、手动滑动、播放/暂停、收藏当前效果、打开收藏夹、音乐控制、歌单弹窗。
- 播放到最后一个特效后，延迟 2 秒跳转到结束页。

### 3. 收藏页

- `FavoriteActivity` 读取用户已收藏的 `variantId`，重建为完整 `Effect` 列表。
- 使用 `GridView` 以双列形式展示收藏特效预览。
- 若没有收藏内容，则展示空状态文案。

### 4. 结束页

- `EndingActivity` 作为整段展示流程的收尾页面。
- 页面展示感谢文案、浮动爱心动画、重新开始按钮和系统分享按钮。
- 音乐在此页面继续播放，可单独暂停/恢复。

### 5. 音乐系统

- `MusicManager` 会优先扫描 `res/raw` 下的音频资源，自动生成歌单。
- 当前项目内已经包含 `bg_music.mp3` 与 `love_song_1.mp3` 两首音乐资源。
- 支持列表循环、单曲循环、随机播放三种模式，主页面提供上下首、播放/暂停、歌单查看等控制。

## 数据模型与业务实体

### EffectType

项目定义了 12 类基础浪漫特效：

- `HEART_RAIN`
- `FIREWORK`
- `STARRY_SKY`
- `PETAL_FALL`
- `BUBBLE_FLOAT`
- `TYPEWRITER`
- `HEART_PULSE`
- `RIPPLE`
- `SNOW_FALL`
- `METEOR_SHOWER`
- `BUTTERFLY`
- `AURORA`

### EffectVariant

`EffectVariant` 是真正驱动页面表现的配置实体，包含：

- 变体 ID
- 基础特效类型
- 变体名称
- 主色/辅色/背景色
- 动画速度
- 粒子数量
- 主文案与副文案

`EffectVariants.kt` 预定义了全部视觉变体。按代码实际统计，当前总计约 100 个变体，远多于简单的“若干特效页面”概念，是项目最核心的内容资产。

### Effect

`Effect` 是运行时展示对象，包含：

- 唯一运行时 ID
- 一个 `EffectVariant`
- 收藏状态 `isFavorite`

它相当于“当前要展示的一页”。

## 代码逻辑梳理

### 1. 随机效果生成链路

主流程由 `LoveViewModel.generateRandomEffects()` 发起：

1. 随机决定本轮展示数量，范围为 6 到 8 页。
2. 调用 `EffectRepository.getRandomEffects(count)`。
3. `EffectRepository` 再委托 `EffectVariants.getRandomVariants(count)` 选出若干变体。
4. `EffectVariants` 先按 `baseType` 分组，优先确保同一轮中不重复同一种基础特效类型。
5. 每个变体再包装成 `Effect` 对象，生成唯一 ID。
6. `LoveViewModel` 根据收藏仓库修正 `isFavorite` 状态。
7. `MainActivity` 观察 `effects`，更新 `ViewPager2` 列表与底部指示器。

这条链路的重点是“随机但尽量不重复类型”，确保每一轮展示既多样又有新鲜感。

### 2. 页面轮播逻辑

`MainActivity` 通过 `Handler + Runnable` 实现自动轮播：

- 默认每页停留 10 秒。
- 当用户点击暂停或页面已结束时，停止轮播任务。
- 当用户手动滑动页面时，会重新计算下一次自动轮播时间。
- 切页时不是直接依赖默认滑动，而是通过 `switchToNextPageWithFade()` 做一次淡出、切页、淡入的过渡。
- `PageTransitionManager` 还额外给 `ViewPager2` 提供透明度和缩放过渡，让页面切换更柔和。

### 3. 特效渲染机制

所有特效 View 都继承 `BaseEffectView`：

- `bindEffect()` 绑定当前页的 `EffectVariant`。
- `onUpdateAnimation()` 每约 16ms 被调用一次，用于更新粒子、位置、透明度等状态。
- `onDrawEffect()` 负责真正的 Canvas 绘制。
- 基类统一封装了颜色获取、动画循环、文本对比度处理和文字底板绘制逻辑。

这种设计把“特效共性”和“特效差异”分离开了：

- 共性：帧循环、颜色访问、文本增强、播放控制。
- 差异：每种特效如何初始化、如何更新粒子、如何绘制背景和文字。

### 4. 文案展示逻辑

- `LoveMessages` 按特效类别维护情感文案池。
- 每个变体在定义时就绑定了一组主文案和副文案。
- 文案不是页面层写死，而是作为视觉配置的一部分跟随变体走。
- `TextEffectManager` 为不同类型文本提供横排、竖排、波浪、弧形、散开、下落等展示方式。
- `TypewriterEffect` 还实现了独立的打字机演出：逐字出现、光标闪烁、完成后浮动和发光。

这意味着项目不仅是“粒子特效集合”，而是“视觉特效 + 情绪文案 + 文本动画”的组合引擎。

### 5. 收藏逻辑

收藏由 `FavoriteRepository` 管理：

- 持久化介质是 `SharedPreferences`。
- 存储的不是完整 `Effect`，而是 `variantId` 集合。
- 读取时根据 `variantId` 到 `EffectVariants` 反查完整变体，再重新构建 `Effect`。

这种做法的优点是：

- 存储结构轻量。
- 不依赖运行时随机 ID。
- 收藏记录稳定，应用重启后依旧可以恢复。

### 6. 音乐逻辑

`MusicManager` 使用单例对象封装 `MediaPlayer`：

- 启动时初始化播放器。
- 主页面首次进入时扫描 `R.raw` 自动构建歌单。
- 可以播放、暂停、切歌、按索引播放、获取当前歌曲名。
- 播放完成后根据当前播放模式决定下一步行为。
- 页面销毁时默认不立即释放音乐，确保从主页面跳到结束页时音乐不中断。

## 页面与模块关系

### 页面流转

```text
SplashActivity
    -> MainActivity
        -> FavoriteActivity
        -> EndingActivity
             -> MainActivity（Replay）
```

### 页面职责对应

- `SplashActivity`：启动动画、倒计时、音乐预热。
- `MainActivity`：随机特效展示、主交互、轮播调度、音乐控制。
- `FavoriteActivity`：收藏内容浏览。
- `EndingActivity`：收尾演出、分享、重新开始。

## 目录结构

```text
loveai/
├─ app/
│  ├─ src/main/
│  │  ├─ java/com/loveai/
│  │  │  ├─ manager/
│  │  │  │  └─ MusicManager.kt
│  │  │  ├─ model/
│  │  │  │  └─ Effect.kt
│  │  │  ├─ repository/
│  │  │  │  ├─ EffectRepository.kt
│  │  │  │  ├─ EffectVariants.kt
│  │  │  │  └─ FavoriteRepository.kt
│  │  │  ├─ ui/
│  │  │  │  ├─ SplashActivity.kt
│  │  │  │  ├─ MainActivity.kt
│  │  │  │  ├─ FavoriteActivity.kt
│  │  │  │  ├─ EndingActivity.kt
│  │  │  │  ├─ PageTransitionManager.kt
│  │  │  │  └─ effects/
│  │  │  └─ viewmodel/
│  │  │     └─ LoveViewModel.kt
│  │  ├─ res/
│  │  │  ├─ layout/
│  │  │  ├─ drawable/
│  │  │  ├─ raw/
│  │  │  └─ values/
│  │  └─ AndroidManifest.xml
│  └─ build.gradle
├─ build.gradle
├─ settings.gradle
└─ BUILD_GUIDE.md
```

## 构建与运行

### 开发环境

- Android Studio 任意较新稳定版
- JDK 17
- Android SDK 34

### 直接运行

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 默认输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 依赖与技术选型

- `androidx.appcompat`
- `androidx.core-ktx`
- `material`
- `constraintlayout`
- `viewpager2`
- `lifecycle-viewmodel-ktx`
- `lifecycle-livedata-ktx`
- `activity-ktx`

项目没有引入复杂网络层、数据库或第三方动画框架，主要依靠 Android 原生绘制能力、`MediaPlayer`、`LiveData` 和 `ViewModel` 完成体验搭建。

## 适合继续演进的方向

- 把乱码资源文件统一转成 UTF-8，修复部分中文注释和字符串编码问题。
- 为 `MusicManager` 增加更稳健的生命周期与异常处理。
- 为收藏页增加点击预览、删除收藏、分享收藏效果等能力。
- 将特效配置抽离为 JSON 或本地配置文件，降低后续扩展成本。
- 为 `ViewModel` 和仓库层补充单元测试。
- 增加应用截图、录屏或设计稿，让 README 更完整。

## 总结

这个项目本质上是一个“浪漫特效播放引擎”示例应用。它的技术重点不在复杂业务系统，而在于把随机特效、视觉配置、情感文案、背景音乐和页面流转编排成一段完整的沉浸式体验。对于接手维护者来说，最值得优先理解的三个点是：

1. `EffectVariants` 如何定义内容资产。
2. `LoveViewModel` 如何组织页面播放状态。
3. `BaseEffectView` 如何统一承载所有自定义动画绘制。
