package com.tobiso.tobisoappnative

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tobiso.tobisoappnative.db.AppDatabase
import com.tobiso.tobisoappnative.db.entity.FeedbackEntity
import com.tobiso.tobisoappnative.model.ApiClient
import com.tobiso.tobisoappnative.model.FeedbackDto
import timber.log.Timber

class FeedbackSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val dao = db.feedbackDao()
        val pendingList = dao.getAllPending()
        if (pendingList.isEmpty()) return Result.success()

        var allSucceeded = true
        for (feedback in pendingList) {
            try {
                val dto = FeedbackDto(
                    name = feedback.name,
                    email = feedback.email,
                    message = feedback.message,
                    platform = feedback.platform
                )
                ApiClient.apiService.sendFeedback(dto)
                dao.deleteById(feedback.id)
                Timber.i("Sent queued feedback #${feedback.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to send queued feedback #${feedback.id}")
                dao.incrementRetryCount(feedback.id)
                allSucceeded = false
            }
        }
        return if (allSucceeded) Result.success() else Result.retry()
    }
}
