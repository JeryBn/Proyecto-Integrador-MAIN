package pe.tecsup.main.mobile

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.webrtc.*
import pe.tecsup.main.mobile.data.CallIceServer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class NativeCallMedia(context: Context, video: Boolean, ice: List<CallIceServer>) : AutoCloseable {
    val state = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val remote = MutableStateFlow<VideoTrack?>(null)
    private val egl = EglBase.create()
    val eglContext: EglBase.Context get() = egl.eglBaseContext
    private val factory: PeerConnectionFactory
    private val peer: PeerConnection
    private val audioSource: AudioSource
    private val microphone: AudioTrack
    private var surface: SurfaceTextureHelper? = null
    private var source: VideoSource? = null
    private var capturer: CameraVideoCapturer? = null
    var local: VideoTrack? = null
        private set

    init {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions())
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext, true, false))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
            .createPeerConnectionFactory()
        val config = PeerConnection.RTCConfiguration(ice.map {
            PeerConnection.IceServer.builder(it.urls).setUsername(it.username).setPassword(it.credential).createIceServer()
        }).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
        peer = requireNotNull(factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(value: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(value: PeerConnection.IceConnectionState) = Unit
            override fun onIceConnectionReceivingChange(value: Boolean) = Unit
            override fun onIceGatheringChange(value: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidate(value: IceCandidate) = Unit
            override fun onIceCandidatesRemoved(value: Array<IceCandidate>) = Unit
            override fun onAddStream(value: MediaStream) = Unit
            override fun onRemoveStream(value: MediaStream) = Unit
            override fun onDataChannel(value: DataChannel) = value.dispose()
            override fun onRenegotiationNeeded() = Unit
            override fun onConnectionChange(value: PeerConnection.PeerConnectionState) { state.value = value }
            override fun onTrack(transceiver: RtpTransceiver) { remote.value = transceiver.receiver.track() as? VideoTrack ?: remote.value }
        }))
        audioSource = factory.createAudioSource(MediaConstraints())
        microphone = factory.createAudioTrack("main-audio", audioSource)
        try {
            peer.addTrack(microphone, listOf("main"))
            if (video) startCamera(context)
        } catch (failure: Exception) {
            close()
            throw failure
        }
    }

    private fun startCamera(context: Context) {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(context)) Camera2Enumerator(context) else Camera1Enumerator(false)
        val camera = enumerator.deviceNames.firstOrNull(enumerator::isFrontFacing)
            ?: enumerator.deviceNames.firstOrNull() ?: error("El dispositivo no tiene una cámara disponible.")
        val capture = requireNotNull(enumerator.createCapturer(camera, null)) { "No se pudo abrir la cámara." }
        capturer = capture
        val helper = SurfaceTextureHelper.create("MainCallCamera", eglContext)
        surface = helper
        val videoSource = factory.createVideoSource(false)
        source = videoSource
        capture.initialize(helper, context, videoSource.capturerObserver)
        capture.startCapture(640, 480, 20)
        local = factory.createVideoTrack("main-video", videoSource).also { peer.addTrack(it, listOf("main")) }
    }

    suspend fun createAnswer(offer: String): String {
        setRemote(SessionDescription.Type.OFFER, offer)
        return localSdp(true)
    }

    suspend fun createOffer(): String = localSdp(false)

    suspend fun applyAnswer(answer: String) = setRemote(SessionDescription.Type.ANSWER, answer)

    private suspend fun localSdp(answer: Boolean): String {
        val description = suspendCancellableCoroutine<SessionDescription> { continuation ->
            val observer = object : EmptySdpObserver() {
                override fun onCreateSuccess(value: SessionDescription) { if (continuation.isActive) continuation.resume(value) }
                override fun onCreateFailure(error: String) {
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException("No se pudo preparar la llamada."))
                }
            }
            if (answer) peer.createAnswer(observer, MediaConstraints()) else peer.createOffer(observer, MediaConstraints())
        }
        setDescription { peer.setLocalDescription(it, description) }
        withTimeout(12_000) {
            while (peer.iceGatheringState() != PeerConnection.IceGatheringState.COMPLETE) delay(100)
        }
        return requireNotNull(peer.localDescription).description
    }

    private suspend fun setRemote(type: SessionDescription.Type, sdp: String) =
        setDescription { peer.setRemoteDescription(it, SessionDescription(type, sdp)) }

    private suspend fun setDescription(action: (SdpObserver) -> Unit) =
        suspendCancellableCoroutine<Unit> { continuation ->
            action(object : EmptySdpObserver() {
                override fun onSetSuccess() { if (continuation.isActive) continuation.resume(Unit) }
                override fun onSetFailure(error: String) {
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException("No se pudo negociar la llamada."))
                }
            })
        }

    fun mute(muted: Boolean) { microphone.setEnabled(!muted) }

    override fun close() {
        peer.dispose()
        capturer?.let {
            runCatching { it.stopCapture() }
            it.dispose()
        }
        local?.dispose()
        source?.dispose()
        surface?.dispose()
        microphone.dispose()
        audioSource.dispose()
        factory.dispose()
        egl.release()
    }

    private open class EmptySdpObserver : SdpObserver {
        override fun onCreateSuccess(value: SessionDescription) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String) = Unit
        override fun onSetFailure(error: String) = Unit
    }
}
