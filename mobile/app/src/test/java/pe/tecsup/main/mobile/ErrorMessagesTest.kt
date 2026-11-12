package pe.tecsup.main.mobile

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.tecsup.main.mobile.data.ApiFailure
import pe.tecsup.main.mobile.data.InvalidCredentials
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ErrorMessagesTest {
    @Test fun preservesSafeDomainMessages() {
        assertEquals("Usuario o contraseña incorrectos.", userMessage(InvalidCredentials()))
        assertEquals("No tienes permiso para realizar esta operación.",
            userMessage(ApiFailure("No tienes permiso para realizar esta operación.")))
    }

    @Test fun distinguishesTimeoutAndUnavailableServer() {
        assertEquals(
            "El servidor está tardando más de lo esperado. Intenta nuevamente en unos segundos.",
            userMessage(SocketTimeoutException())
        )
        listOf(UnknownHostException(), ConnectException()).forEach {
            assertEquals(
                "No encontramos el servidor. Revisa tu conexión y la dirección configurada.",
                userMessage(it)
            )
        }
    }

    @Test fun distinguishesSecureAndInterruptedConnections() {
        assertEquals(
            "No pudimos establecer una conexión segura con el servidor.",
            userMessage(SSLHandshakeException("certificate rejected"))
        )
        assertEquals(
            "La conexión se interrumpió. Revisa tu red e intenta nuevamente.",
            userMessage(EOFException("internal detail"))
        )
    }

    @Test fun hidesUnexpectedTechnicalDetails() {
        assertEquals(
            "Ocurrió un problema inesperado. Intenta nuevamente.",
            userMessage(IllegalStateException("database password leaked"))
        )
    }
}
