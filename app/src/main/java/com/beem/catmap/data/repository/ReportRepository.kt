package com.beem.catmap.data.repository

import com.beem.catmap.data.local.UserSession
import com.beem.catmap.ui.report.ReportType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ReportRepository() {
    companion object {
        @Volatile
        private var INSTANCE: ReportRepository? = null

        fun getInstance(): ReportRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReportRepository().also {
                    INSTANCE = it
                }
            }
        }
    }

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun sendReport(
        targetId: String,
        reportType: ReportType,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val reportData = hashMapOf(
                "reporterUserId" to UserSession.userId,
                "reason" to reason,
                "timestamp" to Timestamp.now()
            )

            val subCollectionName = "${reportType.name.lowercase()}s"

            firestore.collection("reports")
                .document(targetId)
                .collection(subCollectionName)
                .add(reportData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}