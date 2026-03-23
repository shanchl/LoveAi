package com.loveai.model

data class FavoriteSequence(
    val id: String,
    val name: String,
    val title: String,
    val subtitle: String,
    val effectVariantIds: List<Int>,
    val songKey: String? = null,
    val createdAt: Long
)
