package moe.shizuku.manager

import android.content.Context
import android.content.Intent

/**
 * Sends the START/STOP control broadcasts handled by this library's
 * ManualStartReceiver / ManualStopReceiver (both exported, authenticated).
 *
 * The same two functions work unchanged inside a host app that embeds the
 * manager library — the action strings and the token are derived from the
 * running package at runtime, so nothing is hardcoded here.
 *
 * Preconditions for START (decided by the last launch method in settings):
 *  - ROOT: shell runs the starter directly.
 *  - ADB (wireless, Android 11+ / TV / adb tcpip): requires
 *    WRITE_SECURE_SETTINGS granted once via adb:
 *      adb shell pm grant <packageName> android.permission.WRITE_SECURE_SETTINGS
 *  - Otherwise background start is not supported and a notification shows the error.
 *
 * STOP works whenever the server is running.
 */
object ShizukuControl {

    private const val AUTH_EXTRA = "auth"

    @JvmStatic
    fun start(context: Context) = send(context, ".START")

    @JvmStatic
    fun stop(context: Context) = send(context, ".STOP")

    private fun send(context: Context, actionSuffix: String) {
        val packageName = context.packageName
        val intent = Intent(packageName + actionSuffix)
            .setPackage(packageName)
            .putExtra(AUTH_EXTRA, ShizukuSettings.getAuthToken())
        context.sendBroadcast(intent)
    }
}
