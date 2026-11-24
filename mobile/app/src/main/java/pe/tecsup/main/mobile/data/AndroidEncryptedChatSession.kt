package pe.tecsup.main.mobile.data

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.util.Base64
import java.util.UUID

internal class AndroidEncryptedChatSession(
    private val client: MainApi,
    private val conversation: UUID,
    private val identity: AndroidChatIdentityStore.Identity,
    private val group: Boolean,
    private val isVerified: (EncryptedChatDevice) -> Boolean
) {
    private val crypto = EncryptedChatCrypto()

    internal data class Prepared(
        val messageId: UUID,
        val recipients: List<Pair<EncryptedChatDevice, EncryptedChatPayload>>,
        val revision: String?
    )

    fun prepare(text: String, recipient: UUID?, moderateLocally: (String) -> String): Prepared {
        val roster = if (group) client.encryptedChatGroup(conversation) else null
        val devices = roster?.devices ?: client.encryptedChatDevices(conversation)
            .filter { it.deviceId == recipient && it.deviceId != identity.deviceId }
        check(devices.isNotEmpty()) { "No hay dispositivos de destino disponibles." }
        devices.forEach(::verifyPeer)
        check(moderateLocally(text) == "APPROVE") { "La IA local no aprobó el mensaje." }
        val message = UUID.randomUUID()
        val payloads = devices.map { peer ->
            val metadata = EncryptedChatContext(conversation, message, identity.deviceId, peer.deviceId)
            peer to crypto.encrypt(text, metadata, identity.encryption, peer.encryptionKey)
        }
        return Prepared(message, payloads, roster?.revision)
    }

    fun send(prepared: Prepared) {
        if (group) {
            client.sendEncryptedChatGroup(conversation, prepared.messageId, identity.deviceId,
                requireNotNull(prepared.revision), prepared.recipients)
        } else {
            val (peer, payload) = prepared.recipients.single()
            client.sendEncryptedChatText(conversation, OutgoingEncryptedChatText(
                prepared.messageId, identity.deviceId, peer.deviceId, payload))
        }
    }

    fun read(after: Long): List<Pair<IncomingEncryptedChatText, String>> {
        val devices = if (group) client.encryptedChatGroup(conversation).devices else client.encryptedChatDevices(conversation)
        return client.encryptedChatInbox(conversation, identity.deviceId, after).map { message ->
            val peer = devices.find { it.username == message.sender && it.deviceId == message.senderDeviceId }
                ?: error("No se encontró el dispositivo del remitente.")
            verifyPeer(peer)
            val metadata = EncryptedChatContext(conversation, message.messageId, peer.deviceId, identity.deviceId)
            message to crypto.decrypt(message.envelope, metadata, identity.encryption, peer.encryptionKey)
        }
    }

    private fun verifyPeer(peer: EncryptedChatDevice) {
        check(isVerified(peer)) { "Compara el código de este dispositivo antes de continuar." }
        val decoder = Base64.getUrlDecoder()
        val message = listOf("MAIN chat encryption key v1", peer.username, peer.deviceId.toString(),
            peer.signingKey, peer.encryptionKey).joinToString("\n").toByteArray(Charsets.UTF_8)
        val verifier = Ed25519Signer().apply {
            init(false, Ed25519PublicKeyParameters(decoder.decode(peer.signingKey), 0))
            update(message, 0, message.size)
        }
        check(verifier.verifySignature(decoder.decode(peer.encryptionProof))) { "La firma del dispositivo no es válida." }
    }
}
