# Shizuku 管理器库 · 宿主接入指南

> 目标：宿主 App 只加 **1 个依赖**，即可内置完整的 Shizuku 管理器（启动/停止、无线调试配对、应用管理、授权弹窗、开机自启全部随包），用户不再需要单独安装 Shizuku App。

---

## 0. 前置条件

| 项目 | 要求 | 原因 |
|---|---|---|
| minSdk | ≥ 24 | Shizuku 13.7 基线 |
| compileSdk | 37（建议 36+） | 库以 compileSdk 37 构建 |
| Gradle JDK | 21 | 库字节码 jvmTarget = 21 |
| 宿主包名 | **不能含 `-`** | server 从 `/data/app/<包名>-<hash>/` 按 `-` 切分解析包名 |
| 仓库 | `mavenCentral()` + `maven { url 'https://jitpack.io' }`（未发布时改用 `mavenLocal()`） | jitpack 拿本库 + libsu |
| 设备 | 已卸载独立 Shizuku App，或签名与之相同 | `API_V23` 权限同名冲突，见第 5 节第 4 条 |

---

## 1. 依赖接入（模块级 build.gradle，共 5 处）

以 Groovy DSL 为例（Kotlin DSL 写法等价）：

```groovy
android {
    defaultConfig {
        // ★ 关键占位符：provider authority / 授权弹窗 action / START/STOP receiver
        //   / permission-group 全部由它按宿主包名生成，跟 applicationId 保持一致即可
        manifestPlaceholders += ["shizukuApplicationId": applicationId]
    }

    packagingOptions {
        jniLibs {
            // libshizuku.so（starter）必须解压到磁盘，server 以 app_process 加载它
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = '21'
    }
}

dependencies {
    // Shizuku 管理器库（POM 自带 server/starter/rish/api/provider 全链传递依赖）
    implementation "com.github.jeraypop.Shizuku:manager:13.7.0-jeraypop"

    // ★ 仅当宿主开 minifyEnabled（R8）时需要：hidden API 桩，编译期存在、不进 APK
    //   不加的话 R8 会报 Missing class android.os.ServiceManager 等约 30 个类
    compileOnly "dev.rikka.hidden:stub:4.4.0"

    // （可选）宿主自己要"使用" Shizuku 跑特权代码时才需要——见第 4 节
    implementation "dev.rikka.shizuku:api:13.1.5"
    implementation "dev.rikka.shizuku:provider:13.1.5"
}
```

**Kotlin DSL 等价写法**（`build.gradle.kts`）：

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["shizukuApplicationId"] = applicationId.toString()
    }
    packaging {
        jniLibs { useLegacyPackaging = true }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // AGP 9 内置 Kotlin：不要再 apply org.jetbrains.kotlin.android，用 android { kotlin { compilerOptions } }
    kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }
}

