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
  CampusBanner("Asistente académico")
  OutlinedTextField(value=model.assistantQuestion,onValueChange={if(it.length<=2000)model.assistantQuestion=it},
   modifier=Modifier.fillMaxWidth(),label={Text("Consulta académica")},enabled=!model.assistantBusy,minLines=3)
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
