package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

object ReportStatus {
    const val DRAFT = "DRAFT"
    const val SUBMITTED = "SUBMITTED"
}

@Entity(
    tableName = "daily_reports",
    indices = [Index(value = ["userId", "workDate"], unique = true)]
)
@JsonClass(generateAdapter = true)
data class DailyReport(
    @PrimaryKey
    @Json(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @Json(name = "user_id")
    val userId: String = "local-user",

    @Json(name = "work_date")
    val workDate: String,

    @Json(name = "status")
    val status: String = ReportStatus.DRAFT,

    @Json(name = "total_reels")
    val totalReels: Int = 0,

    @Json(name = "submitted_at")
    val submittedAt: String? = null,

    @Json(name = "created_at")
    val createdAt: String = "",

    @Json(name = "updated_at")
    val updatedAt: String = ""
) {
    val isSubmitted: Boolean
        get() = status == ReportStatus.SUBMITTED
}
