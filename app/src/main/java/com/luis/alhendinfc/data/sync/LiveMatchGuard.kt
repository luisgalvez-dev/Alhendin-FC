package com.luis.alhendinfc.data.sync

import com.luis.alhendinfc.domain.model.MatchStatus

object LiveMatchGuard {
    fun isLocalLive(status: String?): Boolean = status == MatchStatus.LIVE.name

    /** Un LIVE local no acepta un documento remoto de ese partido. */
    fun deferRemoteMatch(localStatus: String?): Boolean = isLocalLive(localStatus)

    fun shouldPushMatch(localStatus: String?): Boolean =
        localStatus != null && localStatus != MatchStatus.LIVE.name

    fun shouldPushMatchEvent(parentMatchStatus: String?): Boolean =
        parentMatchStatus == MatchStatus.FINISHED.name

    fun finishedCarriesFieldSeconds(status: String?, fieldSecondsJson: String): Boolean =
        status == MatchStatus.FINISHED.name && fieldSecondsJson.isNotBlank()
}
