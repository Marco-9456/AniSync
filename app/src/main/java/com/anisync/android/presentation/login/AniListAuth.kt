package com.anisync.android.presentation.login

/**
 * AniList OAuth entry point shared by the login screen and the account manager.
 * Implicit grant — the browser redirects back to `anisync://auth#access_token=...&expires_in=...`,
 * which [com.anisync.android.MainActivity] handles.
 */
object AniListAuth {
    private const val CLIENT_ID = "32893"
    const val AUTH_URL =
        "https://anilist.co/api/v2/oauth/authorize?client_id=$CLIENT_ID&response_type=token"
}
