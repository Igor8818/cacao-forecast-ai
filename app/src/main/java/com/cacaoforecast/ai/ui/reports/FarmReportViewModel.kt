package com.cacaoforecast.ai.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cacaoforecast.ai.domain.calculator.CumulativeStatsCalculator
import com.cacaoforecast.ai.domain.model.CumulativePlotReport
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.FarmReportUiModel
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.ProductionBasis
import com.cacaoforecast.ai.domain.model.ReportSortOrder
import com.cacaoforecast.ai.domain.repository.CacaoRepository
import com.cacaoforecast.ai.pdf.CsvReportExporter
import com.cacaoforecast.ai.pdf.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

data class ReportFilterState(
    val selectedFarmId: Long? = null,
    val selectedPlotId: Long? = null, // null = Todas as Quadras
    val startMillis: Long,
    val endMillis: Long,
    val periodLabel: String,
    val basis: ProductionBasis = ProductionBasis.NET_HEALTHY_NFS,
    val sortOrder: ReportSortOrder = ReportSortOrder.LOSS_DESC,
    val cumulativePlotId: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class FarmReportViewModel(
    private val repository: CacaoRepository,
    private val pdfReportGenerator: PdfReportGenerator,
    private val csvReportExporter: CsvReportExporter
) : ViewModel() {

    private val activeFarmsSource = repository.observeActiveFarms()

    val farms: StateFlow<List<Farm>> = activeFarmsSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filters = MutableStateFlow(currentMonthFilter())
    val filters: StateFlow<ReportFilterState> = _filters.asStateFlow()

    private val effectiveFarmId = combine(activeFarmsSource, _filters) { farmList, f ->
        f.selectedFarmId?.takeIf { id -> farmList.any { it.id == id } } ?: farmList.firstOrNull()?.id
    }

    private val plotsSource = effectiveFarmId.flatMapLatest { farmId ->
        if (farmId == null) flowOf(emptyList()) else repository.observeActivePlotsByFarm(farmId)
    }

    val plotsForSelectedFarm: StateFlow<List<Plot>> = plotsSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val report: StateFlow<FarmReportUiModel?> = combine(effectiveFarmId, _filters) { farmId, f ->
        farmId to f
    }.flatMapLatest { (farmId, f) ->
        if (farmId == null) {
            flowOf(null)
        } else {
            repository.observeFarmReport(
                farmId = farmId,
                startMillis = f.startMillis,
                endMillis = f.endMillis,
                selectedPlotId = f.selectedPlotId,
                basis = f.basis,
                sortOrder = f.sortOrder,
                periodLabel = f.periodLabel
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val cumulativeReport: StateFlow<CumulativePlotReport> = combine(plotsSource, _filters) { plotList, f ->
        val plotId = f.cumulativePlotId?.takeIf { id -> plotList.any { it.id == id } }
            ?: plotList.firstOrNull()?.id
        plotId to f.basis
    }.flatMapLatest { (plotId, basis) ->
        if (plotId == null) {
            flowOf(CumulativeStatsCalculator.build(null, emptyList(), basis))
        } else {
            repository.observeCompletedEvaluationsByPlot(plotId)
                .flatMapLatest { evals ->
                    flowOf(CumulativeStatsCalculator.build(plotId, evals, basis))
                }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CumulativeStatsCalculator.build(null, emptyList(), ProductionBasis.NET_HEALTHY_NFS)
    )

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectFarm(farmId: Long) {
        _filters.update {
            it.copy(
                selectedFarmId = farmId,
                selectedPlotId = null,
                cumulativePlotId = null
            )
        }
    }

    fun selectPlotFilter(plotId: Long?) {
        _filters.update { it.copy(selectedPlotId = plotId) }
    }

    fun selectCumulativePlot(plotId: Long) {
        _filters.update { it.copy(cumulativePlotId = plotId) }
    }

    fun selectBasis(basis: ProductionBasis) {
        _filters.update { it.copy(basis = basis) }
    }

    fun selectSortOrder(sortOrder: ReportSortOrder) {
        _filters.update { it.copy(sortOrder = sortOrder) }
    }

    fun selectPeriod(startMillis: Long, endMillis: Long, label: String) {
        _filters.update {
            it.copy(
                startMillis = startMillis,
                endMillis = endMillis,
                periodLabel = label
            )
        }
    }

    fun generatePdf(onGenerated: (File, String) -> Unit) {
        val currentReport = report.value ?: return
        if (currentReport.rows.isEmpty()) {
            _message.value = "Não há avaliações concluídas neste período para gerar PDF."
            return
        }
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            val res = withContext(Dispatchers.IO) {
                pdfReportGenerator.generate(currentReport)
            }
            _isGeneratingPdf.value = false
            res.onSuccess { file ->
                val caption = "Relatório CacaoForecastAI • ${currentReport.farm.nome} (${currentReport.periodLabel})"
                onGenerated(file, caption)
            }.onFailure { err ->
                _message.value = err.message ?: "Erro ao gerar PDF."
            }
        }
    }

    fun generateCsv(onGenerated: (File, String) -> Unit) {
        val currentReport = report.value ?: return
        if (currentReport.rows.isEmpty()) {
            _message.value = "Não há avaliações concluídas neste período para exportar planilha CSV."
            return
        }
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            val res = withContext(Dispatchers.IO) {
                csvReportExporter.export(currentReport)
            }
            _isGeneratingPdf.value = false
            res.onSuccess { file ->
                val caption = "Planilha CSV CacaoForecastAI • ${currentReport.farm.nome} (${currentReport.periodLabel})"
                onGenerated(file, caption)
            }.onFailure { err ->
                _message.value = err.message ?: "Erro ao exportar CSV."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        fun monthRange(year: Int, monthZeroBased: Int): Pair<Long, Long> {
            val start = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroBased)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val end = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroBased)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            return start to end
        }

        private fun currentMonthFilter(): ReportFilterState {
            val cal = Calendar.getInstance()
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val (start, end) = monthRange(y, m)
            return ReportFilterState(
                startMillis = start,
                endMillis = end,
                periodLabel = formatMonthLabel(y, m)
            )
        }

        fun formatMonthLabel(year: Int, monthZeroBased: Int): String {
            val months = listOf(
                "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
                "Jul", "Ago", "Set", "Out", "Nov", "Dez"
            )
            return "${months.getOrElse(monthZeroBased) { "Mês" }}/$year"
        }
    }

    class Factory(
        private val repository: CacaoRepository,
        private val pdfReportGenerator: PdfReportGenerator,
        private val csvReportExporter: CsvReportExporter
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FarmReportViewModel(repository, pdfReportGenerator, csvReportExporter) as T
        }
    }
}
