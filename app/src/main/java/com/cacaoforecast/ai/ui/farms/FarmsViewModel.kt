package com.cacaoforecast.ai.ui.farms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.repository.CacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FarmsViewModel(
    private val repository: CacaoRepository
) : ViewModel() {

    val farms: StateFlow<List<Farm>> = repository.observeActiveFarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun observeFarm(farmId: Long): Flow<Farm?> =
        repository.observeFarmById(farmId)

    fun observePlots(farmId: Long): Flow<List<Plot>> =
        repository.observeActivePlotsByFarm(farmId)

    fun observeFarmPlotsSummary(farmId: Long): Flow<Pair<Int, Double>> =
        repository.observeActivePlotsByFarm(farmId)
            .map { list -> list.size to list.sumOf { it.areaHa } }

    fun saveFarm(farm: Farm, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveFarm(farm)
                .onSuccess {
                    _message.value = if (farm.id == 0L) "Fazenda cadastrada." else "Fazenda atualizada."
                    onSuccess()
                }
                .onFailure {
                    _message.value = it.message ?: "Erro ao salvar fazenda."
                }
        }
    }

    fun archiveFarm(farmId: Long) {
        viewModelScope.launch {
            repository.archiveFarm(farmId)
                .onSuccess { _message.value = "Fazenda arquivada (histórico preservado)." }
                .onFailure { _message.value = it.message ?: "Erro ao arquivar fazenda." }
        }
    }

    fun savePlot(plot: Plot, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.savePlot(plot)
                .onSuccess {
                    _message.value = if (plot.id == 0L) "Roça / Quadra cadastrada." else "Roça / Quadra atualizada."
                    onSuccess()
                }
                .onFailure {
                    _message.value = it.message ?: "Erro ao salvar Roça / Quadra."
                }
        }
    }

    fun archivePlot(plotId: Long) {
        viewModelScope.launch {
            repository.archivePlot(plotId)
                .onSuccess { _message.value = "Roça / Quadra arquivada (histórico preservado)." }
                .onFailure { _message.value = it.message ?: "Erro ao arquivar Roça / Quadra." }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(private val repository: CacaoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FarmsViewModel(repository) as T
        }
    }
}
