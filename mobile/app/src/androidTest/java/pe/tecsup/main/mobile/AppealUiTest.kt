package pe.tecsup.main.mobile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import pe.tecsup.main.mobile.data.*
import java.util.UUID

class AppealUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun waitFor(text:String){ui.waitUntil(30000){ui.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun studentAppealsAndSeesIndependentResolutionBeforeNewHumanApproval(){
  val reviewerName=requireNotNull(InstrumentationRegistry.getArguments().getString("appealReviewer")){"Provisiona un ADMIN sintético independiente solo para este ensayo."}
  val admin=AdminTestApi();val reviewer=AdminTestApi(reviewerName,"SyntheticAppealReviewer2026!")
  val stamp=System.currentTimeMillis();val owner="appeal_android_$stamp";val password="SyntheticAppealOwner2026!"
  admin.post("/api/admin/v1/users",JSONObject().put("username",owner).put("name","Estudiante apelación sintética").put("role","ALUMNO").put("password",password).put("alumnoId",stamp))
  val api=MainApi("http://127.0.0.1:8088");api.login(owner,password)
  val title="Apelación Android $stamp";val conversation=api.startGroup(title)
  api.sendMessage(conversation,UUID.randomUUID().toString(),"Mensaje sintético para apelar")
  val draft=api.messages(conversation).first();admin.review(draft.id,false)
  ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088");ui.onNodeWithText("Usuario").performTextReplacement(owner);ui.onNodeWithText("Contraseña").performTextReplacement(password);ui.onNodeWithText("Ingresar").performClick();waitFor("Tu espacio académico")
  ui.onNodeWithContentDescription("Mensajes").performClick();waitFor(title);ui.onNodeWithText(title).performClick();waitFor("Apelar rechazo")
  ui.onNodeWithText("Apelar rechazo").performScrollTo().performClick();ui.onNodeWithText("Motivo de apelación").performTextInput("Solicito revisar el contexto educativo sintético.");ui.onNodeWithText("Enviar apelación").performClick()
  waitFor("Mis apelaciones");waitFor("En revisión");Evidence.capture("apelacion_android_abierta.png")
  val appeal=api.appeals().single()
  reviewer.post("/api/admin/v1/appeals/${appeal.id}/resolve",JSONObject().put("outcome","RETURNED").put("reason","Se devuelve a revisión humana por contexto educativo.").put("contentHash",draft.contentHash))
  assertEquals("PENDING",api.messages(conversation).first().state)
  ui.onNodeWithText("Actualizar apelaciones").performScrollTo().performClick();waitFor("Devuelto a revisión humana");waitFor("Se devuelve a revisión humana por contexto educativo.");Evidence.capture("apelacion_android_resuelta.png")
  admin.review(draft.id,true);assertEquals("APPROVED",api.messages(conversation).first().state);api.logout()
 }
}
