package pe.tecsup.main.mobile.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiResponseLimitTest {
    @Test fun readsAValidUtf8Response() {
        val value = "Respuesta académica válida."
        assertEquals(value, readApiResponse(ByteArrayInputStream(value.toByteArray(Charsets.UTF_8))))
    }

    @Test fun acceptsTheExactByteLimit() {
        assertEquals("12345678", readApiResponse(ByteArrayInputStream("12345678".toByteArray()), 8))
    }

    @Test fun rejectsAResponseBeyondTheLimit() {
        val error = assertThrows(ApiFailure::class.java) {
            readApiResponse(ByteArrayInputStream("123456789".toByteArray()), 8)
        }
        assertEquals("El servidor envió una respuesta demasiado grande. Intenta nuevamente.", error.message)
    }

    @Test fun countsUtf8BytesInsteadOfCharacters() {
        assertThrows(ApiFailure::class.java) {
            readApiResponse(ByteArrayInputStream("ááá".toByteArray(Charsets.UTF_8)), 5)
        }
    }
}
