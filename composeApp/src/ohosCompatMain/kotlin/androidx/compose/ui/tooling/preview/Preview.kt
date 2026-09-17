// CPF-KMP-CMP OHOS compat shim.
//
// Compose Multiplatform publishes `@Preview` (and friends) under `org.jetbrains.compose.ui.tooling.preview`,
// which does not exist on Android, where shared code imports `androidx.compose.ui.tooling.preview`.
// The androidx artifact is Android-only, so it is absent from the OHOS compile classpath.
//
// Declaring the androidx type here keeps `composeApp/src/commonMain` identical to upstream, so the
// OHOS branch does not have to rewrite every file that contains a preview. The values are inert:
// `@Preview` is only read by IDE tooling.
package androidx.compose.ui.tooling.preview

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Repeatable
annotation class Preview(
    val name: String = "",
    val group: String = "",
    val apiLevel: Int = -1,
    val widthDp: Int = -1,
    val heightDp: Int = -1,
    val locale: String = "",
    val fontScale: Float = 1f,
    val showSystemUi: Boolean = false,
    val showBackground: Boolean = false,
    val backgroundColor: Long = 0,
    val uiMode: Int = 0,
    val device: String = Devices.DEFAULT,
    val wallpaper: Int = 0,
)

/**
 * Device specs accepted by [Preview.device]. Only the constant names matter on OHOS; the preview
 * renderer that would interpret the spec strings is Android-only.
 */
object Devices {
    const val DEFAULT = ""
    const val NEXUS_5 = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=440"
    const val NEXUS_7 = "spec:shape=Normal,width=600,height=960,unit=dp,dpi=320"
    const val NEXUS_9 = "spec:shape=Normal,width=800,height=1280,unit=dp,dpi=320"
    const val PIXEL_2 = "spec:shape=Normal,width=411,height=731,unit=dp,dpi=420"
    const val PIXEL_3 = "spec:shape=Normal,width=393,height=786,unit=dp,dpi=440"
    const val PIXEL_4 = "spec:shape=Normal,width=393,height=829,unit=dp,dpi=440"
    const val PIXEL_5 = "spec:shape=Normal,width=393,height=851,unit=dp,dpi=440"
    const val PIXEL_6 = "spec:shape=Normal,width=411,height=914,unit=dp,dpi=420"
    const val PIXEL_C = "spec:shape=Normal,width=891,height=891,unit=dp,dpi=420"
    const val PIXEL_TABLET = "spec:shape=Normal,width=1280,height=800,unit=dp,dpi=240"
    const val PIXEL_FOLD = "spec:shape=Normal,width=673,height=841,unit=dp,dpi=480"
    const val DESKTOP = "spec:width=1280dp,height=1024dp,dpi=240"
    const val AUTOMOTIVE_1024p = "spec:width=1024dp,height=768dp,dpi=240"
}
