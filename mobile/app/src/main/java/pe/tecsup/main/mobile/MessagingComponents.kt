package pe.tecsup.main.mobile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable internal fun PersonAvatar(name:String,large:Boolean=false) {
 Box(Modifier.size(if(large)88.dp else 52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),contentAlignment=Alignment.Center) {
  Text(name.trim().split(" ").filter{it.isNotBlank()}.take(2).joinToString(""){it.take(1)}.uppercase().ifBlank{"M"},
   style=if(large)MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSecondaryContainer)
 }
}
@Composable internal fun PageHeading(title:String,onBack:()->Unit) {
 Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
  IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Volver")}
  Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
 }
}
@Composable internal fun SettingsRow(icon:ImageVector,title:String,subtitle:String="",danger:Boolean=false,onClick:()->Unit) {
 Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=20.dp,vertical=17.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
  val color=if(danger)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
  Icon(icon,null,tint=color)
  Column(Modifier.weight(1f)){Text(title,color=color,style=MaterialTheme.typography.titleMedium)
   if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
  Icon(Icons.Outlined.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
