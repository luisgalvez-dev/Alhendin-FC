package com.luis.alhendinfc.domain.model

data class Team(
    val id: Int = 0,
    val name: String,
    val category: String,
    val season: String,
    val shieldUri: String? = null,
    val isSelected: Boolean = false,
    val syncId: String = ""
)
