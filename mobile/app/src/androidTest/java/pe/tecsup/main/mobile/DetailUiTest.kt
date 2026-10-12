package pe.tecsup.main.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test

class DetailUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private fun login(user: String = "alumno", endpoint: String = "http://127.0.0.1:8088") {
        ui.onNodeWithText("Servidor").performTextReplacement(endpoint)
        ui.onNodeWithText("Usuario").performTextReplacement(user)
        ui.onNodeWithText("Contraseña").performTextInput("MainDemo2026!")
        ui.onNodeWithText("Ingresar").performClick()
    }
    private fun waitFor(text: String) {
        ui.waitUntil(20_000) { ui.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) { ui.waitForIdle(); Evidence.capture(name) }

    @Test fun openDetailRotateAndReturnPreservesSearchAndFilter() {
        login()
        waitFor("Revisión de criterios de aceptación")
        ui.onNodeWithText("Buscar por título o curso").performTextInput("Primer")
        ui.onNode(hasText("Vencidas") and hasClickAction()).performClick()
        ui.onNodeWithContentDescription("Ver detalle de Primer avance de MAIN").performScrollTo().performClick()
        waitFor("Docente de demostración")
        ui.onNodeWithText("Indicaciones").assertIsDisplayed()
        capture("07_detalle_autorizado.png")
        ui.activityRule.scenario.recreate()
        waitFor("Docente de demostración")
        ui.onNodeWithText("← Volver a mis actividades").performClick()
        ui.onNodeWithText("Primer").assertIsDisplayed()
        ui.onNode(hasText("Vencidas") and hasClickAction()).assertIsSelected()
        capture("08_regreso_con_contexto.png")
        ui.onNodeWithContentDescription("Ver detalle de Primer avance de MAIN").performScrollTo().performClick()
        waitFor("Docente de demostración")
        Espresso.pressBack()
        ui.onNodeWithText("Primer").assertIsDisplayed()
    }

    @Test fun unavailableActivityHasClearMessageAndReturn() {
        login()
        waitFor("Revisión de criterios de aceptación")
        // Exercise a manipulated ID against the real authorization, without exposing it in production UI.
        ui.runOnIdle { ViewModelProvider(ui.activity)[MainViewModel::class.java].openDetail(5) }
        waitFor("La actividad no existe o no está disponible para tu cuenta.")
        ui.onNodeWithText("Actividad de otro alumno").assertDoesNotExist()
        capture("09_detalle_no_disponible.png")
        ui.onNodeWithText("← Volver a mis actividades").performClick()
        waitFor("Revisión de criterios de aceptación")
    }

    @Test fun expiredDetailClearsPreviousStudentDataAndAllowsNewLogin() {
        ApiBridge().use { bridge ->
            login(endpoint = bridge.endpoint)
            waitFor("Revisión de criterios de aceptación")
            // Omit cookie for this one request: the REAL backend returns 401.
            bridge.expireNextApi.set(true)
            ui.onNodeWithContentDescription("Ver detalle de Revisión de criterios de aceptación").performScrollTo().performClick()
            waitFor("Tu sesión finalizó. Ingresa nuevamente.")
            ui.onNodeWithText("Alex Demo").assertDoesNotExist()
            capture("10_sesion_finalizada.png")
            login(user = "vacio")
            waitFor("No tienes actividades pendientes.")
            ui.onNodeWithText("Primer avance de MAIN").assertDoesNotExist()
        }
    }
}
