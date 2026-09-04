package com.luis.alhendinfc.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.preferences.HomePreferencesRepository
import com.luis.alhendinfc.domain.model.HomeLayoutConfig
import com.luis.alhendinfc.domain.model.HomeModule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomizeHomeViewModel(
    private val preferences: HomePreferencesRepository
) : ViewModel() {

    val layoutConfig: StateFlow<HomeLayoutConfig> = preferences.layoutConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeLayoutConfig.defaults())

    fun setEnabled(module: HomeModule, enabled: Boolean) {
        viewModelScope.launch { preferences.setEnabled(module, enabled) }
    }

    fun moveUp(module: HomeModule) {
        viewModelScope.launch { preferences.moveUp(module) }
    }

    fun moveDown(module: HomeModule) {
        viewModelScope.launch { preferences.moveDown(module) }
    }

    fun resetDefaults() {
        viewModelScope.launch { preferences.resetDefaults() }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CustomizeHomeViewModel(
                        HomePreferencesRepository.getInstance(context.applicationContext)
                    ) as T
                }
            }
    }
}
