package pe.tecsup.main.mobile.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class Profile(val usuario: String, val nombre: String, val rol: String)
data class Notice(val id:String,val kind:String,val conversationId:String,val read:Boolean)
data class Activity(
    val id: Long, val titulo: String, val descripcion: String, val curso: String,
    val docente: String, val fechaLimite: String, val vencida: Boolean, val completada: Boolean
)
class SessionExpired : IOException("Tu sesión finalizó. Ingresa nuevamente.")
class ActivityUnavailable : IOException("La actividad no existe o no está disponible para tu cuenta.")
class InvalidCredentials : IOException("Usuario o contraseña incorrectos.")
class ApiFailure(message: String) : IOException(message)
class ResourceUnavailable : IOException("El recurso ya no está disponible para tu cuenta.")

/** One cookie jar per login. No cookies or passwords are written to storage. */
class MainApi(val endpoint: String) {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    private data class Response(val status: Int, val body: String, val location: String?, val path: String)

    private fun request(path: String, form: Map<String, String>? = null, payload: JSONObject? = null,
                        method: String = "GET", headers: Map<String,String> = emptyMap()): Response {
        val uri = URI(endpoint + path)
        val connection = uri.toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = if(path=="/api/v1/assistant") 55_000 else 8_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            headers.forEach { (name,value) -> connection.setRequestProperty(name,value) }
            cookies.get(uri, emptyMap()).forEach { (name, values) ->
                connection.setRequestProperty(name, values.joinToString("; "))
            }
            if (form != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                val body = form.entries.joinToString("&") {
                    URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
                }.toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            } else if (payload != null) {
                connection.requestMethod = method
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            cookies.put(uri, connection.headerFields.filterKeys { it != null })
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return Response(status, body, connection.getHeaderField("Location"), path)
        } finally {
            connection.disconnect()
        }
    }

    private fun json(path: String): JSONObject = JSONObject(checked(request(path)))
    private fun checked(response: Response): String = when (response.status) {
        in 200..299 -> response.body
        401 -> { clearSession(); throw SessionExpired() }
        404 -> if(response.path.startsWith("/api/v1/")) throw ResourceUnavailable() else throw ActivityUnavailable()
        403 -> throw ApiFailure("No tienes permiso para realizar esta operación.")
        else -> throw ApiFailure(runCatching { JSONObject(response.body).getJSONObject("error").getString("message") }
            .getOrDefault("El servidor no respondió correctamente. Intenta nuevamente."))
    }

    fun login(username: String, password: String): Profile {
        clearSession()
        try {
            val csrf = json("/login-token")
            val response = request("/login", mapOf(
                "username" to username.trim(), "password" to password,
                csrf.getString("parameterName") to csrf.getString("token")
            ))
            val destination = response.location?.let { URI(endpoint).resolve(it) }
            if (response.status !in listOf(302, 303) || destination?.path != "/" || destination.query != null) {
                if (response.status == 403) throw ApiFailure("No se pudo validar el acceso. Intenta nuevamente.")
                throw InvalidCredentials()
            }
            return profile()
        } catch (error: Exception) {
            clearSession()
            throw error
        }
    }

    fun profile(): Profile = json("/api/perfil").let {
        Profile(it.getString("usuario"), it.getString("nombre"), it.getString("rol"))
    }

    fun pending(): List<Activity> {
        val rows = JSONArray(checked(request("/api/actividades/pendientes")))
        return (0 until rows.length()).map { parseActivity(rows.getJSONObject(it)) }
    }

    fun detail(id: Long): Activity = parseActivity(json("/api/actividades/$id"))

    fun logout() {
        try {
            val token = json("/api/csrf")
            val response = request("/logout", mapOf(token.getString("parameterName") to token.getString("token")))
            if (response.status !in listOf(302, 303)) checked(response)
        } finally {
            clearSession()
        }
    }

    fun clearSession() { cookies.cookieStore.removeAll() }

    fun askAssistant(key:String,question:String):String = mutate("/api/v1/assistant",
        JSONObject().put("clientKey",key).put("question",question),"POST").getString("answer")
    fun notifications(offset:Int=0):List<Notice> = rows(JSONArray(checked(request("/api/v1/notifications?offset="+offset+"&limit=30")))) {
        Notice(it.getString("id"),it.getString("kind"),it.getString("conversationId"),it.getBoolean("read"))
    }
    fun readNotification(id:String) { mutate("/api/v1/notifications/"+id+"/read",JSONObject(),"POST") }

