package pe.tecsup.main.mobile.data

import java.util.UUID

internal data class EncryptedChatDevice(
    val username: String,
    val deviceId: UUID,
    val signingKey: String,
    val encryptionKey: String,
    val encryptionProof: String
)

internal data class EncryptedChatPayload(val version: Int, val encapsulatedKey: String, val ciphertext: String)

internal data class OutgoingEncryptedChatText(
    val messageId: UUID,
    val senderDeviceId: UUID,
    val recipientDeviceId: UUID,
    val envelope: EncryptedChatPayload
)

internal data class IncomingEncryptedChatText(
    val cursor: Long,
    val sender: String,
    val senderDeviceId: UUID,
    val messageId: UUID,
    val envelope: EncryptedChatPayload
)

internal data class EncryptedChatGroup(val revision: String, val devices: List<EncryptedChatDevice>)
