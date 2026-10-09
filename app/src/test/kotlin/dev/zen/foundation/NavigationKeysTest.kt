package dev.zen.foundation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationKeysTest {
    @Test
    fun destinationKeysHaveRoundTripSerialization() {
        assertEquals(Tasks, Json.decodeFromString<Tasks>(Json.encodeToString(Tasks)))
        assertEquals(About, Json.decodeFromString<About>(Json.encodeToString(About)))
    }
}
