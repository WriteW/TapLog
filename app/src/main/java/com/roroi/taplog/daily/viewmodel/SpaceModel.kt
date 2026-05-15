package com.roroi.taplog.daily.viewmodel

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class DSpace(
    val colorBgArgb: Int,      // 背景色的 ARGB 整数值
    val colorBallArgb: Int,    // 小球颜色的 ARGB 整数值
    val isDark: Boolean = false,
    val name: String,
    val id: String = UUID.randomUUID().toString(),
    val entryId: String,
    val password: String = "",
    var isEncrypted: Boolean = false
)