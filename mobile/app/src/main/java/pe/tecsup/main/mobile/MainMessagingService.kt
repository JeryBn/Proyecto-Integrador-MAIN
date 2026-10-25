package pe.tecsup.main.mobile
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

object PushLocal {
 private fun state(context:Context)=context.getSharedPreferences("main_push",Context.MODE_PRIVATE)
 fun binding(context:Context)=state(context).getString("binding",null)
 fun save(context:Context,binding:String){state(context).edit().putString("binding",binding).apply()}
 fun clear(context:Context){state(context).edit().clear().apply();NotificationManagerCompat.from(context).cancelAll()}
}
class MainMessagingService:FirebaseMessagingService(){
 override fun onNewToken(token:String){
  // A rotated token is registered on the next authenticated foreground visit.
  // Never store a session cookie or attempt unauthenticated registration here.
 }
 override fun onMessageReceived(message:RemoteMessage){
  if(!acceptsPush(PushLocal.binding(this),message.data["binding"],message.data["event"]))return
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  val manager=getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("main_updates","Novedades de MAIN",NotificationManager.IMPORTANCE_DEFAULT))
  val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val notice=NotificationCompat.Builder(this,"main_updates").setSmallIcon(pe.tecsup.main.mobile.R.drawable.ic_main)
   .setContentTitle("MAIN").setContentText("Tienes novedades. Abre MAIN para consultarlas.")
   .setVisibility(NotificationCompat.VISIBILITY_SECRET).setContentIntent(open).setAutoCancel(true).build()
  // Stable ID collapses redelivery and never includes a student's name or text.
  try{NotificationManagerCompat.from(this).notify(1001,notice)}catch(_:SecurityException){}
 }
}
