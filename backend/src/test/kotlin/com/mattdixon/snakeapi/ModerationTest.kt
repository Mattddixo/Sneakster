package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.model.containsBlockedContent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModerationTest {

    @Test
    fun `flags a blocked word regardless of case`() {
        assertTrue(containsBlockedContent("SHIT_head"))
    }

    @Test
    fun `flags a blocked word as a substring`() {
        assertTrue(containsBlockedContent("xXfuckboiXx"))
    }

    @Test
    fun `allows an ordinary nickname`() {
        assertFalse(containsBlockedContent("phatboislim-77"))
    }
}
