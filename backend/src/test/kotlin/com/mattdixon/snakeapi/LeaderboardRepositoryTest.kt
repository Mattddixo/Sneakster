package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.db.LeaderboardRepository
import com.mattdixon.snakeapi.db.Scores
import com.mattdixon.snakeapi.model.ScoreSubmission
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LeaderboardRepositoryTest {

    // Connected once for the whole class: Exposed pins a TransactionManager to the JUnit
    // executor thread on first use, so reconnecting to a new database per test silently
    // leaves later transactions on that thread still targeting the first connection.
    companion object {
        init {
            Database.connect("jdbc:h2:mem:leaderboard-repository-test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        }
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
