package com.anisync.android.data

import android.content.Context
import android.util.AtomicFile
import androidx.core.util.readText
import androidx.core.util.writeText
import com.anisync.android.domain.recap.FetchedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

/**
 * The list history a first build has read so far, kept so that a build the system stops carries on
 * from it instead of reading every page again. [from] is the start of the window it belongs to.
 */
data class RecapCheckpoint(
    val from: Long,
    /** Epoch milliseconds. */
    val savedAt: Long,
    /** True once the whole window was read and only the lookups were left. */
    val complete: Boolean,
    val activities: List<FetchedActivity>,
)

/** One [RecapCheckpoint] per account. */
interface RecapCheckpoints {
    suspend fun load(owner: Int): RecapCheckpoint?

    suspend fun save(owner: Int, checkpoint: RecapCheckpoint)

    suspend fun clear(owner: Int)
}

/** A file per account in no-backup storage, so a restore never brings a half read history back. */
class FileRecapCheckpoints @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecapCheckpoints {

    private fun file(owner: Int) = AtomicFile(File(context.noBackupFilesDir, "recap_checkpoint_$owner.json"))

    override suspend fun load(owner: Int): RecapCheckpoint? = withContext(Dispatchers.IO) {
        val file = file(owner)
        if (!file.baseFile.exists()) return@withContext null
        runCatching { decodeRecapCheckpoint(file.readText()) }.getOrNull()
    }

    override suspend fun save(owner: Int, checkpoint: RecapCheckpoint) = withContext(Dispatchers.IO) {
        file(owner).writeText(checkpoint.encode())
    }

    override suspend fun clear(owner: Int) = withContext(Dispatchers.IO) {
        file(owner).delete()
    }
}

/** Bumped whenever the stored shape changes, so an older file is read as no checkpoint at all. */
private const val CHECKPOINT_VERSION = 1

@Serializable
private data class StoredCheckpoint(
    @SerialName("v") val version: Int,
    @SerialName("f") val from: Long,
    @SerialName("s") val savedAt: Long,
    @SerialName("c") val complete: Boolean,
    @SerialName("a") val activities: List<StoredActivity>,
)

private val checkpointJson = Json { ignoreUnknownKeys = true }

internal fun RecapCheckpoint.encode(): String = checkpointJson.encodeToString(
    StoredCheckpoint.serializer(),
    StoredCheckpoint(
        version = CHECKPOINT_VERSION,
        from = from,
        savedAt = savedAt,
        complete = complete,
        activities = activities.map { StoredActivity(it.id, it.mediaId, it.status, it.progress, it.createdAt) },
    ),
)

/** The checkpoint in [json], or null when it is unreadable or from another version. */
internal fun decodeRecapCheckpoint(json: String): RecapCheckpoint? =
    runCatching { checkpointJson.decodeFromString(StoredCheckpoint.serializer(), json) }.getOrNull()
        ?.takeIf { it.version == CHECKPOINT_VERSION }
        ?.let { stored ->
            RecapCheckpoint(
                from = stored.from,
                savedAt = stored.savedAt,
                complete = stored.complete,
                activities = stored.activities.map { FetchedActivity(it.id, it.mediaId, it.status, it.progress, it.createdAt) },
            )
        }
