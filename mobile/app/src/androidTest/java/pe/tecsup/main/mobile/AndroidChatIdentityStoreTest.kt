package pe.tecsup.main.mobile.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AndroidChatIdentityStoreTest {
    @Test fun keyMaterialSurvivesReopeningAndIsScopedByAccountAndServer() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val marker = UUID.randomUUID().toString()
        val firstStore = AndroidChatIdentityStore(ctx)
        val a = firstStore.load("https://example.invalid", marker)
        val same = AndroidChatIdentityStore(ctx).load("https://example.invalid", marker)
        val other = firstStore.load("https://example.invalid", marker + "-other")
        val differentServer = firstStore.load("https://different.example.invalid", marker)
        val crypto = EncryptedChatCrypto()
        assertEquals(a.deviceId, same.deviceId)
        assertArrayEquals(a.signing.generatePublicKey().encoded, same.signing.generatePublicKey().encoded)
        assertEquals(crypto.publicKey(a.encryption), crypto.publicKey(same.encryption))
        assertNotEquals(a.deviceId, other.deviceId)
        assertNotEquals(a.deviceId, differentServer.deviceId)
        val context = EncryptedChatContext(UUID.randomUUID(), UUID.randomUUID(), other.deviceId, a.deviceId)
        val payload = crypto.encrypt("Prueba Android", context, other.encryption, crypto.publicKey(a.encryption))
        assertEquals("Prueba Android", crypto.decrypt(payload, context, same.encryption, crypto.publicKey(other.encryption)))
    }
}
