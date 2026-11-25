package com.cacaoforecast.ai.ui.sampling

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cacaoforecast.ai.domain.ml.DetectedFruitBox
import com.cacaoforecast.ai.domain.ml.FruitAnalysisEngine
import com.cacaoforecast.ai.domain.ml.FruitHealthCategory
import com.cacaoforecast.ai.domain.ml.PlantPhotoAnalysisResult
import com.cacaoforecast.ai.domain.model.EvaluationSummary
import com.cacaoforecast.ai.domain.model.Farm
import com.cacaoforecast.ai.domain.model.Plot
import com.cacaoforecast.ai.domain.model.SamplingSessionDraft
import com.cacaoforecast.ai.domain.repository.CacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SamplingUiState(
    val selectedFarmId: Long? = null,
    val selectedPlotId: Long? = null,
    val pendingResumeDraft: SamplingSessionDraft? = null,
    val activeDraft: SamplingSessionDraft? = null,
    val engineReady: Boolean = false,
    val isPreparingEngine: Boolean = false,
    val isAnalyzingPhoto: Boolean = false,
    val currentPhotoUri: String? = null,
    val currentPlantIndex: Int = 1,
    val detectedBoxes: List<DetectedFruitBox> = emptyList(),
    val reviewNfs: Int = 0,
    val reviewNfd: Int = 0,
    val reviewPodridao: Int = 0,
    val reviewMoniliase: Int = 0,
    val reviewVassoura: Int = 0,
    val manualEdited: Boolean = false,
    val inferenceTimeMs: Long = 0L,
    val isSavingPlant: Boolean = false,
    val completedSummary: EvaluationSummary? = null,
    val errorMessage: String? = null
) {
    val canFinalize: Boolean
        get() = activeDraft != null && activeDraft.sampledCount == activeDraft.snapshotNpf
}

