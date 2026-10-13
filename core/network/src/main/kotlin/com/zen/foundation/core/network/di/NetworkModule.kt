package com.zen.foundation.core.network.di

import com.zen.foundation.core.network.BuildConfig
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Network stack shared by every feature. Features declare Retrofit interfaces in their data layer
 * and create them from the injected [Retrofit]; they never build their own clients.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun providesJson(): Json = foundationJson()

    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                        redactHeader("Cookie")
                    }
                )
            }
        }.build()

    /** `Lazy` keeps OkHttp's initialization off the startup path until the first request. */
    @Provides
    @Singleton
    fun providesRetrofit(json: Json, okHttpClient: Lazy<OkHttpClient>): Retrofit =
        foundationRetrofit(
            baseUrl = BuildConfig.API_BASE_URL,
            json = json,
            callFactory = { request -> okHttpClient.get().newCall(request) }
        )
}

internal fun foundationJson(): Json = Json {
    // Servers add fields over time; unknown keys must not break older app versions.
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal fun foundationRetrofit(baseUrl: String, json: Json, callFactory: Call.Factory): Retrofit =
    Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .callFactory(callFactory)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
