# Libraries ship their own consumer rules (Hilt, Room, Retrofit, OkHttp, kotlinx.serialization).
# Add rules here only for code reached by reflection, with a comment saying why.

# Navigation 3 restores the back stack after process death by looking up each key's serializer.
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
