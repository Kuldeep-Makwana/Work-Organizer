package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(
    tableName = "daily_report_entries",
    indices = [
        Index(value = ["reportId", "clientId"], unique = true),
        Index(value = ["reportId"]),
        Index(value = ["clientId"])
    ]
)
@JsonClass(generateAdapter = true)
data class DailyReportEntry(
    @PrimaryKey
    @Json(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @Json(name = "report_id")
    val reportId: String,

    @Json(name = "client_id")
    val clientId: String,

    @Json(name = "reel_codes")
    val reelCodes: List<String> = emptyList(),

    @Json(name = "created_at")
    val createdAt: String = "",

    @Json(name = "updated_at")
    val updatedAt: String = ""
)
