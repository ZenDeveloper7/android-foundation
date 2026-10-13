package com.zen.foundation.core.network

import com.google.common.truth.Truth.assertThat
import com.zen.foundation.core.network.di.foundationJson
import com.zen.foundation.core.network.di.foundationRetrofit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.create
import retrofit2.http.GET

class NetworkModuleTest {
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun response_withUnknownFields_isParsed() = runTest {
        server.enqueue(
            MockResponse
                .Builder()
                .body("""{"name":"foundation","addedLater":true}""")
                .build()
        )
        val api =
            foundationRetrofit(
                baseUrl = server.url("/").toString(),
                json = foundationJson(),
                callFactory = OkHttpClient()
            ).create<TestApi>()

        assertThat(api.item()).isEqualTo(TestItem(name = "foundation"))
    }

    interface TestApi {
        @GET("item")
        suspend fun item(): TestItem
    }

    @Serializable
    data class TestItem(val name: String)
}
