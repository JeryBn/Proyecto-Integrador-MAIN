package pe.tecsup.main.mobile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.tecsup.main.mobile.data.*

internal fun attachmentLabel(kind:String)=when(kind){"IMAGE"->"Ver foto";"VIDEO"->"Reproducir video";"AUDIO"->"Escuchar audio";else->"Abrir documento TXT"}

@Composable internal fun MessageAttachmentPicker(model:MainViewModel,enabled:Boolean){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var error by remember{mutableStateOf("")};var reading by remember{mutableStateOf(false)}
 var account by remember{mutableStateOf<String?>(null)};var conversation by remember{mutableStateOf<String?>(null)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null){val expectedAccount=account;val expectedConversation=conversation
   scope.launch{reading=true;error=""
    try{
     val value=withContext(Dispatchers.IO){
      val mime=context.contentResolver.getType(uri).orEmpty()
      if(mime !in listOf("image/jpeg","image/png","text/plain")&&!mime.startsWith("video/")&&!mime.startsWith("audio/"))throw ApiFailure("Elige una foto JPG/PNG, video, audio o TXT UTF-8.")
      val bytes=context.contentResolver.openInputStream(uri)?.use{readStatusBytes(it)}?:throw ApiFailure("No se pudo leer el archivo.")
      if(mime=="text/plain"&&bytes.size>1024*1024)throw ApiFailure("Documento TXT de hasta 1 MB.")
      StatusAttachment(bytes,mime)
     }
     withContext(Dispatchers.Main.immediate){if(expectedAccount==model.profile?.usuario&&expectedConversation==model.selectedConversation)model.updateMessageAttachment(value)}
    }catch(e:CancellationException){throw e}catch(e:Exception){withContext(Dispatchers.Main.immediate){error=e.message?:"No se pudo abrir el archivo."}}finally{reading=false}
   }
  }
 }
 Column(Modifier.fillMaxWidth().padding(horizontal=12.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){
   TextButton(enabled=enabled&&!reading,onClick={
    account=model.profile?.usuario;conversation=model.selectedConversation
    if(BuildConfig.DEBUG)android.util.Log.d("MAIN_MEDIA","Opening Android document picker")
    picker.launch(arrayOf("image/jpeg","image/png","video/*","audio/*","text/plain"))
   }){
    Icon(Icons.Outlined.AttachFile,null);Text("Adjuntar archivo")
   }
   val attachment=model.messageAttachment
   if(attachment!=null){Text(when{attachment.mime.startsWith("image/")->"Foto seleccionada";attachment.mime.startsWith("video/")->"Video seleccionado";attachment.mime.startsWith("audio/")->"Audio seleccionado";else->"TXT seleccionado"},Modifier.weight(1f));TextButton(enabled=enabled,onClick={model.updateMessageAttachment(null)}){Text("Quitar")}}
  }
  if(reading)LinearProgressIndicator(Modifier.fillMaxWidth())
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
  if(model.messageAttachment!=null)Text("Hasta 12 MB; video y audio hasta 60 s. TXT hasta 1 MB. Se revisará antes de entregarlo.",style=MaterialTheme.typography.bodySmall)
 }
}

@Composable internal fun MessageAttachmentViewer(model:MainViewModel,message:ChatMessage,onClose:()->Unit){
 val account=model.profile?.usuario;val context=LocalContext.current;val scope=rememberCoroutineScope()
 var error by remember(message.id,message.revision){mutableStateOf("")}
 var bytes by remember(message.id,message.revision,account){mutableStateOf<ByteArray?>(null)}
 var retry by remember(message.id,message.revision){mutableIntStateOf(0)}
 LaunchedEffect(message.id,message.revision,account,retry){
  bytes=null;error=""
  if(BuildConfig.DEBUG)android.util.Log.d("MAIN_MEDIA","Message attachment loading")
  try{
   val downloaded=model.messageMedia(message)
   withContext(Dispatchers.Main.immediate){bytes=downloaded}
   if(BuildConfig.DEBUG)android.util.Log.d("MAIN_MEDIA","Message attachment ready")
  }
  catch(e:CancellationException){if(BuildConfig.DEBUG)android.util.Log.d("MAIN_MEDIA","Message attachment cancelled");throw e}
  catch(e:Exception){withContext(Dispatchers.Main.immediate){error=e.message?.takeIf{it.isNotBlank()}?:"No se pudo abrir el adjunto."};if(BuildConfig.DEBUG)android.util.Log.d("MAIN_MEDIA","Message attachment failed")}
 }
 var paused by remember(message.id){mutableStateOf(false)};var progress by remember(message.id){mutableFloatStateOf(0f)}
 val owner=LocalLifecycleOwner.current
 DisposableEffect(owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE)paused=true};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
 LaunchedEffect(model.chatMessages,model.profile){
  if(model.profile?.usuario!=account||model.chatMessages.none{it.id==message.id&&it.revision==message.revision})onClose()
 }
 val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->
  val content=bytes
  if(uri!=null&&content!=null&&model.profile?.usuario==account)scope.launch{
   try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.use{it.write(content)}?:throw ApiFailure("No se pudo guardar el documento.")}}
   catch(e:Exception){error="No se pudo guardar el documento."}
  }
 }
 Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(message.senderName,Modifier.weight(1f),color=Color.White);TextButton(onClick=onClose){Text("Cerrar adjunto",color=Color.White)}}
   if(message.text.isNotBlank())Text(message.text,color=Color.White)
   Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
    val content=bytes
    if(content==null&&error.isBlank())Column(horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator(color=Color.White);Text("Cargando adjunto…",color=Color.White)}
    else if(content!=null)when(message.attachmentKind){
     "IMAGE"->{val bitmap=remember(content){statusBitmap(content)};if(bitmap!=null)Image(bitmap.asImageBitmap(),"Foto del mensaje",Modifier.fillMaxSize())else Text("No se pudo abrir la foto.",color=Color.White)}
     "FILE"->Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Text(String(content,Charsets.UTF_8).take(6000),color=Color.White);if(content.size>6000)Text("Vista previa limitada. Guarda el TXT para leer el archivo completo.",color=Color.White)}
     else->key(message.id,message.revision){StatusPlayback(message.id,message.attachmentKind,content,paused,onProgress={progress=it},onComplete={paused=true;progress=1f},label="mensaje")}
    }
   }
   if(error.isNotBlank()){Text(error,color=Color.White);TextButton(onClick={retry++}){Text("Reintentar descarga",color=Color.White)}}
   if(bytes!=null&&message.attachmentKind in listOf("VIDEO","AUDIO")){
    LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth(),color=Color.White)
    TextButton(onClick={paused=!paused}){Text(if(paused)"Reproducir" else "Pausar",color=Color.White)}
   }
   if(bytes!=null&&message.attachmentKind=="FILE")TextButton(onClick={save.launch("MAIN-documento.txt")}){Text("Guardar documento TXT",color=Color.White)}
  }
 }
}
