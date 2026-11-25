package com.cacaoforecast.ai.domain.calculator

import com.cacaoforecast.ai.domain.model.DiseaseSeverity
import com.cacaoforecast.ai.domain.model.DominantDisease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ForecastCalculatorTest {

    @Test
    fun `calculo nominal calcula producao bruta NFT, liquida NFS, produtividade e perda em arrobas`() {
        // Exemplo CEPLAC: NFS = 180, NFD = 20 (NFT = 200), NPF = 20 -> 10 frutos/planta
        // NPT = 2000 plantas -> 20.000 frutos / FA 250 = 80 @ bruta; 18.000 / 250 = 72 @ liquida
        // Area = 2.0 ha -> produtividade bruta = 40 @/ha, liquida = 36 @/ha, perdas = 10%
        val metrics = ForecastCalculator.calculate(
            nfs = 180,
            nfd = 20,
            nPodridao = 15,
            nMoniliase = 0,
            nVassoura = 5,
            npf = 20,
            npt = 2000,
            fa = 250.0,
            areaHa = 2.0
        )

        assertEquals(80.0, metrics.producaoBrutaArrobas, 1e-6)
        assertEquals(72.0, metrics.producaoLiquidaArrobas, 1e-6)
        assertEquals(8.0, metrics.perdaArrobas, 1e-6)
        assertEquals(40.0, metrics.produtividadeBrutaPorHa, 1e-6)
        assertEquals(36.0, metrics.produtividadeLiquidaPorHa, 1e-6)
        assertEquals(10.0, metrics.perdasPct, 1e-6)
        assertEquals(DiseaseSeverity.WARNING, metrics.severity)
        assertEquals(DominantDisease.PODRIDAO_PARDA, metrics.dominantDisease)
    }

    @Test
    fun `entressafra com NFT zero retorna zero sem NaN ou Infinity`() {
        val metrics = ForecastCalculator.calculate(
            nfs = 0,
            nfd = 0,
            npf = 20,
            npt = 1500,
            fa = 240.0,
            areaHa = 1.5
        )

        assertFalse(metrics.perdasPct.isNaN())
        assertFalse(metrics.producaoBrutaArrobas.isNaN())
        assertFalse(metrics.produtividadeBrutaPorHa.isInfinite())
        assertEquals(0.0, metrics.producaoBrutaArrobas, 1e-9)
        assertEquals(0.0, metrics.producaoLiquidaArrobas, 1e-9)
        assertEquals(0.0, metrics.produtividadeBrutaPorHa, 1e-9)
        assertEquals(0.0, metrics.produtividadeLiquidaPorHa, 1e-9)
        assertEquals(0.0, metrics.perdaArrobas, 1e-9)
        assertEquals(0.0, metrics.perdasPct, 1e-9)
        assertEquals(DiseaseSeverity.CONTROLLED, metrics.severity)
        assertEquals(DominantDisease.NONE, metrics.dominantDisease)
    }

    @Test
    fun `semaforo fitossanitario classifica faixas exatas`() {
        assertEquals(DiseaseSeverity.CONTROLLED, ForecastCalculator.classifySeverity(9.99))
        assertEquals(DiseaseSeverity.WARNING, ForecastCalculator.classifySeverity(10.0))
        assertEquals(DiseaseSeverity.WARNING, ForecastCalculator.classifySeverity(19.99))
        assertEquals(DiseaseSeverity.CRITICAL, ForecastCalculator.classifySeverity(20.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejeita FA menor ou igual a zero`() {
        ForecastCalculator.calculate(
            nfs = 10,
            nfd = 2,
            npf = 20,
            npt = 1000,
            fa = 0.0,
            areaHa = 1.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejeita area menor ou igual a zero`() {
        ForecastCalculator.calculate(
            nfs = 10,
            nfd = 2,
            npf = 20,
            npt = 1000,
            fa = 250.0,
            areaHa = -0.5
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejeita NPF igual a zero`() {
        ForecastCalculator.calculate(
            nfs = 10,
            nfd = 2,
            npf = 0,
            npt = 1000,
            fa = 250.0,
            areaHa = 1.0
        )
    }
}
