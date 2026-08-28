package io.github.uplush.dingoobox

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class AppLanguage(
    val preferenceValue: String,
    val localeTag: String?,
    val displayNameResource: Int
) {
    SYSTEM(
        preferenceValue = "system",
        localeTag = null,
        displayNameResource =
            R.string.language_system
    ),
    SIMPLIFIED_CHINESE(
        preferenceValue = "zh-CN",
        localeTag = "zh-CN",
        displayNameResource =
            R.string.language_simplified_chinese
    ),
    ENGLISH(
        preferenceValue = "en",
        localeTag = "en",
        displayNameResource =
            R.string.language_english
    );

    companion object {
        fun fromPreferenceValue(
            value: String?
        ): AppLanguage {
            return values()
                .firstOrNull { language ->
                    language.preferenceValue == value
                }
                ?: SYSTEM
        }
    }
}

private val LocalAppResources =
    staticCompositionLocalOf<Resources> {
        error(
            "App language resources are not available."
        )
    }

private val LocalAppLocale =
    staticCompositionLocalOf<Locale> {
        Locale.getDefault()
    }

internal fun Context.resourcesForAppLanguage(
    language: AppLanguage,
    baseConfiguration: Configuration =
        resources.configuration
): Resources {
    if (language.localeTag == null) {
        return resources
    }

    val locale =
        Locale.forLanguageTag(
            language.localeTag
        )

    val configuration =
        Configuration(
            baseConfiguration
        ).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }

    return createConfigurationContext(
        configuration
    ).resources
}

@Composable
internal fun AppLanguageProvider(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val context =
        LocalContext.current

    val baseConfiguration =
        LocalConfiguration.current

    val locale =
        if (language.localeTag == null) {
            baseConfiguration.locales[0]
        } else {
            Locale.forLanguageTag(
                language.localeTag
            )
        }

    val resources =
        if (language.localeTag == null) {
            context.resources
        } else {
            val configuration =
                Configuration(
                    baseConfiguration
                ).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }

            context
                .createConfigurationContext(
                    configuration
                )
                .resources
        }

    CompositionLocalProvider(
        LocalAppResources provides resources,
        LocalAppLocale provides locale,
        content = content
    )
}

@Composable
fun appLocale(): Locale =
    LocalAppLocale.current

@Composable
fun appResources(): Resources =
    LocalAppResources.current

@Composable
fun appStringResource(
    resourceId: Int,
    vararg formatArguments: Any
): String {
    val resources =
        LocalAppResources.current

    return if (formatArguments.isEmpty()) {
        resources.getString(
            resourceId
        )
    } else {
        resources.getString(
            resourceId,
            *formatArguments
        )
    }
}