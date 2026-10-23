package com.anisync.android.presentation.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** An odd width makes most GPUs pad their rows, the case that sheared cards on Android 8. */
    @Test
    fun readBackKeepsColumnsStraight() {
        val width = 1001
        val height = 400
        val column = 500
        lateinit var layer: GraphicsLayer
        composeTestRule.setContent {
            layer = rememberGraphicsLayer()
            Box(
                Modifier
                    .layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(width, height))
                        layout(width, height) { placeable.place(0, 0) }
                    }
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color.Blue)
                    drawRect(Color.Red, Offset(column.toFloat(), 0f), Size(1f, size.height))
                }
            }
        }
        composeTestRule.waitForIdle()

        val bitmap = runBlocking(Dispatchers.Main) { layer.readBackThroughImageReader() }

        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        for (y in 0 until height) {
            assertEquals("row $y", Color.Red.toArgb(), bitmap.getPixel(column, y))
            assertEquals("row $y", Color.Blue.toArgb(), bitmap.getPixel(column - 1, y))
        }
    }
}
