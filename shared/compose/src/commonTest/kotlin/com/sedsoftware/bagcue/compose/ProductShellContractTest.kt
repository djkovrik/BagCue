package com.sedsoftware.bagcue.compose

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
