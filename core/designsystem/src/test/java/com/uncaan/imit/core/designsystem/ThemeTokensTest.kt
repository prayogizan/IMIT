package com.uncaan.imit.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.uncaan.imit.core.designsystem.theme.DarkColorScheme
import com.uncaan.imit.core.designsystem.theme.LightColorScheme
import com.uncaan.imit.core.designsystem.theme.MitRed
import com.uncaan.imit.core.designsystem.theme.Shapes
import com.uncaan.imit.core.designsystem.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun mitBrandColors_areCorrectlyDefined() {
        assertEquals(Color(0xFFA31F34), MitRed)
    }

    @Test
    fun lightColorScheme_usesMitRedAsPrimary() {
        assertEquals(MitRed, LightColorScheme.primary)
    }

    @Test
    fun darkColorScheme_hasDistinctPrimary() {
        assertNotEquals(LightColorScheme.primary, DarkColorScheme.primary)
    }

    @Test
    fun spacingDefaults_matchDesignSpecs() {
        val spacing = Spacing()
        assertEquals(0.dp, spacing.none)
        assertEquals(4.dp, spacing.extraSmall)
        assertEquals(8.dp, spacing.small)
        assertEquals(16.dp, spacing.medium)
        assertEquals(24.dp, spacing.large)
        assertEquals(32.dp, spacing.extraLarge)
        assertEquals(48.dp, spacing.huge)
    }

    @Test
    fun shapes_haveValidCornerRadii() {
        assertEquals(4.dp, (Shapes.extraSmall as androidx.compose.foundation.shape.RoundedCornerShape).topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)
    }
}
