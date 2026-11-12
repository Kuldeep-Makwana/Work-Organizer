package com.example.data.remote

import com.example.data.model.Client
import com.example.data.model.DailyReport
import com.example.data.model.DailyReportEntry
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class AuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "user") val user: AuthUser? = null
)

@JsonClass(generateAdapter = true)
data class AuthUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class SubmitReportRpcRequest(
    @Json(name = "p_report_id") val reportId: String
)

interface SupabaseApi {

    // --- AUTH ---
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: AuthRequest,
        @Header("apikey") apiKey: String
    ): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Body request: AuthRequest,
        @Header("apikey") apiKey: String
    ): Response<AuthResponse>

    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<ResponseBody>

    @GET("auth/v1/user")
    suspend fun getCurrentUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<AuthUser>

    // --- CLIENTS ---
    @GET("rest/v1/clients")
    suspend fun getClients(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "client_name.asc"
    ): Response<List<Client>>

    @POST("rest/v1/clients")
    suspend fun insertClient(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body client: Client
    ): Response<List<Client>>

    @PATCH("rest/v1/clients")
    suspend fun updateClient(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idFilter: String,
        @Body client: Map<String, Any?>
    ): Response<List<Client>>

    // --- DAILY REPORTS ---
    @GET("rest/v1/daily_reports")
    suspend fun getDailyReportByDate(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("work_date") workDateFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<DailyReport>>

    @GET("rest/v1/daily_reports")
    suspend fun getAllReports(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "work_date.desc"
    ): Response<List<DailyReport>>

    @POST("rest/v1/daily_reports")
    suspend fun createDailyReport(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body report: DailyReport
    ): Response<List<DailyReport>>

    // --- DAILY REPORT ENTRIES ---
    @GET("rest/v1/daily_report_entries")
    suspend fun getReportEntries(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("report_id") reportIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<DailyReportEntry>>

    @GET("rest/v1/daily_report_entries")
    suspend fun getAllEntries(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*"
    ): Response<List<DailyReportEntry>>

    @POST("rest/v1/daily_report_entries")
    suspend fun upsertReportEntry(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body entry: DailyReportEntry
    ): Response<List<DailyReportEntry>>

    // --- RPC ATOMIC FINAL SUBMISSION ---
    @POST("rest/v1/rpc/submit_daily_report")
    suspend fun submitDailyReportRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: SubmitReportRpcRequest
    ): Response<ResponseBody>
}
