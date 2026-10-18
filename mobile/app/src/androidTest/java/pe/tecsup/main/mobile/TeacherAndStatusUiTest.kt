package pe.tecsup.main.mobile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import pe.tecsup.main.mobile.data.*
import java.util.UUID
class TeacherAndStatusUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun waitFor(text:String){try{ui.waitUntil(30000){ui.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}}catch(error:Throwable){Evidence.capture("fallo_notas_estados.png");throw AssertionError("Esperando: $text\n"+ui.onRoot().printToString(),error)}}
 private fun login(user:String,password:String){ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088");ui.onNodeWithText("Usuario").performTextReplacement(user);ui.onNodeWithText("Contraseña").performTextReplacement(password);ui.onNodeWithText("Ingresar").performClick();waitFor("Tu espacio académico")}
 @Test fun teacherRecordsEditsAndStudentReadsTheSavedGrade(){
  val admin=AdminTestApi();val records=admin.list("/api/admin/v1/users?limit=100")
  if((0 until records.length()).none{records.getJSONObject(it).getString("username")=="docente_revision"})admin.post("/api/admin/v1/users",JSONObject().put("username","docente_revision").put("name","Profesor de revisión").put("role","DOCENTE").put("password","MainReview2026!").put("docenteId",1))
  login("docente_revision","MainReview2026!")
  ui.onAllNodesWithText("Registrar y editar notas").onFirst().performScrollTo().performClick();waitFor("Alex Demo")
  ui.onNodeWithText("Registrar nota").performScrollTo().performClick()
  val label="Prueba docente "+System.currentTimeMillis()
  ui.onNodeWithText("Evaluación").performTextInput(label);ui.onNodeWithText("Nota",substring=false).performTextInput("17")
  ui.onNodeWithText("Guardar",substring=false).performClick();waitFor("Nota guardada.")
  ui.onNodeWithText(label).performScrollTo().assertIsDisplayed();ui.onNodeWithTag("grade-score-1-"+label).assertTextEquals("17 / 20")
  ui.onNodeWithContentDescription("Editar "+label+" de Alex Demo").performClick()
  ui.onNodeWithText("Nota",substring=false).performTextReplacement("18,5")
  ui.onNodeWithText("Guardar",substring=false).performClick();ui.waitUntil(30000){ui.onAllNodes(hasTestTag("grade-score-1-"+label) and hasText("18.5 / 20")).fetchSemanticsNodes().isNotEmpty()}
  Evidence.capture("19_profesor_edita_notas.png")
  val student=MainApi("http://127.0.0.1:8088");student.login("alumno","MainDemo2026!")
  assertEquals("18.5",student.grades().flatMap{it.grades}.first{it.assessment==label}.score);student.logout()
 }
 @Test fun imageViewerAndTextStateRespectReviewBeforeDelivery(){
  val admin=AdminTestApi();admin.post("/api/admin/v1/contacts",JSONObject().put("first","alumno").put("second","otro"))
  val publisher=MainApi("http://127.0.0.1:8088");publisher.login("alumno","MainDemo2026!");publisher.savePreferences(UserPreferences())
  publisher.saveContact(publisher.contacts().first{it.username=="otro"}.copy(blocked=false))
  val bitmap=android.graphics.Bitmap.createBitmap(300,500,android.graphics.Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.rgb(8,127,104));val image=java.io.ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,image);bitmap.recycle()
  val caption="Foto sintética "+System.currentTimeMillis();publisher.createStatus(UUID.randomUUID().toString(),caption,"#087F68","CONTACTS",StatusAttachment(image.toByteArray(),"image/png"))
  val photo=publisher.statuses().first{it.text==caption};val pending=admin.list("/api/admin/v1/moderation/statuses?limit=100");val item=(0 until pending.length()).map{pending.getJSONObject(it)}.first{it.getString("id")==photo.id}
  admin.post("/api/admin/v1/moderation/statuses/"+photo.id+"/review",JSONObject().put("contentHash",item.getString("contentHash")).put("approved",true))
  val peer=MainApi("http://127.0.0.1:8088");peer.login("otro","MainDemo2026!");peer.savePreferences(UserPreferences());peer.saveContact(peer.contacts().first{it.username=="alumno"}.copy(blocked=false))
  login("otro","MainDemo2026!");ui.onNodeWithContentDescription("Estados").performClick();waitFor("Toca para ver")
  ui.onAllNodesWithText("Toca para ver").onFirst().performClick();waitFor(caption)
  Evidence.capture("20_visor_estado_imagen.png");ui.onNodeWithText("Cerrar").performClick()
  ui.onNodeWithContentDescription("Crear estado de texto").performClick()
  val text="Estado de revisión "+System.currentTimeMillis();ui.onNodeWithText("Escribe tu estado").performTextInput(text)
  ui.onNodeWithText("Enviar estado").performScrollTo().performClick();waitFor("Pendiente de revisión")
  assertFalse(publisher.statuses().any{it.text==text})
  Evidence.capture("21_estado_pendiente.png")
  val own=peer.statuses().first{it.text==text};peer.deleteStatus(own.id);publisher.deleteStatus(photo.id);peer.logout();publisher.logout()
 }
 @Test fun approvedVideoAndAudioPlayInTheViewer(){
  val admin=AdminTestApi();admin.post("/api/admin/v1/contacts",JSONObject().put("first","vacio").put("second","otro"))
  val publisher=MainApi("http://127.0.0.1:8088");publisher.login("vacio","MainDemo2026!");publisher.savePreferences(UserPreferences());publisher.saveContact(publisher.contacts().first{it.username=="otro"}.copy(blocked=false))
  val peer=MainApi("http://127.0.0.1:8088");peer.login("otro","MainDemo2026!");peer.saveContact(peer.contacts().first{it.username=="vacio"}.copy(blocked=false))
  val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context
  val ids=mutableListOf<String>()
  try{
   for((asset,mime) in listOf("status-synthetic.mp4" to "video/mp4","status-synthetic.m4a" to "audio/mp4")){
    val label=asset+" "+System.currentTimeMillis();publisher.createStatus(UUID.randomUUID().toString(),label,"#087F68","CONTACTS",StatusAttachment(context.assets.open(asset).use{it.readBytes()},mime))
    val entry=publisher.statuses().first{it.text==label};ids+=entry.id
    val pending=admin.list("/api/admin/v1/moderation/statuses?limit=100");val row=(0 until pending.length()).map{pending.getJSONObject(it)}.first{it.getString("id")==entry.id}
    admin.post("/api/admin/v1/moderation/statuses/"+entry.id+"/review",JSONObject().put("contentHash",row.getString("contentHash")).put("approved",true))
   }
   login("otro","MainDemo2026!");ui.onNodeWithContentDescription("Estados").performClick();waitFor("Toca para ver")
   val name=peer.statuses().first{it.owner=="vacio"}.name
   ui.onNodeWithText(name).performClick()
   ui.waitUntil(30000){ui.onAllNodesWithContentDescription("Video del estado").fetchSemanticsNodes().isNotEmpty()}
   Evidence.capture("22_estado_video.png")
   ui.waitUntil(30000){ui.onAllNodesWithContentDescription("Reproduciendo audio").fetchSemanticsNodes().isNotEmpty()}
   Evidence.capture("23_estado_audio.png")
  }finally{ids.forEach{publisher.deleteStatus(it)};publisher.logout();peer.logout()}
 }

}
