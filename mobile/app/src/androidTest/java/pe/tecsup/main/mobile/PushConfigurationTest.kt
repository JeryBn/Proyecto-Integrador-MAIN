package pe.tecsup.main.mobile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class PushConfigurationTest {
 @Test fun configuredFirebaseObtainsRealTokenWithoutPublishingContent(){
  org.junit.Assume.assumeTrue("Prueba de red optativa",InstrumentationRegistry.getArguments().getString("realPushProbe")=="true")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val app=FirebaseApp.initializeApp(context)
  assertNotNull("Falta google-services.json local",app)
  assertEquals("main-integrador-jery-2026",app!!.options.projectId)
  val token=Tasks.await(FirebaseMessaging.getInstance().token,45,TimeUnit.SECONDS)
  assertTrue("Firebase no devolvió un token",token.length>20)
  // Token is deliberately never logged or written to evidence.
 }
 @Test fun logoutClearsLocalBindingAndRejectsDelayedNotifications(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  PushLocal.save(context,"synthetic-session")
  assertTrue(acceptsPush(PushLocal.binding(context),"synthetic-session","MAIN_UPDATE"))
  PushLocal.clear(context)
  assertFalse(acceptsPush(PushLocal.binding(context),"synthetic-session","MAIN_UPDATE"))
 }
}
