package pe.tecsup.main.mobile
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

@Composable internal fun PushSettings(model:MainViewModel){
 val context=LocalContext.current.applicationContext
 var active by remember{mutableStateOf(PushLocal.binding(context)!=null)}
 var feedback by remember{mutableStateOf("")}
 fun enable(){
  if(!model.preferences.notifications){feedback="Activa primero Avisos en MAIN.";return}
  if(FirebaseApp.initializeApp(context)==null){feedback="Falta configurar Firebase para esta versión de la app.";return}
  val account=model.profile?.usuario
  FirebaseMessaging.getInstance().token.addOnCompleteListener{result->
   if(!result.isSuccessful){feedback="No se pudo registrar el teléfono con Firebase. Intenta de nuevo.";return@addOnCompleteListener}
   model.registerPush(result.result){binding,error->
    if(binding!=null&&account==model.profile?.usuario){PushLocal.save(context,binding);active=true;feedback="Avisos del teléfono activados para esta sesión."}
    else feedback=error?:"La sesión cambió. Vuelve a activar los avisos."
   }
  }
 }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)enable()else feedback="Permiso denegado. Los avisos siguen disponibles dentro de MAIN."}
 LaunchedEffect(model.profile?.usuario,model.preferences.notifications){if(model.profile==null||!model.preferences.notifications){PushLocal.clear(context);active=false}}
 Text("Avisos en este teléfono")
 Text("Opcionales para la sesión actual. El proveedor recibe un aviso genérico, sin contenido del chat.",style=MaterialTheme.typography.bodySmall)
 if(active)TextButton(onClick={PushLocal.clear(context);active=false;model.disablePush();feedback="Avisos del teléfono desactivados."}){Text("Desactivar avisos del teléfono")}
 else TextButton(onClick={
  if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS) else enable()
 },enabled=!model.busy){Text("Activar avisos del teléfono")}
 if(feedback.isNotBlank())Text(feedback)
}
