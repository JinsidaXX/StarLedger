# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
