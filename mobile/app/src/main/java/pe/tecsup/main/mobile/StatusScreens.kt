package pe.tecsup.main.mobile
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.tecsup.main.mobile.data.*
import java.io.File
import java.time.Instant
import java.util.UUID

private fun statusLabel(state:String)=when(state){"PENDING"->"Pendiente de revisión · solo tú puedes verlo";"REJECTED"->"Rechazado · solo tú puedes verlo";else->"Disponible durante 24 horas"}
@Composable internal fun StatusListScreen(model:MainViewModel){
 val own=model.statuses.filter{it.owner==model.profile?.usuario};val other=model.statuses.filter{it.owner!=model.profile?.usuario}
 Scaffold(contentWindowInsets=WindowInsets(0,0,0,0),floatingActionButton={if(model.profile?.rol in listOf("Alumno","Docente"))Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  SmallFloatingActionButton(onClick={model.statusComposer=true},containerColor=MaterialTheme.colorScheme.secondaryContainer){Icon(Icons.Outlined.Edit,"Crear estado de texto")}
  FloatingActionButton(onClick={model.statusComposer=true},containerColor=MaterialTheme.colorScheme.primary){Icon(Icons.Outlined.AddPhotoAlternate,"Crear estado multimedia",tint=Color.White)}
 }}){padding->
 LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Estados",style=MaterialTheme.typography.headlineLarge);IconButton(onClick=model::loadStatuses,enabled=!model.statusBusy){Icon(Icons.Outlined.Refresh,"Actualizar estados")}}}
  item{if(model.statusBusy)LinearProgressIndicator(Modifier.fillMaxWidth());if(model.statusError.isNotBlank())Text(model.statusError,color=MaterialTheme.colorScheme.error)}
  if(model.profile?.rol in listOf("Alumno","Docente"))item{
   Row(Modifier.fillMaxWidth().clickable(enabled=!model.statusBusy){if(own.isEmpty())model.statusComposer=true else model.openStatus(own.first())},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
    Box{PersonAvatar(model.profile?.nombre.orEmpty());Icon(Icons.Outlined.AddCircle,null,Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface,CircleShape),tint=MaterialTheme.colorScheme.primary)}
    Column{Text("Mi estado",style=MaterialTheme.typography.titleMedium);Text(if(own.isEmpty())"Añade una actualización" else if(own.size==1)"1 actualización" else own.size.toString()+" actualizaciones",style=MaterialTheme.typography.bodyMedium)}
   }
  }
  if(own.isNotEmpty())item{Text("Mis actualizaciones",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
  items(own,key={"own:"+it.id}){StatusRow(it,model)}
  item{Text("Actualizaciones recientes",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}
  if(other.isEmpty())item{Text("Tus contactos aún no tienen estados disponibles.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  items(other.groupBy{it.owner}.values.filter{updates->updates.any{!it.seen}}.map{updates->updates.filter{!it.seen}.minBy{it.createdAt}},key={"contact:"+it.owner}){StatusRow(it,model)}
  if(other.any{it.seen})item{Text("Vistos",style=MaterialTheme.typography.labelLarge)}
  items(other.groupBy{it.owner}.values.filter{updates->updates.all{it.seen}}.map{updates->updates.minBy{it.createdAt}},key={"contact:"+it.owner}){StatusRow(it,model)}
  item{Text("Los estados duran 24 horas. Solo se muestran a su audiencia después de la revisión.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
 }
 }
}
@Composable private fun StatusRow(value:StatusUpdate,model:MainViewModel){
 Row(Modifier.fillMaxWidth().clickable(enabled=!model.statusBusy){model.openStatus(value)}.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
  Box(Modifier.border(2.dp,if(value.seen)MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,CircleShape).padding(4.dp)){PersonAvatar(value.name)}
  Column{Text(if(value.owner==model.profile?.usuario)"Mi estado" else value.name,style=MaterialTheme.typography.titleMedium)
   Text(if(value.owner==model.profile?.usuario)statusLabel(value.state) else "Toca para ver",style=MaterialTheme.typography.bodySmall)
   if(value.owner==model.profile?.usuario)Text(when(value.kind){"TEXT"->value.text.take(45);"IMAGE"->"Foto";"VIDEO"->"Video";else->"Audio"},style=MaterialTheme.typography.bodySmall)
  }
 }
}
@Composable internal fun StatusComposerScreen(model:MainViewModel){
 var text by rememberSaveable{mutableStateOf("")};var color by rememberSaveable{mutableStateOf("#087F68")}
 var audience by rememberSaveable{mutableStateOf(model.preferences.statusAudience)}
 var attachment by remember{mutableStateOf<StatusAttachment?>(null)};var error by remember{mutableStateOf("")};var reading by remember{mutableStateOf(false)}
 var key by rememberSaveable{mutableStateOf(UUID.randomUUID().toString())}
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var captureFile by remember{mutableStateOf<File?>(null)}
 fun readAttachment(uri:android.net.Uri,mimeOverride:String?=null){reading=true;error="";scope.launch{
  try{attachment=withContext(Dispatchers.IO){val mime=mimeOverride?:context.contentResolver.getType(uri).orEmpty();if(mime !in listOf("image/jpeg","image/png") && !mime.startsWith("video/") && !mime.startsWith("audio/"))throw Exception("Elige JPG, PNG, video o audio.");val bytes=context.contentResolver.openInputStream(uri)?.use{readStatusBytes(it)}?:throw Exception("No se pudo leer el archivo.");if(bytes.size>12*1024*1024)throw Exception("El archivo debe pesar hasta 12 MB.");StatusAttachment(bytes,mime)};key=UUID.randomUUID().toString()}
  catch(e:Exception){error=e.message?:"No se pudo abrir el archivo."}finally{reading=false;captureFile?.delete();captureFile=null}
 }}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)readAttachment(uri)}
 val photo=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){success->val file=captureFile;if(success && file!=null)readAttachment(androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".statusfiles",file),"image/jpeg")else{file?.delete();captureFile=null}}
 val video=rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()){success->val file=captureFile;if(success && file!=null)readAttachment(androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".statusfiles",file),"video/mp4")else{file?.delete();captureFile=null}}
 fun capture(record:Boolean){
  try{val folder=File(context.cacheDir,"status-captures").also{it.mkdirs()};val file=File.createTempFile("capture-",if(record)".mp4" else ".jpg",folder);captureFile=file
   val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".statusfiles",file);if(record)video.launch(uri)else photo.launch(uri)
  }catch(e:Exception){captureFile?.delete();captureFile=null;error="No hay una cámara disponible para esta acción."}
 }
 DisposableEffect(Unit){onDispose{captureFile?.delete()}}

 Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  PageHeading("Crear estado"){if(!model.statusBusy)model.statusComposer=false}
  Box(Modifier.fillMaxWidth().height(240.dp).background(Color(android.graphics.Color.parseColor(color))),contentAlignment=Alignment.Center){
   if(attachment?.mime?.startsWith("image/")==true){val bitmap=remember(attachment){statusBitmap(attachment!!.bytes)};if(bitmap!=null)Image(bitmap.asImageBitmap(),"Vista previa",Modifier.fillMaxSize())}
   else Text(if(attachment==null)text.ifBlank{"Escribe una actualización"} else if(attachment!!.mime.startsWith("video/"))"Video seleccionado" else "Audio seleccionado",Modifier.padding(20.dp),color=Color.White,textAlign=TextAlign.Center,style=MaterialTheme.typography.headlineSmall)
  }
  OutlinedTextField(value=text,onValueChange={text=it.take(700);key=UUID.randomUUID().toString()},label={Text(if(attachment==null)"Escribe tu estado" else "Añade un comentario")},modifier=Modifier.fillMaxWidth(),enabled=!model.statusBusy)
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf("#087F68","#685AA4","#A34154","#34495E").forEach{shade->Box(Modifier.size(36.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(shade))).border(if(color==shade)3.dp else 0.dp,MaterialTheme.colorScheme.onSurface,CircleShape).clickable(enabled=!model.statusBusy){color=shade;key=UUID.randomUUID().toString()})}}
  OutlinedButton(enabled=!model.statusBusy && !reading,onClick={picker.launch(arrayOf("image/jpeg","image/png","video/*","audio/*"))}){Icon(Icons.Outlined.AttachFile,null);Text("Añadir foto, video o audio")}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedButton(enabled=!model.statusBusy && !reading,onClick={capture(false)}){Icon(Icons.Outlined.CameraAlt,null);Text("Foto")}
   OutlinedButton(enabled=!model.statusBusy && !reading,onClick={capture(true)}){Icon(Icons.Outlined.Videocam,null);Text("Grabar video")}
  }
  if(attachment!=null)TextButton(enabled=!model.statusBusy,onClick={attachment=null;key=UUID.randomUUID().toString()}){Text("Quitar archivo")}
  Text("Privacidad del estado",style=MaterialTheme.typography.titleMedium)
  Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=audience=="CONTACTS",enabled=!model.statusBusy,onClick={audience="CONTACTS";key=UUID.randomUUID().toString()});Text("Mis contactos")}
  Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=audience=="PRIVATE",enabled=!model.statusBusy,onClick={audience="PRIVATE";key=UUID.randomUUID().toString()});Text("Solo yo")}
  Text("Se revisará antes de compartirlo. Caduca 24 horas después de enviarlo. Multimedia: hasta 12 MB; video y audio: hasta 60 segundos.",style=MaterialTheme.typography.bodySmall)
  if(model.preferences.statusAudience=="PRIVATE" && audience=="CONTACTS")Text("Tu privacidad general está en Solo yo: seguirá privado hasta que cambies ese ajuste.")
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
  if(model.statusError.isNotBlank())Text(model.statusError,color=MaterialTheme.colorScheme.error)
  if(reading||model.statusBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
  Button(onClick={model.createStatus(key,text,color,audience,attachment)},enabled=!model.statusBusy && !reading && (text.isNotBlank()||attachment!=null),modifier=Modifier.fillMaxWidth()){Text(if(model.statusBusy)"Enviando a revisión…" else "Enviar estado")}
 }
}
@Composable internal fun StatusViewerScreen(model:MainViewModel){
 val status=model.statusViewer?:return
 var progress by remember(status.id){mutableFloatStateOf(0f)};var paused by remember(status.id){mutableStateOf(false)};var confirmDelete by remember{mutableStateOf(false)}
 val lifecycleOwner=LocalLifecycleOwner.current
 DisposableEffect(lifecycleOwner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE)paused=true else if(event==Lifecycle.Event.ON_RESUME)paused=false};lifecycleOwner.lifecycle.addObserver(observer);onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}}
 val playlist=model.statuses.filter{it.owner==status.owner}.sortedBy{it.createdAt};val index=playlist.indexOfFirst{it.id==status.id}
 fun next(){if(model.statusBusy)return;if(index>=0 && index+1<playlist.size)model.openStatus(playlist[index+1]) else model.closeStatus()}
 LaunchedEffect(status.id,model.statusBusy,paused,model.statusError){if(model.statusBusy||model.statusError.isNotBlank())return@LaunchedEffect
  while(true){delay(100);if(Instant.parse(status.expiresAt).isBefore(Instant.now())){model.closeStatus();return@LaunchedEffect};if(!paused && status.kind in listOf("TEXT","IMAGE")){progress+=0.02f;if(progress>=1f){next();return@LaunchedEffect}}}
 }
 Box(Modifier.fillMaxSize().background(if(status.kind=="TEXT")Color(android.graphics.Color.parseColor(status.background)) else Color.Black)){
  Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){playlist.forEachIndexed{i,_->LinearProgressIndicator(progress={if(i<index)1f else if(i==index)progress else 0f},modifier=Modifier.weight(1f),color=Color.White,trackColor=Color.White.copy(alpha=.3f))}}
   Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick=model::closeStatus){Text("Cerrar",color=Color.White)};Text(status.name,Modifier.weight(1f),color=Color.White,style=MaterialTheme.typography.titleMedium);if(status.owner==model.profile?.usuario)IconButton(enabled=!model.statusBusy,onClick={confirmDelete=true}){Icon(Icons.Outlined.Delete,"Eliminar estado",tint=Color.White)}}
   if(model.statusBusy)LinearProgressIndicator(Modifier.fillMaxWidth())
   if(model.statusError.isNotBlank()){Text(model.statusError,color=Color.White);TextButton(onClick={model.openStatus(status)}){Text("Reintentar",color=Color.White)}}
   Box(Modifier.weight(1f).fillMaxWidth().pointerInput(status.id,model.statusBusy){detectTapGestures(onPress={paused=true;tryAwaitRelease();paused=false},onTap={offset->if(offset.x<size.width/3 && index>0)model.openStatus(playlist[index-1]) else next()})},contentAlignment=Alignment.Center){
    when(status.kind){
     "TEXT"->Text(status.text,Modifier.padding(24.dp),style=MaterialTheme.typography.headlineLarge,color=Color.White,textAlign=TextAlign.Center)
     "IMAGE"->{val bytes=model.statusBytes;val bitmap=remember(bytes){bytes?.let{statusBitmap(it)}};if(bitmap!=null)Image(bitmap.asImageBitmap(),"Estado de "+status.name,Modifier.fillMaxSize())}
     else->key(status.id){StatusPlayback(status.id,status.kind,model.statusBytes,paused,onProgress={progress=it},onComplete={progress=1f;next()})}
    }
   }
   if(status.kind!="TEXT" && status.text.isNotBlank())Text(status.text,color=Color.White,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
   if(status.owner==model.profile?.usuario){Text(statusLabel(status.state),color=Color.White);if(status.state=="REJECTED")Text(status.reason,color=Color.White);if(status.state=="APPROVED")Text(status.views.toString()+" visualizaciones",color=Color.White)}
   if(status.owner!=model.profile?.usuario)OutlinedButton(enabled=!model.chatBusy && !model.statusBusy,onClick={model.closeStatus();model.selectedSectionForChat();model.startDirect(status.owner)},modifier=Modifier.fillMaxWidth()){Text("Responder por mensaje",color=Color.White)}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(enabled=index>0 && !model.statusBusy,onClick={model.openStatus(playlist[index-1])}){Text("Anterior",color=Color.White)};TextButton(enabled=!model.statusBusy,onClick={next()}){Text("Siguiente",color=Color.White)}}
  }
 }
 if(confirmDelete)AlertDialog(onDismissRequest={confirmDelete=false},title={Text("¿Eliminar estado?")},text={Text("Dejará de estar disponible para tus contactos.")},confirmButton={TextButton(onClick={confirmDelete=false;model.deleteStatus()}){Text("Eliminar")}},dismissButton={TextButton(onClick={confirmDelete=false}){Text("Cancelar")}})
}
@Composable internal fun StatusPlayback(id:String,kind:String,bytes:ByteArray?,paused:Boolean,onProgress:(Float)->Unit,onComplete:()->Unit,label:String="estado"){
 val context=LocalContext.current;val completed by rememberUpdatedState(onComplete)
 var failure by remember(id){mutableStateOf("")}
 val file by produceState<File?>(null,id,bytes){
  if(bytes!=null){val temporary=File.createTempFile("main-status-",if(kind=="VIDEO")".mp4" else ".m4a",context.cacheDir)
   try{withContext(Dispatchers.IO){temporary.writeBytes(bytes)};value=temporary;awaitCancellation()}
   finally{temporary.delete()}
  }
 }
 val ready=file
 if(ready!=null){
  if(kind=="VIDEO"){
   var view by remember(id){mutableStateOf<VideoView?>(null)};var prepared by remember(id){mutableStateOf(false)};var rendered by remember(id){mutableStateOf(false)}
   AndroidView(factory={VideoView(it).apply{setVideoPath(ready.absolutePath);setOnPreparedListener{prepared=true};setOnInfoListener{_,what,_->if(what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)rendered=true;false};setOnCompletionListener{completed()};setOnErrorListener{_,_,_->failure="No se pudo reproducir el video.";true};start();view=this}},modifier=Modifier.fillMaxSize().semantics{if(prepared && rendered && failure.isBlank())contentDescription="Video del $label"},update={if(paused)it.pause() else it.start()})
   LaunchedEffect(view){while(true){delay(100);view?.let{video->runCatching{if(video.duration>0)onProgress(video.currentPosition.toFloat()/video.duration)}}}}
   DisposableEffect(id){onDispose{view?.stopPlayback()}}
  }else{
   val player=remember(ready){MediaPlayer()};var prepared by remember(ready){mutableStateOf(false)}
   DisposableEffect(player){
    try{player.setDataSource(ready.absolutePath);player.setOnCompletionListener{completed()};player.setOnErrorListener{_,_,_->failure="No se pudo reproducir el audio.";true};player.setOnPreparedListener{prepared=true};player.prepareAsync()}
    catch(error:Exception){failure="No se pudo reproducir el audio."}
    onDispose{player.release()}
   }
   LaunchedEffect(paused,prepared){if(prepared)runCatching{if(paused)player.pause() else player.start()}.onFailure{failure="No se pudo reproducir el audio."}}
   LaunchedEffect(player,prepared){if(prepared)while(true){delay(100);runCatching{if(player.duration>0)onProgress(player.currentPosition.toFloat()/player.duration)}}}
   if(prepared && failure.isBlank())Icon(Icons.Outlined.GraphicEq,"Reproduciendo audio",Modifier.size(100.dp),tint=Color.White) else if(failure.isBlank())CircularProgressIndicator(color=Color.White)
  }
 }
 if(failure.isNotBlank())Text(failure,color=Color.White)
}

internal fun statusBitmap(bytes:ByteArray):android.graphics.Bitmap? {
 val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
 var sample=1;while(bounds.outWidth/sample>1600||bounds.outHeight/sample>1600)sample*=2
 return BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample})
}

internal fun readStatusBytes(input:java.io.InputStream):ByteArray {
 val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0
 while(true){val count=input.read(buffer);if(count<0)break;total+=count;if(total>12*1024*1024)throw java.io.IOException("El archivo debe pesar hasta 12 MB.");output.write(buffer,0,count)}
 return output.toByteArray()
}
