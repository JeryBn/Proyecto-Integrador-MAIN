package pe.tecsup.main.mobile.data

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.hpke.HPKE
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

internal data class EncryptedChatContext(
    val conversationId: UUID,
    val messageId: UUID,
    val senderDeviceId: UUID,
    val recipientDeviceId: UUID
) {
    fun associatedData(): ByteArray =
        listOf(conversationId, messageId, senderDeviceId, recipientDeviceId)
            .joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }
            .toByteArray(Charsets.UTF_8)
}

internal class EncryptedChatCrypto {
    private val hpke = HPKE(HPKE.mode_auth, HPKE.kem_P256_SHA256, HPKE.kdf_HKDF_SHA256, HPKE.aead_AES_GCM256)
    private val info = "MAIN private message HPKE v1".toByteArray(Charsets.UTF_8)
    private val base64 = Base64.getUrlEncoder().withoutPadding()

    fun generateKeys(): AsymmetricCipherKeyPair = hpke.generatePrivateKey()

    fun publicKey(keys: AsymmetricCipherKeyPair): String = base64.encodeToString(hpke.serializePublicKey(keys.public))

    fun encrypt(text: String, context: EncryptedChatContext, sender: AsymmetricCipherKeyPair,
                recipientPublicKey: String): EncryptedChatPayload {
        val plaintext = text.toByteArray(Charsets.UTF_8)
        require(plaintext.size in 1..MAX_TEXT_BYTES)
        val recipient = hpke.deserializePublicKey(decode(recipientPublicKey, 65))
        val session = hpke.setupAuthS(recipient, info, sender)
        return EncryptedChatPayload(1, base64.encodeToString(session.encapsulation),
            base64.encodeToString(session.seal(context.associatedData(), plaintext)))
    }

    fun decrypt(envelope: EncryptedChatPayload, context: EncryptedChatContext, recipient: AsymmetricCipherKeyPair,
                senderPublicKey: String): String {
        require(envelope.version == 1)
        val encodedKey = decode(envelope.encapsulatedKey, 65)
        val ciphertext = decode(envelope.ciphertext, MAX_TEXT_BYTES + 16)
        require(ciphertext.size >= 17)
        val sender = hpke.deserializePublicKey(decode(senderPublicKey, 65))
        val session = hpke.setupAuthR(encodedKey, recipient, info, sender)
        return Charsets.UTF_8.newDecoder()
            .decode(ByteBuffer.wrap(session.open(context.associatedData(), ciphertext))).toString()
    }

    private fun decode(encoded: String, maximum: Int): ByteArray {
        require(encoded.isNotEmpty() && encoded.length <= (maximum * 4 + 2) / 3)
        val decoded = Base64.getUrlDecoder().decode(encoded)
        require(base64.encodeToString(decoded) == encoded && decoded.size <= maximum)
        return decoded
    }

    private companion object {
        const val MAX_TEXT_BYTES = 16_384
    }
}
