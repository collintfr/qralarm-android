package com.sweak.qralarm.core.data.custom_object

import com.sweak.qralarm.core.domain.custom_object.CustomObject
import com.sweak.qralarm.core.domain.custom_object.CustomObjectsRepository
import com.sweak.qralarm.core.storage.database.dao.CustomObjectsDao
import com.sweak.qralarm.core.storage.database.model.CustomObjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CustomObjectsRepositoryImpl @Inject constructor(
    private val dao: CustomObjectsDao,
    private val files: CustomObjectFileStore
) : CustomObjectsRepository {

    override fun getAllObjects(): Flow<List<CustomObject>> =
        dao.getAllObjects().map { list -> list.map { it.toDomain() } }

    override suspend fun getObject(objectId: Long): CustomObject? = dao.getObject(objectId)?.toDomain()

    override suspend fun addObject(name: String, photos: List<ByteArray>, keywords: List<String>): Long =
        withContext(Dispatchers.IO) {
            val id = dao.insertObject(
                CustomObjectEntity(
                    name = name.trim(),
                    createdAtMillis = System.currentTimeMillis(),
                    referenceCount = photos.size,
                    embeddingThreshold = 0f,
                    keywords = keywords.joinToString(KEYWORD_SEPARATOR)
                )
            )
            try {
                files.write(id, photos)
            } catch (exception: Exception) {
                dao.deleteObjectRow(id)
                throw exception
            }
            id
        }

    override suspend fun replaceReferences(objectId: Long, photos: List<ByteArray>, keywords: List<String>) =
        withContext(Dispatchers.IO) {
            val existing = dao.getObject(objectId) ?: return@withContext
            files.write(objectId, photos)
            dao.updateObject(
                existing.copy(referenceCount = photos.size, keywords = keywords.joinToString(KEYWORD_SEPARATOR))
            )
        }

    override suspend fun renameObject(objectId: Long, name: String) {
        dao.getObject(objectId)?.let { dao.updateObject(it.copy(name = name.trim())) }
    }

    override suspend fun deleteObject(objectId: Long) = withContext(Dispatchers.IO) {
        dao.deleteObject(objectId)
        files.delete(objectId)
    }

    override suspend fun countAlarmsUsingObject(objectId: Long): Int = dao.countAlarmsUsingObject(objectId)

    override suspend fun loadReferenceJpegs(objectId: Long): List<ByteArray> =
        withContext(Dispatchers.IO) { files.readJpegs(objectId) }

    private fun CustomObjectEntity.toDomain() = CustomObject(
        objectId = objectId,
        name = name,
        createdAtMillis = createdAtMillis,
        referenceCount = referenceCount,
        keywords = keywords.split(KEYWORD_SEPARATOR).filter { it.isNotBlank() }
    )

    companion object {
        const val KEYWORD_SEPARATOR = "\n"
    }
}
