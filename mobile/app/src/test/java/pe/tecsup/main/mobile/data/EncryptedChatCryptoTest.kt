package pe.tecsup.main.mobile.data

import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.hpke.HPKE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64
import java.util.UUID

class EncryptedChatCryptoTest {
    @Test fun opensAnEnvelopeProducedByTheWebClient() {
        val codec = HPKE(HPKE.mode_auth, HPKE.kem_P256_SHA256, HPKE.kdf_HKDF_SHA256, HPKE.aead_AES_GCM256)
        val decoder = Base64.getUrlDecoder()
        // Synthetic fixture exported from the web client specifically for interoperability testing.
        val recipient = codec.deserializePrivateKey(decoder.decode("6rYPKwufRRdi5Nd_tn2a-4OdeFS1RxCN1Fcq5ctw7xU"),
            decoder.decode("BBoCrELex6icQh_Qhyjhm9-L_eo39QTrKplZkgv9fRSIHMQclG5W-A2LXS2csgq_X7rkkk15jlTI_c-pAqylpoA"))
        val sender = "BA_CeipjHJIcpY5e3to5U59mtyA4BTx88FwTyO39_RrVefEGVrsj3TZfZLhF47LLwQ_o8AIo45G9Hr-0I76xL98"
        val envelope = EncryptedChatPayload(1,
            "BKh3ZRUBDUkoYL_JEmMv_DTcRPbBNXJTABGIHKWiu7WqKoXpQiwCbmhES_ruf3RdGI_dmGU175_uX3wWYZLSzR4",
            "duXlEjAEiMdgzgpKOJpHPsfQImG5b_46duW9gOiEDLY4UdeULLkHv0eN3Il5Xbk9uZrKCSc")
        fun id(index: Int) = UUID.fromString("00000000-0000-4000-8000-00000000000$index")
        val context = EncryptedChatContext(id(1), id(2), id(3), id(4))
        val crypto = EncryptedChatCrypto()
        assertEquals("Texto sintético Web → Android 📚", crypto.decrypt(envelope, context, recipient, sender))
        assertThrows(InvalidCipherTextException::class.java) {
            crypto.decrypt(envelope, context.copy(messageId = UUID.randomUUID()), recipient, sender)
        }
        val newSender = crypto.generateKeys()
        val reply = crypto.encrypt("Respuesta", context, newSender, crypto.publicKey(recipient))
        assertEquals("Respuesta", crypto.decrypt(reply, context, recipient, crypto.publicKey(newSender)))
    }
}
