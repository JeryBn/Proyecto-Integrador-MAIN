package pe.tecsup.main.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.tecsup.main.mobile.data.*
import java.util.UUID

class MessageAttachmentUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun waitText(text:String){ui.waitUntil(30000){ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun interruptedUploadCanRetryWithoutDuplicate(){
  val sender=MainApi("http://127.0.0.1:8088");sender.login("alumno","MainDemo2026!")
  try{
   val group=sender.startGroup("Cancelación sintética "+System.currentTimeMillis())
   val key=UUID.randomUUID().toString();val file=StatusAttachment(ByteArray(256*1024){65},"text/plain")
   var interrupted=false
   try{sender.sendAttachment(group,key,"Reintento",file){if(!interrupted){interrupted=true;sender.cancelAttachmentUpload()}}}catch(_:Exception){}
   assertTrue(interrupted)
   sender.sendAttachment(group,key,"Reintento",file)
   assertEquals(1,sender.messages(group).count{it.text=="Reintento"})
  }finally{sender.logout()}
 }
 @Test fun privateAttachmentsUploadRetryAndDisplayInAndroid(){
  val admin=AdminTestApi();admin.post("/api/admin/v1/contacts",JSONObject().put("first","alumno").put("second","otro"))
  val sender=MainApi("http://127.0.0.1:8088");sender.login("alumno","MainDemo2026!")
  val recipient=MainApi("http://127.0.0.1:8088");recipient.login("otro","MainDemo2026!")
  for((api,target) in listOf(sender to "otro",recipient to "alumno"))api.contacts().first{it.username==target}.let{api.saveContact(it.copy(blocked=false))}
  val title="Adjuntos M08 "+System.currentTimeMillis();val group=sender.startGroup(title)
  sender.invite(group,"otro");recipient.invitation(group,true)
  val context=InstrumentationRegistry.getInstrumentation().context
  val bitmap=android.graphics.Bitmap.createBitmap(8,8,android.graphics.Bitmap.Config.ARGB_8888)
  val image=java.io.ByteArrayOutputStream().also{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}.toByteArray();bitmap.recycle()
  val inputs=listOf(
   Triple("TXT","text/plain","Material sintético de curso".toByteArray()),
   Triple("Foto","image/png",image),
   Triple("Video","video/mp4",context.assets.open("status-synthetic.mp4").use{it.readBytes()}),
   Triple("Audio","audio/mp4",context.assets.open("status-synthetic.m4a").use{it.readBytes()})
  )
  for((label,mime,bytes) in inputs){
   val key=UUID.randomUUID().toString();val progress=mutableListOf<Int>()
   val attachment=StatusAttachment(bytes,mime)
   sender.sendAttachment(group,key,label,attachment){progress.add(it)}
   assertEquals(100,progress.last());assertTrue(progress.zipWithNext().all{(a,b)->b>=a})
   val message=sender.messages(group).first{it.text==label}
   assertFalse(recipient.messages(group).any{it.id==message.id})
   val sanitized=admin.attachmentBytes(message.id,message.revision);assertTrue(sanitized.isNotEmpty())
   sender.sendAttachment(group,key,label,attachment)
   assertEquals(1,sender.messages(group).count{it.text==label})
   admin.review(message.id,true,attachmentReviewed=true)
   assertArrayEquals(sanitized,recipient.messageAttachment(message.id,message.revision))
  }
  ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
  ui.onNodeWithText("Usuario").performTextReplacement("otro")
  ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
  ui.onNodeWithText("Ingresar").performClick();waitText("Tu espacio académico")
  ui.onNodeWithContentDescription("Mensajes").performClick();waitText(title)
  ui.onNodeWithText(title).performClick();waitText("Abrir documento TXT")
  ui.onNodeWithText("Abrir documento TXT").performScrollTo().performClick();waitText("Material sintético de curso")
  Evidence.capture("25_adjunto_txt_chat.png");ui.onNodeWithText("Cerrar adjunto").performClick()
  ui.onNodeWithText("Ver foto").performScrollTo().performClick()
  ui.waitUntil(30000){ui.onAllNodesWithContentDescription("Foto del mensaje").fetchSemanticsNodes().isNotEmpty()}
  Evidence.capture("26_adjunto_foto_chat.png");ui.onNodeWithText("Cerrar adjunto").performClick()
  ui.onNodeWithText("Reproducir video").performScrollTo().performClick()
  ui.waitUntil(30000){ui.onAllNodesWithContentDescription("Video del mensaje").fetchSemanticsNodes().isNotEmpty()}
  Evidence.capture("27_adjunto_video_chat.png");ui.onNodeWithText("Cerrar adjunto").performClick()
  ui.onNodeWithText("Escuchar audio").performScrollTo().performClick()
  try{ui.waitUntil(30000){ui.onAllNodesWithContentDescription("Reproduciendo audio").fetchSemanticsNodes().isNotEmpty()}}
  catch(error:Throwable){Evidence.capture("fallo_adjunto_audio.png");throw AssertionError(ui.onAllNodes(isRoot()).fetchSemanticsNodes().indices.joinToString("\n"){ui.onAllNodes(isRoot())[it].printToString()},error)}
  Evidence.capture("28_adjunto_audio_chat.png");ui.onNodeWithText("Cerrar adjunto").performClick()
  sender.logout();recipient.logout()
 }
}
