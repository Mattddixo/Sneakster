package com.mattdixon.snakeapi.db

import com.mattdixon.snakeapi.model.LeaderboardEntry
import com.mattdixon.snakeapi.model.ScoreSubmission
import java.time.Instant
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll

object LeaderboardRepository {

    suspend fun submit(submission: ScoreSubmission): Pair<LeaderboardEntry, Int> = DatabaseFactory.dbQuery {
        val now = Instant.now()
        val id = Scores.insertAndGetId {
            it[nickname] = submission.nickname
            it[score] = submission.score
            it[difficulty] = submission.difficulty
            it[createdAt] = now
        }
        val betterCount = Scores.selectAll()
            .andWhere { Scores.score greater submission.score }
            .count()
        val rank = betterCount.toInt() + 1
        val entry = LeaderboardEntry(
            rank = rank,
            nickname = submission.nickname,
            score = submission.score,
            difficulty = submission.difficulty,
            achievedAt = now.toEpochMilli(),
        )
        entry to rank
    }

    suspend fun top(limit: Int, difficulty: String?): List<LeaderboardEntry> = DatabaseFactory.dbQuery {
        var query = Scores.selectAll()
        if (difficulty != null) {
            query = query.andWhere { Scores.difficulty eq difficulty }
        }
        query
            .orderBy(Scores.score to SortOrder.DESC, Scores.createdAt to SortOrder.ASC)
            .limit(limit)
            .mapIndexed { index, row ->
                LeaderboardEntry(
                    rank = index + 1,
                    nickname = row[Scores.nickname],
                    score = row[Scores.score],
                    difficulty = row[Scores.difficulty],
                    achievedAt = row[Scores.createdAt].toEpochMilli(),
                )
            }
    }

    suspend fun personalBest(nickname: String): Int? = DatabaseFactory.dbQuery {
        Scores.selectAll()
            .andWhere { Scores.nickname eq nickname }
            .orderBy(Scores.score to SortOrder.DESC)
            .limit(1)
            .map { it[Scores.score] }
            .firstOrNull()
    }
}
