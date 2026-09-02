package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.db.PoolContributions
import com.mattdixon.snakeapi.db.PoolRepository
import com.mattdixon.snakeapi.model.PoolContribution
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PoolRepositoryTest {

    // See TestDatabase for why every repository test class shares one H2 database rather than
    // connecting to its own.
    init {
        TestDatabase.ensureConnected()
    }

    @BeforeTest
    fun setup() {
        transaction {
            SchemaUtils.drop(PoolContributions)
            SchemaUtils.create(PoolContributions)
        }
    }

    @Test
    fun `pulling from an empty pool returns null`() = runTest {
        assertNull(PoolRepository.pullRandom())
    }

    @Test
    fun `a contribution can be pulled back out with its contributor`() = runTest {
        PoolRepository.contribute(PoolContribution(nickname = "alice", effectType = "SHARED_GIFT", deviceId = "11111111-1111-1111-1111-111111111111"))

        val pulled = PoolRepository.pullRandom()

        assertEquals("SHARED_GIFT", pulled?.effectType)
        assertEquals("alice", pulled?.contributedBy)
    }

    @Test
    fun `pulling consumes the contribution so it can never be handed out twice`() = runTest {
        PoolRepository.contribute(PoolContribution(nickname = "alice", effectType = "SHARED_GIFT", deviceId = "11111111-1111-1111-1111-111111111111"))

        val first = PoolRepository.pullRandom()
        val second = PoolRepository.pullRandom()

        assertTrue(first != null)
        assertNull(second)
    }

    @Test
    fun `multiple contributions are each pulled exactly once`() = runTest {
        repeat(5) { PoolRepository.contribute(PoolContribution(nickname = "player$it", effectType = "SHARED_PRANK", deviceId = "2222222$it-2222-2222-2222-222222222222")) }

        val pulled = (1..5).map { PoolRepository.pullRandom() }

        assertTrue(pulled.all { it != null })
        assertEquals(5, pulled.map { it?.contributedBy }.toSet().size)
        assertNull(PoolRepository.pullRandom())
    }

    @Test
    fun `a device is capped after too many contributions in the window`() = runTest {
        val deviceId = "33333333-3333-3333-3333-333333333333"
        val accepted = (1..11).map {
            PoolRepository.contribute(PoolContribution(nickname = "player", effectType = "SHARED_PRANK", deviceId = deviceId))
        }

        assertEquals(10, accepted.count { it }, "expected exactly the first 10 to be accepted")
        assertTrue(!accepted.last(), "the 11th contribution from the same device should be rejected")
    }

    @Test
    fun `different devices are rate-limited independently`() = runTest {
        repeat(10) { PoolRepository.contribute(PoolContribution(nickname = "player", effectType = "SHARED_PRANK", deviceId = "44444444-4444-4444-4444-444444444444")) }

        val fromAnotherDevice = PoolRepository.contribute(
            PoolContribution(nickname = "player", effectType = "SHARED_PRANK", deviceId = "55555555-5555-5555-5555-555555555555"),
        )

        assertTrue(fromAnotherDevice, "a different device's own contributions shouldn't count against another device's cap")
    }
}
