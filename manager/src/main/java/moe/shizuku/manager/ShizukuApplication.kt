package moe.shizuku.manager

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.ktx.logd
import moe.shizuku.manager.service.WatchdogService
import moe.shizuku.manager.utils.ShizukuStateMachine
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.core.util.BuildUtils.atLeast30
import rikka.material.app.LocaleDelegate

/**
 * Standalone Shizuku app still declares this class as its Application.
 *
 * When Shizuku is embedded into a host app as a library, the host keeps its own
 * Application class; [initOnce] is then called by [ShizukuInitializer]
 * (androidx.startup) in the host process instead.
 */
class ShizukuApplication : Application() {

    companion object {

        init {
            logd("ShizukuApplication", "init")

            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
            if (Build.VERSION.SDK_INT >= 28) {
                HiddenApiBypass.setHiddenApiExemptions("")
            }
            if (atLeast30) {
                System.loadLibrary("adb")
            }
        }

        lateinit var application: Application
            private set

        lateinit var appContext: Context
            private set

        /**
         * Initialize the manager code inside whatever process hosts it.
         * Idempotent: calling again with the same context is a no-op.
         */
        @JvmStatic
        fun initOnce(context: Context) {
            val appContext = context.applicationContext
            if (::application.isInitialized && this.appContext === appContext) return

            application = appContext as Application
            this.appContext = appContext

            ShizukuSettings.initialize(appContext)
            LocaleDelegate.defaultLocale = ShizukuSettings.getLocale()
            AppCompatDelegate.setDefaultNightMode(ShizukuSettings.getNightMode())

            if (ShizukuSettings.getWatchdog()) WatchdogService.start(appContext)
        }
    }

    override fun onCreate() {
        super.onCreate()
        initOnce(this)
    }

}
