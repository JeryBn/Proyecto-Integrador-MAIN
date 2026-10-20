package pe.tecsup.main.mobile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import pe.tecsup.main.mobile.data.ChatMessage

@Composable internal fun ConversationsScreen(model:MainViewModel){
 var search by rememberSaveable{mutableStateOf("")}
 var filter by rememberSaveable{mutableStateOf("Todos")}
 var showNotices by rememberSaveable{mutableStateOf(false)}
 Scaffold(
  floatingActionButton={ExtendedFloatingActionButton(modifier=Modifier.semantics{contentDescription="Nuevo chat"},onClick={model.showChatPicker=true},icon={Icon(Icons.Outlined.ChatBubbleOutline,null)},text={Text("Nuevo chat")})}
 ) { padding ->
 Column(Modifier.fillMaxSize().padding(padding)){
  Row(Modifier.fillMaxWidth().padding(start=20.dp,top=12.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically){
   Text("Chats",Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
   IconButton(onClick={showNotices=!showNotices}){Icon(Icons.Outlined.Notifications,"Ver avisos")}
   IconButton(onClick=model::loadConversations,enabled=!model.chatBusy){Icon(Icons.Outlined.Refresh,"Actualizar conversaciones")}
  }
  OutlinedTextField(value=search,onValueChange={search=it},placeholder={Text("Buscar chats")},singleLine=true,
   leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp))
  Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   listOf("Todos","Privados","Grupos").forEach{label->FilterChip(selected=filter==label,onClick={filter=label},label={Text(label)})}
  }
  ChatFeedback(model)
  LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=96.dp)){
   if(showNotices) {
    item{Text("Avisos",Modifier.padding(20.dp),style=MaterialTheme.typography.titleLarge)}
    if(model.notices.isEmpty()) item{Text("No hay avisos visibles.",Modifier.padding(20.dp))}
    items(model.notices,key={"notice-"+it.id}){notice->
     ListItem(headlineContent={Text(if(notice.kind=="MESSAGE")"Mensaje aprobado disponible" else "Invitación a grupo")},
      supportingContent={Text(if(notice.read)"Leído" else "Sin leer")},
      trailingContent={if(!notice.read)TextButton(onClick={model.readNotice(notice.id)},enabled=!model.chatBusy){Text("Marcar leído")}})
    }
    item{Row{TextButton(onClick={model.noticePage(-1)},enabled=model.noticePage>0&&!model.chatBusy){Text("Anteriores")};TextButton(onClick={model.noticePage(1)},enabled=model.notices.size==30&&!model.chatBusy){Text("Más avisos")}}}
    item{HorizontalDivider()}
   }
   val visible=model.conversations.filter{it.title.contains(search,true)&&(filter=="Todos"||filter=="Privados"&&it.kind=="DIRECT"||filter=="Grupos"&&it.kind=="GROUP")}
   if(visible.isEmpty()&&!model.chatBusy)item{
    Column(Modifier.fillMaxWidth().padding(36.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
     Icon(Icons.Outlined.Forum,null,Modifier.size(44.dp),tint=MaterialTheme.colorScheme.primary)
     Text(if(search.isNotBlank())"No hay chats que coincidan" else "Tu próxima conversación empieza aquí",style=MaterialTheme.typography.titleMedium)
     Text("Toca Nuevo chat para hablar con un compañero o profesor.",color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
   items(visible,key={it.id}){conversation->
    Row(Modifier.fillMaxWidth().clickable(enabled=!model.chatBusy&&conversation.state!="INVITED"){model.openConversation(conversation.id)}
     .padding(horizontal=20.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically){
     PersonAvatar(conversation.title)
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
      Text(conversation.title,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
      Text(if(conversation.state=="INVITED")"Invitación pendiente" else if(conversation.kind=="GROUP")"Grupo de MAIN" else "Conversación privada",
       style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
      if(conversation.state=="INVITED")Row{
       TextButton(onClick={model.respondInvitation(conversation.id,true)},enabled=!model.chatBusy){Text("Aceptar")}
       TextButton(onClick={model.respondInvitation(conversation.id,false)},enabled=!model.chatBusy){Text("Rechazar")}
      }
     }
     if(conversation.kind=="GROUP")Icon(Icons.Outlined.Groups,"Grupo",tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(Modifier.padding(start=86.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.4f))
   }
   item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){
    if(model.conversationPage>0)TextButton(onClick={model.conversationPage(-1)},enabled=!model.chatBusy){Text("Página anterior")}
    if(model.conversations.size==30)TextButton(onClick={model.conversationPage(1)},enabled=!model.chatBusy){Text("Más conversaciones")}
   }}
  }
 }
 }
}
@Composable internal fun NewChatScreen(model:MainViewModel){
 var search by rememberSaveable{mutableStateOf("")}
 var filter by rememberSaveable{mutableStateOf("Todos")}
 var createGroup by rememberSaveable{mutableStateOf(false)}
 var title by rememberSaveable{mutableStateOf("")}
 Column(Modifier.fillMaxSize()){
  PageHeading("Nuevo chat"){model.showChatPicker=false}
  OutlinedTextField(value=search,onValueChange={search=it},placeholder={Text("Buscar estudiante o profesor")},singleLine=true,
   leadingIcon={Icon(Icons.Outlined.Search,null)},shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth().padding(16.dp))
  Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   listOf("Todos","Estudiantes","Profesores").forEach {label->FilterChip(selected=filter==label,onClick={filter=label},label={Text(label)})}
  }
  SettingsRow(Icons.Outlined.GroupAdd,"Nuevo grupo","Organiza una conversación de equipo",onClick={createGroup=true})
  HorizontalDivider()
  LazyColumn {
   item{Text("Contactos de MAIN",Modifier.padding(horizontal=20.dp,vertical=12.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
   val visible=model.contacts.filter{(it.name.contains(search,true)||it.username.contains(search,true))&&(filter=="Todos"||filter=="Estudiantes"&&it.role=="ALUMNO"||filter=="Profesores"&&it.role=="DOCENTE")}
   if(visible.isEmpty()) item{Text("No hay contactos disponibles. La institución debe vincular tus contactos de MAIN.",Modifier.padding(20.dp))}
   items(visible,key={it.username}){contact->ContactRow(contact){
    if(contact.blocked)model.contactProfile=contact.username
    else if(!model.chatBusy){model.showChatPicker=false;model.startDirect(contact.username)}
   }}
  }
 }
 if(createGroup)AlertDialog(onDismissRequest={createGroup=false},title={Text("Nuevo grupo")},
  text={OutlinedTextField(value=title,onValueChange={title=it.take(100)},label={Text("Nombre del grupo")})},
  confirmButton={TextButton(enabled=title.isNotBlank()&&!model.chatBusy,onClick={model.showChatPicker=false;model.createGroup(title);createGroup=false}){Text("Crear grupo")}},
  dismissButton={TextButton(onClick={createGroup=false}){Text("Cancelar")}})
}
@Composable private fun ChatFeedback(model:MainViewModel){
 if(model.chatBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
 if(model.chatError.isNotBlank())Text(model.chatError,Modifier.padding(horizontal=16.dp),color=MaterialTheme.colorScheme.error)
}
@Composable internal fun ConversationScreen(model:MainViewModel){
 val id=model.selectedConversation
 var mediaViewer by remember(id,model.profile?.usuario){mutableStateOf<ChatMessage?>(null)}
 val owner=LocalLifecycleOwner.current
 DisposableEffect(owner,id){
  val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_START)model.startMessageUpdates();if(event==Lifecycle.Event.ON_STOP)model.stopMessageUpdates()}
  owner.lifecycle.addObserver(observer)
  if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))model.startMessageUpdates()
  onDispose{owner.lifecycle.removeObserver(observer);model.stopMessageUpdates()}
 }
 var manage by rememberSaveable{mutableStateOf(false)}
 var menu by rememberSaveable{mutableStateOf(false)}
 val conversation=model.conversations.firstOrNull{it.id==id}
 val other=model.chatMembers.firstOrNull{it.username!=model.profile?.usuario}
 val contact=model.contacts.firstOrNull{it.username==other?.username}
 Column(Modifier.fillMaxSize().imePadding()){
  Surface(shadowElevation=1.dp){
   Row(Modifier.fillMaxWidth().padding(end=4.dp),verticalAlignment=Alignment.CenterVertically){
    IconButton(onClick=model::closeConversation){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Volver a chats")}
    Row(Modifier.weight(1f).clickable{if(conversation?.kind=="GROUP")manage=true else if(contact!=null)model.contactProfile=contact.username}.padding(vertical=10.dp),
     horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
     PersonAvatar(conversation?.title?:"Chat")
     Column{Text(conversation?.title?:"Conversación",style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
      Text(if(conversation?.kind=="GROUP")"Toca para ver miembros" else "Ver información del contacto",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
    Box{
     IconButton(onClick={menu=true}){Icon(Icons.Outlined.MoreVert,"Opciones del chat")}
     DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
      DropdownMenuItem(text={Text("Actualizar")},onClick={model.refreshMessages();menu=false})
      if(conversation?.kind=="GROUP")DropdownMenuItem(text={Text("Miembros del grupo")},onClick={manage=true;menu=false})
      else if(contact!=null)DropdownMenuItem(text={Text("Ver contacto")},onClick={model.contactProfile=contact.username;menu=false})
     }
    }
   }
  }
  ChatFeedback(model)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){
   if(model.chatMessages.size==30)TextButton(onClick={model.messagePage(1)},enabled=!model.chatBusy){Text("Mensajes anteriores")}
   if(model.chatPage>0)TextButton(onClick={model.messagePage(-1)},enabled=!model.chatBusy){Text("Más recientes")}
  }
  LazyColumn(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)),reverseLayout=true,contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   if(model.chatMessages.isEmpty()&&!model.chatBusy)item{Text("Los mensajes se revisan antes de entregarse. Saluda para empezar.",Modifier.padding(24.dp),style=MaterialTheme.typography.bodySmall)}
   items(model.chatMessages.reversed(),key={it.id}){message->
    val mine=message.sender==model.profile?.usuario
    var actions by remember(message.id){mutableStateOf(false)}
    Column(Modifier.fillMaxWidth(),horizontalAlignment=if(mine)Alignment.End else Alignment.Start){
     Surface(Modifier.widthIn(max=300.dp),shape=RoundedCornerShape(topStart=16.dp,topEnd=16.dp,bottomStart=if(mine)16.dp else 3.dp,bottomEnd=if(mine)3.dp else 16.dp),
      color=if(mine)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface){
      Column(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
       if(!mine&&conversation?.kind=="GROUP")Text(message.senderName,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
       Text(message.text)
       if(message.attachmentKind!="NONE"){
        TextButton(enabled=message.state!="CANCELLED"&&!model.chatBusy,onClick={mediaViewer=message}){Text(attachmentLabel(message.attachmentKind))}
        Text((message.attachmentSize/1024).toString()+" KB",style=MaterialTheme.typography.labelSmall)
       }
       Row(verticalAlignment=Alignment.CenterVertically){
        Text(when(message.state){"PENDING"->"Pendiente de revisión";"REJECTED"->"No enviado · revisar";"CANCELLED"->"Cancelado";else->"Entregado"},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(mine&&message.state!="CANCELLED")Box{
         IconButton(onClick={actions=true},modifier=Modifier.size(30.dp)){Icon(Icons.Outlined.MoreHoriz,"Opciones del mensaje",Modifier.size(18.dp))}
         DropdownMenu(expanded=actions,onDismissRequest={actions=false}){
          DropdownMenuItem(text={Text("Editar mensaje")},onClick={model.editMessage(message);actions=false})
          if(message.state!="APPROVED")DropdownMenuItem(text={Text("Cancelar envío")},onClick={model.cancelMessage(message);actions=false})
         }
        }
       }
       if(message.reason.isNotBlank())Text(message.reason,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
       if(mine&&message.state=="REJECTED")TextButton(onClick={model.editMessage(message)},enabled=!model.chatBusy){Text("Corregir mensaje")}
       if(mine&&message.state=="REJECTED"&&message.attachmentKind!="NONE")Text("Para sustituir el archivo, cancela este envío y adjunta uno nuevo.",style=MaterialTheme.typography.bodySmall)
       if(message.correctedBeforeDelivery)Text("Editado antes del envío para cumplir las normas de MAIN",style=MaterialTheme.typography.labelSmall)
       if(message.editedAfterDelivery)Text("Editado después del envío",style=MaterialTheme.typography.labelSmall)
      }
     }
    }
   }
  }
  if(contact?.blocked==true)Text("Has bloqueado este contacto. Toca su nombre para desbloquearlo.",Modifier.padding(16.dp),color=MaterialTheme.colorScheme.error)
  if(model.editingMessage!=null)Row(verticalAlignment=Alignment.CenterVertically){Text("Editando mensaje",Modifier.weight(1f).padding(start=16.dp));TextButton(onClick=model::cancelEdit){Text("Descartar edición")}}
  if(model.editingMessage==null)MessageAttachmentPicker(model,!model.chatBusy&&contact?.blocked!=true)
  if(model.sendingMessage&&model.messageAttachment!=null){
   LinearProgressIndicator(progress={model.messageUploadProgress/100f},modifier=Modifier.fillMaxWidth())
   Row(verticalAlignment=Alignment.CenterVertically){Text(if(model.messageUploadProgress<100)"Subiendo archivo: ${model.messageUploadProgress}%" else "Esperando confirmación del servidor…",Modifier.weight(1f).padding(start=12.dp));TextButton(onClick=model::cancelMessageUpload){Text("Cancelar espera")}}
  }
  Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedTextField(value=model.messageDraft,onValueChange=model::editDraft,placeholder={Text("Escribe un mensaje")},shape=RoundedCornerShape(26.dp),
    enabled=!model.sendingMessage&&contact?.blocked!=true,modifier=Modifier.weight(1f).semantics{contentDescription="Escribe un mensaje"},maxLines=4)
   FilledIconButton(onClick=model::sendMessage,enabled=(model.messageDraft.isNotBlank()||model.messageAttachment!=null||model.editingMessage?.let{it.attachmentKind!="NONE"}==true)&&!model.chatBusy&&contact?.blocked!=true,modifier=Modifier.size(56.dp)){
    Icon(Icons.AutoMirrored.Outlined.Send,if(model.editingMessage!=null)"Enviar corrección a revisión" else if(model.messageAttachment!=null)"Enviar archivo a revisión" else "Enviar a revisión")
   }
  }
 }
 mediaViewer?.let{message->key(message.id,message.revision){MessageAttachmentViewer(model,message){mediaViewer=null}}}
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
