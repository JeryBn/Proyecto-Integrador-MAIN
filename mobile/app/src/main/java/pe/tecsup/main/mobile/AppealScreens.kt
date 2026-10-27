package pe.tecsup.main.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal fun appealLabel(state:String)=when(state){"OPEN"->"En revisión";"UPHELD"->"Se mantiene el rechazo";"RETURNED"->"Devuelto a revisión humana";"SUPERSEDED"->"Cerrada: contenido cambiado o caducado";else->"Estado pendiente"}
@Composable internal fun NewAppealDialog(model:MainViewModel){
 val target=model.appealTarget?:return
 var reason by remember(target){mutableStateOf("")}
 AlertDialog(onDismissRequest={if(!model.accountBusy)model.appealTarget=null},title={Text("Apelar rechazo")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Explica por qué solicitas otra revisión. El contenido seguirá privado y protegido mientras se resuelve.")
  OutlinedTextField(value=reason,onValueChange={if(it.length<=1000)reason=it},label={Text("Motivo de apelación")},enabled=!model.accountBusy)
  if(model.accountError.isNotBlank())Text(model.accountError,color=MaterialTheme.colorScheme.error)
 }},confirmButton={TextButton(enabled=!model.accountBusy&&reason.isNotBlank(),onClick={model.submitAppeal(reason.trim())}){Text("Enviar apelación")}},dismissButton={TextButton(enabled=!model.accountBusy,onClick={model.appealTarget=null}){Text("Cancelar")}})
}
@Composable internal fun AppealHistoryScreen(model:MainViewModel){
 LaunchedEffect(Unit){model.loadAppeals()}
 PageHeading("Mis apelaciones"){model.settingsPage="MAIN"}
 Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("La apelación no publica el contenido. Si vuelve a revisión, espera la nueva decisión humana. El historial resuelto se conserva noventa días, salvo protección abierta.")
  TextButton(enabled=!model.accountBusy,onClick={model.loadAppeals()}){Text("Actualizar apelaciones")}
  if(model.appealItems.isEmpty()&&!model.accountBusy)Text("No tienes apelaciones en esta página.")
  model.appealItems.forEach{a->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text((if(a.kind=="messages")"Mensaje · versión "+a.revision else "Estado")+" · "+appealLabel(a.state),style=MaterialTheme.typography.titleMedium)
   Text(a.createdAt);Text("Tu motivo: "+a.reason)
   if(a.decisionReason.isNotBlank())Text("Resolución: "+a.decisionReason)
   if(a.decidedAt.isNotBlank())Text(a.decidedAt)
  }}}
  Row{TextButton(enabled=!model.accountBusy&&model.appealPage>0,onClick={model.loadAppeals(-1)}){Text("Anteriores")};TextButton(enabled=!model.accountBusy&&model.appealItems.size==30,onClick={model.loadAppeals(1)}){Text("Más apelaciones")}}
 }
}
