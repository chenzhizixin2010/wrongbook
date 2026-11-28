package com.chenzixin.wrongbook.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateChannelTest {

    @Test
    fun `默认通道是 Beta`() {
        assertEquals(UpdateChannel.BETA, UpdateChannel.DEFAULT)
        assertEquals(UpdateChannel.BETA, UpdateChannel.fromKey(null))
        assertEquals(UpdateChannel.BETA, UpdateChannel.fromKey("不认识的值"))
    }

    @Test
    fun `从持久化的名字还原`() {
        assertEquals(UpdateChannel.STABLE, UpdateChannel.fromKey("STABLE"))
        assertEquals(UpdateChannel.ALPHA, UpdateChannel.fromKey("ALPHA"))
    }

    @Test
    fun `正式版通道不接收任何测试版`() {
        assertTrue(UpdateChannel.STABLE.accepts("0.13.0", isPrerelease = false))
        assertFalse(UpdateChannel.STABLE.accepts("0.13.0", isPrerelease = true))
        assertFalse(UpdateChannel.STABLE.accepts("0.13.A01", isPrerelease = true))
    }

    @Test
    fun `Beta 通道收预发布但挡住 Alpha`() {
        assertTrue(UpdateChannel.BETA.accepts("0.13.0", isPrerelease = true))
        assertTrue(UpdateChannel.BETA.accepts("0.13.0", isPrerelease = false))
        assertFalse(UpdateChannel.BETA.accepts("0.13.A01", isPrerelease = true))
    }

    @Test
    fun `Alpha 通道全收`() {
        assertTrue(UpdateChannel.ALPHA.accepts("0.13.A01", isPrerelease = true))
        assertTrue(UpdateChannel.ALPHA.accepts("0.13.0", isPrerelease = false))
    }

    @Test
    fun `三个通道都有可展示的文案`() {
        UpdateChannel.entries.forEach { channel ->
            assertTrue(channel.label.isNotBlank())
            assertTrue(channel.summary.isNotBlank())
        }
    }
}
