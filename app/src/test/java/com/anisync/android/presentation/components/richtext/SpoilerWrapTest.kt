package com.anisync.android.presentation.components.richtext

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class SpoilerWrapTest {

    @Test
    fun `a selection on one line is wrapped in place`() {
        val state = TextFieldState("He dies at the end", TextRange(3, 7))

        state.wrapSpoiler()

        assertEquals("He ~!dies!~ at the end", state.text.toString())
    }

    @Test
    fun `a selection over lines gets the markers on lines of their own`() {
        val text = "Intro\nfirst secret\nsecond secret\nOutro"
        val state = TextFieldState(text, TextRange(6, 33))

        state.wrapSpoiler()

        assertEquals("Intro\n~!\nfirst secret\nsecond secret\n!~\nOutro", state.text.toString())
    }

    @Test
    fun `a selection starting mid line breaks the line first`() {
        val state = TextFieldState("Look: one\ntwo", TextRange(6, 13))

        state.wrapSpoiler()

        assertEquals("Look: \n~!\none\ntwo\n!~", state.text.toString())
    }
}
