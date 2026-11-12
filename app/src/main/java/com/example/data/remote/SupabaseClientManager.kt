package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SupabaseAuthConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SupabaseClientManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadConfig())
    val configState: StateFlow<SupabaseAuthConfig> = _configState.asStateFlow()

    private var currentApi: SupabaseApi? = null

    init {
        rebuildApi(_configState.value)
    }

    private fun loadConfig(): SupabaseAuthConfig {
        val url = prefs.getString("supabase_url", "") ?: ""
        val anonKey = prefs.getString("supabase_anon_key", "") ?: ""
        val token = prefs.getString("access_token", null)
        val email = prefs.getString("user_email", null)
        val userId = prefs.getString("user_id", null)
        val isConnected = url.isNotBlank() && anonKey.isNotBlank()

        return SupabaseAuthConfig(
            url = url,
            anonKey = anonKey,
            accessToken = token,
            userEmail = email,
            userId = userId,
            isConnected = isConnected
        )
    }

    fun saveCredentials(url: String, anonKey: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = anonKey.trim()

        prefs.edit()
            .putString("supabase_url", cleanUrl)
            .putString("supabase_anon_key", cleanKey)
            .apply()

        val updated = _configState.value.copy(
            url = cleanUrl,
            anonKey = cleanKey,
            isConnected = cleanUrl.isNotBlank() && cleanKey.isNotBlank()
        )
        _configState.value = updated
        rebuildApi(updated)
    }

    fun saveAuthSession(token: String?, email: String?, userId: String?) {
        prefs.edit()
            .putString("access_token", token)
            .putString("user_email", email)
            .putString("user_id", userId)
            .apply()

        val updated = _configState.value.copy(
            accessToken = token,
            userEmail = email,
            userId = userId
        )
        _configState.value = updated
        rebuildApi(updated)
    }

    fun clearAuthSession() {
        prefs.edit()
            .remove("access_token")
            .remove("user_email")
            .remove("user_id")
            .apply()

        val updated = _configState.value.copy(
            accessToken = null,
            userEmail = null,
            userId = null
        )
        _configState.value = updated
        rebuildApi(updated)
    }

    private fun rebuildApi(config: SupabaseAuthConfig) {
        if (config.url.isBlank()) {
            currentApi = null
            return
        }

        try {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val baseUrl = if (config.url.endsWith("/")) config.url else "${config.url}/"

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            currentApi = retrofit.create(SupabaseApi::class.java)
        } catch (_: Exception) {
            currentApi = null
        }
    }

    fun getApi(): SupabaseApi? = currentApi

    fun getEffectiveUserId(): String {
        return _configState.value.userId ?: "local-user"
    }

    fun isConfigured(): Boolean {
        return _configState.value.isConnected && currentApi != null
    }

    fun getAuthHeader(): String {
        val token = _configState.value.accessToken
        return if (!token.isNullOrBlank()) "Bearer $token" else "Bearer ${_configState.value.anonKey}"
    }

    fun getApiKey(): String {
        return _configState.value.anonKey
    }
}
