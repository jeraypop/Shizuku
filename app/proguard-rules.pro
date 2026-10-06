# Entrances are kept by manager/consumer-rules.pro which is applied automatically.

# Room / WorkManager: androidX.work:work-runtime:2.10.4 resolves androidx.room:room-runtime:2.6.1,
# whose keep rule does not cover the generated <init>() that Room needs by reflection. Without
# this the app dies at startup with
#   NoSuchMethodException: androidx.work.impl.WorkDatabase_Impl.<init> []
# (also provided by manager/consumer-rules.pro, repeated here so a host building without the
# library's consumer rules still gets it).
-keep class * extends androidx.room.RoomDatabase { void <init>(); }
