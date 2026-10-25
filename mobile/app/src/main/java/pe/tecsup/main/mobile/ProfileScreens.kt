package pe.tecsup.main.mobile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.tecsup.main.mobile.data.ContactUser

@Composable internal fun AccountProfileScreen(model:MainViewModel) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
  when(model.settingsPage) {
   "BLOCKED" -> {
    PageHeading("Contactos bloqueados"){model.settingsPage="PRIVACY"}
    Text("Los contactos bloqueados no pueden intercambiar mensajes privados contigo.",Modifier.padding(20.dp),style=MaterialTheme.typography.bodySmall)
    if(model.contacts.none{it.blocked}) Text("No tienes contactos bloqueados.",Modifier.padding(20.dp))
    model.contacts.filter{it.blocked}.forEach { contact -> ContactRow(contact){model.contactProfile=contact.username} }
    HorizontalDivider()
    Text("Añadir contacto bloqueado",Modifier.padding(20.dp),style=MaterialTheme.typography.titleMedium)
    model.contacts.filter{!it.blocked}.forEach { contact -> ContactRow(contact){model.contactProfile=contact.username} }
   }
   "PRIVACY" -> {
    PageHeading("Privacidad"){model.settingsPage="MAIN"}
    SettingsRow(Icons.Outlined.Block,"Contactos bloqueados",model.contacts.count{it.blocked}.toString(),onClick={model.settingsPage="BLOCKED"})
    HorizontalDivider()
    if(model.profile?.rol!="Familiar")Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     Text("Privacidad de estados",style=MaterialTheme.typography.titleMedium)
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      FilterChip(selected=model.preferences.statusAudience=="CONTACTS",enabled=!model.accountBusy,onClick={model.savePreferences(model.preferences.copy(statusAudience="CONTACTS"))},label={Text("Mis contactos")})
      FilterChip(selected=model.preferences.statusAudience=="PRIVATE",enabled=!model.accountBusy,onClick={model.savePreferences(model.preferences.copy(statusAudience="PRIVATE"))},label={Text("Solo yo")})
     }
    }
   }
   else -> {
    Text("Ajustes",Modifier.padding(20.dp),style=MaterialTheme.typography.headlineMedium)
    Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)){
     PersonAvatar(model.profile?.nombre.orEmpty(),true)
     Column{Text(model.profile?.nombre.orEmpty(),style=MaterialTheme.typography.titleLarge);Text(model.profile?.rol.orEmpty(),color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
    HorizontalDivider()
    SettingsRow(Icons.Outlined.Lock,"Privacidad","Contactos bloqueados y audiencia",onClick={model.settingsPage="PRIVACY"})
    Column(Modifier.padding(horizontal=20.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
     PreferenceSwitch("Tema oscuro","Apariencia de MAIN",model.darkTheme,!model.accountBusy){model.savePreferences(model.preferences.copy(darkTheme=it))}
     PreferenceSwitch("Avisos en MAIN","Mensajes e invitaciones",model.preferences.notifications,!model.accountBusy){model.savePreferences(model.preferences.copy(notifications=it))}
     PushSettings(model)
    }
    HorizontalDivider()
    SettingsRow(Icons.AutoMirrored.Outlined.Logout,"Cerrar sesión",danger=true,onClick={if(!model.busy)model.logout()})
   }
  }
  Column(Modifier.padding(20.dp)){AccountFeedback(model);TextButton(onClick=model::refreshAccount,enabled=!model.accountBusy){Text("Actualizar preferencias")}}
 }
}
@Composable internal fun ContactRow(contact:ContactUser,onClick:()->Unit) {
 Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=20.dp,vertical=13.dp),
  verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
  PersonAvatar(contact.name)
  Column(Modifier.weight(1f)){Text(contact.name,style=MaterialTheme.typography.titleMedium)
   Text(if(contact.blocked)"Bloqueado" else when(contact.role){"DOCENTE"->"Profesor";"ALUMNO"->"Estudiante";else->"Familiar"},
    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
 }
}
@Composable internal fun ContactInfoScreen(model:MainViewModel) {
 val contact=model.contacts.firstOrNull{it.username==model.contactProfile}
 var confirmBlock by rememberSaveable {mutableStateOf(false)}
 var report by rememberSaveable {mutableStateOf(false)}
 var reason by rememberSaveable {mutableStateOf("")}
 LaunchedEffect(model.accountMessage){if(model.accountMessage=="Reporte enviado para revisión administrativa."){report=false;reason=""}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
  PageHeading("Información del contacto"){model.contactProfile=null}
  if(contact==null){Text("Contacto no disponible.",Modifier.padding(20.dp));return@Column}
  Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){
   PersonAvatar(contact.name,true);Text(contact.name,style=MaterialTheme.typography.headlineMedium)
   Text(when(contact.role){"DOCENTE"->"Profesor";"ALUMNO"->"Estudiante";else->"Familiar"},color=MaterialTheme.colorScheme.onSurfaceVariant)
   FilledTonalButton(enabled=!contact.blocked&&!model.chatBusy,onClick={model.contactProfile=null;model.selectedSectionForChat();model.startDirect(contact.username)}) {Text("Enviar mensaje")}
  }
  HorizontalDivider()
  Column(Modifier.padding(20.dp)){PreferenceSwitch("Silenciar notificaciones","No recibir avisos de este contacto",contact.muted,!model.accountBusy){model.saveContact(contact.copy(muted=it))}}
  HorizontalDivider()
  SettingsRow(Icons.Outlined.Block,if(contact.blocked)"Desbloquear a "+contact.name else "Bloquear a "+contact.name,danger=true,onClick={confirmBlock=true})
  SettingsRow(Icons.Outlined.Flag,"Reportar a "+contact.name,danger=true,onClick={report=true})
  Column(Modifier.padding(20.dp)){AccountFeedback(model)}
 }
 if(confirmBlock&&contact!=null)AlertDialog(onDismissRequest={confirmBlock=false},
  title={Text(if(contact.blocked)"¿Desbloquear contacto?" else "¿Bloquear contacto?")},
  text={Text(if(contact.blocked)"Podrán volver a intercambiar mensajes privados." else "No podrán intercambiar mensajes privados. Puedes desbloquearlo desde Privacidad. El historial existente no se elimina.")},
  confirmButton={TextButton(enabled=!model.accountBusy,onClick={model.saveContact(contact.copy(blocked=!contact.blocked));confirmBlock=false}){Text(if(contact.blocked)"Desbloquear" else "Bloquear")}},
  dismissButton={TextButton(onClick={confirmBlock=false}){Text("Cancelar")}})
 if(report&&contact!=null)AlertDialog(onDismissRequest={report=false},title={Text("Reportar contacto")},
  text={Column{Text("El motivo será revisado por la administración de MAIN.");OutlinedTextField(value=reason,onValueChange={reason=it.take(1000)},label={Text("Motivo del reporte")});AccountFeedback(model)}},
  confirmButton={TextButton(enabled=reason.isNotBlank()&&!model.accountBusy,onClick={model.reportContact(contact.username,reason)}){Text("Enviar reporte")}},
  dismissButton={TextButton(onClick={report=false}){Text("Cancelar")}})
}
