package com.gymtracker.app.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {

    @Test
    fun testTealPrimaryColorTokens() {
        assertEquals(Color(0xFF0F766E), LightPrimary)
        assertEquals(Color(0xFF2DD4BF), DarkPrimary)
    }

    @Test
    fun testLightAndDarkExtendedSemanticColors() {
        assertEquals(LightSuccess, LightExtendedColors.success)
        assertEquals(LightWarning, LightExtendedColors.warning)
        assertEquals(LightInfo, LightExtendedColors.info)

        assertEquals(DarkSuccess, DarkExtendedColors.success)
        assertEquals(DarkWarning, DarkExtendedColors.warning)
        assertEquals(DarkInfo, DarkExtendedColors.info)
    }

    @Test
    fun testTypographyRolesAndTabularNumerals() {
        // 28/34 screen title
        assertEquals(28.sp, ScreenTitleStyle.fontSize)
        assertEquals(34.sp, ScreenTitleStyle.lineHeight)

        // 20/26 section title
        assertEquals(20.sp, SectionTitleStyle.fontSize)
        assertEquals(26.sp, SectionTitleStyle.lineHeight)

        // 16/24 body
        assertEquals(16.sp, BodyStyle.fontSize)
        assertEquals(24.sp, BodyStyle.lineHeight)

        // 14/20 label
        assertEquals(14.sp, LabelStyle.fontSize)
        assertEquals(20.sp, LabelStyle.lineHeight)

        // 32/38 metric tabular numerals
        assertEquals(32.sp, MetricStyle.fontSize)
        assertEquals(38.sp, MetricStyle.lineHeight)
        assertNotNull(MetricStyle.fontFeatureSettings)
        assertTrue(MetricStyle.fontFeatureSettings!!.contains(TABULAR_NUMERALS_FEATURE))

        // Small tabular metric
        assertNotNull(MetricTabularSmallStyle.fontFeatureSettings)
        assertTrue(MetricTabularSmallStyle.fontFeatureSettings!!.contains(TABULAR_NUMERALS_FEATURE))
    }

    @Test
    fun testSpacingScaleTokens() {
        val spacing = Spacing()
        assertEquals(4.dp, spacing.xxSmall)
        assertEquals(8.dp, spacing.xSmall)
        assertEquals(12.dp, spacing.small)
        assertEquals(16.dp, spacing.medium)
        assertEquals(24.dp, spacing.large)
        assertEquals(32.dp, spacing.xLarge)

        // Layout & Phone Gutters
        assertEquals(16.dp, spacing.phoneGutter)
        assertEquals(12.dp, spacing.controlSpacing)
        assertEquals(16.dp, spacing.cardPadding)

        // Dense Row Padding Exceptions
        assertEquals(6.dp, spacing.denseRowVerticalPadding)
        assertEquals(8.dp, spacing.denseRowHorizontalPadding)
    }
}
