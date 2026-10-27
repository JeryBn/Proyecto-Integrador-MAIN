package pe.tecsup.main.mobile
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import pe.tecsup.main.mobile.data.*

class ReminderContractTest {
 @Test fun settingsPersistAcrossSessionsAndStayIsolated(){
  val stamp=System.currentTimeMillis();val username="android_reminder_$stamp"
  AdminTestApi().post("/api/admin/v1/users",JSONObject().put("username",username).put("name","Recordatorio Android sintético").put("role","ALUMNO").put("password","SyntheticReminder2026!").put("alumnoId",stamp))
  val api=MainApi("http://127.0.0.1:8088");api.login(username,"SyntheticReminder2026!")
  assertFalse(api.reminderPreferences().enabled)
  assertEquals(ReminderPreferences(true,15),api.reminderPreferences(ReminderPreferences(true,15)));api.logout()
  val next=MainApi("http://127.0.0.1:8088");next.login(username,"SyntheticReminder2026!")
  assertEquals(ReminderPreferences(true,15),next.reminderPreferences())
  val other=MainApi("http://127.0.0.1:8088");other.login("vacio","MainDemo2026!")
  assertFalse(other.reminderPreferences().enabled)
  next.reminderPreferences(ReminderPreferences(false,15));next.logout();other.logout()
 }
 @Test fun invalidWindowIsRejectedWithoutChangingSavedPreferences(){
  val api=MainApi("http://127.0.0.1:8088");api.login("alumno","MainDemo2026!")
  val previous=api.reminderPreferences()
  try {api.reminderPreferences(ReminderPreferences(true,-1));fail("Invalid interval accepted")}
  catch(expected:ApiFailure){assertEquals(previous,api.reminderPreferences())}
  finally{api.logout()}
 }
}
