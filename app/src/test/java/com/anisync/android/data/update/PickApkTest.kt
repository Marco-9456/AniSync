package com.anisync.android.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins which release asset the updater downloads. The list is in the order GitHub returns it,
 * sorted by name, which is why taking the first APK handed every phone the arm64 split.
 */
class PickApkTest {

    private val release = listOf(
        "AniSync-v3.3.0-arm64-v8a-release.apk",
        "AniSync-v3.3.0-armeabi-v7a-release.apk",
        "AniSync-v3.3.0-universal-release.apk",
        "AniSync-v3.3.0-x86-release.apk",
        "AniSync-v3.3.0-x86_64-release.apk",
    ).map { it to "https://example.com/$it" }

    private fun picked(vararg abis: String) =
        pickApk(release, abis.asList())?.substringAfterLast('/')

    @Test
    fun arm64PhoneGetsTheArm64Split() {
        assertEquals(
            "AniSync-v3.3.0-arm64-v8a-release.apk",
            picked("arm64-v8a", "armeabi-v7a", "armeabi")
        )
    }

    @Test
    fun thirtyTwoBitPhoneGetsTheArmeabiSplit() {
        assertEquals("AniSync-v3.3.0-armeabi-v7a-release.apk", picked("armeabi-v7a", "armeabi"))
    }

    @Test
    fun translatingEmulatorGetsItsNativeSplit() {
        assertEquals("AniSync-v3.3.0-x86_64-release.apk", picked("x86_64", "arm64-v8a"))
    }

    @Test
    fun x86IsNotMistakenForX8664() {
        assertEquals("AniSync-v3.3.0-x86-release.apk", picked("x86"))
    }

    @Test
    fun unknownAbiFallsBackToUniversal() {
        assertEquals("AniSync-v3.3.0-universal-release.apk", picked("riscv64"))
    }

    @Test
    fun releaseWithoutSplitsFallsBackToItsFirstApk() {
        val single = listOf("AniSync.apk" to "https://example.com/AniSync.apk")
        assertEquals("https://example.com/AniSync.apk", pickApk(single, listOf("arm64-v8a")))
    }

    @Test
    fun releaseWithoutApksPicksNothing() {
        assertNull(pickApk(emptyList(), listOf("arm64-v8a")))
    }
}
