package pe.tecsup.main.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AssistantScreen(model:MainViewModel) {
 Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
  verticalArrangement=Arrangement.spacedBy(14.dp)) {
  Text("Asistente académico",style=MaterialTheme.typography.headlineMedium)
  Text("Tu consulta y los nombres de tus cursos y horarios autorizados se envían a OpenAI. No se envían tus chats ni contactos. Evita incluir datos personales. Las respuestas pueden contener errores; comprueba la información importante.",
   style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(value=model.assistantQuestion,onValueChange={if(it.length<=2000)model.assistantQuestion=it},
   modifier=Modifier.fillMaxWidth(),label={Text("Consulta académica")},enabled=!model.assistantBusy,minLines=3)
  Text("Hasta 20 consultas por cuenta cada 24 horas.",style=MaterialTheme.typography.bodySmall)
  if(model.assistantBusy) {
   CircularProgressIndicator()
   Text("Consultando…")
   TextButton(onClick=model::cancelAssistant){Text("Cancelar consulta")}
  } else {
   Button(onClick=model::askAssistant,enabled=model.assistantQuestion.isNotBlank()){Text(if(model.assistantError.isBlank()) "Consultar" else "Reintentar consulta")}
  }
  if(model.assistantError.isNotBlank()) Text(model.assistantError,color=MaterialTheme.colorScheme.error)
  if(model.assistantAnswer.isNotBlank()) Card(Modifier.fillMaxWidth()) {
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text("Respuesta de IA",style=MaterialTheme.typography.titleMedium)
    Text(model.assistantAnswer)
   }
  }
 }
}
