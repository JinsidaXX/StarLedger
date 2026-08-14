package com.starledger.app.core.starmap

import androidx.compose.ui.graphics.Color
import com.starledger.app.core.design.theme.toArgbLong
import com.starledger.app.core.design.theme.toColor
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorRoundTripTest {

    @Test
    fun `颜色 Long 往返正确`() {
        val original = Color(0.95f, 0.72f, 0.37f)
        val saved = original.toArgbLong()
        val restored = saved.toColor()
        assertEquals(original, restored)
    }

    @Test
    fun `色相生成颜色往返`() {
        val c = Color.hsv(219f, 0.72f, 0.92f)
        val saved = c.toArgbLong()
        val restored = saved.toColor()
        assertEquals(c, restored)
    }
}
