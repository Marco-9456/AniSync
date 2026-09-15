package com.anisync.android.data.network

import com.anisync.android.data.util.ApiError
import com.apollographql.apollo.api.Error
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * One AniList error entry, however it arrived.
 *
 * `status` and `validation` are not part of the GraphQL error spec. AniList puts them at the top
 * level of each error object, which is where Apollo files them under
 * [Error.nonStandardFields] rather than `extensions`. Reading `extensions` is why the previous code
 * never saw a status code and reported every API error as an untyped string.
 */
data class AniListError(
    val message: String,
    val status: Int? = null,
    val validation: Map<String, List<String>> = emptyMap(),
)

/**
 * Turns an AniList failure into a typed [ApiError].
 *
 * The awkward part of this API is that the meaningful status lives in the response body, not the
 * HTTP status line, and the two disagree often enough that neither can be trusted alone:
 *
 * - A bad or expired token answers **HTTP 400** with `{"message":"Invalid token","status":400}`,
 *   not the 401 you would expect. Keying the session-expired flow on 401 alone misses it.
 * - Permission denials answer 401 even though the token is fine.
 * - A 429 and a whole-API shutdown both carry their real meaning in the body's `status`.
 *
 * So the body wins where it says something, and the HTTP status is the fallback.
 *
 * Kept free of Apollo plumbing so every shape in AniList's documentation can be unit tested from a
 * literal JSON string.
 */
object AniListErrors {

    /**
     * Operations AniList answers 401 for when the token is valid but the account may not do this,
     * such as deleting a moderator's post. Treating those as session expiry would sign the user out
     * for a permission error.
     */
    private val PERMISSION_GATED_OPERATIONS = setOf(
        "DeleteActivity",
        "DeleteActivityReply",
        "DeleteThread",
        "DeleteThreadComment",
    )

    private const val INVALID_TOKEN = "invalid token"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * @param httpStatus the HTTP status line, or null when the response was a 200 carrying errors
     * @param tokenScoped true when the request carried its own account token, so a failure must not
     *   end the active session
     */
    fun classify(
        httpStatus: Int?,
        errors: List<AniListError>,
        operationName: String?,
        tokenScoped: Boolean,
        retryAfterSeconds: Long?,
    ): ApiError {
        val first = errors.firstOrNull()
        val status = first?.status ?: httpStatus
        val message = first?.message

        errors.firstOrNull { it.validation.isNotEmpty() }?.let {
            return ApiError.Validation(it.validation)
        }

        if (message != null && message.trim().lowercase() == INVALID_TOKEN) {
            return tokenFailure(tokenScoped)
        }

        return when {
            status == 429 -> ApiError.RateLimited(
                retryAfterSeconds ?: RateLimitWindowDefaults.RETRY_AFTER_SECONDS,
            )

            // Documented as the whole API being switched off, not a per-user permission problem.
            status == 403 -> ApiError.ApiDisabled(message ?: "The AniList API is unavailable.")

            status == 401 -> when {
                operationName in PERMISSION_GATED_OPERATIONS -> ApiError.PermissionDenied(message)
                else -> tokenFailure(tokenScoped)
            }

            status != null && status in 500..599 -> ApiError.ServerError(status)

            // Everything left keeps its status so callers can still branch on it, but is not a
            // server error: labelling a 400 that way made the retry policy repeat a request the
            // server had already refused on its merits.
            else -> ApiError.GraphQLError(
                errors.map { it.message }.ifEmpty { listOfNotNull(message) }
                    .ifEmpty { listOf("The server returned an error.") },
                status,
            )
        }
    }

    private fun tokenFailure(tokenScoped: Boolean): ApiError =
        if (tokenScoped) ApiError.TokenRejected() else ApiError.SessionExpired()

    /** Reads the `errors` array out of a raw AniList response body. Returns empty on anything unparseable. */
    fun parseBody(body: String?): List<AniListError> {
        if (body.isNullOrBlank()) return emptyList()
        return runCatching {
            val root = json.parseToJsonElement(body) as? JsonObject ?: return emptyList()
            val array = root["errors"] as? JsonArray ?: return emptyList()
            array.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                AniListError(
                    message = (obj["message"] as? JsonPrimitive)?.contentOrNullSafe().orEmpty(),
                    status = (obj["status"] as? JsonPrimitive)?.intOrNull,
                    validation = readValidation(obj["validation"]),
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Reads the same shape back out of errors Apollo has already parsed. */
    fun fromApolloErrors(errors: List<Error>): List<AniListError> = errors.map { error ->
        val fields = error.nonStandardFields.orEmpty()
        AniListError(
            message = error.message,
            status = (fields["status"] as? Number)?.toInt(),
            validation = readValidation(fields["validation"]),
        )
    }

    private fun readValidation(node: Any?): Map<String, List<String>> = when (node) {
        is JsonObject -> node.mapValues { (_, value) ->
            runCatching { value.jsonArray.map { it.jsonPrimitive.content } }
                .getOrElse { listOf(runCatching { value.jsonPrimitive.content }.getOrDefault("")) }
        }.filterValues { it.any { message -> message.isNotBlank() } }

        is Map<*, *> -> node.entries.mapNotNull { (key, value) ->
            val name = key as? String ?: return@mapNotNull null
            val messages = when (value) {
                is List<*> -> value.mapNotNull { it?.toString() }
                null -> emptyList()
                else -> listOf(value.toString())
            }.filter { it.isNotBlank() }
            if (messages.isEmpty()) null else name to messages
        }.toMap()

        else -> emptyMap()
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? = runCatching { content }.getOrNull()
}

/** Kept separate so [AniListErrors] does not need to reach into the gate's state model. */
internal object RateLimitWindowDefaults {
    const val RETRY_AFTER_SECONDS = RateLimitWindow.DEFAULT_RETRY_AFTER_SECONDS
}
