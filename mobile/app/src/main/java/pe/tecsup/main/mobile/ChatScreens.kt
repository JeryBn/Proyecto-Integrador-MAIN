package pe.tecsup.main.mobile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable internal fun ConversationsScreen(model:MainViewModel){
 var title by rememberSaveable{mutableStateOf("")}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("Mensajes",style=MaterialTheme.typography.headlineMedium);Text("El contenido se revisa antes de entregarse.")}
  item{ChatFeedback(model);TextButton(onClick=model::loadConversations,enabled=!model.chatBusy){Text("Actualizar conversaciones")}}
  item{
   OutlinedTextField(value=title,onValueChange={title=it.take(100)},label={Text("Nombre del grupo")},modifier=Modifier.fillMaxWidth())
   Button(enabled=title.isNotBlank()&&!model.chatBusy,onClick={model.createGroup(title)}){Text("Crear grupo")}
  }
  item{Text("Conversaciones",style=MaterialTheme.typography.titleLarge)}
  if(model.conversations.isEmpty()&&!model.chatBusy)item{Text("No hay conversaciones en esta página.")}
  items(model.conversations,key={it.id}){conversation->
   Card(Modifier.fillMaxWidth()){
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Text(conversation.title,style=MaterialTheme.typography.titleMedium)
     Text(if(conversation.kind=="GROUP")"Grupo privado" else "Conversación privada")
     if(conversation.state=="INVITED"){
      Text("Invitación pendiente")
      Row{TextButton(onClick={model.respondInvitation(conversation.id,true)},enabled=!model.chatBusy){Text("Aceptar")}
       TextButton(onClick={model.respondInvitation(conversation.id,false)},enabled=!model.chatBusy){Text("Rechazar")}}
     }else Button(onClick={model.openConversation(conversation.id)},enabled=!model.chatBusy){Text("Abrir conversación")}
    }
   }
  }
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(onClick={model.conversationPage(-1)},enabled=model.conversationPage>0&&!model.chatBusy){Text("Página anterior")}
   TextButton(onClick={model.conversationPage(1)},enabled=model.conversations.size==30&&!model.chatBusy){Text("Página siguiente")}
  }}
  item{Text("Contactos autorizados",style=MaterialTheme.typography.titleLarge)}
  if(model.contacts.isEmpty())item{Text("No tienes contactos autorizados. La administración debe verificar los contactos.")}
  items(model.contacts,key={it.username}){contact->
   Card(Modifier.fillMaxWidth()){
    Column(Modifier.padding(16.dp)){
     Text(contact.name,style=MaterialTheme.typography.titleMedium);Text(contact.role)
     Button(enabled=!contact.blocked&&!model.chatBusy,onClick={model.startDirect(contact.username)}){Text("Conversar con "+contact.name)}
    }
   }
  }
  item{Text("Avisos",style=MaterialTheme.typography.titleLarge);Text("Se sincronizan al abrir o actualizar Mensajes. No incluyen contenido privado en la vista previa.",style=MaterialTheme.typography.bodySmall)}
  if(model.notices.isEmpty()) item{Text("No hay avisos visibles en esta página.")}
  items(model.notices,key={"notice-"+it.id}){notice->
   Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
    Text(if(notice.kind=="MESSAGE")"Mensaje aprobado disponible" else "Invitación a grupo pendiente")
    if(notice.read) Text("Leído") else TextButton(onClick={model.readNotice(notice.id)},enabled=!model.chatBusy){Text("Marcar leído")}
   } }
  }
  item{Row{
   TextButton(onClick={model.noticePage(-1)},enabled=model.noticePage>0&&!model.chatBusy){Text("Avisos anteriores")}
   TextButton(onClick={model.noticePage(1)},enabled=model.notices.size==30&&!model.chatBusy){Text("Más avisos")}
  }}
 }
}
@Composable private fun ChatFeedback(model:MainViewModel){
 if(model.chatBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
 if(model.chatError.isNotBlank())Text(model.chatError,color=MaterialTheme.colorScheme.error)
}
@Composable internal fun ConversationScreen(model:MainViewModel){
 val id=model.selectedConversation
 val owner=LocalLifecycleOwner.current
 DisposableEffect(owner,id){
  val observer=LifecycleEventObserver{_,event->
   if(event==Lifecycle.Event.ON_START)model.startMessageUpdates()
   if(event==Lifecycle.Event.ON_STOP)model.stopMessageUpdates()
  }
  owner.lifecycle.addObserver(observer)
  if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))model.startMessageUpdates()
  onDispose{owner.lifecycle.removeObserver(observer);model.stopMessageUpdates()}
 }
 var manage by rememberSaveable{mutableStateOf(false)}
 val conversation=model.conversations.firstOrNull{it.id==id}
 Column(Modifier.fillMaxSize().imePadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row{
   TextButton(onClick=model::closeConversation){Text("← Conversaciones")}
   TextButton(onClick=model::refreshMessages,enabled=!model.chatBusy){Text("Actualizar")}
  }
  Text(conversation?.title?:"Conversación",style=MaterialTheme.typography.titleLarge)
  if(conversation?.kind=="GROUP")TextButton(onClick={manage=true}){Text("Miembros del grupo")}
  ChatFeedback(model)
  Row{
   TextButton(onClick={model.messagePage(1)},enabled=model.chatMessages.size==30&&!model.chatBusy){Text("Mensajes anteriores")}
   TextButton(onClick={model.messagePage(-1)},enabled=model.chatPage>0&&!model.chatBusy){Text("Más recientes")}
  }
  LazyColumn(Modifier.weight(1f),reverseLayout=true,verticalArrangement=Arrangement.spacedBy(8.dp)){
   if(model.chatMessages.isEmpty()&&!model.chatBusy)item{Text("No hay mensajes visibles en esta página.")}
   items(model.chatMessages.reversed(),key={it.id}){message->
    val mine=message.sender==model.profile?.usuario
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(mine)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)){
     Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
      Text(message.senderName,style=MaterialTheme.typography.labelLarge);Text(message.text)
      Text(when(message.state){"PENDING"->"Pendiente de revisión";"REJECTED"->"Rechazado";"CANCELLED"->"Cancelado";else->"Aprobado"},style=MaterialTheme.typography.labelMedium)
      if(message.reason.isNotBlank())Text(message.reason,color=MaterialTheme.colorScheme.error)
      if(message.correctedBeforeDelivery)Text("Editado antes del envío para cumplir las normas de MAIN",style=MaterialTheme.typography.bodySmall)
      if(message.editedAfterDelivery)Text("Editado después del envío",style=MaterialTheme.typography.bodySmall)
      if(mine&&message.state!="CANCELLED")Row{
       TextButton(onClick={model.editMessage(message)},enabled=!model.chatBusy){Text("Editar mensaje")}
       if(message.state!="APPROVED")TextButton(onClick={model.cancelMessage(message)},enabled=!model.chatBusy){Text("Cancelar envío")}
      }
     }
    }
   }
  }
  if(model.editingMessage!=null)Row{Text("Editando mensaje");TextButton(onClick=model::cancelEdit,enabled=!model.sendingMessage){Text("Descartar edición")}}
  OutlinedTextField(value=model.messageDraft,onValueChange=model::editDraft,label={Text("Escribe un mensaje")},
   enabled=!model.sendingMessage,modifier=Modifier.fillMaxWidth(),maxLines=4)
  Button(onClick=model::sendMessage,enabled=model.messageDraft.isNotBlank()&&!model.chatBusy,modifier=Modifier.fillMaxWidth()){
   Text(if(model.sendingMessage)"Enviando…" else if(model.editingMessage!=null)"Enviar corrección a revisión" else "Enviar a revisión")
  }
 }
 if(manage)AlertDialog(onDismissRequest={manage=false},title={Text("Miembros del grupo")},confirmButton={TextButton(onClick={manage=false}){Text("Cerrar")}},
  text={LazyColumn(Modifier.heightIn(max=420.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   item{ChatFeedback(model)}
   items(model.chatMembers,key={it.username}){member->
    Column{
     Text(member.name+" · "+member.state)
     if(member.administrator)Text("Administrador")
     if(conversation?.administrator==true&&member.state=="ACTIVE"&&member.username!=model.profile?.usuario)Row{
      TextButton(onClick={model.updateMember(member.username,!member.administrator,false)},enabled=!model.chatBusy){Text(if(member.administrator)"Quitar administración" else "Hacer administrador")}
      TextButton(onClick={model.updateMember(member.username,false,true)},enabled=!model.chatBusy){Text("Retirar")}
     }
    }
   }
   if(conversation?.administrator==true){
    item{Text("Invitar contacto",style=MaterialTheme.typography.titleMedium)}
    items(model.contacts.filter{contact->!contact.blocked&&model.chatMembers.none{it.username==contact.username}},key={it.username}){contact->
     TextButton(onClick={model.inviteMember(contact.username)},enabled=!model.chatBusy){Text("Invitar a "+contact.name)}
    }
   }
   item{TextButton(onClick={model.updateMember(model.profile?.usuario.orEmpty(),false,true)},enabled=!model.chatBusy){Text("Salir del grupo")}}
  }})
}
