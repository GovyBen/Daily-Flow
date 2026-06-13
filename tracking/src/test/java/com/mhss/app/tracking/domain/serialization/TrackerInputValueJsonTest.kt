package com.mhss.app.tracking.domain.serialization

import com.mhss.app.tracking.domain.validation.TrackerInputValue
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerInputValueJsonTest {

    @Test
    fun allSealedInputTypesRoundTrip() {
        val inputs = listOf(
            TrackerInputValue.MultiSelect(setOf("a", "b")),
            TrackerInputValue.SingleSelect("a"),
            TrackerInputValue.Counter(3.0),
            TrackerInputValue.Scale(7.5),
            TrackerInputValue.BooleanValue(false),
            TrackerInputValue.Duration(3_723),
            TrackerInputValue.NumberValue(12.5),
            TrackerInputValue.Text("private")
        )

        inputs.forEach { input ->
            assertEquals(input, TrackerInputValueJson.decode(TrackerInputValueJson.encode(input)))
        }
    }
}
