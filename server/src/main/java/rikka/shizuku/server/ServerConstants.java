package rikka.shizuku.server;

import moe.shizuku.server.BuildConfig;

public class ServerConstants {

    public static final int MANAGER_APP_NOT_FOUND = 50;

    public static final String PERMISSION = "moe.shizuku.manager.permission.API_V23";

    /**
     * The manager package name is resolved at runtime from CLASSPATH (the APK that the
     * server is loaded from), so the host app package works for both the standalone
     * Shizuku app and apps embedding Shizuku as a library. BuildConfig value is only a
     * fallback used when CLASSPATH parsing fails.
     */
    public static String getRequestPermissionAction() {
        String managerApplicationId = ShizukuService.MANAGER_APPLICATION_ID;
        if (managerApplicationId == null) {
            managerApplicationId = BuildConfig.MANAGER_APPLICATION_ID;
        }
        return managerApplicationId + ".intent.action.REQUEST_PERMISSION";
    }

    public static final int BINDER_TRANSACTION_getApplications = 10001;
}
