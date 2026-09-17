package world.hachimi.app.i18n

import androidx.compose.runtime.*
import androidx.compose.ui.text.intl.OhosPlatformLocale
import androidx.compose.ui.text.platform.OhosTextPlatformProvider
import androidx.compose.ui.text.platform.OhosTextPlatformProviderRegistry

actual object LocalAppLocale {
    private val local = staticCompositionLocalOf { "zh-CN" }
    private var adapter: LocaleProvider? = null
    actual val current: String @Composable get() = local.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val provider = OhosTextPlatformProviderRegistry.getTextPlatformProvider()
        val wrapped = adapter ?: provider?.let { LocaleProvider(it).also { adapter = it } }
        if (wrapped != null) {
            wrapped.selected = value
            OhosTextPlatformProviderRegistry.registerTextPlatformProvider(wrapped)
        }
        return local provides (value ?: wrapped?.getLanguageTag() ?: "zh-CN")
    }
}

private class LocaleProvider(private val system: OhosTextPlatformProvider) : OhosTextPlatformProvider by system {
    var selected: String? = null
    private fun locale(): OhosPlatformLocale = system.parseLanguageTag(selected ?: system.getLanguageTag())
    override fun getLanguageTag(): String = selected ?: system.getLanguageTag()
    override fun getLanguage(): String = locale().language
    override fun getRegion(): String = locale().region
    override fun getScript(): String = locale().script
    override fun isRtl(): Boolean = locale().isRtl
    override fun getPreferredLanguages(): List<OhosPlatformLocale> =
        if (selected == null) system.getPreferredLanguages() else listOf(locale())
}
