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
    var items by mutableStateOf<List<Activity>>(emptyList())
        private set
    var loadingList by mutableStateOf(false)
        private set
    var listError by mutableStateOf("")
        private set
    var search by mutableStateOf("")
    var filter by mutableStateOf("all")
    var selectedId by mutableStateOf<Long?>(null)
        private set
    var detail by mutableStateOf<Activity?>(null)
        private set
    var loadingDetail by mutableStateOf(false)
        private set
    var detailError by mutableStateOf("")
        private set
    private var detailRequest = 0L
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
                loadPending()
            } catch (error: Exception) {
                message = friendly(error)
            } finally { busy = false }
        }
    }

    fun loadPending() {
        val client = api ?: return
        if (loadingList) return
        loadingList = true
        listError = ""
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { client.pending() }
                if (api === client) items = result
            } catch (error: Exception) {
                if (api === client) {
                    if (error is SessionExpired) expireSession()
                    else listError = friendly(error)
                }
            } finally { if (api === client) loadingList = false }
        }
    }

    private fun clearAccount() {
        profile = null
        items = emptyList()
        search = ""
        filter = "all"
        listError = ""
        loadingList = false
        backToList()
    }

    fun openDetail(id: Long) {
        val client = api ?: return
        val request = ++detailRequest
        selectedId = id
        detail = null
        detailError = ""
        loadingDetail = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { client.detail(id) }
                if (api === client && detailRequest == request) detail = result
            } catch (error: Exception) {
                if (api === client && detailRequest == request) {
                    if (error is SessionExpired) expireSession()
                    else detailError = friendly(error)
                }
            } finally {
                if (api === client && detailRequest == request) loadingDetail = false
            }
        }
    }

    fun backToList() {
        detailRequest++
        selectedId = null
        detail = null
        detailError = ""
        loadingDetail = false
    }

    private fun expireSession() {
        api?.clearSession()
        api = null
        clearAccount()
        message = "Tu sesión finalizó. Ingresa nuevamente."
    }

    fun logout() {
        if (busy) return
        viewModelScope.launch {
            busy = true
            val client = api
            try { withContext(Dispatchers.IO) { client?.logout() } }
            catch (_: Exception) { message = "Saliste de la app. No se pudo confirmar el cierre en el servidor." }
            finally { api = null; clearAccount(); busy = false }
        }
    }

    private fun friendly(error: Exception): String = when (error) {
        is InvalidCredentials, is ApiFailure, is SessionExpired, is ActivityUnavailable,
        is IllegalArgumentException -> error.message ?: "No pudimos completar la operación."
        is IOException -> "No pudimos conectar con el servidor. Revisa la conexión e intenta nuevamente."
        else -> "No pudimos leer la respuesta del servidor. Intenta nuevamente."
    }
}
