package pe.tecsup.main.mobile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import pe.tecsup.main.mobile.data.DirectCallSignal
import pe.tecsup.main.mobile.data.ResourceUnavailable
import java.util.UUID

private data class CallRequest(val conversation: UUID, val kind: String, val invitation: DirectCallSignal? = null)

@Composable
internal fun DirectCallHost(model: MainViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val client = model.activeCallApi() ?: return
    var incoming by remember(client) { mutableStateOf<DirectCallSignal?>(null) }
    var request by remember(client) { mutableStateOf<CallRequest?>(null) }
    var permissionsFor by remember(client) { mutableStateOf<CallRequest?>(null) }
    var media by remember(client) { mutableStateOf<NativeCallMedia?>(null) }
    var status by remember(client) { mutableStateOf("") }
    var error by remember(client) { mutableStateOf("") }
    var muted by remember(client) { mutableStateOf(false) }
    var rejecting by remember(client) { mutableStateOf(false) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) request = null
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val next = permissionsFor
        permissionsFor = null
        if (next != null && results.isNotEmpty() && results.values.all { it }) request = next
        else error = "Permite el micrófono y, para una videollamada, la cámara."
    }

    fun begin(next: CallRequest) {
        if (request != null || permissionsFor != null) return
        error = ""
        val required = if (next.kind == "VIDEO") listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            else listOf(Manifest.permission.RECORD_AUDIO)
        val missing = required.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) request = next else {
            permissionsFor = next
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(client, model.outgoingCall) {
        val outgoing = model.outgoingCall ?: return@LaunchedEffect
        model.outgoingCall = null
        begin(CallRequest(UUID.fromString(outgoing.first), outgoing.second))
    }

    LaunchedEffect(client, request, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (request != null) return@repeatOnLifecycle
            while (isActive) {
                try {
                    incoming = withContext(Dispatchers.IO) { client.incomingCalls().firstOrNull() }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    incoming = null
                }
                delay(2_500)
            }
        }
    }

    LaunchedEffect(client, request) {
        val next = request ?: return@LaunchedEffect
        val callId = next.invitation?.id ?: UUID.randomUUID()
        var activeMedia: NativeCallMedia? = null
        var mayHaveSignal = next.invitation != null
        status = "Preparando llamada…"
        incoming = null
        muted = false
        try {
            val ice = withContext(Dispatchers.IO) { client.callIceServers() }
            val connection = NativeCallMedia(context.applicationContext, next.kind == "VIDEO", ice)
            activeMedia = connection
            media = connection
            val offer = next.invitation?.offer
            val sdp = if (next.invitation == null) connection.createOffer()
                else connection.createAnswer(requireNotNull(offer) { "La invitación ya no está disponible." })
            mayHaveSignal = true
            withContext(Dispatchers.IO) {
                if (next.invitation == null) client.startCall(callId, next.conversation, next.kind, sdp)
                else client.answerCall(callId, sdp)
            }
            status = if (next.invitation == null) "Llamando…" else "Conectando…"
            var applied = next.invitation != null
            var attempts = 0
            var disconnected = 0
            while (isActive) {
                val signal = withContext(Dispatchers.IO) { client.callSignal(callId) }
                if (signal.state == "ENDED") {
                    mayHaveSignal = false
                    break
                }
                if (!applied && signal.answer != null && signal.answer != "null" && signal.state == "ACTIVE") {
                    connection.applyAnswer(signal.answer)
                    applied = true
                    attempts = 0
                }
                when (connection.state.value) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        status = "En llamada"
                        disconnected = 0
                    }
                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        status = "Recuperando conexión…"
                        check(++disconnected < 8) { "Se perdió la conexión." }
                    }
                    PeerConnection.PeerConnectionState.FAILED -> error("No se pudo conectar la llamada.")
                    else -> {
                        if (applied) status = "Conectando…"
                        check(++attempts < if (applied) 12 else 30) {
                            if (applied) "No se pudo conectar la llamada." else "La otra persona no contestó."
                        }
                    }
                }
                delay(1_800)
            }
        } catch (_: TimeoutCancellationException) {
            error = "No se pudo preparar la conexión de la llamada."
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = if (failure is ResourceUnavailable) "La llamada ya no está disponible."
                else (failure as? IllegalStateException)?.message ?: "No se pudo completar la llamada."
        } finally {
            media = null
            activeMedia?.close()
            if (request == next) request = null
            if (mayHaveSignal) withContext(NonCancellable + Dispatchers.IO) { runCatching { client.endCall(callId) } }
        }
    }

    val invite = incoming
    if (invite != null && request == null && permissionsFor == null) {
        AlertDialog(onDismissRequest = {}, title = { Text(if (invite.kind == "VIDEO") "Videollamada entrante" else "Llamada entrante") },
            text = { Text(invite.callerName) }, confirmButton = {
                TextButton(enabled = !rejecting, onClick = { begin(CallRequest(invite.conversationId, invite.kind, invite)) }) { Text("Aceptar") }
            }, dismissButton = {
                TextButton(enabled = !rejecting, onClick = {
                    rejecting = true
                    scope.launch {
                        try { withContext(Dispatchers.IO) { client.endCall(invite.id) }; incoming = null }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "No se pudo rechazar la llamada." }
                        finally { rejecting = false }
                    }
                }) { Text("Rechazar") }
            })
    }
    request?.let { current ->
        val currentMedia = media
        AlertDialog(onDismissRequest = {}, title = { Text(if (current.kind == "VIDEO") "Videollamada" else "Llamada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status)
                    if (current.kind == "VIDEO" && currentMedia != null) {
                        val remote by currentMedia.remote.collectAsState()
                        remote?.let { VideoSurface(it, currentMedia, Modifier.fillMaxWidth().height(180.dp)) }
                        currentMedia.local?.let { VideoSurface(it, currentMedia, Modifier.width(120.dp).height(90.dp), true) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { request = null }) { Text("Colgar") } },
            dismissButton = {
                TextButton(enabled = currentMedia != null, onClick = { muted = !muted; currentMedia?.mute(muted) }) {
                    Text(if (muted) "Activar micrófono" else "Silenciar")
                }
            })
    }
    if (error.isNotEmpty() && request == null && invite == null) {
        AlertDialog(onDismissRequest = { error = "" }, title = { Text("Llamadas de MAIN") }, text = { Text(error) },
            confirmButton = { TextButton(onClick = { error = "" }) { Text("Cerrar") } })
    }
}

@Composable
private fun VideoSurface(track: VideoTrack, media: NativeCallMedia, modifier: Modifier, mirror: Boolean = false) {
    key(track) {
        AndroidView(modifier = modifier, factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(media.eglContext, null)
                setMirror(mirror)
                setZOrderMediaOverlay(mirror)
                track.addSink(this)
            }
        }, onRelease = { view ->
            runCatching { track.removeSink(view) }
            view.release()
        }, update = {})
    }
}