class SamplingViewModel(
    private val repository: CacaoRepository,
    private val fruitAnalysisEngine: FruitAnalysisEngine,
    private val sessionDirProvider: (Long) -> File
) : ViewModel() {

    val farms: StateFlow<List<Farm>> = repository.observeActiveFarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(SamplingUiState())
    val uiState: StateFlow<SamplingUiState> = _uiState.asStateFlow()

    fun observePlotsForFarm(farmId: Long) =
        repository.observeActivePlotsByFarm(farmId)

    fun initializeSetup(preselectedFarmId: Long?, preselectedPlotId: Long?) {
        viewModelScope.launch {
            val draft = if (preselectedPlotId != null && preselectedPlotId > 0L) {
                repository.findInProgressSession(preselectedPlotId) ?: repository.findAnyInProgressSession()
            } else {
                repository.findAnyInProgressSession()
            }
            _uiState.update { state ->
                state.copy(
                    selectedFarmId = preselectedFarmId?.takeIf { it > 0L } ?: state.selectedFarmId ?: draft?.farmId,
                    selectedPlotId = preselectedPlotId?.takeIf { it > 0L } ?: state.selectedPlotId ?: draft?.plotId,
                    pendingResumeDraft = draft,
                    errorMessage = null
                )
            }
        }
    }

    fun selectFarm(farmId: Long) {
        _uiState.update {
            it.copy(
                selectedFarmId = farmId,
                selectedPlotId = null,
                errorMessage = null
            )
        }
    }

    fun selectPlot(plotId: Long) {
        viewModelScope.launch {
            val draftForPlot = repository.findInProgressSession(plotId) ?: repository.findAnyInProgressSession()
            _uiState.update {
                it.copy(
                    selectedPlotId = plotId,
                    pendingResumeDraft = draftForPlot,
                    errorMessage = null
                )
            }
        }
    }

    fun startNewSession(plot: Plot, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingEngine = true, errorMessage = null) }
            val result = repository.startNewSamplingSession(plot)
            result.onSuccess { draft ->
                prepareEngineInternal()
                _uiState.update {
                    it.copy(
                        pendingResumeDraft = null,
                        activeDraft = draft,
                        currentPlantIndex = draft.nextPlantIndex,
                        isPreparingEngine = false
                    )
                }
                onStarted(draft.evaluationId)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isPreparingEngine = false,
                        errorMessage = err.message ?: "Não foi possível iniciar a amostragem."
                    )
                }
            }
        }
    }

    fun resumeSession(evaluationId: Long, onReady: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingEngine = true, errorMessage = null) }
            val draft = repository.getSessionDraftByEvaluationId(evaluationId)
            if (draft == null) {
                _uiState.update {
                    it.copy(
                        isPreparingEngine = false,
                        pendingResumeDraft = null,
                        errorMessage = "Sessão pendente não encontrada."
                    )
                }
                return@launch
            }
            prepareEngineInternal()
            _uiState.update {
                it.copy(
                    pendingResumeDraft = null,
                    activeDraft = draft,
                    currentPlantIndex = draft.nextPlantIndex,
                    isPreparingEngine = false
                )
            }
            onReady()
        }
    }

    fun loadSessionIfNeeded(evaluationId: Long) {
        val current = _uiState.value.activeDraft
        if (current != null && current.evaluationId == evaluationId && _uiState.value.engineReady) {
            return
        }
        resumeSession(evaluationId)
    }

    fun discardPendingDraft(evaluationId: Long) {
        viewModelScope.launch {
            repository.discardSamplingSession(evaluationId)
            val nextDraft = repository.findAnyInProgressSession()
            _uiState.update {
                it.copy(
                    pendingResumeDraft = nextDraft,
                    activeDraft = if (it.activeDraft?.evaluationId == evaluationId) null else it.activeDraft
                )
            }
        }
    }

    fun discardActiveSession(onDiscarded: () -> Unit) {
        val evalId = _uiState.value.activeDraft?.evaluationId
        viewModelScope.launch {
            if (evalId != null) {
                repository.discardSamplingSession(evalId)
            }
            fruitAnalysisEngine.releaseSession()
            _uiState.update {
                it.copy(
                    activeDraft = null,
                    pendingResumeDraft = null,
                    engineReady = false,
                    currentPhotoUri = null
                )
            }
            onDiscarded()
        }
    }

    fun saveDraftAndExit(onExited: () -> Unit) {
        fruitAnalysisEngine.releaseSession()
        _uiState.update {
            it.copy(
                engineReady = false,
                currentPhotoUri = null
            )
        }
        onExited()
    }

    fun sessionPhotoFile(evaluationId: Long, plantIndex: Int): File {
        val dir = sessionDirProvider(evaluationId)
        return File(dir, "plant_${plantIndex}.jpg")
    }

    fun analyzeCapturedPhoto(evaluationId: Long, plantIndex: Int, photoUriString: String) {
        viewModelScope.launch {
            val draft = _uiState.value.activeDraft ?: repository.getSessionDraftByEvaluationId(evaluationId)
            _uiState.update {
                it.copy(
                    activeDraft = draft,
                    currentPhotoUri = photoUriString,
                    currentPlantIndex = plantIndex,
                    isAnalyzingPhoto = true,
                    errorMessage = null
                )
            }
            val uri = Uri.parse(photoUriString)
            fruitAnalysisEngine.analyzePhoto(uri)
                .onSuccess { res -> applyAnalysisResult(res) }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isAnalyzingPhoto = false,
                            detectedBoxes = emptyList(),
                            reviewNfs = 0,
                            reviewNfd = 0,
                            reviewPodridao = 0,
                            reviewMoniliase = 0,
                            reviewVassoura = 0,
                            manualEdited = false,
                            errorMessage = err.message ?: "Falha na análise da foto."
                        )
                    }
                }
        }
    }

    private fun applyAnalysisResult(result: PlantPhotoAnalysisResult) {
        _uiState.update {
            it.copy(
                isAnalyzingPhoto = false,
                detectedBoxes = result.boxes,
                reviewNfs = result.countHealthy,
                reviewNfd = result.countDiseased,
                reviewPodridao = result.countPodridaoParda,
                reviewMoniliase = result.countMoniliase,
                reviewVassoura = result.countVassouraBruxa,
                manualEdited = false,
                inferenceTimeMs = result.inferenceTimeMs
            )
        }
    }

    /** Toque na caixa alterna: Sadio -> Doente (Podridão Parda) -> Remover. */
    fun cycleDetectedBox(boxId: String) {
        val current = _uiState.value
        val box = current.detectedBoxes.find { it.id == boxId } ?: return
        val nextBoxes = if (!box.category.isDiseased) {
            current.detectedBoxes.map {
                if (it.id == boxId) {
                    it.copy(category = FruitHealthCategory.PODRIDAO_PARDA, isManualOverride = true)
                } else {
                    it
                }
            }
        } else {
            current.detectedBoxes.filterNot { it.id == boxId }
        }
        recomputeReviewCountsFromBoxes(nextBoxes)
    }

    private fun recomputeReviewCountsFromBoxes(boxes: List<DetectedFruitBox>) {
        val healthy = boxes.count { !it.category.isDiseased }
        val podridao = boxes.count { it.category == FruitHealthCategory.PODRIDAO_PARDA }
        val moniliase = boxes.count { it.category == FruitHealthCategory.MONILIASE }
        val vassoura = boxes.count { it.category == FruitHealthCategory.VASSOURA_BRUXA }
        _uiState.update {
            it.copy(
                detectedBoxes = boxes,
                reviewNfs = healthy,
                reviewNfd = podridao + moniliase + vassoura,
                reviewPodridao = podridao,
                reviewMoniliase = moniliase,
                reviewVassoura = vassoura,
                manualEdited = true
            )
        }
    }

    fun adjustHealthyCount(delta: Int) {
        _uiState.update {
            it.copy(
                reviewNfs = (it.reviewNfs + delta).coerceAtLeast(0),
                manualEdited = true
            )
        }
    }

    fun adjustDiseasedCount(delta: Int) {
        _uiState.update { state ->
            val nextNfd = (state.reviewNfd + delta).coerceAtLeast(0)
            val diff = nextNfd - state.reviewNfd
            val nextPodridao = if (diff > 0) {
                state.reviewPodridao + diff
            } else {
                // Reduz proporcionalmente mantendo soma <= nextNfd
                state.reviewPodridao.coerceAtMost(nextNfd)
            }
            val remaining = (nextNfd - nextPodridao).coerceAtLeast(0)
            val nextMoniliase = state.reviewMoniliase.coerceAtMost(remaining)
            val nextVassoura = state.reviewVassoura.coerceAtMost((remaining - nextMoniliase).coerceAtLeast(0))
            state.copy(
                reviewNfd = nextNfd,
                reviewPodridao = nextPodridao,
                reviewMoniliase = nextMoniliase,
                reviewVassoura = nextVassoura,
                manualEdited = true
            )
        }
    }

    fun confirmCurrentPlant(
        onNextPlant: () -> Unit,
        onCompleted: (Long) -> Unit
    ) {
        val state = _uiState.value
        val draft = state.activeDraft ?: return
        if (state.isSavingPlant) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPlant = true, errorMessage = null) }
            val tempPath = state.currentPhotoUri?.let { Uri.parse(it).path }
            val saveRes = repository.savePlantSample(
                evaluationId = draft.evaluationId,
                plantIndex = state.currentPlantIndex,
                nfs = state.reviewNfs,
                nfd = state.reviewNfd,
                nPodridao = state.reviewPodridao,
                nMoniliase = state.reviewMoniliase,
                nVassoura = state.reviewVassoura,
                manualEdited = state.manualEdited,
                tempPhotoPath = tempPath
            )
            saveRes.onSuccess { updatedDraft ->
                if (updatedDraft.isReadyToFinalize) {
                    finalizeSessionInternal(updatedDraft.evaluationId, onCompleted)
                } else {
                    _uiState.update {
                        it.copy(
                            activeDraft = updatedDraft,
                            currentPlantIndex = updatedDraft.nextPlantIndex,
                            currentPhotoUri = null,
                            detectedBoxes = emptyList(),
                            isSavingPlant = false
                        )
                    }
                    onNextPlant()
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSavingPlant = false,
                        errorMessage = err.message ?: "Erro ao salvar amostra da planta."
                    )
                }
            }
        }
    }

    /** Trava rígida: impede finalização se sampledCount != snapshotNpf. */
    fun finalizeIfReady(evaluationId: Long, onCompleted: (Long) -> Unit) {
        val draft = _uiState.value.activeDraft
        if (draft == null || draft.sampledCount != draft.snapshotNpf) {
            _uiState.update {
                it.copy(errorMessage = "Complete todas as ${draft?.snapshotNpf ?: 0} plantas antes de finalizar.")
            }
            return
        }
        viewModelScope.launch {
            finalizeSessionInternal(evaluationId, onCompleted)
        }
    }

    fun loadCompletedSummary(evaluationId: Long) {
        viewModelScope.launch {
            val summary = repository.getEvaluationById(evaluationId)
            _uiState.update { it.copy(completedSummary = summary) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun finalizeSessionInternal(evaluationId: Long, onCompleted: (Long) -> Unit) {
        repository.finalizeSamplingSession(evaluationId)
            .onSuccess { id ->
                fruitAnalysisEngine.releaseSession()
                val summary = repository.getEvaluationById(id)
                _uiState.update {
                    it.copy(
                        activeDraft = null,
                        pendingResumeDraft = null,
                        engineReady = false,
                        currentPhotoUri = null,
                        isSavingPlant = false,
                        completedSummary = summary
                    )
                }
                onCompleted(id)
            }
            .onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSavingPlant = false,
                        errorMessage = err.message ?: "Falha ao concluir avaliação."
                    )
                }
            }
    }

    private suspend fun prepareEngineInternal() {
        fruitAnalysisEngine.prepareSession()
            .onSuccess {
                _uiState.update { it.copy(engineReady = true) }
            }
            .onFailure { err ->
                _uiState.update {
                    it.copy(
                        engineReady = false,
                        errorMessage = err.message ?: "Falha ao carregar modelos TFLite."
                    )
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        fruitAnalysisEngine.releaseSession()
    }

    class Factory(
        private val repository: CacaoRepository,
        private val fruitAnalysisEngine: FruitAnalysisEngine,
        private val sessionDirProvider: (Long) -> File
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SamplingViewModel(repository, fruitAnalysisEngine, sessionDirProvider) as T
        }
    }
}
