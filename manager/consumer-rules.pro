# Consumer rules: applied to apps that depend on the Shizuku manager library.
# Keep the entries that are loaded via app_process / reflection, because the
# Shizuku server runs as a shell process loading classes straight from the
# host APK — R8 must not rename or strip them.

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepnames class moe.shizuku.api.BinderContainer

# Missing class android.app.IProcessObserver$Stub
# Missing class android.app.IUidObserver$Stub
-keepclassmembers class rikka.hidden.compat.adapter.ProcessObserverAdapter {
    <methods>;
}

-keepclassmembers class rikka.hidden.compat.adapter.UidObserverAdapter {
    <methods>;
}

# Entrance of Shizuku service
-keep class rikka.shizuku.server.ShizukuService {
    public static void main(java.lang.String[]);
}

# Entrance of user service starter
-keep class moe.shizuku.starter.ServiceStarter {
    public static void main(java.lang.String[]);
}

# Entrance of shell
-keep class moe.shizuku.manager.shell.Shell {
    public static void main(java.lang.String[], java.lang.String, android.os.IBinder, android.os.Handler);
}

# com.reandroid.ARSCLIB implements android/xmlpull stubs which R8 tries to strip, causing crashes
# https://github.com/REAndroid/ARSCLib/issues/95
-keep public interface android.util.AttributeSet { *; }
-keep public interface android.content.res.XmlResourceParser { *; }
-keep public interface org.xmlpull.v1.** { *; }

# ---------------------------------------------------------------------------
# dev.rikka.hidden:stub is compileOnly inside this library and does NOT
# propagate to consumers. These -dontwarn rules (one per stub class, covering
# nested $-classes) let host R8 builds succeed without adding the stub.
# Real implementations come from the framework at runtime.
# Regenerate from the stub AAR class list when bumping dev.rikka.hidden.
# ---------------------------------------------------------------------------
-dontwarn android.app.ActivityManagerHidden**
-dontwarn android.app.ActivityManagerNative**
-dontwarn android.app.ActivityTaskManager**
-dontwarn android.app.ActivityThread**
-dontwarn android.app.AppOpInfo**
-dontwarn android.app.AppOpsManager**
-dontwarn android.app.AppOpsManagerHidden**
-dontwarn android.app.ContentProviderHolder**
-dontwarn android.app.ContextImpl**
-dontwarn android.app.IActivityManager**
-dontwarn android.app.IActivityManagerPre26**
-dontwarn android.app.IAppTask**
-dontwarn android.app.IApplicationThread**
-dontwarn android.app.IProcessObserver**
-dontwarn android.app.ITaskStackListener**
-dontwarn android.app.IUidObserver**
-dontwarn android.app.ProfilerInfo**
-dontwarn android.content.ContentProviderNative**
-dontwarn android.content.ContentProviderProxy**
-dontwarn android.content.ContextHidden**
-dontwarn android.content.IContentProvider**
-dontwarn android.content.IIntentReceiver**
-dontwarn android.content.IIntentSender**
-dontwarn android.content.pm.ApplicationInfoHidden**
-dontwarn android.content.pm.BaseParceledListSlice**
-dontwarn android.content.pm.ILauncherApps**
-dontwarn android.content.pm.IOnAppsChangedListener**
-dontwarn android.content.pm.IPackageInstaller**
-dontwarn android.content.pm.IPackageInstallerCallback**
-dontwarn android.content.pm.IPackageInstallerSession**
-dontwarn android.content.pm.IPackageManager**
-dontwarn android.content.pm.IShortcutService**
-dontwarn android.content.pm.IShortcutServiceV31**
-dontwarn android.content.pm.PackageInfoHidden**
-dontwarn android.content.pm.PackageInstallerHidden**
-dontwarn android.content.pm.PackageManagerHidden**
-dontwarn android.content.pm.ParceledListSlice**
-dontwarn android.content.pm.UserInfo**
-dontwarn android.ddm.DdmHandleAppName**
-dontwarn android.hardware.display.DisplayManagerGlobal**
-dontwarn android.hardware.display.DisplayManagerHidden**
-dontwarn android.hardware.display.IDisplayManager**
-dontwarn android.hardware.display.IDisplayManagerCallback**
-dontwarn android.os.BatteryProperty**
-dontwarn android.os.IBatteryPropertiesRegistrar**
-dontwarn android.os.ICancellationSignal**
-dontwarn android.os.IDeviceIdleController**
-dontwarn android.os.IInterface**
-dontwarn android.os.IUserManager**
-dontwarn android.os.RemoteCallback**
-dontwarn android.os.SELinux**
-dontwarn android.os.ServiceManager**
-dontwarn android.os.SystemProperties**
-dontwarn android.os.UserHandleHidden**
-dontwarn android.permission.IPermissionManager**
-dontwarn android.provider.DeviceConfig**
-dontwarn android.util.LongSparseLongArray**
-dontwarn android.view.DisplayInfo**
-dontwarn android.view.IWindowManager**
-dontwarn android.view.WindowManagerHidden**
-dontwarn android.view.WindowManagerImpl**
-dontwarn com.android.internal.app.IAppOpsActiveCallback**
-dontwarn com.android.internal.app.IAppOpsNotedCallback**
-dontwarn com.android.internal.app.IAppOpsService**
-dontwarn com.android.internal.content.ReferrerIntent**
-dontwarn com.android.internal.infra.AndroidFuture**
-dontwarn com.android.internal.policy.IKeyguardLockedStateListener**
-dontwarn com.android.org.conscrypt.Conscrypt**
-dontwarn dalvik.system.VMRuntime**
-dontwarn stub.dalvik.system.VMRuntimeHidden**
