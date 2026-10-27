package pe.tecsup.main.mobile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import pe.tecsup.main.mobile.data.*

class ReminderUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun studentCanEnableAndChooseLeadTimeFromProfile(){
  val stamp=System.currentTimeMillis();val name="reminder_ui_$stamp"
  AdminTestApi().post("/api/admin/v1/users",JSONObject().put("username",name).put("name","Recordatorios UI sintéticos").put("role","ALUMNO").put("password","SyntheticReminder2026!").put("alumnoId",stamp))
  ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
  ui.onNodeWithText("Usuario").performTextReplacement(name)
  ui.onNodeWithText("Contraseña").performTextReplacement("SyntheticReminder2026!")
  ui.onNodeWithText("Ingresar").performClick()
  ui.waitUntil(20000){ui.onAllNodesWithContentDescription("Perfil").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithContentDescription("Perfil").performClick()
  ui.waitUntil(20000){ui.onAllNodesWithContentDescription("Recordar tareas pendientes").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithContentDescription("Recordar tareas pendientes").performScrollTo().assertIsOff().performClick()
  ui.waitUntil(20000){ui.onAllNodesWithText("Recordatorios guardados.").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithContentDescription("Recordar tareas pendientes").assertIsOn()
  ui.onNodeWithText("Un día antes").performScrollTo().performClick()
  ui.waitUntil(20000){ui.onAllNodes(hasText("Un día antes") and isSelected()).fetchSemanticsNodes().isNotEmpty()}
  val api=MainApi("http://127.0.0.1:8088");api.login(name,"SyntheticReminder2026!")
  assertEquals(ReminderPreferences(true,1440),api.reminderPreferences())
  api.reminderPreferences(ReminderPreferences(false,1440));api.logout()
  Evidence.capture("recordatorios_perfil.png")
 }
}
