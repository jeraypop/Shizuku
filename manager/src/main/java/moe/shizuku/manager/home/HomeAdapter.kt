package moe.shizuku.manager.home

import android.os.Build
import kotlinx.coroutines.CoroutineScope
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_HOME_CARD_AUTOMATION
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_HOME_CARD_LEARN_MORE
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_HOME_CARD_STEALTH
import moe.shizuku.manager.ShizukuSettings.Keys.KEY_HOME_CARD_TERMINAL
import moe.shizuku.manager.management.AppsViewModel
import moe.shizuku.manager.utils.ApkUtils.isStandaloneBuild
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.UserHandleCompat
import rikka.recyclerview.IdBasedRecyclerViewAdapter
import rikka.recyclerview.IndexCreatorPool
import rikka.shizuku.Shizuku

class HomeAdapter(private val homeModel: HomeViewModel, private val appsModel: AppsViewModel, private val scope: CoroutineScope) :
    IdBasedRecyclerViewAdapter(ArrayList()) {

    init {
        updateData()
        setHasStableIds(true)
    }

    companion object {

        private const val ID_STATUS = 0L
        private const val ID_APPS = 1L
        private const val ID_TERMINAL = 2L
        private const val ID_START_ROOT = 3L
        private const val ID_START_WADB = 4L
        private const val ID_START_ADB = 5L
        private const val ID_LEARN_MORE = 6L
        private const val ID_ADB_PERMISSION_LIMITED = 7L
        private const val ID_AUTOMATION = 8L
        private const val ID_STEALTH = 9L
    }

    override fun onCreateCreatorPool(): IndexCreatorPool {
        return IndexCreatorPool()
    }

    fun updateData() {
        val status = homeModel.serviceStatus.value?.data ?: return
        val grantedCount = appsModel.grantedCount.value?.data ?: 0
        val adbPermission = status.permission
        val running = status.isRunning
        val isPrimaryUser = UserHandleCompat.myUserId() == 0

        clear()
        addItem(ServerStatusViewHolder.CREATOR, status, ID_STATUS)

        if (adbPermission) {
            addItem(ManageAppsViewHolder.CREATOR, status to grantedCount, ID_APPS)
            if (homeCardEnabled(KEY_HOME_CARD_TERMINAL)) addItem(TerminalViewHolder.CREATOR, status, ID_TERMINAL)
        }

        if (running && !adbPermission) {
            addItem(AdbPermissionLimitedViewHolder.CREATOR, status, ID_ADB_PERMISSION_LIMITED)
        }

        if (isPrimaryUser) {
            val rootRestart = running && status.uid == 0

            if (EnvironmentUtils.isRooted()) addItem(StartRootViewHolder.CREATOR, rootRestart, ID_START_ROOT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                EnvironmentUtils.isTelevision() ||
                EnvironmentUtils.getAdbTcpPort() > 0
            ) addItem(StartWirelessAdbViewHolder.creator(scope), null, ID_START_WADB)

            addItem(StartAdbViewHolder.CREATOR, null, ID_START_ADB)
        }
        if (homeCardEnabled(KEY_HOME_CARD_AUTOMATION)) addItem(AutomationViewHolder.CREATOR, null, ID_AUTOMATION)

        // Stealth is always off when embedded in a host app, no matter what is stored in settings.
        if (isStandaloneBuild && homeCardEnabled(KEY_HOME_CARD_STEALTH)) addItem(StealthViewHolder.CREATOR, null, ID_STEALTH)

        if (homeCardEnabled(KEY_HOME_CARD_LEARN_MORE)) addItem(LearnMoreViewHolder.CREATOR, null, ID_LEARN_MORE)
        notifyDataSetChanged()
    }

    /**
     * Each optional home card is user-controlled from Settings. The default is
     * decided by the build: on for the standalone app, off when the manager is
     * embedded in a host app.
     */
    private fun homeCardEnabled(key: String): Boolean = ShizukuSettings.getBoolean(key, isStandaloneBuild)
}
