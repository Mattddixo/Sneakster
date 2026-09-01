package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.model.PoolContributionRequest
import com.mattdixon.snakeapi.model.ValidationException
import com.mattdixon.snakeapi.model.validated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PoolValidationTest {

    @Test
    fun `accepts a known effect type and normalizes case`() {
        val result = PoolContributionRequest(nickname = "  Mat_Dixon-77  ", effectType = "shared_gift").validated()
        assertEquals("Mat_Dixon-77", result.nickname)
        assertEquals("SHARED_GIFT", result.effectType)
    }

    @Test
    fun `rejects an unknown effect type`() {
        assertFailsWith<ValidationException> {
            PoolContributionRequest(nickname = "player", effectType = "SPEED_UP").validated()
        }
    }

    @Test
    fun `rejects a blank nickname`() {
        assertFailsWith<ValidationException> {
            PoolContributionRequest(nickname = "   ", effectType = "SHARED_PRANK").validated()
        }
    }
}
