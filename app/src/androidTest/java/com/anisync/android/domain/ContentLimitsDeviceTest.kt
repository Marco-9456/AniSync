package com.anisync.android.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android compiles regexes with ICU, not the JDK the unit tests run on. An inline `(?U)` passed the
 * unit tests and then failed `ContentLimits` on first use, which took down every editor.
 */
@RunWith(AndroidJUnit4::class)
class ContentLimitsDeviceTest {

    @Test
    fun reviewLengthCompilesAndCountsOnDevice() {
        assertEquals(16, ContentLimits.reviewBodyLength("**bold** _it_ ~~del~~ # head"))
        assertEquals(3, ContentLimits.reviewBodyLength("a​​b"))
    }

    @Test
    fun anEmojiCountsAsOneCharacterOnDevice() {
        assertEquals(1, ContentLimits.characterCount("🐢"))
    }
}
