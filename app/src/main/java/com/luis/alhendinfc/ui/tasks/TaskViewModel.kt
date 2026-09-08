package com.luis.alhendinfc.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.domain.model.Task
import com.luis.alhendinfc.domain.repository.TaskRepository
import com.luis.alhendinfc.domain.repository.TaskRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val teamId: Int
) : ViewModel() {

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query

    val tasks: StateFlow<List<Task>> = query
        .flatMapLatest { needle -> repository.searchByName(teamId, needle) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setQuery(value: String) {
        query.value = value
    }

    fun add(task: Task) {
        viewModelScope.launch { repository.add(task.copy(teamId = teamId)) }
    }

    fun update(task: Task) {
        viewModelScope.launch { repository.update(task.copy(teamId = teamId)) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { repository.delete(task) }
    }

    companion object {
        fun factory(context: Context, teamId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AlhendinDatabase.getInstance(context)
                    val repository = TaskRepositoryImpl(db.taskDao())
                    return TaskViewModel(repository, teamId) as T
                }
            }
    }
}
