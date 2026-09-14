# Keep Oblivion public API classes and methods from being stripped when used as a dependency
-keep class io.oblivion.android.Oblivion { *; }
-keep class io.oblivion.domain.model.** { *; }
-keep class io.oblivion.domain.port.** { *; }
-dontwarn java.lang.management.**
