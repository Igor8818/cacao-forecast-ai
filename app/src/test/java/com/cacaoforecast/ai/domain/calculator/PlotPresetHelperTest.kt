package com.cacaoforecast.ai.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlotPresetHelperTest {

    @Test
    fun `presets de clone espaçamento e NPF possuem valores agronomicos validos`() {
        assertTrue(PlotPresetHelper.cloneFaPresets.all { it.fa > 0.0 })
        assertTrue(PlotPresetHelper.spacingPresets.all { it.plantsPerHa > 0 })
        assertEquals(listOf(10, 20, 30), PlotPresetHelper.npfPresets)
    }

    @Test
    fun `estimateNpt calcula plantas arredondadas e rejeita entradas invalidas`() {
        assertEquals(2778, PlotPresetHelper.estimateNpt(2.5, 1111))
        assertEquals(1333, PlotPresetHelper.estimateNpt(1.0, 1333))
        assertEquals(1, PlotPresetHelper.estimateNpt(0.0001, 650))

        assertNull(PlotPresetHelper.estimateNpt(0.0, 1111))
        assertNull(PlotPresetHelper.estimateNpt(-1.5, 1111))
        assertNull(PlotPresetHelper.estimateNpt(Double.NaN, 1111))
        assertNull(PlotPresetHelper.estimateNpt(Double.POSITIVE_INFINITY, 1111))
        assertNull(PlotPresetHelper.estimateNpt(2.0, 0))
    }
}
