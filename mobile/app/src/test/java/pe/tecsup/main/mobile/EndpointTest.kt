package pe.tecsup.main.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.tecsup.main.mobile.data.validateEndpoint

class EndpointTest {
    @Test fun debugAllowsOnlyLocalHttp() {
        assertEquals("http://127.0.0.1:8088", validateEndpoint(" http://127.0.0.1:8088/ ", true))
        assertEquals("http://10.0.2.2:8088", validateEndpoint("http://10.0.2.2:8088", true))
        assertThrows(IllegalArgumentException::class.java) { validateEndpoint("http://example.com", true) }
    }
    @Test fun releaseRequiresHttps() {
        assertThrows(IllegalArgumentException::class.java) { validateEndpoint("http://127.0.0.1:8088", false) }
        assertEquals("https://main.example.com", validateEndpoint("https://main.example.com", false))
    }
    @Test fun noEmbeddedCredentialsOrUnexpectedPaths() {
        listOf("https://user:pass@example.com", "https://example.com/api", "https://example.com?key=x",
            "https://example.com#x", "notaurl", "ftp://example.com", "http://127.0.0.1:99999").forEach {
            assertThrows(IllegalArgumentException::class.java) { validateEndpoint(it, true) }
        }
    }
}
