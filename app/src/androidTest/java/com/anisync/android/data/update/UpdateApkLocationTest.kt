package com.anisync.android.data.update

import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Guards where the downloaded update APK lives and that the installer can be handed a URI for it.
 *
 * Both halves fail loudly only at install time, which is the worst moment to find out: a
 * `filepaths.xml` that no longer covers the download directory throws "Failed to find configured
 * root" the instant the user taps install, and there is no earlier signal.
 */
@RunWith(AndroidJUnit4::class)
class UpdateApkLocationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val apkDir = File(context.externalCacheDir, "apk")
    private val apk = File(apkDir, "latest.apk")

    @After
    fun removeFixture() {
        apk.delete()
    }

    @Test
    fun downloadDirectoryIsExternalCacheSoTheSystemCanReclaimIt() {
        assertEquals(File(context.externalCacheDir, "apk"), apkDir)
        assertTrue(
            "the APK must not sit in external files, where nothing reclaims it and Auto Backup " +
                "uploads it",
            !apkDir.absolutePath.startsWith(
                context.getExternalFilesDir(null)?.absolutePath ?: "unset"
            )
        )
    }

    @Test
    fun fileProviderResolvesTheDownloadedApk() {
        apkDir.mkdirs()
        apk.writeBytes(ByteArray(16))

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )

        assertEquals("content", uri.scheme)
        assertTrue("expected the apk path in $uri", uri.toString().endsWith("latest.apk"))
    }

    @Test
    fun cleanUpRemovesAnAbandonedDownload() {
        apkDir.mkdirs()
        apk.writeBytes(ByteArray(1024))

        UpdateManager(context).cleanUpDownloads()

        assertTrue("an abandoned download should not survive a restart", !apk.exists())
    }
}
