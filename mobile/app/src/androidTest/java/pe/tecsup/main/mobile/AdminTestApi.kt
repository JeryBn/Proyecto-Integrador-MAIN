package pe.tecsup.main.mobile
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

/** Synthetic credentials for the isolated loopback test server only; never included in the application APK. */
internal class AdminTestApi {
 private val cookies=CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER)
 private val origin="http://127.0.0.1:8088"
 init{
  val csrf=get("/api/admin/csrf")
  val form=mapOf("username" to "android_test_admin","password" to "SyntheticAndroidTest2026!",csrf.getString("parameterName") to csrf.getString("token"))
  exchange("/api/admin/login","POST",form.entries.joinToString("&"){URLEncoder.encode(it.key,"UTF-8")+"="+URLEncoder.encode(it.value,"UTF-8")},"application/x-www-form-urlencoded")
 }
 private fun exchange(path:String,method:String="GET",body:String?=null,type:String="application/json",headers:Map<String,String> = emptyMap()):String{
  val uri=URI(origin+path);val connection=uri.toURL().openConnection() as HttpURLConnection
  try{
   connection.connectTimeout=8000;connection.readTimeout=8000;connection.instanceFollowRedirects=false;connection.requestMethod=method
   cookies.get(uri,emptyMap()).forEach{(key,values)->connection.setRequestProperty(key,values.joinToString("; "))}
   headers.forEach{(key,value)->connection.setRequestProperty(key,value)}
   if(body!=null){connection.doOutput=true;connection.setRequestProperty("Content-Type",type);connection.outputStream.use{it.write(body.toByteArray(Charsets.UTF_8))}}
   val status=connection.responseCode;cookies.put(uri,connection.headerFields.filterKeys{it!=null})
   val result=(if(status>=400)connection.errorStream else connection.inputStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
   check(status in 200..299){"Synthetic test admin request failed: HTTP "+status}
   return result
  }finally{connection.disconnect()}
 }
 fun get(path:String):JSONObject=JSONObject(exchange(path))
 fun list(path:String):JSONArray=JSONArray(exchange(path))
 fun post(path:String,body:JSONObject):JSONObject{
  val csrf=get("/api/admin/csrf")
  val result=exchange(path,"POST",body.toString(),headers=mapOf(csrf.getString("headerName") to csrf.getString("token")))
  return if(result.isBlank())JSONObject() else JSONObject(result)
 }
 fun review(id:String,approved:Boolean,reason:String="Regla sintética de prueba"){
  val queue=list("/api/admin/v1/moderation/messages?limit=100")
  val message=(0 until queue.length()).map{queue.getJSONObject(it)}.first{it.getString("id")==id}
  post("/api/admin/v1/moderation/messages/"+id+"/review",JSONObject().put("revision",message.getInt("revision"))
   .put("contentHash",message.getString("contentHash")).put("approved",approved).put("reason",reason))
 }
}
