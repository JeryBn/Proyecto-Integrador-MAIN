package pe.tecsup.main.mobile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import pe.tecsup.main.mobile.data.*

class ReminderUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun studentCanEnableAndChooseLeadTimeFromProfile(){
  val api=MainApi("http://127.0.0.1:8088")
  api.login("alumno","MainDemo2026!")
  val previous=api.reminderPreferences()
  try {
  api.reminderPreferences(ReminderPreferences(false,60))
  ui.onNodeWithText("Usuario").performTextReplacement("alumno")
  ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
  ui.onNodeWithText("Ingresar").performClick()
  ui.waitUntil(20000){ui.onAllNodesWithContentDescription("Perfil").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithContentDescription("Perfil").performClick()
  ui.waitUntil(20000){ui.onAllNodesWithContentDescription("Recordar tareas pendientes").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithContentDescription("Recordar tareas pendientes").performScrollTo().assertIsOff().performClick()
  ui.waitUntil(20000){ui.onAllNodesWithText("Recordatorios guardados.").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithContentDescription("Recordar tareas pendientes").assertIsOn()
  ui.onNodeWithText("Un día antes").performScrollTo().performClick()
  ui.waitUntil(20000){ui.onAllNodes(hasText("Un día antes") and isSelected()).fetchSemanticsNodes().isNotEmpty()}
  assertEquals(ReminderPreferences(true,1440),api.reminderPreferences())
  Evidence.capture("recordatorios_perfil.png")
  } finally {
   try { api.reminderPreferences(previous) } finally { api.logout() }
  }
 }
}
