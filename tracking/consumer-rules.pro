# Consumer ProGuard rules for the tracking module.

# AndroidPlot configures renderers and formatters through typed attributes.
-keep class com.androidplot.** { *; }

# Commons CSV references this CLASS-retained analysis annotation only.
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
