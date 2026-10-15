package com.anisync.android.data.network

import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.android.AndroidDns

/**
 * Resolves through the platform DNS resolver, which also returns a host's HTTPS records. Those
 * carry the Encrypted Client Hello config, so a host that publishes one, AnimeThemes for example,
 * never sees its name sent in the clear. Android only negotiates ECH from 17 on for apps that
 * target it, and the resolver API needs 29, so older devices keep the system default.
 */
fun OkHttpClient.Builder.encryptedClientHello(): OkHttpClient.Builder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) dns(AndroidDns()) else this
