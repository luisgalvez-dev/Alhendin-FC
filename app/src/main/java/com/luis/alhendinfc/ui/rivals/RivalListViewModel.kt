package com.luis.alhendinfc.ui.rivals

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luis.alhendinfc.data.files.AndroidAttachmentStore
import com.luis.alhendinfc.data.files.SharedMediaWriter
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.Attachment
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import com.luis.alhendinfc.domain.repository.AttachmentRepositoryImpl
import com.luis.alhendinfc.domain.repository.SeasonCalendarRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RivalListViewModel(
    private val calendarRepository: SeasonCalendarRepository,
    private val attachments: AttachmentRepository,
    private val media: SharedMediaWriter,
    private val teamId: Int
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val clubs: StateFlow<List<OpponentClub>> =
        _query.flatMapLatest { calendarRepository.searchClubs(teamId, it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val opponentShields: StateFlow<Map<String, Attachment>> =
        attachments.getActiveByType(AttachmentParentType.OPPONENT_SHIELD)
            .map { SharedMedia.byParentSyncId(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun addClub(
        name: String,
        shortName: String,
        stadium: String,
        shieldUri: String?,
        kitColors: String,
        pickedShield: Uri? = shieldUri?.takeIf { SharedMedia.isPickerUri(it) }?.let(Uri::parse),
        onCreated: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val id = calendarRepository.addClub(
                OpponentClub(
                    teamId = teamId,
                    name = name.trim(),
                    shortName = shortName.trim(),
                    stadium = stadium.trim(),
                    shieldUri = SharedMedia.persistableLegacyUri(shieldUri),
                    kitColors = kitColors.trim()
                )
            )
            val created = calendarRepository.getClubOnce(id)
            if (pickedShield != null && created != null && created.syncId.isNotBlank()) {
                runCatching { media.setOpponentShield(created.syncId, pickedShield) }
            }
            onCreated(id)
        }
    }

    fun deleteClub(club: OpponentClub) {
        viewModelScope.launch {
            if (club.syncId.isNotBlank()) {
                attachments.deleteByParent(AttachmentParentType.OPPONENT_SHIELD, club.syncId)
                attachments.deleteByParent(AttachmentParentType.OPPONENT, club.syncId)
            }
            calendarRepository.deleteClub(club)
        }
    }

    companion object {
        fun factory(context: Context, teamId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    val db = AlhendinDatabase.getInstance(app)
                    val attachments = AttachmentRepositoryImpl(db.attachmentDao())
                    return RivalListViewModel(
                        SeasonCalendarRepository(db.opponentClubDao(), db.seasonFixtureDao()),
                        attachments,
                        SharedMediaWriter(attachments, AndroidAttachmentStore(app)),
                        teamId
                    ) as T
                }
            }
    }
}
