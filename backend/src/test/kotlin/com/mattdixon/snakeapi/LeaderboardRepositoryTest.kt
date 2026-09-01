package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.db.LeaderboardRepository
import com.mattdixon.snakeapi.db.Scores
import com.mattdixon.snakeapi.model.ScoreSubmission
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LeaderboardRepositoryTest {

    // See TestDatabase for why every repository test class shares one H2 database rather than
    // connecting to its own.
    init {
        TestDatabase.ensureConnected()
    }

    @BeforeTest
    fun setup() {
        transaction {
            SchemaUtils.drop(Scores)
            SchemaUtils.create(Scores)
        }
    }

    @Test
    fun `higher score ranks first`() = runTest {
        LeaderboardRepository.submit(ScoreSubmission("alice", 100, "normal"))
        LeaderboardRepository.submit(ScoreSubmission("bob", 250, "normal"))
        LeaderboardRepository.submit(ScoreSubmission("carol", 175, "normal"))

        val top = LeaderboardRepository.top(limit = 10, difficulty = null)

        assertEquals(listOf("bob", "carol", "alice"), top.map { it.nickname })
        assertEquals(listOf(1, 2, 3), top.map { it.rank })
    }

    @Test
    fun `difficulty filter only returns matching entries`() = runTest {
        LeaderboardRepository.submit(ScoreSubmission("alice", 100, "easy"))
        LeaderboardRepository.submit(ScoreSubmission("bob", 300, "hard"))

        val hardOnly = LeaderboardRepository.top(limit = 10, difficulty = "hard")

        assertEquals(1, hardOnly.size)
        assertEquals("bob", hardOnly.first().nickname)
    }

    @Test
    fun `submit reports rank among existing scores`() = runTest {
        LeaderboardRepository.submit(ScoreSubmission("alice", 500, "normal"))
        val (_, rank) = LeaderboardRepository.submit(ScoreSubmission("bob", 100, "normal"))

        assertTrue(rank == 2)
    }
}
