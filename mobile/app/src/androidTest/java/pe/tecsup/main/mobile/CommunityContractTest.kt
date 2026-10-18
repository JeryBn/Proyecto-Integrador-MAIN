package pe.tecsup.main.mobile
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import pe.tecsup.main.mobile.data.*

class CommunityContractTest{
 @Test fun familyCannotSelfGrantAndVerifiedAgendaAndSettingsSurviveNewLogin(){
  val admin=AdminTestApi();val username="family_"+System.currentTimeMillis()
  admin.post("/api/admin/v1/users",JSONObject().put("username",username).put("name","Familiar sintético").put("role","FAMILIAR").put("password","SyntheticFamily2026!"))
  val api=MainApi("http://127.0.0.1:8088");assertEquals("Familiar",api.login(username,"SyntheticFamily2026!").rol)
  assertTrue(api.agenda().courses.isEmpty())
  admin.post("/api/admin/v1/guardians",JSONObject().put("guardian",username).put("studentId",1))
  assertEquals(2,api.agenda().courses.size);assertEquals(listOf("Alex Demo"),api.agenda().students)
  api.savePreferences(UserPreferences(true,false,"PRIVATE"));api.logout()
  val another=MainApi("http://127.0.0.1:8088");another.login(username,"SyntheticFamily2026!")
  assertTrue(another.preferences().darkTheme);assertFalse(another.preferences().notifications)
  try{another.pending();fail("A family must not use the student's legacy activity scope")}catch(expected:ApiFailure){assertTrue(expected.message!!.contains("permiso"))}
  another.logout()
 }
 @Test fun approvedContactMessagingUsesRealModerationAndBlockedContactCannotSend(){
  val admin=AdminTestApi()
  admin.post("/api/admin/v1/contacts",JSONObject().put("first","alumno").put("second","otro"))
  val sender=MainApi("http://127.0.0.1:8088");sender.login("alumno","MainDemo2026!")
  val receiver=MainApi("http://127.0.0.1:8088");receiver.login("otro","MainDemo2026!")
  sender.saveContact(sender.contacts().first{it.username=="otro"}.copy(blocked=false))
  receiver.saveContact(receiver.contacts().first{it.username=="alumno"}.copy(blocked=false))
  val id=sender.startGroup("Contrato Android "+System.currentTimeMillis());sender.invite(id,"otro");receiver.invitation(id,true)
  sender.sendMessage(id,java.util.UUID.randomUUID().toString(),"Mensaje de contrato sintético")
  val draft=sender.messages(id).first();assertEquals("PENDING",draft.state);assertTrue(receiver.messages(id).isEmpty())
  admin.review(draft.id,true);assertEquals("Mensaje de contrato sintético",receiver.messages(id).first().text)
  sender.logout();receiver.logout()
 }
}
