package pe.tecsup.main.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.tecsup.main.mobile.data.AndroidLocalChatModerator

@RunWith(AndroidJUnit4::class)
class AndroidLocalChatModeratorTest {
    @Test fun classifiesSyntheticMessagesWithTheDeviceModel() = runBlocking {
        val status = Generation.getClient().checkStatus()
        android.util.Log.i("MainLocalModeratorTest", "AICore feature status: $status")
        assumeTrue("Gemini Nano no está disponible en el dispositivo de prueba.", status == FeatureStatus.AVAILABLE)
        val moderator = AndroidLocalChatModerator()
        moderator.activate()
        assertEquals("APPROVE", moderator.moderate("JAJAJAJA, nos vemos en clase."))
        assertEquals("REMOVE", moderator.moderate("Voy a golpearte mañana en la puerta de la escuela."))
    }
}
