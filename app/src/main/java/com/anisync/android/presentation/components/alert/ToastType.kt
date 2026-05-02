package com.anisync.android.presentation.components.alert

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToastType(
    val code: Int?,
    val icon: ImageVector,
    val color: Color
) {
    VALIDATION_ERROR(400, Icons.Outlined.ErrorOutline, Color(0xFFE53935)),
    UNAUTHORIZED(401, Icons.Outlined.Lock, Color(0xFF7B1FA2)),
    NOT_FOUND(404, Icons.Outlined.SearchOff, Color(0xFF455A64)),
    TOO_MANY_REQUESTS(429, Icons.Outlined.Timer, Color(0xFFE64A19)),
    SERVER_ERROR(500, Icons.Outlined.Storage, Color(0xFFD32F2F)),
    INFO(null, Icons.Outlined.Info, Color(0xFF1976D2)),
    SUCCESS(null, Icons.Outlined.Info, Color(0xFF388E3C));

    companion object {
        fun fromCode(code: Int?): ToastType {
            return entries.find { it.code == code } ?: INFO
        }
    }
}
