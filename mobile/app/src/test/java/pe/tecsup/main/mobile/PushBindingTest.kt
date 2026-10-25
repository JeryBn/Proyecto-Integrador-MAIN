package pe.tecsup.main.mobile
import org.junit.Assert.*
import org.junit.Test
class PushBindingTest {
 @Test fun onlyCurrentLoginGenericUpdatesAreAccepted(){
  assertTrue(acceptsPush("current","current","MAIN_UPDATE"))
  assertFalse(acceptsPush(null,"current","MAIN_UPDATE"))
  assertFalse(acceptsPush("new-account","old-account","MAIN_UPDATE"))
  assertFalse(acceptsPush("current","current","MESSAGE_CONTENT"))
  assertFalse(acceptsPush("","","MAIN_UPDATE"))
 }
}
