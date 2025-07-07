package cumulus.battery.stats.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

open class CumulusColor(
    val blue: Color,
    val yellow: Color,
    val pink: Color,
    val purple: Color
)

object CumulusLightColor : CumulusColor(
    blue = Color(0xFF1A6AE6),
    yellow = Color(0xFFD69400),
    pink = Color(0xFF9A4675),
    purple = Color(0xFF4F3E9C)
)

object CumulusDarkColor : CumulusColor(
    blue = Color(0xFF3D8AFF),
    yellow = Color(0xFFFFB81C),
    pink = Color(0xFFC05F97),
    purple = Color(0xFF6A5ACD)
)

@Composable
fun cumulusColor(): CumulusColor {
    return when {
        isSystemInDarkTheme() -> CumulusDarkColor
        else -> CumulusLightColor
    }
}