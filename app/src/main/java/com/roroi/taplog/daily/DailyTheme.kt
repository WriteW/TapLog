package com.roroi.taplog.daily

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.roroi.taplog.R

// 暖黄色调，不要太黄
val WarmYellowBg = Color(0xFFFFFBE6) // 极浅的暖黄
private val BlackTextColor = Color(0xFF333333)
private val WhiteTextColor = Color(0xFFFFFFFF)
fun getTextColor(isDark: Boolean) = if (isDark) WhiteTextColor else BlackTextColor
val TimelineGray = Color(0xFFE0E0E0)
val GoldenYellow = Color(0xFFFFD700)
val cascadiaFont = FontFamily(
    Font(R.font.cascadia_mono, FontWeight.Normal),
    Font(R.font.cascadia_mono_bold, FontWeight.Bold),
)
val soBiscuitFont = FontFamily(
    Font(R.font.so_biscuit, FontWeight.Normal)
)

val dymonFont = FontFamily(Font(R.font.dymon))

const val cardTransparentScale = 0.6f
const val longClickMs = 500L