package com.sweak.qralarm.core.domain.custom_object

import kotlinx.coroutines.flow.Flow

data class CustomObject(
    val objectId: Long,
    val name: String,
    val createdAtMillis: Long,
    val referenceCount: Int,
    /** Label words the item is recognised by; empty for items added before text recognition. */
    val keywords: List<String>
) {
    val needsNewPhotos: Boolean get() = keywords.isEmpty()
}

interface CustomObjectsRepository {
    fun getAllObjects(): Flow<List<CustomObject>>
    suspend fun getObject(objectId: Long): CustomObject?
    /** [photos]: JPEGs of the label, kept for thumbnails and backups. */
    suspend fun addObject(name: String, photos: List<ByteArray>, keywords: List<String>): Long
    suspend fun replaceReferences(objectId: Long, photos: List<ByteArray>, keywords: List<String>)
    suspend fun renameObject(objectId: Long, name: String)
    suspend fun deleteObject(objectId: Long)
    suspend fun countAlarmsUsingObject(objectId: Long): Int
    suspend fun loadReferenceJpegs(objectId: Long): List<ByteArray>
}
