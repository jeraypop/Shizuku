package moe.shizuku.manager

import android.os.Bundle
import androidx.core.os.bundleOf
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import moe.shizuku.api.BinderContainer
import moe.shizuku.manager.utils.Logger.LOGGER
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_TOKEN
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.server.ktx.workerHandler

class ShizukuManagerProvider : ShizukuProvider() {

    companion object {
        private const val EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER"
        private const val METHOD_SEND_USER_SERVICE = "sendUserService"
    }

    override fun onCreate(): Boolean {
        disableAutomaticSuiInitialization()
        return super.onCreate()
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (extras == null) return null

        return if (method == METHOD_SEND_USER_SERVICE) {
            try {
                extras.classLoader = BinderContainer::class.java.classLoader

                val token = extras.getString(USER_SERVICE_ARG_TOKEN) ?: return null
                val binder = extras.getParcelable<BinderContainer>(EXTRA_BINDER)?.binder ?: return null

                return runBlocking {
                    // This is a synchronous ContentProvider#call() invoked from the server's own
                    // binder thread when a user service starts. Waiting for State.RUNNING used to be
                    // a dead end: that state is only emitted from a listener Shizuku dispatches on
                    // the main thread, so the wait could only ever end when the main thread was free
                    // — and right after the first pairing the main thread is busy starting the
                    // server (or blocked by RequestPermissionActivity), which made every user
                    // service attach fail with "Binder not received in 5s".
                    // awaitBinder() polls Shizuku.pingBinder() and never depends on main-thread
                    // delivery.
                    if (!ShizukuStateMachine.awaitBinder(5000)) {
                        LOGGER.e("Binder not received in 5s")
                        return@runBlocking null
                    }
                    withContext(workerHandler.asCoroutineDispatcher()) {
                        try {
                            val reply = Bundle()
                            Shizuku.attachUserService(binder, bundleOf(USER_SERVICE_ARG_TOKEN to token))
                            reply!!.putParcelable(EXTRA_BINDER, BinderContainer(Shizuku.getBinder()))
                            reply
                        } catch (e: Throwable) {
                            LOGGER.e(e, "attachUserService $token")
                            null
                        }
                    }
                }
            } catch (e: Throwable) {
                LOGGER.e(e, "sendUserService")
                null
            }
        } else {
            super.call(method, arg, extras)
        }
    }
}
