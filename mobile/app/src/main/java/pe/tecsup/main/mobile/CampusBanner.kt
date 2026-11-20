package pe.tecsup.main.mobile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val campusBrand = Color(android.graphics.Color.parseColor(BuildConfig.CAMPUS_COLOR))
internal val campusLime = Color(0xFFD9F264)
internal val campusInk = Color(0xFF16221A)

@Composable
internal fun CampusBanner(
    title: String,
    subtitle: String = BuildConfig.CAMPUS_NAME,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Surface(color=Color(0xFFF7F2E7),contentColor=campusInk,shape=MaterialTheme.shapes.large,
        modifier=Modifier.fillMaxWidth()) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawPath(Path().apply {
                    moveTo(size.width*.81f,0f); lineTo(size.width,0f)
                    lineTo(size.width,size.height); lineTo(size.width*.58f,size.height); close()
                },campusBrand.copy(alpha=.18f))
                drawPath(Path().apply {
                    moveTo(size.width*.93f,0f); lineTo(size.width,0f)
                    lineTo(size.width,size.height); lineTo(size.width*.72f,size.height); close()
                },campusLime.copy(alpha=.51f))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
                }
                if (actions != null) actions()
            }
        }
    }
}
