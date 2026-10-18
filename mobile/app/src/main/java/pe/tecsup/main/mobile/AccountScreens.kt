package pe.tecsup.main.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable internal fun AcademicAgendaScreen(model: MainViewModel) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("Cursos y horario",style=MaterialTheme.typography.headlineMedium);AgendaContent(model)
 }
}
@Composable internal fun AgendaContent(model: MainViewModel) {
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Horario semanal",style=MaterialTheme.typography.titleLarge)
  if(model.agenda.students.isNotEmpty()){
   Text("Estudiantes vinculados",style=MaterialTheme.typography.titleMedium);model.agenda.students.forEach{Text(it)}
  }
  if(model.agenda.timetable.isEmpty())Text("No hay clases registradas en tu horario.")
  val days=listOf("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo")
  model.agenda.timetable.forEach{slot->
   Card(Modifier.fillMaxWidth()){
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
     Text(slot.course,style=MaterialTheme.typography.titleMedium)
     Text(days.getOrElse(slot.dayOfWeek-1){"Día"}+" · "+slot.startsAt+"–"+slot.endsAt)
     Text(slot.classroom+" · "+slot.teacher)
    }
   }
  }
  Text("Mis cursos",style=MaterialTheme.typography.titleLarge)
  if(model.agenda.courses.isEmpty())Text("No hay cursos vinculados a tu cuenta.")
  model.agenda.courses.forEach{course->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(course.name,style=MaterialTheme.typography.titleMedium);Text(course.teacher)}}}
  AccountFeedback(model)
  TextButton(onClick=model::refreshAccount,enabled=!model.accountBusy){Text("Actualizar agenda")}
 }
}
@Composable private fun AccountFeedback(model: MainViewModel){
 if(model.accountBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
 if(model.accountError.isNotBlank())Text(model.accountError,color=MaterialTheme.colorScheme.error)
 if(model.accountMessage.isNotBlank())Text(model.accountMessage,color=MaterialTheme.colorScheme.primary)
}
@Composable internal fun AccountProfileScreen(model: MainViewModel){
 var reportTarget by rememberSaveable{mutableStateOf("")};var reportReason by rememberSaveable{mutableStateOf("")}
 LaunchedEffect(model.accountMessage){if(model.accountMessage=="Reporte enviado para revisión administrativa."){reportTarget="";reportReason=""}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("MAIN",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
  Text("Perfil",style=MaterialTheme.typography.headlineMedium)
  Card(Modifier.fillMaxWidth()){
   Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Text(model.profile?.nombre.orEmpty(),style=MaterialTheme.typography.titleLarge)
    Text("Usuario: "+model.profile?.usuario.orEmpty());Text("Rol: "+model.profile?.rol.orEmpty())
   }
  }
  PreferenceSwitch("Tema oscuro","Se guarda en tu cuenta.",model.darkTheme,!model.accountBusy){model.savePreferences(model.preferences.copy(darkTheme=it))}
  PreferenceSwitch("Avisos en MAIN","Preferencia de recepción de avisos.",model.preferences.notifications,!model.accountBusy){model.savePreferences(model.preferences.copy(notifications=it))}
  if(model.profile?.rol!="Familiar"){
   Text("Audiencia para nuevos estados",style=MaterialTheme.typography.titleMedium)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    FilterChip(selected=model.preferences.statusAudience=="CONTACTS",enabled=!model.accountBusy,onClick={model.savePreferences(model.preferences.copy(statusAudience="CONTACTS"))},label={Text("Mis contactos")})
    FilterChip(selected=model.preferences.statusAudience=="PRIVATE",enabled=!model.accountBusy,onClick={model.savePreferences(model.preferences.copy(statusAudience="PRIVATE"))},label={Text("Solo yo")})
   }
  }else Text("Los familiares pueden consultar estados autorizados; no pueden publicar estados.")
  AccountFeedback(model)
  TextButton(onClick=model::refreshAccount,enabled=!model.accountBusy){Text("Actualizar preferencias y contactos")}
  Text("Privacidad de contactos",style=MaterialTheme.typography.titleLarge)
  if(model.contacts.isEmpty())Text("No tienes contactos autorizados.")
  model.contacts.forEach{contact->
   Card(Modifier.fillMaxWidth()){
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Text(contact.name,style=MaterialTheme.typography.titleMedium);Text(contact.username)
     PreferenceSwitch("Silenciar "+contact.name,"Evita avisos de este contacto.",contact.muted,!model.accountBusy){model.saveContact(contact.copy(muted=it))}
     PreferenceSwitch("Bloquear "+contact.name,"Impide comunicación directa en ambos sentidos.",contact.blocked,!model.accountBusy){model.saveContact(contact.copy(blocked=it))}
     TextButton(onClick={reportTarget=contact.username},enabled=!model.accountBusy){Text("Reportar contacto")}
    }
   }
  }
  Button(onClick=model::logout,enabled=!model.busy,modifier=Modifier.fillMaxWidth()){Text(if(model.busy)"Cerrando sesión…" else "Cerrar sesión")}
 }
 if(reportTarget.isNotEmpty())AlertDialog(
  onDismissRequest={if(!model.accountBusy)reportTarget=""},
  title={Text("Reportar contacto")},
  text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("El reporte será visible para la administración de MAIN.")
   OutlinedTextField(value=reportReason,onValueChange={reportReason=it.take(1000)},label={Text("Motivo del reporte")})
   if(model.accountError.isNotBlank())Text(model.accountError,color=MaterialTheme.colorScheme.error)
  }},
  confirmButton={TextButton(enabled=reportReason.isNotBlank()&&!model.accountBusy,onClick={model.reportContact(reportTarget,reportReason)}){Text("Enviar reporte")}},
  dismissButton={TextButton(enabled=!model.accountBusy,onClick={reportTarget=""}){Text("Cancelar")}}
 )
}
@Composable private fun PreferenceSwitch(label:String,description:String,checked:Boolean,enabled:Boolean,onChange:(Boolean)->Unit){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Column(Modifier.weight(1f)){Text(label,style=MaterialTheme.typography.titleMedium);Text(description,style=MaterialTheme.typography.bodySmall)}
  Switch(checked=checked,onCheckedChange=onChange,enabled=enabled,modifier=Modifier.semantics{contentDescription=label})
 }
}
