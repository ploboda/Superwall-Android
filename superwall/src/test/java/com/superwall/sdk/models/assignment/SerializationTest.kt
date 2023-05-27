package com.superwall.sdk.models.assignment


import com.superwall.sdk.assertTrue
import com.superwall.sdk.models.getSWJson
import org.json.JSONObject
import org.junit.Test
import java.util.*

class SerializationTest {

    @Test
    fun `make sure snake case works `() {
        val swJson = getSWJson()
        val assignment = Assignment("123", "456")
        val serialized = swJson.encodeToString(Assignment.serializer(), assignment)
        assertTrue(serialized.contains("experiment_id"))

        // Parse using JSONObject,  not kotlinx.serialization
        val parsed = JSONObject(serialized)
        assert(parsed.getString("experiment_id") == "123")
        assert(parsed.getString("variant_id") == "456")
    }
}