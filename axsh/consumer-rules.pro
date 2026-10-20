-dontwarn android.app.**
-dontwarn android.content.**
-dontwarn android.os.**
-dontwarn android.view.**
-dontwarn android.hardware.**
-dontwarn android.permission.**
-dontwarn android.ddm.**
-dontwarn com.android.internal.**

-keep class frb.axeron.axsh.Bootstrap {
    public static void main(java.lang.String[]);
}
