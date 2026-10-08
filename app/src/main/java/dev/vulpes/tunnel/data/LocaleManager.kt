package dev.vulpes.tunnel.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the user's language choice to a [Context].
 *
 * This deliberately avoids `androidx.appcompat`'s per-app locale support, which is the usual way
 * to do it but drags in the whole AppCompat library. Wrapping the base context is a dozen lines
 * and adds nothing to the APK.
 */
object LocaleManager {

    /** Follow whatever language the device is set to. */
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val PERSIAN = "fa"

    val SUPPORTED = listOf(SYSTEM, ENGLISH, PERSIAN)

    fun wrap(context: Context, tag: String): Context {
        if (tag == SYSTEM || tag.isBlank()) return context

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
