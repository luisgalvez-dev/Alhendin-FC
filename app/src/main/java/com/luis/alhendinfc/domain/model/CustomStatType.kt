package com.luis.alhendinfc.domain.model

data class CustomStatType(
    val id: Int = 0,
    val teamId: Int,
    val code: String,
    val label: String,
    val shortLabel: String,
    val appliesTo: CustomStatAppliesTo = CustomStatAppliesTo.ALL,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