    fun conversations(offset:Int=0): List<Conversation> = rows(JSONArray(checked(request("/api/v1/conversations?offset="+offset+"&limit=30")))) {
        Conversation(it.getString("id"),it.getString("kind"),it.getString("title"),it.getBoolean("administrator"),it.getString("state"))
    }
    fun startDirect(username:String): String = mutate("/api/v1/conversations/direct",JSONObject().put("username",username),"POST").getString("id")
    fun startGroup(title:String): String = mutate("/api/v1/conversations/groups",JSONObject().put("title",title),"POST").getString("id")
    fun invite(conversation:String,username:String) { mutate("/api/v1/conversations/"+conversation+"/invitations",JSONObject().put("username",username),"POST") }
    fun invitation(conversation:String,accept:Boolean) { mutate("/api/v1/conversations/"+conversation+"/invitation",JSONObject().put("accept",accept),"POST") }
    fun members(conversation:String): List<ConversationMember> = rows(JSONArray(checked(request("/api/v1/conversations/"+conversation+"/members")))) {
        ConversationMember(it.getString("username"),it.getString("name"),it.getString("role"),it.getString("state"),it.getBoolean("administrator"))
    }
    fun membership(conversation:String,username:String,administrator:Boolean,remove:Boolean) {
        mutate("/api/v1/conversations/"+conversation+"/members/"+URLEncoder.encode(username,"UTF-8"),
            JSONObject().put("administrator",administrator).put("remove",remove))
    }
    fun messages(conversation:String,offset:Int=0): List<ChatMessage> = rows(JSONArray(checked(request("/api/v1/conversations/"+conversation+"/messages?offset="+offset+"&limit=30")))) {
        ChatMessage(it.getString("id"),it.getString("sender"),it.getString("senderName"),it.getInt("revision"),it.getString("text"),it.getString("state"),
            it.optString("reason",""),it.getBoolean("correctedBeforeDelivery"),it.getBoolean("editedAfterDelivery"))
    }
    fun sendMessage(conversation:String,key:String,text:String) {
        mutate("/api/v1/conversations/"+conversation+"/messages",JSONObject().put("clientKey",key).put("text",text),"POST")
    }
    fun editMessage(id:String,revision:Int,text:String) { mutate("/api/v1/messages/"+id,JSONObject().put("expectedRevision",revision).put("text",text)) }
    fun cancelMessage(id:String,revision:Int) { mutate("/api/v1/messages/"+id+"/cancel",JSONObject().put("expectedRevision",revision),"POST") }

    private fun mutate(path: String, body: JSONObject, method: String = "PUT"): JSONObject {
        val token = json("/api/csrf")
        return JSONObject(checked(request(path, payload=body, method=method,
            headers=mapOf(token.optString("headerName","X-CSRF-TOKEN") to token.getString("token")))))
    }

    fun preferences(): UserPreferences = parsePreferences(json("/api/v1/preferences"))
    fun savePreferences(value: UserPreferences): UserPreferences = parsePreferences(mutate("/api/v1/preferences",
        JSONObject().put("darkTheme",value.darkTheme).put("notifications",value.notifications).put("statusAudience",value.statusAudience)))
    private fun parsePreferences(value: JSONObject) = UserPreferences(value.getBoolean("darkTheme"),value.getBoolean("notifications"),value.getString("statusAudience"))

    fun contacts(): List<ContactUser> = rows(JSONArray(checked(request("/api/v1/contacts")))) {
        ContactUser(it.getString("username"),it.getString("name"),it.getString("role"),it.getBoolean("muted"),it.getBoolean("blocked"))
    }
    fun grades():List<CourseGrades> = rows(JSONArray(checked(request("/api/v1/grades")))) { course ->
        CourseGrades(course.getLong("courseId"),course.getString("course"),course.getString("teacher"),rows(course.getJSONArray("grades")) {
            Grade(it.getString("assessment"),it.get("score").toString().toBigDecimal().stripTrailingZeros().toPlainString(),it.get("maximum").toString().toBigDecimal().stripTrailingZeros().toPlainString())
        })
    }
    fun saveContact(value: ContactUser) {
        mutate("/api/v1/contacts/"+URLEncoder.encode(value.username,"UTF-8")+"/preferences",
            JSONObject().put("muted",value.muted).put("blocked",value.blocked))
    }
    fun reportContact(username: String, reason: String) {
        mutate("/api/v1/contacts/"+URLEncoder.encode(username,"UTF-8")+"/reports",JSONObject().put("reason",reason),"POST")
    }
    fun agenda(): AcademicAgenda = json("/api/v1/agenda").let { value ->
        AcademicAgenda(
            rows(value.getJSONArray("courses")) { Course(it.getLong("id"),it.getString("name"),it.getString("teacher")) },
            rows(value.getJSONArray("timetable")) { TimeSlot(it.getString("id"),it.getString("course"),it.getString("teacher"),
                it.getInt("dayOfWeek"),it.getString("startsAt"),it.getString("endsAt"),it.getString("classroom")) },
            rows(value.getJSONArray("students")) { it.getString("name") }
        )
    }
    private fun <T> rows(array: JSONArray, parse: (JSONObject) -> T): List<T> = (0 until array.length()).map { parse(array.getJSONObject(it)) }

    private fun parseActivity(json: JSONObject) = Activity(
        json.getLong("id"), json.getString("titulo"), json.getString("descripcion"),
        json.getString("curso"), json.getString("docente"), json.getString("fechaLimite"),
        json.getBoolean("vencida"), json.getBoolean("completada")
    )
}
