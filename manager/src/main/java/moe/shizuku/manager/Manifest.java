package moe.shizuku.manager;

public class Manifest {

    public static class permission_group {
        /** Parameterized per applicationId so host app and standalone app can coexist
         *  (permission groups are globally unique, owned by the first installer). */
        public static String API(String applicationId) {
            return applicationId + ".permission-group.API";
        }
    }

    public static class permission {
        public static final String API_V23 = "moe.shizuku.manager.permission.API_V23";
    }
}
