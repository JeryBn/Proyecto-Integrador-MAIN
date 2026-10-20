package pe.tecsup.main.mobile

import android.content.ContentValues
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import pe.tecsup.main.mobile.data.MainApi

/** Exercises Android's real document provider with a disposable synthetic TXT. */
class MessagePickerUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun waitText(text:String){ui.waitUntil(30000){ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()}}
 @Test fun systemPickerSelectsTxtAndSendsItForReview(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val resolver=context.contentResolver
  val name="MAIN-picker-${System.currentTimeMillis()}.txt"
  val content="Documento sintético elegido desde Android"
  val values=ContentValues().apply{put(MediaStore.Downloads.DISPLAY_NAME,name);put(MediaStore.Downloads.MIME_TYPE,"text/plain");put(MediaStore.Downloads.RELATIVE_PATH,"Download/MAIN-tests");put(MediaStore.Downloads.IS_PENDING,1)}
  val uri=checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values))
  val api=MainApi("http://127.0.0.1:8088")
  try{
   checkNotNull(resolver.openOutputStream(uri)).use{it.write(content.toByteArray())}
   values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);resolver.update(uri,values,null,null)
   api.login("alumno","MainDemo2026!");val title="Selector M08 "+System.currentTimeMillis();val group=api.startGroup(title)
   ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
   ui.onNodeWithText("Usuario").performTextReplacement("alumno")
   ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
   ui.onNodeWithText("Ingresar").performClick();waitText("Tu espacio académico")
   ui.onNodeWithContentDescription("Mensajes").performClick();waitText(title);ui.onNodeWithText(title).performClick()
   ui.waitUntil(30000){ui.onAllNodes(hasText("Adjuntar archivo") and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
   ui.onNodeWithText("Adjuntar archivo").performSemanticsAction(SemanticsActions.OnClick){it()}
   val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
   var selected:AccessibilityNodeInfo?=null
   val end=android.os.SystemClock.elapsedRealtime()+20000
   while(selected==null&&android.os.SystemClock.elapsedRealtime()<end){
    selected=automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(name)?.firstOrNull{it.text?.toString()==name}
    if(selected==null)android.os.SystemClock.sleep(150)
   }
   if(selected==null)Evidence.capture("fallo_selector_android.png")
   assertNotNull("Synthetic TXT should appear in Android document provider",selected)
   var node=selected;var clicked=false
   while(node!=null&&!clicked){if(node.isClickable)clicked=node.performAction(AccessibilityNodeInfo.ACTION_CLICK);node=node.parent}
   assertTrue("Select the synthetic file",clicked)
   waitText("TXT seleccionado");ui.onNodeWithContentDescription("Enviar archivo a revisión").performClick()
   waitText("Abrir documento TXT");ui.onNodeWithText("Abrir documento TXT").performClick();waitText(content)
   Evidence.capture("29_selector_archivo_android.png")
   val message=api.messages(group).single();assertEquals("PENDING",message.state)
   assertArrayEquals(content.toByteArray(),api.messageAttachment(message.id,message.revision))
  }finally{resolver.delete(uri,null,null);runCatching{api.logout()}}
 }
}
