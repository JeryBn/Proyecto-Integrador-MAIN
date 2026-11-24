package pe.tecsup.main.mobile.data

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.util.Base64
import java.util.UUID
import java.security.MessageDigest
import org.json.JSONArray

internal class AndroidEncryptedChatSession(
    private val client: MainApi,
    private val conversation: UUID,
    private val identity: AndroidChatIdentityStore.Identity,
    private val group: Boolean,
    private val username: String,
    private val isVerified: (EncryptedChatDevice) -> Boolean
) {
    private val crypto = EncryptedChatCrypto()

    internal data class Prepared(
        val messageId: UUID,
        val recipients: List<Pair<EncryptedChatDevice, EncryptedChatPayload>>,
        val revision: String?
    )

    suspend fun prepare(text: String, recipient: UUID?, moderateLocally: suspend (String) -> String): Prepared {
        val roster = if (group) client.encryptedChatGroup(conversation) else null
        val devices = roster?.devices ?: client.encryptedChatDevices(conversation)
            .filter { it.deviceId == recipient && it.username != username }
        check(devices.isNotEmpty()) { "No hay dispositivos de destino disponibles." }
        devices.forEach(::verifyPeer)
        val decision = moderateLocally(text)
        check(decision == "APPROVE") {
            if (decision == "EDIT") "Edita el mensaje antes de enviarlo." else "El mensaje no se puede enviar."
        }
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

    internal fun comparisonCodes(): List<Pair<EncryptedChatDevice, String>> {
        val devices = if (group) client.encryptedChatGroup(conversation).devices
            else client.encryptedChatDevices(conversation)
        return devices.filter { it.username != username && it.encryptionKey.isNotEmpty() }.map { peer ->
            verifySignature(peer)
            peer to safetyCode(peer)
        }
    }

    internal fun safetyCode(peer: EncryptedChatDevice): String {
        val base64 = Base64.getUrlEncoder().withoutPadding()
        val self = listOf(username, identity.deviceId.toString(),
            base64.encodeToString(identity.signing.generatePublicKey().encoded), crypto.publicKey(identity.encryption))
        val other = listOf(peer.username, peer.deviceId.toString(), peer.signingKey, peer.encryptionKey)
        val pair = listOf(JSONArray(self).toString(), JSONArray(other).toString()).sorted()
        val bytes = JSONArray(listOf("MAIN device comparison v2") + pair).toString().toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02X".format(it.toInt() and 0xff) }.chunked(4).joinToString(" ")
    }

    private fun verifyPeer(peer: EncryptedChatDevice) {
        check(isVerified(peer)) { "Compara el código de este dispositivo antes de continuar." }
        verifySignature(peer)
    }

    private fun verifySignature(peer: EncryptedChatDevice) {
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
