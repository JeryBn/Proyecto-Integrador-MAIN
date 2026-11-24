package pe.tecsup.main.mobile

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.webrtc.PeerConnection
import org.webrtc.VideoSink
import pe.tecsup.main.mobile.data.MainApi
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NativeCallTest {

    @Test fun nativePeersNegotiateAudioUsingTheMainSignalingApi() = runBlocking { exercise(false) }

    @Test fun nativePeersReceiveCameraFramesUsingTheMainSignalingApi() = runBlocking { exercise(true) }

    private suspend fun exercise(video: Boolean) {
        val endpoint = "http://127.0.0.1:8088"
        val caller = MainApi(endpoint)
        val callee = MainApi(endpoint)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext.applicationContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
        if (video) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)
        val id = UUID.randomUUID()
        withContext(Dispatchers.IO) {
            caller.login("alumno", "MainDemo2026!")
            callee.login("otro", "MainDemo2026!")
        }
        try {
            val conversation = withContext(Dispatchers.IO) { UUID.fromString(caller.startDirect("otro")) }
            withContext(Dispatchers.Main) {
                NativeCallMedia(context, video, emptyList()).use { left ->
                    NativeCallMedia(context, false, emptyList()).use { right ->
                        val offer = left.createOffer()
                        val invite = withContext(Dispatchers.IO) {
                            caller.startCall(id, conversation, if (video) "VIDEO" else "AUDIO", offer)
                            callee.incomingCalls().first { it.id == id }
                        }
                        assertEquals("RINGING", invite.state)
                        val answer = right.createAnswer(requireNotNull(invite.offer))
                        val reply = withContext(Dispatchers.IO) {
                            callee.answerCall(id, answer)
                            caller.callSignal(id)
                        }
                        left.applyAnswer(requireNotNull(reply.answer))
                        withTimeout(20_000) {
                            left.state.first { it == PeerConnection.PeerConnectionState.CONNECTED }
                            right.state.first { it == PeerConnection.PeerConnectionState.CONNECTED }
                            if (video) {
                                val track = requireNotNull(right.remote.first { it != null })
                                val frame = CompletableDeferred<Unit>()
                                val sink = VideoSink { value -> if (value.buffer.width > 0) frame.complete(Unit) }
                                track.addSink(sink)
                                try { frame.await() } finally { track.removeSink(sink) }
                            }
                        }
                        withContext(Dispatchers.IO) {
                            caller.endCall(id)
                            assertEquals("ENDED", callee.callSignal(id).state)
                        }
                    }
                }
            }
        } finally {
            withContext(Dispatchers.IO) {
                runCatching { caller.endCall(id) }
                runCatching { caller.logout() }
                runCatching { callee.logout() }
            }
        }
    }
}
