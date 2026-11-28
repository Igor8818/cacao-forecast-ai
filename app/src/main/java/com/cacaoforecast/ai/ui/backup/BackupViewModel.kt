package com.cacaoforecast.ai.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cacaoforecast.ai.data.backup.SqliteBackupManager
import com.cacaoforecast.ai.data.demo.DemoSeedManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class BackupUiState(
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

class BackupViewModel(
    private val backupManager: SqliteBackupManager,
    private val demoSeedManager: DemoSeedManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportToUri(destUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            backupManager.exportToUri(destUri)
                .onSuccess { bytes ->
                    val kb = (bytes / 1024L).coerceAtLeast(1L)
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = "Backup exportado com sucesso ($kb KB).",
                            isError = false
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = err.message ?: "Erro ao exportar backup.",
                            isError = true
                        )
                    }
                }
        }
    }

    fun exportForShare(onReady: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            backupManager.exportToShareableFile()
                .onSuccess { file ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = "Arquivo .db pronto para compartilhamento.",
                            isError = false
                        )
                    }
                    onReady(file)
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = err.message ?: "Erro ao preparar backup.",
                            isError = true
                        )
                    }
                }
        }
    }

    fun importFromUri(sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            backupManager.importFromUri(sourceUri)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = "Backup restaurado e validado com sucesso!",
                            isError = false
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = "Importação rejeitada: ${err.message ?: "arquivo inválido"}. A base atual foi mantida intacta.",
                            isError = true
                        )
                    }
                }
        }
    }

    fun loadSempreVivaDemo() {
        val seeder = demoSeedManager ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            seeder.seedSempreVivaDemo()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = "Fazenda \"Sempre Viva\" carregada para demonstração!",
                            isError = false
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = err.message ?: "Erro ao carregar fazenda de demonstração.",
                            isError = true
                        )
                    }
                }
        }
    }

    class Factory(
        private val backupManager: SqliteBackupManager,
        private val demoSeedManager: DemoSeedManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupViewModel(backupManager, demoSeedManager) as T
        }
    }
}
