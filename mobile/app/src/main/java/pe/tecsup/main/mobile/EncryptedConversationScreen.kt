package pe.tecsup.main.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.tecsup.main.mobile.data.AndroidEncryptedChatSession
import pe.tecsup.main.mobile.data.AndroidLocalChatModerator
import pe.tecsup.main.mobile.data.EncryptedChatDevice
import pe.tecsup.main.mobile.data.ResourceUnavailable

@Composable
internal fun EncryptedConversationScreen(model: MainViewModel, id: String, group: Boolean, close: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val moderator = remember { AndroidLocalChatModerator() }
    val verified = remember { mutableStateListOf<String>() }
    var chat by remember { mutableStateOf<AndroidEncryptedChatSession?>(null) }
    var devices by remember { mutableStateOf<List<Pair<EncryptedChatDevice, String>>>(emptyList()) }
    var selected by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var cursor by remember { mutableLongStateOf(0L) }
    var messages by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var pending by remember { mutableStateOf<AndroidEncryptedChatSession.Prepared?>(null) }
    var busy by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        notice = ""
        scope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                notice = when (failure) {
                    is ResourceUnavailable -> "El chat cifrado no está disponible en esta conversación."
                    is IllegalStateException -> failure.message ?: "No se pudo completar la operación."
                    else -> "No se pudo completar la operación. Intenta nuevamente."
                }
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = close) { Text("Volver al chat") }
        Text("Chat cifrado de prueba", style = MaterialTheme.typography.titleLarge)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(enabled = !busy && pending == null, onClick = {
            run {
                val active = chat ?: model.openEncryptedChat(context, id, group) { peer ->
                    chat?.safetyCode(peer) in verified
                }.also { chat = it }
                devices = withContext(Dispatchers.IO) { active.comparisonCodes() }
                selected = 0
                code = ""
                if (devices.isEmpty()) notice = "El contacto todavía no tiene un dispositivo registrado."
            }
        }) { Text("Cargar dispositivos") }
        devices.getOrNull(selected)?.let { (peer, expected) ->
            Text("Dispositivo de ${peer.username}")
            Text(expected)
            if (devices.size > 1) {
                TextButton(enabled = !busy && pending == null, onClick = {
                    selected = (selected + 1) % devices.size
                    code = ""
                }) { Text("Siguiente dispositivo") }
            }
            if (expected !in verified) {
                OutlinedTextField(code, { code = it.take(96) }, label = { Text("Código recibido por otro canal") })
                Button(enabled = !busy && code.filterNot(Char::isWhitespace).equals(expected.replace(" ", ""), true),
                    onClick = { verified.add(expected); code = "" }) { Text("Confirmar código") }
            }
            if (!ready) {
                Button(enabled = !busy, onClick = { run { moderator.activate(); ready = true } }) { Text("Activar IA local") }
            }
            if (expected in verified) {
                Button(enabled = !busy, onClick = {
                    run {
                        val incoming = withContext(Dispatchers.IO) { requireNotNull(chat).read(cursor) }
                        messages = messages + incoming.map { (envelope, body) -> envelope.sender to body }
                        incoming.lastOrNull()?.let { cursor = it.first.cursor }
                        if (incoming.isEmpty()) notice = "No hay mensajes nuevos."
                    }
                }) { Text("Recibir mensajes cifrados") }
            }
            if (ready && expected in verified && (!group || devices.all { it.second in verified })) {
                OutlinedTextField(text, { text = it.take(4000) }, enabled = !busy && pending == null,
                    label = { Text("Mensaje cifrado") })
                Button(enabled = !busy && text.isNotBlank(), onClick = {
                    run {
                        val active = requireNotNull(chat)
                        val prepared = pending ?: withContext(Dispatchers.IO) {
                            active.prepare(text, peer.deviceId, moderator::moderate)
                        }
                        pending = prepared
                        withContext(Dispatchers.IO) { active.send(prepared) }
                        pending = null
                        messages = messages + ((model.profile?.usuario ?: "") to text)
                        text = ""
                    }
                }) { Text(if (pending == null) "Enviar cifrado" else "Reintentar envío") }
            }
        }
        if (notice.isNotEmpty()) Text(notice)
        messages.forEach { (sender, body) -> Text("$sender · $body") }
    }
}
