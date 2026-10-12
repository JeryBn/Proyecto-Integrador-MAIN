package pe.tecsup.main.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.tecsup.main.mobile.data.*

@RunWith(AndroidJUnit4::class)
class DetailContractTest {
    @Test fun authorizedDetailIncludesAllRequiredFields() {
        val api = MainApi("http://127.0.0.1:8088")
        try {
            api.login("alumno", "MainDemo2026!")
            val detail = api.detail(1)
            assertEquals("Primer avance de MAIN", detail.titulo)
            assertTrue(detail.descripcion.startsWith("Demuestra el listado"))
            assertEquals("Construcción y Pruebas de Software", detail.curso)
            assertEquals("Docente de demostración", detail.docente)
            assertEquals("2026-10-06T15:30:00", detail.fechaLimite)
        } finally { api.logout() }
    }
    @Test fun foreignAndMissingDetailsAreHiddenAndAnonymousIsRejected() {
        val api = MainApi("http://127.0.0.1:8088")
        assertThrows(SessionExpired::class.java) { api.detail(1) }
        try {
            api.login("alumno", "MainDemo2026!")
            assertThrows(ActivityUnavailable::class.java) { api.detail(5) }
            assertThrows(ActivityUnavailable::class.java) { api.detail(999) }
        } finally { api.logout() }
    }
}
