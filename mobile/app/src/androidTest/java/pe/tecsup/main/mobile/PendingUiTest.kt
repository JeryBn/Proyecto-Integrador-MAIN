package pe.tecsup.main.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class PendingUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun login(user: String, endpoint: String = "http://127.0.0.1:8088", expectSuccess: Boolean = true) {
        ui.onNodeWithText("Servidor").performTextReplacement(endpoint)
        ui.onNodeWithText("Usuario").performTextInput(user)
        ui.onNodeWithText("Contraseña").performTextInput("MainDemo2026!")
        ui.onNodeWithText("Ingresar").performClick()
        if (expectSuccess) {
            waitFor("Tu espacio académico")
            ui.onNodeWithContentDescription("Actividades").performClick()
        }
    }
    private fun waitFor(text: String) {
        ui.waitUntil(20_000) { ui.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        Evidence.capture(name)
    }
    private fun showListMessage(text: String) {
        ui.onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = true))
        ui.onNodeWithText(text, substring = true).assertIsDisplayed()
    }
    @Test fun alumnoListSearchAndEmptyFilterThenLogout() {
        login("alumno")
        waitFor("Revisión de criterios de aceptación")
        capture("02_listado_alumno.png")
        ui.onNodeWithText("Buscar por título o curso").performTextInput("no-existe")
        showListMessage("No hay actividades que coincidan con tu búsqueda o filtro.")
        capture("03_sin_coincidencias.png")
        ui.onNodeWithText("Cerrar sesión").performClick()
        waitFor("Ingresar")
        ui.onNodeWithText("Usuario").assertIsDisplayed()
    }
    @Test fun emptyAccountHasExplicitEmptyState() {
        login("vacio")
        waitFor("No tienes actividades pendientes.")
        showListMessage("No tienes actividades pendientes.")
        capture("04_cuenta_vacia.png")
    }
    @Test fun unreachableServerShowsRecoverableLoginError() {
        login("alumno", "http://127.0.0.1:65530", expectSuccess = false)
        waitFor("No pudimos conectar con el servidor.")
        capture("05_error_conexion.png")
        ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
        ui.onNodeWithText("Contraseña").performTextInput("MainDemo2026!")
        ui.onNodeWithText("Ingresar").performClick()
        waitFor("Tu espacio académico")
        ui.onNodeWithContentDescription("Actividades").performClick()
        waitFor("Revisión de criterios de aceptación")
    }
    @Test fun failedPendingRequestShowsRetryThenRecoversFromRealBackend() {
        ApiBridge().use { bridge ->
            bridge.failNextPending.set(true)
            login("alumno", bridge.endpoint)
            waitFor("El servidor no respondió correctamente.")
            showListMessage("El servidor no respondió correctamente.")
            capture("06_error_listado.png")
            ui.onNodeWithText("Reintentar").performClick()
            waitFor("Revisión de criterios de aceptación")
            ui.onNodeWithText("Alex Demo").assertIsDisplayed()
        }
    }
}
