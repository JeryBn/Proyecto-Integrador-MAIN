package pe.tecsup.main.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.tecsup.main.mobile.data.*

/** Ejecutar tras scripts/verificar_arquitectura_compartida.py, pasando sharedActivityId. */
class SharedArchitectureUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun djangoActivitySurvivesRestartAndIsVisibleOnlyToEnrolledStudent() {
        val id = requireNotNull(InstrumentationRegistry.getArguments().getString("sharedActivityId")) {
            "Pasar el ID sintético creado por la prueba Django/MySQL; no usar datos de producción."
        }.toLong()
        val student = MainApi("http://127.0.0.1:8088")
        try {
            student.login("alumno", "MainDemo2026!")
            assertTrue(student.pending().any { it.id == id })
            assertTrue(student.detail(id).titulo.startsWith("Sprint 12: Django y Spring"))
            assertEquals("2026-11-20T15:30:00", student.detail(id).fechaLimite)
        } finally { student.logout() }
        val other = MainApi("http://127.0.0.1:8088")
        try {
            other.login("otro", "MainDemo2026!")
            assertFalse(other.pending().any { it.id == id })
            assertThrows(ActivityUnavailable::class.java) { other.detail(id) }
        } finally { other.logout() }

        ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
        ui.onNodeWithText("Usuario").performTextReplacement("alumno")
        ui.onNodeWithText("Contraseña").performTextInput("MainDemo2026!")
        ui.onNodeWithText("Ingresar").performClick()
        ui.waitUntil(20_000) { ui.onAllNodesWithText("Tu espacio académico").fetchSemanticsNodes().isNotEmpty() }
        // Abrir el ID creado por Django; el detalle consulta Spring real y su autorización.
        ui.runOnIdle { ViewModelProvider(ui.activity)[MainViewModel::class.java].openDetail(id) }
        ui.waitUntil(20_000) { ui.onAllNodesWithText("Docente de demostración").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Indicaciones").assertIsDisplayed()
        Evidence.capture("31_django_mysql_android.png")
    }
}
