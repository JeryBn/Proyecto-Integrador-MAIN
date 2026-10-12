package pe.tecsup.main.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.tecsup.main.mobile.data.MainApi

@RunWith(AndroidJUnit4::class)
class PendingContractTest {
    @Test fun alumnoSeesOnlyUnsubmittedAuthorizedActivitiesInServerOrder() {
        val api = MainApi("http://127.0.0.1:8088")
        try {
            api.login("alumno", "MainDemo2026!")
            val items = api.pending()
            assertEquals(listOf(3L, 1L, 2L), items.map { it.id })
            assertTrue(items.all { !it.completada })
            assertTrue(items.all { it.titulo.isNotBlank() && it.curso.isNotBlank() && it.fechaLimite.isNotBlank() })
        } finally { api.logout() }
    }
    @Test fun vacioGetsEmptyListAndOtherStudentHasOwnCourse() {
        val api = MainApi("http://127.0.0.1:8088")
        try {
            api.login("vacio", "MainDemo2026!")
            assertTrue(api.pending().isEmpty())
            api.login("otro", "MainDemo2026!")
            assertEquals(listOf(5L), api.pending().map { it.id })
        } finally { api.logout() }
    }
}
