package com.uncaan.imit.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// MIT Brand Palette Tokens
val MitRed = Color(0xFFA31F34)
val MitRedDark = Color(0xFF751221)
val MitRedLight = Color(0xFFFFB3B6)
val MitRedContainerLight = Color(0xFFFFDAD9)
val MitRedContainerDark = Color(0xFF840520)

val MitSilverGray = Color(0xFF8A8B8C)
val MitDarkGray = Color(0xFF232528)
val MitLightGray = Color(0xFFF2F4F8)

// Light Color Scheme
val LightColorScheme = lightColorScheme(
    primary = MitRed,
    onPrimary = Color.White,
    primaryContainer = MitRedContainerLight,
    onPrimaryContainer = Color(0xFF40000A),
    secondary = Color(0xFF775656),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD9),
    onSecondaryContainer = Color(0xFF2C1516),
    tertiary = Color(0xFF755A2F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF291800),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCF8F8),
    onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFCF8F8),
    onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFF4DDDD),
    onSurfaceVariant = Color(0xFF524343),
    outline = Color(0xFF857373),
    outlineVariant = Color(0xFFD7C1C1)
)

// Dark Color Scheme
val DarkColorScheme = darkColorScheme(
    primary = MitRedLight,
    onPrimary = Color(0xFF680016),
    primaryContainer = MitRedContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE6BDBC),
    onSecondary = Color(0xFF44292A),
    secondaryContainer = Color(0xFF5D3F3F),
    onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFFE5C18D),
    onTertiary = Color(0xFF422C05),
    tertiaryContainer = Color(0xFF5B421A),
    onTertiaryContainer = Color(0xFFFFDDB0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF181212),
    onBackground = Color(0xFFEDE0DF),
    surface = Color(0xFF181212),
    onSurface = Color(0xFFEDE0DF),
    surfaceVariant = Color(0xFF524343),
    onSurfaceVariant = Color(0xFFD7C1C1),
    outline = Color(0xFFA08C8C),
    outlineVariant = Color(0xFF524343)
)
