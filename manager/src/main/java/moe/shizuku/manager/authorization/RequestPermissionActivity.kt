package moe.shizuku.manager.authorization

import android.app.Dialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.databinding.ConfirmationDialogBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.utils.Logger.LOGGER
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.core.res.resolveColor
import rikka.html.text.HtmlCompat
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME

class RequestPermissionActivity : AppActivity() {

    private lateinit var dialog: Dialog

    private fun setResult(requestUid: Int, requestPid: Int, requestCode: Int, allowed: Boolean, onetime: Boolean) {
        val data = Bundle()
        data.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed)
        data.putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime)
        try {
            Shizuku.dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data)
        } catch (e: Throwable) {
            LOGGER.e("dispatchPermissionConfirmationResult")
        }
    }

    private fun checkSelfPermission(): Boolean {
        val permission = Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED
        if (permission) return true

        val icon = getDrawable(R.drawable.ic_system_icon)
        icon?.setTint(theme.resolveColor(android.R.attr.colorAccent))

        val dialog = MaterialAlertDialogBuilder(this)
                .setIcon(icon)
                .setTitle("Shizuku: ${getString(R.string.app_management_dialog_adb_is_limited_title)}")
                .setMessage(getString(R.string.app_management_dialog_adb_is_limited_message, Helps.ADB.get()).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE))
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener { finish() }
                .create()
        dialog.setOnShowListener {
            (it as AlertDialog).findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
        }
        try {
            dialog.show()
        } catch (ignored: Throwable) {
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = intent.getIntExtra("uid", -1)
        val pid = intent.getIntExtra("pid", -1)
        val requestCode = intent.getIntExtra("requestCode", -1)
        val ai = intent.getParcelableExtra<ApplicationInfo>("applicationInfo")
        if (uid == -1 || pid == -1 || ai == null) {
            finish()
            return
        }

        // The binder is handed over by a listener that Shizuku dispatches on the MAIN thread, so
        // waiting for it here must never block the main thread: doing so deadlocks (the very event
        // being waited for can only be delivered by the blocked thread) and ANRs whatever activity
        // is in front. This is triggered for real right after the first wireless pairing — the
        // server starts and immediately asks this app for a runtime permission, while StarterActivity
        // is still on screen waiting for focus.
        lifecycleScope.launch {
            if (!ShizukuStateMachine.awaitBinder(5000)) {
                LOGGER.e("Binder not received in 5s")
                finish()
                return@launch
            }
            showPermissionDialog(uid, pid, requestCode, ai)
        }
    }

    private fun showPermissionDialog(uid: Int, pid: Int, requestCode: Int, ai: ApplicationInfo) {
        if (!checkSelfPermission()) {
            setResult(uid, pid, requestCode, allowed = false, onetime = true)
            return
        }

        val label = try {
            ai.loadLabel(packageManager)
        } catch (e: Exception) {
            ai.packageName
        }

        val binding = ConfirmationDialogBinding.inflate(layoutInflater).apply {
            button1.setOnClickListener {
                setResult(uid, pid, requestCode, allowed = true, onetime = false)
                dialog.dismiss()
            }
            button3.setOnClickListener {
                setResult(uid, pid, requestCode, allowed = false, onetime = true)
                dialog.dismiss()
            }
            title.text = HtmlCompat.fromHtml(getString(R.string.permission_warning_template,
                    label, getString(R.string.permission_group_description)))
        }

        dialog = MaterialAlertDialogBuilder(this)
                .setView(binding.root)
                .setCancelable(false)
                .setOnDismissListener { finish() }
                .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
}
