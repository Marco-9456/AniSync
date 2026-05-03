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
    val color: Color,
    val accessibilityLabel: String
) {
    VALIDATION_ERROR(400, Icons.Outlined.ErrorOutline, Color(0xFFE53935), "Validation Error"),
    UNAUTHORIZED(401, Icons.Outlined.Lock, Color(0xFF7B1FA2), "Unauthorized"),
    NOT_FOUND(404, Icons.Outlined.SearchOff, Color(0xFF455A64), "Not Found"),
    TOO_MANY_REQUESTS(429, Icons.Outlined.Timer, Color(0xFFE64A19), "Too Many Requests"),
    SERVER_ERROR(500, Icons.Outlined.Storage, Color(0xFFD32F2F), "Server Error"),
    INFO(null, Icons.Outlined.Info, Color(0xFF1976D2), "Information"),
    SUCCESS(null, Icons.Outlined.Info, Color(0xFF388E3C), "Success");

    // Pre-calculate derived colors to prevent recreating them during UI recomposition
    val surfaceColor: Color = color.copy(alpha = 0.1f)
    val codeBackgroundColor: Color = color.copy(alpha = 0.15f)

    companion object {
        private val codeToTypeMap: Map<Int, ToastType> = entries
            .filter { it.code != null }
            .associateBy { it.code!! }

        fun fromCode(code: Int?): ToastType {
            if (code == null) return INFO
            return codeToTypeMap[code] ?: INFO
        }
    }
}