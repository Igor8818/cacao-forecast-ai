package com.cacaoforecast.ai.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPaginationMathTest {

    @Test
    fun `mais de 25 quadras paginam sem cortar linhas e com rodape unico na ultima pagina`() {
        val slices = PdfPaginationMath.computePageSlices(
            rowCount = 40,
            firstPageCapacity = 18,
            nextPageCapacity = 24,
            footerRowEquivalent = 4
        )

        assertTrue(slices.size >= 2)
        // Verifica contiguidade perfeita de 0..40 sem sobreposicao ou perda de linha
        var expectedStart = 0
        var totalRowsCovered = 0
        slices.forEach { slice ->
            assertEquals(expectedStart, slice.startRowInclusive)
            assertTrue(slice.endRowExclusive >= slice.startRowInclusive)
            totalRowsCovered += (slice.endRowExclusive - slice.startRowInclusive)
            expectedStart = slice.endRowExclusive
        }
        assertEquals(40, totalRowsCovered)
        assertEquals(1, slices.count { it.renderFooter })
        assertTrue(slices.last().renderFooter)
    }

    @Test
    fun `cria pagina extra para rodape quando a ultima pagina de dados fica cheia`() {
        // 18 linhas na pagina 1 (capacidade 18 -> sobra 0 espaco para rodape)
        val slices = PdfPaginationMath.computePageSlices(
            rowCount = 18,
            firstPageCapacity = 18,
            nextPageCapacity = 24,
            footerRowEquivalent = 4
        )

        assertEquals(2, slices.size)
        assertEquals(0, slices[0].startRowInclusive)
        assertEquals(18, slices[0].endRowExclusive)
        assertEquals(false, slices[0].renderFooter)
        assertEquals(18, slices[1].startRowInclusive)
        assertEquals(18, slices[1].endRowExclusive)
        assertEquals(true, slices[1].renderFooter)
    }

    @Test
    fun `suporta 75 quadras e tambem relatorio vazio com disclaimer`() {
        val many = PdfPaginationMath.computePageSlices(75, 16, 24, 4)
        assertEquals(4, many.size)
        assertEquals(75, many.sumOf { it.endRowExclusive - it.startRowInclusive })
        assertEquals(1, many.count { it.renderFooter })

        val empty = PdfPaginationMath.computePageSlices(0, 16, 24, 4)
        assertEquals(1, empty.size)
        assertTrue(empty.single().renderFooter)
    }
}
