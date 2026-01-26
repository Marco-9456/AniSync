package com.anisync.android.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.anisync.android.worker.EpisodeUpdateWorker

class DecrementEpisodeCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val mediaId = parameters[MediaIdKey] ?: return

        val workRequest = OneTimeWorkRequestBuilder<EpisodeUpdateWorker>()
            .setInputData(workDataOf(
                "mediaId" to mediaId,
                "amount" to -1
            ))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    companion object {
        val MediaIdKey = ActionParameters.Key<Int>("mediaId")
    }
}
