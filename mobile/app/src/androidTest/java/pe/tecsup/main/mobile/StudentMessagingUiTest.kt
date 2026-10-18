package pe.tecsup.main.mobile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import pe.tecsup.main.mobile.data.MainApi

class StudentMessagingUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun waitFor(text:String){ui.waitUntil(25000){ui.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun studentCanSeeCourseGradesStartPrivateChatsAndBlockFromContactInfo(){
  val admin=AdminTestApi()
  val users=admin.list("/api/admin/v1/users?limit=100")
  val records=(0 until users.length()).map{users.getJSONObject(it)}
  if(records.none{it.getString("username")=="docente_revision"})
   admin.post("/api/admin/v1/users",JSONObject().put("username","docente_revision").put("name","Profesor de revisión").put("role","DOCENTE").put("password","MainReview2026!").put("docenteId",1))
  val teacher=admin.list("/api/admin/v1/users?limit=100").let{rows->(0 until rows.length()).map{rows.getJSONObject(it)}.first{it.getString("username")=="docente_revision"}.getString("name")}
  for(target in listOf("otro","docente_revision"))admin.post("/api/admin/v1/contacts",JSONObject().put("first","alumno").put("second",target))
  val api=MainApi("http://127.0.0.1:8088")
  api.login("alumno","MainDemo2026!");api.savePreferences(pe.tecsup.main.mobile.data.UserPreferences())
  api.contacts().filter{it.username in listOf("otro","docente_revision")}.forEach{api.saveContact(it.copy(blocked=false))}
  admin.put("/api/admin/v1/courses/1/grades",JSONObject().put("studentId",1).put("assessment","Evaluación sintética de revisión").put("score",16).put("maximum",20))
  ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
  ui.onNodeWithText("Usuario").performTextReplacement("alumno")
  ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
  ui.onNodeWithText("Ingresar").performClick();waitFor("Tu espacio académico")
  ui.onNode(hasClickAction() and hasText("Construcción y Pruebas de Software")).performScrollTo().performClick()
  ui.onNodeWithText("Evaluación sintética de revisión").performScrollTo().assertIsDisplayed()
  ui.onNodeWithText("16 / 20").assertIsDisplayed()
  Evidence.capture("15_notas_por_curso.png")
  ui.onNodeWithContentDescription("Mensajes").performClick()
  ui.onNodeWithContentDescription("Nuevo chat").performClick()
  ui.onNodeWithText("Profesores").performClick()
  ui.onNodeWithText(teacher).performClick();waitFor("Ver información del contacto")
  ui.onNodeWithText(teacher).assertIsDisplayed()
  Evidence.capture("16_chat_privado_profesor.png")
  ui.onNodeWithText("Ver información del contacto").performClick()
  ui.onNodeWithText("Bloquear a "+teacher).performScrollTo().performClick()
  ui.onNodeWithText("Bloquear",substring=false).performClick()
  waitFor("Desbloquear a "+teacher)
  Evidence.capture("17_contacto_bloqueado.png")
  api.saveContact(api.contacts().first{it.username=="docente_revision"}.copy(blocked=false))
  androidx.test.espresso.Espresso.pressBack()
  androidx.test.espresso.Espresso.pressBack()
  ui.onNodeWithContentDescription("Nuevo chat").performClick()
  ui.onNodeWithText("Estudiantes").performClick()
  ui.onNodeWithText("Sam Demo").performClick();waitFor("Ver información del contacto")
  ui.onNodeWithText("Sam Demo").assertIsDisplayed()
  Evidence.capture("18_chat_privado_estudiante.png")
  api.logout()
 }
}
