package pe.tecsup.main.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import pe.tecsup.main.mobile.data.*
import java.util.UUID

/** Fixtures disposable creados por Django y la UI web; nunca usa cuentas personales. */
@RunWith(AndroidJUnit4::class)
class WebParityBridgeTest {
 private val args=InstrumentationRegistry.getArguments()
 private fun arg(name:String)=requireNotNull(args.getString(name)){"Falta fixture sintético $name"}
 @Test fun webDataAndModerationAreSharedWithAndroidInBothDirections(){
  val student=MainApi("http://127.0.0.1:8088")
  val peer=MainApi("http://127.0.0.1:8088")
  val outsider=MainApi("http://127.0.0.1:8088")
  try{
   student.login(arg("webStudent"),"SyntheticWebParity2026!")
   peer.login(arg("webPeer"),"SyntheticWebParity2026!")
   outsider.login(arg("webOutsider"),"SyntheticWebParity2026!")
   val conversation=arg("webConversation")
   val message=peer.messages(conversation).first{it.id==arg("webMessage")}
   assertEquals(2,message.revision);assertTrue(message.correctedBeforeDelivery)
   assertEquals("WEB-PARITY mensaje corregido desde la interfaz web",message.text)
   val text=peer.messageAttachment(arg("webAttachment"),1).toString(Charsets.UTF_8)
   assertTrue(text.contains("WEB-PARITY documento sintético"))
   assertThrows(ResourceUnavailable::class.java){outsider.messages(conversation)}
   assertThrows(ResourceUnavailable::class.java){outsider.messageAttachment(arg("webAttachment"),1)}
   assertTrue(peer.statuses().any{it.text=="WEB-PARITY estado de texto creado desde la web"})
   assertFalse(peer.statuses().any{it.text=="WEB-PARITY imagen privada creada desde web"})
   assertTrue(student.statuses().any{it.text=="WEB-PARITY imagen privada creada desde web"})
   assertEquals("19",student.grades().first{it.courseId==arg("webCourse").toLong()}.grades
    .first{it.assessment=="Evaluación web de prueba"}.score)
   val label="WEB-PARITY respuesta creada desde Android "+System.currentTimeMillis()
   peer.sendMessage(conversation,UUID.randomUUID().toString(),label)
   val created=peer.messages(conversation).first{it.text==label}
   assertFalse(student.messages(conversation).any{it.id==created.id})
   val admin=AdminTestApi();admin.review(created.id,true)
   assertTrue(student.messages(conversation).any{it.id==created.id && it.state=="APPROVED"})
   val statusLabel="WEB-PARITY estado creado desde Android "+System.currentTimeMillis()
   peer.createStatus(UUID.randomUUID().toString(),statusLabel,"#087F68","CONTACTS",null)
   val state=peer.statuses().first{it.text==statusLabel}
   assertFalse(student.statuses().any{it.id==state.id})
   val queue=admin.list("/api/admin/v1/moderation/statuses?limit=100")
   val entry=(0 until queue.length()).map{queue.getJSONObject(it)}.first{it.getString("id")==state.id}
   admin.post("/api/admin/v1/moderation/statuses/${state.id}/review",JSONObject()
    .put("contentHash",entry.getString("contentHash")).put("approved",true))
   assertTrue(student.statuses().any{it.id==state.id && it.state=="APPROVED"})
   // Se conservan ambos fixtures para verificar su recepción en la UI web.
  }finally{student.logout();peer.logout();outsider.logout()}
 }
}
