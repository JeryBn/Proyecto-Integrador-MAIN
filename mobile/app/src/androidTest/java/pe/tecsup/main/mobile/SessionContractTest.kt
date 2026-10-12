package pe.tecsup.main.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.tecsup.main.mobile.data.*

/** Requires the real local MAIN backend and adb reverse tcp:8088 tcp:8088. */
@RunWith(AndroidJUnit4::class)
class SessionContractTest {
    private fun client() = MainApi("http://127.0.0.1:8088")
    @Test fun realLoginPreservesSessionAndLogoutInvalidatesIt() {
        val api = client()
        assertEquals("alumno", api.login("alumno", "MainDemo2026!").usuario)
        assertEquals("Alex Demo", api.profile().nombre)
        api.logout()
        assertThrows(SessionExpired::class.java) { api.profile() }
    }
    @Test fun rejectedLoginDoesNotReuseAnotherStudentsSession() {
        val api = client()
        api.login("alumno", "MainDemo2026!")
        assertThrows(InvalidCredentials::class.java) { api.login("otro", "wrong") }
        assertThrows(SessionExpired::class.java) { api.profile() }
    }
    @Test fun missingSessionHasExplicitExpiredState() {
        assertThrows(SessionExpired::class.java) { client().pending() }
    }
}
