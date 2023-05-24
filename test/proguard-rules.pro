-keep public class androidx.tracing.Trace {
   *;
}
#
#-keep public class com.hcaptcha.sdk.test.** {
#   *;
#}
#
## -dontobfuscate
#
#-keepclassmembers @org.junit.runner.RunWith public class ** {
#    @org.junit.runner.Test public *;
#}

-keep class com.hcaptcha.sdk.**