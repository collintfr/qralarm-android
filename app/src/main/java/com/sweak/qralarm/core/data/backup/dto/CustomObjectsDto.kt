package com.sweak.qralarm.core.data.backup.dto

import kotlinx.serialization.Serializable

/** custom_objects.json - the user's own items; their files are under custom_objects/[objectId]/. */
@Serializable
data class CustomObjectsDto(
    val customObjects: List<CustomObjectDto> = emptyList()
)

@Serializable
data class CustomObjectDto(
    val objectId: Long = 0,
    val name: String = "",
    val createdAtMillis: Long = 0,
    /** Label keywords; empty for items backed up before text recognition (they need new photos). */
    val keywords: List<String> = emptyList(),
    val referenceCount: Int = 0,
    val fileNames: List<String> = emptyList()
)
