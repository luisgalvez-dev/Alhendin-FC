package com.luis.alhendinfc.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.backup.BackupRepository
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.CustomStatAppliesTo
import com.luis.alhendinfc.domain.model.CustomStatType
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackupUiMessage(
    val text: String,
    val isError: Boolean = false,
    val requireRestart: Boolean = false
)

class EventTypesViewModel(
    private val repository: CustomStatTypeRepositoryImpl,
    private val backupRepository: BackupRepository,
    private val teamId: Int
) : ViewModel() {

    val types: StateFlow<List<CustomStatType>> = repository.getByTeam(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backupMessage = MutableStateFlow<BackupUiMessage?>(null)
    val backupMessage: StateFlow<BackupUiMessage?> = _backupMessage.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val summary = backupRepository.exportToUri(uri)
                _backupMessage.value = BackupUiMessage(
                    summary.asMessage(
                        "Copia completa exportada (toda la BD + estadísticas). Guárdala en el PC."
                    )
                )
            } catch (e: Exception) {
                _backupMessage.value = BackupUiMessage(
                    text = "Error al exportar: ${e.message ?: "desconocido"}",
                    isError = true
                )
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val summary = backupRepository.importFromUri(uri)
                _backupMessage.value = BackupUiMessage(
                    text = summary.asMessage(
                        "Datos restaurados (incluye eventos y estadísticas). " +
                            "Se guardó una copia previa automática. " +
                            "Cierra la app por completo y vuelve a abrirla."
                    ),
                    requireRestart = true
                )
            } catch (e: Exception) {
                _backupMessage.value = BackupUiMessage(
                    text = "Error al importar: ${e.message ?: "desconocido"}",
                    isError = true
                )
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun add(label: String, shortLabel: String, appliesTo: CustomStatAppliesTo) {
        val clean = label.trim()
        if (clean.isEmpty()) return
        val short = shortLabel.trim().ifBlank { clean.take(8) }
        viewModelScope.launch {
            val order = (types.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            repository.add(
                CustomStatType(
                    teamId = teamId,
                    code = "",
                    label = clean,
                    shortLabel = short,
                    appliesTo = appliesTo,
                    sortOrder = order
                )
            )
        }
    }

    fun update(
        type: CustomStatType,
        label: String,
        shortLabel: String,
        appliesTo: CustomStatAppliesTo
    ) {
        val clean = label.trim()
        if (clean.isEmpty()) return
        val short = shortLabel.trim().ifBlank { clean.take(8) }
        viewModelScope.launch {
            repository.update(
                type.copy(
                    label = clean,
                    shortLabel = short,
                    appliesTo = appliesTo,
                    isActive = true
                )
            )
        }
    }

    fun setActive(type: CustomStatType, active: Boolean) {
        viewModelScope.launch {
            repository.update(type.copy(isActive = active))
        }
    }

    fun delete(type: CustomStatType) {
        viewModelScope.launch {
            repository.deleteOrDeactivate(type)
        }
    }

    companion object {
        fun factory(context: Context, teamId: Int) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext
                val db = AlhendinDatabase.getInstance(app)
                @Suppress("UNCHECKED_CAST")
                return EventTypesViewModel(
                    CustomStatTypeRepositoryImpl(db.customStatTypeDao()),
                    BackupRepository.getInstance(app),
                    teamId
                ) as T
            }
        }
    }
}
