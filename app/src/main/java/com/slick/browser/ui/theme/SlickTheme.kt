package com.slick.browser.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.slick.browser.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val jetBrainsMonoGoogleFont = GoogleFont("JetBrains Mono")
private val kanitGoogleFont = GoogleFont("Kanit")

val JetBrainsMonoFamily = FontFamily(
    Font(
        googleFont = jetBrainsMonoGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ),
    Font(
        googleFont = jetBrainsMonoGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal
    ),
    Font(
        googleFont = jetBrainsMonoGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.ExtraBold,
        style = FontStyle.Normal
    )
)

val KanitLogoFamily = FontFamily(
    Font(
        googleFont = kanitGoogleFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Black,
        style = FontStyle.Italic
    )
)

@Immutable
data class SlickColors(
    val bg: Color,
    val fg: Color,
    val fgMuted: Color,
    val isLight: Boolean
)

val LocalSlickColors = staticCompositionLocalOf {
    SlickColors(
        bg = Color(0xFF000000),
        fg = Color(0xFFFFFFFF),
        fgMuted = Color(0x8CFFFFFF),
        isLight = false
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

val SlickTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = KanitLogoFamily,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        fontSize = 86.sp
    )
)

@Composable
fun SlickBrowserTheme(
    isLightTheme: Boolean,
    content: @Composable () -> Unit
) {
    val slickColors = if (isLightTheme) {
        SlickColors(
            bg = Color(0xFFFFFFFF),
            fg = Color(0xFF000000),
            fgMuted = Color(0x8C000000),
            isLight = true
        )
    } else {
        SlickColors(
            bg = Color(0xFF000000),
            fg = Color(0xFFFFFFFF),
            fgMuted = Color(0x8CFFFFFF),
            isLight = false
        )
    }

    // Принудительное переопределение цвета выделения текста и ползунков (никакого зелёного/бирюзового)
    val customTextSelectionColors = if (isLightTheme) {
        TextSelectionColors(
            handleColor = Color.Black,
            backgroundColor = Color.Black.copy(alpha = 0.22f)
        )
    } else {
        TextSelectionColors(
            handleColor = Color.White,
            backgroundColor = Color.White.copy(alpha = 0.28f)
        )
    }

    CompositionLocalProvider(
        LocalSlickColors provides slickColors,
        LocalTextSelectionColors provides customTextSelectionColors
    ) {
        MaterialTheme(
            colorScheme = if (isLightTheme) LightColorScheme else DarkColorScheme,
            typography = SlickTypography,
            content = content
        )
    }
}