dependencies {
    implementation("com.github.jeraypop.Shizuku:manager:13.7.0-jeraypop")
    compileOnly("dev.rikka.hidden:stub:4.4.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
```

> **JitPack 未发布前的本地验证**（当前状态：`13.7.0-jeraypop` 尚未 push tag，JitPack 上还取不到）：
> 在本仓库跑 `./gradlew publishToMavenLocal`，宿主 `settings.gradle` 仓库里加 `mavenLocal()`，
> 坐标不变（同组 `com.github.jeraypop.Shizuku`）。发布流程：commit → `git tag 13.7.0-jeraypop`
> → `git push && git push --tags` → 等 JitPack 构建完成后换成 jitpack.io 仓库即可。

---

## 2. 宿主不用写任何初始化代码

库通过 androidx.startup 自动初始化（`ShizukuInitializer`），随 manifest 合并进宿主进程，宿主**不需要**改自己的 Application 类。

⚠️ **唯一红线**：宿主 manifest 里不要对 `androidx.startup.InitializationProvider`
做 `tools:node="remove"`（部分项目为了启动优化会删它——删了 Shizuku 就不会初始化）。

---

## 3. 跳转管理器 UI

所有 Activity/Service/Receiver 都已随 manifest 合并，宿主直接 Intent 跳转即可：

```kotlin
// 主页（启动状态、应用管理入口、设置入口）
startActivity(Intent(this, moe.shizuku.manager.MainActivity::class.java))

// 启动页（无线调试 / root 启动，含配对引导）
startActivity(Intent(this, moe.shizuku.manager.starter.StarterActivity::class.java))
```

桌面不会多出图标——launcher 入口已从库中移除，宿主图标即唯一入口。

> **Stealth（隐藏模式）入口**：主页的 "Hide Shizuku from other apps" 卡片**只对独立 App 显示**
> （`:app` 壳的 manifest 带 `moe.shizuku.manager.STANDALONE` meta-data 标记）。
> 宿主自动不显示——stealth 会克隆并改宿主 APK 的包名，对集成场景无意义且有风险。
> 宿主如需在代码里判断：`moe.shizuku.manager.utils.ApkUtils.isStandaloneBuild`。

**代码方式启动/停止 server**（不走 UI 时，推荐直接用库里的工具类）：

```kotlin
import moe.shizuku.manager.ShizukuControl

ShizukuControl.start(context)   // 按设置里上次的方式启动（root / 无线调试）
ShizukuControl.stop(context)    // 停止 server
```

内部就是发送 `<宿主包名>.START` / `.STOP` 广播并带上鉴权 token（不带 token 会被 receiver 拒绝），
由库里的 `ManualStartReceiver` / `ManualStopReceiver` 接收，宿主无需自己拼 action。
被杀后台时也能收到，广播会把进程拉起；只有用户手动"强制停止"后收不到。

> **宿主构建下默认隐藏的入口**（`:app` 独立壳默认显示）：
> 更新相关（自动检查、关于里的更新按钮、设置里的更新模式）、整个"关于"菜单、
> 设置里的 Support 整个分类（帮助 / 报告错误 / 连标题一起隐藏）与翻译组（参与翻译、翻译贡献者）。
>
> 首页的可选卡片（在终端应用中使用 Shizuku、自动化控制、开发者指南）
> 已改为**设置页 "Home page cards" 分类下的开关**，宿主与独立 App 都能看到：
> 宿主默认全部关闭、独立 App 默认全部开启，用户可自行切换，改完返回首页立即生效。
> 例外：**Stealth** 的开关与卡片只在独立 App 出现，宿主构建下两者都强制隐藏
> （它会克隆并改名整个 APK，对集成场景无意义）。

---

## 4. 宿主自己"使用" Shizuku（可选）

宿主集成库后已自动持有 `moe.shizuku.manager.permission.API_V23` 权限声明，
自己的进程就是 manager 进程，调用 Shizuku API 与普通第三方完全一致：

```kotlin
if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
    val p = Shizuku.newProcess(arrayOf("sh", "-c", "pm list packages"), null, null)
} else {
    Shizuku.requestPermission(0)   // 弹宿主自己的授权框
}
```

第三方 App 接入你的 Shizuku：用官方 `dev.rikka.shizuku:api:13.1.5`，
标准写法零改动（binder 由 server 主动推送，客户端无需感知宿主包名）。

---

## 5. 硬性约束与已知坑（务必通读）

1. **包名不能含 `-`** —— server 启动时从 CLASSPATH 目录名解析宿主包名，含 `-` 会导致解析失败。
2. **不要 remove `InitializationProvider`** —— 见第 2 节。
3. **R8 missing classes** —— 开混淆必须加 `compileOnly "dev.rikka.hidden:stub:4.4.0"`（库的 consumer-rules.pro 已自动带入 keep 规则：server/starter/shell 的 main 入口、Parcelable CREATOR、native 方法等）。
4. **与独立 Shizuku App 无法共存（签名不同时）** —— `moe.shizuku.manager.permission.API_V23` 是公共 API 固定名，全设备只能声明一次；宿主与独立 App 签名不同时，装第二个会被 `INSTALL_FAILED_DUPLICATE_PERMISSION` 拒绝。要共存必须统一 keystore。
5. **Android Studio Run 安装失败 `INSTALL_FAILED_TEST_ONLY`** —— Studio 会给 Run 产物注入 `android:testOnly`；根治：gradle.properties 加 `android.injected.testOnly=false`（本仓库已加，宿主项目建议也加）。
6. **权限组已按包名参数化** —— permission-group 为 `<宿主包名>.permission-group.API`，多 App 各自独立，互不抢占。

---

## 6. 用户侧使用流程（集成完成后）

```
安装宿主 App（唯一入口）
  → 宿主内打开 Shizuku 主页
  → 无线调试配对（或 root）启动 server
  → server 以 shell 权限从宿主 APK 拉起（app_process 加载宿主 APK 内的类）
  → 第三方 App 请求权限 → 弹宿主内的授权框
  → 授权后第三方通过 server 推送的 binder 使用特权能力
```

授权记录存在 server 侧，客户端重装不掉授权；宿主卸载即整套下线。

---

## 7. 快速排错表

| 症状 | 原因 / 处理 |
|---|---|
| R8 报 Missing class（ServiceManager/SystemProperties/IActivityManager…） | 加 `compileOnly "dev.rikka.hidden:stub:4.4.0"` |
| 运行时 Shizuku 永远未初始化 | 检查宿主是否 remove 了 InitializationProvider |
| `INSTALL_FAILED_DUPLICATE_PERMISSION` | 与独立 App 签名不同，卸载另一方或统一 keystore |
| `-126 redeclare permission group` | 老版本库的固定组名，更新到参数化版本 |
| server 启动后立即退出、日志找不到包名 | 宿主包名含 `-`，改包名 |
| Studio 装机报 `INSTALL_BASELINE_PROFILE_FAILED` | 实为前一步 testOnly 静默失败，加 `android.injected.testOnly=false` |
