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
data class Activity(
    val id: Long, val titulo: String, val descripcion: String, val curso: String,
    val docente: String, val fechaLimite: String, val vencida: Boolean, val completada: Boolean
)
class SessionExpired : IOException("Tu sesión finalizó. Ingresa nuevamente.")
class ActivityUnavailable : IOException("La actividad no existe o no está disponible para tu cuenta.")
class InvalidCredentials : IOException("Usuario o contraseña incorrectos.")
class ApiFailure(message: String) : IOException(message)

/** One cookie jar per login. No cookies or passwords are written to storage. */
class MainApi(val endpoint: String) {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    private data class Response(val status: Int, val body: String, val location: String?)

    private fun request(path: String, form: Map<String, String>? = null): Response {
        val uri = URI(endpoint + path)
        val connection = uri.toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
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
            }
            val status = connection.responseCode
            cookies.put(uri, connection.headerFields.filterKeys { it != null })
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return Response(status, body, connection.getHeaderField("Location"))
        } finally {
            connection.disconnect()
        }
    }

    private fun json(path: String): JSONObject = JSONObject(checked(request(path)))
    private fun checked(response: Response): String = when (response.status) {
        200 -> response.body
        401 -> { clearSession(); throw SessionExpired() }
        404 -> throw ActivityUnavailable()
        403 -> throw ApiFailure("No tienes permiso para realizar esta operación.")
        else -> throw ApiFailure("El servidor no respondió correctamente. Intenta nuevamente.")
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

    private fun parseActivity(json: JSONObject) = Activity(
        json.getLong("id"), json.getString("titulo"), json.getString("descripcion"),
        json.getString("curso"), json.getString("docente"), json.getString("fechaLimite"),
        json.getBoolean("vencida"), json.getBoolean("completada")
    )
}
