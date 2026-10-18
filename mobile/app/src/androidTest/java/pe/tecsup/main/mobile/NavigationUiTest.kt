package pe.tecsup.main.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class NavigationUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Before fun resetSyntheticPreferences() {
        pe.tecsup.main.mobile.data.MainApi("http://127.0.0.1:8088").apply {
            login("alumno","MainDemo2026!")
            savePreferences(pe.tecsup.main.mobile.data.UserPreferences())
            logout()
        }
    }

    private fun login(user: String = "alumno") {
        ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
        ui.onNodeWithText("Usuario").performTextReplacement(user)
        ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
        ui.onNodeWithText("Ingresar").performClick()
        ui.waitUntil(20_000) {
            ui.onAllNodesWithText("Tu espacio académico").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun homeModulesAndProfileAreReachableWithoutInventedData() {
        login()
        ui.onNodeWithText("Tus actividades pendientes").assertIsDisplayed()
        Evidence.capture("11_inicio.png")

        ui.onNodeWithContentDescription("Mensajes").performClick()
        ui.onNodeWithContentDescription("Nuevo chat").performClick()
        ui.onNodeWithText("Contactos de MAIN").assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        ui.onNodeWithContentDescription("Estados").performClick()
        ui.onNodeWithContentDescription("Actualizar estados").assertIsDisplayed()
        ui.onNodeWithContentDescription("Asistente").performClick()
        ui.onNodeWithText("Asistente académico").assertIsDisplayed()

        ui.onNodeWithContentDescription("Perfil").performClick()
        ui.onNodeWithText("Alex Demo").assertIsDisplayed()
        ui.onNodeWithContentDescription("Tema oscuro").assertIsOff()
        ui.onNodeWithContentDescription("Tema oscuro").performClick()
        ui.waitUntil(20_000) {
            ui.onAllNodesWithText("Preferencias guardadas.").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithContentDescription("Tema oscuro").assertIsOn()
        ui.activityRule.scenario.recreate()
        ui.waitUntil(20_000) {
            ui.onAllNodesWithContentDescription("Tema oscuro").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithContentDescription("Perfil").assertIsSelected()
        ui.onNodeWithContentDescription("Tema oscuro").assertIsOn()
        Evidence.capture("12_perfil_oscuro.png")

        ui.onNodeWithContentDescription("Inicio").performClick()
        ui.onNodeWithContentDescription("Actividades").performClick()
        ui.waitUntil(20_000) {
            ui.onAllNodesWithText("Revisión de criterios de aceptación").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithText("Revisión de criterios de aceptación").assertIsDisplayed()
        ui.onNodeWithContentDescription("Perfil").performClick()
        ui.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        ui.waitUntil(20_000) { ui.onAllNodesWithText("Ingresar").fetchSemanticsNodes().isNotEmpty() }
        login("vacio")
        ui.waitUntil(20_000) {
            ui.onAllNodesWithText("Cuando tengas actividades pendientes, aparecerán aquí.").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithContentDescription("Inicio").assertIsSelected()
        ui.onNodeWithText("Hola, Sol Demo").assertIsDisplayed()
        ui.onNodeWithText("Hola, Alex Demo").assertDoesNotExist()
    }
}
