package com.anisync.android.data.network

import com.anisync.android.data.util.ApiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every payload here is a real AniList response, taken either from their documentation or from a
 * live probe of the API. The classifier exists because the HTTP status line and the body's `status`
 * disagree often enough that neither can be read on its own.
 */
class AniListErrorsTest {

    private fun classify(
        httpStatus: Int?,
        body: String?,
        operationName: String? = "GetMedia",
        tokenScoped: Boolean = false,
        retryAfterSeconds: Long? = null,
    ): ApiError = AniListErrors.classify(
        httpStatus = httpStatus,
        errors = AniListErrors.parseBody(body),
        operationName = operationName,
        tokenScoped = tokenScoped,
        retryAfterSeconds = retryAfterSeconds,
    )

    @Test
    fun `a rate limited response carries the wait the server asked for`() {
        val error = classify(
            httpStatus = 429,
            body = """{"data":null,"errors":[{"message":"Too Many Requests.","status":429}]}""",
            retryAfterSeconds = 30,
        )

        assertTrue(error is ApiError.RateLimited)
        assertEquals(30, (error as ApiError.RateLimited).retryAfterSeconds)
    }

    @Test
    fun `a rate limited response without a header falls back to the documented minute`() {
        val error = classify(429, """{"errors":[{"message":"Too Many Requests.","status":429}]}""")
        assertEquals(60, (error as ApiError.RateLimited).retryAfterSeconds)
    }

    /**
     * A bad or expired token answers HTTP 400, not 401. Keying the session-expired flow on 401
     * alone, as the previous interceptor did, never fires for the case it was written for.
     */
    @Test
    fun `an invalid token ends the session even though AniList answers 400`() {
        val error = classify(400, """{"data":null,"errors":[{"message":"Invalid token","status":400}]}""")
        assertTrue("was $error", error is ApiError.SessionExpired)
    }

    @Test
    fun `an invalid token on a background account poll spares the active session`() {
        val error = classify(
            httpStatus = 400,
            body = """{"data":null,"errors":[{"message":"Invalid token","status":400}]}""",
            tokenScoped = true,
        )
        assertTrue("was $error", error is ApiError.TokenRejected)
    }

    /**
     * The names here are the ones in `app/src/main/graphql`, which is what `operation.name()`
     * returns. Asserting against the GraphQL field names instead is how this passed while the
     * set it guards matched nothing.
     */
    @Test
    fun `a 401 on a delete is a permission denial, not a dead session`() {
        listOf(
            "DeleteActivity",
            "DeleteActivityReply",
            "DeleteForumThread",
            "DeleteForumComment",
        ).forEach { operation ->
            val error = classify(
                httpStatus = 200,
                body = """{"data":null,"errors":[{"message":"Forbidden","status":401}]}""",
                operationName = operation,
            )

            assertTrue("$operation was $error", error is ApiError.PermissionDenied)
            assertEquals("Forbidden", (error as ApiError.PermissionDenied).reason)
        }
    }

    @Test
    fun `a 401 on anything else ends the session`() {
        val error = classify(401, """{"errors":[{"message":"Unauthorized","status":401}]}""")
        assertTrue("was $error", error is ApiError.SessionExpired)
    }

    @Test
    fun `the documented API shutdown is not reported as a permission problem`() {
        val body = """
            {"errors":[{"message":"The AniList API has been temporarily disabled due to severe
            stability issues. Please check the official AniList Discord for more information.",
            "status":403,"locations":[{"line":1,"column":1}]}],"data":null}
        """.trimIndent().replace("\n", " ")

        val error = classify(403, body)

        assertTrue("was $error", error is ApiError.ApiDisabled)
        assertTrue((error as ApiError.ApiDisabled).notice.contains("temporarily disabled"))
    }

    @Test
    fun `validation messages reach the user instead of the word validation`() {
        val body = """
            {"data":null,"errors":[{"message":"validation","status":400,
            "locations":[{"line":2,"column":3}],
            "validation":{"id":["The selected id is invalid."],
            "score":["The score may not be greater than 100."]}}]}
        """.trimIndent().replace("\n", "")

        val error = classify(400, body)

        assertTrue("was $error", error is ApiError.Validation)
        val validation = error as ApiError.Validation
        assertEquals(
            listOf("The selected id is invalid."),
            validation.fields["id"],
        )
        assertEquals(
            listOf("The score may not be greater than 100."),
            validation.fields["score"],
        )
        assertTrue(validation.messages.contains("The score may not be greater than 100."))
    }

    @Test
    fun `a plain query error keeps its message and status`() {
        val body = """
            {"data":null,"errors":[{"message":"Cannot query field \"nope\" on type \"MediaList\".",
            "status":400,"locations":[{"line":4,"column":5}]}]}
        """.trimIndent().replace("\n", "")

        val error = classify(400, body)

        assertTrue("was $error", error is ApiError.GraphQLError)
        assertEquals(400, (error as ApiError.GraphQLError).statusCode)
    }

    @Test
    fun `a server error keeps its status code`() {
        val error = classify(502, null)
        assertEquals(502, (error as ApiError.ServerError).statusCode)
    }

    @Test
    fun `a Cloudflare page with no parseable body still classifies from the status`() {
        val error = classify(503, "<html><head><title>503</title></head></html>")
        assertTrue("was $error", error is ApiError.ServerError)
    }

    @Test
    fun `an empty body is not mistaken for a parsed error`() {
        assertTrue(AniListErrors.parseBody(null).isEmpty())
        assertTrue(AniListErrors.parseBody("").isEmpty())
        assertTrue(AniListErrors.parseBody("not json at all").isEmpty())
        assertTrue(AniListErrors.parseBody("""{"data":{"Media":{"id":1}}}""").isEmpty())
    }

    @Test
    fun `the body status wins over a disagreeing HTTP status`() {
        // AniList returns 200 with an embedded status for permission denials.
        val error = classify(
            httpStatus = 200,
            body = """{"data":null,"errors":[{"message":"Too Many Requests.","status":429}]}""",
            retryAfterSeconds = 12,
        )
        assertTrue("was $error", error is ApiError.RateLimited)
    }
}
