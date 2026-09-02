package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.model.PoolContributionRequest
import com.mattdixon.snakeapi.model.ValidationException
import com.mattdixon.snakeapi.model.validated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val VALID_DEVICE_ID = "a1b2c3d4-e5f6-4a1b-8c2d-1234567890ab"

class PoolValidationTest {

    @Test
    fun `accepts a known effect type and normalizes case`() {
        val result = PoolContributionRequest(nickname = "  Mat_Dixon-77  ", effectType = "shared_gift", deviceId = VALID_DEVICE_ID).validated()
        assertEquals("Mat_Dixon-77", result.nickname)
        assertEquals("SHARED_GIFT", result.effectType)
    }

    @Test
    fun `accepts the newer shield and fog effect types`() {
        assertEquals("SHARED_SHIELD", PoolContributionRequest(nickname = "player", effectType = "shared_shield", deviceId = VALID_DEVICE_ID).validated().effectType)
        assertEquals("SHARED_FOG", PoolContributionRequest(nickname = "player", effectType = "shared_fog", deviceId = VALID_DEVICE_ID).validated().effectType)
    }

    @Test
    fun `rejects an unknown effect type`() {
        assertFailsWith<ValidationException> {
            PoolContributionRequest(nickname = "player", effectType = "SPEED_UP", deviceId = VALID_DEVICE_ID).validated()
        }
    }

    @Test
    fun `rejects a blank nickname`() {
        assertFailsWith<ValidationException> {
            PoolContributionRequest(nickname = "   ", effectType = "SHARED_PRANK", deviceId = VALID_DEVICE_ID).validated()
        }
    }

    @Test
    fun `rejects a nickname containing a blocked word`() {
        assertFailsWith<ValidationException> {
            PoolContributionRequest(nickname = "shitlord", effectType = "SHARED_PRANK", deviceId = VALID_DEVICE_ID).validated()
        }
    }

    @Test
    fun `normalizes a device id to lowercase`() {
        val result = PoolContributionRequest(nickname = "player", effectType = "SHARED_PRANK", deviceId = VALID_DEVICE_ID.uppercase()).validated()
        assertEquals(VALID_DEVICE_ID, result.deviceId)
    }

    @Test
    fun `rejects a device id that isn't a UUID`() {
        assertFailsWith<ValidationException> {
            PoolContributionRequest(nickname = "player", effectType = "SHARED_PRANK", deviceId = "not-a-uuid").validated()
        }
    }
}
