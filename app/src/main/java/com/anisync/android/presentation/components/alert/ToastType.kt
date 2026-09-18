package com.anisync.android.presentation.components.alert

import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.anisync.android.data.util.ApiError

/**
 * How much room a toast takes.
 *
 * It follows from how much the user has to do about it. A strip explains a slowdown nobody needs to
 * act on, an alert reports something that went wrong and may carry an action.
 */
enum class ToastDensity { STRIP, ALERT }

/**
 * What a toast is about.
 *
 * Previously an HTTP status with a hardcoded colour each, which meant the toast showed "429" to the
 * user, could not tell a deferred background request from a real rate limit, and painted six fixed
 * hexes that fought whatever palette MaterialKolor had built. The kinds mirror [ApiError], and the
 * colour for each is a role in the active scheme, resolved in `ToastVisuals`.
 */
enum class ToastType(
    val density: ToastDensity,
    val icon: ImageVector,
    /** Stays until the user acts on it or the cause clears, rather than timing out. */
    val persistent: Boolean = false,
) {
    /** The gate is spacing requests out. Nothing has failed. */
    PACING(ToastDensity.STRIP, Icons.Outlined.Update),

    /** Background work gave its turn up to the user. Nothing was sent and nothing failed. */
    DEFERRED(ToastDensity.STRIP, Icons.Outlined.CloudUpload),

    RATE_LIMITED(ToastDensity.ALERT, Icons.Outlined.Schedule, persistent = true),
    OFFLINE(ToastDensity.ALERT, Icons.Outlined.CloudOff),
    TIMEOUT(ToastDensity.ALERT, Icons.Outlined.HourglassEmpty),
    SERVER_ERROR(ToastDensity.ALERT, Icons.Outlined.Dns),
    VALIDATION_ERROR(ToastDensity.ALERT, Icons.Outlined.EditNote),
    PERMISSION_DENIED(ToastDensity.ALERT, Icons.Outlined.VisibilityOff),
    SESSION_EXPIRED(ToastDensity.ALERT, Icons.Outlined.PersonOff, persistent = true),
    API_DISABLED(ToastDensity.ALERT, Icons.Outlined.Report, persistent = true),
    NOT_FOUND(ToastDensity.ALERT, Icons.Outlined.SearchOff),
    ERROR(ToastDensity.ALERT, Icons.Outlined.ErrorOutline),
    SUCCESS(ToastDensity.ALERT, Icons.Outlined.CheckCircle),
    INFO(ToastDensity.ALERT, Icons.Outlined.Info);

    companion object {

        fun of(error: ApiError): ToastType = when (error) {
            is ApiError.RateLimited -> RATE_LIMITED
            is ApiError.Deferred -> DEFERRED
            is ApiError.SessionExpired, is ApiError.TokenRejected -> SESSION_EXPIRED
            is ApiError.PermissionDenied -> PERMISSION_DENIED
            is ApiError.ApiDisabled -> API_DISABLED
            is ApiError.ServerError -> SERVER_ERROR
            is ApiError.Offline -> OFFLINE
            is ApiError.Timeout -> TIMEOUT
            is ApiError.Validation -> VALIDATION_ERROR
            is ApiError.GraphQLError, is ApiError.Unknown -> ERROR
        }

        /** The last resort for a failure that never reached the classifier and only has a status. */
        fun fromCode(code: Int?): ToastType = when (code) {
            400 -> VALIDATION_ERROR
            401, 403 -> PERMISSION_DENIED
            404 -> NOT_FOUND
            429 -> RATE_LIMITED
            in 500..599 -> SERVER_ERROR
            null -> INFO
            else -> ERROR
        }
    }
}

/**
 * The colour a kind is drawn in.
 *
 * A role in the active scheme, never a literal. The six hardcoded hexes this replaces were picked
 * against one palette and clashed with every other one MaterialKolor can build, which is most of
 * them.
 *
 * Kinds share roles freely: what tells a rate limit from being offline is the title and the
 * sentence under it, both of which a screen reader gets in full, and each kind's icon is a distinct
 * silhouette at 20dp on top of that.
 */
@Composable
fun ToastType.accentColor(): Color = when (this) {
    ToastType.PACING, ToastType.DEFERRED -> MaterialTheme.colorScheme.onSurfaceVariant
    ToastType.RATE_LIMITED, ToastType.OFFLINE, ToastType.TIMEOUT ->
        MaterialTheme.colorScheme.tertiary

    ToastType.SERVER_ERROR, ToastType.VALIDATION_ERROR, ToastType.API_DISABLED, ToastType.ERROR ->
        MaterialTheme.colorScheme.error

    ToastType.PERMISSION_DENIED, ToastType.NOT_FOUND, ToastType.INFO ->
        MaterialTheme.colorScheme.secondary

    ToastType.SESSION_EXPIRED, ToastType.SUCCESS -> MaterialTheme.colorScheme.primary
}
