package pe.tecsup.main.mobile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import pe.tecsup.main.mobile.data.MainApi

class ConversationUiTest{
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun waitFor(text:String){ui.waitUntil(20_000){ui.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun rejectedTextIsCorrectedInAppAndOnlyApprovedRevisionReachesOtherAccount(){
  val admin=AdminTestApi();admin.post("/api/admin/v1/contacts",JSONObject().put("first","alumno").put("second","otro"))
  val observer=MainApi("http://127.0.0.1:8088");observer.login("alumno","MainDemo2026!")
  observer.savePreferences(pe.tecsup.main.mobile.data.UserPreferences())
  val receiver=MainApi("http://127.0.0.1:8088");receiver.login("otro","MainDemo2026!")
  observer.saveContact(observer.contacts().first{it.username=="otro"}.copy(blocked=false))
  receiver.saveContact(receiver.contacts().first{it.username=="alumno"}.copy(blocked=false))
  ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
  ui.onNodeWithText("Usuario").performTextReplacement("alumno")
  ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
  ui.onNodeWithText("Ingresar").performClick();waitFor("Tu espacio académico")
  ui.onNodeWithContentDescription("Mensajes").performClick()
  ui.onNodeWithContentDescription("Nuevo chat").performClick()
  ui.onNodeWithText("Nuevo grupo").performClick()
  val title="Grupo UI "+System.currentTimeMillis()
  ui.onNodeWithText("Nombre del grupo").performTextInput(title)
  androidx.test.espresso.Espresso.closeSoftKeyboard()
  ui.onNodeWithText("Crear grupo").performClick();waitFor("Escribe un mensaje")
  val conversation=observer.conversations().first{it.title==title}
  observer.invite(conversation.id,"otro");receiver.invitation(conversation.id,true)
  ui.onNodeWithContentDescription("Escribe un mensaje").performTextInput("Borrador privado para revisión")
  androidx.test.espresso.Espresso.closeSoftKeyboard()
  ui.onNodeWithContentDescription("Enviar a revisión").performClick();waitFor("Pendiente de revisión")
  val first=observer.messages(conversation.id).first()
  assertTrue(receiver.messages(conversation.id).isEmpty());admin.review(first.id,false)
  ui.onNodeWithContentDescription("Opciones del chat").performClick()
  ui.onNodeWithText("Actualizar",substring=false).performClick();waitFor("No enviado · revisar")
  ui.onNodeWithText("Corregir mensaje").performClick()
  ui.onNodeWithContentDescription("Escribe un mensaje").performTextReplacement("Texto corregido aprobado")
  androidx.test.espresso.Espresso.closeSoftKeyboard()
  ui.onNodeWithContentDescription("Enviar corrección a revisión").performClick();waitFor("Pendiente de revisión")
  assertTrue(receiver.messages(conversation.id).isEmpty());admin.review(first.id,true)
  ui.onNodeWithContentDescription("Opciones del chat").performClick()
  ui.onNodeWithText("Actualizar",substring=false).performClick();waitFor("Entregado")
  val delivered=receiver.messages(conversation.id).first()
  assertEquals("Texto corregido aprobado",delivered.text);assertTrue(delivered.correctedBeforeDelivery)
  assertFalse(delivered.text.contains("Borrador privado"));Evidence.capture("13_conversacion_revisada.png")
  observer.logout();receiver.logout()
 }
}
