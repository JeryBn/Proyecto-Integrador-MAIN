package pe.tecsup.main.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val campusLime = ColegiaYellow
internal val campusInk = ColegiaNavy
private val campusBorder = if (BuildConfig.CAMPUS_COLOR.equals("#087f68", ignoreCase = true)) {
    Color(0xFFD3E4FE)
} else {
    Color(android.graphics.Color.parseColor(BuildConfig.CAMPUS_COLOR))
}

@Composable
internal fun CampusBanner(
    title: String,
    subtitle: String = BuildConfig.CAMPUS_NAME,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Surface(color = Color(0xFFE4EFFF), contentColor = campusInk, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, campusBorder),
        modifier=Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(title, style = MaterialTheme.typography.headlineMedium)
            }
            if (actions != null) actions()
        }
    }
}
