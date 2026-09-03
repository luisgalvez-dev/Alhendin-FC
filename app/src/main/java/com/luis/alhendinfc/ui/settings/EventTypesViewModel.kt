package com.luis.alhendinfc.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.CustomStatAppliesTo
import com.luis.alhendinfc.domain.model.CustomStatType
import com.luis.alhendinfc.domain.repository.CustomStatTypeRepositoryImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventTypesViewModel(
    private val repository: CustomStatTypeRepositoryImpl,
    private val teamId: Int
) : ViewModel() {

    val types: StateFlow<List<CustomStatType>> = repository.getByTeam(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    init {
        viewModelScope.launch {
            repository.ensureSampleCustomStats(teamId)
        }
    }

    companion object {
        fun factory(context: Context, teamId: Int) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AlhendinDatabase.getInstance(context.applicationContext)
                @Suppress("UNCHECKED_CAST")
                return EventTypesViewModel(
                    CustomStatTypeRepositoryImpl(db.customStatTypeDao()),
                    teamId
                ) as T
            }
        }
    }
}
