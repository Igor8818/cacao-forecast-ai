package com.cacaoforecast.ai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cacaoforecast.ai.domain.model.EvaluationSummary
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.repository.CacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlotHistoryViewModel(
    private val plotId: Long,
    private val repository: CacaoRepository
) : ViewModel() {

    val evaluations: StateFlow<List<EvaluationSummary>> =
        repository.observeCompletedEvaluationsByPlot(plotId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _plot = MutableStateFlow<Plot?>(null)
    val plot: StateFlow<Plot?> = _plot.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            _plot.value = repository.getPlotById(plotId)
        }
    }

    fun updateRealProduction(evaluationId: Long, realArrobas: Double?, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateRealProduction(evaluationId, realArrobas)
                .onSuccess {
                    _message.value = if (realArrobas == null) {
                        "Produção real removida."
                    } else {
                        "Produção real salva no histórico."
                    }
                    onSuccess()
                }
                .onFailure { err ->
                    _message.value = err.message ?: "Erro ao salvar produção real."
                }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(
        private val plotId: Long,
        private val repository: CacaoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlotHistoryViewModel(plotId, repository) as T
        }
    }
}
