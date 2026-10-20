package pe.tecsup.main.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test

/** Uses only the isolated loopback fixture, never institutional accounts or production data. */
class AcademicAdministrationUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun administrativeEnrollmentAppearsInAndroidAndRevocationRemovesIt(){
  val admin=AdminTestApi()
  val accounts=admin.list("/api/admin/v1/users?limit=100")
  if((0 until accounts.length()).none{accounts.getJSONObject(it).getString("username")=="docente_revision"})
   admin.post("/api/admin/v1/users",JSONObject().put("username","docente_revision").put("name","Docente de pruebas").put("role","DOCENTE").put("password","MainReview2026!").put("docenteId",1))
  val id=9200;val label="Integración académica M13"
  val courses=admin.list("/api/admin/v1/courses?limit=100")
  if((0 until courses.length()).none{courses.getJSONObject(it).getInt("id")==id})
   admin.post("/api/admin/v1/courses",JSONObject().put("id",id).put("name",label).put("teacherUsername","docente_revision"))
  admin.put("/api/admin/v1/courses/$id/students",JSONObject().put("username","alumno").put("enrolled",true))
  try{
   ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
   ui.onNodeWithText("Usuario").performTextReplacement("alumno")
   ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
   ui.onNodeWithText("Ingresar").performClick()
   ui.waitUntil(30000){ui.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()}
   ui.onAllNodesWithText(label).onFirst().performScrollTo().assertIsDisplayed()
   Evidence.capture("24_matricula_administrativa_android.png")
  }finally{
   admin.put("/api/admin/v1/courses/$id/students",JSONObject().put("username","alumno").put("enrolled",false))
  }
  val student=pe.tecsup.main.mobile.data.MainApi("http://127.0.0.1:8088")
  student.login("alumno","MainDemo2026!")
  org.junit.Assert.assertFalse(student.agenda().courses.any{it.id==id.toLong()})
  student.logout()
 }
}
