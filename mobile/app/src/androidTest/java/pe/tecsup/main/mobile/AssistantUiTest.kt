package pe.tecsup.main.mobile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class AssistantUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun liveAssistantShowsAnswerOrExplicitProviderFailure() {
  ui.onNodeWithText("Servidor").performTextReplacement("http://127.0.0.1:8088")
  ui.onNodeWithText("Usuario").performTextReplacement("alumno")
  ui.onNodeWithText("Contraseña").performTextReplacement("MainDemo2026!")
  ui.onNodeWithText("Ingresar").performClick()
  ui.waitUntil(20000){ui.onAllNodesWithText("Tu espacio académico").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithContentDescription("Asistente").performClick()
  ui.onNodeWithText("Consultar").assertIsNotEnabled()
  ui.onNodeWithText("Consulta académica").performTextReplacement("Prueba sintética MAIN: explica 2 + 2 en una frase.")
  androidx.test.espresso.Espresso.closeSoftKeyboard()
  ui.onNodeWithText("Consultar").performScrollTo().performClick()
  ui.waitUntil(65000) {
   ui.onAllNodesWithText("Respuesta de IA").fetchSemanticsNodes().isNotEmpty() ||
   ui.onAllNodesWithText("Reintentar consulta").fetchSemanticsNodes().isNotEmpty()
  }
  // This verifies the real UI/transport. A provider failure is not a successful generated answer.
  val answered=ui.onAllNodesWithText("Respuesta de IA").fetchSemanticsNodes().isNotEmpty()
  if(!answered) ui.onNodeWithText("El proyecto de OpenAI no tiene cuota o saldo disponible. El responsable debe revisar la facturación.").performScrollTo().assertIsDisplayed()
  Evidence.capture("14_asistente_resultado_real.png")
 }
}
