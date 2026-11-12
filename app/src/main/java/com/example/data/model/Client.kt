package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(tableName = "clients")
@JsonClass(generateAdapter = true)
data class Client(
    @PrimaryKey
    @Json(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @Json(name = "user_id")
    val userId: String = "local-user",

    @Json(name = "client_name")
    val clientName: String,

    @Json(name = "is_active")
    val isActive: Boolean = true,

    @Json(name = "created_at")
    val createdAt: String = "",

    @Json(name = "updated_at")
    val updatedAt: String = ""
)
