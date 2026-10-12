package pe.tecsup.main.mobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.tecsup.main.mobile.data.*
import java.io.IOException

class MainViewModel : ViewModel() {
    var profile by mutableStateOf<Profile?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    private var api: MainApi? = null

    fun login(endpoint: String, username: String, password: String) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            message = ""
            try {
                val client = MainApi(validateEndpoint(endpoint, BuildConfig.DEBUG))
                val result = withContext(Dispatchers.IO) { client.login(username, password) }
                api = client
                profile = result
            } catch (error: Exception) {
                message = friendly(error)
            } finally { busy = false }
        }
    }

    fun logout() {
        if (busy) return
        viewModelScope.launch {
            busy = true
            val client = api
            try { withContext(Dispatchers.IO) { client?.logout() } }
            catch (_: Exception) { message = "Saliste de la app. No se pudo confirmar el cierre en el servidor." }
            finally { api = null; profile = null; busy = false }
        }
    }

    private fun friendly(error: Exception): String = when (error) {
        is InvalidCredentials, is ApiFailure, is SessionExpired, is ActivityUnavailable,
        is IllegalArgumentException -> error.message ?: "No pudimos completar la operación."
        is IOException -> "No pudimos conectar con el servidor. Revisa la conexión e intenta nuevamente."
        else -> "No pudimos leer la respuesta del servidor. Intenta nuevamente."
    }
}
