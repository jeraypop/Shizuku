package moe.shizuku.manager

import android.content.Context
import androidx.startup.Initializer

/**
 * Runs [ShizukuApplication.initOnce] in the host app process via androidx.startup,
 * so embedding apps do not need to touch their own Application class.
 */
class ShizukuInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        ShizukuApplication.initOnce(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
