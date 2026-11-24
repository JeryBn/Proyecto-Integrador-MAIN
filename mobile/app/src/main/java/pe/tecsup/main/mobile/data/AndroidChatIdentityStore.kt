package pe.tecsup.main.mobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.hpke.HPKE
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidChatIdentityStore(private val context: Context) {
    private val hpke = HPKE(HPKE.mode_auth, HPKE.kem_P256_SHA256, HPKE.kdf_HKDF_SHA256, HPKE.aead_AES_GCM256)
    private val base64 = Base64.getUrlEncoder().withoutPadding()

    internal data class Identity(val deviceId: UUID, val signing: Ed25519PrivateKeyParameters,
                                 val encryption: AsymmetricCipherKeyPair)

    fun load(endpoint: String, username: String): Identity = synchronized(storageLock) {
        val scope = "$endpoint\n$username".toByteArray(Charsets.UTF_8)
        val suffix = base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(scope))
        val file = AtomicFile(File(context.noBackupFilesDir, "main-chat-identity-$suffix"))
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "main.chat.identity.$suffix"
        val exists = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
        val key = store.getKey(alias, null) as? SecretKey
        if (exists) {
            checkNotNull(key) { "La clave protegida de este dispositivo ya no está disponible." }
            return@synchronized read(file, key, scope)
        }
        val wrappingKey = key ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
            generateKey()
        }
        val identity = Identity(UUID.randomUUID(), Ed25519PrivateKeyParameters(SecureRandom()), hpke.generatePrivateKey())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        cipher.updateAAD(scope)
        val clear = ByteBuffer.allocate(145).putLong(identity.deviceId.mostSignificantBits)
            .putLong(identity.deviceId.leastSignificantBits).put(identity.signing.encoded)
            .put(hpke.serializePrivateKey(identity.encryption.private)).put(hpke.serializePublicKey(identity.encryption.public)).array()
        val encrypted = try { cipher.doFinal(clear) } finally { clear.fill(0) }
        val stream = file.startWrite()
        try {
            stream.write(byteArrayOf(1) + cipher.iv + encrypted)
            file.finishWrite(stream)
        } catch (failure: Exception) {
            file.failWrite(stream)
            throw failure
        }
        identity
    }

    private fun read(file: AtomicFile, key: SecretKey, scope: ByteArray): Identity {
        val encrypted = file.readFully()
        require(encrypted.size == 174 && encrypted[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, encrypted, 1, 12))
        cipher.updateAAD(scope)
        val clear = cipher.doFinal(encrypted, 13, encrypted.size - 13)
        try {
            val input = ByteBuffer.wrap(clear)
            val device = UUID(input.long, input.long)
            val signing = Ed25519PrivateKeyParameters(clear, 16)
            val secret = clear.copyOfRange(48, 80)
            return try { Identity(device, signing, hpke.deserializePrivateKey(secret, clear.copyOfRange(80, 145))) }
                finally { secret.fill(0) }
        } finally {
            clear.fill(0)
        }
    }

    fun register(client: MainApi, username: String) {
        val identity = load(client.endpoint, username)
        val signingKey = base64.encodeToString(identity.signing.generatePublicKey().encoded)
        val encryptionKey = base64.encodeToString(hpke.serializePublicKey(identity.encryption.public))
        val signed = listOf("MAIN chat encryption key v1", username, identity.deviceId.toString(), signingKey, encryptionKey)
            .joinToString("\n").toByteArray(Charsets.UTF_8)
        val signer = Ed25519Signer().apply {
            init(true, identity.signing)
            update(signed, 0, signed.size)
        }
        client.registerEncryptedChatDevice(identity.deviceId, signingKey, encryptionKey,
            base64.encodeToString(signer.generateSignature()))
    }

    private companion object {
        val storageLock = Any()
    }
}
