package com.sedsoftware.bagcue.compose

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.sedsoftware.bagcue.root.RootComponent

class ProductShellContractTest {
    @Test
    fun compactWidthsUseNavigationBar() {
        assertEquals(ProductNavigationLayout.Bar, productNavigationLayout(599.dp))
    }

    @Test
    fun mediumAndExpandedWidthsUseNavigationRail() {
        assertEquals(ProductNavigationLayout.Rail, productNavigationLayout(600.dp))
        assertEquals(ProductNavigationLayout.Rail, productNavigationLayout(840.dp))
    }

    @Test
    fun zeroDurationScaleSelectsImmediateNavigationMotion() {
        assertTrue(reduceNavigationMotion(0f))
        assertFalse(reduceNavigationMotion(0.5f))
        assertFalse(reduceNavigationMotion(1f))
    }

    @Test
    fun largeFontNavigationKeepsFourDestinationsInStableTwoByTwoOrder() {
        val rows = primaryNavigationRows(fontScale = 2f)

        assertEquals(2, rows.size)
        assertEquals(
            listOf(
                RootComponent.PrimaryDestination.Today,
                RootComponent.PrimaryDestination.Sessions,
            ),
            rows[0].map(PrimaryDestinationUi::destination),
        )
        assertEquals(
            listOf(
                RootComponent.PrimaryDestination.Templates,
                RootComponent.PrimaryDestination.Settings,
            ),
            rows[1].map(PrimaryDestinationUi::destination),
        )
        assertEquals(4, rows.flatten().map(PrimaryDestinationUi::testTag).distinct().size)
    }

    @Test
    fun normalFontNavigationKeepsOneStableRow() {
        assertEquals(4, primaryNavigationRows(fontScale = 1f).single().size)
    }
}
